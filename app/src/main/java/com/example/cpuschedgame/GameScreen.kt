package com.example.cpuschedgame

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onGameOver: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(viewModel.isGameOver) {
        if (viewModel.isGameOver) {
            delay(600)
            onGameOver()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            GameHUD(
                score    = viewModel.score,
                lives    = viewModel.lives,
                time     = viewModel.wallTime,
                algo     = viewModel.assignedAlgorithm,
                isPaused = viewModel.isPaused,
                onPause  = viewModel::togglePause,
                onBack   = { viewModel.stopGame(); onBackClick() }
            )
            CPUPanel(viewModel.runningProcess, viewModel.cpuBurstProgress, viewModel.cpuRemainingBurst)
            ProcessQueuePanel(
                waitingProcesses = viewModel.waitingProcesses,
                pendingProcesses = viewModel.pendingProcesses,
                algorithm        = viewModel.assignedAlgorithm,
                optimalPid       = viewModel.getOptimalPid(),
                lastWrongPid     = viewModel.lastWrongPid,
                onSchedule       = viewModel::scheduleProcess,
                modifier         = Modifier.weight(1f)
            )
            GanttPanel(viewModel.ganttChart)
        }

        // Algorithm intro overlay
        if (viewModel.showAlgoIntro) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg.copy(alpha = 0.93f))
                    .clickable { viewModel.dismissAlgoIntro() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .border(1.dp, GoldenBright.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    Text("YOUR ALGORITHM", color = TextSecondary, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(
                        viewModel.assignedAlgorithm.shortName,
                        color = GoldenBright, fontSize = 36.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        viewModel.assignedAlgorithm.displayName.replace("\n", " "),
                        color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        viewModel.assignedAlgorithm.detail,
                        color = TextSecondary, fontSize = 12.sp,
                        lineHeight = 18.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "★ Watch for the star — it marks the optimal pick!",
                        color = GoldenLight, fontSize = 11.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.dismissAlgoIntro() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = TextPrimary)
                    ) {
                        Text("▶  LET'S GO", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Pause overlay
        if (viewModel.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg.copy(alpha = 0.88f))
                    .clickable { viewModel.togglePause() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .border(1.dp, GoldenBright.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(24.dp)
                ) {
                    Text("⏸  PAUSED", color = GoldenBright, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        viewModel.assignedAlgorithm.displayName.replace("\n", " "),
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        viewModel.assignedAlgorithm.detail,
                        color = TextSecondary, fontSize = 11.sp,
                        lineHeight = 17.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("TAP ANYWHERE TO RESUME", color = TextSecondary, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

// ── HUD ──────────────────────────────────────────────────────────

@Composable
private fun GameHUD(
    score: Int, lives: Int, time: Float,
    algo: SchedulingAlgorithm, isPaused: Boolean,
    onPause: () -> Unit, onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(6.dp)) {
            Text("✕", color = DangerRed, fontSize = 16.sp)
        }

        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SCORE", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
            Text("$score", color = GoldenBright, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LIVES", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
            Row {
                repeat(3) { i ->
                    Text(
                        if (i < lives) "♥" else "♡",
                        color = if (i < lives) DangerRed else TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TIME", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
            Text("${time.toInt()}s", color = TextPrimary, fontSize = 14.sp)
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .background(DarkCard, RoundedCornerShape(4.dp))
                .border(1.dp, GoldenBright.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(algo.shortName, color = GoldenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onPause, contentPadding = PaddingValues(6.dp)) {
            Text(if (isPaused) "▶" else "⏸", color = TextPrimary, fontSize = 16.sp)
        }
    }
}

// ── CPU Panel ────────────────────────────────────────────────────

@Composable
private fun CPUPanel(running: Process?, progress: Float, remainingBurst: Int) {
    val animProgress by animateFloatAsState(progress, tween(250), label = "cpu")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(10.dp)
            .background(DarkCard, RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (running != null) GreenAccent.copy(alpha = 0.18f) else DarkBorder.copy(alpha = 0.2f),
                    RoundedCornerShape(6.dp)
                )
                .border(
                    1.dp,
                    if (running != null) GreenBright else DarkBorder,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "CPU",
                color = if (running != null) GreenBright else TextSecondary,
                fontSize = 9.sp, fontWeight = FontWeight.Bold
            )
        }

        Column(Modifier.weight(1f)) {
            if (running != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeBadge(running.type)
                    Text(
                        running.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text("P${running.priority}", color = TextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BURST PROGRESS", color = TextSecondary, fontSize = 9.sp)
                    Text("${remainingBurst}/${running.burstTime} units", color = TextSecondary, fontSize = 9.sp)
                }
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(7.dp)
                        .background(DarkBg, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProgress).fillMaxHeight()
                            .background(GreenBright, RoundedCornerShape(4.dp))
                    )
                }
            } else {
                Text("IDLE", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Waiting for a process…", color = TextSecondary.copy(alpha = 0.45f), fontSize = 11.sp)
            }
        }

        if (running != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text("PID", color = TextSecondary, fontSize = 9.sp)
                Text("#${running.pid}", color = GoldenLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Process Queue Panel ──────────────────────────────────────────

@Composable
private fun ProcessQueuePanel(
    waitingProcesses: List<Process>,
    pendingProcesses: List<Process>,
    algorithm: SchedulingAlgorithm,
    optimalPid: Int?,
    lastWrongPid: Int?,
    onSchedule: (Process) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "READY QUEUE",
                color = TextSecondary, fontSize = 10.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${waitingProcesses.size} waiting",
                    color = if (waitingProcesses.size >= 6) WarningOrange else TextSecondary,
                    fontSize = 10.sp
                )
                if (pendingProcesses.isNotEmpty()) {
                    Text(
                        "${pendingProcesses.size} incoming",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (waitingProcesses.isEmpty() && pendingProcesses.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth().weight(1f)
                    .background(DarkCard.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("All processes completed!", color = GreenBright, fontSize = 11.sp)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Waiting (tappable)
                waitingProcesses.forEach { proc ->
                    val isOptimal = proc.pid == optimalPid
                    val isWrong = proc.pid == lastWrongPid
                    ProcessCard(
                        process   = proc,
                        isOptimal = isOptimal,
                        isWrong   = isWrong,
                        isPending = false,
                        onSchedule = { onSchedule(proc) }
                    )
                }
                // Pending (grayed, not tappable)
                pendingProcesses.forEach { proc ->
                    ProcessCard(
                        process    = proc,
                        isOptimal  = false,
                        isWrong    = false,
                        isPending  = true,
                        onSchedule = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessCard(
    process: Process,
    isOptimal: Boolean,
    isWrong: Boolean,
    isPending: Boolean,
    onSchedule: () -> Unit
) {
    val waitFraction = if (process.maxWaitTime > 0f)
        (process.waitTimer / process.maxWaitTime).coerceIn(0f, 1f) else 0f
    val urgency = 1f - waitFraction

    val dlColor by animateColorAsState(
        when {
            isPending       -> TextSecondary.copy(alpha = 0.4f)
            urgency > 0.55f -> GreenBright
            urgency > 0.25f -> WarningOrange
            else            -> DangerRed
        }, tween(400), label = "dl"
    )
    val dlAnim by animateFloatAsState(urgency, tween(180), label = "dlBar")

    val borderColor = when {
        isWrong   -> DangerRed
        isOptimal -> GoldenBright.copy(alpha = 0.75f)
        isPending -> DarkBorder.copy(alpha = 0.4f)
        else      -> DarkBorder
    }
    val borderWidth = if (isWrong || isOptimal) 1.5.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isWrong   -> DangerRed.copy(alpha = 0.10f)
                    isPending -> DarkCard.copy(alpha = 0.4f)
                    else      -> DarkCard
                }
            )
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .then(if (!isPending) Modifier.clickable(onClick = onSchedule) else Modifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TypeBadge(process.type, dimmed = isPending)

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    process.name,
                    color = if (isPending) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                when {
                    isPending -> Text("⏳", fontSize = 10.sp)
                    isWrong   -> Text("✗", color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    isOptimal -> Text("★", color = GoldenBright, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("⚡${process.burstTime}")
                Chip("P${process.priority}")
                Chip("#${process.pid}")
                if (isPending) Chip("@T${process.arrivalTime}")
            }
            if (!isPending) {
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .fillMaxWidth().height(4.dp)
                        .background(DarkBg, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(dlAnim).fillMaxHeight()
                            .background(dlColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        if (!isPending) {
            Column(horizontalAlignment = Alignment.End) {
                Text("TTL", color = TextSecondary, fontSize = 8.sp)
                val remaining = ((process.maxWaitTime - process.waitTimer)).toInt().coerceAtLeast(0)
                Text(
                    "${remaining}s",
                    color = dlColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Gantt Chart ──────────────────────────────────────────────────

@Composable
private fun GanttPanel(entries: List<GanttEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text("GANTT CHART", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        if (entries.isEmpty()) {
            Text("No completed processes yet", color = TextSecondary.copy(alpha = 0.4f), fontSize = 10.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                items(entries) { e ->
                    val c = typeColor(e.type)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width((e.duration * 26).dp).height(28.dp)
                                .background(c.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                                .border(1.dp, c.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "P${e.pid}", color = c, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, maxLines = 1
                            )
                        }
                        Text("${e.startTime}", color = TextSecondary.copy(alpha = 0.5f), fontSize = 7.sp)
                    }
                }
                // Final time label
                if (entries.isNotEmpty()) {
                    item {
                        Column {
                            Spacer(Modifier.height(28.dp))
                            Text(
                                "${entries.last().endTime}",
                                color = TextSecondary.copy(alpha = 0.5f), fontSize = 7.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Shared small composables ─────────────────────────────────────

@Composable
fun TypeBadge(type: ProcessType, dimmed: Boolean = false) {
    val c = typeColor(type).let { if (dimmed) it.copy(alpha = 0.3f) else it }
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(c.copy(alpha = 0.13f), RoundedCornerShape(6.dp))
            .border(1.dp, c.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(type.label, color = c, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Chip(text: String) {
    Text(
        text, color = TextSecondary, fontSize = 10.sp,
        modifier = Modifier
            .background(DarkBg, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

fun typeColor(type: ProcessType): Color = when (type) {
    ProcessType.IO     -> ProcessIO
    ProcessType.CPU    -> ProcessCPU
    ProcessType.SYSTEM -> ProcessSystem
}