package com.example.cpuschedgame

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class GameViewModel : ViewModel() {

    // ── Exposed state ────────────────────────────────────────────
    var processQueue = mutableStateListOf<Process>(); private set
    var runningProcess by mutableStateOf<Process?>(null); private set
    var completedProcesses = mutableStateListOf<Process>(); private set
    var ganttChart = mutableStateListOf<GanttEntry>(); private set

    var score by mutableIntStateOf(0); private set
    var lives by mutableIntStateOf(3); private set
    var timeElapsed by mutableFloatStateOf(0f); private set
    var isGameOver by mutableStateOf(false); private set
    var isPaused by mutableStateOf(false); private set
    var selectedAlgorithm by mutableStateOf(SchedulingAlgorithm.FCFS); private set
    var cpuBurstProgress by mutableFloatStateOf(0f); private set

    // ── Private fields ───────────────────────────────────────────
    private var pidCounter = 1
    private var spawnTimer = 0f
    private var spawnInterval = 2.5f
    private var cpuTimer = 0f
    private var gameJob: Job? = null

    private val nameParts = listOf(
        "calc", "browser", "mail", "media", "net",
        "disk", "ui", "db", "auth", "kernel", "svc", "daemon"
    )

    // ── Public API ───────────────────────────────────────────────

    fun selectAlgorithm(algorithm: SchedulingAlgorithm) {
        selectedAlgorithm = algorithm
    }

    fun startGame() {
        resetState()
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            while (isActive) {
                delay(100L) // 10 ticks / second
                if (!isPaused && !isGameOver) gameTick(0.1f)
            }
        }
    }

    fun scheduleProcess(process: Process) {
        if (isGameOver || isPaused) return
        if (processQueue.none { it.pid == process.pid }) return

        val current = runningProcess
        when {
            current == null -> {
                removeFromQueue(process.pid)
                setRunning(process)
            }
            selectedAlgorithm == SchedulingAlgorithm.PRIORITY
                    && process.priority < current.priority -> {
                // Preempt — put current back at front
                removeFromQueue(process.pid)
                processQueue.add(0, current.copy(state = ProcessState.WAITING))
                setRunning(process)
            }
            selectedAlgorithm == SchedulingAlgorithm.ROUND_ROBIN -> {
                removeFromQueue(process.pid)
                processQueue.add(current.copy(state = ProcessState.WAITING))
                setRunning(process)
            }
            // FCFS / SJF: non-preemptive — ignore tap while CPU busy
        }
    }

    fun togglePause() { isPaused = !isPaused }

    fun stopGame() {
        gameJob?.cancel()
        isGameOver = true
    }

    // ── Game tick ────────────────────────────────────────────────

    private fun gameTick(delta: Float) {
        timeElapsed += delta

        // Spawn
        spawnTimer += delta
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnInterval = (15..50).random() / 10f
            if (processQueue.size < 7) spawnProcess()
        }

        // Decrement deadlines
        val expired = mutableListOf<Int>()
        val updated = processQueue.map { p ->
            val dl = p.deadlineRemaining - delta
            if (dl <= 0) { expired.add(p.pid); p.copy(deadlineRemaining = 0f) }
            else p.copy(deadlineRemaining = dl)
        }
        processQueue.clear()
        processQueue.addAll(updated.filter { it.pid !in expired })

        if (expired.isNotEmpty()) {
            lives -= expired.size
            score = maxOf(0, score - 50 * expired.size)
            if (lives <= 0) {
                lives = 0
                isGameOver = true
                gameJob?.cancel()
                return
            }
        }

        // CPU execution
        runningProcess?.let { proc ->
            cpuTimer += delta
            val unitsDone = cpuTimer.toInt()
            if (unitsDone >= 1) {
                cpuTimer -= unitsDone.toFloat()
                val remaining = proc.remainingBurst - unitsDone
                if (remaining <= 0) {
                    ganttChart.add(GanttEntry(proc.pid, proc.name, proc.burstTime, proc.type))
                    completedProcesses.add(proc.copy(remainingBurst = 0, state = ProcessState.COMPLETED))
                    score += computeScore(proc)
                    runningProcess = null
                    cpuBurstProgress = 0f
                    autoScheduleNext()
                } else {
                    runningProcess = proc.copy(remainingBurst = remaining)
                    cpuBurstProgress = 1f - remaining.toFloat() / proc.burstTime.toFloat()
                }
            } else {
                val effective = proc.remainingBurst - cpuTimer
                cpuBurstProgress = (1f - effective / proc.burstTime).coerceIn(0f, 1f)
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun autoScheduleNext() {
        if (processQueue.isEmpty()) return
        val next = when (selectedAlgorithm) {
            SchedulingAlgorithm.FCFS     -> processQueue.first()
            SchedulingAlgorithm.SJF      -> processQueue.minByOrNull { it.burstTime }!!
            SchedulingAlgorithm.PRIORITY -> processQueue.minByOrNull { it.priority }!!
            SchedulingAlgorithm.ROUND_ROBIN -> processQueue.first()
        }
        removeFromQueue(next.pid)
        setRunning(next)
    }

    private fun computeScore(proc: Process): Int {
        val base = 100
        val deadlineBonus = (proc.deadlineRemaining / proc.deadline * 60f).toInt()
        val priorityBonus = (6 - proc.priority) * 10
        return base + deadlineBonus + priorityBonus
    }

    private fun spawnProcess() {
        val type = ProcessType.values().random()
        val burst = when (type) {
            ProcessType.IO     -> (1..4).random()
            ProcessType.CPU    -> (3..8).random()
            ProcessType.SYSTEM -> (1..6).random()
        }
        val dl = burst * 2.5f + (3..7).random()
        processQueue.add(
            Process(
                pid = pidCounter,
                name = "${nameParts.random()}_$pidCounter",
                burstTime = burst,
                deadline = dl,
                deadlineRemaining = dl,
                priority = (1..5).random(),
                type = type
            )
        )
        pidCounter++
    }

    private fun removeFromQueue(pid: Int) = processQueue.removeAll { it.pid == pid }

    private fun setRunning(proc: Process) {
        runningProcess = proc.copy(state = ProcessState.RUNNING)
        cpuTimer = 0f
        cpuBurstProgress = 0f
    }

    private fun resetState() {
        processQueue.clear(); completedProcesses.clear(); ganttChart.clear()
        runningProcess = null; score = 0; lives = 3; timeElapsed = 0f
        isGameOver = false; isPaused = false; pidCounter = 1
        spawnTimer = 0f; spawnInterval = 2.5f; cpuTimer = 0f; cpuBurstProgress = 0f
        spawnProcess() // seed first process immediately
    }

    override fun onCleared() { super.onCleared(); gameJob?.cancel() }
}