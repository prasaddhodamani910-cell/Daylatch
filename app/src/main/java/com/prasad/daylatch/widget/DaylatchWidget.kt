package com.prasad.daylatch.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import com.prasad.daylatch.MainActivity
import com.prasad.daylatch.data.AppDatabase
import com.prasad.daylatch.data.HabitRepository
import com.prasad.daylatch.data.HabitStatus
import com.prasad.daylatch.data.TodayHabit
import kotlinx.coroutines.flow.first

class DaylatchWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.create(context)
        val repo = HabitRepository(db)
        
        // Load the data
        val habits = try {
            repo.observeToday().first()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            val primaryColor = Color(0xFFA5EDA8)
            val fgColor = Color(0xFF423D53)
            val cardColor = Color(0xFFFFFFFF)
            val mutedFgColor = Color(0xFF6E687C)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(primaryColor)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Today's Habits",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(fgColor),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                if (habits.isEmpty()) {
                    Text(
                        text = "No habits for today.",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(fgColor),
                            fontSize = 14.sp
                        )
                    )
                } else {
                    habits.forEach { todayHabit ->
                        val isDone = todayHabit.status == HabitStatus.COMPLETED
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDone) "✓" else "○",
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(if (isDone) fgColor else mutedFgColor),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = todayHabit.habit.name,
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(if (isDone) mutedFgColor else fgColor),
                                    fontSize = 14.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
