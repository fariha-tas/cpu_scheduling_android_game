package com.example.cpuschedgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                    vm.startGame()
                    navController.navigate("game") { popUpTo("home") }
                },
                onHowToPlayClick = { navController.navigate("howtoplay") }
            )
        }

        composable("howtoplay") {
            HowToPlayScreen(onBackClick = { navController.popBackStack() })
        }

        composable("game") {
            GameScreen(
                viewModel = vm,
                onGameOver = {
                    navController.navigate("result") {
                        popUpTo("game") { inclusive = true }
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
                algorithm      = vm.assignedAlgorithm,
                onPlayAgain = {
                    vm.startGame()
                    navController.navigate("game") { popUpTo("home") }
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