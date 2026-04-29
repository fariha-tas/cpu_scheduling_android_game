package com.example.cpuschedgame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cpuschedgame.ui.theme.CPUSchedGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPUSchedGameTheme {
                val navController = rememberNavController()
                val gameViewModel: GameViewModel = viewModel()
                AppNavHost(navController, gameViewModel)
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, vm: GameViewModel) {
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onStartGameClick = {
                    navController.navigate("levelselect") { popUpTo("home") }
                },
                onHowToPlayClick = { navController.navigate("howtoplay") }
            )
        }

        composable("levelselect"){
            var selectedLevel by remember { mutableStateOf(Level.Easy) }

            LevelSelectScreen(
                selectedLevel = selectedLevel,
                onSelectLevel = { level ->
                    selectedLevel = level
                },
                onStartGame = {
                    navController.navigate("game/${selectedLevel.name}") {
                        popUpTo("home")
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("howtoplay") {
            HowToPlayScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = "game/{level}",
            arguments = listOf(
                navArgument("level") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val levelName = backStackEntry.arguments?.getString("level") ?: Level.Easy.name
            val level = Level.valueOf(levelName)

            LaunchedEffect(level) {
                vm.startGame(level)
            }

            GameScreen(
                viewModel = vm,
                onGameOver = {
                    navController.navigate("result") {
                        popUpTo("game/{level}") { inclusive = true }
                    }
                },
                onBackClick = {
                    vm.stopGame()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("result") {
            ResultScreen(
                score          = vm.score,
                isGameWon      = vm.isGameWon,
                completedCount = vm.completedProcesses.size,
                correctPicks   = vm.correctPicks,
                wrongPicks     = vm.wrongPicks,
                timeElapsed    = vm.wallTime,
                algorithm = vm.assignedAlgorithm ?: SchedulingAlgorithm.FCFS,
                onPlayAgain = {
                    navController.navigate("game/${vm.selectedLevel.name}") {
                        popUpTo("result") { inclusive = true }
                    }
                },
                onHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}