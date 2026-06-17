package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Alarm
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Theme Specific Colors for Cosmic Deep vibe
val CosmicBackground = Color(0xFF0A071E)
val CosmicSurface = Color(0xFF131032)
val CosmicSubSurface = Color(0xFF1C1844)
val CosmicCyan = Color(0xFF0DFFF0)
val CosmicPink = Color(0xFFFF2B85)
val CosmicPurple = Color(0xFF9D4EDD)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ClockScreen(viewModel: ClockViewModel) {
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val worldClocks by viewModel.worldClocks.collectAsStateWithLifecycle()
    val alarms by viewModel.alarmsList.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()

    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var showWorldClockSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color.Transparent,
        bottomBar = {
            // Elegant Cosmic Floating Bottom Nav bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE0131032))
                    .border(1.dp, Color(0x330DFFF0), RoundedCornerShape(24.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        TabInfo("World", Icons.Filled.Language, Icons.Outlined.Language, 0),
                        TabInfo("Alarm", Icons.Filled.Alarm, Icons.Outlined.Alarm, 1),
                        TabInfo("Stopwatch", Icons.Filled.Timer, Icons.Outlined.Timer, 2),
                        TabInfo("Timer", Icons.Filled.HourglassFull, Icons.Outlined.HourglassEmpty, 3)
                    )

                    tabs.forEach { tab ->
                        val isSelected = viewModel.currentTab == tab.index
                        val color = if (isSelected) CosmicCyan else Color.White.copy(alpha = 0.5f)
                        val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "tab_scale")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .testTag("tab_${tab.title.lowercase()}")
                                .clickable { viewModel.currentTab = tab.index }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title,
                                tint = color,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.title,
                                color = color,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Header background radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1F0DFFF0), Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.25f),
                            radius = size.width * 0.8f
                        )
                    )
                }
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                },
                label = "tab_animation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ClockTabContent(
                        currentTime = currentTime,
                        worldClocks = worldClocks,
                        viewModel = viewModel,
                        onAddWorldClockClick = { showWorldClockSheet = true }
                    )
                    1 -> AlarmTabContent(
                        alarms = alarms,
                        viewModel = viewModel,
                        onAddAlarmClick = { showAddAlarmDialog = true }
                    )
                    2 -> StopwatchTabContent(
                        viewModel = viewModel,
                        laps = laps
                    )
                    3 -> TimerTabContent(
                        viewModel = viewModel
                    )
                }
            }
        }

        // Add Alarm Dialog
        if (showAddAlarmDialog) {
            AddAlarmDialog(
                onDismiss = { showAddAlarmDialog = false },
                onSave = { hour, minute, label, days ->
                    viewModel.addAlarm(hour, minute, label, days)
                    showAddAlarmDialog = false
                }
            )
        }

        // Add/Manage World Cities sheet/dialog
        if (showWorldClockSheet) {
            WorldClockSelectorDialog(
                worldClocks = worldClocks,
                onDismiss = { showWorldClockSheet = false },
                onTogglePin = { clock -> viewModel.togglePinWorldClock(clock) }
            )
        }
    }
}

data class TabInfo(
    val title: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val index: Int
)

// =========================================================================
// TAB 1: CLOCK & WORLD CLOCKS CONTENT
// =========================================================================
@Composable
fun ClockTabContent(
    currentTime: Date,
    worldClocks: List<ClockViewModel.WorldClock>,
    viewModel: ClockViewModel,
    onAddWorldClockClick: () -> Unit
) {
    val pinnedClocks = worldClocks.filter { it.isPinned }
    val localSdfTime = SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(currentTime)
    val localSdfAmPm = SimpleDateFormat("a", Locale.getDefault()).format(currentTime)
    val localSdfDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(currentTime)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 50.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cosmic Clock",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Local System Time",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Beautiful custom drawn Analog clock
            AnalogClock(
                currentTime = currentTime,
                modifier = Modifier
                    .size(230.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Large Digital Clock displaying seconds as a subtle indicator
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = localSdfTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = localSdfAmPm,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = localSdfDate,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // World Clocks Card list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "World Clocks",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddWorldClockClick,
                    modifier = Modifier.testTag("manage_cities_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add City",
                        tint = CosmicCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Cities", color = CosmicCyan, fontSize = 14.sp)
                }
            }
        }

        if (pinnedClocks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicSurface)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pinned world clocks.\nTap Add Cities to select world locations.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            items(pinnedClocks, key = { it.cityName }) { clock ->
                WorldClockRowItem(
                    clock = clock,
                    time = viewModel.getFormattedTimeForZone(clock.timeZoneId),
                    dateStr = viewModel.getFormattedDateForZone(clock.timeZoneId),
                    onUnpin = { viewModel.togglePinWorldClock(clock) }
                )
            }
        }
    }
}

@Composable
fun WorldClockRowItem(
    clock: ClockViewModel.WorldClock,
    time: String,
    dateStr: String,
    onUnpin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Color(0x1F0DFFF0), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clock.cityName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${clock.country} • $dateStr",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    color = CosmicCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier
                        .testTag("unpin_${clock.cityName.lowercase()}")
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove World Clock",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Custom Analog Clock Renderer using drawing brush metrics
@Composable
fun AnalogClock(currentTime: Date, modifier: Modifier = Modifier) {
    val calendar = Calendar.getInstance()
    calendar.time = currentTime

    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    // Sweep seconds calculation for extremely fluid hand tracking
    val sweepSecond = seconds + (millis / 1000f)
    val sweepMinute = minutes + (sweepSecond / 60f)
    val sweepHour = hours + (sweepMinute / 60f)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // 1. Sleek Shadow / Glowing outer ring
        drawCircle(
            color = Color(0x330DFFF0),
            center = center,
            radius = radius,
            style = Stroke(width = 3.dp.toPx())
        )

        // 2. Draw standard hour ticks
        for (i in 0 until 12) {
            val angle = i * 30f
            val isMain = i % 3 == 0
            val tickLen = (if (isMain) 12.dp else 6.dp).toPx()
            val strokeWidth = (if (isMain) 3.dp else 1.5.dp).toPx()
            val color = if (isMain) CosmicCyan else Color.White.copy(alpha = 0.4f)

            rotate(angle, center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, 10.dp.toPx()),
                    end = Offset(center.x, 10.dp.toPx() + tickLen),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Hour Hand
        val hourLen = radius * 0.5f
        rotate(sweepHour * 30f, center) {
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x, center.y - hourLen),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 4. Minute Hand
        val minLen = radius * 0.72f
        rotate(sweepMinute * 6f, center) {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = center,
                end = Offset(center.x, center.y - minLen),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Sweep Second Hand (Cosmic Accent color)
        val secLen = radius * 0.85f
        rotate(sweepSecond * 6f, center) {
            drawLine(
                color = CosmicPink,
                start = center,
                end = Offset(center.x, center.y - secLen),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 6. Pivot Center Cap
        drawCircle(
            color = Color.White,
            center = center,
            radius = 6.dp.toPx()
        )
        drawCircle(
            color = CosmicPink,
            center = center,
            radius = 3.dp.toPx()
        )
    }
}


// =========================================================================
// TAB 2: ALARMS CONTENT
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTabContent(
    alarms: List<Alarm>,
    viewModel: ClockViewModel,
    onAddAlarmClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 90.dp)
        ) {
            item {
                Text(
                    text = "Alarms",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Set daily or custom alarms",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (alarms.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "No alarms",
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No alarms scheduled",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap the plus button below to create one.",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRowItem(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarmEnabled(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }
        }

        // Floating add button, custom glowing Material Look
        FloatingActionButton(
            onClick = onAddAlarmClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 20.dp)
                .testTag("add_alarm_button"),
            containerColor = CosmicCyan,
            contentColor = CosmicBackground,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Alarm",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun AlarmRowItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteIcon by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.3.dp,
                color = if (alarm.isEnabled) Color(0x400DFFF0) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { showDeleteIcon = !showDeleteIcon },
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) CosmicSurface else CosmicSurface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.formattedTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alarm.label,
                        color = if (alarm.isEnabled) CosmicCyan else Color.White.copy(alpha = 0.3f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CosmicBackground,
                            checkedTrackColor = CosmicCyan,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarm.daysOfWeek,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )

                AnimatedVisibility(
                    visible = showDeleteIcon,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("alarm_delete_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alarm",
                            tint = CosmicPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


// =========================================================================
// TAB 3: STOPWATCH CONTENT
// =========================================================================
@Composable
fun StopwatchTabContent(
    viewModel: ClockViewModel,
    laps: List<ClockViewModel.Lap>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Stopwatch",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.sp
        )
        Text(
            text = "High precision chronometer",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Large high-precision ticker timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = viewModel.formatStopwatchTime(viewModel.stopwatchTime),
                fontSize = 58.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stopwatch actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Secondary button (Reset / Lap)
            IconButton(
                onClick = {
                    if (viewModel.stopwatchRunning) {
                        viewModel.lapStopwatch()
                    } else {
                        viewModel.resetStopwatch()
                    }
                },
                enabled = viewModel.stopwatchTime > 0,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (viewModel.stopwatchTime > 0) CosmicSurface else CosmicSurface.copy(alpha = 0.3f)
                    )
                    .border(
                        1.dp,
                        if (viewModel.stopwatchTime > 0) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                    .testTag("stopwatch_secondary_button")
            ) {
                Text(
                    text = if (viewModel.stopwatchRunning) "Lap" else "Reset",
                    color = if (viewModel.stopwatchTime > 0) Color.White else Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // Primary Play / Pause
            IconButton(
                onClick = {
                    if (viewModel.stopwatchRunning) {
                        viewModel.pauseStopwatch()
                    } else {
                        viewModel.startStopwatch()
                    }
                },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (viewModel.stopwatchRunning) CosmicPink else CosmicCyan)
                    .testTag("stopwatch_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.stopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (viewModel.stopwatchRunning) "Pause" else "Start",
                    tint = CosmicBackground,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Laps Header
        if (laps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LAP", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("LAP TIME", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("TOTAL TIME", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(laps, key = { it.lapNumber }) { lap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("#%02d", lap.lapNumber),
                            color = CosmicCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = viewModel.formatStopwatchTime(lap.lapTime),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = viewModel.formatStopwatchTime(lap.cumulativeTime),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            // Empty lap state decoration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No laps tracked yet",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 13.sp
                )
            }
        }
    }
}


// =========================================================================
// TAB 4: COUNTDOWN TIMER CONTENT
// =========================================================================
@Composable
fun TimerTabContent(viewModel: ClockViewModel) {
    // Temp States for visual dial picker
    var inputHours by remember { mutableStateOf(0L) }
    var inputMinutes by remember { mutableStateOf(10L) } // default 10 min
    var inputSeconds by remember { mutableStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition()
    val pulsateGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsate_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Timer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.sp
        )
        Text(
            text = "Accurate radial countdown",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (viewModel.timerStatus == ClockViewModel.TimerStatus.IDLE) {
            // Elegant Slider Picker setup
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%02d:%02d:%02d", inputHours, inputMinutes, inputSeconds),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Slider controls for hours, minutes, seconds
                TimerPickerSlider(
                    label = "Hours",
                    value = inputHours,
                    maxValue = 12,
                    onValueChange = { inputHours = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimerPickerSlider(
                    label = "Minutes",
                    value = inputMinutes,
                    maxValue = 59,
                    onValueChange = { inputMinutes = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimerPickerSlider(
                    label = "Seconds",
                    value = inputSeconds,
                    maxValue = 59,
                    onValueChange = { inputSeconds = it }
                )
            }
        } else {
            // Display Ring Countdown Timer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Percentage left
                val percentLeft = if (viewModel.timerInitialDuration > 0) {
                    viewModel.timerRemainingTime.toFloat() / viewModel.timerInitialDuration
                } else 0f

                val isFinished = viewModel.timerStatus == ClockViewModel.TimerStatus.FINISHED

                // Background Ring with custom drawn glow and visual colors
                Canvas(modifier = Modifier.size(240.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val r = size.width / 2 - 12.dp.toPx()

                    // Grey Base Ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        center = center,
                        radius = r,
                        style = Stroke(width = 8.dp.toPx())
                    )

                    // Active Countdown Arc
                    val arcColor = if (isFinished) CosmicPink else CosmicCyan
                    val arcGlowColor = if (isFinished) CosmicPink.copy(alpha = 0.3f) else CosmicCyan.copy(alpha = 0.3f)

                    // Optional outer glowing aura
                    drawCircle(
                        color = arcGlowColor.copy(alpha = arcGlowColor.alpha * pulsateGlow),
                        center = center,
                        radius = r + 4.dp.toPx() * pulsateGlow,
                        style = Stroke(width = 12.dp.toPx())
                    )

                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * percentLeft,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner countdown digital values
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (isFinished) {
                        Text(
                            text = "TIME'S UP!",
                            color = CosmicPink,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Timer finished",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = viewModel.formatTimerTime(viewModel.timerRemainingTime),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "remaining",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Timer actions layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalStartMs = (inputHours * 3600 + inputMinutes * 60 + inputSeconds) * 1000

            if (viewModel.timerStatus == ClockViewModel.TimerStatus.IDLE) {
                // Clear button
                IconButton(
                    onClick = {
                        inputHours = 0
                        inputMinutes = 0
                        inputSeconds = 0
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(CosmicSurface)
                        .testTag("timer_clear_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Clear", tint = Color.White)
                }

                // Start button
                IconButton(
                    onClick = {
                        viewModel.setTimerDuration(inputHours, inputMinutes, inputSeconds)
                        viewModel.startTimer()
                    },
                    enabled = totalStartMs > 0,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (totalStartMs > 0) CosmicCyan else CosmicCyan.copy(alpha = 0.3f))
                        .testTag("timer_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Timer",
                        tint = CosmicBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                val isFinished = viewModel.timerStatus == ClockViewModel.TimerStatus.FINISHED

                // Stop / Reset
                IconButton(
                    onClick = {
                        if (isFinished) {
                            viewModel.clearTimer()
                        } else {
                            viewModel.resetTimer()
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(CosmicSubSurface)
                        .testTag("timer_reset_button")
                ) {
                    Text(
                        text = if (isFinished) "Dismiss" else "Reset",
                        color = if (isFinished) CosmicPink else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isFinished) {
                    // Play / Pause toggle
                    val isRunning = viewModel.timerStatus == ClockViewModel.TimerStatus.RUNNING
                    IconButton(
                        onClick = {
                            if (isRunning) {
                                viewModel.pauseTimer()
                            } else {
                                viewModel.startTimer()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) CosmicPink else CosmicCyan)
                            .testTag("timer_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Resume",
                            tint = CosmicBackground,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerPickerSlider(
    label: String,
    value: Long,
    maxValue: Int,
    onValueChange: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text("$value", color = CosmicCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toLong()) },
            valueRange = 0f..maxValue.toFloat(),
            steps = if (maxValue > 12) maxValue - 1 else 11,
            colors = SliderDefaults.colors(
                thumbColor = CosmicCyan,
                activeTrackColor = CosmicCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("slider_${label.lowercase()}")
        )
    }
}


// =========================================================================
// FLOATING MODAL DIALOGS
// =========================================================================

// Custom Add Alarm Dialog using styled numbers
@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, days: String) -> Unit
) {
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("") }
    var isPm by remember { mutableStateOf(false) }

    // Day selections represented as boolean flags
    val days = remember { mutableStateMapOf(
        "Mon" to true, "Tue" to true, "Wed" to true, "Thu" to true, "Fri" to true, "Sat" to false, "Sun" to false
    ) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x330DFFF0), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Alarm",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Styled Picker row (Hours, Minutes, AM/PM toggle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours column control
                    NumberPicker(
                        value = hour,
                        range = 1..12,
                        label = "Hrs",
                        onValueChange = { hour = it }
                    )

                    Text(
                        text = ":",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Minutes column control
                    NumberPicker(
                        value = minute,
                        range = 0..59,
                        label = "Min",
                        onValueChange = { minute = it }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM/PM Column Selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AM",
                            color = if (!isPm) CosmicCyan else Color.White.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .testTag("select_am")
                                .clickable { isPm = false }
                                .padding(6.dp)
                        )
                        Text(
                            text = "PM",
                            color = if (isPm) CosmicCyan else Color.White.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .testTag("select_pm")
                                .clickable { isPm = true }
                                .padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Label Text Field
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("Alarm Label (e.g. Wake Up)", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = CosmicCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_label_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Days selector row
                Text(
                    "Repeat on",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    daysList.forEach { dayName ->
                        val active = days[dayName] ?: false
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (active) CosmicCyan else CosmicSubSurface)
                                .clickable { days[dayName] = !active }
                                .testTag("day_picker_$dayName"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName.first().toString(),
                                color = if (active) CosmicBackground else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_alarm_button")) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var actualHour = if (hour == 12) 0 else hour
                            if (isPm) {
                                actualHour += 12
                            }
                            val finalLabel = if (label.isNotBlank()) label else "Alarm"

                            // Format active days
                            val activeDaysList = days.filter { it.value }.keys.toList()
                            val daysStr = when {
                                activeDaysList.size == 7 -> "Every day"
                                activeDaysList.size == 5 && !activeDaysList.contains("Sat") && !activeDaysList.contains("Sun") -> "Weekdays"
                                activeDaysList.size == 2 && activeDaysList.contains("Sat") && activeDaysList.contains("Sun") -> "Weekends"
                                activeDaysList.isEmpty() -> "Once"
                                else -> activeDaysList.joinToString(", ")
                            }

                            onSave(actualHour, minute, finalLabel, daysStr)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier.testTag("save_alarm_button")
                    ) {
                        Text("Save", color = CosmicBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick = {
                val nextValue = if (value == range.last) range.first else value + 1
                onValueChange(nextValue)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ArrowDropUp, contentDescription = "Up", tint = CosmicCyan)
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .background(CosmicSubSurface, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = {
                val prevValue = if (value == range.first) range.last else value - 1
                onValueChange(prevValue)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Down", tint = CosmicCyan)
        }

        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Medium
        )
    }
}

// Custom World Clock multi-selection Dialog
@Composable
fun WorldClockSelectorDialog(
    worldClocks: List<ClockViewModel.WorldClock>,
    onDismiss: () -> Unit,
    onTogglePin: (clock: ClockViewModel.WorldClock) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x330DFFF0), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "World Cities",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(worldClocks) { clock ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTogglePin(clock) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = clock.cityName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = clock.country,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Checkbox(
                                checked = clock.isPinned,
                                onCheckedChange = { onTogglePin(clock) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CosmicCyan,
                                    checkmarkColor = CosmicBackground,
                                    uncheckedColor = Color.White.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("pin_checkbox_${clock.cityName.lowercase()}")
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
