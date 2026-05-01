package com.example.cpuschedgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
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
                val gameViewModel: GameViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory
                        .getInstance(application)
                )
                AppNavHost(navController, gameViewModel)
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, vm: GameViewModel) {
    NavHost(
        navController    = navController,
        startDestination = "splash"
    ) {

        // ── Splash screen ─────────────────────────────────────────

        composable("splash") {
            CpuAnimation(
                onSplashComplete = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // ── Auth screens ──────────────────────────────────────────

        composable("login") {
            LoginScreen(
                authError   = vm.authError,
                onLogin     = { username, password ->
                    vm.login(username, password) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onGoToSignup = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            SignupScreen(
                authError = vm.authError,
                onSignup  = { username, password ->
                    vm.signup(username, password) {
                        navController.navigate("login") {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                },
                onGoToLogin = {
                    vm.clearAuthError()
                    navController.popBackStack()
                }
            )
        }

        // ── Main screens ──────────────────────────────────────────

        composable("home") {
            HomeScreen(
                onStartGameClick = { navController.navigate("game_start") },
                onHowToPlayClick = { navController.navigate("howtoplay") }
            )
        }

        composable("howtoplay") {
            HowToPlayScreen(onBackClick = { navController.popBackStack() })
        }

        composable("game_start") {
            LaunchedEffect(Unit) {       // ← fixed: runs only once
                vm.startGame()
            }
            GameScreen(
                viewModel  = vm,
                onGameOver = {
                    navController.navigate("result") {
                        popUpTo("game_start") { inclusive = true }
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
                completedCount = vm.completedProcesses.size,
                timeElapsed    = vm.wallTime,
                algorithm      = vm.assignedAlgorithm,
                isWon          = vm.isGameWon,
                correctPicks   = vm.correctPicks,
                wrongPicks     = vm.wrongPicks,
                onPlayAgain = {
                    navController.navigate("game_start") {
                        popUpTo("home")
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