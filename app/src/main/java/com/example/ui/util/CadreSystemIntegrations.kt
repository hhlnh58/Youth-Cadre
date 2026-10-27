package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.notification.CadreNotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

object CadreSystemIntegrations {

    const val CHANNEL_ID = "cadre_missions_channel"
    const val CHANNEL_NAME = "Nhắc nhở Nhiệm vụ & Lịch Đoàn"

    // -------------------------------------------------------------
    // 1. NHẮC NHỞ & THÔNG BÁO (REMINDERS & NOTIFICATIONS)
    // -------------------------------------------------------------
    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo tác nghiệp chỉ đạo công tác Đoàn và phong trào thanh thiếu nhi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendTestNotification(
        context: Context,
        title: String = "🔔 Nhắc nhở Tác nghiệp Cán bộ Đoàn",
        message: String = "Hệ thống kết nối toàn diện đã sẵn sàng! Nhiệm vụ và lịch họp sẽ được thông báo đúng giờ."
    ): Boolean {
        initNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Vui lòng cho phép quyền Thông báo để nhận nhắc nhở!", Toast.LENGTH_LONG).show()
                return false
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notifId = (System.currentTimeMillis() % 10000).toInt()
            notificationManager.notify(notifId, builder.build())
            Toast.makeText(context, "Đã gửi thông báo nhắc nhở thành công!", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: SecurityException) {
            Toast.makeText(context, "Chưa cấp quyền thông báo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return false
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi gửi thông báo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * Gửi ngay Điểm tin Công tác Đoàn và Cảnh báo việc tồn đọng lên thanh thông báo
     */
    fun sendDailyBriefingNotification(context: Context) {
        val intent = Intent(context, CadreNotificationReceiver::class.java).apply {
            action = CadreNotificationReceiver.ACTION_DAILY_BRIEFING
        }
        context.sendBroadcast(intent)
        Toast.makeText(context, "Đã phát Điểm tin Công tác hằng ngày & Việc tồn đọng!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Đặt báo thức lặp lại hằng ngày (Mặc định 07:30 sáng)
     */
    fun scheduleDailyBriefingAlarm(context: Context, hour: Int = 7, minute: Int = 30) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val intent = Intent(context, CadreNotificationReceiver::class.java).apply {
                action = CadreNotificationReceiver.ACTION_DAILY_BRIEFING
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                7701,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setInexactRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                android.app.AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            android.util.Log.e("CadreSystemIntegrations", "Error scheduling daily alarm", e)
        }
    }

    /**
     * Đặt báo thức nhắc nhở nhiệm vụ sắp đến hạn
     */
    fun scheduleTaskReminder(
        context: Context,
        taskId: String,
        taskTitle: String,
        dueDateStr: String, // dd/MM/yyyy
        dueTimeStr: String, // HH:mm
        minutesBefore: Int = 15
    ): Boolean {
        if (dueDateStr.isBlank()) return false
        try {
            CadreNotificationReceiver.ensureNotificationChannels(context)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val time = dueTimeStr.ifBlank { "09:00" }
            val date = sdf.parse("$dueDateStr $time") ?: return false
            val dueExactMs = date.time
            val now = System.currentTimeMillis()

            // Nếu hạn đã qua hơn 1 phút trước, không nhắc nhở nữa
            if (dueExactMs < now - 60_000) {
                return false
            }

            // Nếu thời điểm nhắc trước đã qua nhưng hạn vẫn còn sắp tới, đặt nhắc sau 4 giây
            var triggerMs = dueExactMs - (minutesBefore * 60 * 1000)
            if (triggerMs <= now) {
                triggerMs = now + 4000
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return false
            val intent = Intent(context, CadreNotificationReceiver::class.java).apply {
                action = CadreNotificationReceiver.ACTION_TASK_REMINDER
                putExtra(CadreNotificationReceiver.EXTRA_TITLE, "⚡ Nhắc nhở Nhiệm vụ Đoàn")
                putExtra(CadreNotificationReceiver.EXTRA_MESSAGE, "Nhiệm vụ \"$taskTitle\" sắp đến hạn ($dueDateStr $time)")
                putExtra(CadreNotificationReceiver.EXTRA_ENTITY_ID, taskId)
                putExtra(CadreNotificationReceiver.EXTRA_TYPE, "TASK")
            }

            val reqCode = taskId.hashCode().let { if (it == 0) 1001 else kotlin.math.abs(it) }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } catch (se: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            }
            return true
        } catch (e: Exception) {
            android.util.Log.e("CadreSystemIntegrations", "Error scheduling task reminder", e)
            return false
        }
    }

    /**
     * Đặt báo thức nhắc nhở hạn quyết toán kinh phí
     */
    fun scheduleSettlementReminder(
        context: Context,
        settlementId: String,
        settlementTitle: String,
        deadlineStr: String
    ): Boolean {
        if (deadlineStr.isBlank()) return false
        try {
            CadreNotificationReceiver.ensureNotificationChannels(context)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = sdf.parse("$deadlineStr 09:00") ?: return false
            val dueExactMs = date.time
            val now = System.currentTimeMillis()

            if (dueExactMs < now - 60_000) {
                return false
            }

            var triggerMs = dueExactMs - (24 * 60 * 60 * 1000) // Nhắc trước 1 ngày
            if (triggerMs <= now) {
                triggerMs = now + 4000
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return false
            val intent = Intent(context, CadreNotificationReceiver::class.java).apply {
                action = CadreNotificationReceiver.ACTION_SETTLEMENT_REMINDER
                putExtra(CadreNotificationReceiver.EXTRA_TITLE, "💰 Nhắc nhở Hạn Quyết toán kinh phí")
                putExtra(CadreNotificationReceiver.EXTRA_MESSAGE, "Mục quyết toán \"$settlementTitle\" sắp đến hạn ($deadlineStr). Vui lòng hoàn thiện chứng từ thanh quyết toán.")
                putExtra(CadreNotificationReceiver.EXTRA_ENTITY_ID, settlementId)
                putExtra(CadreNotificationReceiver.EXTRA_TYPE, "SETTLEMENT")
            }

            val reqCode = settlementId.hashCode().let { if (it == 0) 3003 else kotlin.math.abs(it) }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } catch (se: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            }
            return true
        } catch (e: Exception) {
            android.util.Log.e("CadreSystemIntegrations", "Error scheduling settlement reminder", e)
            return false
        }
    }

    /**
     * Đặt báo thức nhắc nhở lịch công tác / hội họp
     */
    fun scheduleEventReminder(
        context: Context,
        eventId: String,
        eventTitle: String,
        dateStr: String, // dd/MM/yyyy
        timeStr: String, // HH:mm
        location: String,
        minutesBefore: Int = 30
    ): Boolean {
        if (dateStr.isBlank()) return false
        try {
            CadreNotificationReceiver.ensureNotificationChannels(context)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val time = timeStr.ifBlank { "08:00" }
            val date = sdf.parse("$dateStr $time") ?: return false
            val eventExactMs = date.time
            val now = System.currentTimeMillis()

            // Nếu lịch đã qua hơn 1 phút trước, không nhắc nhở nữa
            if (eventExactMs < now - 60_000) {
                return false
            }

            // Nếu thời điểm nhắc trước đã qua nhưng lịch vẫn còn sắp tới, đặt nhắc sau 4 giây
            var triggerMs = eventExactMs - (minutesBefore * 60 * 1000)
            if (triggerMs <= now) {
                triggerMs = now + 4000
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return false
            val intent = Intent(context, CadreNotificationReceiver::class.java).apply {
                action = CadreNotificationReceiver.ACTION_EVENT_REMINDER
                val locText = if (location.isNotBlank()) " tại $location" else ""
                putExtra(CadreNotificationReceiver.EXTRA_TITLE, "📅 Nhắc hẹn Lịch công tác / Họp")
                putExtra(CadreNotificationReceiver.EXTRA_MESSAGE, "\"$eventTitle\" lúc $time ngày $dateStr$locText")
                putExtra(CadreNotificationReceiver.EXTRA_ENTITY_ID, eventId)
                putExtra(CadreNotificationReceiver.EXTRA_TYPE, "EVENT")
            }

            val reqCode = eventId.hashCode().let { if (it == 0) 2002 else kotlin.math.abs(it) }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } catch (se: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            }
            return true
        } catch (e: Exception) {
            android.util.Log.e("CadreSystemIntegrations", "Error scheduling event reminder", e)
            return false
        }
    }

    /**
     * Cập nhật tức thời tất cả các widget ngoài màn hình chính Android
     */
    fun updateAppWidget(context: Context) {
        try {
            com.example.ui.widget.CadreAppWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            android.util.Log.w("CadreSystemIntegrations", "Lỗi update widget", e)
        }
    }

    // -------------------------------------------------------------
    // 2. GOOGLE LỊCH (GOOGLE CALENDAR INTEGRATION)
    // -------------------------------------------------------------
    fun openGoogleCalendarApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.calendar")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        } catch (_: Exception) {}

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.calendar/time/")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Tự động tạo sự kiện vào Google Calendar trên máy
     * Thử ghi trực tiếp vào CalendarProvider nếu có quyền, hoặc mở Intent nếu cần
     */
    fun exportEventToGoogleCalendar(
        context: Context,
        title: String,
        description: String,
        location: String,
        dateStr: String, // dd/MM/yyyy
        timeStr: String, // HH:mm
        endTimeStr: String = ""
    ): Boolean {
        var startMs = System.currentTimeMillis()
        var endMs = startMs + 90 * 60 * 1000
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = sdf.parse("$dateStr ${timeStr.ifBlank { "08:00" }}")
            if (date != null) {
                startMs = date.time
                endMs = if (endTimeStr.isNotBlank()) {
                    sdf.parse("$dateStr $endTimeStr")?.time ?: (startMs + 90 * 60 * 1000)
                } else {
                    startMs + 90 * 60 * 1000
                }
            }
        } catch (_: Exception) {}

        // 1. Thử ghi trực tiếp vào Google Calendar của thiết bị qua ContentResolver
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                var targetCalId: Long = -1L
                val projection = arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.ACCOUNT_TYPE,
                    CalendarContract.Calendars.IS_PRIMARY
                )
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    CalendarContract.Calendars.VISIBLE + " = 1",
                    null,
                    null
                )?.use { cursor ->
                    var fallbackId = -1L
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val accType = cursor.getString(2) ?: ""
                        val isPrimary = try { cursor.getInt(3) } catch (_: Exception) { 0 }
                        if (fallbackId == -1L) fallbackId = id
                        if (accType.contains("google", ignoreCase = true) || isPrimary == 1) {
                            targetCalId = id
                            break
                        }
                    }
                    if (targetCalId == -1L) targetCalId = fallbackId
                }

                if (targetCalId != -1L) {
                    val values = android.content.ContentValues().apply {
                        put(CalendarContract.Events.CALENDAR_ID, targetCalId)
                        put(CalendarContract.Events.TITLE, title)
                        put(CalendarContract.Events.DESCRIPTION, description)
                        put(CalendarContract.Events.EVENT_LOCATION, location)
                        put(CalendarContract.Events.DTSTART, startMs)
                        put(CalendarContract.Events.DTEND, endMs)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                        put(CalendarContract.Events.HAS_ALARM, 1)
                    }
                    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    if (uri != null) {
                        val eventId = uri.lastPathSegment?.toLongOrNull()
                        if (eventId != null) {
                            val reminderValues = android.content.ContentValues().apply {
                                put(CalendarContract.Reminders.EVENT_ID, eventId)
                                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                                put(CalendarContract.Reminders.MINUTES, 30)
                            }
                            try {
                                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                            } catch (_: Exception) {}
                        }
                        Toast.makeText(context, "✅ Đã tự động tạo sự kiện trên Google Lịch!", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("CadreSystemIntegrations", "Ghi trực tiếp calendar thất bại, chuyển intent", e)
            }
        }

        // 2. Chuyển sang Native Calendar Intent nếu chưa cấp quyền hoặc không tìm thấy ID
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Đang mở Google Lịch...", Toast.LENGTH_SHORT).show()
            return true
        } catch (_: Exception) {}

        // 3. Fallback Web Google Calendar URL
        return try {
            val webSdf = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
            val dateParam = "${webSdf.format(Date(startMs))}/${webSdf.format(Date(endMs))}"
            val webUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                    "&text=${Uri.encode(title)}" +
                    "&dates=$dateParam" +
                    "&details=${Uri.encode(description)}" +
                    "&location=${Uri.encode(location)}"

            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Toast.makeText(context, "Đang mở Google Lịch trên trình duyệt...", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở Google Lịch: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // -------------------------------------------------------------
    // 3. DANH BẠ MÁY (CONTACTS INTEGRATION)
    // -------------------------------------------------------------
    data class ContactData(
        val name: String,
        val phone: String
    )

    fun resolveContact(context: Context, contactUri: Uri): ContactData? {
        try {
            var name = ""
            var phone = ""

            // 1. Thử đọc trực tiếp nếu URI là data phone row (content://.../data/...)
            try {
                context.contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: ""
                        if (phoneIdx != -1) phone = cursor.getString(phoneIdx) ?: ""
                    }
                }
            } catch (_: Exception) {}

            if (name.isNotBlank()) {
                return ContactData(name = name.trim(), phone = phone.trim())
            }

            // 2. Thử truy vấn Contact ID nếu URI là contacts lookup
            var contactId = ""
            try {
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: ""
                        if (idIdx != -1) contactId = cursor.getString(idIdx) ?: ""
                    }
                }
            } catch (_: Exception) {}

            // 3. Tìm số điện thoại theo contactId
            if (contactId.isNotBlank()) {
                try {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            val pIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (pIdx != -1) phone = pCursor.getString(pIdx) ?: ""
                        }
                    }
                } catch (_: SecurityException) {
                    // Quyền chưa được cấp nhưng đã lấy được Tên
                } catch (_: Exception) {}
            }

            if (name.isNotBlank()) {
                return ContactData(name = name.trim(), phone = phone.trim())
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi đọc danh bạ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    // -------------------------------------------------------------
    // 4. TỆP & HÌNH ẢNH (STORAGE & MEDIA)
    // -------------------------------------------------------------
    fun queryFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "van_ban_${System.currentTimeMillis()}"
    }
}
