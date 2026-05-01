package com.example.cpuschedgame

data class Process(
    val pid: Int,
    val name: String,
    val burstTime: Int,
    val arrivalTime: Int,   // scheduling-clock time when this process becomes ready
    val priority: Int,      // 1 = highest priority, 5 = lowest
    val type: ProcessType,
    val maxWaitTime: Float, // real-time seconds before this process expires from the ready queue
    val waitTimer: Float = 0f,
    val state: ProcessState = ProcessState.WAITING
)

enum class ProcessType(val label: String) {
    IO("I/O"), CPU("CPU"), SYSTEM("SYS")
}

enum class ProcessState { WAITING, RUNNING, COMPLETED }

// ── Difficulty levels (Branch 2) ─────────────────────────────────
enum class Level(
    val displayName: String,
    val description: String
) {
    Easy(
        displayName = "Easy Level",
        description = "Non-preemptive Algorithm: Once a process is in the CPU queue, " +
                "you cannot remove it. It will be automatically removed after completion!"
    ),
    Medium(
        displayName = "Medium Level",
        description = "Preemptive Algorithm: You can remove processes from CPU " +
                "mid-execution and send the next most suitable process to increase CPU efficiency!"
    ),
    Hard(
        displayName = "Hard Level",
        description = "Hybrid Algorithm: Multiple algorithms are applicable in order. " +
                "You can remove processes from CPU mid-execution!"
    )
}

// ── Scheduling algorithms (Branch 2 — all 9 variants) ────────────
enum class SchedulingAlgorithm(
    val shortName: String,
    val displayName: String,
    val description: String,
    val detail: String,
    val hint: String,       // shown to player during game intro / pause
    val hintShort: String   // compact hint for the always-visible hint bar
) {
    // ── Easy ──────────────────────────────────────────────────────
    FCFS(
        shortName = "FCFS",
        displayName = "First Come\nFirst Served",
        description = "Queue order — first in, first out",
        detail = "Simplest algorithm. Processes execute in the order they arrive " +
                "(lowest AT). Can cause a 'convoy effect' where short jobs wait behind long ones.",
        hint = "Pick the process that ARRIVED EARLIEST (lowest AT value)",
        hintShort = "Pick LOWEST Arrival Time (AT)"
    ),
    SJF_NP(
        shortName = "SJF-NP",
        displayName = "Shortest Job\nFirst",
        description = "Pick the shortest burst time first",
        detail = "Gives optimal average waiting time. Schedule the process with the smallest " +
                "burst time. Use lowest AT as a tie-breaker. Long processes may starve.",
        hint = "Pick the process with the SMALLEST burst time (BT)",
        hintShort = "Pick SMALLEST Burst Time (BT)"
    ),
    PRIORITY_NP(
        shortName = "PRI-NP",
        displayName = "Priority\nScheduling",
        description = "Execute highest priority (P1) first",
        detail = "Each process has a priority 1–5 where 1 is highest. Break ties with lowest AT. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint = "Pick the process with the LOWEST priority number (P1 is most urgent)",
        hintShort = "Pick LOWEST Priority number (P1 first)"
    ),

    // ── Medium ────────────────────────────────────────────────────
    ROUND_ROBIN(
        shortName = "RR",
        displayName = "Round\nRobin",
        description = "Each process gets an equal time slice",
        detail = "Fair CPU sharing via time quantum. No starvation. Best for interactive systems.",
        hint = "Pick any process — Round Robin is fair!",
        hintShort = "Any order is fine"
    ),
    SJF_P(
        shortName = "SJF-P",
        displayName = "Shortest Job\nFirst (P)",
        description = "Pick the shortest burst time first. Can remove process mid-execution!",
        detail = "Gives optimal average waiting time. Schedule the process with the smallest " +
                "burst time. Break ties with lowest AT. Long processes may starve.",
        hint = "Pick the process with the SMALLEST burst time (BT). " +
                "Processes can be removed mid-execution!",
        hintShort = "Pick SMALLEST Burst Time (BT)\nProcesses can be removed mid-execution!"
    ),
    PRIORITY_P(
        shortName = "PRI-P",
        displayName = "Priority\nScheduling (P)",
        description = "Execute highest priority (P1) first. Can remove process mid-execution!",
        detail = "Each process has a priority 1–5 where 1 is highest. Break ties with lowest AT. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint = "Pick the process with the LOWEST priority number (P1 is most urgent). " +
                "Processes can be removed mid-execution!",
        hintShort = "Pick LOWEST Priority number (P1 first)\nProcesses can be removed mid-execution!"
    ),

    // ── Hard (hybrid) ─────────────────────────────────────────────
    FCFS_SJF(
        shortName = "FCFS+SJF",
        displayName = "FCFS & Shortest Job First\nScheduling",
        description = "Lowest AT first; tie-break with smallest BT, then order. Preemptive.",
        detail = "Primary key: earliest arrival time. If two processes arrive at the same time, " +
                "prefer the shorter burst. Can remove the running process mid-execution.",
        hint = "Pick LOWEST Arrival Time (AT). Same AT → pick SMALLEST Burst Time (BT). " +
                "Processes can be removed mid-execution!",
        hintShort = "LOWEST AT → tie: SMALLEST BT\nProcesses can be removed mid-execution!"
    ),
    FCFS_Priority(
        shortName = "FCFS+PRI",
        displayName = "FCFS & Priority\nScheduling",
        description = "Lowest AT first; tie-break with highest priority, then order. Preemptive.",
        detail = "Primary key: earliest arrival time. If two processes arrive together, " +
                "prefer the highest priority (lowest number). Can preempt mid-execution.",
        hint = "Pick LOWEST Arrival Time (AT). Same AT → pick LOWEST Priority number. " +
                "Processes can be removed mid-execution!",
        hintShort = "LOWEST AT → tie: LOWEST Priority\nProcesses can be removed mid-execution!"
    ),
    SJF_Priority(
        shortName = "SJF+PRI",
        displayName = "Shortest Job First & Priority\nScheduling",
        description = "Smallest BT first; tie-break with highest priority, then AT, then order. Preemptive.",
        detail = "Primary key: shortest burst time. Ties broken first by priority, then arrival time. " +
                "Can preempt the running process mid-execution.",
        hint = "Pick SMALLEST Burst Time (BT). Same BT → pick LOWEST Priority number. " +
                "Processes can be removed mid-execution!",
        hintShort = "SMALLEST BT → tie: LOWEST Priority\nProcesses can be removed mid-execution!"
    ),
    Priority_SJF(
        shortName = "PRI+SJF",
        displayName = "Priority & Shortest Job First\nScheduling",
        description = "Highest priority first; tie-break with lowest BT, then AT, then order. Preemptive.",
        detail = "Primary key: highest priority (lowest number). Ties broken by burst time. " +
                "Can preempt the running process mid-execution.",
        hint = "Pick LOWEST Priority number (P1 most urgent). Same priority → pick SMALLEST BT. " +
                "Processes can be removed mid-execution!",
        hintShort = "LOWEST Priority → tie: SMALLEST BT\nProcesses can be removed mid-execution!"
    )
}

// ── Gantt chart entry ─────────────────────────────────────────────
data class GanttEntry(
    val pid: Int,
    val name: String,
    val duration: Int,
    val type: ProcessType,
    val startTime: Int,
    val endTime: Int
)