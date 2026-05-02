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
    SJF_P(
        shortName = "SJF-P",
        displayName = "Shortest Job\nFirst (Preemptive)",
        description = "Pick the shortest remaining burst time first — preempt after every time unit!",
        detail = "After every CPU time unit you decide: keep the running process OR switch to the " +
                "one with the smallest remaining burst time. Ties broken by lowest arrival time.",
        hint = "Each time unit: if a waiting process has SMALLER BT than the running one, tap it to preempt. Otherwise tap the running process to continue.",
        hintShort = "Every unit: SMALLEST BT wins\nTap running card to continue or switch"
    ),
    PRIORITY_P(
        shortName = "PRI-P",
        displayName = "Priority\nScheduling (Preemptive)",
        description = "Highest priority (P1) wins every time unit — you decide each tick!",
        detail = "After every CPU time unit you decide: keep the running process OR switch to a " +
                "higher-priority one. Lowest priority number = most urgent. Ties broken by lowest AT.",
        hint = "Each time unit: if a waiting process has LOWER priority number than the running one, tap it to preempt. Otherwise tap the running process to continue.",
        hintShort = "Every unit: LOWEST Priority number wins\nTap running card to continue or switch"
    ),
    ROUND_ROBIN(
        shortName = "RR",
        displayName = "Round\nRobin",
        description = "Each process gets a fixed quantum (2–3 units), then rotates to the next in queue.",
        detail = "Fair CPU sharing with a time quantum. Each process runs for its full quantum " +
                "before being rotated out — no decision needed mid-quantum. After rotation, " +
                "pick the NEXT process in queue order. No starvation — every process gets a turn.",
        hint = "Process runs for its FULL QUANTUM automatically, then you MUST pick the NEXT " +
                "process in the RR queue (shown below the CPU panel). Check the RR Queue strip " +
                "to see who comes next — pick whoever is at position #1!",
        hintShort = "Process auto-runs for full quantum\nAfter rotation: tap #1 in RR queue order"
    ),

    // ── Hard (hybrid preemptive) ──────────────────────────────────
    SJF_Priority(
        shortName = "SJF+PRI",
        displayName = "Shortest Job First & Priority\nScheduling",
        description = "Smallest BT first; tie-break with highest priority. Preemptive per time unit.",
        detail = "Primary key: shortest remaining burst time. Ties broken by priority number (lowest = most urgent), then by arrival time. After every time unit you decide whether to switch.",
        hint = "Each unit: pick SMALLEST BT. Same BT → pick LOWEST Priority number. Preempt the running process if a better one is waiting!",
        hintShort = "Every unit: SMALLEST BT → tie: LOWEST Priority\nPreemptive!"
    ),
    Priority_SJF(
        shortName = "PRI+SJF",
        displayName = "Priority & Shortest Job First\nScheduling",
        description = "Highest priority first; tie-break with lowest BT. Preemptive per time unit.",
        detail = "Primary key: highest priority (lowest number). Ties broken by shortest burst time, then arrival time. After every time unit you decide whether to switch.",
        hint = "Each unit: pick LOWEST Priority number. Same priority → pick SMALLEST BT. Preempt if a more urgent process arrives!",
        hintShort = "Every unit: LOWEST Priority → tie: SMALLEST BT\nPreemptive!"
    ),
    SJF_RR(
        shortName = "SJF+RR",
        displayName = "Shortest Job First & Round Robin\nScheduling",
        description = "Smallest burst time first; tie-break with RR queue order. Preemptive per time unit.",
        detail = "Primary key: shortest remaining burst time. When multiple processes share the same burst time, fall back to Round Robin queue order as the tie-breaker. Preempt after every time unit.",
        hint = "Each unit: pick SMALLEST BT. Same BT → pick whoever is FIRST in the RR queue. Preempt the running process if a shorter-burst one is waiting!",
        hintShort = "Every unit: SMALLEST BT → tie: RR queue order\nPreemptive!"
    ),
    Priority_RR(
        shortName = "PRI+RR",
        displayName = "Priority & Round Robin\nScheduling",
        description = "Highest priority (P1) first; tie-break with RR queue order. Preemptive per time unit.",
        detail = "Primary key: highest priority (lowest number). When multiple processes share the same priority, fall back to Round Robin queue order as the tie-breaker. Preempt after every time unit.",
        hint = "Each unit: pick LOWEST Priority number. Same priority → pick whoever is FIRST in the RR queue. Preempt if a more urgent process arrives!",
        hintShort = "Every unit: LOWEST Priority → tie: RR queue order\nPreemptive!"
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