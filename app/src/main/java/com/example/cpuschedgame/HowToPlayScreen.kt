package com.example.cpuschedgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

private data class Instruction(val num: String, val title: String, val body: String)

private val instructions = listOf(
    Instruction("01", "SELECT ALGORITHM",
        "Choose FCFS, SJF, Priority, or Round Robin. Each changes how you score and which process you should schedule first."),
    Instruction("02", "WATCH THE QUEUE",
        "Processes appear with a countdown deadline bar. If a process expires before you schedule it, you lose a life and points."),
    Instruction("03", "TAP TO SCHEDULE",
        "Tap any process card to send it to the CPU. The star (★) marks the optimal pick for your chosen algorithm."),
    Instruction("04", "EARN BONUS POINTS",
        "You score more when you schedule early (high remaining deadline) and follow your algorithm's ideal order."),
    Instruction("05", "READ THE GANTT CHART",
        "The bar at the bottom records every completed process. Use it to review your scheduling decisions."),
    Instruction("06", "SURVIVE!",
        "You start with 3 lives ♥♥♥. Every expired process costs one life and 50 points. Game over when lives hit zero.")
)

@Composable
fun HowToPlayScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
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
            Text("HOW TO PLAY", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            instructions.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Number badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GreenAccent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .border(1.dp, GreenAccent, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.num, color = GreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(item.title, color = GoldenLight, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(item.body, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun HowToPlayPreview() {
    com.example.cpuschedgame.ui.theme.CPUSchedGameTheme {
        HowToPlayScreen(onBackClick = {})
    }
}