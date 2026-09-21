package com.prasad.daylatch.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.prasad.daylatch.R
import com.prasad.daylatch.MainActivity
import com.prasad.daylatch.data.AppDatabase
import com.prasad.daylatch.data.HabitRepository
import com.prasad.daylatch.data.HabitStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val db = AppDatabase.create(context)
        val repo = HabitRepository(db)
        
        CoroutineScope(Dispatchers.IO).launch {
            val todayHabits = repo.observeToday().first()
            val remaining = todayHabits.count { it.status == HabitStatus.PENDING }
            
            if (remaining > 0) {
                showNotification(context, remaining)
            }
        }
    }

    private fun showNotification(context: Context, remainingCount: Int) {
        val channelId = "daylatch_daily_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) 
            .setContentTitle("Daylatch")
            .setContentText("You have $remainingCount habits left to complete today!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}
