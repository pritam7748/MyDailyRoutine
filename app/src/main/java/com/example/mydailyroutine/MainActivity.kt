/**
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */
fun main() {
    println("Hello, world!!!")
}package com.example.mydailyroutine

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var countLabel: TextView
    private val prefs by lazy { getSharedPreferences("routine", MODE_PRIVATE) }
    private data class Task(val id: Int, val name: String, val hour: Int, val minute: Int)

    private val ink = Color.rgb(24, 30, 68)
    private val muted = Color.rgb(205, 211, 244)
    private val accent = Color.rgb(164, 151, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(30, 31, 83)
        window.navigationBarColor = Color.rgb(24, 25, 67)
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
            background = gradient(intArrayOf(Color.rgb(34, 35, 91), Color.rgb(66, 56, 130), Color.rgb(31, 48, 103)), 0)
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("MY SPACE  •  DAILY PLANNER", 10f, Color.rgb(184, 178, 255), true))
        brand.addView(label("My Daily Routine", 27f, Color.WHITE, true).apply { setPadding(0, dp(3), 0, 0) })
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(label("✦", 28f, Color.rgb(255, 224, 153), true).apply { gravity = Gravity.CENTER; background = glass(0.28f, 24); setPadding(dp(13), dp(8), dp(13), dp(8)) })
        root.addView(top)
        root.addView(label("A little progress every day adds up.", 13f, muted, false).apply { setPadding(0, dp(5), 0, dp(18)) })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = glass(0.17f, 24)
            elevation = dp(3).toFloat()
        }
        hero.addView(label("TODAY", 10f, Color.rgb(205, 198, 255), true))
        hero.addView(label(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 20f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, dp(3)) })
        hero.addView(label("Build your day, one task at a time.", 12f, muted, false))
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, 0) }
        countLabel = label("0 planned tasks", 12f, Color.WHITE, true)
        stats.addView(countLabel, LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(label("●  DAILY REMINDERS", 10f, Color.rgb(178, 255, 221), true))
        hero.addView(stats)
        val hp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
        root.addView(hero, hp)

        val section = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(2), 0, dp(2), dp(10)) }
        section.addView(label("Your routines", 19f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        section.addView(label("SORTED BY TIME", 9f, Color.rgb(191, 190, 236), true))
        root.addView(section)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { isFillViewport = false; clipToPadding = false; addView(list); overScrollMode = View.OVER_SCROLL_NEVER }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val add = Button(this).apply {
            text = "+   Add a routine"
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.rgb(39, 35, 91))
            typeface = Typeface.DEFAULT_BOLD
            background = gradient(intArrayOf(Color.rgb(218, 213, 255), Color.rgb(166, 222, 255)), 18)
            elevation = dp(7).toFloat()
            setPadding(dp(12), dp(7), dp(12), dp(7))
            setOnClickListener { addTask() }
        }
        val addLp = LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) }
        root.addView(add, addLp)
        setContentView(root)
        render()
    }

    private fun tasks(): MutableList<Task> = prefs.getString("tasks", "").orEmpty().lines().filter { it.isNotBlank() }.mapNotNull {
        val p = it.split("|"); if (p.size != 4) null else runCatching { Task(p[0].toInt(), p[1], p[2].toInt(), p[3].toInt()) }.getOrNull()
    }.toMutableList()

    private fun save(items: List<Task>) {
        prefs.edit().putString("tasks", items.joinToString("\n") { "${it.id}|${it.name.replace("|", " ").replace("\n", " ")}|${it.hour}|${it.minute}" }).apply()
    }

    private fun addTask() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(10), dp(22), 0) }
        val name = EditText(this).apply {
            hint = "e.g. Math practice"
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(244, 245, 255), 14, Color.rgb(220, 220, 242))
            setTextColor(ink); setHintTextColor(Color.GRAY)
        }
        box.addView(name, LinearLayout.LayoutParams(-1, dp(52)))
        val choose = TextView(this).apply { text = "Choose reminder time"; textSize = 13f; setTextColor(ink); setPadding(0, dp(17), 0, dp(3)) }
        box.addView(choose)
        val time = TimePicker(this).apply { setIs24HourView(false) }
        box.addView(time)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add daily routine")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save routine", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val taskName = name.text.toString().trim()
                if (taskName.isEmpty()) {
                    name.error = "Please enter a task name"
                    name.requestFocus()
                    return@setOnClickListener
                }
                val hour = if (Build.VERSION.SDK_INT >= 23) time.hour else time.currentHour
                val minute = if (Build.VERSION.SDK_INT >= 23) time.minute else time.currentMinute
                val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val updated = tasks().apply { add(Task(id, taskName, hour, minute)) }
                save(updated)
                AlarmHelper.schedule(this, id, taskName, hour, minute)
                render()
                dialog.dismiss()
                Toast.makeText(this, "Routine saved", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun render() {
        list.removeAllViews()
        val all = tasks().sortedWith(compareBy<Task> { it.hour }.thenBy { it.minute })
        countLabel.text = "${all.size} planned ${if (all.size == 1) "task" else "tasks"}"
        if (all.isEmpty()) {
            val empty = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(30), dp(20), dp(30)); background = glass(0.12f, 22) }
            empty.addView(label("☁", 32f, Color.rgb(211, 208, 255), true).apply { gravity = Gravity.CENTER })
            empty.addView(label("Your day is a blank canvas", 17f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(5)) })
            empty.addView(label("Add your first routine and set a reminder.", 12f, muted, false).apply { gravity = Gravity.CENTER })
            list.addView(empty)
        }
        all.forEachIndexed { index, task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(10), dp(14))
                background = glass(0.16f, 20)
                elevation = dp(2).toFloat()
            }
            val timeBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = rounded(Color.argb(48, 205, 199, 255), 15, Color.argb(80, 255, 255, 255))
            }
            timeBox.addView(label(String.format(Locale.getDefault(), "%02d:%02d", task.hour, task.minute), 17f, Color.WHITE, true))
            timeBox.addView(label("DAILY", 8f, Color.rgb(206, 202, 255), true).apply { setPadding(0, dp(3), 0, 0) })
            card.addView(timeBox)
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(4), 0) }
            info.addView(label(task.name, 15f, Color.WHITE, true))
            info.addView(label(if (index == 0) "Next on your schedule" else "Daily reminder", 11f, muted, false).apply { setPadding(0, dp(4), 0, 0) })
            card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            val delete = TextView(this).apply {
                text = "×"; textSize = 23f; gravity = Gravity.CENTER; setTextColor(Color.rgb(255, 190, 202))
                background = rounded(Color.argb(35, 255, 255, 255), 13, Color.argb(65, 255, 255, 255))
                setPadding(dp(10), dp(3), dp(10), dp(3))
                contentDescription = "Delete ${task.name}"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity).setTitle("Delete routine?").setMessage(task.name)
                        .setNegativeButton("Keep", null).setPositiveButton("Delete") { _, _ ->
                            AlarmHelper.cancel(this@MainActivity, task.id)
                            save(tasks().filterNot { it.id == task.id })
                            render()
                        }.show()
                }
            }
            card.addView(delete)
            val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
            list.addView(card, lp)
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun glass(alpha: Float, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(Color.argb((alpha * 255).toInt(), 255, 255, 255))
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(75, 255, 255, 255))
    }

    private fun rounded(color: Int, radius: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }

    private fun gradient(colors: IntArray, radius: Int): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("routine_alarm", "Routine alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Daily routine reminders"; enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

object AlarmHelper {
    fun schedule(context: Context, id: Int, name: String, hour: Int, minute: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("id", id).putExtra("name", name).putExtra("hour", hour).putExtra("minute", minute)
        val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            else am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun cancel(context: Context, id: Int) {
        val pi = PendingIntent.getBroadcast(context, id, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
}
