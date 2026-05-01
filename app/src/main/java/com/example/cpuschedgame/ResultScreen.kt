package com.example.cpuschedgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

@Composable
fun ResultScreen(
    score: Int,
    isGameWon: Boolean = false,
    completedCount: Int,
    correctPicks: Int,
    wrongPicks: Int,
    timeElapsed: Float,
    algorithm: SchedulingAlgorithm,
    onPlayNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPicks = correctPicks + wrongPicks
    val accuracy = if (totalPicks > 0) (correctPicks * 100 / totalPicks) else 0

    // Rating uses accuracy as primary key (Branch 2 improvement over Branch 1's score-only rating)
    val (ratingLabel, ratingColor) = when {
        accuracy >= 90 && score >= 600 -> "S" to GoldenBright
        accuracy >= 75 || score >= 450 -> "A" to GreenBright
        accuracy >= 60 || score >= 300 -> "B" to InfoBlue
        accuracy >= 40 || score >= 150 -> "C" to WarningOrange
        else                           -> "D" to DangerRed
    }

    val pulseBadge by rememberInfiniteTransition(label = "badge").animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine),
            RepeatMode.Reverse),
        label = "badgeScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Radial glow behind rating color
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Win / Lose header
            Text(
                if (isGameWon) "MISSION COMPLETE" else "GAME OVER",
                color = if (isGameWon) GreenBright else TextSecondary,
                fontSize = 12.sp, letterSpacing = 4.sp
            )

            // Pulsing rating badge
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .scale(pulseBadge)
                    .background(ratingColor.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
                    .border(2.dp, ratingColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(ratingLabel, color = ratingColor, fontSize = 42.sp,
                    fontWeight = FontWeight.Bold)
            }

            // Score display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", color = GoldenBright, fontSize = 56.sp,
                    fontWeight = FontWeight.Bold)
                Text("POINTS", color = TextSecondary, fontSize = 11.sp, letterSpacing = 4.sp)
            }

            // Main stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("$completedCount", "DONE", GreenBright)
                StatItem("${timeElapsed.toInt()}s", "TIME", InfoBlue)
                StatItem(algorithm.shortName, "ALGO", GoldenLight)
            }

            // Accuracy panel (Branch 2 improvement)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "SCHEDULING ACCURACY",
                    color = TextSecondary, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val accColor = when {
                        accuracy >= 75 -> GreenBright
                        accuracy >= 50 -> WarningOrange
                        else           -> DangerRed
                    }
                    Text("$accuracy%", color = accColor, fontSize = 26.sp,
                        fontWeight = FontWeight.Bold)

                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth().height(8.dp)
                                .background(DarkBg, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(accuracy / 100f).fillMaxHeight()
                                    .background(accColor, RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("✓ $correctPicks correct", color = GreenBright, fontSize = 10.sp)
                            Text("✗ $wrongPicks wrong", color = DangerRed, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Algorithm lesson box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp),
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

            // Next Level button — locked if accuracy < 80 % (Branch 2)
            val canProgress = accuracy >= 80
            Button(
                onClick = { if (canProgress) onPlayNextLevel() },
                enabled = canProgress,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent, contentColor = TextPrimary,
                    disabledContainerColor = DarkCard, disabledContentColor = TextSecondary
                )
            ) {
                Text(
                    if (canProgress) "▶ Next Level"
                    else "▶ Next Level (need 80% accuracy)",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }

            // Retry / Home row (Branch 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange)
                ) {
                    Text("↺ Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("⌂ Home", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
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