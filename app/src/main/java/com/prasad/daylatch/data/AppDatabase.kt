package com.prasad.daylatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

class AppConverters {
    @TypeConverter fun fromHabitType(value: HabitType) = value.name
    @TypeConverter fun toHabitType(value: String) = HabitType.valueOf(value)
    @TypeConverter fun fromHabitStatus(value: HabitStatus) = value.name
    @TypeConverter fun toHabitStatus(value: String) = HabitStatus.valueOf(value)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun observeForDate(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date >= :fromDate AND date <= :toDate ORDER BY date ASC")
    suspend fun logsInRange(habitId: String, fromDate: String, toDate: String): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE date >= :fromDate AND date <= :toDate ORDER BY date ASC")
    fun observeLogsInRange(fromDate: String, toDate: String): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLog(log: HabitLog)

    @Query("UPDATE habits SET archived = 1 WHERE id = :habitId")
    suspend fun archive(habitId: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0") fun observe(): Flow<Settings?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(settings: Settings)
}

@Database(entities = [Habit::class, HabitLog::class, Settings::class], version = 3, exportSchema = false)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun settings(): SettingsDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "daylatch.db"
        ).fallbackToDestructiveMigration().build()
    }
}
