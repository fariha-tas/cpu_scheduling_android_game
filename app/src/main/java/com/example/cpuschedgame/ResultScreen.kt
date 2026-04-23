package com.example.cpuschedgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

@Composable
fun ResultScreen(
    score: Int,
    completedCount: Int,
    timeElapsed: Float,
    algorithm: SchedulingAlgorithm,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (ratingLabel, ratingColor) = when {
        score >= 800 -> "S" to GoldenBright
        score >= 600 -> "A" to GreenBright
        score >= 400 -> "B" to InfoBlue
        score >= 200 -> "C" to WarningOrange
        else         -> "D" to DangerRed
    }

    val pulseBadge by rememberInfiniteTransition(label = "badge").animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "badgeScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(ratingColor.copy(alpha = 0.06f), Color.Transparent),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("GAME OVER", color = TextSecondary, fontSize = 12.sp, letterSpacing = 4.sp)

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .scale(pulseBadge)
                    .background(ratingColor.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
                    .border(2.dp, ratingColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(ratingLabel, color = ratingColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", color = GoldenBright, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text("POINTS", color = TextSecondary, fontSize = 11.sp, letterSpacing = 4.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("$completedCount", "COMPLETED", GreenBright)
                StatItem("${timeElapsed.toInt()}s", "TIME", InfoBlue)
                StatItem(algorithm.shortName, "ALGORITHM", GoldenLight)
            }

            // Algorithm lesson
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    algorithm.displayName.replace("\n", " "),
                    color = GoldenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    algorithm.detail,
                    color = TextSecondary, fontSize = 11.sp,
                    lineHeight = 17.sp, textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = TextPrimary)
            ) {
                Text("▶  PLAY AGAIN", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("⌂  HOME", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun ResultPreview() {
    com.example.cpuschedgame.ui.theme.CPUSchedGameTheme {
        ResultScreen(
            score = 720, completedCount = 8, timeElapsed = 65f,
            algorithm = SchedulingAlgorithm.SJF,
            onPlayAgain = {}, onHome = {}
        )
    }
}