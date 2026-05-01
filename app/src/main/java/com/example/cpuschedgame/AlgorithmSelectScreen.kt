package com.example.cpuschedgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

@Composable
fun AlgorithmSelectScreen(
    selectedAlgorithm: SchedulingAlgorithm,
    onAlgorithmSelected: (SchedulingAlgorithm) -> Unit,
    onStartGame: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top bar
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
            Text("SELECT ALGORITHM", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Choose how processes are scheduled. Each algorithm has different optimal play.",
                color = TextSecondary, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            SchedulingAlgorithm.values().forEach { algo ->
                val selected = algo == selectedAlgorithm
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) DarkCard else DarkSurface)
                        .border(
                            width  = if (selected) 1.5.dp else 1.dp,
                            color  = if (selected) GoldenBright else DarkBorder,
                            shape  = RoundedCornerShape(8.dp)
                        )
                        .clickable { onAlgorithmSelected(algo) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Short name badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (selected) GoldenBright.copy(alpha = 0.12f) else DarkCard,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) GoldenBright else DarkBorder,
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            algo.shortName,
                            color = if (selected) GoldenBright else TextSecondary,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            algo.displayName.replace("\n", " "),
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            algo.description,
                            color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }

                    if (selected) Text("✓", color = GoldenBright, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom start button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Button(
                onClick = onStartGame,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent, contentColor = TextPrimary
                )
            ) {
                Text(
                    "▶  START  [ ${selectedAlgorithm.shortName} ]",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun AlgorithmSelectPreview() {
    com.example.cpuschedgame.ui.theme.CPUSchedGameTheme {
        AlgorithmSelectScreen(
            selectedAlgorithm = SchedulingAlgorithm.SJF_NP,
            onAlgorithmSelected = {}, onStartGame = {}, onBackClick = {}
        )
    }
}