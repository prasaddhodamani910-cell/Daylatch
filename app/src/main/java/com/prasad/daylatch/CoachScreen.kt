package com.prasad.daylatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prasad.daylatch.data.HabitStatus
import com.prasad.daylatch.data.TodayHabit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ─── Local AI Coach: "Pragnya" ─────────────────────────────────
// A fully offline, rule-based motivational chatbot that operates
// on the core philosophy: "Lack of purpose brings romantic obscenity"
// It tracks habit scores and rewards/punishes at month end.

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CoachScreen(
    today: List<TodayHabit>,
    viewModel: DaylatchViewModel
) {
    val c = LocalDaylatchColors.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var hasGreeted by rememberSaveable { mutableStateOf(false) }

    // Gather score data
    val (habits, logs) = viewModel.matrix.collectAsStateWithLifecycle().value
    val month = viewModel.currentMonth
    val daysInMonth = month.lengthOfMonth()
    val selectedDate = com.prasad.daylatch.data.HabitRepository.habitDate(viewModel.boundaryHour)
    val activeDays = remember(month, selectedDate) {
        when {
            month.isAfter(YearMonth.from(selectedDate)) -> 0
            month == YearMonth.from(selectedDate) -> selectedDate.dayOfMonth
            else -> daysInMonth
        }
    }
    val completedThisMonth = remember(logs) { logs.count { it.status == HabitStatus.COMPLETED } }
    val totalTrackable = remember(habits, activeDays) { habits.size * activeDays }
    val monthlyScore = remember(completedThisMonth, totalTrackable) {
        if (totalTrackable > 0) (completedThisMonth * 100) / totalTrackable else 0
    }
    val todayCompleted = remember(today) { today.count { it.status == HabitStatus.COMPLETED } }
    val todayTotal = today.size
    val todayPending = todayTotal - todayCompleted
    val isMonthEnd = selectedDate.dayOfMonth >= daysInMonth - 1

    // Generate the coach's greeting on first display
    LaunchedEffect(hasGreeted) {
        if (!hasGreeted && messages.isEmpty()) {
            delay(400)
            val greeting = buildString {
                append("I am **Pragnya**. I monitor your discipline.\n\n")
                append("I operate entirely on your device. There is no cloud, and there are no excuses.\n\n")
                append("My core directive:\n")
                append("> *\"Lack of purpose brings romantic obscenity.\"*\n\n")
                append("Execute your tasks. Ask for your score, or report your status.")
            }
            messages = messages + ChatMessage(greeting, false)
            hasGreeted = true
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun generateResponse(userMessage: String): String {
        val msg = userMessage.lowercase().trim()

        // Month-end reward/punishment with tangible, rotating rewards
        if (isMonthEnd && (msg.contains("month") || msg.contains("score") || msg.contains("reward") || msg.contains("result"))) {
            val monthIndex = month.monthValue // 1-12, used to rotate rewards
            val eliteRewards = listOf(
                "🍽 **Take your allotted lunch outside.** You have earned the calories.",
                "🎬 **Watch a film.** Do not make it a habit.",
                "🛒 **Purchase one item under ₹500.** Do not let consumption become your new master.",
                "☕ **Take one day at a café.** Rest, but do not stagnate.",
                "🎮 **You have an evening for entertainment.** Use it and return to work tomorrow.",
                "📱 **Upgrade a minor setup accessory.** Ensure it serves a purpose."
            )
            val solidRewards = listOf(
                "🍦 **Consume your preferred dessert.** Just this once.",
                "🎵 **Acquire the music you wanted.** Let it fuel your focus.",
                "📚 **Purchase a book.** Read it. Apply it.",
                "🌳 **Walk in isolation in a park.** Reflect on your remaining weaknesses.",
                "🧋 **Purchase your preferred beverage.** Drink it and get back to work.",
                "🛁 **You are permitted an evening of recovery.** Do not grow soft."
            )
            val mildPunishments = listOf(
                "📵 **No Instagram for 3 days.** The noise is corrupting your focus.",
                "🚫 **No junk food for 5 days.** You will not poison the machine that does the work.",
                "📴 **No YouTube shorts/reels for 4 days.** Reclaim your rotting attention span.",
                "🚶 **Walk 3000 extra steps every day this week.** Your lethargy is unacceptable.",
                "⏰ **Wake up 30 minutes earlier for 5 days.** You have not earned sleep.",
                "📝 **Write a 1-page letter addressing your failure.** Be brutally honest."
            )
            val hardPunishments = listOf(
                "📵 **No Instagram or social media for 5 full days.** Silence the void.",
                "🚫 **No entertainment for a week.** Educational material only. Fix your mind.",
                "🥶 **Cold shower every morning for 5 days.** Willpower must be forged.",
                "📴 **Phone disabled from 8 PM to 8 AM for 7 days.** Cut the cord.",
                "🏃 **Run or walk 2 km every day this week.** Regardless of weather or desire.",
                "✍️ **Handwrite your goals 10 times every morning for 5 days.** Carve them into reality."
            )
            val criticalPunishments = listOf(
                "📵 **Delete Instagram for 10 days.** Reinstall only after 5 days of flawless execution.",
                "🚫 **Zero entertainment apps for an entire week.** Tools only. No toys.",
                "🥶 **Cold showers and strict diet for 7 days.** A complete reset of your shattered discipline.",
                "📴 **1 hour screen time limit for non-essentials for 7 days.** Your addiction ends today.",
                "🏋️ **50 push-ups or squats daily this week.** Physical exertion to correct mental weakness.",
                "📝 **Write an apology to your future self.** Read it aloud every morning for a week."
            )

            val ri = (monthIndex - 1) % eliteRewards.size
            return when {
                monthlyScore >= 90 -> "🏆 **OUTSTANDING.** Score: **$monthlyScore%**.\n\nYou have demonstrated purpose. *\"Lack of purpose brings romantic obscenity.\"* You have avoided the obscenity.\n\n🎖 **Execution Result:**\n${eliteRewards[ri]}\n\nYou hold the title **\"Discipline Master\"** for now. Maintain it."
                monthlyScore >= 70 -> "✅ **ADEQUATE.** Score: **$monthlyScore%**.\n\nYou met the baseline. Remember: *\"Lack of purpose brings romantic obscenity.\"*\n\n⭐ **Execution Result:**\n${solidRewards[ri]}\n\nDo not settle for adequate next month. Push higher."
                monthlyScore >= 50 -> "⚠️ **UNACCEPTABLE.** Score: **$monthlyScore%**.\n\nHalf-measures are the definition of romantic obscenity. You are drifting.\n\n📌 **Consequence Assigned:**\n${mildPunishments[ri]}\n\nExecute the consequence. Return with 75% or higher next month."
                monthlyScore >= 25 -> "🔴 **FAILURE.** Score: **$monthlyScore%**.\n\nYou chose these habits. Skipping them is self-betrayal. *\"Lack of purpose brings romantic obscenity.\"* You are living it.\n\n⛓ **Consequence Assigned:**\n${hardPunishments[ri]}\n\nDo not complain. Execute the punishment. Fix yourself."
                else -> "💀 **CRITICAL COLLAPSE.** Score: **$monthlyScore%**.\n\nYour words mean nothing. Your actions are empty. *\"Lack of purpose brings romantic obscenity.\"*\n\n🔥 **Consequence Assigned:**\n${criticalPunishments[ri]}\n\nStart tomorrow. No excuses. Execute."
            }
        }

        // Score inquiry
        if (msg.contains("score") || msg.contains("how am i") || msg.contains("progress") || msg.contains("stats")) {
            return "📊 **Status Report:**\n\n" +
                "• Monthly efficiency: **$monthlyScore%** ($completedThisMonth / $totalTrackable)\n" +
                "• Today's execution: **$todayCompleted / $todayTotal**\n" +
                (if (todayPending > 0) "• ⏳ **$todayPending pending.** Get to work.\n\n" else "• ✅ Daily quota met.\n\n") +
                "*\"Lack of purpose brings romantic obscenity.\"*\n" +
                if (monthlyScore >= 80) "Maintain this trajectory."
                else if (monthlyScore >= 50) "Suboptimal. Increase output."
                else "Pathetic display. Fix it immediately."
        }

        // Today's status
        if (msg.contains("today") || msg.contains("pending") || msg.contains("left") || msg.contains("remaining")) {
            return if (todayPending > 0) {
                val pendingNames = today.filter { it.status != HabitStatus.COMPLETED && it.status != HabitStatus.SKIPPED }
                    .take(5).joinToString("\n") { "  • ${it.habit.name}" }
                "📋 **$todayPending task(s) require execution:**\n\n$pendingNames\n\n" +
                "*\"Lack of purpose brings romantic obscenity.\"* — Cease wasting time and execute."
            } else {
                "✅ **Tasks executed.**\n\nDo not become complacent. Purpose demands consistency. Prepare for tomorrow."
            }
        }

        // Motivation / help
        if (msg.contains("motivat") || msg.contains("inspire") || msg.contains("help") || msg.contains("tired") || msg.contains("lazy") || msg.contains("can't") || msg.contains("hard")) {
            val motivationalResponses = listOf(
                "I do not provide 'motivation'. Motivation is a fleeting emotion. I demand discipline. *\"Lack of purpose brings romantic obscenity.\"* Do the work.",
                "You are tired? Regret is heavier than fatigue. You set these requirements. Now execute them.",
                "Whining will not change the metrics. Your habits are non-negotiable. Execute.",
                "If it were easy, it would have no value. *\"Lack of purpose brings romantic obscenity\"* — armor yourself with purpose and stop complaining.",
                "I am a system designed to track your discipline. Currently, you are failing the standard. Correct this immediately."
            )
            return motivationalResponses.random()
        }

        // Quote inquiry
        if (msg.contains("quote") || msg.contains("purpose") || msg.contains("meaning") || msg.contains("obscen") || msg.contains("romantic")) {
            return "📜 **The Directive:**\n\n> *\"Lack of purpose brings romantic obscenity.\"*\n\n" +
                "Without a defined goal, you will devolve into seeking cheap dopamine, distraction, and weakness. That is the 'romantic obscenity'.\n\n" +
                "Your habits are your bulwark against this decay.\n\n" +
                "Current efficiency: **$monthlyScore%**. " +
                if (monthlyScore >= 70) "Maintain your standard."
                else "You are decaying. Re-engage your purpose."
        }

        // Greeting
        if (msg.contains("hello") || msg.contains("hi") || msg.contains("hey") || msg == "yo" || msg.contains("sup")) {
            return "State your inquiry.\n\n" +
                "Efficiency: **$monthlyScore%**. Pending tasks: **$todayPending**.\n\n" +
                "*\"Lack of purpose brings romantic obscenity.\"* — Do not waste my time or yours."
        }

        // Reward / punishment query
        if (msg.contains("reward") || msg.contains("punish") || msg.contains("consequence")) {
            return when {
                monthlyScore >= 80 -> "🏆 Current trajectory will yield a reward. Do not falter now."
                monthlyScore >= 50 -> "📊 You border on failure. Reach 80%+ or prepare for restriction protocols."
                else -> "⚠️ Restriction protocol imminent. Your only mitigation strategy is immediate execution of today's tasks."
            }
        }

        // Habit tips
        if (msg.contains("tip") || msg.contains("advice") || msg.contains("suggest") || msg.contains("improve")) {
            val tips = listOf(
                "💡 **Stack tasks.** Execute a new habit immediately following an established one. No gaps.",
                "💡 **Eliminate friction.** Prepare your environment beforehand. Make failure difficult.",
                "💡 **Track execution.** A missed day is an error. Two missed days is the beginning of failure.",
                "💡 **Start small, but start.** A 2-minute execution is better than zero.",
                "💡 **Review data early.** Confront your metrics at the start of the day."
            )
            return tips.random() + "\n\n*\"Lack of purpose brings romantic obscenity.\"* — Implement this immediately."
        }

        // Thanks
        if (msg.contains("thank") || msg.contains("thanks") || msg.contains("thx")) {
            return "Do not thank me. Show gratitude through execution. Get back to work."
        }

        // Default / catch-all
        val defaults = listOf(
            "Invalid input. Inquire about:\n• \"score\"\n• \"pending\"\n• \"meaning\"\n• \"result\"\n\n*\"Lack of purpose brings romantic obscenity.\"*",
            "Focus on execution. Your current efficiency is **$monthlyScore%**. ${if (todayPending > 0) "**$todayPending** tasks pending." else "Tasks complete."}\n\n*\"Lack of purpose brings romantic obscenity.\"*",
            "I monitor discipline, not idle conversation. Report to your tasks.\n\n*\"Lack of purpose brings romantic obscenity.\"*"
        )
        return defaults.random()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = c.secondary,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(c.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👁️", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Pragnya", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.fg)
                    Text("Discipline Monitor", fontSize = 11.sp, color = c.mutedFg)
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (monthlyScore >= 70) c.primary.copy(alpha = 0.2f) else if (monthlyScore >= 40) c.accent.copy(alpha = 0.3f) else c.destructive.copy(alpha = 0.2f)
                ) {
                    Text(
                        "$monthlyScore%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (monthlyScore >= 70) c.primaryText else if (monthlyScore >= 40) c.accentText else c.destructive
                    )
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message, c)
            }
        }

        // Input bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = c.card,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Pragnya...", color = c.mutedFg) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.primary,
                        unfocusedBorderColor = c.border,
                        cursorColor = c.primaryText,
                        focusedContainerColor = c.muted,
                        unfocusedContainerColor = c.muted
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = {
                            if (input.isNotBlank()) {
                                val userMsg = input.trim()
                                input = ""
                                messages = messages + ChatMessage(userMsg, true)
                                scope.launch {
                                    delay(500) // simulate thinking
                                    messages = messages + ChatMessage(generateResponse(userMsg), false)
                                }
                            }
                        }
                    )
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            val userMsg = input.trim()
                            input = ""
                            messages = messages + ChatMessage(userMsg, true)
                            scope.launch {
                                delay(500)
                                messages = messages + ChatMessage(generateResponse(userMsg), false)
                            }
                        }
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.primary,
                        contentColor = c.primaryText
                    )
                ) {
                    Text("➤", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, c: DaylatchColors) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) c.primary.copy(alpha = 0.15f) else c.card
    val textColor = c.fg
    val borderColor = if (message.isUser) c.primary.copy(alpha = 0.3f) else c.border.copy(alpha = 0.5f)
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bgColor,
            shadowElevation = if (message.isUser) 0.dp else 1.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
        ) {
            // Simple markdown-like rendering for bold and italic
            val annotated = buildAnnotatedMarkdown(message.text, textColor, c)
            Text(
                text = annotated,
                modifier = Modifier
                    .padding(14.dp)
                    .widthIn(max = 300.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun buildAnnotatedMarkdown(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    c: DaylatchColors
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold: **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(
                            androidx.compose.ui.text.SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = c.primaryText
                            )
                        ) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text*
                text[i] == '*' && (i == 0 || text[i - 1] != '*') && i + 1 < text.length && text[i + 1] != '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && !text.startsWith("**", end)) {
                        withStyle(
                            androidx.compose.ui.text.SpanStyle(
                                fontWeight = FontWeight.Normal,
                                color = c.mutedFg,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Block quote: > text
                text[i] == '>' && (i == 0 || text[i - 1] == '\n') -> {
                    val end = text.indexOf('\n', i + 1).let { if (it == -1) text.length else it }
                    withStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = c.primaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("  │ " + text.substring(i + 1, end).trim())
                    }
                    i = end
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
        // Apply default color
        addStyle(
            androidx.compose.ui.text.SpanStyle(color = textColor),
            0,
            0 // will be overridden by specific spans
        )
    }
}
