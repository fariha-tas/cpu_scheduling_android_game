package com.example.cpuschedgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

@Composable
fun HomeScreen(
    onStartGameClick: () -> Unit,
    onHowToPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.75f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Subtle radial glow behind title
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GreenAccent.copy(alpha = 0.09f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "[ OS SIMULATION v1.0 ]",
                color = TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp
            )

            Spacer(Modifier.height(20.dp))

            // Title card
            Box(
                modifier = Modifier
                    .alpha(pulse)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldenBright, Color.Transparent)
                        ),
                        RoundedCornerShape(10.dp)
                    )
                    .background(DarkCard, RoundedCornerShape(10.dp))
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "CPU", fontSize = 52.sp, fontWeight = FontWeight.Bold,
                        color = GreenBright, letterSpacing = 10.sp
                    )
                    Text(
                        "SCHEDULER", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = GoldenBright, letterSpacing = 4.sp
                    )
                    Text(
                        "GAME", fontSize = 13.sp,
                        color = TextSecondary, letterSpacing = 6.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Status row
            Row(
                modifier = Modifier
                    .background(DarkCard, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(GreenBright, CircleShape)
                )
                Text("SYSTEM READY", color = GreenBright, fontSize = 11.sp)
                Text("|", color = DarkBorder, fontSize = 11.sp)
                Text("4 ALGORITHMS", color = TextSecondary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(60.dp))

            Button(
                onClick = onStartGameClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent, contentColor = TextPrimary
                )
            ) {
                Text("▶  START GAME", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onHowToPlayClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Golden),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenLight)
            ) {
                Text("?  HOW TO PLAY", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(48.dp))

            Text(
                "FCFS  •  SJF  •  PRIORITY  •  ROUND ROBIN",
                color = TextSecondary, fontSize = 10.sp,
                letterSpacing = 1.sp, textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun HomeScreenPreview() {
    com.example.cpuschedgame.ui.theme.CPUSchedGameTheme {
        HomeScreen(onStartGameClick = {}, onHowToPlayClick = {})
    }
}