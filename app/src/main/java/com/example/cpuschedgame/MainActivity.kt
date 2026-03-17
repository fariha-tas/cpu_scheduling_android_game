package com.example.cpuschedgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpuschedgame.ui.theme.CPUSchedGameTheme
import com.example.cpuschedgame.ui.theme.Golden
import com.example.cpuschedgame.ui.theme.Green
import com.example.cpuschedgame.ui.theme.Purple40

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPUSchedGameTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onHowToPlayClick = { navController.navigate("howtoplay")},
                            onStartGameClick = { navController.navigate("game") }
                            //All button clicks for homescreen
                        )
                    }
                    composable("howtoplay") {
                        HowToPlayScreen(
                            onBackClick = { navController.navigate("home") }
                            //All button clicks for howtoplay screen
                        )
                    }
                    composable("game") {
                        //GameScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onHowToPlayClick: () -> Unit, onStartGameClick: () -> Unit) {

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Golden, // pick from color.kt
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "CPU Scheduling Game",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green
                )
            }

            Spacer(modifier = Modifier.height(100.dp))

            Button(
                onClick = onStartGameClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = Golden)
            )
            {
                Text(text = "Start Game", color = Golden, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick =  onHowToPlayClick ,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = Golden)
            )
            {
                Text(text = "How to Play", fontSize = 20.sp )
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun HomeScreenPreview() {
    CPUSchedGameTheme {
        HomeScreen(
            onHowToPlayClick = {},
            onStartGameClick = {}
        )
    }
}