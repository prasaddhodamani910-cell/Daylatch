package com.prasad.daylatch

import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.prasad.daylatch.receivers.AlarmScheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.prasad.daylatch.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.prasad.daylatch.widget.DaylatchWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

// ─── Theme Colors ─────────────────────────────────
data class DaylatchColors(
    val bg: Color, val fg: Color, val primary: Color, val primaryText: Color,
    val secondary: Color, val tertiary: Color, val tertiaryText: Color,
    val muted: Color, val mutedFg: Color, val accent: Color, val accentText: Color,
    val border: Color, val card: Color, val destructive: Color
)

val LightColors = DaylatchColors(
    bg = Color(0xFFFEFEFE), fg = Color(0xFF423D53), primary = Color(0xFFA5EDA8),
    primaryText = Color(0xFF3F8445), secondary = Color(0xFFEAE8F4),
    tertiary = Color(0xFFD0EAED), tertiaryText = Color(0xFF456E73),
    muted = Color(0xFFF5F4F8), mutedFg = Color(0xFF6E687C),
    accent = Color(0xFFFEDFE9), accentText = Color(0xFFB45974),
    border = Color(0xFFD7D2E1), card = Color(0xFFFFFFFF), destructive = Color(0xFFD87993)
)

val DarkColors = DaylatchColors(
    bg = Color(0xFF1A1A2E), fg = Color(0xFFE0DEF0), primary = Color(0xFF4CAF6A),
    primaryText = Color(0xFF8EE8A0), secondary = Color(0xFF2A2842),
    tertiary = Color(0xFF1E3438), tertiaryText = Color(0xFF80C0CC),
    muted = Color(0xFF252336), mutedFg = Color(0xFF9993AB),
    accent = Color(0xFF3D2030), accentText = Color(0xFFE88DA5),
    border = Color(0xFF3A3650), card = Color(0xFF222038), destructive = Color(0xFFE06080)
)

val LocalDaylatchColors = staticCompositionLocalOf { LightColors }


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AlarmScheduler.scheduleDailyReminder(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                AlarmScheduler.scheduleDailyReminder(this)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            AlarmScheduler.scheduleDailyReminder(this)
        }

        val repository = HabitRepository(AppDatabase.create(this))
        setContent {
            val viewModel: DaylatchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = DaylatchViewModel.factory(repository)
            )
            DaylatchApp(viewModel)
        }
    }
}

class DaylatchViewModel(private val repository: HabitRepository) : ViewModel() {
    val today = repository.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var currentMonth by mutableStateOf(YearMonth.now())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val matrix = snapshotFlow { currentMonth }
        .flatMapLatest { repository.observeMatrix(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Pair(emptyList(), emptyList()))

    var selected by mutableStateOf<Habit?>(null)
    var boundaryHour by mutableIntStateOf(0)
    var onboardingDone by mutableStateOf(true) // default true to avoid flash
    var theme by mutableStateOf("system")

    fun updateWidget(context: android.content.Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(DaylatchWidget::class.java)
            ids.forEach { DaylatchWidget().update(context, it) }
        } catch (_: Exception) {}
    }

    fun updateStatusForDate(context: android.content.Context, habit: Habit, date: String, status: HabitStatus) = viewModelScope.launch {
        repository.setStatusForDate(habit, date, status, boundaryHour)
        updateWidget(context)
    }

    fun toggleToday(item: TodayHabit) = viewModelScope.launch {
        val newStatus = when (item.status) {
            HabitStatus.COMPLETED -> HabitStatus.SKIPPED
            HabitStatus.SKIPPED -> HabitStatus.PENDING
            else -> HabitStatus.COMPLETED
        }
        repository.setStatus(item.habit, newStatus, boundaryHour)
    }

    fun add(name: String, type: HabitType, target: Int, window: Int) = viewModelScope.launch {
        repository.addHabit(name, type, target, window)
    }

    fun archive(habit: Habit) = viewModelScope.launch {
        repository.archive(habit.id)
        selected = null
    }

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                boundaryHour = settings.dayBoundaryHour
                onboardingDone = settings.onboardingDone
                theme = settings.theme
            }
        }
    }

    fun saveBoundary(hour: Int) = viewModelScope.launch {
        boundaryHour = hour
        // assuming settings has a theme field which we shouldn't overwrite blindly, but we don't have getSettings
        // We'll assume saveBoundary and saveTheme can just pass the other field from state
        repository.saveSettings(Settings(dayBoundaryHour = hour, onboardingDone = onboardingDone, theme = theme))
    }

    fun saveTheme(t: String) = viewModelScope.launch {
        theme = t
        repository.saveSettings(Settings(dayBoundaryHour = boundaryHour, onboardingDone = onboardingDone, theme = t))
    }

    fun finishOnboarding(suggestedHabits: List<SuggestedHabit>) = viewModelScope.launch {
        suggestedHabits.forEach { habit ->
            repository.addHabit(habit.name, habit.type, habit.target, habit.window)
        }
        repository.completeOnboarding()
    }

    suspend fun stats(habit: Habit) = repository.statsFor(habit, boundaryHour)

    companion object {
        fun factory(repository: HabitRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DaylatchViewModel(repository) as T
        }
    }
}

private enum class Tab { TODAY, COACH, INSIGHTS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaylatchApp(viewModel: DaylatchViewModel) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var adding by rememberSaveable { mutableStateOf(false) }

    val isDark = when (viewModel.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (isDark) DarkColors else LightColors

    val colorScheme = remember(colors) {
        if (isDark) {
            darkColorScheme(
                background = colors.bg, surface = colors.card, onBackground = colors.fg, onSurface = colors.fg,
                outline = colors.border, primary = colors.primary, onPrimary = colors.primaryText,
                primaryContainer = colors.tertiary, secondary = colors.secondary, onSecondary = colors.mutedFg,
                error = colors.destructive, onError = colors.accentText, surfaceVariant = colors.muted
            )
        } else {
            lightColorScheme(
                background = colors.bg, surface = colors.card, onBackground = colors.fg, onSurface = colors.fg,
                outline = colors.border, primary = colors.primary, onPrimary = colors.primaryText,
                primaryContainer = colors.tertiary, secondary = colors.secondary, onSecondary = colors.mutedFg,
                error = colors.destructive, onError = colors.accentText, surfaceVariant = colors.muted
            )
        }
    }

    CompositionLocalProvider(LocalDaylatchColors provides colors) {
        MaterialTheme(colorScheme = colorScheme) {
        // ─── Onboarding Gate ─────────────────────────────
        if (!viewModel.onboardingDone) {
            OnboardingScreen(onComplete = { habits -> viewModel.finishOnboarding(habits) })
            return@MaterialTheme
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Daylatch", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Small steps, every day", fontSize = 11.sp, color = LocalDaylatchColors.current.mutedFg)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalDaylatchColors.current.card)
                )
            },
            floatingActionButton = {
                if (tab == Tab.TODAY) {
                    FloatingActionButton(
                        onClick = { adding = true },
                        containerColor = LocalDaylatchColors.current.primary, contentColor = LocalDaylatchColors.current.primaryText,
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = LocalDaylatchColors.current.card, tonalElevation = 0.dp) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Text(when (item) { Tab.TODAY -> "◫"; Tab.COACH -> "🤖"; Tab.INSIGHTS -> "◈"; Tab.SETTINGS -> "⚙" }, fontSize = 18.sp) },
                            label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalDaylatchColors.current.primaryText, selectedTextColor = LocalDaylatchColors.current.primaryText,
                                indicatorColor = LocalDaylatchColors.current.primary.copy(alpha = 0.2f),
                                unselectedIconColor = LocalDaylatchColors.current.mutedFg, unselectedTextColor = LocalDaylatchColors.current.mutedFg
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.TODAY -> TodayScreen(today, viewModel, adding, onAddDismiss = { adding = false })
                    Tab.COACH -> CoachScreen(today, viewModel)
                    Tab.INSIGHTS -> InsightsScreen(today, viewModel)
                    Tab.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
        if (adding && tab != Tab.TODAY) {
            AddHabitDialog(
                onDismiss = { adding = false },
                onSave = { name, type, target, window -> viewModel.add(name, type, target, window); adding = false }
            )
        }
        viewModel.selected?.let { habit ->
            DetailDialog(habit, viewModel, onDismiss = { viewModel.selected = null })
        }
    }
    }
}

// ─── Today Screen (Checklist + Matrix) ─────────────────────────────
@Composable
fun TodayScreen(today: List<TodayHabit>, viewModel: DaylatchViewModel, adding: Boolean, onAddDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val selectedDate = HabitRepository.habitDate(viewModel.boundaryHour)
    val completed = today.count { it.status == HabitStatus.COMPLETED }
    val total = today.size
    val pct = if (total > 0) (completed * 100) / total else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LocalDaylatchColors.current.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today header card
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = LocalDaylatchColors.current.secondary) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TODAY", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalDaylatchColors.current.fg)
                        }
                        Box(Modifier.size(56.dp).clip(CircleShape).background(LocalDaylatchColors.current.card), contentAlignment = Alignment.Center) {
                            Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace,
                                color = if (pct >= 80) LocalDaylatchColors.current.primaryText else if (pct >= 50) LocalDaylatchColors.current.fg else LocalDaylatchColors.current.accentText)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = LocalDaylatchColors.current.primary, trackColor = LocalDaylatchColors.current.card.copy(alpha = 0.5f))
                    Spacer(Modifier.height(4.dp))
                    Text("$completed of $total completed", color = LocalDaylatchColors.current.mutedFg, fontSize = 11.sp)
                }
            }
        }

        // Empty state
        if (today.isEmpty()) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = LocalDaylatchColors.current.card, shadowElevation = 1.dp) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No habits yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalDaylatchColors.current.fg)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to add your first habit!", color = LocalDaylatchColors.current.mutedFg, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        // Interactive habit checklist
        items(today, key = { it.habit.id }) { item ->
            val bgColor = when (item.status) {
                HabitStatus.COMPLETED -> LocalDaylatchColors.current.primary.copy(alpha = 0.12f)
                HabitStatus.SKIPPED -> LocalDaylatchColors.current.accent.copy(alpha = 0.15f)
                else -> LocalDaylatchColors.current.card
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = bgColor,
                shadowElevation = if (item.status == HabitStatus.PENDING) 1.dp else 0.dp) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleToday(item)
                        if (item.status != HabitStatus.COMPLETED) {
                            try {
                                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                MediaPlayer.create(context, uri)?.apply { start(); setOnCompletionListener { release() } }
                            } catch (_: Exception) {}
                        }
                        viewModel.updateWidget(context)
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                            .background(when (item.status) { HabitStatus.COMPLETED -> LocalDaylatchColors.current.primary; HabitStatus.SKIPPED -> LocalDaylatchColors.current.accent; else -> LocalDaylatchColors.current.muted })
                            .border(1.5.dp, when (item.status) { HabitStatus.COMPLETED -> LocalDaylatchColors.current.primaryText.copy(alpha = 0.3f); HabitStatus.SKIPPED -> LocalDaylatchColors.current.accentText.copy(alpha = 0.3f); else -> LocalDaylatchColors.current.border }, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.status) {
                            HabitStatus.COMPLETED -> Text("✓", color = LocalDaylatchColors.current.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            HabitStatus.SKIPPED -> Text("–", color = LocalDaylatchColors.current.accentText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            else -> {}
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.habit.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            color = if (item.status == HabitStatus.COMPLETED) LocalDaylatchColors.current.mutedFg else LocalDaylatchColors.current.fg,
                            textDecoration = if (item.status == HabitStatus.COMPLETED) TextDecoration.LineThrough else TextDecoration.None)
                        if (item.habit.type == HabitType.QUOTA) {
                            Text("${item.habit.targetPerWindow}x / ${item.habit.windowDays} days", color = LocalDaylatchColors.current.mutedFg, fontSize = 11.sp)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = when (item.status) {
                        HabitStatus.COMPLETED -> LocalDaylatchColors.current.primary.copy(alpha = 0.3f); HabitStatus.SKIPPED -> LocalDaylatchColors.current.accent.copy(alpha = 0.3f); else -> LocalDaylatchColors.current.muted
                    }) {
                        Text(
                            when (item.status) { HabitStatus.COMPLETED -> "Done"; HabitStatus.SKIPPED -> "Skipped"; else -> "Tap to do" },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = when (item.status) { HabitStatus.COMPLETED -> LocalDaylatchColors.current.primaryText; HabitStatus.SKIPPED -> LocalDaylatchColors.current.accentText; else -> LocalDaylatchColors.current.mutedFg }
                        )
                    }
                }
            }
        }

        // Tip
        if (today.isNotEmpty()) {
            item { Text("Tap: Done → Skipped → Clear", color = LocalDaylatchColors.current.mutedFg, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
    if (adding) {
        AddHabitDialog(onDismiss = onAddDismiss, onSave = { name, type, target, window -> viewModel.add(name, type, target, window); onAddDismiss() })
    }
}

// ─── Insights Screen ─────────────────────────────────
@Composable
fun InsightsScreen(today: List<TodayHabit>, viewModel: DaylatchViewModel) {
    var detailScreen by rememberSaveable { mutableStateOf<String?>(null) }
    val (habits, logs) = viewModel.matrix.collectAsStateWithLifecycle().value
    val month = viewModel.currentMonth
    val daysInMonth = month.lengthOfMonth()
    val selectedDate = HabitRepository.habitDate(viewModel.boundaryHour)
    val activeDays = remember(month, selectedDate, daysInMonth) {
        when {
            month.isAfter(YearMonth.from(selectedDate)) -> 0
            month == YearMonth.from(selectedDate) -> selectedDate.dayOfMonth
            else -> daysInMonth
        }
    }
    val completedThisMonth = remember(logs) { logs.count { it.status == HabitStatus.COMPLETED } }
    val skippedThisMonth = remember(logs) { logs.count { it.status == HabitStatus.SKIPPED } }
    
    val openThisMonth = remember(habits, activeDays, completedThisMonth, skippedThisMonth) {
        val totalTrackableCells = habits.size * activeDays
        (totalTrackableCells - completedThisMonth - skippedThisMonth).coerceAtLeast(0)
    }
    
    val habitRanking = remember(habits, logs) {
        habits.map { habit ->
            val completed = logs.count { it.habitId == habit.id && it.status == HabitStatus.COMPLETED }
            habit to completed
        }.sortedByDescending { it.second }
    }
    
    val weeklyProgress = remember(activeDays, habits, logs) {
        (0..3).map { week ->
            val lastDay = (activeDays - ((3 - week) * 7)).coerceIn(0, activeDays)
            val firstDay = (lastDay - 6).coerceAtLeast(1)
            if (lastDay < firstDay || habits.isEmpty()) 0f else {
                val done = logs.count { log -> log.status == HabitStatus.COMPLETED && LocalDate.parse(log.date).dayOfMonth in firstDay..lastDay }
                done.toFloat() / ((lastDay - firstDay + 1) * habits.size).toFloat()
            }
        }
    }

    if (detailScreen != null) {
        androidx.activity.compose.BackHandler { detailScreen = null }
        when (detailScreen) {
            "Monthly Detail" -> MonthlyScoreDetailScreen(month, daysInMonth, habits, logs, onBack = { detailScreen = null })
            "Completion Details" -> CompletionMixDetailScreen(habits.size, completedThisMonth, skippedThisMonth, openThisMonth, onBack = { detailScreen = null })
            "Four-Week Trend" -> FourWeekRhythmDetailScreen(weeklyProgress, onBack = { detailScreen = null })
        }
    } else {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalDaylatchColors.current.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Quick Stats
        item {
            val completed = today.count { it.status == HabitStatus.COMPLETED }
            val total = today.size
            val pct = if (total > 0) (completed * 100) / total else 0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = LocalDaylatchColors.current.secondary
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TODAY", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                HabitRepository.habitDate(viewModel.boundaryHour).format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = LocalDaylatchColors.current.fg
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(LocalDaylatchColors.current.card),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☀", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Today's habit checklist
                    today.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalDaylatchColors.current.card)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (item.status == HabitStatus.COMPLETED) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.muted)
                                    .border(1.dp, if (item.status == HabitStatus.COMPLETED) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.border, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.status == HabitStatus.COMPLETED) {
                                    Text("✓", color = LocalDaylatchColors.current.primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                item.habit.name,
                                color = if (item.status == HabitStatus.COMPLETED) LocalDaylatchColors.current.mutedFg else LocalDaylatchColors.current.fg,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                textDecoration = if (item.status == HabitStatus.COMPLETED) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (item.status == HabitStatus.COMPLETED) "Done" else "Pending",
                                color = LocalDaylatchColors.current.mutedFg,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (today.isEmpty()) {
                        Text(
                            "No habits yet. Tap + to add one!",
                            color = LocalDaylatchColors.current.mutedFg,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Score bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Today's score", color = LocalDaylatchColors.current.mutedFg, fontSize = 12.sp)
                        Text(
                            "$pct%",
                            color = LocalDaylatchColors.current.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = LocalDaylatchColors.current.primary,
                        trackColor = LocalDaylatchColors.current.muted,
                    )
                }
            }
        }

        // Monthly Score Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { detailScreen = "Monthly Detail" },
                shape = RoundedCornerShape(16.dp),
                color = LocalDaylatchColors.current.card,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MONTHLY SCORE", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("You're showing up", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalDaylatchColors.current.fg)
                        }
                        Text("✦", fontSize = 20.sp, color = LocalDaylatchColors.current.primaryText)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val totalCells = remember(habits, daysInMonth) { habits.size * daysInMonth }
                    val completedCells = remember(habits, daysInMonth, month, logs) {
                        habits.sumOf { habit ->
                            (1..daysInMonth).count { day ->
                                val date = month.atDay(day)
                                logs.any { it.habitId == habit.id && it.date == date.toString() && it.status == HabitStatus.COMPLETED }
                            }
                        }
                    }
                    val score = if (totalCells > 0) (completedCells * 100) / totalCells else 0

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "$score%",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LocalDaylatchColors.current.fg
                        )
                    }
                    Text(
                        "complete this month",
                        modifier = Modifier.fillMaxWidth(),
                        color = LocalDaylatchColors.current.mutedFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(RoundedCornerShape(4.dp)), color = LocalDaylatchColors.current.primary, trackColor = LocalDaylatchColors.current.muted)
                }
            }
        }

        if (habits.isNotEmpty() && activeDays > 0) {
            item {
                Surface(modifier = Modifier.fillMaxWidth().clickable { detailScreen = "Completion Details" }, shape = RoundedCornerShape(20.dp), color = LocalDaylatchColors.current.tertiary) {
                    Column(Modifier.padding(20.dp)) {
                        Text("COMPLETION MIX", color = LocalDaylatchColors.current.tertiaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            MonthlyDonutChart(completedThisMonth, skippedThisMonth, openThisMonth, Modifier.size(136.dp))
                            Text(habits.size.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.tertiaryText)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ChartLegend(LocalDaylatchColors.current.primary, "Completed", completedThisMonth)
                            ChartLegend(LocalDaylatchColors.current.accent, "Skipped", skippedThisMonth)
                            ChartLegend(LocalDaylatchColors.current.mutedFg, "Open", openThisMonth)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Results are based on $activeDays tracked day${if (activeDays == 1) "" else "s"}.", color = LocalDaylatchColors.current.tertiaryText, fontSize = 11.sp)
                    }
                }
            }
            item {
                Surface(modifier = Modifier.fillMaxWidth().clickable { detailScreen = "Four-Week Trend" }, shape = RoundedCornerShape(20.dp), color = LocalDaylatchColors.current.card, shadowElevation = 2.dp) {
                    Column(Modifier.padding(20.dp)) {
                        Text("FOUR-WEEK RHYTHM", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Your completion trend", color = LocalDaylatchColors.current.fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        WeeklyBarChart(weeklyProgress, Modifier.fillMaxWidth().height(132.dp).padding(top = 14.dp))
                    }
                }
            }
            item {
                Text("MONTHLY SCOREBOARD", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp))
            }
            items(habitRanking.take(5)) { (habit, completed) ->
                val max = habitRanking.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = LocalDaylatchColors.current.card, shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(habit.name, color = LocalDaylatchColors.current.fg, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("$completed checks", color = LocalDaylatchColors.current.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { completed.toFloat() / max }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(7.dp).clip(RoundedCornerShape(4.dp)), color = LocalDaylatchColors.current.primary, trackColor = LocalDaylatchColors.current.muted)
                    }
                }
            }
        }

        // Per-habit stats
        if (habits.isNotEmpty()) {
            item {
                Text(
                    "HABIT STREAKS",
                    color = LocalDaylatchColors.current.mutedFg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(habits) { habit ->
                var stats by remember { mutableStateOf<HabitStats?>(null) }
                LaunchedEffect(habit.id) {
                    stats = viewModel.stats(habit)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = LocalDaylatchColors.current.card,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LocalDaylatchColors.current.fg)
                            Text(
                                if (habit.type == HabitType.DAILY) "Daily" else "${habit.targetPerWindow}x / ${habit.windowDays} days",
                                color = LocalDaylatchColors.current.mutedFg,
                                fontSize = 11.sp
                            )
                        }
                        stats?.let { s ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${(s.score * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (s.score >= 0.8f) LocalDaylatchColors.current.primaryText
                                    else if (s.score >= 0.5f) LocalDaylatchColors.current.fg
                                    else LocalDaylatchColors.current.accentText
                                )
                                if (s.streak > 0) {
                                    Text(
                                        "🔥 ${s.streak} day streak",
                                        color = LocalDaylatchColors.current.primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun MonthlyScoreDetailScreen(
    month: YearMonth,
    daysInMonth: Int,
    habits: List<Habit>,
    logs: List<HabitLog>,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(LocalDaylatchColors.current.bg)) {
        DetailHeader("Monthly Score", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items((1..daysInMonth).toList()) { day ->
                val date = month.atDay(day)
                val dayLogs = logs.filter { it.date == date.toString() }
                val completed = dayLogs.count { it.status == HabitStatus.COMPLETED }
                val skipped = dayLogs.count { it.status == HabitStatus.SKIPPED }
                val open = (habits.size - completed - skipped).coerceAtLeast(0)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LocalDaylatchColors.current.card,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.fg, modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChartLegend(LocalDaylatchColors.current.primary, "Done", completed)
                            ChartLegend(LocalDaylatchColors.current.accent, "Skip", skipped)
                            ChartLegend(LocalDaylatchColors.current.mutedFg, "Open", open)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionMixDetailScreen(
    totalHabits: Int,
    completed: Int,
    skipped: Int,
    open: Int,
    onBack: () -> Unit
) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    
    val total = (completed + skipped + open).coerceAtLeast(1)
    val compSweep = (completed.toFloat() / total * 360f)
    val skipSweep = (skipped.toFloat() / total * 360f)
    val openSweep = (open.toFloat() / total * 360f)

    val aCompSweep by androidx.compose.animation.core.animateFloatAsState(if (animationPlayed) compSweep else 0f, animationSpec = androidx.compose.animation.core.tween(1000), label = "")
    val aSkipSweep by androidx.compose.animation.core.animateFloatAsState(if (animationPlayed) skipSweep else 0f, animationSpec = androidx.compose.animation.core.tween(1000), label = "")
    val aOpenSweep by androidx.compose.animation.core.animateFloatAsState(if (animationPlayed) openSweep else 0f, animationSpec = androidx.compose.animation.core.tween(1000), label = "")

    Column(Modifier.fillMaxSize().background(LocalDaylatchColors.current.bg)) {
        DetailHeader("Completion Mix", onBack)
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp).padding(vertical = 16.dp)) {
                val cPrimary = LocalDaylatchColors.current.primary
                val cAccent = LocalDaylatchColors.current.accent
                val cMutedFg = LocalDaylatchColors.current.mutedFg
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * .20f
                    var start = -90f
                    if (aCompSweep > 0f) drawArc(cPrimary, start, aCompSweep - 2f, false, style = Stroke(stroke))
                    start += aCompSweep
                    if (aSkipSweep > 0f) drawArc(cAccent, start, aSkipSweep - 2f, false, style = Stroke(stroke))
                    start += aSkipSweep
                    if (aOpenSweep > 0f) drawArc(cMutedFg, start, aOpenSweep - 2f, false, style = Stroke(stroke))
                }
                Text(totalHabits.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.fg)
            }
            Spacer(Modifier.height(32.dp))
            val pctComp = if(total > 0) completed * 100 / total else 0
            val pctSkip = if(total > 0) skipped * 100 / total else 0
            val pctOpen = if(total > 0) open * 100 / total else 0
            MixListItem(LocalDaylatchColors.current.primary, "Completed", pctComp, completed)
            MixListItem(LocalDaylatchColors.current.accent, "Skipped", pctSkip, skipped)
            MixListItem(LocalDaylatchColors.current.mutedFg, "Open/Failed", pctOpen, open)
        }
    }
}

@Composable
fun MixListItem(color: Color, label: String, pct: Int, count: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(label, color = LocalDaylatchColors.current.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("$pct% ($count checks)", color = LocalDaylatchColors.current.mutedFg, fontSize = 14.sp)
    }
}

@Composable
fun FourWeekRhythmDetailScreen(
    weeklyProgress: List<Float>,
    onBack: () -> Unit
) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    
    Column(Modifier.fillMaxSize().background(LocalDaylatchColors.current.bg)) {
        DetailHeader("Four-Week Rhythm", onBack)
        Column(Modifier.padding(24.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val labels = listOf("W1", "W2", "W3", "W4")
                weeklyProgress.forEachIndexed { index, raw ->
                    val value = raw.coerceIn(0f, 1f)
                    val aValue by androidx.compose.animation.core.animateFloatAsState(if (animationPlayed) value else 0f, animationSpec = androidx.compose.animation.core.tween(1000, delayMillis = index * 100), label = "")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(0.5f)
                                .clip(RoundedCornerShape(50)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(Modifier.fillMaxSize().background(LocalDaylatchColors.current.muted))
                            if (aValue > 0f) {
                                Box(
                                    Modifier.fillMaxWidth().fillMaxHeight(aValue)
                                        .background(if (index == weeklyProgress.lastIndex) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.tertiary)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(labels.getOrElse(index) { "" }, color = LocalDaylatchColors.current.mutedFg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            weeklyProgress.forEachIndexed { index, raw ->
                val pct = (raw.coerceIn(0f, 1f) * 100).toInt()
                val labels = listOf("Four weeks ago", "Three weeks ago", "Two weeks ago", "Last week")
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(labels.getOrElse(index) { "" }, color = LocalDaylatchColors.current.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("$pct% completed", color = if (index == weeklyProgress.lastIndex) LocalDaylatchColors.current.primaryText else LocalDaylatchColors.current.mutedFg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = LocalDaylatchColors.current.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LocalDaylatchColors.current.fg,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.width(64.dp))
    }
}

@Composable
private fun ChartLegend(color: Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(7.dp))
        Text("$label  $value", color = LocalDaylatchColors.current.fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MonthlyDonutChart(completed: Int, skipped: Int, open: Int, modifier: Modifier = Modifier) {
    val total = (completed + skipped + open).coerceAtLeast(1)
    val cPrimary = LocalDaylatchColors.current.primary
    val cAccent = LocalDaylatchColors.current.accent
    val cMutedFg = LocalDaylatchColors.current.mutedFg
    Canvas(modifier) {
        val stroke = size.minDimension * .20f
        var start = -90f
        listOf(cPrimary to completed, cAccent to skipped, cMutedFg to open).forEach { (color, amount) ->
            val sweep = amount.toFloat() / total * 360f
            if (sweep > 0f) drawArc(color = color, startAngle = start, sweepAngle = sweep - 2f, useCenter = false, style = Stroke(stroke))
            start += sweep
        }
    }
}

@Composable
private fun WeeklyBarChart(values: List<Float>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        val labels = listOf("W1", "W2", "W3", "W4")
        values.forEachIndexed { index, raw ->
            val value = raw.coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.4f)
                        .clip(RoundedCornerShape(50)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LocalDaylatchColors.current.muted)
                    )
                    if (value > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(value)
                                .background(if (index == values.lastIndex) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.tertiary)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(labels.getOrElse(index) { "" }, color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Settings Screen ─────────────────────────────────
@Composable
fun SettingsScreen(viewModel: DaylatchViewModel) {
    var editHour by rememberSaveable { mutableStateOf(viewModel.boundaryHour.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalDaylatchColors.current.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Selection
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = LocalDaylatchColors.current.card,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("THEME", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                            val selected = viewModel.theme == id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.saveTheme(id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) LocalDaylatchColors.current.primary else LocalDaylatchColors.current.muted,
                                border = BorderStroke(1.dp, if (selected) LocalDaylatchColors.current.primaryText.copy(alpha=0.5f) else LocalDaylatchColors.current.border)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(label, fontWeight = FontWeight.Bold, color = if (selected) LocalDaylatchColors.current.primaryText else LocalDaylatchColors.current.mutedFg)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Data Vault
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = LocalDaylatchColors.current.tertiary
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("🔒 DATA VAULT", color = LocalDaylatchColors.current.tertiaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This app does not have internet permissions. Your data physically cannot leave this device.",
                        color = LocalDaylatchColors.current.fg,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Day Boundary
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = LocalDaylatchColors.current.card,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DAY BOUNDARY", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Set when your \"day\" starts. Night owls: try 3 AM so late-night check-ins count for the right day.",
                        color = LocalDaylatchColors.current.fg,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editHour,
                            onValueChange = { editHour = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Hour (0–23)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val h = editHour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                                    viewModel.saveBoundary(h)
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val h = editHour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                                viewModel.saveBoundary(h)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LocalDaylatchColors.current.primary, contentColor = LocalDaylatchColors.current.primaryText),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Currently: ${if (viewModel.boundaryHour == 0) "Midnight" else "${viewModel.boundaryHour}:00 AM"}",
                        color = LocalDaylatchColors.current.mutedFg,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // About
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = LocalDaylatchColors.current.card,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ABOUT", color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Daylatch", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = LocalDaylatchColors.current.fg)
                    Text("v2.0.0", color = LocalDaylatchColors.current.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Built by Prasad Dhodamani", color = LocalDaylatchColors.current.mutedFg, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Math over magic, disk over cloud.",
                        color = LocalDaylatchColors.current.fg,
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val features = listOf(
                        "🔒 Offline-first — zero cloud dependency",
                        "📊 Habit insights & analytics",
                        "🌙 Dark mode support",
                        "🔔 Daily smart reminders",
                        "📱 Home screen widget"
                    )
                    features.forEach { feature ->
                        Text(feature, color = LocalDaylatchColors.current.fg, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ─── Add Habit Dialog ─────────────────────────────────
@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, HabitType, Int, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var isDaily by rememberSaveable { mutableStateOf(true) }
    var target by rememberSaveable { mutableStateOf("1") }
    var window by rememberSaveable { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalDaylatchColors.current.card,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Add New Habit", fontWeight = FontWeight.Bold, color = LocalDaylatchColors.current.fg)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type toggle
                Text("Type", color = LocalDaylatchColors.current.mutedFg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isDaily,
                        onClick = { isDaily = true },
                        label = { Text("Daily") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LocalDaylatchColors.current.primary,
                            selectedLabelColor = LocalDaylatchColors.current.primaryText
                        )
                    )
                    FilterChip(
                        selected = !isDaily,
                        onClick = { isDaily = false },
                        label = { Text("Quota") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LocalDaylatchColors.current.tertiary,
                            selectedLabelColor = LocalDaylatchColors.current.fg
                        )
                    )
                }

                if (!isDaily) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Times") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = window,
                            onValueChange = { window = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Per X days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val t = if (isDaily) HabitType.DAILY else HabitType.QUOTA
                        val tgt = if (isDaily) 1 else (target.toIntOrNull() ?: 1).coerceAtLeast(1)
                        val win = if (isDaily) 1 else (window.toIntOrNull() ?: 7).coerceAtLeast(1)
                        onSave(name, t, tgt, win)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LocalDaylatchColors.current.primary, contentColor = LocalDaylatchColors.current.primaryText),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add habit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LocalDaylatchColors.current.mutedFg)
            }
        }
    )
}

// ─── Detail Dialog ─────────────────────────────────
@Composable
fun DetailDialog(habit: Habit, viewModel: DaylatchViewModel, onDismiss: () -> Unit) {
    var stats by remember { mutableStateOf<HabitStats?>(null) }
    LaunchedEffect(habit.id) {
        stats = viewModel.stats(habit)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalDaylatchColors.current.card,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = LocalDaylatchColors.current.fg)
                Text(
                    if (habit.type == HabitType.DAILY) "Daily habit" else "Quota: ${habit.targetPerWindow}x / ${habit.windowDays} days",
                    color = LocalDaylatchColors.current.mutedFg,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            stats?.let { s ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Hero stat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalDaylatchColors.current.secondary)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(s.score * 100).toInt()}%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LocalDaylatchColors.current.fg
                            )
                            Text("Consistency Score", color = LocalDaylatchColors.current.mutedFg, fontSize = 12.sp)
                        }
                    }

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBlock("Completed", "${s.completed}", LocalDaylatchColors.current.primary)
                        StatBlock("Expected", "${s.expected}", LocalDaylatchColors.current.tertiary)
                        if (s.streak > 0) {
                            StatBlock("Streak", "${s.streak}d", LocalDaylatchColors.current.accent)
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LocalDaylatchColors.current.primary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = LocalDaylatchColors.current.primaryText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.archive(habit) }) {
                Text("Archive", color = LocalDaylatchColors.current.accentText)
            }
        }
    )
}

@Composable
private fun StatBlock(label: String, value: String, bgColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = FontFamily.Monospace, color = LocalDaylatchColors.current.fg)
        Text(label, color = LocalDaylatchColors.current.mutedFg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
