package com.prasad.daylatch.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HabitType { DAILY, QUOTA }
enum class HabitStatus { PENDING, COMPLETED, SKIPPED, FAILED }

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val type: HabitType,
    val targetPerWindow: Int,
    val windowDays: Int,
    val createdAt: Long,
    val archived: Boolean = false
)

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitLog(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val status: HabitStatus,
    val completedAtUtc: Long? = null,
    val note: String? = null
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0,
    val dayBoundaryHour: Int = 0,
    val theme: String = "system",
    val onboardingDone: Boolean = false
)
