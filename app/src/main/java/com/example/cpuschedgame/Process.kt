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

// ── Difficulty levels ─────────────────────────────────────────────
enum class Level(
    val displayName: String,
    val description: String
) {
    Easy(
        displayName = "Easy",
        description = "Non-preemptive: FCFS, SJF, or Priority. Once a process starts " +
                "on the CPU it runs to completion — no interruptions!"
    ),
    Medium(
        displayName = "Medium",
        description = "Preemptive: FCFS-P, SJF-P, Round Robin, or Priority-P. " +
                "You can tap a new process to preempt the one currently running."
    ),
    Hard(
        displayName = "Hard",
        description = "Hybrid preemptive: combinations of FCFS, SJF, and Priority. " +
                "Primary and tie-break keys both apply; preemption is allowed."
    )
}

// ── Scheduling algorithms ─────────────────────────────────────────
enum class SchedulingAlgorithm(
    val shortName: String,
    val displayName: String,
    val description: String,
    val detail: String,
    val hint: String,
    val hintShort: String
) {
    // ── Easy (non-preemptive) ─────────────────────────────────────
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
        shortName = "SJF",
        displayName = "Shortest Job\nFirst",
        description = "Pick the shortest burst time first (non-preemptive)",
        detail = "Gives optimal average waiting time. Schedule the process with the smallest " +
                "burst time. Use lowest AT as a tie-breaker. Long processes may starve.",
        hint = "Pick the process with the SMALLEST burst time (BT)",
        hintShort = "Pick SMALLEST Burst Time (BT)"
    ),
    PRIORITY_NP(
        shortName = "PRI",
        displayName = "Priority\nScheduling",
        description = "Execute highest priority (P1) first (non-preemptive)",
        detail = "Each process has a priority 1–5 where 1 is highest. Break ties with lowest AT. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint = "Pick the process with the LOWEST priority number (P1 is most urgent)",
        hintShort = "Pick LOWEST Priority number (P1 first)"
    ),

    // ── Medium (preemptive single-algorithm) ─────────────────────
    FCFS_P(
        shortName = "FCFS-P",
        displayName = "First Come First Served\n(Preemptive)",
        description = "Earliest arrival gets CPU; new arrivals can preempt the running process.",
        detail = "Like FCFS but preemptive. If a process with an earlier arrival time appears " +
                "while another is running, it can take over the CPU immediately.",
        hint = "Pick the process that ARRIVED EARLIEST (lowest AT). You can preempt the running process!",
        hintShort = "Pick LOWEST Arrival Time (AT)\nYou can preempt mid-execution!"
    ),
    SJF_P(
        shortName = "SJF-P",
        displayName = "Shortest Job\nFirst (Preemptive)",
        description = "Pick the shortest burst time first — can preempt mid-execution!",
        detail = "Gives optimal average waiting time. Schedule the process with the smallest " +
                "burst time. Break ties with lowest AT. Long processes may starve.",
        hint = "Pick the process with the SMALLEST burst time (BT). You can preempt the running process!",
        hintShort = "Pick SMALLEST Burst Time (BT)\nYou can preempt mid-execution!"
    ),
    PRIORITY_P(
        shortName = "PRI-P",
        displayName = "Priority\nScheduling (Preemptive)",
        description = "Execute highest priority (P1) first — can preempt mid-execution!",
        detail = "Each process has a priority 1–5 where 1 is highest. Break ties with lowest AT. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint = "Pick the process with the LOWEST priority number (P1 is most urgent). You can preempt!",
        hintShort = "Pick LOWEST Priority number (P1 first)\nYou can preempt mid-execution!"
    ),
    ROUND_ROBIN(
        shortName = "RR",
        displayName = "Round\nRobin",
        description = "Each process gets an equal time slice (preemptive)",
        detail = "Fair CPU sharing via time quantum. No starvation. Best for interactive systems. " +
                "Every process gets a turn in order.",
        hint = "Pick any process in order — Round Robin is fair! Each gets equal CPU time.",
        hintShort = "Pick in ARRIVAL ORDER\nEach process gets equal CPU time"
    ),

    // ── Hard (hybrid preemptive) ──────────────────────────────────
    FCFS_SJF(
        shortName = "FCFS+SJF",
        displayName = "FCFS & Shortest Job First\nScheduling",
        description = "Lowest AT first; tie-break with smallest BT. Preemptive.",
        detail = "Primary key: earliest arrival time. If two processes arrive at the same time, " +
                "prefer the shorter burst. Can remove the running process mid-execution.",
        hint = "Pick LOWEST Arrival Time (AT). Same AT → pick SMALLEST Burst Time (BT). " +
                "Processes can be removed mid-execution!",
        hintShort = "LOWEST AT → tie: SMALLEST BT\nPreemptive!"
    ),
    FCFS_Priority(
        shortName = "FCFS+PRI",
        displayName = "FCFS & Priority\nScheduling",
        description = "Lowest AT first; tie-break with highest priority. Preemptive.",
        detail = "Primary key: earliest arrival time. If two processes arrive together, " +
                "prefer the highest priority (lowest number). Can preempt mid-execution.",
        hint = "Pick LOWEST Arrival Time (AT). Same AT → pick LOWEST Priority number. " +
                "Processes can be removed mid-execution!",
        hintShort = "LOWEST AT → tie: LOWEST Priority\nPreemptive!"
    ),
    SJF_Priority(
        shortName = "SJF+PRI",
        displayName = "Shortest Job First & Priority\nScheduling",
        description = "Smallest BT first; tie-break with highest priority. Preemptive.",
        detail = "Primary key: shortest burst time. Ties broken first by priority, then arrival time. " +
                "Can preempt the running process mid-execution.",
        hint = "Pick SMALLEST Burst Time (BT). Same BT → pick LOWEST Priority number. " +
                "Processes can be removed mid-execution!",
        hintShort = "SMALLEST BT → tie: LOWEST Priority\nPreemptive!"
    ),
    Priority_SJF(
        shortName = "PRI+SJF",
        displayName = "Priority & Shortest Job First\nScheduling",
        description = "Highest priority first; tie-break with lowest BT. Preemptive.",
        detail = "Primary key: highest priority (lowest number). Ties broken by burst time. " +
                "Can preempt the running process mid-execution.",
        hint = "Pick LOWEST Priority number (P1 most urgent). Same priority → pick SMALLEST BT. " +
                "Processes can be removed mid-execution!",
        hintShort = "LOWEST Priority → tie: SMALLEST BT\nPreemptive!"
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