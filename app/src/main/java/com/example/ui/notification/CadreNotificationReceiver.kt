package com.example.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CadreNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.example.ACTION_TASK_REMINDER"
        const val ACTION_EVENT_REMINDER = "com.example.ACTION_EVENT_REMINDER"
        const val ACTION_SETTLEMENT_REMINDER = "com.example.ACTION_SETTLEMENT_REMINDER"
        const val ACTION_TEST_NOTIFICATION = "com.example.ACTION_TEST_NOTIFICATION"
        const val ACTION_DAILY_BRIEFING = "com.example.ACTION_DAILY_BRIEFING"

        const val CHANNEL_ID_TASKS = "cadre_tasks_channel"
        const val CHANNEL_ID_EVENTS = "cadre_calendar_channel"
        const val CHANNEL_ID_DAILY = "cadre_daily_channel"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_ENTITY_ID = "extra_entity_id"
        const val EXTRA_TYPE = "extra_type" // "TASK", "EVENT", or "SETTLEMENT"

        fun ensureNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Tasks Channel
                val taskChannel = NotificationChannel(
                    CHANNEL_ID_TASKS,
                    "Nhiệm vụ Cán bộ Đoàn",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc nhở hạn chót và tiến độ nhiệm vụ chỉ đạo"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(taskChannel)

                // Calendar Channel
                val eventChannel = NotificationChannel(
                    CHANNEL_ID_EVENTS,
                    "Lịch công tác & Họp Đoàn",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc hẹn các cuộc họp, hội nghị và chuyến đi cơ sở"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(eventChannel)

                // Daily Briefing Channel
                val dailyChannel = NotificationChannel(
                    CHANNEL_ID_DAILY,
                    "Điểm tin Công tác hằng ngày & Việc tồn đọng",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Báo cáo tổng hợp lịch trình trong ngày và cảnh báo nhiệm vụ tồn đọng"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(dailyChannel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        ensureNotificationChannels(context)

        if (intent.action == ACTION_DAILY_BRIEFING) {
            handleDailyBriefing(context)
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Cán bộ Đoàn - Nhắc nhở"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Đồng chí có nhiệm vụ hoặc lịch trình cần thực hiện."
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "TASK"
        val entityId = intent.getStringExtra(EXTRA_ENTITY_ID) ?: ""

        val channelId = if (type == "EVENT") CHANNEL_ID_EVENTS else CHANNEL_ID_TASKS

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", if (type == "EVENT") "CALENDAR" else "TASKS")
            putExtra("selected_id", entityId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = entityId.hashCode().let { if (it == 0) (System.currentTimeMillis() % 100000).toInt() else kotlin.math.abs(it) }
        notificationManager.notify(notifId, builder.build())
    }

    private fun handleDailyBriefing(context: Context) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.example.data.local.CadreDatabase.getDatabase(context)
                val tasks = db.cadreDao().getAllTasksSync()
                val events = db.cadreDao().getAllCalendarEventsSync()
                val settlements = db.cadreDao().getAllSettlementsSync()

                val todaySdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val todayStr = todaySdf.format(java.util.Date())

                val todayEvents = events.filter { it.date == todayStr && !it.isAttended }
                val overdueTasks = tasks.filter { !it.isCompleted && it.dueDate.isNotBlank() && it.dueDate < todayStr }
                val p1Tasks = tasks.filter { !it.isCompleted && it.priority == "P1" }
                val todayTasks = tasks.filter { !it.isCompleted && it.dueDate == todayStr }
                val pendingSettlements = settlements.filter { !it.isCompleted }

                val title = "📢 Điểm tin Công tác Đoàn ngày $todayStr"
                val body = buildString {
                    if (todayEvents.isNotEmpty()) {
                        appendLine("📅 LỊCH TRÌNH HÔM NAY (${todayEvents.size}):")
                        todayEvents.take(3).forEach { ev ->
                            val loc = if (ev.location.isNotBlank()) " tại ${ev.location}" else ""
                            appendLine("  • ${ev.startTime}: ${ev.title}$loc")
                        }
                    } else {
                        appendLine("📅 Hôm nay chưa có lịch họp/công tác mới.")
                    }

                    appendLine()

                    if (overdueTasks.isNotEmpty()) {
                        appendLine("⚠️ NHIỆM VỤ TỒN ĐỌNG (${overdueTasks.size} việc):")
                        overdueTasks.take(3).forEach { t ->
                            appendLine("  • [${t.priority}] ${t.title} (Hạn: ${t.dueDate})")
                        }
                    } else if (p1Tasks.isNotEmpty()) {
                        appendLine("⚡ NHIỆM VỤ KHẨN P1 (${p1Tasks.size} việc):")
                        p1Tasks.take(2).forEach { t ->
                            appendLine("  • ${t.title}")
                        }
                    } else if (todayTasks.isNotEmpty()) {
                        appendLine("⚡ NHIỆM VỤ ĐẾN HẠN HÔM NAY: ${todayTasks.size} việc")
                    } else {
                        appendLine("✅ Các nhiệm vụ đều đang bảo đảm tiến độ tốt!")
                    }

                    if (pendingSettlements.isNotEmpty()) {
                        appendLine()
                        appendLine("💰 QUYẾT TOÁN CẦN LÀM (${pendingSettlements.size} mục):")
                        pendingSettlements.take(2).forEach { s ->
                            appendLine("  • ${s.title} (Hạn: ${s.deadline})")
                        }
                    }
                }.trim()

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", if (todayEvents.isNotEmpty()) "CALENDAR" else "TASKS")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    9901,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText("${todayEvents.size} lịch trình • ${overdueTasks.size} việc tồn đọng")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(longArrayOf(0, 200, 100, 200))

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(8888, builder.build())
            } catch (e: Exception) {
                android.util.Log.e("CadreNotificationReceiver", "Error sending daily briefing", e)
            }
        }
    }
}
