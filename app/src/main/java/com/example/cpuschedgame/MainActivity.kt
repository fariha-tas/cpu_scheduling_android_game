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
import androidx.lifecycle.ViewModelProvider
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
                // Use AndroidViewModelFactory (Branch 1) so DB/auth works
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
    NavHost(navController = navController, startDestination = "splash") {

        // ── Splash screen (Branch 1) ───────────────────────────────
        composable("splash") {
            CpuAnimation(
                onSplashComplete = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // ── Auth screens (Branch 1) ────────────────────────────────
        composable("login") {
            LoginScreen(
                authError = vm.authError,
                onLogin = { username, password ->
                    vm.login(username, password) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onGoToSignup = { navController.navigate("signup") }
            )
        }

        composable("signup") {
            SignupScreen(
                authError = vm.authError,
                onSignup = { username, password ->
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

        // ── Main screens ───────────────────────────────────────────
        composable("home") {
            HomeScreen(
                onStartGameClick = {
                    // Branch 2: go to level select first
                    navController.navigate("levelselect") { popUpTo("home") }
                },
                onHowToPlayClick = { navController.navigate("howtoplay") }
            )
        }

        // ── Level Select (Branch 2) ────────────────────────────────
        composable("levelselect") {
            var selectedLevel by remember { mutableStateOf(Level.Easy) }
            LevelSelectScreen(
                selectedLevel = selectedLevel,
                onSelectLevel = { level -> selectedLevel = level },
                onStartGame = {
                    navController.navigate("game/${selectedLevel.name}") {
                        popUpTo("home")
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("howtoplay") {
            HowToPlayScreen(onBackClick = { navController.popBackStack() })
        }

        // ── Game (Branch 2 route with level arg) ───────────────────
        composable(
            route = "game/{level}",
            arguments = listOf(navArgument("level") { type = NavType.StringType })
        ) { backStackEntry ->
            val levelName = backStackEntry.arguments?.getString("level")
                ?: Level.Easy.name
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
                    vm.resetLevel()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // ── Result (Branch 2 with retry / next-level) ──────────────
        composable("result") {
            ResultScreen(
                score = vm.score,
                isGameWon = vm.isGameWon,
                completedCount = vm.completedProcesses.size,
                correctPicks = vm.correctPicks,
                wrongPicks = vm.wrongPicks,
                timeElapsed = vm.wallTime,
                algorithm = vm.assignedAlgorithm ?: SchedulingAlgorithm.FCFS,
                onPlayNextLevel = {
                    vm.incrementLevel()
                    navController.navigate("game/${vm.selectedLevel.name}") {
                        popUpTo("result") { inclusive = true }
                    }
                },
                onRetry = {
                    vm.replayLevel()
                    navController.navigate("game/${vm.selectedLevel.name}") {
                        popUpTo("result") { inclusive = true }
                    }
                },
                onHome = {
                    vm.resetLevel()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}