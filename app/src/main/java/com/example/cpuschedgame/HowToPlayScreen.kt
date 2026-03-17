package com.example.cpuschedgame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.CPUSchedGameTheme
import com.example.cpuschedgame.ui.theme.Golden
import com.example.cpuschedgame.ui.theme.Green
import com.example.cpuschedgame.ui.theme.Purple40

@Composable
fun HowToPlayScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "How to Play", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "1. Select a scheduling algorithm\n2. Drag processes to CPU queue\n3. Observe the Gantt chart\n4. Score points by efficient scheduling",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(60.dp))
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Golden,
                contentColor = Green
            )
        )
        {
            Text(text = "Home", fontSize = 20.sp)
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun HowToPlayScreenPreview() {
    CPUSchedGameTheme {
        HowToPlayScreen(
            onBackClick = {}
        )
    }
}