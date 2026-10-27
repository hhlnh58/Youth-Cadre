package com.example.ui.screens.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.util.CadreSystemIntegrations
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class AdministrativeDocCategory(
    val code: String,
    val title: String,
    val color: Color,
    val bg: Color
)

val CADRE_DOC_CATEGORIES = listOf(
    AdministrativeDocCategory("KE_HOACH", "Kế hoạch", Color(0xFF1E3A8A), Color(0xFFEFF6FF)),
    AdministrativeDocCategory("CONG_VAN", "Công văn", Color(0xFF0369A1), Color(0xFFF0F9FF)),
    AdministrativeDocCategory("TO_TRINH", "Tờ trình", Color(0xFF6D28D9), Color(0xFFF5F3FF)),
    AdministrativeDocCategory("BAO_CAO", "Báo cáo", Color(0xFF047857), Color(0xFFECFDF5)),
    AdministrativeDocCategory("THONG_BAO", "Thông báo", Color(0xFFB45309), Color(0xFFFFFBEB)),
    AdministrativeDocCategory("PHIEU_CHUYEN", "Phiếu chuyển", Color(0xFFC2410C), Color(0xFFFFF7ED)),
    AdministrativeDocCategory("HUONG_DAN", "Hướng dẫn", Color(0xFF0F766E), Color(0xFFF0FDFA)),
    AdministrativeDocCategory("QUYET_DINH", "Quyết định", Color(0xFFB91C1C), Color(0xFFFEF2F2)),
    AdministrativeDocCategory("KICH_BAN", "Kịch bản", Color(0xFF4338CA), Color(0xFFEEF2FF))
)

fun inferDocCategory(fileName: String): String {
    val lower = fileName.lowercase()
    return when {
        lower.contains("kế hoạch") || lower.contains("ke hoach") || lower.contains("kh-") -> "KE_HOACH"
        lower.contains("thông báo") || lower.contains("thong bao") || lower.contains("tb-") -> "THONG_BAO"
        lower.contains("tờ trình") || lower.contains("to trinh") || lower.contains("ttr-") -> "TO_TRINH"
        lower.contains("báo cáo") || lower.contains("bao cao") || lower.contains("bc-") -> "BAO_CAO"
        lower.contains("phiếu chuyển") || lower.contains("phieu chuyen") || lower.contains("pc-") -> "PHIEU_CHUYEN"
        lower.contains("công văn") || lower.contains("cong van") || lower.contains("cv-") -> "CONG_VAN"
        lower.contains("hướng dẫn") || lower.contains("huong dan") || lower.contains("hd-") -> "HUONG_DAN"
        lower.contains("quyết định") || lower.contains("quyet dinh") || lower.contains("qd-") -> "QUYET_DINH"
        lower.contains("kịch bản") || lower.contains("kich ban") || lower.contains("kb-") -> "KICH_BAN"
        else -> "KE_HOACH"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    task: TaskEntity?,
    portfolios: List<PortfolioEntity>,
    people: List<PersonEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String,
        location: String,
        assignerName: String,
        responsiblePersonName: String,
        collaborators: String,
        priority: String,
        status: String,
        dueDate: String,
        dueTime: String,
        portfolioId: String,
        projectId: String,
        waitingForWho: String,
        driveLink: String,
        outputNotes: String,
        progressPercent: Int,
        attachedFilesJson: String,
        progressNotesJson: String
    ) -> Unit,
    onDelete: ((TaskEntity) -> Unit)? = null
) {
    val isEdit = task != null
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var description by remember(task?.id) { mutableStateOf(task?.description ?: "") }
    var location by remember(task?.id) { mutableStateOf(task?.location ?: "") }
    var assignerName by remember(task?.id) { mutableStateOf(task?.assignerName ?: "") }
    var responsiblePersonName by remember(task?.id) { mutableStateOf(task?.responsiblePersonName ?: "") }
    var collaborators by remember(task?.id) { mutableStateOf(task?.collaborators ?: "") }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: "P2") }
    var status by remember(task?.id) { mutableStateOf(task?.status ?: "TODO") }
    var dueDate by remember(task?.id) { mutableStateOf(task?.dueDate?.ifEmpty { todayStr } ?: todayStr) }
    var dueTime by remember(task?.id) { mutableStateOf(task?.dueTime?.ifEmpty { "17:00" } ?: "17:00") }
    var portfolioId by remember(task?.id) { mutableStateOf(task?.portfolioId ?: portfolios.firstOrNull()?.id ?: "") }
    var waitingForWho by remember(task?.id) { mutableStateOf(task?.waitingForWho ?: "") }
    var driveLink by remember(task?.id) { mutableStateOf(task?.driveLink ?: "") }
    var outputNotes by remember(task?.id) { mutableStateOf(task?.outputNotes ?: "") }
    var progressPercent by remember(task?.id) { mutableStateOf(task?.progressPercent ?: if (status == "COMPLETED") 100 else 0) }

    // Attached files parsing
    val initialAttachedList = remember(task?.id, task?.attachedFilesJson) {
        val list = mutableListOf<AttachedDocItem>()
        try {
            val jsonArray = JSONArray(task?.attachedFilesJson ?: "[]")
            for (i in 0 until jsonArray.length()) {
                val element = jsonArray.opt(i)
                if (element is JSONObject) {
                    val fName = element.optString("name", "Tài liệu")
                    list.add(
                        AttachedDocItem(
                            id = element.optString("id", UUID.randomUUID().toString()),
                            name = fName,
                            uri = element.optString("uri", ""),
                            type = element.optString("type", "PDF"),
                            category = element.optString("category", inferDocCategory(fName)),
                            size = element.optString("size", "1.0 MB")
                        )
                    )
                } else if (element is String && element.isNotBlank()) {
                    try {
                        val obj = JSONObject(element)
                        val fName = obj.optString("name", "Tài liệu")
                        list.add(
                            AttachedDocItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                name = fName,
                                uri = obj.optString("uri", ""),
                                type = obj.optString("type", "PDF"),
                                category = obj.optString("category", inferDocCategory(fName)),
                                size = obj.optString("size", "1.0 MB")
                            )
                        )
                    } catch (_: Exception) {
                        list.add(
                            AttachedDocItem(
                                id = UUID.randomUUID().toString(),
                                name = element,
                                uri = "",
                                type = if (element.endsWith(".pdf", true)) "PDF" else "DOCX",
                                category = inferDocCategory(element),
                                size = "1.0 MB"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }
    val attachedFiles = remember(task?.id) {
        mutableStateListOf<AttachedDocItem>().apply { addAll(initialAttachedList) }
    }

    // Progress notes parsing
    val initialNotesList = remember(task?.id, task?.progressNotesJson) {
        val list = mutableListOf<ProgressNoteItem>()
        try {
            val jsonArray = JSONArray(task?.progressNotesJson ?: "[]")
            for (i in 0 until jsonArray.length()) {
                val element = jsonArray.opt(i)
                if (element is JSONObject) {
                    list.add(
                        ProgressNoteItem(
                            id = element.optString("id", UUID.randomUUID().toString()),
                            date = element.optString("date", todayStr),
                            content = element.optString("content", "")
                        )
                    )
                } else if (element is String && element.isNotBlank()) {
                    try {
                        val obj = JSONObject(element)
                        list.add(
                            ProgressNoteItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                date = obj.optString("date", todayStr),
                                content = obj.optString("content", "")
                            )
                        )
                    } catch (_: Exception) {
                        list.add(
                            ProgressNoteItem(
                                id = UUID.randomUUID().toString(),
                                date = todayStr,
                                content = element
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }
    val progressNotes = remember(task?.id) {
        mutableStateListOf<ProgressNoteItem>().apply { addAll(initialNotesList) }
    }
    var newNoteContent by remember { mutableStateOf("") }
    var enableTaskReminder by remember(task?.id) { mutableStateOf(true) }
    var taskReminderOffsetMinutes by remember(task?.id) { mutableIntStateOf(15) }

    var showAssignerPicker by remember { mutableStateOf(false) }
    var showResponsiblePicker by remember { mutableStateOf(false) }

    // Native Date & Time Picker Helpers
    val showDatePicker = {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(dueDate)?.let { cal.time = it }
        } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                dueDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(newCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showTimePicker = {
        val h = try { dueTime.substringBefore(":").trim().toInt() } catch (_: Exception) { 17 }
        val m = try { dueTime.substringAfter(":").trim().toInt() } catch (_: Exception) { 0 }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                dueTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            },
            h,
            m,
            true
        ).show()
    }

    // Contact Picker Launcher from System Contacts (uses Phone content URI for 100% reliable read)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            val contact = CadreSystemIntegrations.resolveContact(context, uri)
            if (contact != null) {
                responsiblePersonName = contact.name
                Toast.makeText(context, "Đã chọn từ Danh bạ: ${contact.name} (${contact.phone})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Universal document file picker via SAF (*/*)
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            var fileName = "Tai_lieu_dinh_kem"
            var fileSizeStr = "1.0 MB"
            try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            fileSizeStr = if (sizeBytes > 1024 * 1024) {
                                String.format(Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
                            } else {
                                String.format(Locale.US, "%.0f KB", sizeBytes.toDouble() / 1024)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            val lower = fileName.lowercase()
            val docType = when {
                lower.endsWith(".doc") || lower.endsWith(".docx") -> "DOCX"
                lower.endsWith(".pdf") -> "PDF"
                lower.endsWith(".xls") || lower.endsWith(".xlsx") -> "XLSX"
                lower.endsWith(".ppt") || lower.endsWith(".pptx") -> "PPTX"
                lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg") -> "IMG"
                else -> "FILE"
            }

            attachedFiles.add(
                AttachedDocItem(
                    id = UUID.randomUUID().toString(),
                    name = fileName,
                    uri = it.toString(),
                    type = docType,
                    category = inferDocCategory(fileName),
                    size = fileSizeStr
                )
            )
            Toast.makeText(context, "✅ Đã đính kèm tệp: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo picker via Android Photo Picker
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val timeStamp = SimpleDateFormat("ddMM_HHmm", Locale.getDefault()).format(Date())
            val photoName = "Hinh_anh_$timeStamp.jpg"
            attachedFiles.add(
                AttachedDocItem(
                    id = UUID.randomUUID().toString(),
                    name = photoName,
                    uri = it.toString(),
                    type = "IMG",
                    category = "CONG_VAN",
                    size = "1.2 MB"
                )
            )
            Toast.makeText(context, "✅ Đã đính kèm ảnh tài liệu", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("task_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
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
                                .background(Brush.linearGradient(listOf(NavyDeep, NavyPrimary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                if (isEdit) "Chi tiết & Chỉnh sửa Nhiệm vụ" else "Tạo Nhiệm vụ Mới",
                                fontWeight = FontWeight.Bold,
                                color = NavyHeroDark,
                                fontSize = 17.sp
                            )
                            Text(
                                "Điều chỉnh nội dung, thời gian, địa điểm & tài liệu đính kèm",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Task Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tên công việc / Nhiệm vụ chỉ đạo *", color = NavyDeep, fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Nhập tiêu đề hoặc tên đầu việc...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDeep,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = Color(0xFFFAFCFF)
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    // 1. THỜI GIAN (TIME & DEADLINE) WITH INTERACTIVE PICKERS
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(16.dp))
                                    Text("Thời hạn hoàn thành:", color = NavyHeroDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Chạm biểu tượng để chọn lịch", fontSize = 10.sp, color = TextMuted)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = dueDate,
                                    onValueChange = { dueDate = it },
                                    label = { Text("Hạn ngày (dd/MM/yyyy)") },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(10.dp),
                                    trailingIcon = {
                                        IconButton(onClick = showDatePicker) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày", tint = NavyDeep, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = dueTime,
                                    onValueChange = { dueTime = it },
                                    label = { Text("Giờ (HH:mm)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    trailingIcon = {
                                        IconButton(onClick = showTimePicker) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Chọn giờ", tint = NavyDeep, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    singleLine = true
                                )
                            }

                            // Quick Date Shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val cal = Calendar.getInstance()
                                val today = sdf.format(cal.time)
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                val tomorrow = sdf.format(cal.time)
                                cal.add(Calendar.DAY_OF_YEAR, 2)
                                val in3Days = sdf.format(cal.time)
                                cal.add(Calendar.DAY_OF_YEAR, 4)
                                val in1Week = sdf.format(cal.time)

                                listOf(
                                    Pair("Hôm nay", today),
                                    Pair("Ngày mai", tomorrow),
                                    Pair("+3 ngày", in3Days),
                                    Pair("+1 tuần", in1Week)
                                ).forEach { (lbl, dateVal) ->
                                    val isSelected = dueDate == dateVal
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { dueDate = dateVal },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) NavyDeep else PureWhite,
                                        border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight)
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                lbl,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Phone Notification Reminder Settings
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(0.8.dp, Color(0xFFFDE68A))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                            Column {
                                                Text("Bật thông báo trên điện thoại", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF92400E))
                                                Text("Chuông báo & rung trước giờ hạn", fontSize = 10.sp, color = Color(0xFFB45309))
                                            }
                                        }
                                        Switch(
                                            checked = enableTaskReminder,
                                            onCheckedChange = { enableTaskReminder = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFB45309))
                                        )
                                    }

                                    if (enableTaskReminder) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "15 phút trước" to 15,
                                                "30 phút trước" to 30,
                                                "1 giờ trước" to 60,
                                                "1 ngày trước" to 1440
                                            ).forEach { (label, mins) ->
                                                val isSel = taskReminderOffsetMinutes == mins
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { taskReminderOffsetMinutes = mins },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSel) Color(0xFFB45309) else PureWhite,
                                                    border = BorderStroke(0.6.dp, if (isSel) Color(0xFFB45309) else Color(0xFFFCD34D))
                                                ) {
                                                    Text(
                                                        label,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSel) Color.White else Color(0xFF92400E),
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. ĐỊA ĐIỂM (LOCATION)
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Địa điểm / Địa bàn / Đơn vị thực hiện", color = NavyDeep, fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Ví dụ: Hội trường Thành Đoàn, UBND Xã, Cơ sở Đoàn...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // 3. NỘI DUNG & CHỈ ĐẠO (CONTENT & DESCRIPTION)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả nội dung & Chỉ đạo thực hiện", color = NavyDeep, fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Nhập chi tiết nội dung chỉ đạo, lưu ý thực hiện, mốc kiểm tra...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )

                    // Priority Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mức độ ưu tiên:", color = NavyHeroDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("P1", "Khẩn cấp", CoralUrgent),
                                Triple("P2", "Trọng tâm", ChampagneGold),
                                Triple("P3", "Thường kỳ", NavyPrimary),
                                Triple("P4", "Theo dõi", Color(0xFF64748B))
                            ).forEach { (pCode, pLabel, pColor) ->
                                val selected = priority == pCode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { priority = pCode },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) pColor.copy(alpha = 0.14f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (selected) pColor else BorderLight)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(pCode, fontWeight = FontWeight.Black, color = if (selected) pColor else TextSecondary, fontSize = 13.sp)
                                        Text(pLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) pColor else TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Status Step Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Trạng thái xử lý:", color = NavyHeroDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val statusList = listOf(
                            Pair("TODO", "Chưa làm"),
                            Pair("IN_PROGRESS", "Đang xử lý"),
                            Pair("WAITING_FOR_OTHERS", "Chờ duyệt"),
                            Pair("COMPLETED", "Đã xong")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            statusList.forEach { (sCode, sLabel) ->
                                val selected = status == sCode
                                val activeColor = when (sCode) {
                                    "COMPLETED" -> EmeraldSuccess
                                    "IN_PROGRESS" -> NavyPrimary
                                    "WAITING_FOR_OTHERS" -> ChampagneGold
                                    else -> NavyDeep
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            status = sCode
                                            if (sCode == "COMPLETED") progressPercent = 100
                                            else if (progressPercent == 100) progressPercent = 50
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) activeColor.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (selected) activeColor else BorderLight)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            sLabel,
                                            fontSize = 11.sp,
                                            color = if (selected) activeColor else TextSecondary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Progress Slider
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (progressPercent == 100) EmeraldSuccess else NavyDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Tiến độ: $progressPercent%", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (progressPercent == 100) EmeraldSuccess.copy(alpha = 0.15f) else NavyPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        if (progressPercent == 100) "Hoàn tất" else "$progressPercent%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (progressPercent == 100) EmeraldSuccess else NavyDeep,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Slider(
                                value = progressPercent.toFloat(),
                                onValueChange = { newVal ->
                                    progressPercent = newVal.toInt()
                                    if (progressPercent == 100) status = "COMPLETED"
                                    else if (status == "COMPLETED" && progressPercent < 100) status = "IN_PROGRESS"
                                },
                                valueRange = 0f..100f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (progressPercent == 100) EmeraldSuccess else NavyDeep,
                                    activeTrackColor = if (progressPercent == 100) EmeraldSuccess else NavyPrimary
                                )
                            )
                        }
                    }

                    // Personnel Assignment
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Phân công & Phối hợp:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyHeroDark)

                            // Assigner
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Người giao việc:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    if (people.isNotEmpty()) {
                                        Text(
                                            if (showAssignerPicker) "Thu gọn ▲" else "Chọn từ danh bạ ▼",
                                            fontSize = 10.5.sp,
                                            color = NavyDeep,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { showAssignerPicker = !showAssignerPicker }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = assignerName,
                                    onValueChange = { assignerName = it },
                                    placeholder = { Text("Ví dụ: Đ/c Bí thư, Ban Thường vụ...", fontSize = 12.sp, color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                if (showAssignerPicker && people.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        people.forEach { p ->
                                            val label = "${p.fullName} (${p.position})"
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        assignerName = label
                                                        showAssignerPicker = false
                                                    },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (assignerName == label) Color(0xFFEFF6FF) else Color.Transparent
                                            ) {
                                                Text(label, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Responsible Person
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Người phụ trách chính:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(0.6.dp, Color(0xFFA7F3D0)),
                                            modifier = Modifier.clickable {
                                                val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                                contactPickerLauncher.launch(intent)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Default.Contacts, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                                Text("Danh bạ máy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                            }
                                        }
                                        if (people.isNotEmpty()) {
                                            Text(
                                                if (showResponsiblePicker) "Thu gọn ▲" else "Nội bộ ▼",
                                                fontSize = 10.sp,
                                                color = NavyDeep,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { showResponsiblePicker = !showResponsiblePicker }
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = responsiblePersonName,
                                    onValueChange = { responsiblePersonName = it },
                                    placeholder = { Text("Ví dụ: Cán bộ chuyên trách...", fontSize = 12.sp, color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                if (showResponsiblePicker && people.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        people.forEach { p ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        responsiblePersonName = p.fullName
                                                        showResponsiblePicker = false
                                                    },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (responsiblePersonName == p.fullName) Color(0xFFEFF6FF) else Color.Transparent
                                            ) {
                                                Text("${p.fullName} • ${p.position}", fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Collaborators
                            OutlinedTextField(
                                value = collaborators,
                                onValueChange = { collaborators = it },
                                label = { Text("Đơn vị / Cán bộ phối hợp") },
                                placeholder = { Text("Ví dụ: Ban Tuyên giáo, Chi đoàn 1...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }

                    // 4. TÀI LIỆU ĐÍNH KÈM (ATTACHED DOCUMENTS)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
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
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(16.dp))
                                    Text("Tài liệu đính kèm:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (attachedFiles.isNotEmpty()) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        "${attachedFiles.size} tệp",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (attachedFiles.isNotEmpty()) NavyDeep else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Action buttons: Pick Document (*/*) and Pick Image
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // Universal mime type to support all files (Word, PDF, Excel, txt, etc.)
                                        documentPicker.launch(arrayOf("*/*"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tải lên tệp văn bản", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NavyMedium.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp), tint = NavyDeep)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ảnh tài liệu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                                }
                            }

                            // Display attached files list
                            if (attachedFiles.isEmpty()) {
                                Text(
                                    "Chưa có tệp nào. Bấm 'Tải lên tệp văn bản' để chọn PDF, Word, Excel từ máy.",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    attachedFiles.forEachIndexed { index, doc ->
                                        AttachedFileItemRow(
                                            doc = doc,
                                            onCategoryChange = { newCat ->
                                                attachedFiles[index] = doc.copy(category = newCat)
                                            },
                                            onDelete = {
                                                attachedFiles.removeAt(index)
                                                Toast.makeText(context, "Đã gỡ tệp đính kèm", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Progress Activity Log
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Icon(Icons.Default.Comment, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
                                    Text("Nhật ký & Ghi chú tiến độ:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                }
                                Text("${progressNotes.size} ghi chú", fontSize = 10.sp, color = TextSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newNoteContent,
                                    onValueChange = { newNoteContent = it },
                                    placeholder = { Text("Ghi chú mốc tiến độ...", fontSize = 11.sp, color = TextMuted) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (newNoteContent.isNotBlank()) {
                                            val noteDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                                            progressNotes.add(
                                                0,
                                                ProgressNoteItem(
                                                    date = noteDateStr,
                                                    content = newNoteContent.trim()
                                                )
                                            )
                                            newNoteContent = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (progressNotes.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    progressNotes.forEach { item ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = PureWhite,
                                            border = BorderStroke(1.dp, BorderLight)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(item.date, fontSize = 9.5.sp, color = ChampagneGold, fontWeight = FontWeight.Bold)
                                                Text(item.content, fontSize = 11.5.sp, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Google Drive Link
                    OutlinedTextField(
                        value = driveLink,
                        onValueChange = { driveLink = it },
                        label = { Text("Đường dẫn Google Drive / Tài liệu liên kết") },
                        placeholder = { Text("https://drive.google.com/...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEdit && onDelete != null) {
                        TextButton(onClick = {
                            onDelete(task)
                            CadreSystemIntegrations.updateAppWidget(context)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa", color = CoralUrgent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderLight)
                        ) {
                            Text("Hủy", color = TextSecondary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val filesArr = JSONArray()
                                    attachedFiles.forEach { f ->
                                        filesArr.put(
                                            JSONObject().apply {
                                                put("id", f.id)
                                                put("name", f.name)
                                                put("uri", f.uri)
                                                put("type", f.type)
                                                put("category", f.category)
                                                put("size", f.size)
                                            }
                                        )
                                    }

                                    val notesArr = JSONArray()
                                    progressNotes.forEach { n ->
                                        notesArr.put(
                                            JSONObject().apply {
                                                put("id", n.id)
                                                put("date", n.date)
                                                put("content", n.content)
                                            }
                                        )
                                    }

                                    val finalTaskId = task?.id ?: UUID.randomUUID().toString()

                                    onSave(
                                        finalTaskId,
                                        title.trim(),
                                        description.trim(),
                                        location.trim(),
                                        assignerName.trim(),
                                        responsiblePersonName.trim(),
                                        collaborators.trim(),
                                        priority,
                                        status,
                                        dueDate.trim(),
                                        dueTime.trim(),
                                        portfolioId,
                                        task?.projectId ?: "",
                                        waitingForWho.trim(),
                                        driveLink.trim(),
                                        outputNotes.trim(),
                                        progressPercent,
                                        filesArr.toString(),
                                        notesArr.toString()
                                    )

                                    if (enableTaskReminder && dueDate.isNotBlank()) {
                                        CadreSystemIntegrations.scheduleTaskReminder(
                                            context = context,
                                            taskId = finalTaskId,
                                            taskTitle = title.trim(),
                                            dueDateStr = dueDate.trim(),
                                            dueTimeStr = dueTime.trim(),
                                            minutesBefore = taskReminderOffsetMinutes
                                        )
                                    }

                                    CadreSystemIntegrations.updateAppWidget(context)
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Vui lòng nhập tên nhiệm vụ", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_task_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEdit) "Cập nhật nhiệm vụ" else "Lưu nhiệm vụ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachedFileItemRow(
    doc: AttachedDocItem,
    onCategoryChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expandedCategoryMenu by remember { mutableStateOf(false) }
    val currentCat = CADRE_DOC_CATEGORIES.find { it.code == doc.category }
        ?: AdministrativeDocCategory("KE_HOACH", "Kế hoạch", Color(0xFF1E3A8A), Color(0xFFEFF6FF))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isPdf = doc.type.uppercase() == "PDF"
                val isImg = doc.type.uppercase() == "IMG" || doc.name.lowercase().endsWith(".jpg") || doc.name.lowercase().endsWith(".png")
                Icon(
                    when {
                        isImg -> Icons.Default.Image
                        isPdf -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = when {
                        isImg -> Color(0xFF0D9488)
                        isPdf -> CoralUrgent
                        else -> NavyMedium
                    },
                    modifier = Modifier.size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        doc.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${doc.type} • ${doc.size.ifBlank { "Tệp đính kèm" }}", fontSize = 10.sp, color = TextSecondary)
                }

                if (doc.uri.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(doc.uri), if (isPdf) "application/pdf" else "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Không thể mở tệp trực tiếp", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Mở tệp", tint = NavyDeep, modifier = Modifier.size(15.dp))
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = CoralUrgent, modifier = Modifier.size(15.dp))
                }
            }

            // Category selector tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { expandedCategoryMenu = true },
                        shape = RoundedCornerShape(6.dp),
                        color = currentCat.bg,
                        border = BorderStroke(0.6.dp, currentCat.color.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = currentCat.color, modifier = Modifier.size(10.dp))
                            Text(
                                currentCat.title,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentCat.color
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = currentCat.color, modifier = Modifier.size(11.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        CADRE_DOC_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(cat.color)
                                        )
                                        Text(
                                            cat.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (cat.code == doc.category) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    onCategoryChange(cat.code)
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Text(
                    "Đã lưu trữ an toàn",
                    fontSize = 9.sp,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
