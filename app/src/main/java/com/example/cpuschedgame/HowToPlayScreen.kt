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
    Instruction(
        "01", "YOU GET A RANDOM ALGORITHM",
        "Each game the system randomly assigns FCFS, SJF, or Priority Scheduling. " +
                "An intro screen tells you exactly which one you got and what it means before you start."
    ),
    Instruction(
        "02", "UNDERSTAND THE SCHEDULING CLOCK",
        "The CPU Clock (T=) at the top is not real time — it jumps forward by a process's burst time " +
                "whenever that process finishes. Processes arrive when T reaches their Arrival Time (AT)."
    ),
    Instruction(
        "03", "READ PROCESS CARDS",
        "Each card shows: AT (Arrival Time) — when it arrived, BT (Burst Time) — how long it needs the CPU, " +
                "P (Priority) — urgency level 1–5 where P1 is most important, and EXP — seconds before it expires."
    ),
    Instruction(
        "04", "TAP THE CORRECT PROCESS",
        "Tap a card to send it to the CPU. The golden ★ marks the optimal pick for your algorithm. " +
                "FCFS → lowest AT  |  SJF → lowest BT  |  Priority → lowest P number (P1 first)."
    ),
    Instruction(
        "05", "WRONG PICK = LOSE A LIFE",
        "If you pick a process that isn't the algorithm's correct choice, you lose −1 life and −50 points. " +
                "The process still runs (non-preemptive — no interruptions!), but the penalty stings."
    ),
    Instruction(
        "06", "DON'T LET PROCESSES EXPIRE",
        "Every waiting process has an expiry timer (EXP bar). If it hits zero before you schedule it, " +
                "you lose −1 life and −50 points. High-priority processes expire faster — don't ignore P1!"
    ),
    Instruction(
        "07", "INCOMING PROCESSES",
        "Some processes appear in the INCOMING strip at the bottom — they haven't arrived yet. " +
                "Their AT value is greater than the current CPU Clock. Keep an eye on what's coming."
    ),
    Instruction(
        "08", "READ THE GANTT CHART",
        "The bar at the very bottom shows every completed process with its start and end time (e.g. 0–5). " +
                "Use it to review whether you followed your algorithm's order correctly."
    )
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
            // Algorithm quick reference
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldenBright.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                    .border(1.dp, GoldenBright.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("QUICK REFERENCE", color = GoldenBright, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                AlgoRef("FCFS", "Pick LOWEST Arrival Time (AT)")
                AlgoRef("SJF",  "Pick LOWEST Burst Time (BT)")
                AlgoRef("PRI",  "Pick LOWEST Priority number (P1 first)")
            }

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
                        Text(
                            item.title, color = GoldenLight, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(item.body, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AlgoRef(algo: String, rule: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(GoldenBright.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(algo, color = GoldenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(rule, color = TextSecondary, fontSize = 11.sp)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun HowToPlayPreview() {
    com.example.cpuschedgame.ui.theme.CPUSchedGameTheme {
        HowToPlayScreen(onBackClick = {})
    }
}