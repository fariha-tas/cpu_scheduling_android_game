package com.example.cpuschedgame

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class GameViewModel : ViewModel() {

    // ── Algorithm (randomly assigned at game start) ───────────────
    var assignedAlgorithm by mutableStateOf(SchedulingAlgorithm.FCFS); private set

    // ── Clocks ────────────────────────────────────────────────────
    /** Discrete scheduling clock — jumps forward by burstTime when a process completes. */
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
    private var cpuTimer        = 0f
    private var spawnWallTimer  = 0f
    private var spawnWallInterval = 2.5f
    private var pidCounter      = 1
    private var gameJob: Job?   = null
    private var wrongFlashJob: Job? = null

    private val nameParts = listOf(
        "calc", "browser", "mail", "media", "net",
        "disk", "ui", "db", "auth", "kernel", "svc", "daemon"
    )

    // ── Public API ────────────────────────────────────────────────

    fun startGame() {

        resetState()
        hasGameStarted = true
        // Non-preemptive only: FCFS, SJF, Priority
        assignedAlgorithm = listOf(
            SchedulingAlgorithm.FCFS,
            SchedulingAlgorithm.SJF,
            SchedulingAlgorithm.PRIORITY
        ).random()

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
     * Player taps a process card. Non-preemptive: if CPU is busy, the tap is ignored.
     * Correctness is evaluated against the current algorithm's rule.
     */
    fun scheduleProcess(process: Process,  context: Context) {

        if (isGameOver || isPaused || showAlgoIntro || !hasGameStarted) return
        if (runningProcess != null) return          // CPU busy — non-preemptive
        if (waitingProcesses.none { it.pid == process.pid }) return

        val optimalPid = getOptimalPid()
        val isCorrect  = optimalPid == null || process.pid == optimalPid

        if (isCorrect) {
            playSound(context, R.raw.correct)
            correctPicks++
            score += computeScore(process)
            waitingProcesses.removeAll { it.pid == process.pid }
            runningProcess    = process.copy(state = ProcessState.RUNNING)
        } else {
            playSound(context, R.raw.wrong)
            wrongPicks++
            lives = maxOf(0, lives - 1)
            score = maxOf(0, score - 50)
            // Flash the wrong pick
            lastWrongPid = process.pid
            wrongFlashJob?.cancel()
            wrongFlashJob = viewModelScope.launch {
                delay(500L)
                lastWrongPid = null
            }
            if (lives == 0) {
                isGameOver = true
                gameJob?.cancel()
                return
            }
        }

        cpuTimer          = 0f
        cpuRemainingBurst = process.burstTime
        cpuBurstProgress  = 0f
    }

    fun playSound(context: Context, soundRes: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundRes)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
    }
    fun dismissAlgoIntro() { showAlgoIntro = false }
    fun togglePause()       { isPaused = !isPaused }
    fun stopGame()          { gameJob?.cancel(); isGameOver = true }

    /** Returns the PID the current algorithm says should run next, or null if queue is empty. */
    fun getOptimalPid(): Int? {
        if (waitingProcesses.isEmpty()) return null
        return when (assignedAlgorithm) {
            SchedulingAlgorithm.FCFS     ->
                waitingProcesses.minByOrNull { it.arrivalTime }?.pid
                    ?: waitingProcesses.minByOrNull { it.pid }?.pid
            SchedulingAlgorithm.SJF      ->
                waitingProcesses.minByOrNull { it.burstTime }?.pid
            SchedulingAlgorithm.PRIORITY ->
                waitingProcesses.minByOrNull { it.priority }?.pid
            SchedulingAlgorithm.ROUND_ROBIN ->
                waitingProcesses.firstOrNull()?.pid
        }
    }

    // ── Game tick (100 ms cadence) ────────────────────────────────

    /** Scale factor: 1.0 = real-time, 0.4 = 2.5× slower. */
    private val GAME_SPEED = 0.4f

    private fun tick(rawDelta: Float) {
        val delta = rawDelta * GAME_SPEED
        wallTime       += delta
        spawnWallTimer += delta

        // ── Spawn new processes on wall-time interval ──────────────
        if (spawnWallTimer >= spawnWallInterval && pidCounter <= MAX_PROCESSES) {
            spawnWallTimer    = 0f
            spawnWallInterval = (20..55).random() / 10f
            // Occasionally arrive immediately; mostly arrive a few scheduling units ahead
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
            lives  = maxOf(0, lives - expired.size)
            score  = maxOf(0, score - 50 * expired.size)
            if (lives <= 0) {
                lives      = 0
                isGameOver = true
                gameJob?.cancel()
                return
            }
        }

        // ── CPU execution (1 burst-unit = 1 real second) ──────────
        runningProcess?.let { proc ->
            cpuTimer         += delta
            val remaining     = proc.burstTime - cpuTimer
            cpuRemainingBurst = remaining.toInt().coerceAtLeast(0)
            cpuBurstProgress  = (cpuTimer / proc.burstTime).coerceIn(0f, 1f)

            if (cpuTimer >= proc.burstTime.toFloat()) {
                // Process finished
                val startT   = schedulingTime
                schedulingTime += proc.burstTime

                ganttChart.add(GanttEntry(proc.pid, proc.name, proc.burstTime, proc.type, startT, schedulingTime))
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
            autoAdvanceIfIdle()  // also run when CPU becomes idle immediately
            checkWinCondition()
        }
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
        val prio = (1..5).random()
        // High-priority processes (P1) expire faster from the ready queue
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
        val base         = 100
        val priorityBonus = (6 - proc.priority) * 5   // P1 → +25, P5 → +5
        val burstBonus    = maxOf(0, (8 - proc.burstTime) * 4)  // shorter = more
        return base + priorityBonus + burstBonus
    }

    private fun resetState() {
        waitingProcesses.clear(); pendingProcesses.clear()
        completedProcesses.clear(); ganttChart.clear()
        runningProcess    = null
        score             = 0; lives = 3
        wallTime          = 0f; schedulingTime = 0
        hasGameStarted    = false
        isGameOver        = false; isGameWon = false; isPaused = false; showAlgoIntro = true
        cpuTimer          = 0f; cpuBurstProgress = 0f; cpuRemainingBurst = 0
        spawnWallTimer    = 0f; spawnWallInterval = 2.5f
        pidCounter        = 1; correctPicks = 0; wrongPicks = 0
        lastWrongPid      = null
    }

    override fun onCleared() { super.onCleared(); gameJob?.cancel() }
}