package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.local.CadreDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CadreAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.example.ACTION_WIDGET_REFRESH"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CadreAppWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, CadreAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CadreAppWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, widgetIds)
            Toast.makeText(context, "✅ Đã làm mới Widget Cán bộ Đoàn!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return

        widgetScope.launch {
            try {
                val db = CadreDatabase.getDatabase(context)
                val tasks = db.cadreDao().getAllTasksSync()
                val events = db.cadreDao().getAllCalendarEventsSync()
                val settlements = db.cadreDao().getAllSettlementsSync()

                val todaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayStr = todaySdf.format(Date())

                val displayDateSdf = SimpleDateFormat("EEEE, dd/MM", Locale("vi"))
                val dateDisplay = displayDateSdf.format(Date()).replaceFirstChar { it.uppercase() }

                val p1Count = tasks.count { it.priority == "P1" && !it.isCompleted }
                val todayEventsCount = events.count { it.date == todayStr && !it.isAttended }
                val pendingSettlementCount = settlements.count { !it.isCompleted }

                // Top upcoming event: today first, then future, ignore attended
                val nextEvent = events.firstOrNull { it.date == todayStr && !it.isAttended }
                    ?: events.firstOrNull { it.date > todayStr && !it.isAttended }
                    ?: events.firstOrNull { !it.isAttended }
                    ?: events.firstOrNull()

                // Top pending task
                val nextTask = tasks.firstOrNull { it.priority == "P1" && !it.isCompleted }
                    ?: tasks.firstOrNull { !it.isCompleted }
                val nextSettlement = settlements.firstOrNull { !it.isCompleted }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_cadre_dashboard)

                    views.setTextViewText(R.id.widget_current_date, dateDisplay)
                    val p1Text = if (p1Count > 0) "P1 Khẩn: $p1Count" else if (pendingSettlementCount > 0) "Quyết toán: $pendingSettlementCount" else "P1 Khẩn: 0"
                    views.setTextViewText(R.id.widget_p1_count, p1Text)
                    views.setTextViewText(R.id.widget_today_event_count, "Lịch hôm nay: $todayEventsCount")

                    if (nextEvent != null) {
                        val timePrefix = if (nextEvent.startTime.isNotBlank()) "${nextEvent.startTime} " else ""
                        views.setTextViewText(R.id.widget_next_event_title, "$timePrefix• ${nextEvent.title}")
                        views.setTextViewText(
                            R.id.widget_next_event_location,
                            if (nextEvent.location.isNotBlank()) "📍 ${nextEvent.location}" else "📍 Cơ quan Đoàn"
                        )
                    } else {
                        views.setTextViewText(R.id.widget_next_event_title, "Chưa có lịch họp hôm nay")
                        views.setTextViewText(R.id.widget_next_event_location, "Bấm để mở lịch công tác")
                    }

                    if (nextTask != null) {
                        views.setTextViewText(R.id.widget_next_task_title, "[${nextTask.priority}] ${nextTask.title}")
                        val deadlineStr = if (nextTask.dueDate.isNotBlank()) "Hạn: ${nextTask.dueDate}" else "Chưa đặt hạn"
                        val assigneeStr = if (nextTask.responsiblePersonName.isNotBlank()) " • ${nextTask.responsiblePersonName}" else ""
                        views.setTextViewText(R.id.widget_next_task_meta, "$deadlineStr$assigneeStr")
                    } else if (nextSettlement != null) {
                        views.setTextViewText(R.id.widget_next_task_title, "💰 [Quyết toán] ${nextSettlement.title}")
                        val respStr = if (nextSettlement.responsiblePerson.isNotBlank()) " • ${nextSettlement.responsiblePerson}" else ""
                        views.setTextViewText(R.id.widget_next_task_meta, "Hạn: ${nextSettlement.deadline}$respStr")
                    } else {
                        views.setTextViewText(R.id.widget_next_task_title, "Không có nhiệm vụ tồn đọng")
                        views.setTextViewText(R.id.widget_next_task_meta, "Mọi nhiệm vụ đã hoàn thành tốt!")
                    }

                    // Click on Refresh Button
                    val refreshIntent = Intent(context, CadreAppWidgetProvider::class.java).apply {
                        action = ACTION_WIDGET_REFRESH
                    }
                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

                    // Click on Root or Schedules or Tasks opens MainActivity
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId * 10,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

                    // Event section opens Calendar screen
                    val calendarIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "CALENDAR")
                    }
                    val calendarPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId * 10 + 1,
                        calendarIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_event_container, calendarPendingIntent)

                    // Task section opens Tasks screen
                    val taskIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "TASKS")
                    }
                    val taskPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId * 10 + 2,
                        taskIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_task_container, taskPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("CadreAppWidgetProvider", "Error updating widget", e)
            }
        }
    }
}
