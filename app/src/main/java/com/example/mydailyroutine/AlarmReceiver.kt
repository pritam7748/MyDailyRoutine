package com.example.mydailyroutine

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0); val name = intent.getStringExtra("name") ?: "Routine"
        val hour = intent.getIntExtra("hour", 8); val minute = intent.getIntExtra("minute", 0)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, "routine_alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Routine time!").setContentText(name)
            .setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
        AlarmHelper.schedule(context, id, name, hour, minute)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val raw = context.getSharedPreferences("routine", Context.MODE_PRIVATE).getString("tasks", "").orEmpty()
        raw.lines().filter { it.isNotBlank() }.forEach { line ->
            val p = line.split("|"); if (p.size == 4) runCatching { AlarmHelper.schedule(context, p[0].toInt(), p[1], p[2].toInt(), p[3].toInt()) }
        }
    }
}
