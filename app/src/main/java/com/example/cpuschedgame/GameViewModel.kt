package com.example.cpuschedgame

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.security.MessageDigest

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // ── Database ──────────────────────────────────────────────────
    private val db      = AppDatabase.getDatabase(application)
    private val dao     = db.playerScoreDao()
    private val userDao = db.userDao()

    val highScore = dao.getHighScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val topScores = dao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // ── Auth state ────────────────────────────────────────────────
    var currentUsername by mutableStateOf(""); private set
    var authError       by mutableStateOf(""); private set
    var isLoggedIn      by mutableStateOf(false); private set

    // ── Algorithm ─────────────────────────────────────────────────
    var assignedAlgorithm by mutableStateOf(SchedulingAlgorithm.FCFS); private set

    // ── Clocks ────────────────────────────────────────────────────
    var schedulingTime by mutableIntStateOf(0); private set
    var wallTime       by mutableFloatStateOf(0f); private set

    // ── Process lists ─────────────────────────────────────────────
    var waitingProcesses   = mutableStateListOf<Process>(); private set
    var pendingProcesses   = mutableStateListOf<Process>(); private set
    var runningProcess     by mutableStateOf<Process?>(null); private set
    var completedProcesses = mutableStateListOf<Process>(); private set
    var ganttChart         = mutableStateListOf<GanttEntry>(); private set

    // ── Game state ────────────────────────────────────────────────
    var score             by mutableIntStateOf(0);   private set
    var lives             by mutableIntStateOf(3);   private set
    var isGameOver        by mutableStateOf(false);  private set
    var isGameWon         by mutableStateOf(false);  private set
    var isPaused          by mutableStateOf(false);  private set
    var showAlgoIntro     by mutableStateOf(true);   private set
    var cpuBurstProgress  by mutableFloatStateOf(0f);private set
    var cpuRemainingBurst by mutableIntStateOf(0);   private set
    var correctPicks      by mutableIntStateOf(0);   private set
    var wrongPicks        by mutableIntStateOf(0);   private set
    var lastWrongPid      by mutableStateOf<Int?>(null); private set

    // ── Private fields ────────────────────────────────────────────
    private val MAX_PROCESSES     = 10
    private val GAME_SPEED        = 0.4f
    private var cpuTimer          = 0f
    private var spawnWallTimer    = 0f
    private var spawnWallInterval = 2.5f
    private var pidCounter        = 1
    private var gameJob: Job?     = null
    private var wrongFlashJob: Job? = null

    private val nameParts = listOf(
        "calc", "browser", "mail", "media", "net",
        "disk", "ui", "db", "auth", "kernel", "svc", "daemon"
    )

    // ── Auth ──────────────────────────────────────────────────────

    /** SHA-256 hash — never store plain text passwords */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Clears any leftover auth error — call when switching between login/signup screens */
    fun clearAuthError() { authError = "" }

    /**
     * Signup — validates input, checks uniqueness, saves new user.
     * Does NOT set isLoggedIn — player must log in manually after signup.
     */
    fun signup(username: String, password: String, onSuccess: () -> Unit) {
        authError = ""
        if (username.isBlank())  { authError = "Username cannot be empty."; return }
        if (username.length < 3) { authError = "Username must be at least 3 characters."; return }
        if (password.length < 6) { authError = "Password must be at least 6 characters."; return }

        viewModelScope.launch {
            val exists = userDao.usernameExists(username.trim())
            if (exists > 0) {
                authError = "Username \"${username.trim()}\" is already taken."
                return@launch
            }
            userDao.insertUser(
                User(
                    username = username.trim(),
                    password = hashPassword(password)
                )
            )
            // Do NOT set isLoggedIn here — player goes to login screen next
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    /**
     * Login — finds user by username, compares hashed password.
     * Sets isLoggedIn and currentUsername on success.
     */
    fun login(username: String, password: String, onSuccess: () -> Unit) {
        authError = ""
        if (username.isBlank()) { authError = "Please enter your username."; return }
        if (password.isBlank()) { authError = "Please enter your password."; return }

        viewModelScope.launch {
            val user = userDao.getUserByUsername(username.trim())
            when {
                user == null ->
                    authError = "No account found for \"${username.trim()}\"."
                user.password != hashPassword(password) ->
                    authError = "Incorrect password."
                else -> {
                    currentUsername = user.username
                    isLoggedIn = true
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            }
        }
    }

    /** Logout — clears session and stops any running game */
    fun logout() {
        stopGame()
        currentUsername = ""
        isLoggedIn      = false
        authError       = ""
    }

    // ── Game API ──────────────────────────────────────────────────

    fun startGame() {
        resetState()
        assignedAlgorithm = listOf(
            SchedulingAlgorithm.FCFS,
            SchedulingAlgorithm.SJF,
            SchedulingAlgorithm.PRIORITY
        ).random()

        repeat((2..3).random()) { spawnProcess(arrivalTime = 0) }
        updateAvailability()

        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            while (isActive) {
                delay(100L)
                if (!isPaused && !isGameOver) tick(0.1f)
            }
        }
    }

    fun scheduleProcess(process: Process) {
        if (isGameOver || isPaused) return
        if (runningProcess != null) return
        if (waitingProcesses.none { it.pid == process.pid }) return

        val optimalPid = getOptimalPid()
        val isCorrect  = optimalPid == null || process.pid == optimalPid

        if (isCorrect) {
            correctPicks++
            score += computeScore(process)
        } else {
            wrongPicks++
            lives = maxOf(0, lives - 1)
            score = maxOf(0, score - 50)
            lastWrongPid = process.pid
            wrongFlashJob?.cancel()
            wrongFlashJob = viewModelScope.launch {
                delay(900L)
                lastWrongPid = null
            }
            if (lives == 0) {
                isGameOver = true
                saveScore()
                gameJob?.cancel()
                return
            }
        }

        waitingProcesses.removeAll { it.pid == process.pid }
        runningProcess    = process.copy(state = ProcessState.RUNNING)
        cpuTimer          = 0f
        cpuRemainingBurst = process.burstTime
        cpuBurstProgress  = 0f
    }

    fun getOptimalPid(): Int? {
        if (waitingProcesses.isEmpty()) return null
        return when (assignedAlgorithm) {
            SchedulingAlgorithm.FCFS ->
                waitingProcesses.minByOrNull { it.arrivalTime }?.pid
                    ?: waitingProcesses.minByOrNull { it.pid }?.pid
            SchedulingAlgorithm.SJF         -> waitingProcesses.minByOrNull { it.burstTime }?.pid
            SchedulingAlgorithm.PRIORITY    -> waitingProcesses.minByOrNull { it.priority }?.pid
            SchedulingAlgorithm.ROUND_ROBIN -> waitingProcesses.firstOrNull()?.pid
        }
    }

    fun dismissAlgoIntro() { showAlgoIntro = false }
    fun togglePause()       { isPaused = !isPaused }
    fun stopGame()          { gameJob?.cancel(); isGameOver = true }

    // ── Game tick ─────────────────────────────────────────────────

    private fun tick(rawDelta: Float) {
        val delta = rawDelta * GAME_SPEED
        wallTime       += delta
        spawnWallTimer += delta

        if (spawnWallTimer >= spawnWallInterval && pidCounter <= MAX_PROCESSES) {
            spawnWallTimer    = 0f
            spawnWallInterval = (20..55).random() / 10f
            val offset        = listOf(0, 0, 1, 2, 3).random()
            spawnProcess(arrivalTime = schedulingTime + offset)
            updateAvailability()
        }

        val expired = mutableListOf<Int>()
        val updated = waitingProcesses.map { p ->
            val newTimer = p.waitTimer + delta
            if (newTimer >= p.maxWaitTime) { expired.add(p.pid); p }
            else p.copy(waitTimer = newTimer)
        }
        waitingProcesses.clear()
        waitingProcesses.addAll(updated.filter { it.pid !in expired })

        if (expired.isNotEmpty()) {
            lives = maxOf(0, lives - expired.size)
            score = maxOf(0, score - 50 * expired.size)
            if (lives <= 0) {
                lives      = 0
                isGameOver = true
                saveScore()
                gameJob?.cancel()
                return
            }
        }

        runningProcess?.let { proc ->
            cpuTimer         += delta
            val remaining     = proc.burstTime - cpuTimer
            cpuRemainingBurst = remaining.toInt().coerceAtLeast(0)
            cpuBurstProgress  = (cpuTimer / proc.burstTime).coerceIn(0f, 1f)

            if (cpuTimer >= proc.burstTime.toFloat()) {
                val startT = schedulingTime
                schedulingTime += proc.burstTime
                ganttChart.add(
                    GanttEntry(proc.pid, proc.name, proc.burstTime, proc.type, startT, schedulingTime)
                )
                completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
                runningProcess    = null
                cpuTimer          = 0f
                cpuRemainingBurst = 0
                cpuBurstProgress  = 0f
                updateAvailability()
                autoAdvanceIfIdle()
                checkWinCondition()
            }
        } ?: run {
            autoAdvanceIfIdle()
            checkWinCondition()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun autoAdvanceIfIdle() {
        if (runningProcess != null || waitingProcesses.isNotEmpty()) return
        if (pendingProcesses.isEmpty()) return
        val nextArrival = pendingProcesses.minOf { it.arrivalTime }
        if (nextArrival > schedulingTime) {
            schedulingTime = nextArrival
            updateAvailability()
        }
    }

    private fun checkWinCondition() {
        if (isGameOver) return
        val allSpawned = pidCounter > MAX_PROCESSES
        val allClear   = runningProcess == null &&
                waitingProcesses.isEmpty() &&
                pendingProcesses.isEmpty()
        if (allSpawned && allClear) {
            isGameWon  = true
            isGameOver = true
            saveScore()
            gameJob?.cancel()
        }
    }

    private fun updateAvailability() {
        val nowArrived = pendingProcesses.filter { it.arrivalTime <= schedulingTime }
        if (nowArrived.isEmpty()) return
        val arrivedPids = nowArrived.map { it.pid }.toSet()
        pendingProcesses.removeAll { it.pid in arrivedPids }
        waitingProcesses.addAll(nowArrived)
    }

    private fun spawnProcess(arrivalTime: Int) {
        val type  = ProcessType.entries.random()
        val burst = when (type) {
            ProcessType.IO     -> (1..4).random()
            ProcessType.CPU    -> (3..8).random()
            ProcessType.SYSTEM -> (1..6).random()
        }
        val prio    = (1..5).random()
        val maxWait = burst * 5f + prio * 5f + 25f

        val p = Process(
            pid         = pidCounter,
            name        = "${nameParts.random()}_$pidCounter",
            burstTime   = burst,
            arrivalTime = arrivalTime,
            priority    = prio,
            type        = type,
            maxWaitTime = maxWait
        )
        pidCounter++

        if (arrivalTime <= schedulingTime) waitingProcesses.add(p)
        else                               pendingProcesses.add(p)
    }

    private fun computeScore(proc: Process): Int {
        val base          = 100
        val priorityBonus = (6 - proc.priority) * 5
        val burstBonus    = maxOf(0, (8 - proc.burstTime) * 4)
        return base + priorityBonus + burstBonus
    }

    private fun saveScore() {
        viewModelScope.launch {
            val rating = when {
                score >= 800 -> "S"
                score >= 600 -> "A"
                score >= 400 -> "B"
                score >= 200 -> "C"
                else         -> "D"
            }
            dao.insertScore(
                PlayerScore(
                    username           = currentUsername,
                    score              = score,
                    algorithm          = assignedAlgorithm.shortName,
                    completedProcesses = completedProcesses.size,
                    timeElapsed        = wallTime,
                    rating             = rating
                )
            )
        }
    }

    private fun resetState() {
        waitingProcesses.clear(); pendingProcesses.clear()
        completedProcesses.clear(); ganttChart.clear()
        runningProcess    = null
        score             = 0; lives = 3
        wallTime          = 0f; schedulingTime = 0
        isGameOver        = false; isGameWon = false
        isPaused          = false; showAlgoIntro = true
        cpuTimer          = 0f; cpuBurstProgress = 0f; cpuRemainingBurst = 0
        spawnWallTimer    = 0f; spawnWallInterval = 2.5f
        pidCounter        = 1; correctPicks = 0; wrongPicks = 0
        lastWrongPid      = null
    }

    override fun onCleared() { super.onCleared(); gameJob?.cancel() }
}