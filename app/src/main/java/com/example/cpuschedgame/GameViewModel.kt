package com.example.cpuschedgame

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.security.MessageDigest

// Extends AndroidViewModel (Branch 1) so the Room database and Application
// context are always available without leaking a Context reference.
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // ── Database (Branch 1) ───────────────────────────────────────
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.playerScoreDao()
    private val userDao = db.userDao()

    val highScore = dao.getHighScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    val topScores = dao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // ── Auth state (Branch 1) ─────────────────────────────────────
    var currentUsername by mutableStateOf(""); private set
    var authError by mutableStateOf(""); private set
    var isLoggedIn by mutableStateOf(false); private set

    // ── Algorithm & Level (Branch 2) ──────────────────────────────
    // Nullable so it is clear no algorithm is chosen before startGame()
    var assignedAlgorithm by mutableStateOf<SchedulingAlgorithm?>(null)
        private set
    var selectedLevel by mutableStateOf(Level.Easy)
        private set

    // ── Clocks ────────────────────────────────────────────────────
    /** Discrete scheduling clock — jumps in Easy, advances continuously in Medium/Hard. */
    var schedulingTime by mutableIntStateOf(0); private set
    /** Real wall-clock used for animations and spawn timing. */
    var wallTime by mutableFloatStateOf(0f); private set

    // ── Process lists ─────────────────────────────────────────────
    var waitingProcesses = mutableStateListOf<Process>(); private set
    var pendingProcesses = mutableStateListOf<Process>(); private set
    var runningProcess by mutableStateOf<Process?>(null); private set
    var completedProcesses = mutableStateListOf<Process>(); private set
    var ganttChart = mutableStateListOf<GanttEntry>(); private set

    // ── Game state ────────────────────────────────────────────────
    var currLevelNo by mutableStateOf(1); private set
    var score by mutableIntStateOf(0); private set
    var lives by mutableIntStateOf(3); private set
    var hasGameStarted by mutableStateOf(false); private set
    var isGameOver by mutableStateOf(false); private set
    var isGameWon by mutableStateOf(false); private set
    var isPaused by mutableStateOf(false); private set
    var showAlgoIntro by mutableStateOf(true); private set
    var cpuBurstProgress by mutableFloatStateOf(0f); private set
    var cpuRemainingBurst by mutableIntStateOf(0); private set
    var correctPicks by mutableIntStateOf(0); private set
    var wrongPicks by mutableIntStateOf(0); private set
    /** PID of last wrong-picked process, cleared after a short window. */
    var lastWrongPid by mutableStateOf<Int?>(null); private set

    // ── Private fields ────────────────────────────────────────────
    private val MAX_PROCESSES = 10
    private val GAME_SPEED = 0.4f   // 1.0 = real-time, 0.4 = 2.5× slower
    private var cpuTimer = 0f       // seconds elapsed since current process started
    private var spawnWallTimer = 0f
    private var spawnWallInterval = 2.5f
    private var pidCounter = 1
    private var gameJob: Job? = null
    private var wrongFlashJob: Job? = null
    /** schedulingTime snapshot when the current process was dispatched. */
    private var schedulingCpuStart = 0

    private val nameParts = listOf(
        "calc", "browser", "mail", "media", "net",
        "disk", "ui", "db", "auth", "kernel", "svc", "daemon"
    )

    // ─────────────────────────────────────────────────────────────
    // AUTH  (Branch 1)
    // ─────────────────────────────────────────────────────────────

    /** SHA-256 hash — plain-text passwords are never stored. */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Clear any leftover auth error when switching login ↔ signup. */
    fun clearAuthError() { authError = "" }

    /**
     * Validate input, check uniqueness, persist new user.
     * Does NOT set isLoggedIn — player must log in manually.
     */
    fun signup(username: String, password: String, onSuccess: () -> Unit) {
        authError = ""
        if (username.isBlank()) { authError = "Username cannot be empty."; return }
        if (username.length < 3) { authError = "Username must be at least 3 characters."; return }
        if (password.length < 6) { authError = "Password must be at least 6 characters."; return }

        viewModelScope.launch {
            val exists = userDao.usernameExists(username.trim())
            if (exists > 0) {
                authError = "Username \"${username.trim()}\" is already taken."
                return@launch
            }
            userDao.insertUser(
                User(username = username.trim(), password = hashPassword(password))
            )
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    /** Find user, compare hashed password, set session on success. */
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

    /** Logout — clears session and stops any running game. */
    fun logout() {
        stopGame()
        currentUsername = ""
        isLoggedIn = false
        authError = ""
    }

    // ─────────────────────────────────────────────────────────────
    // GAME API  (Branch 2 logic)
    // ─────────────────────────────────────────────────────────────

    fun startGame(level: Level, forcedAlgorithm: SchedulingAlgorithm? = null) {
        selectedLevel = level
        resetState()
        hasGameStarted = true

        val easyAlgorithms = listOf(
            SchedulingAlgorithm.FCFS,
            SchedulingAlgorithm.SJF_NP,
            SchedulingAlgorithm.PRIORITY_NP
        )
        val mediumAlgorithms = listOf(
            SchedulingAlgorithm.ROUND_ROBIN,
            SchedulingAlgorithm.SJF_P,
            SchedulingAlgorithm.PRIORITY_P
        )
        val hardAlgorithms = listOf(
            SchedulingAlgorithm.FCFS_Priority,
            SchedulingAlgorithm.FCFS_SJF,
            SchedulingAlgorithm.Priority_SJF,
            SchedulingAlgorithm.SJF_Priority
        )

        assignedAlgorithm = forcedAlgorithm ?: when (level) {
            Level.Easy   -> easyAlgorithms.random()
            Level.Medium -> mediumAlgorithms.random()
            Level.Hard   -> hardAlgorithms.random()
        }

        // Seed 2-3 processes that arrive at time 0
        repeat((2..3).random()) { spawnProcess(arrivalTime = 0) }
        updateAvailability()

        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            while (isActive && hasGameStarted) {
                delay(100L)
                if (!isPaused && !isGameOver && !showAlgoIntro) tick(0.1f)
            }
        }
    }

    /**
     * Player taps a process card.
     *
     * Easy / non-preemptive: tap ignored while CPU is busy.
     * Medium & Hard / preemptive: running process is preempted and the
     * tapped process starts immediately if the pick is correct.
     */
    fun scheduleProcess(process: Process, context: Context) {
        if (isGameOver || isPaused || showAlgoIntro || !hasGameStarted) return
        if (waitingProcesses.none { it.pid == process.pid }) return

        val isPreemptive = isPreemptiveLevel()

        // Block scheduling while CPU is busy for non-preemptive modes
        if (!isPreemptive && runningProcess != null) return

        val optimalPid = getOptimalPid()
        val isCorrect = optimalPid == null || process.pid == optimalPid

        if (isCorrect) {
            playSound(context, R.raw.correct)
            correctPicks++
            score += computeScore(process)

            // Preempt the running process before dispatching the new one
            if (isPreemptive && runningProcess != null) {
                preemptRunningProcess()
            }

            waitingProcesses.removeAll { it.pid == process.pid }
            runningProcess = process.copy(state = ProcessState.RUNNING)
            cpuTimer = 0f
            cpuRemainingBurst = process.burstTime
            cpuBurstProgress = 0f
            schedulingCpuStart = schedulingTime
        } else {
            playSound(context, R.raw.wrong)
            wrongPicks++
            lives = maxOf(0, lives - 1)
            score = maxOf(0, score - 50)
            lastWrongPid = process.pid
            wrongFlashJob?.cancel()
            wrongFlashJob = viewModelScope.launch {
                delay(500L)
                lastWrongPid = null
            }
            if (lives == 0) {
                isGameOver = true
                saveScore()
                gameJob?.cancel()
            }
        }
    }

    /** Play a one-shot sound effect and release the MediaPlayer automatically. */
    fun playSound(context: Context, soundRes: Int) {
        val mp = MediaPlayer.create(context, soundRes)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    }

    /** Returns the PID the current algorithm considers optimal, or null if queue is empty. */
    fun getOptimalPid(): Int? {
        if (waitingProcesses.isEmpty()) return null
        return when (assignedAlgorithm) {
            SchedulingAlgorithm.FCFS ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.arrivalTime }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.SJF_NP,
            SchedulingAlgorithm.SJF_P ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.burstTime }
                        .thenBy { it.arrivalTime }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.PRIORITY_NP,
            SchedulingAlgorithm.PRIORITY_P ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.priority }
                        .thenBy { it.arrivalTime }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.ROUND_ROBIN ->
                waitingProcesses.firstOrNull()?.pid

            SchedulingAlgorithm.FCFS_SJF ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.arrivalTime }
                        .thenBy { it.burstTime }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.FCFS_Priority ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.arrivalTime }
                        .thenBy { it.priority }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.SJF_Priority ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.burstTime }
                        .thenBy { it.priority }.thenBy { it.arrivalTime }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.Priority_SJF ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.priority }
                        .thenBy { it.burstTime }.thenBy { it.arrivalTime }.thenBy { it.pid })?.pid

            null -> null
        }
    }

    fun dismissAlgoIntro() { showAlgoIntro = false }
    fun togglePause() { isPaused = !isPaused }
    fun stopGame() { gameJob?.cancel(); isGameOver = true }

    /** Advance to the next level number (tracked across sessions in the same run). */
    fun incrementLevel() { currLevelNo++ }

    /** Reset level counter when the player returns home. */
    fun resetLevel() { currLevelNo = 1 }

    /** Replay the current level with the same algorithm. */
    fun replayLevel() {
        val algo = assignedAlgorithm
        startGame(selectedLevel, forcedAlgorithm = algo)
    }

    // ─────────────────────────────────────────────────────────────
    // GAME TICK  (Branch 2 logic)
    // ─────────────────────────────────────────────────────────────

    /** True for Medium and Hard levels, which use preemptive scheduling. */
    private fun isPreemptiveLevel(): Boolean =
        selectedLevel == Level.Medium || selectedLevel == Level.Hard

    private fun tick(rawDelta: Float) {
        val delta = rawDelta * GAME_SPEED
        wallTime += delta
        spawnWallTimer += delta

        // ── Spawn processes on wall-clock interval ─────────────────
        if (spawnWallTimer >= spawnWallInterval && pidCounter <= MAX_PROCESSES) {
            spawnWallTimer = 0f
            spawnWallInterval = (20..55).random() / 10f
            val offset = listOf(0, 0, 1, 2, 3).random()
            spawnProcess(arrivalTime = schedulingTime + offset)
            updateAvailability()
        }

        // ── Expire overdue waiting processes ───────────────────────
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
                lives = 0
                isGameOver = true
                saveScore()
                gameJob?.cancel()
                return
            }
        }

        // ── CPU execution ──────────────────────────────────────────
        runningProcess?.let { proc ->
            cpuTimer += delta

            // Preemptive (Medium/Hard): scheduling clock advances continuously
            if (isPreemptiveLevel()) {
                val elapsed = cpuTimer.toInt()
                val newSchedTime = schedulingCpuStart + elapsed
                if (newSchedTime > schedulingTime) {
                    val nextArrival = pendingProcesses.minOfOrNull { it.arrivalTime }
                    schedulingTime = newSchedTime

                    // Auto-preempt when the clock crosses a pending arrival
                    val shouldAutoPreempt = nextArrival != null
                            && schedulingTime >= nextArrival
                            && elapsed < proc.burstTime
                    if (shouldAutoPreempt) {
                        val executedUnits = (nextArrival!! - schedulingCpuStart)
                            .coerceIn(0, proc.burstTime)
                        schedulingTime = schedulingCpuStart + executedUnits
                        preemptRunningProcessWithUnits(proc, executedUnits)
                        updateAvailability()
                        return
                    }
                    updateAvailability()
                }
            }

            val remaining = proc.burstTime - cpuTimer
            cpuRemainingBurst = remaining.toInt().coerceAtLeast(0)
            cpuBurstProgress = (cpuTimer / proc.burstTime).coerceIn(0f, 1f)

            // ── Process finishes ───────────────────────────────────
            if (cpuTimer >= proc.burstTime.toFloat()) {
                val executedUnits = proc.burstTime
                val endTime = schedulingCpuStart + executedUnits

                if (!isPreemptiveLevel()) {
                    // Easy: clock jumps forward on completion
                    schedulingTime += executedUnits
                } else {
                    // Medium/Hard: clock was already advanced continuously
                    schedulingTime = endTime
                }

                ganttChart.add(
                    GanttEntry(
                        pid = proc.pid, name = proc.name,
                        duration = executedUnits, type = proc.type,
                        startTime = schedulingCpuStart, endTime = endTime
                    )
                )
                completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
                runningProcess = null
                cpuTimer = 0f; cpuRemainingBurst = 0; cpuBurstProgress = 0f
                updateAvailability()
                autoAdvanceIfIdle()
                checkWinCondition()
            }
        } ?: run {
            autoAdvanceIfIdle()
            checkWinCondition()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS  (Branch 2)
    // ─────────────────────────────────────────────────────────────

    /**
     * Preempts the running process at its current cpuTimer boundary.
     * Adds a partial Gantt entry and returns the process to the ready queue
     * with its remaining burst time.
     */
    private fun preemptRunningProcess() {
        val proc = runningProcess ?: return
        val executedUnits = cpuTimer.toInt().coerceAtMost(proc.burstTime)
        preemptRunningProcessWithUnits(proc, executedUnits)
    }

    /** Core preemption logic given the number of units already executed. */
    private fun preemptRunningProcessWithUnits(proc: Process, executedUnits: Int) {
        val remainingBurst = (proc.burstTime - executedUnits).coerceAtLeast(0)
        val endTime = schedulingCpuStart + executedUnits

        if (executedUnits > 0) {
            ganttChart.add(
                GanttEntry(
                    pid = proc.pid, name = proc.name,
                    duration = executedUnits, type = proc.type,
                    startTime = schedulingCpuStart, endTime = endTime
                )
            )
        }
        schedulingTime = endTime

        if (remainingBurst > 0) {
            // Return to ready queue with fresh expiry timer
            waitingProcesses.add(
                proc.copy(
                    burstTime = remainingBurst,
                    state = ProcessState.WAITING,
                    waitTimer = 0f
                )
            )
        } else {
            // Finished exactly on preemption boundary
            completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
        }

        runningProcess = null
        cpuTimer = 0f; cpuRemainingBurst = 0; cpuBurstProgress = 0f
    }

    /**
     * If CPU is idle with no waiting processes but some are pending,
     * jump the scheduling clock to the next arrival time (idle gap).
     */
    private fun autoAdvanceIfIdle() {
        if (runningProcess != null || waitingProcesses.isNotEmpty()) return
        if (pendingProcesses.isEmpty()) return
        val nextArrival = pendingProcesses.minOf { it.arrivalTime }
        if (nextArrival > schedulingTime) {
            schedulingTime = nextArrival
            updateAvailability()
        }
    }

    /** Win when all processes are spawned, completed, and CPU is idle. */
    private fun checkWinCondition() {
        if (isGameOver) return
        val allSpawned = pidCounter > MAX_PROCESSES
        val allClear = runningProcess == null
                && waitingProcesses.isEmpty()
                && pendingProcesses.isEmpty()
        if (allSpawned && allClear) {
            val totalPicks = correctPicks + wrongPicks
            val accuracy = if (totalPicks > 0) (correctPicks * 100 / totalPicks) else 0
            isGameWon = accuracy >= 80   // win only if accuracy meets threshold
            isGameOver = true
            saveScore()
            gameJob?.cancel()
        }
    }

    /** Move pending processes whose arrivalTime ≤ schedulingTime into the waiting queue. */
    private fun updateAvailability() {
        val nowArrived = pendingProcesses.filter { it.arrivalTime <= schedulingTime }
        if (nowArrived.isEmpty()) return
        val arrivedPids = nowArrived.map { it.pid }.toSet()
        pendingProcesses.removeAll { it.pid in arrivedPids }
        waitingProcesses.addAll(nowArrived)
    }

    private fun spawnProcess(arrivalTime: Int) {
        val type = ProcessType.entries.random()
        val burst = when (type) {
            ProcessType.IO     -> (1..4).random()
            ProcessType.CPU    -> (3..8).random()
            ProcessType.SYSTEM -> (1..6).random()
        }
        val prio = (1..5).random()
        val maxWait = burst * 5f + prio * 5f + 25f
        val p = Process(
            pid = pidCounter,
            name = "${nameParts.random()}_$pidCounter",
            burstTime = burst,
            arrivalTime = arrivalTime,
            priority = prio,
            type = type,
            maxWaitTime = maxWait
        )
        pidCounter++
        if (arrivalTime <= schedulingTime) waitingProcesses.add(p)
        else pendingProcesses.add(p)
    }

    private fun computeScore(proc: Process): Int {
        val base = 100
        val priorityBonus = (6 - proc.priority) * 5
        val burstBonus = maxOf(0, (8 - proc.burstTime) * 4)
        return base + priorityBonus + burstBonus
    }

    /** Persist the current score to Room using Branch 1's rating logic (accuracy-aware). */
    private fun saveScore() {
        viewModelScope.launch {
            val totalPicks = correctPicks + wrongPicks
            val accuracy = if (totalPicks > 0) (correctPicks * 100 / totalPicks) else 0
            val rating = when {
                accuracy >= 90 && score >= 600 -> "S"
                accuracy >= 75 || score >= 450 -> "A"
                accuracy >= 60 || score >= 300 -> "B"
                accuracy >= 40 || score >= 150 -> "C"
                else -> "D"
            }
            dao.insertScore(
                PlayerScore(
                    username = currentUsername,
                    score = score,
                    algorithm = assignedAlgorithm?.shortName ?: "?",
                    completedProcesses = completedProcesses.size,
                    timeElapsed = wallTime,
                    rating = rating
                )
            )
        }
    }

    private fun resetState() {
        waitingProcesses.clear(); pendingProcesses.clear()
        completedProcesses.clear(); ganttChart.clear()
        runningProcess = null
        score = 0; lives = 3
        wallTime = 0f; schedulingTime = 0
        hasGameStarted = false
        isGameOver = false; isGameWon = false; isPaused = false; showAlgoIntro = true
        cpuTimer = 0f; cpuBurstProgress = 0f; cpuRemainingBurst = 0
        spawnWallTimer = 0f; spawnWallInterval = 2.5f
        pidCounter = 1; correctPicks = 0; wrongPicks = 0
        lastWrongPid = null
        schedulingCpuStart = 0
    }

    override fun onCleared() { super.onCleared(); gameJob?.cancel() }
}