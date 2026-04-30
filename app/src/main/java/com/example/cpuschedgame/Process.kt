package com.example.cpuschedgame

data class Process(
    val pid: Int,
    val name: String,
    val burstTime: Int,
    val arrivalTime: Int,    // scheduling-clock time when this process becomes ready
    val priority: Int,       // 1 = highest priority, 5 = lowest
    val type: ProcessType,
    val maxWaitTime: Float,  // real-time seconds before this expires from the ready queue
    val waitTimer: Float = 0f,
    val state: ProcessState = ProcessState.WAITING
)

enum class ProcessType(val label: String) {
    IO("I/O"), CPU("CPU"), SYSTEM("SYS")
}

enum class ProcessState { WAITING, RUNNING, COMPLETED }

enum class Level(
    val displayName: String,
    val description: String
) {
    Easy(
        displayName = "Easy Level",
        description = "Non-preemptive Algorithm : Once a process is in the CPU queue, you cannot remove it. It will be automatically removed after completion!"
    ),
    Medium(
        displayName = "Medium Level",
        description = "Preemptive Algorithm: You can remove processes from CPU mid-execution and send next most suitable process to increase efficiency of CPU!"
    ),
    Hard(
        displayName = "Hard Level",
        description = "Hybrid Algorithm: Multiple algorithms will be applicable in order. You can remove processes from CPU mid-execution!"
    )
}

enum class SchedulingAlgorithm(
    val shortName: String,
    val displayName: String,
    val description: String,
    val detail: String,
    val hint: String,           // shown to player during game
    val hintShort: String       // compact hint for hint bar
) {
    FCFS(
        shortName   = "FCFS",
        displayName = "First Come\nFirst Served",
        description = "Queue order — first in, first out",
        detail      = "Simplest algorithm. Processes execute in the order they arrive or shortest arrival time (AT). " +
                "Can cause a 'convoy effect' where short jobs wait behind long ones.",
        hint        = "Pick the process that ARRIVED EARLIEST (lowest AT value)",
        hintShort   = "Pick LOWEST Arrival Time (AT)"
    ),
    SJF_NP(
        shortName   = "SJF-NP",
        displayName = "Shortest Job\nFirst",
        description = "Pick the shortest burst time first",
        detail      = "Gives optimal average waiting time. Schedule the process with the " +
                "smallest burst time. Smallest AT and then order for tie breaker. Long processes may starve if short ones keep arriving.",
        hint        = "Pick the process with the SMALLEST burst time (BT)",
        hintShort   = "Pick SMALLEST Burst Time (BT)"
    ),
    PRIORITY_NP(
        shortName   = "PRI-NP",
        displayName = "Priority\nScheduling",
        description = "Execute highest priority (P1) first",
        detail      = "Each process has a priority 1–5 where 1 is the highest. Break tie with smallest AT and then order. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint        = "Pick the process with the LOWEST priority number (P1 is most urgent)",
        hintShort   = "Pick LOWEST Priority number (P1 first)"
    ),
    ROUND_ROBIN(
        shortName   = "RR",
        displayName = "Round\nRobin",
        description = "Each process gets an equal time slice",
        detail      = "Fair CPU sharing via time quantum. No starvation.",
        hint        = "Pick any process — Round Robin is fair!",
        hintShort   = "Any order is fine"
    ),
    SJF_P(
        shortName   = "SJF-P",
        displayName = "Shortest Job\nFirst",
        description = "Pick the shortest burst time first. Can remove process mid-execution!",
        detail      = "Gives optimal average waiting time. Schedule the process with the " +
                        "smallest burst time. Break tie with smallest AT and then order. Long processes may starve if short ones keep arriving.",
        hint        = "Pick the process with the SMALLEST burst time (BT)." +
                        " Processes can be removed mid-execution!",
        hintShort   = "Pick SMALLEST Burst Time (BT)" + "\nProcesses can be removed mid-execution!"
    ),
    PRIORITY_P(
        shortName   = "PRI-P",
        displayName = "Priority\nScheduling",
        description = "Execute highest priority (P1) first. Can remove process mid-execution!",
        detail      = "Each process has a priority 1–5 where 1 is the highest. Break tie with smallest AT and then order. " +
                        "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint        = "Pick the process with the LOWEST priority number (P1 is most urgent)."
                        + " Processes can be removed mid-execution!",
        hintShort   = "Pick LOWEST Priority number (P1 first)" + "\nProcesses can be removed mid-execution!"
    ),
    FCFS_SJF(
        shortName   = "FCFS+SJF",
        displayName = "First COme First Served & Shortest Job First\nScheduling",
        description = "Execute lowest arrival time (AT) first. If same AT, break the tie with shortest burst time (BT). Then order. Can remove process mid-execution!",
        detail      = "Each process has a priority 1–5 where 1 is the highest. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint        = "Pick the process with the LOWEST priority number (P1 is most urgent)."
                + " Processes can be removed mid-execution!",
        hintShort   = "Pick LOWEST Priority number (P1 first)" + "\nProcesses can be removed mid-execution!"
    ),
    FCFS_Priority(
        shortName   = "FCFS+PRI",
        displayName = "First COme First Served & Priority\nScheduling",
        description = "Execute lowest arrival time (AT) first. If same AT, break the tie with highest priority. Then order. Can remove process mid-execution!",
        detail      = "Each process has a priority 1–5 where 1 is the highest. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint        = "Pick the process with the LOWEST priority number (P1 is most urgent)."
                + " Processes can be removed mid-execution!",
        hintShort   = "Pick LOWEST Priority number (P1 first)" + "\nProcesses can be removed mid-execution!"
    ),
    SJF_Priority(
        shortName   = "SJF+PRI",
        displayName = "Shortest Job First & Priority\nScheduling",
        description = "Execute shortest burst time (BT) first. If same BT, break the tie with highest priority. Then AT, then order. Can remove process mid-execution!",
        detail      = "Each process has a priority 1–5 where 1 is the highest. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint        = "Pick the process with the LOWEST priority number (P1 is most urgent)."
                + " Processes can be removed mid-execution!",
        hintShort   = "Pick LOWEST Priority number (P1 first)" + "\nProcesses can be removed mid-execution!"
    ),
    Priority_SJF(
        shortName   = "PRI+SJF",
        displayName = "Priority & Shortest Job First\nScheduling",
        description = "Execute highest priority first. If same priority, break the tie with lowest BT. Then AT, then order. Can remove process mid-execution!",
        detail      = "Each process has a priority 1–5 where 1 is the highest. " +
                "Always run the most critical process first. Risk of starvation for P5 processes.",
        hint        = "Pick the process with the LOWEST priority number (P1 is most urgent)."
                + " Processes can be removed mid-execution!",
        hintShort   = "Pick LOWEST Priority number (P1 first)" + "\nProcesses can be removed mid-execution!"
    )
}

data class GanttEntry(
    val pid: Int,
    val name: String,
    val duration: Int,
    val type: ProcessType,
    val startTime: Int,
    val endTime: Int
)