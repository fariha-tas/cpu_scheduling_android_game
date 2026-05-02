package com.example.cpuschedgame

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.security.MessageDigest

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // ── Database ──────────────────────────────────────────────────
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.playerScoreDao()
    private val userDao = db.userDao()

    val highScore = dao.getHighScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    val topScores = dao.getTopScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // ── Auth state ────────────────────────────────────────────────
    var currentUsername by mutableStateOf(""); private set
    var authError by mutableStateOf(""); private set
    var isLoggedIn by mutableStateOf(false); private set

    // ── Algorithm & Level ─────────────────────────────────────────
    var assignedAlgorithm by mutableStateOf<SchedulingAlgorithm?>(null); private set
    var selectedLevel by mutableStateOf(Level.Easy); private set

    // ── Clocks ────────────────────────────────────────────────────
    var schedulingTime by mutableIntStateOf(0); private set
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
    var lastWrongPid by mutableStateOf<Int?>(null); private set
    /** True when a preemptive time-unit has just completed and we're waiting for user to decide */
    var awaitingPreemptDecision by mutableStateOf(false); private set
    /** How many time units the current running process has executed so far */
    var unitsExecutedThisBurst by mutableIntStateOf(0); private set

    // ── Private fields ────────────────────────────────────────────
    private val MAX_PROCESSES = 10
    private val GAME_SPEED = 0.4f
    private var cpuTimer = 0f
    private var spawnWallTimer = 0f
    private var spawnWallInterval = 2.5f
    private var pidCounter = 1
    private var gameJob: Job? = null
    private var wrongFlashJob: Job? = null
    private var schedulingCpuStart = 0
    /** Round Robin queue — tracks order processes should be served */
    private val rrQueue = mutableListOf<Int>()   // holds PIDs in RR order

    private val nameParts = listOf(
        "calc", "browser", "mail", "media", "net",
        "disk", "ui", "db", "auth", "kernel", "svc", "daemon"
    )

    // ── Algorithm groups per level ────────────────────────────────
    private val easyAlgorithms = listOf(
        SchedulingAlgorithm.FCFS,
        SchedulingAlgorithm.SJF_NP,
        SchedulingAlgorithm.PRIORITY_NP
    )

    // Medium = preemptive (per time-unit decision): SJF-P, PRIORITY-P, RR
    private val mediumAlgorithms = listOf(
        SchedulingAlgorithm.SJF_P,
        SchedulingAlgorithm.PRIORITY_P,
        SchedulingAlgorithm.ROUND_ROBIN
    )

    // Hard = hybrid preemptive (primary + tie-break key), per-time-unit decision
    private val hardAlgorithms = listOf(
        SchedulingAlgorithm.SJF_Priority,
        SchedulingAlgorithm.Priority_SJF,
        SchedulingAlgorithm.RR_SJF,
        SchedulingAlgorithm.RR_Priority
    )

    // ─────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun clearAuthError() { authError = "" }

    fun signup(username: String, password: String, onSuccess: () -> Unit) {
        authError = ""
        if (username.isBlank()) { authError = "Username cannot be empty."; return }
        if (username.length < 3) { authError = "Username must be at least 3 characters."; return }
        if (password.length < 6) { authError = "Password must be at least 6 characters."; return }
        viewModelScope.launch {
            val exists = userDao.usernameExists(username.trim())
            if (exists > 0) {
                authError = "Username \"${username.trim()}\" is already taken."; return@launch
            }
            userDao.insertUser(User(username = username.trim(), password = hashPassword(password)))
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        authError = ""
        if (username.isBlank()) { authError = "Please enter your username."; return }
        if (password.isBlank()) { authError = "Please enter your password."; return }
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username.trim())
            when {
                user == null -> authError = "No account found for \"${username.trim()}\"."
                user.password != hashPassword(password) -> authError = "Incorrect password."
                else -> {
                    currentUsername = user.username
                    isLoggedIn = true
                    withContext(Dispatchers.Main) { onSuccess() }
                }
            }
        }
    }

    fun logout() {
        stopGame()
        currentUsername = ""
        isLoggedIn = false
        authError = ""
    }

    // ─────────────────────────────────────────────────────────────
    // PROFILE helpers
    // ─────────────────────────────────────────────────────────────

    /** Best score the current user achieved on a specific level. */
    fun bestScoreForLevel(level: Level): Flow<Int?> =
        dao.getBestScoreForUserAndLevel(currentUsername, level.name)

    /** Total games played by the current user. */
    fun totalGamesForUser(): Flow<Int> =
        dao.getGameCountForUser(currentUsername)

    /** Recent scores for the current user. */
    fun recentScoresForUser(): Flow<List<PlayerScore>> =
        dao.getScoresForUser(currentUsername)

    // ─────────────────────────────────────────────────────────────
    // GAME API
    // ─────────────────────────────────────────────────────────────

    fun startGame(level: Level, forcedAlgorithm: SchedulingAlgorithm? = null) {
        selectedLevel = level
        resetState()
        hasGameStarted = true

        assignedAlgorithm = forcedAlgorithm ?: when (level) {
            Level.Easy   -> easyAlgorithms.random()
            Level.Medium -> mediumAlgorithms.random()
            Level.Hard   -> hardAlgorithms.random()
        }

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
     * Player taps a process card (from waiting queue) OR taps the currently running process
     * (to continue it for another time unit on preemptive levels).
     *
     * Non-preemptive (Easy): tap ignored while CPU is busy.
     * Preemptive (Medium/Hard): after each time unit the game pauses for a user decision.
     *   - Tap a WAITING card  → preempt/switch to that process (correct if it's the optimal pick)
     *   - Tap the RUNNING card (pid == runningProcess.pid) → continue it (correct if it IS optimal)
     */
    fun scheduleProcess(process: Process, context: Context) {
        if (isGameOver || isPaused || showAlgoIntro || !hasGameStarted) return
        val isPreemptive = isPreemptiveLevel()

        // Non-preemptive: ignore taps while CPU busy
        if (!isPreemptive && runningProcess != null) return

        val currentRunning = runningProcess

        // On preemptive levels determine if the user tapped the currently running process
        // (meaning "keep it going for another unit")
        val tappedRunning = isPreemptive
                && currentRunning != null
                && process.pid == currentRunning.pid

        if (tappedRunning) {
            // "Continue" tap — valid only when we are awaiting a decision
            if (!awaitingPreemptDecision) return
            val optimalPid = getOptimalPid(includingRunning = true)
            val isCorrect = optimalPid == null || currentRunning!!.pid == optimalPid
            if (isCorrect) {
                playSound(context, R.raw.correct)
                correctPicks++
                score += 10   // small bonus for a correct "continue" decision
            } else {
                playSound(context, R.raw.wrong)
                wrongPicks++
                lives = maxOf(0, lives - 1)
                score = maxOf(0, score - 50)
                lastWrongPid = currentRunning!!.pid
                wrongFlashJob?.cancel()
                wrongFlashJob = viewModelScope.launch {
                    delay(500L)
                    lastWrongPid = null
                }
                if (lives == 0) {
                    isGameOver = true; saveScore(); gameJob?.cancel(); return
                }
            }
            // Resume executing the same process
            awaitingPreemptDecision = false
            return
        }

        // Tapped a waiting process card
        if (waitingProcesses.none { it.pid == process.pid }) return

        // On preemptive levels we only accept a switch tap while awaiting decision
        // (or when CPU is idle — always allowed)
        if (isPreemptive && currentRunning != null && !awaitingPreemptDecision) return

        val optimalPid = getOptimalPid(includingRunning = true)
        val isCorrect = optimalPid == null || process.pid == optimalPid

        if (isCorrect) {
            playSound(context, R.raw.correct)
            correctPicks++
            score += computeScore(process)

            if (isPreemptive && currentRunning != null) {
                preemptRunningProcess()
            }

            waitingProcesses.removeAll { it.pid == process.pid }
            runningProcess = process.copy(state = ProcessState.RUNNING)
            cpuTimer = 0f
            unitsExecutedThisBurst = 0
            cpuRemainingBurst = process.burstTime
            cpuBurstProgress = 0f
            schedulingCpuStart = schedulingTime
            awaitingPreemptDecision = false

            // RR: move this pid to the back of the RR queue
            if (isRRBased()) {
                rrQueue.remove(process.pid)
                rrQueue.add(process.pid)
            }
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
                isGameOver = true; saveScore(); gameJob?.cancel()
            }
        }
    }

    fun playSound(context: Context, soundRes: Int) {
        val mp = MediaPlayer.create(context, soundRes)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    }

    /**
     * Returns the PID of the process that the current algorithm says should run next.
     * [includingRunning] — if true, the currently running process is included in the
     * candidate pool (used for preemptive per-unit decisions so the player can be told
     * whether "continue" or "switch" is optimal).
     */
    fun getOptimalPid(includingRunning: Boolean = false): Int? {
        // Build the candidate pool
        val candidates: List<Process> = if (includingRunning && runningProcess != null) {
            waitingProcesses + runningProcess!!
        } else {
            waitingProcesses.toList()
        }
        if (candidates.isEmpty()) return null

        return when (assignedAlgorithm) {
            SchedulingAlgorithm.FCFS ->
                candidates.minWithOrNull(compareBy<Process> { it.arrivalTime }.thenBy { it.pid })?.pid

            SchedulingAlgorithm.SJF_NP,
            SchedulingAlgorithm.SJF_P ->
                candidates.minWithOrNull(
                    compareBy<Process> { it.burstTime }.thenBy { it.arrivalTime }.thenBy { it.pid }
                )?.pid

            SchedulingAlgorithm.PRIORITY_NP,
            SchedulingAlgorithm.PRIORITY_P ->
                candidates.minWithOrNull(
                    compareBy<Process> { it.priority }.thenBy { it.arrivalTime }.thenBy { it.pid }
                )?.pid

            SchedulingAlgorithm.ROUND_ROBIN -> {
                // For RR the optimal pick is the FIRST pid in rrQueue that exists in candidates
                val candidatePids = candidates.map { it.pid }.toSet()
                val nextRR = rrQueue.firstOrNull { it in candidatePids }
                // If nothing in rrQueue matches, take earliest arrival
                nextRR ?: candidates.minByOrNull { it.arrivalTime }?.pid
            }

            SchedulingAlgorithm.SJF_Priority ->
                candidates.minWithOrNull(
                    compareBy<Process> { it.burstTime }.thenBy { it.priority }
                        .thenBy { it.arrivalTime }.thenBy { it.pid }
                )?.pid

            SchedulingAlgorithm.Priority_SJF ->
                candidates.minWithOrNull(
                    compareBy<Process> { it.priority }.thenBy { it.burstTime }
                        .thenBy { it.arrivalTime }.thenBy { it.pid }
                )?.pid

            SchedulingAlgorithm.RR_SJF -> {
                // RR order first; among tied-next candidates pick smallest BT
                val candidatePids = candidates.map { it.pid }.toSet()
                val nextRRIdx = rrQueue.indexOfFirst { it in candidatePids }
                if (nextRRIdx < 0) {
                    candidates.minWithOrNull(compareBy { it.burstTime })?.pid
                } else {
                    // Collect all that would be "next" in RR (same queue position ties)
                    val nextPid = rrQueue[nextRRIdx]
                    // In practice RR has a single next; use BT as direct tie-break on the pool
                    candidates.filter { it.pid == nextPid }
                        .minWithOrNull(compareBy { it.burstTime })?.pid
                        ?: candidates.minWithOrNull(
                            compareBy<Process> { it.burstTime }.thenBy { it.arrivalTime }
                        )?.pid
                }
            }

            SchedulingAlgorithm.RR_Priority -> {
                val candidatePids = candidates.map { it.pid }.toSet()
                val nextRRIdx = rrQueue.indexOfFirst { it in candidatePids }
                if (nextRRIdx < 0) {
                    candidates.minWithOrNull(compareBy { it.priority })?.pid
                } else {
                    val nextPid = rrQueue[nextRRIdx]
                    candidates.filter { it.pid == nextPid }
                        .minWithOrNull(compareBy { it.priority })?.pid
                        ?: candidates.minWithOrNull(
                            compareBy<Process> { it.priority }.thenBy { it.arrivalTime }
                        )?.pid
                }
            }

            null -> null
        }
    }

    /** True if the assigned algorithm uses Round Robin as primary key */
    fun isRRBased(): Boolean = assignedAlgorithm == SchedulingAlgorithm.ROUND_ROBIN
            || assignedAlgorithm == SchedulingAlgorithm.RR_SJF
            || assignedAlgorithm == SchedulingAlgorithm.RR_Priority

    fun dismissAlgoIntro() { showAlgoIntro = false }
    fun togglePause() { isPaused = !isPaused }
    fun stopGame() { gameJob?.cancel(); isGameOver = true }
    fun incrementLevel() { currLevelNo++ }
    fun resetLevel() { currLevelNo = 1 }

    fun replayLevel() {
        val algo = assignedAlgorithm
        startGame(selectedLevel, forcedAlgorithm = algo)
    }

    // ─────────────────────────────────────────────────────────────
    // GAME TICK
    // ─────────────────────────────────────────────────────────────

    fun isPreemptiveLevel(): Boolean =
        selectedLevel == Level.Medium || selectedLevel == Level.Hard

    private fun tick(rawDelta: Float) {
        val delta = rawDelta * GAME_SPEED
        wallTime += delta

        // While awaiting preemptive decision only expiry timers tick
        if (awaitingPreemptDecision) {
            tickExpiryOnly(delta)
            return
        }

        spawnWallTimer += delta
        if (spawnWallTimer >= spawnWallInterval && pidCounter <= MAX_PROCESSES) {
            spawnWallTimer = 0f
            spawnWallInterval = (20..55).random() / 10f
            val offset = listOf(0, 0, 1, 2, 3).random()
            spawnProcess(arrivalTime = schedulingTime + offset)
            updateAvailability()
        }

        tickExpiryOnly(delta)
        if (isGameOver) return

        runningProcess?.let { proc ->
            cpuTimer += delta

            if (isPreemptiveLevel()) {
                val unitsDone = cpuTimer.toInt()
                if (unitsDone > unitsExecutedThisBurst) {
                    unitsExecutedThisBurst = unitsDone
                    schedulingTime = schedulingCpuStart + unitsDone
                    updateAvailability()

                    cpuRemainingBurst = (proc.burstTime - unitsDone).coerceAtLeast(0)
                    cpuBurstProgress = (unitsDone.toFloat() / proc.burstTime).coerceIn(0f, 1f)

                    if (unitsDone >= proc.burstTime) {
                        finishRunningProcess(proc, unitsDone)
                        autoAdvanceIfIdle()
                        checkWinCondition()
                    } else {
                        // For RR-based: mandatory rotation after 1 unit
                        if (isRRBased()) {
                            val endTime = schedulingCpuStart + unitsDone
                            ganttChart.add(
                                GanttEntry(
                                    pid = proc.pid, name = proc.name,
                                    duration = 1, type = proc.type,
                                    startTime = endTime - 1, endTime = endTime
                                )
                            )
                            schedulingTime = endTime
                            val remainingBurst = (proc.burstTime - unitsDone).coerceAtLeast(0)
                            rrQueue.remove(proc.pid)
                            if (remainingBurst > 0) {
                                rrQueue.add(proc.pid)
                                waitingProcesses.add(
                                    proc.copy(
                                        burstTime = remainingBurst,
                                        state = ProcessState.WAITING,
                                        waitTimer = 0f
                                    )
                                )
                            } else {
                                completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
                            }
                            runningProcess = null
                            cpuTimer = 0f; cpuRemainingBurst = 0; cpuBurstProgress = 0f
                            unitsExecutedThisBurst = 0
                            updateAvailability()
                        }
                        // Pause for user decision (continue or switch)
                        awaitingPreemptDecision = true
                    }
                } else {
                    val fracInUnit = cpuTimer - unitsDone.toFloat()
                    cpuBurstProgress = ((unitsDone + fracInUnit) / proc.burstTime).coerceIn(0f, 1f)
                    cpuRemainingBurst = (proc.burstTime - cpuTimer).toInt().coerceAtLeast(0)
                }
            } else {
                // Non-preemptive: run to completion
                cpuRemainingBurst = (proc.burstTime - cpuTimer).toInt().coerceAtLeast(0)
                cpuBurstProgress = (cpuTimer / proc.burstTime).coerceIn(0f, 1f)
                if (cpuTimer >= proc.burstTime.toFloat()) {
                    schedulingTime += proc.burstTime
                    ganttChart.add(
                        GanttEntry(
                            pid = proc.pid, name = proc.name,
                            duration = proc.burstTime, type = proc.type,
                            startTime = schedulingCpuStart,
                            endTime = schedulingCpuStart + proc.burstTime
                        )
                    )
                    completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
                    runningProcess = null
                    cpuTimer = 0f; cpuRemainingBurst = 0; cpuBurstProgress = 0f
                    updateAvailability()
                    autoAdvanceIfIdle()
                    checkWinCondition()
                }
            }
        } ?: run {
            autoAdvanceIfIdle()
            checkWinCondition()
        }
    }

    private fun tickExpiryOnly(delta: Float) {
        val expired = mutableListOf<Int>()
        val updated = waitingProcesses.map { p ->
            val newTimer = p.waitTimer + delta
            if (newTimer >= p.maxWaitTime) { expired.add(p.pid); p }
            else p.copy(waitTimer = newTimer)
        }
        waitingProcesses.clear()
        waitingProcesses.addAll(updated.filter { it.pid !in expired })
        if (expired.isNotEmpty()) {
            expired.forEach { rrQueue.remove(it) }
            lives = maxOf(0, lives - expired.size)
            score = maxOf(0, score - 50 * expired.size)
            if (lives <= 0) {
                lives = 0; isGameOver = true; saveScore(); gameJob?.cancel()
            }
        }
    }

    private fun finishRunningProcess(proc: Process, executedUnits: Int) {
        val endTime = schedulingCpuStart + executedUnits
        schedulingTime = endTime
        ganttChart.add(
            GanttEntry(
                pid = proc.pid, name = proc.name,
                duration = executedUnits, type = proc.type,
                startTime = schedulingCpuStart, endTime = endTime
            )
        )
        completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
        rrQueue.remove(proc.pid)
        runningProcess = null
        cpuTimer = 0f; cpuRemainingBurst = 0; cpuBurstProgress = 0f
        unitsExecutedThisBurst = 0
        awaitingPreemptDecision = false
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun preemptRunningProcess() {
        val proc = runningProcess ?: return
        val executedUnits = unitsExecutedThisBurst.coerceAtMost(proc.burstTime)
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
            waitingProcesses.add(
                proc.copy(burstTime = remainingBurst, state = ProcessState.WAITING, waitTimer = 0f)
            )
        } else {
            completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
            rrQueue.remove(proc.pid)
        }
        runningProcess = null
        cpuTimer = 0f; cpuRemainingBurst = 0; cpuBurstProgress = 0f
        unitsExecutedThisBurst = 0
        awaitingPreemptDecision = false
    }

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
        val allClear = runningProcess == null
                && waitingProcesses.isEmpty()
                && pendingProcesses.isEmpty()
        if (allSpawned && allClear) {
            val totalPicks = correctPicks + wrongPicks
            val accuracy = if (totalPicks > 0) (correctPicks * 100 / totalPicks) else 0
            isGameWon = accuracy >= 80
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
        // Register new arrivals at the back of the RR queue
        nowArrived.forEach { p -> if (p.pid !in rrQueue) rrQueue.add(p.pid) }
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
        if (arrivalTime <= schedulingTime) {
            waitingProcesses.add(p)
            if (p.pid !in rrQueue) rrQueue.add(p.pid)
        } else {
            pendingProcesses.add(p)
        }
    }

    private fun computeScore(proc: Process): Int {
        val base = 100
        val priorityBonus = (6 - proc.priority) * 5
        val burstBonus = maxOf(0, (8 - proc.burstTime) * 4)
        return base + priorityBonus + burstBonus
    }

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
                    level = selectedLevel.name,
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
        lastWrongPid = null; schedulingCpuStart = 0
        awaitingPreemptDecision = false; unitsExecutedThisBurst = 0
        rrQueue.clear()
    }

    override fun onCleared() { super.onCleared(); gameJob?.cancel() }
}