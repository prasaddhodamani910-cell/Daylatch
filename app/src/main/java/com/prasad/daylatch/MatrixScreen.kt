package com.prasad.daylatch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasad.daylatch.data.Habit
import com.prasad.daylatch.data.HabitLog
import com.prasad.daylatch.data.HabitStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Wantui Theme Colors
private val BG = Color(0xFFFEFEFE)
private val FG = Color(0xFF423D53)
private val PRIMARY = Color(0xFFA5EDA8)
private val PRIMARY_TEXT = Color(0xFF3F8445)
private val TERTIARY = Color(0xFFD0EAED)
private val TERTIARY_TEXT = Color(0xFF637A7D)
private val ACCENT = Color(0xFFFEDFE9)
private val ACCENT_TEXT = Color(0xFFB45974)
private val MUTED = Color(0xFFF5F4F8)
private val MUTED_FG = Color(0xFF6E687C)
private val BORDER = Color(0xFFD7D2E1)
private val SECONDARY = Color(0xFFEAE8F4)
private val CARD = Color(0xFFFFFFFF)

private val CELL_SIZE = 30.dp
private val NAME_WIDTH = 140.dp

@Composable
fun MatrixScreen(
    currentMonth: YearMonth,
    todayDate: LocalDate,
    habits: List<Habit>,
    logs: List<HabitLog>,
    onToggle: (Habit, LocalDate) -> Unit,
    onOpen: (Habit) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val daysInMonth = currentMonth.lengthOfMonth()

    val logsMap = logs.groupBy { it.habitId }.mapValues { (_, habitLogs) ->
        habitLogs.associateBy { LocalDate.parse(it.date) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        // Month Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}".uppercase(),
                    color = PRIMARY_TEXT,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Your month at a glance",
                    color = FG,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today is editable · past Tap cells to track · tap names for stats future days are locked",
                    color = MUTED_FG,
                    fontSize = 12.sp
                )
            }
            Row {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Text("<", color = FG, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Text(">", color = FG, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }

        // Legend Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot(PRIMARY, "Done")
            LegendDot(ACCENT, "Skipped")
            LegendDot(MUTED, "Upcoming")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Matrix Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = CARD,
            shadowElevation = 4.dp,
            tonalElevation = 0.dp
        ) {
            Column {
                // Scrollable content
                Row(modifier = Modifier.weight(1f)) {
                    // Sticky left column (habit names)
                    Column(
                        modifier = Modifier
                            .width(NAME_WIDTH)
                            .verticalScroll(verticalScrollState)
                    ) {
                        // Header placeholder
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .fillMaxWidth()
                                .background(SECONDARY)
                                .padding(start = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "HABIT",
                                color = FG,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Habit name rows
                        habits.forEach { habit ->
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .fillMaxWidth()
                                    .clickable { onOpen(habit) }
                                    .drawBehind {
                                        drawLine(
                                            BORDER,
                                            Offset(0f, size.height),
                                            Offset(size.width, size.height),
                                            strokeWidth = 1f
                                        )
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = habit.name,
                                    color = FG,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Summary footer label
                        if (habits.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .fillMaxWidth()
                                    .background(MUTED)
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "SCORE",
                                    color = MUTED_FG,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Scrollable right area (days grid)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState)
                    ) {
                        // Day header row
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .background(SECONDARY)
                        ) {
                            for (day in 1..daysInMonth) {
                                val date = currentMonth.atDay(day)
                                val isToday = date == todayDate
                                Column(
                                    modifier = Modifier
                                        .width(CELL_SIZE)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = date.dayOfWeek.getDisplayName(
                                            TextStyle.SHORT,
                                            Locale.getDefault()
                                        ).take(1),
                                        color = if (isToday) PRIMARY_TEXT else MUTED_FG,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$day",
                                        color = if (isToday) PRIMARY_TEXT else FG,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Habit rows
                        habits.forEach { habit ->
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .drawBehind {
                                        drawLine(
                                            BORDER,
                                            Offset(0f, size.height),
                                            Offset(size.width, size.height),
                                            strokeWidth = 1f
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (day in 1..daysInMonth) {
                                    val date = currentMonth.atDay(day)
                                    val log = logsMap[habit.id]?.get(date)
                                    val status = log?.status ?: HabitStatus.PENDING

                                    val targetColor = when (status) {
                                        HabitStatus.COMPLETED -> PRIMARY
                                        HabitStatus.SKIPPED -> ACCENT
                                        HabitStatus.FAILED -> Color(0xFFD87993)
                                        HabitStatus.PENDING -> MUTED
                                    }
                                    val animatedColor by animateColorAsState(
                                        targetValue = targetColor,
                                        animationSpec = tween(200),
                                        label = "cell"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(CELL_SIZE)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(animatedColor)
                                            .then(if (date == todayDate) Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToggle(habit, date)
                                                } else Modifier)
                                            ,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (status) {
                                            HabitStatus.COMPLETED -> Text(
                                                "✓",
                                                color = PRIMARY_TEXT,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            HabitStatus.SKIPPED -> Text(
                                                "–",
                                                color = ACCENT_TEXT,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            HabitStatus.FAILED -> Text(
                                                "✗",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            HabitStatus.PENDING -> Text(
                                                "□",
                                                color = MUTED_FG.copy(alpha = 0.4f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Summary footer row (daily completion %)
                        if (habits.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .background(MUTED),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (day in 1..daysInMonth) {
                                    val date = currentMonth.atDay(day)
                                    val completed = habits.count { habit ->
                                        logsMap[habit.id]?.get(date)?.status == HabitStatus.COMPLETED
                                    }
                                    val pct = if (habits.isNotEmpty()) (completed * 100) / habits.size else 0

                                    Box(
                                        modifier = Modifier.width(CELL_SIZE),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (pct > 0) "$pct" else "·",
                                            color = if (pct >= 80) PRIMARY_TEXT
                                            else if (pct >= 50) TERTIARY_TEXT
                                            else MUTED_FG,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = MUTED_FG, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
