package com.prasad.daylatch.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class TodayHabit(val habit: Habit, val status: HabitStatus)
data class HabitStats(val completed: Int, val expected: Int, val score: Float, val streak: Int)

class HabitRepository(private val database: AppDatabase) {
    private val habits = database.habits()
    fun observeSettings(): Flow<Settings> = database.settings().observe().map { it ?: Settings() }

    fun observeToday(): Flow<List<TodayHabit>> = combine(
        database.settings().observe(), habits.observeActive()
    ) { setting, active ->
        Pair(setting ?: Settings(), active)
    }.combineLatestLogs({ (settings, active) ->
        habits.observeForDate(habitDate(settings.dayBoundaryHour).toString())
    }) { (settings, active), logs ->
        active.map { habit -> TodayHabit(habit, logs.firstOrNull { it.habitId == habit.id }?.status ?: HabitStatus.PENDING) }
    }

    fun observeMatrix(yearMonth: java.time.YearMonth): Flow<Pair<List<Habit>, List<HabitLog>>> = combine(
        database.settings().observe(), habits.observeActive()
    ) { setting, active ->
        Pair(setting ?: Settings(), active)
    }.combineLatestLogs({ (settings, active) ->
        val fromDate = yearMonth.atDay(1).toString()
        val toDate = yearMonth.atEndOfMonth().toString()
        habits.observeLogsInRange(fromDate, toDate)
    }) { (settings, active), logs ->
        Pair(active, logs)
    }

    private fun <T, R, S> Flow<T>.combineLatestLogs(
        other: (T) -> Flow<R>, transform: suspend (T, R) -> S
    ): Flow<S> = this.flatMapLatest { first ->
        other(first).map { secondValue -> transform(first, secondValue) }
    }

    suspend fun addHabit(name: String, type: HabitType, target: Int, window: Int) {
        habits.saveHabit(Habit(UUID.randomUUID().toString(), name.trim(), type, target, window, System.currentTimeMillis()))
    }

    suspend fun setStatus(habit: Habit, status: HabitStatus, boundaryHour: Int = 0) {
        val date = habitDate(boundaryHour).toString()
        setStatusForDate(habit, date, status, boundaryHour)
    }

    suspend fun setStatusForDate(habit: Habit, date: String, status: HabitStatus, boundaryHour: Int = 0): Boolean {
        val requestedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        if (requestedDate != habitDate(boundaryHour)) return false
        habits.saveLog(HabitLog(
            id = "${habit.id}_$date", habitId = habit.id, date = date, status = status,
            completedAtUtc = if (status == HabitStatus.COMPLETED) System.currentTimeMillis() else null
        ))
        return true
    }


    suspend fun archive(habitId: String) = habits.archive(habitId)
    suspend fun saveSettings(settings: Settings) = database.settings().save(settings)
    suspend fun completeOnboarding() {
        val current = database.settings().observe().first() ?: Settings()
        database.settings().save(current.copy(onboardingDone = true))
    }

    suspend fun statsFor(habit: Habit, boundaryHour: Int): HabitStats {
        val days = if (habit.type == HabitType.DAILY) 30 else habit.windowDays
        val end = habitDate(boundaryHour)
        val start = end.minusDays((days - 1).toLong())
        val logs = habits.logsInRange(habit.id, start.toString(), end.toString())
        val completed = logs.count { it.status == HabitStatus.COMPLETED }
        val expected = if (habit.type == HabitType.DAILY) days else habit.targetPerWindow
        val streak = if (habit.type == HabitType.DAILY) dailyStreak(logs, end) else 0
        return HabitStats(completed, expected, consistencyScore(completed, expected), streak)
    }

    companion object {
        fun habitDate(boundaryHour: Int, now: LocalDateTime = LocalDateTime.now()): LocalDate =
            if (now.toLocalTime().isBefore(LocalTime.of(boundaryHour.coerceIn(0, 23), 0))) now.toLocalDate().minusDays(1) else now.toLocalDate()

        fun consistencyScore(completed: Int, expected: Int): Float =
            if (expected <= 0) 0f else (completed.toFloat() / expected).coerceAtMost(1f)

        fun dailyStreak(logs: List<HabitLog>, end: LocalDate): Int {
            val completedDates = logs.filter { it.status == HabitStatus.COMPLETED }.map { LocalDate.parse(it.date) }.toSet()
            var cursor = end
            var count = 0
            if (cursor !in completedDates) {
                cursor = cursor.minusDays(1)
            }
            while (cursor in completedDates) { count++; cursor = cursor.minusDays(1) }
            return count
        }
    }
}
