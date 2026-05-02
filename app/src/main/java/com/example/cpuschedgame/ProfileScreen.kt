package com.example.cpuschedgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val easyBest   by viewModel.bestScoreForLevel(Level.Easy).collectAsState(initial = null)
    val mediumBest by viewModel.bestScoreForLevel(Level.Medium).collectAsState(initial = null)
    val hardBest   by viewModel.bestScoreForLevel(Level.Hard).collectAsState(initial = null)
    val gameCount  by viewModel.totalGamesForUser().collectAsState(initial = 0)
    val recentScores by viewModel.recentScoresForUser().collectAsState(initial = emptyList())

    val username = viewModel.currentUsername

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Subtle radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GoldenBright.copy(alpha = 0.05f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Text("← BACK", color = GoldenLight, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("PROFILE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Avatar + username card ────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar circle with first letter
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(GoldenBright.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, GoldenBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = GoldenBright, fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            "[ OS USER ]",
                            color = TextSecondary, fontSize = 10.sp, letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            username,
                            color = TextPrimary, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(GreenBright, CircleShape)
                            )
                            Text("$gameCount games played", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // ── Section header ────────────────────────────────
                Text(
                    "BEST SCORES BY LEVEL",
                    color = GoldenBright, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )

                // ── Level score cards ─────────────────────────────
                LevelScoreCard(
                    level       = Level.Easy,
                    bestScore   = easyBest,
                    accentColor = GreenBright,
                    label       = "Non-preemptive  •  FCFS / SJF / PRIORITY"
                )
                LevelScoreCard(
                    level       = Level.Medium,
                    bestScore   = mediumBest,
                    accentColor = InfoBlue,
                    label       = "Preemptive  •  FCFS-P / SJF-P / PRI-P / RR"
                )
                LevelScoreCard(
                    level       = Level.Hard,
                    bestScore   = hardBest,
                    accentColor = WarningOrange,
                    label       = "Hybrid preemptive  •  FCFS+SJF / FCFS+PRI / etc."
                )

                // ── Recent game history ───────────────────────────
                if (recentScores.isNotEmpty()) {
                    Text(
                        "RECENT GAMES",
                        color = GoldenBright, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                    )

                    recentScores.take(10).forEach { entry ->
                        RecentGameRow(entry)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LevelScoreCard(
    level: Level,
    bestScore: Int?,
    accentColor: Color,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(10.dp))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Level badge
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                level.displayName.take(3).uppercase(),
                color = accentColor, fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                level.displayName,
                color = TextPrimary, fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp
            )
        }

        // Best score display
        Column(horizontalAlignment = Alignment.End) {
            if (bestScore != null && bestScore > 0) {
                Text(
                    "$bestScore",
                    color = accentColor, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("pts", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
            } else {
                Text("—", color = DarkBorder, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("no plays", color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun RecentGameRow(entry: PlayerScore) {
    val ratingColor = when (entry.rating) {
        "S"  -> GoldenBright
        "A"  -> GreenBright
        "B"  -> InfoBlue
        "C"  -> WarningOrange
        else -> DangerRed
    }
    val levelColor = when (entry.level) {
        "Easy"   -> GreenBright
        "Medium" -> InfoBlue
        "Hard"   -> WarningOrange
        else     -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rating badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(ratingColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                .border(1.dp, ratingColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.rating, color = ratingColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.algorithm, color = GoldenLight, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
                Text("•", color = DarkBorder, fontSize = 11.sp)
                Text(entry.level, color = levelColor, fontSize = 11.sp)
            }
            Text(
                "${entry.completedProcesses} processes  •  ${entry.timeElapsed.toInt()}s",
                color = TextSecondary, fontSize = 10.sp
            )
        }

        Text(
            "${entry.score}",
            color = GoldenBright, fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}