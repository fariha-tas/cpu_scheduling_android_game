package com.example.cpuschedgame

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class GameViewModel : ViewModel() {

    // ── Algorithm (randomly assigned at game start) ───────────────
    var assignedAlgorithm by mutableStateOf(SchedulingAlgorithm.FCFS)
        private set

    var selectedLevel by mutableStateOf(Level.Easy)
        private set

    // ── Clocks ────────────────────────────────────────────────────
    /** Discrete scheduling clock — advances continuously in Hard mode, jumps on completion in Easy. */
    var schedulingTime by mutableIntStateOf(0); private set
    /** Real wall-clock for animations and spawning. */
    var wallTime by mutableFloatStateOf(0f); private set

    // ── Process lists ─────────────────────────────────────────────
    /** Arrived and waiting — player picks from here. */
    var waitingProcesses = mutableStateListOf<Process>(); private set
    /** Not yet arrived — shown as "incoming" (grayed). */
    var pendingProcesses = mutableStateListOf<Process>(); private set

    var runningProcess   by mutableStateOf<Process?>(null); private set
    var completedProcesses = mutableStateListOf<Process>(); private set
    var ganttChart         = mutableStateListOf<GanttEntry>(); private set

    // ── Game state ────────────────────────────────────────────────
    var score            by mutableIntStateOf(0);   private set
    var lives            by mutableIntStateOf(3);   private set
    var hasGameStarted   by mutableStateOf(false);  private set
    var isGameOver       by mutableStateOf(false);  private set
    var isPaused         by mutableStateOf(false);  private set
    var showAlgoIntro    by mutableStateOf(true);   private set
    var cpuBurstProgress by mutableFloatStateOf(0f);private set
    var cpuRemainingBurst by mutableIntStateOf(0);  private set
    var correctPicks     by mutableIntStateOf(0);   private set
    var wrongPicks       by mutableIntStateOf(0);   private set
    /** PID of the last wrong-picked process, cleared after a short window. */
    var lastWrongPid     by mutableStateOf<Int?>(null); private set
    /** True when the player cleared all processes without running out of lives. */
    var isGameWon        by mutableStateOf(false);  private set

    // ── Private fields ────────────────────────────────────────────
    private val MAX_PROCESSES   = 10
    private var cpuTimer        = 0f   // seconds elapsed since current process started running
    private var spawnWallTimer  = 0f
    private var spawnWallInterval = 2.5f
    private var pidCounter      = 1
    private var gameJob: Job?   = null
    private var wrongFlashJob: Job? = null

    /**
     * The schedulingTime value at the moment the current process was dispatched to the CPU.
     * Used to compute correct Gantt start/end times when a process is preempted or completes.
     */
    private var schedulingCpuStart = 0

    private val nameParts = listOf(
        "calc", "browser", "mail", "media", "net",
        "disk", "ui", "db", "auth", "kernel", "svc", "daemon"
    )

    // ── Public API ────────────────────────────────────────────────

    fun startGame(level: Level) {
        selectedLevel = level
        resetState()
        hasGameStarted = true

        val easyAlgorithms = listOf(
            SchedulingAlgorithm.FCFS,
            SchedulingAlgorithm.SJF_NP,
            SchedulingAlgorithm.PRIORITY_NP
        )
        val hardAlgorithms = listOf(
            SchedulingAlgorithm.ROUND_ROBIN,
            SchedulingAlgorithm.SJF_P,
            SchedulingAlgorithm.PRIORITY_P
        )

        assignedAlgorithm = when (level) {
            Level.Easy -> easyAlgorithms.random()
            Level.Hard -> hardAlgorithms.random()
        }

        // Seed 2–3 processes that all arrive at time 0
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
     * Easy (non-preemptive): if CPU is busy, tap is ignored.
     * Hard (preemptive):     if CPU is busy, the running process is preempted — it goes back to
     *                        the ready queue with its remaining burst time — and the tapped
     *                        process starts running.  Correctness is still checked.
     */
    fun scheduleProcess(process: Process, context: Context) {
        if (isGameOver || isPaused || showAlgoIntro || !hasGameStarted) return
        if (waitingProcesses.none { it.pid == process.pid }) return

        val isHard = isHardLevel()

        // Non-preemptive: block scheduling while CPU is busy
        if (!isHard && runningProcess != null) return

        val optimalPid = getOptimalPid()
        val isCorrect  = optimalPid == null || process.pid == optimalPid

        if (isCorrect) {
            playSound(context, R.raw.correct)
            correctPicks++
            score += computeScore(process)

            // ── Preempt the currently running process (Hard only) ──
            if (isHard && runningProcess != null) {
                preemptRunningProcess()
            }

            waitingProcesses.removeAll { it.pid == process.pid }
            runningProcess       = process.copy(state = ProcessState.RUNNING)
            cpuTimer             = 0f
            cpuRemainingBurst    = process.burstTime
            cpuBurstProgress     = 0f
            schedulingCpuStart   = schedulingTime

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
                gameJob?.cancel()
            }
        }
    }

    fun playSound(context: Context, soundRes: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundRes)
        mediaPlayer.setOnCompletionListener { it.release() }
        mediaPlayer.start()
    }

    fun dismissAlgoIntro() { showAlgoIntro = false }
    fun togglePause()       { isPaused = !isPaused }
    fun stopGame()          { gameJob?.cancel(); isGameOver = true }

    /** Returns the PID the current algorithm says should run next, or null if queue is empty. */
    fun getOptimalPid(): Int? {
        if (waitingProcesses.isEmpty()) return null
        return when (assignedAlgorithm) {
            SchedulingAlgorithm.FCFS ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.arrivalTime }.thenBy { it.pid })?.pid
            SchedulingAlgorithm.SJF_NP, SchedulingAlgorithm.SJF_P ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.burstTime }.thenBy { it.arrivalTime }.thenBy { it.pid })?.pid
            SchedulingAlgorithm.PRIORITY_NP, SchedulingAlgorithm.PRIORITY_P ->
                waitingProcesses
                    .minWithOrNull(compareBy<Process> { it.priority }.thenBy { it.arrivalTime }.thenBy { it.pid })?.pid
            SchedulingAlgorithm.ROUND_ROBIN ->
                waitingProcesses.firstOrNull()?.pid
            null -> null
        }
    }

    // ── Game tick (100 ms cadence) ────────────────────────────────

    /** Scale factor: 1.0 = real-time, 0.4 = 2.5× slower. */
    private val GAME_SPEED = 0.4f

    private fun isHardLevel() = selectedLevel == Level.Hard

    private fun tick(rawDelta: Float) {
        val delta = rawDelta * GAME_SPEED
        wallTime       += delta
        spawnWallTimer += delta

        // ── Spawn new processes on wall-time interval ──────────────
        if (spawnWallTimer >= spawnWallInterval && pidCounter <= MAX_PROCESSES) {
            spawnWallTimer    = 0f
            spawnWallInterval = (20..55).random() / 10f
            val offset = listOf(0, 0, 1, 2, 3).random()
            spawnProcess(arrivalTime = schedulingTime + offset)
            updateAvailability()
        }

        // ── Update wait timers and expire overdue processes ────────
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
                gameJob?.cancel()
                return
            }
        }

        // ── CPU execution ──────────────────────────────────────────
        runningProcess?.let { proc ->

            cpuTimer += delta

            // Hard: scheduling clock advances continuously with CPU execution
            if (isHardLevel()) {
                val elapsed      = cpuTimer.toInt()          // integer units executed so far
                val newSchedTime = schedulingCpuStart + elapsed

                if (newSchedTime > schedulingTime) {
                    // Snapshot next pending arrival BEFORE updating the clock,
                    // so the process hasn't been moved to waitingProcesses yet.
                    val nextArrival = pendingProcesses.minOfOrNull { it.arrivalTime }

                    schedulingTime = newSchedTime

                    // ── AUTO-PREEMPT when clock crosses a pending arrival time ──
                    val processedUnits = elapsed
                    val shouldAutoPreempt = nextArrival != null
                            && schedulingTime >= nextArrival
                            && processedUnits < proc.burstTime

                    if (shouldAutoPreempt) {
                        // Snap to the exact arrival boundary for clean Gantt entries
                        val executedUnits = (nextArrival - schedulingCpuStart).coerceIn(0, proc.burstTime)
                        schedulingTime = schedulingCpuStart + executedUnits
                        preemptRunningProcessWithUnits(proc, executedUnits)
                        updateAvailability()   // now move the arrived process into waiting
                        return
                    }

                    updateAvailability()       // normal arrival — no preemption needed
                }
            }

            val remaining = proc.burstTime - cpuTimer
            cpuRemainingBurst = remaining.toInt().coerceAtLeast(0)
            cpuBurstProgress  = (cpuTimer / proc.burstTime).coerceIn(0f, 1f)

            // ── Process finishes ───────────────────────────────────
            if (cpuTimer >= proc.burstTime.toFloat()) {
                val executedUnits = proc.burstTime
                val endTime       = schedulingCpuStart + executedUnits

                if (!isHardLevel()) {
                    // Easy: clock jumps forward on completion
                    schedulingTime += executedUnits
                } else {
                    // Hard: clock should already equal endTime from continuous updates above;
                    // ensure it's exact
                    schedulingTime = endTime
                }

                ganttChart.add(
                    GanttEntry(
                        pid       = proc.pid,
                        name      = proc.name,
                        duration  = executedUnits,
                        type      = proc.type,
                        startTime = schedulingCpuStart,
                        endTime   = endTime
                    )
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

    /**
     * Preempts the currently running process at its current cpuTimer boundary.
     * Adds a Gantt entry for the work done, then returns the process to the ready queue
     * with the remaining burst time.  Resets CPU state.
     *
     * Called either by the player dispatching a new process (manual preemption)
     * or automatically when the scheduling clock reaches a pending arrival time.
     */
    private fun preemptRunningProcess() {
        val proc = runningProcess ?: return
        val executedUnits = cpuTimer.toInt().coerceAtMost(proc.burstTime)
        preemptRunningProcessWithUnits(proc, executedUnits)
    }

    /**
     * Core preemption logic given the number of units already executed.
     */
    private fun preemptRunningProcessWithUnits(proc: Process, executedUnits: Int) {
        val remainingBurst = (proc.burstTime - executedUnits).coerceAtLeast(0)
        val endTime        = schedulingCpuStart + executedUnits

        // Record what the CPU did up to this point
        if (executedUnits > 0) {
            ganttChart.add(
                GanttEntry(
                    pid       = proc.pid,
                    name      = proc.name,
                    duration  = executedUnits,
                    type      = proc.type,
                    startTime = schedulingCpuStart,
                    endTime   = endTime
                )
            )
        }

        // Update scheduling clock to end of this slice
        schedulingTime = endTime

        // Return process to ready queue only if it has remaining work
        if (remainingBurst > 0) {
            waitingProcesses.add(
                proc.copy(
                    burstTime = remainingBurst,
                    state     = ProcessState.WAITING,
                    waitTimer = 0f   // reset expiry timer — it's back in the queue fresh
                )
            )
        } else {
            // Process finished exactly — mark completed
            completedProcesses.add(proc.copy(state = ProcessState.COMPLETED))
        }

        runningProcess    = null
        cpuTimer          = 0f
        cpuRemainingBurst = 0
        cpuBurstProgress  = 0f
    }

    /**
     * If CPU is idle and no process is waiting but some are pending,
     * jump the scheduling clock to the next arrival time (CPU idle period).
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

    /**
     * End the game with a win if all processes are spawned, none are pending or waiting,
     * and the CPU is idle.
     */
    private fun checkWinCondition() {
        if (isGameOver) return
        val allSpawned = pidCounter > MAX_PROCESSES
        val allClear   = runningProcess == null &&
                waitingProcesses.isEmpty() &&
                pendingProcesses.isEmpty()
        if (allSpawned && allClear) {
            isGameWon  = true
            isGameOver = true
            gameJob?.cancel()
        }
    }

    /** Move pending processes whose arrivalTime ≤ schedulingTime into the waiting queue. */
    private fun updateAvailability() {
        val nowArrived = pendingProcesses.filter { it.arrivalTime <= schedulingTime }
        if (nowArrived.isEmpty()) return
        val arrivedPids = nowArrived.map { it.pid }.toSet()
        pendingProcesses.removeAll  { it.pid in arrivedPids }
        waitingProcesses.addAll(nowArrived)
    }

    private fun spawnProcess(arrivalTime: Int) {
        val type  = ProcessType.values().random()
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

    private fun resetState() {
        waitingProcesses.clear(); pendingProcesses.clear()
        completedProcesses.clear(); ganttChart.clear()
        runningProcess      = null
        score               = 0; lives = 3
        wallTime            = 0f; schedulingTime = 0
        hasGameStarted      = false
        isGameOver          = false; isGameWon = false; isPaused = false; showAlgoIntro = true
        cpuTimer            = 0f; cpuBurstProgress = 0f; cpuRemainingBurst = 0
        spawnWallTimer      = 0f; spawnWallInterval = 2.5f
        pidCounter          = 1; correctPicks = 0; wrongPicks = 0
        lastWrongPid        = null
        schedulingCpuStart  = 0
    }

    override fun onCleared() { super.onCleared(); gameJob?.cancel() }
}