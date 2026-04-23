package com.example.cpuschedgame

data class Process(
    val pid: Int,
    val name: String,
    val burstTime: Int,
    val deadline: Float,
    val priority: Int,          // 1 = highest, 5 = lowest
    val type: ProcessType,
    val remainingBurst: Int   = burstTime,
    val deadlineRemaining: Float = deadline,
    val state: ProcessState   = ProcessState.WAITING
)

enum class ProcessType(val label: String) {
    IO("I/O"), CPU("CPU"), SYSTEM("SYS")
}

enum class ProcessState { WAITING, RUNNING, COMPLETED, EXPIRED }

enum class SchedulingAlgorithm(
    val shortName: String,
    val displayName: String,
    val description: String,
    val detail: String
) {
    FCFS(
        "FCFS", "First Come\nFirst Served",
        "Queue order — first in, first out",
        "Simplest algorithm. Processes execute in arrival order. Can cause a 'convoy effect' where short jobs wait behind long ones."
    ),
    SJF(
        "SJF", "Shortest Job\nFirst",
        "Pick the shortest burst time first",
        "Optimal average wait time. Schedule the process with the smallest burst time. Beware of starvation for long processes."
    ),
    PRIORITY(
        "PRI", "Priority\nScheduling",
        "Execute highest priority (P1) first",
        "Each process has a priority level 1–5 (1=highest). Higher priority can preempt lower ones. Risk of starvation."
    ),
    ROUND_ROBIN(
        "RR", "Round\nRobin",
        "Each process gets an equal time slice",
        "Fair CPU sharing via time quantum. No starvation. Best for interactive systems. Has context-switch overhead."
    )
}

data class GanttEntry(
    val pid: Int,
    val name: String,
    val duration: Int,
    val type: ProcessType
)