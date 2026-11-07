package com.example.mydailyroutine

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getIntExtra("id", 0)
        val name = intent.getStringExtra("name") ?: "Routine"
        val hour = intent.getIntExtra("hour", 8)
        val minute = intent.getIntExtra("minute", 0)

        val openIntent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, "routine_alarm")
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Routine Time!")
            .setContentText(name)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_MAX)
            .build()

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(id, notification)

        // Schedule next day's alarm
        AlarmHelper.schedule(
            context, id, name, hour, minute
        )
    }
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(
            "routine",
            Context.MODE_PRIVATE
        )

        val raw = prefs.getString("tasks", "").orEmpty()

        raw.lines().filter { it.isNotBlank() }.forEach { line ->

            val p = line.split("|")

            if (p.size == 4) {
                runCatching {
                    AlarmHelper.schedule(
                        context,
                        p[0].toInt(),
                        p[1],
                        p[2].toInt(),
                        p[3].toInt()
                    )
                }
            }
        }
    }
}
