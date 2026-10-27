package com.example.ui.screens.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CalendarEventEntity
import com.example.ui.theme.*
import com.example.ui.util.CadreSystemIntegrations
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventDetailDialog(
    event: CalendarEventEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        eventType: String,
        date: String,
        startTime: String,
        endTime: String,
        location: String,
        destinationUnit: String,
        contactPerson: String,
        contactPhone: String,
        purpose: String,
        participants: String
    ) -> Unit,
    onDelete: ((CalendarEventEntity) -> Unit)? = null
) {
    val isEdit = event != null
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    var title by remember(event?.id) { mutableStateOf(event?.title ?: "") }
    var eventType by remember(event?.id) { mutableStateOf(event?.eventType ?: "MEETING") }
    var date by remember(event?.id) { mutableStateOf(event?.date?.ifEmpty { todayStr } ?: todayStr) }
    var startTime by remember(event?.id) { mutableStateOf(event?.startTime ?: "08:30") }
    var endTime by remember(event?.id) { mutableStateOf(event?.endTime ?: "10:30") }
    var location by remember(event?.id) { mutableStateOf(event?.location ?: "Hội trường cơ quan Đoàn") }
    var destinationUnit by remember(event?.id) { mutableStateOf(event?.destinationUnit ?: "") }
    var contactPerson by remember(event?.id) { mutableStateOf(event?.contactPerson ?: "") }
    var contactPhone by remember(event?.id) { mutableStateOf(event?.contactPhone ?: "") }
    var purpose by remember(event?.id) { mutableStateOf(event?.purpose ?: "") }
    var participants by remember(event?.id) { mutableStateOf(event?.participants ?: "Thường trực Ban Thường vụ") }

    // Google Calendar Sync & System Notification Reminders
    var syncToGoogleCalendar by remember(event?.id) { mutableStateOf(true) }
    var enablePhoneReminder by remember(event?.id) { mutableStateOf(true) }
    var reminderOffsetMinutes by remember(event?.id) { mutableIntStateOf(30) }

    // Interactive Date & Time Picker Helpers
    val showDatePicker = {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(date)?.let { cal.time = it }
        } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(newCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showStartTimePicker = {
        var hour = 8
        var minute = 30
        try {
            val parts = startTime.split(":")
            if (parts.size == 2) {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            }
        } catch (_: Exception) {}
        TimePickerDialog(
            context,
            { _, h, m ->
                startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            },
            hour,
            minute,
            true
        ).show()
    }

    val showEndTimePicker = {
        var hour = 10
        var minute = 30
        try {
            val parts = endTime.split(":")
            if (parts.size == 2) {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            }
        } catch (_: Exception) {}
        TimePickerDialog(
            context,
            { _, h, m ->
                endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            },
            hour,
            minute,
            true
        ).show()
    }

    // Contact Picker Launcher from System Contacts (no dangerous READ_CONTACTS needed)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            val contact = CadreSystemIntegrations.resolveContact(context, uri)
            if (contact != null) {
                contactPerson = contact.name
                if (contact.phone.isNotBlank()) {
                    contactPhone = contact.phone
                }
                Toast.makeText(context, "Đã trích xuất từ Danh bạ: ${contact.name} (${contact.phone})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("event_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                if (isEdit) "Chi tiết Lịch công tác" else "Thêm Lịch công tác mới",
                                fontWeight = FontWeight.Black,
                                color = NavyHeroDark,
                                fontSize = 17.sp
                            )
                            Text(
                                "Chỉ đạo điều hành • Lịch họp & Công tác cơ sở",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tên cuộc họp / Sự kiện / Chuyến công tác *", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("VD: Họp Ban Thường vụ chuẩn bị Tháng Thanh niên", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("event_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDeep,
                            unfocusedBorderColor = BorderLight
                        ),
                        singleLine = false,
                        maxLines = 2
                    )

                    // Event Type Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Phân loại hoạt động:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("MEETING", "Họp / Hội nghị", Color(0xFF2563EB)),
                                Triple("FIELD_VISIT", "Đi cơ sở", Color(0xFF059669)),
                                Triple("EVENT", "Sự kiện / Hoạt động", Color(0xFFD97706))
                            ).forEach { (typeKey, typeLabel, typeColor) ->
                                val selected = eventType == typeKey
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { eventType = typeKey },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) typeColor.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (selected) typeColor else BorderLight)
                                ) {
                                    Text(
                                        typeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) typeColor else TextSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Date & Time with Interactive Pickers
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Thời gian tổ chức:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                            // Row 1: Full-width Date Picker
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Ngày tổ chức") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker() },
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                leadingIcon = {
                                    IconButton(onClick = showDatePicker, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày", tint = NavyDeep, modifier = Modifier.size(17.dp))
                                    }
                                },
                                trailingIcon = {
                                    TextButton(onClick = showDatePicker) {
                                        Text("Chọn ngày", fontWeight = FontWeight.Bold, color = NavyDeep, fontSize = 11.5.sp)
                                    }
                                }
                            )

                            // Quick Date Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Hôm nay" to 0,
                                    "Ngày mai" to 1,
                                    "+3 ngày" to 3,
                                    "+1 tuần" to 7
                                ).forEach { (label, daysOffset) ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysOffset) }
                                                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = PureWhite,
                                        border = BorderStroke(0.6.dp, BorderLight)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.5.sp,
                                            color = NavyDeep,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Row 2: Two Equal-Width Columns for Start Time & End Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("Giờ bắt đầu") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    leadingIcon = {
                                        IconButton(onClick = showStartTimePicker, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Schedule, contentDescription = "Chọn giờ bắt đầu", tint = NavyDeep, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                )

                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text("Giờ kết thúc") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    leadingIcon = {
                                        IconButton(onClick = showEndTimePicker, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Chọn giờ kết thúc", tint = TextMuted, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }


                    // Location
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Địa điểm tổ chức *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(18.dp)) }
                    )

                    // Destination Unit
                    OutlinedTextField(
                        value = destinationUnit,
                        onValueChange = { destinationUnit = it },
                        label = { Text("Đơn vị tiếp đón / Tổ chức liên kết") },
                        placeholder = { Text("VD: Đoàn Trường Đại học Bách Khoa, Huyện Đoàn...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Contact Person & Phone with Direct Contacts Picker Integration
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(16.dp))
                                    Text("Người chủ trì & Đầu mối liên hệ:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        contactPickerLauncher.launch(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.ContactPhone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Từ Danh bạ máy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = contactPerson,
                                    onValueChange = { contactPerson = it },
                                    label = { Text("Người liên hệ / Chủ trì") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = contactPhone,
                                    onValueChange = { contactPhone = it },
                                    label = { Text("Số điện thoại") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Purpose / Agenda
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Mục đích / Nội dung trọng tâm cuộc họp") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Participants
                    OutlinedTextField(
                        value = participants,
                        onValueChange = { participants = it },
                        label = { Text("Thành phần tham dự") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Phone Notification Reminder Settings
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Thông báo trên điện thoại", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                        Text("Báo thức & Rung trước giờ họp", fontSize = 10.sp, color = Color(0xFFB45309))
                                    }
                                }
                                Switch(
                                    checked = enablePhoneReminder,
                                    onCheckedChange = { enablePhoneReminder = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFB45309))
                                )
                            }

                            if (enablePhoneReminder) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "15 phút trước" to 15,
                                        "30 phút trước" to 30,
                                        "1 giờ trước" to 60
                                    ).forEach { (label, mins) ->
                                        val isSel = reminderOffsetMinutes == mins
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { reminderOffsetMinutes = mins },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) Color(0xFFB45309) else PureWhite,
                                            border = BorderStroke(0.6.dp, if (isSel) Color(0xFFB45309) else Color(0xFFFCD34D))
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else Color(0xFF92400E),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Google Calendar Sync Toggle
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text(
                                        "Đồng bộ vào Google Lịch",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NavyHeroDark
                                    )
                                    Text(
                                        "Tự động tạo sự kiện trên Google Calendar",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = syncToGoogleCalendar,
                                onCheckedChange = { syncToGoogleCalendar = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NavyDeep
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Utilities (Google Lịch & Xóa)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                CadreSystemIntegrations.exportEventToGoogleCalendar(
                                    context = context,
                                    title = title,
                                    description = purpose,
                                    location = location,
                                    dateStr = date,
                                    timeStr = startTime
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Lịch", fontSize = 11.sp, color = NavyDeep, fontWeight = FontWeight.Bold)
                        }

                        if (isEdit && onDelete != null) {
                            TextButton(onClick = {
                                onDelete(event)
                                CadreSystemIntegrations.updateAppWidget(context)
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Xóa lịch trình", color = CoralUrgent, fontSize = 12.sp)
                            }
                        }
                    }

                    // Row 2: Main Primary Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 11.dp)
                        ) {
                            Text("Hủy", color = TextSecondary, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val eventId = event?.id ?: UUID.randomUUID().toString()
                                    onSave(
                                        eventId,
                                        title.trim(),
                                        eventType,
                                        date.trim(),
                                        startTime.trim(),
                                        endTime.trim(),
                                        location.trim(),
                                        destinationUnit.trim(),
                                        contactPerson.trim(),
                                        contactPhone.trim(),
                                        purpose.trim(),
                                        participants.trim()
                                    )

                                    // Automatic sync to Google Calendar if toggle is on
                                    if (syncToGoogleCalendar) {
                                        CadreSystemIntegrations.exportEventToGoogleCalendar(
                                            context = context,
                                            title = title.trim(),
                                            description = buildString {
                                                if (purpose.isNotBlank()) appendLine("Nội dung: ${purpose.trim()}")
                                                if (contactPerson.isNotBlank()) appendLine("Chủ trì / Liên hệ: ${contactPerson.trim()} ($contactPhone)")
                                                if (participants.isNotBlank()) appendLine("Thành phần: ${participants.trim()}")
                                                if (destinationUnit.isNotBlank()) appendLine("Đơn vị: ${destinationUnit.trim()}")
                                            }.trim(),
                                            location = location.trim(),
                                            dateStr = date.trim(),
                                            timeStr = startTime.trim(),
                                            endTimeStr = endTime.trim()
                                        )
                                    }

                                    // Schedule Phone Notification
                                    if (enablePhoneReminder) {
                                        CadreSystemIntegrations.scheduleEventReminder(
                                            context = context,
                                            eventId = eventId,
                                            eventTitle = title.trim(),
                                            dateStr = date.trim(),
                                            timeStr = startTime.trim(),
                                            location = location.trim(),
                                            minutesBefore = reminderOffsetMinutes
                                        )
                                    }

                                    // Update Home Screen Widget
                                    CadreSystemIntegrations.updateAppWidget(context)

                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Vui lòng nhập tên cuộc họp / sự kiện", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.6f)
                                .testTag("save_event_button"),
                            contentPadding = PaddingValues(vertical = 11.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEdit) "Cập nhật lịch trình" else "Lưu lịch trình",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
