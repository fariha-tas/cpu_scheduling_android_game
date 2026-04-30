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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val level = viewModel.selectedLevel
    val context = LocalContext.current
    val optimalPid by remember { derivedStateOf { viewModel.getOptimalPid() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            GameHUD(
                score         = viewModel.score,
                lives         = viewModel.lives,
                schedulingTime = viewModel.schedulingTime,
                algo          = viewModel.assignedAlgorithm,
                isPaused      = viewModel.isPaused,
                onPause       = viewModel::togglePause,
                onBack        = { viewModel.stopGame(); onBackClick() }
            )

            AlgoHintBar(algorithm = viewModel.assignedAlgorithm)

            CPUPanel(
                running       = viewModel.runningProcess,
                progress      = viewModel.cpuBurstProgress,
                remainingBurst = viewModel.cpuRemainingBurst
            )

            ReadyQueuePanel(
                processes    = viewModel.waitingProcesses,
                optimalPid   = optimalPid,
                wrongPid     = viewModel.lastWrongPid,
                cpuBusy      = viewModel.runningProcess != null,
                onSchedule = { process ->
                    viewModel.scheduleProcess(process, context)
                },
                modifier     = Modifier.weight(1f)
            )

            if (viewModel.pendingProcesses.isNotEmpty()) {
                IncomingPanel(
                    processes      = viewModel.pendingProcesses,
                    schedulingTime = viewModel.schedulingTime
                )
            }

            GanttPanel(viewModel.ganttChart)
        }

        // ── Algorithm Intro Overlay ───────────────────────────────
        AnimatedVisibility(
            visible = viewModel.showAlgoIntro,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            AlgoIntroOverlay(
                algorithm = viewModel.assignedAlgorithm,
                currLevelNo = viewModel.currLevelNo,
                onDismiss = viewModel::dismissAlgoIntro
            )
        }

        // ── Pause Overlay ─────────────────────────────────────────
        if (viewModel.isPaused && !viewModel.showAlgoIntro) {
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
                        viewModel.assignedAlgorithm.hint,
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

// ── Algorithm Intro Overlay ───────────────────────────────────────

@Composable
private fun AlgoIntroOverlay(
    algorithm: SchedulingAlgorithm,
    currLevelNo: Int,
    onDismiss: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg.copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(28.dp)
                .background(DarkCard, RoundedCornerShape(16.dp))
                .border(1.5.dp, GoldenBright.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(200.dp, 55.dp)
                    .background(DangerRed.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .border(2.dp, DangerRed, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Level - $currLevelNo", color = DangerRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(2.dp))
            Text("YOUR ALGORITHM", color = TextSecondary, fontSize = 10.sp, letterSpacing = 3.sp)
            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(72.dp)
                    .background(GoldenBright.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .border(2.dp, GoldenBright, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(algorithm.shortName, color = GoldenBright, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                algorithm.displayName.replace("\n", " "),
                color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = DarkBorder, thickness = 1.dp)

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())){
                // What to do & Penalty Reminder
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                GreenAccent.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                GreenAccent.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "WHAT TO DO",
                            color = GreenBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            algorithm.hint,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        algorithm.detail,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Penalty box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                DangerRed.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                DangerRed.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠", color = DangerRed, fontSize = 14.sp)

                        Text(
                            "Wrong pick or Expired Process = −1 ❤ & −50 pts",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldenBright, contentColor = DarkBg)
            ) {
                Text("▶  START SCHEDULING", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

// ── HUD ──────────────────────────────────────────────────────────

@Composable
private fun GameHUD(
    score: Int, lives: Int, schedulingTime: Int,
    algo: SchedulingAlgorithm, isPaused: Boolean,
    onPause: () -> Unit, onBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { showExitDialog = true },
            contentPadding = PaddingValues(6.dp)) {
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
                        color = if (i < lives) DangerRed else DarkBorder,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Scheduling clock
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CPU CLK", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
            Text("T=$schedulingTime", color = InfoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .background(GoldenBright.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .border(1.dp, GoldenBright.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(algo.shortName, color = GoldenLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onPause, contentPadding = PaddingValues(6.dp)) {
            Text(if (isPaused) "▶" else "⏸", color = TextPrimary, fontSize = 16.sp)
        }
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text("Exit Game?")
            },
            text = {
                Text("Do you want to go back? Game progress will be lost.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()   // actually go back
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false // Close dialog
                    }
                ) {
                    Text("No")
                }
            }
        )
    }
}

// ── Algorithm Hint Bar ────────────────────────────────────────────

@Composable
private fun AlgoHintBar(algorithm: SchedulingAlgorithm) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GoldenBright.copy(alpha = 0.06f))
            .border(
                width = 0.dp,
                color = Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {

        // First line: hint
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("★", color = GoldenBright, fontSize = 12.sp)

            Text(
                algorithm.hintShort,
                color = GoldenLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Second line: penalty text
        Text(
            "Wrong Pick = −♥ and -50 Score",
            color = DangerRed.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}

// ── CPU Panel ────────────────────────────────────────────────────

@Composable
private fun CPUPanel(running: Process?, progress: Float, remainingBurst: Int) {
    val animProgress by animateFloatAsState(progress, tween(200), label = "cpu")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .background(DarkCard, RoundedCornerShape(8.dp))
            .border(1.dp, if (running != null) GreenBright.copy(alpha = 0.4f) else DarkBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (running != null) GreenAccent.copy(alpha = 0.2f) else DarkBorder.copy(alpha = 0.15f),
                    RoundedCornerShape(6.dp)
                )
                .border(1.dp, if (running != null) GreenBright else DarkBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("CPU", color = if (running != null) GreenBright else TextSecondary,
                fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.weight(1f)) {
            if (running != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeBadge(running.type)
                    Text(
                        running.name, color = TextPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text("P${running.priority}", color = TextSecondary, fontSize = 10.sp)
                    Text("AT:${running.arrivalTime}", color = InfoBlue.copy(alpha = 0.7f), fontSize = 10.sp)
                }
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EXECUTING  (non-preemptive)", color = TextSecondary, fontSize = 9.sp)
                    Text("$remainingBurst / ${running.burstTime} units left", color = TextSecondary, fontSize = 9.sp)
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
                Text("Tap a process below to schedule it →", color = TextSecondary.copy(alpha = 0.45f), fontSize = 11.sp)
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

// ── Ready Queue Panel ─────────────────────────────────────────────

@Composable
private fun ReadyQueuePanel(
    processes: List<Process>,
    optimalPid: Int?,
    wrongPid: Int?,
    cpuBusy: Boolean,
    onSchedule: (Process) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "READY QUEUE",
                color = GreenBright, fontSize = 10.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "${processes.size} waiting",
                color = if (processes.size >= 5) WarningOrange else TextSecondary,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(6.dp))

        when {
            processes.isEmpty() && cpuBusy -> {
                Box(
                    Modifier
                        .fillMaxWidth().weight(1f)
                        .background(DarkCard.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚙", color = GreenBright, fontSize = 24.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("CPU is executing a process…", color = TextSecondary, fontSize = 11.sp)
                        Text("Incoming processes will appear here.", color = TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
            processes.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxWidth().weight(1f)
                        .background(DarkCard.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No processes yet — incoming soon…", color = TextSecondary, fontSize = 11.sp)
                }
            }
            else -> {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    processes.forEach { proc ->
                        ProcessCard(
                            process    = proc,
                            isOptimal  = proc.pid == optimalPid,
                            isWrong    = proc.pid == wrongPid,
                            cpuBusy    = cpuBusy,
                            onSchedule = { onSchedule(proc) }
                        )
                    }
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
    cpuBusy: Boolean,
    onSchedule: () -> Unit
) {
    val urgency  = (process.waitTimer / process.maxWaitTime).coerceIn(0f, 1f)
    val urgencyColor by animateColorAsState(
        when {
            urgency < 0.5f -> GreenBright
            urgency < 0.75f -> WarningOrange
            else -> DangerRed
        }, tween(400), label = "urgency"
    )
    val urgencyAnim by animateFloatAsState(1f - urgency, tween(180), label = "urgBar")

    val shake by animateFloatAsState(
        if (isWrong) 4f else 0f,
        tween(if (isWrong) 100 else 300), label = "shake"
    )

    val borderColor = when {
        isWrong -> DangerRed
        else    -> DarkBorder
    }
    val borderWidth = if (isWrong) 3.dp else 1.dp
    val bgColor     = when {
        isWrong -> DangerRed.copy(alpha = 0.22f)
        else    -> DarkCard
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shake.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .then(if (!cpuBusy) Modifier.clickable(onClick = onSchedule) else Modifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TypeBadge(process.type)

        Column(Modifier.weight(1f)) {
            // Process name + optimal star
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    process.name,
                    color = if (cpuBusy) TextSecondary else TextPrimary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isWrong) Text("✗", color = DangerRed, fontSize = 18.sp)
            }

            Spacer(Modifier.height(4.dp))

            // Info chips: AT | BT | Priority
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                InfoChip("AT:${process.arrivalTime}", InfoBlue)
                InfoChip("BT:${process.burstTime}", GreenBright)
                InfoChip("P${process.priority}", GoldenLight)
                InfoChip("#${process.pid}", TextSecondary)
            }

            Spacer(Modifier.height(5.dp))

            // Wait expiry bar (counts down)
            Box(
                Modifier
                    .fillMaxWidth().height(4.dp)
                    .background(DarkBg, RoundedCornerShape(2.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(urgencyAnim).fillMaxHeight()
                        .background(urgencyColor, RoundedCornerShape(2.dp))
                )
            }
        }

        // Time-to-expire
        val remaining = ((process.maxWaitTime - process.waitTimer)).coerceAtLeast(0f)
        Column(horizontalAlignment = Alignment.End) {
            Text("EXP", color = TextSecondary, fontSize = 8.sp)
            Text(
                "${remaining.toInt()}s",
                color = urgencyColor,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Incoming (Pending) Panel ──────────────────────────────────────

@Composable
private fun IncomingPanel(processes: List<Process>, schedulingTime: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            "INCOMING  (not yet arrived)",
            color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp
        )
        Spacer(Modifier.height(5.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(processes.sortedBy { it.arrivalTime }) { p ->
                val arrivedIn = p.arrivalTime - schedulingTime
                Column(
                    modifier = Modifier
                        .width(86.dp)
                        .background(DarkCard.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        .padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val c = typeColor(p.type)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(c.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                                .border(1.dp, c.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(p.type.label, color = c.copy(alpha = 0.5f), fontSize = 6.sp, fontWeight = FontWeight.Bold) }
                        Text(
                            p.name, color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "AT T=${p.arrivalTime} (+$arrivedIn)",
                        color = InfoBlue.copy(alpha = 0.6f), fontSize = 8.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("BT:${p.burstTime}", color = TextSecondary.copy(alpha = 0.4f), fontSize = 8.sp)
                        Text("P${p.priority}", color = GoldenLight.copy(alpha = 0.4f), fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

// ── Gantt Chart ───────────────────────────────────────────────────

@Composable
private fun GanttPanel(entries: List<GanttEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text("GANTT CHART", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(5.dp))
        if (entries.isEmpty()) {
            Text("No completed processes yet", color = TextSecondary.copy(alpha = 0.35f), fontSize = 10.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries) { e ->
                    val c = typeColor(e.type)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width((e.duration * 24 + 12).dp).height(24.dp)
                                .background(c.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                .border(1.dp, c.copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P${e.pid}", color = c, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${e.startTime}–${e.endTime}", color = TextSecondary.copy(alpha = 0.45f), fontSize = 7.sp)
                    }
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────

@Composable
fun TypeBadge(type: ProcessType) {
    val c = typeColor(type)
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
fun InfoChip(text: String, color: Color = TextSecondary) {
    Text(
        text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

// Keep for backward compat
@Composable
fun Chip(text: String) = InfoChip(text)

fun typeColor(type: ProcessType): Color = when (type) {
    ProcessType.IO     -> ProcessIO
    ProcessType.CPU    -> ProcessCPU
    ProcessType.SYSTEM -> ProcessSystem
}