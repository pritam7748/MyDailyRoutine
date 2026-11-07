package com.example.mydailyroutine

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private val prefs by lazy { getSharedPreferences("routine", MODE_PRIVATE) }
    private data class Task(val id: Int, val name: String, val hour: Int, val minute: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 20); setBackgroundColor(0xFFF5F7FB.toInt()) }
        root.addView(TextView(this).apply { text = "My Daily Routine"; textSize = 27f; setTextColor(0xFF123B70.toInt()); gravity = Gravity.CENTER; setPadding(0, 8, 0, 8) })
        root.addView(TextView(this).apply { text = "Set your daily tasks and get an alarm on time."; textSize = 14f; gravity = Gravity.CENTER; setTextColor(0xFF526174.toInt()); setPadding(0, 0, 0, 18) })
        val add = Button(this).apply { text = "+ Add Routine"; setOnClickListener { addTask() } }
        root.addView(add)
        root.addView(TextView(this).apply { text = "YOUR DAILY ROUTINES"; textSize = 13f; setTextColor(0xFF526174.toInt()); setPadding(0, 22, 0, 8) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    private fun tasks(): MutableList<Task> = prefs.getString("tasks", "").orEmpty().lines().filter { it.isNotBlank() }.mapNotNull {
        val p = it.split("|"); if (p.size != 4) null else runCatching { Task(p[0].toInt(), p[1], p[2].toInt(), p[3].toInt()) }.getOrNull()
    }.toMutableList()

    private fun save(items: List<Task>) { prefs.edit().putString("tasks", items.joinToString("\n") { "${it.id}|${it.name.replace("|", " ")}|${it.hour}|${it.minute}" }).apply() }

    private fun addTask() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val name = EditText(this).apply { hint = "Task name (e.g. Math practice)" }
        box.addView(name)
        val time = TimePicker(this).apply { setIs24HourView(false) }
        box.addView(time)
        AlertDialog.Builder(this).setTitle("Add daily routine").setView(box)
            .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                val label = name.text.toString().trim()
                if (label.isEmpty()) { Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val hour = if (Build.VERSION.SDK_INT >= 23) time.hour else time.currentHour
                val minute = if (Build.VERSION.SDK_INT >= 23) time.minute else time.currentMinute
                val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val updated = tasks().apply { add(Task(id, label, hour, minute)) }
                save(updated); AlarmHelper.schedule(this, id, label, hour, minute); render()
            }.show()
    }

    private fun render() {
        list.removeAllViews()
        val all = tasks().sortedWith(compareBy<Task> { it.hour }.thenBy { it.minute })
        if (all.isEmpty()) list.addView(TextView(this).apply { text = "No routines yet. Tap + Add Routine to begin."; textSize = 15f; setPadding(8, 18, 8, 18); setTextColor(0xFF526174.toInt()) })
        all.forEach { task ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(14, 12, 8, 12); setBackgroundColor(0xFFFFFFFF.toInt()) }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(TextView(this).apply { text = task.name; textSize = 17f; setTextColor(0xFF172033.toInt()) })
            info.addView(TextView(this).apply { text = String.format("%02d:%02d daily", task.hour, task.minute); textSize = 13f; setTextColor(0xFF526174.toInt()) })
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply { text = "Delete"; setOnClickListener { AlarmHelper.cancel(this@MainActivity, task.id); save(tasks().filterNot { it.id == task.id }); render() } })
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = 10; list.addView(row, lp)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("routine_alarm", "Routine alarms", NotificationManager.IMPORTANCE_HIGH).apply { description = "Daily routine reminders"; enableVibration(true) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

object AlarmHelper {
    fun schedule(context: Context, id: Int, name: String, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("id", id).putExtra("name", name).putExtra("hour", hour).putExtra("minute", minute)
        val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) else am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }
    fun cancel(context: Context, id: Int) {
        val pi = PendingIntent.getBroadcast(context, id, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
}
