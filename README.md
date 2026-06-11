# CPU Scheduling Game

An interactive Android game designed to help users learn and practice **CPU scheduling algorithms** through gameplay.

## Game Concept

Players act as a CPU scheduler. In each level, player can choose a difficulty level. Based on the difficulty level a random scheduling algorithm is selected, and different processes arrive with specific requirements. 
The player's task is to correctly assign processes to the CPU by creating the correct execution order in a Gantt chart.

The game aims to make operating system concepts more engaging through visualization and interaction.

## Technologies Used

- **Android Studio**
- **Kotlin**
- **Jetpack Compose**

## Game Level 

- Easy Level (Non-preemptive Algorithms - FCFS, Priority and  SJF)
- Medium Level (Preemptive Algorithms - Preemptive SJF, Preemptive Priority and Round Robin)
- Hard Level (Mix of both non-preemptive and preemptive algorithms)

## How to Play

With time passing, new processes will arrive in the waiting queue and by tapping the correct process according to the algorithm given, the player will move those processes to the executing queue.
The Gantt Chart will be created by the player's choices. For Preemptive round, after every time interval, player will get an option to either continue that ongoing process or select a new befitting process. 
By crossing 80% accuracy, players will be able to move on to the next level.

## Game Features

- Interactive Gantt chart creation
- Randomly generated process scenarios
- Real-time feedback on player decisions
- Time-limited challenges
- Score/star-based evaluation system
- Basic animations for process execution visualization

## Goal

To provide a fun and interactive way for students to understand CPU scheduling algorithms and improve their problem-solving skills.


## Development

This project is being developed as an educational Android application.
