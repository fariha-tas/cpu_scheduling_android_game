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
                onStartGameClick = { navController.navigate("select_algorithm") },
                onHowToPlayClick  = { navController.navigate("howtoplay") }
            )
        }

        composable("howtoplay") {
            HowToPlayScreen(onBackClick = { navController.popBackStack() })
        }

        composable("select_algorithm") {
            AlgorithmSelectScreen(
                selectedAlgorithm  = vm.selectedAlgorithm,
                onAlgorithmSelected = vm::selectAlgorithm,
                onStartGame = {
                    vm.startGame()
                    navController.navigate("game") { popUpTo("home") }
                },
                onBackClick = { navController.popBackStack() }
            )
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
                score               = vm.score,
                completedCount      = vm.completedProcesses.size,
                timeElapsed         = vm.timeElapsed,
                algorithm           = vm.selectedAlgorithm,
                onPlayAgain = {
                    navController.navigate("select_algorithm") { popUpTo("home") }
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