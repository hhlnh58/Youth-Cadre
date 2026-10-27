package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ai.ParsedCadreEntity
import com.example.data.ai.SmartCadreParser
import com.example.data.model.PersonEntity
import com.example.data.model.TaskEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun QuickCaptureDialog(
    people: List<PersonEntity> = emptyList(),
    tasks: List<TaskEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSubmitEntity: (ParsedCadreEntity) -> Unit,
    onCreateSchedule: () -> Unit = {},
    onAppendTaskNote: (TaskEntity, String) -> Unit = { _, _ -> },
    onCreateIndependentNote: (String, String) -> Unit = { _, _ -> },
    onSaveSettlement: (title: String, amount: Long, budgetEstimateCode: String, deadline: String, taskId: String, taskTitle: String, responsiblePerson: String) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var inputText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("TASK") } // TASK, EVENT, NOTE, SETTLEMENT

    // Notes Mode specific state
    var noteMode by remember { mutableStateOf("TASK_NOTE") } // "TASK_NOTE" or "INDEPENDENT"
    var selectedTaskForNote by remember(tasks) { mutableStateOf<TaskEntity?>(tasks.firstOrNull()) }
    var noteText by remember { mutableStateOf("") }
    var independentNoteTitle by remember { mutableStateOf("") }

    // Settlement Mode specific state
    var settlementTitle by remember { mutableStateOf("") }
    var settlementAmountText by remember { mutableStateOf("") }
    var settlementBudgetCode by remember { mutableStateOf("") }
    var settlementDeadline by remember { mutableStateOf("") }
    var settlementSelectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var settlementResponsible by remember { mutableStateOf("") }

    // Auto-parse on input change with personnel cross-referencing
    val parsed = remember(inputText, people) {
        SmartCadreParser.parse(inputText, people)
    }

    // Editable overrides
    var editableTitle by remember { mutableStateOf("") }
    var editableAssigner by remember { mutableStateOf("") }
    var editableResponsiblePerson by remember { mutableStateOf("") }
    var editableDate by remember { mutableStateOf("") }
    var editableTime by remember { mutableStateOf("") }
    var editableLocation by remember { mutableStateOf("") }
    var editableCollaborators by remember { mutableStateOf("") }
    var editablePriority by remember { mutableStateOf("P2") }

    // Real-time suggested people from directory based on current input text
    val suggestedPeople = remember(inputText, people) {
        SmartCadreParser.findSuggestedPeople(inputText, people)
    }

    // Sync state when parser updates
    LaunchedEffect(parsed) {
        if (inputText.isNotBlank()) {
            editableTitle = parsed.title
            editableAssigner = parsed.assigner
            editableResponsiblePerson = parsed.responsiblePerson
            editableDate = parsed.dateString
            editableTime = parsed.timeString
            editableLocation = parsed.location
            editableCollaborators = parsed.collaborators
            editablePriority = parsed.priority
            if (parsed.inferredType == "EVENT" && selectedType == "TASK") {
                selectedType = "EVENT"
            }
        }
    }

    val syntaxTips = listOf(
        "Giao việc: [Người giao] giao [Người nhận] làm [Nội dung] trước [Giờ/Ngày], tại [Địa điểm] phối hợp [Người]",
        "Họp / Lịch: Họp Ban Thường vụ lúc 14h thứ Sáu tại Phòng họp 1",
        "Chỉ đạo: Bí thư An giao đ/c Nam chuẩn bị báo cáo trước 17h ngày mai"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("smart_quick_capture_dialog"),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DeepNavyButtonGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                "Thao tác nhanh",
                                fontWeight = FontWeight.Black,
                                color = NavyHeroDark,
                                fontSize = 17.sp
                            )
                            Text(
                                "Nhận diện & Gợi ý tự động theo Danh bạ cán bộ",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                // Type Toggle: Nhiệm vụ vs Lịch trình vs Ghi chú
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickTypeChip(
                        label = "Nhiệm vụ",
                        icon = Icons.Default.Checklist,
                        isSelected = selectedType == "TASK",
                        onClick = { selectedType = "TASK" },
                        modifier = Modifier.weight(1f)
                    )
                    QuickTypeChip(
                        label = "Lịch trình",
                        icon = Icons.Default.CalendarToday,
                        isSelected = selectedType == "EVENT",
                        onClick = {
                            onDismiss()
                            onCreateSchedule()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickTypeChip(
                        label = "Ghi chú",
                        icon = Icons.Default.NoteAlt,
                        isSelected = selectedType == "NOTE",
                        onClick = { selectedType = "NOTE" },
                        modifier = Modifier.weight(1f)
                    )
                    QuickTypeChip(
                        label = "Quyết toán",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = selectedType == "SETTLEMENT",
                        onClick = { selectedType = "SETTLEMENT" },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedType == "NOTE") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sub-mode Switcher: Theo nhiệm vụ vs Riêng lẻ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { noteMode = "TASK_NOTE" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (noteMode == "TASK_NOTE") NavyDeep else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (noteMode == "TASK_NOTE") NavyDeep else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = if (noteMode == "TASK_NOTE") Color.White else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Theo nhiệm vụ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (noteMode == "TASK_NOTE") Color.White else TextSecondary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { noteMode = "INDEPENDENT" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (noteMode == "INDEPENDENT") NavyDeep else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (noteMode == "INDEPENDENT") NavyDeep else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.NoteAdd,
                                        contentDescription = null,
                                        tint = if (noteMode == "INDEPENDENT") Color.White else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Ghi chú riêng lẻ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (noteMode == "INDEPENDENT") Color.White else TextSecondary
                                    )
                                }
                            }
                        }

                        if (noteMode == "TASK_NOTE") {
                            if (tasks.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Chưa có nhiệm vụ công tác trong hệ thống để gắn ghi chú. Vui lòng tạo nhiệm vụ trước hoặc chọn 'Ghi chú riêng lẻ'.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                var isExpanded by remember { mutableStateOf(false) }
                                var taskSearchQuery by remember { mutableStateOf("") }

                                val filteredTasksForNote = remember(tasks, taskSearchQuery) {
                                    if (taskSearchQuery.isBlank()) tasks
                                    else tasks.filter {
                                        it.title.contains(taskSearchQuery, ignoreCase = true) ||
                                        it.responsiblePersonName.contains(taskSearchQuery, ignoreCase = true) ||
                                        it.assignerName.contains(taskSearchQuery, ignoreCase = true)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Chọn nhiệm vụ cần ghi chú thêm:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, BorderLight),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isExpanded = !isExpanded }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    selectedTaskForNote?.title ?: "Chạm để chọn nhiệm vụ...",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (selectedTaskForNote != null) {
                                                    Text(
                                                        "Phụ trách: ${selectedTaskForNote?.responsiblePersonName?.ifBlank { "Chưa gán" }} | Hạn: ${selectedTaskForNote?.dueDate}",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                            Icon(
                                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = TextSecondary
                                            )
                                        }
                                    }

                                    if (isExpanded) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = PureWhite,
                                            border = BorderStroke(1.dp, BorderLight),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                        ) {
                                            Column {
                                                // Task Search input inside dropdown
                                                OutlinedTextField(
                                                    value = taskSearchQuery,
                                                    onValueChange = { taskSearchQuery = it },
                                                    placeholder = { Text("Tìm theo tên nhiệm vụ, cán bộ...", fontSize = 11.5.sp, color = TextMuted) },
                                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(16.dp)) },
                                                    trailingIcon = {
                                                        if (taskSearchQuery.isNotBlank()) {
                                                            IconButton(onClick = { taskSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                                Icon(Icons.Default.Close, contentDescription = "Xóa", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = NavyDeep,
                                                        unfocusedBorderColor = BorderLight,
                                                        focusedContainerColor = Color(0xFFF8FAFC),
                                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )

                                                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                    if (filteredTasksForNote.isEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(16.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                "Không tìm thấy nhiệm vụ nào khớp",
                                                                fontSize = 12.sp,
                                                                color = TextSecondary
                                                            )
                                                        }
                                                    } else {
                                                        filteredTasksForNote.forEach { t ->
                                                            val isCur = t.id == selectedTaskForNote?.id
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        selectedTaskForNote = t
                                                                        isExpanded = false
                                                                    }
                                                                    .background(if (isCur) Color(0xFFEFF6FF) else PureWhite)
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        t.title,
                                                                        fontSize = 12.sp,
                                                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium,
                                                                        color = TextPrimary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        "${t.responsiblePersonName.ifBlank { "Cán bộ" }} • Hạn ${t.dueDate}",
                                                                        fontSize = 10.5.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                                if (isCur) {
                                                                    Icon(Icons.Default.Check, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                            HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    label = { Text("Ý kiến chỉ đạo / Ghi chú bổ sung", fontSize = 12.sp) },
                                    placeholder = { Text("Nhập nội dung ghi chú cho nhiệm vụ này...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 90.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NavyDeep,
                                        unfocusedBorderColor = BorderLight,
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC)
                                    ),
                                    maxLines = 4
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, BorderLight),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Hủy", color = TextSecondary)
                                    }

                                    Button(
                                        onClick = {
                                            val task = selectedTaskForNote ?: tasks.firstOrNull()
                                            if (task != null && noteText.isNotBlank()) {
                                                onAppendTaskNote(task, noteText)
                                                onDismiss()
                                            }
                                        },
                                        enabled = selectedTaskForNote != null && noteText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.5f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Lưu vào nhiệm vụ", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // Independent note
                            OutlinedTextField(
                                value = independentNoteTitle,
                                onValueChange = { independentNoteTitle = it },
                                label = { Text("Tiêu đề ghi chú", fontSize = 12.sp) },
                                placeholder = { Text("Ví dụ: Ghi chú cuộc họp, ý tưởng...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyDeep,
                                    unfocusedBorderColor = BorderLight,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                label = { Text("Nội dung ghi chú", fontSize = 12.sp) },
                                placeholder = { Text("Nội dung chi tiết...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyDeep,
                                    unfocusedBorderColor = BorderLight,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                maxLines = 5
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hủy", color = TextSecondary)
                                }

                                Button(
                                    onClick = {
                                        if (noteText.isNotBlank()) {
                                            val t = independentNoteTitle.ifBlank { noteText.take(40).trim() }
                                            onCreateIndependentNote(t, noteText)
                                            onDismiss()
                                        }
                                    },
                                    enabled = noteText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lưu sổ tay", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else if (selectedType == "SETTLEMENT") {
                    // Form nhập quyết toán kinh phí
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Nhập thông tin quyết toán kinh phí", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                    Text("Quản lý hạn và chứng từ thanh quyết toán theo dự trù", fontSize = 10.5.sp, color = Color(0xFFB45309))
                                }
                            }
                        }

                        // 1. Nội dung quyết toán
                        OutlinedTextField(
                            value = settlementTitle,
                            onValueChange = { settlementTitle = it },
                            label = { Text("Nội dung quyết toán *", fontSize = 12.sp) },
                            placeholder = { Text("Ví dụ: Mua văn phòng phẩm Đại hội, Thuê âm thanh...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = false,
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyDeep,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        // 2. Số tiền (VNĐ)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = settlementAmountText,
                                onValueChange = { input ->
                                    val digitsOnly = input.filter { it.isDigit() }
                                    settlementAmountText = digitsOnly
                                },
                                label = { Text("Số tiền quyết toán (VNĐ) *", fontSize = 12.sp) },
                                placeholder = { Text("Ví dụ: 3500000", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                trailingIcon = {
                                    Text("đ", fontWeight = FontWeight.Bold, color = NavyDeep, modifier = Modifier.padding(end = 12.dp))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyDeep,
                                    unfocusedBorderColor = BorderLight,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            // Quick Amount pills
                            val parsedAmt = settlementAmountText.toLongOrNull() ?: 0L
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(500_000L to "+500k", 1_000_000L to "+1tr", 2_000_000L to "+2tr", 5_000_000L to "+5tr", 10_000_000L to "+10tr").forEach { (addVal, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(0.7.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable {
                                            settlementAmountText = (parsedAmt + addVal).toString()
                                        }
                                    ) {
                                        Text(label, fontSize = 11.sp, color = NavyDeep, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                if (parsedAmt > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEE2E2),
                                        modifier = Modifier.clickable { settlementAmountText = "" }
                                    ) {
                                        Text("Xóa", fontSize = 11.sp, color = CoralUrgent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        // 3. Dự trù kinh phí số mấy & Hạn quyết toán
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = settlementBudgetCode,
                                onValueChange = { settlementBudgetCode = it },
                                label = { Text("Dự trù số *", fontSize = 11.sp) },
                                placeholder = { Text("VD: DT-04/ĐTN", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyDeep,
                                    unfocusedBorderColor = BorderLight,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            OutlinedTextField(
                                value = settlementDeadline,
                                onValueChange = { settlementDeadline = it },
                                label = { Text("Hạn quyết toán *", fontSize = 11.sp) },
                                placeholder = { Text("dd/MM/yyyy", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(16.dp))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyDeep,
                                    unfocusedBorderColor = BorderLight,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )
                        }

                        // Quick date shortcut buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                            listOf(
                                "+3 ngày" to 3,
                                "+7 ngày (1 tuần)" to 7,
                                "+14 ngày (2 tuần)" to 14,
                                "+30 ngày (1 tháng)" to 30
                            ).forEach { (label, days) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.DAY_OF_YEAR, days)
                                        settlementDeadline = sdf.format(cal.time)
                                    }
                                ) {
                                    Text(label, fontSize = 10.5.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // 4. Cán bộ tạm ứng / Chịu trách nhiệm quyết toán
                        OutlinedTextField(
                            value = settlementResponsible,
                            onValueChange = { settlementResponsible = it },
                            label = { Text("Cán bộ tạm ứng / Chịu trách nhiệm", fontSize = 12.sp) },
                            placeholder = { Text("Tên cán bộ thực hiện...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(18.dp))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyDeep,
                                unfocusedBorderColor = BorderLight,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        // 5. Liên kết nhiệm vụ (tùy chọn)
                        if (tasks.isNotEmpty()) {
                            var isTaskExpanded by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Gắn với nhiệm vụ trọng tâm (không bắt buộc):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { isTaskExpanded = !isTaskExpanded }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            settlementSelectedTask?.title ?: "— Không gắn nhiệm vụ cụ thể —",
                                            fontSize = 11.5.sp,
                                            color = if (settlementSelectedTask != null) TextPrimary else TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(if (isTaskExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (isTaskExpanded) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PureWhite,
                                        border = BorderStroke(1.dp, BorderLight),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                                    ) {
                                        LazyColumn {
                                            item {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            settlementSelectedTask = null
                                                            isTaskExpanded = false
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("— Không gắn nhiệm vụ —", fontSize = 11.sp, color = TextMuted)
                                                }
                                                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                                            }
                                            items(tasks) { t ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            settlementSelectedTask = t
                                                            if (settlementResponsible.isBlank() && t.responsiblePersonName.isNotBlank()) {
                                                                settlementResponsible = t.responsiblePersonName
                                                            }
                                                            isTaskExpanded = false
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                                ) {
                                                    Text(t.title, fontSize = 11.5.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Submit buttons for settlement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hủy", color = TextSecondary)
                            }

                            val canSave = settlementTitle.isNotBlank() && settlementAmountText.isNotBlank()
                            Button(
                                onClick = {
                                    if (canSave) {
                                        val amt = settlementAmountText.toLongOrNull() ?: 0L
                                        val finalDeadline = settlementDeadline.ifBlank {
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.DAY_OF_YEAR, 14)
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                                        }
                                        onSaveSettlement(
                                            settlementTitle.trim(),
                                            amt,
                                            settlementBudgetCode.trim(),
                                            finalDeadline,
                                            settlementSelectedTask?.id ?: "",
                                            settlementSelectedTask?.title ?: "",
                                            settlementResponsible.trim()
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = canSave,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lưu quyết toán", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    // Natural language input box
                    OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Nhập chỉ đạo hoặc công việc...\nVí dụ: Nguyễn Văn B giao cho Lan chuẩn bị báo cáo Đại hội trước 16h thứ Sáu tại Phòng họp 1, phối hợp Ban Phong trào...",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .testTag("quick_capture_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDeep,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 4
                )

                // 💡 REAL-TIME CONTACT SUGGESTION STRIP (Hệ thống nhận diện tên và hiển thị suggest)
                AnimatedVisibility(visible = suggestedPeople.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ContactPhone, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(14.dp))
                                    Text(
                                        "Khớp danh bạ cán bộ (${suggestedPeople.size}):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDeep
                                    )
                                }
                                Text("Chạm gán nhanh vai trò", fontSize = 9.5.sp, color = NavyMedium)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                suggestedPeople.forEach { p ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PureWhite,
                                        border = BorderStroke(0.6.dp, Color(0xFFDBEAFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(NavyDeep),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        p.fullName.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Column {
                                                    Text(p.fullName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    if (p.position.isNotBlank()) {
                                                        Text(p.position, fontSize = 9.5.sp, color = TextSecondary)
                                                    }
                                                }
                                            }

                                            // 3 Fast Action Chips
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                SuggestAssignChip(
                                                    label = "Giao",
                                                    icon = Icons.Default.SupervisorAccount,
                                                    isSelected = editableAssigner == p.fullName,
                                                    onClick = { editableAssigner = p.fullName }
                                                )
                                                SuggestAssignChip(
                                                    label = "Nhận",
                                                    icon = Icons.Default.Person,
                                                    isSelected = editableResponsiblePerson == p.fullName,
                                                    onClick = { editableResponsiblePerson = p.fullName }
                                                )
                                                SuggestAssignChip(
                                                    label = "Phối hợp",
                                                    icon = Icons.Default.Group,
                                                    isSelected = editableCollaborators.contains(p.fullName),
                                                    onClick = {
                                                        editableCollaborators = if (editableCollaborators.isBlank()) {
                                                            p.fullName
                                                        } else if (!editableCollaborators.contains(p.fullName)) {
                                                            "${editableCollaborators}, ${p.fullName}"
                                                        } else {
                                                            editableCollaborators
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick suggestions when input is empty
                if (inputText.isBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cú pháp nhận diện thông minh:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        syntaxTips.forEach { tip ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.FormatQuote, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(13.dp))
                                    Text(tip, fontSize = 11.sp, color = NavyDeep, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Extracted Entities Smart Preview Card
                AnimatedVisibility(visible = inputText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(16.dp))
                                    Text(
                                        "Kết quả nhận diện tự động:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDeep
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "Đã bóc tách",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 1. Nội dung (Title / Content)
                            ParsedFieldRow(
                                icon = Icons.Default.Description,
                                label = "Nội dung",
                                value = editableTitle,
                                placeholder = "Nội dung công việc...",
                                onValueChange = { editableTitle = it }
                            )

                            // 2. Người giao việc (với gợi ý danh bạ)
                            ParsedFieldWithPeoplePicker(
                                icon = Icons.Default.SupervisorAccount,
                                label = "Người giao việc",
                                value = editableAssigner,
                                placeholder = "Chưa có người giao (chạm chọn từ danh bạ)",
                                people = people,
                                onValueChange = { editableAssigner = it }
                            )

                            // 3. Cán bộ thực hiện / Phụ trách chính (với gợi ý danh bạ)
                            ParsedFieldWithPeoplePicker(
                                icon = Icons.Default.Person,
                                label = "Cán bộ thực hiện",
                                value = editableResponsiblePerson,
                                placeholder = "Chưa chỉ định người thực hiện (chạm chọn từ danh bạ)",
                                people = people,
                                onValueChange = { editableResponsiblePerson = it }
                            )

                            // 4. Thời gian & Hạn chót (với sort & format thông minh)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1.2f)) {
                                    ParsedFieldRow(
                                        icon = Icons.Default.CalendarToday,
                                        label = "Hạn ngày",
                                        value = editableDate,
                                        placeholder = "dd/MM/yyyy",
                                        onValueChange = { editableDate = it }
                                    )
                                }
                                Box(modifier = Modifier.weight(0.8f)) {
                                    ParsedFieldRow(
                                        icon = Icons.Default.Schedule,
                                        label = "Giờ",
                                        value = editableTime,
                                        placeholder = "HH:mm",
                                        onValueChange = { editableTime = it }
                                    )
                                }
                            }

                            // Quick shortcuts for date and time
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val today = sdf.format(cal.time)
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                val tomorrow = sdf.format(cal.time)

                                QuickPill("Hôm nay") { editableDate = today }
                                QuickPill("Ngày mai") { editableDate = tomorrow }
                                QuickPill("08:00") { editableTime = "08:00" }
                                QuickPill("14:00") { editableTime = "14:00" }
                                QuickPill("17:00") { editableTime = "17:00" }
                            }

                            // 5. Địa điểm
                            ParsedFieldRow(
                                icon = Icons.Default.Place,
                                label = "Địa điểm",
                                value = editableLocation,
                                placeholder = "Chưa có địa điểm (e.g. Phòng họp 1, Cơ quan...)",
                                onValueChange = { editableLocation = it }
                            )

                            // 6. Người / Đơn vị phối hợp (với gợi ý danh bạ)
                            ParsedFieldWithPeoplePicker(
                                icon = Icons.Default.Group,
                                label = "Phối hợp",
                                value = editableCollaborators,
                                placeholder = "Chưa có người/đơn vị phối hợp",
                                people = people,
                                onValueChange = { editableCollaborators = it }
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val entity = ParsedCadreEntity(
                                    title = editableTitle.ifBlank { inputText.trim() },
                                    assigner = editableAssigner,
                                    responsiblePerson = editableResponsiblePerson,
                                    timeString = editableTime.ifBlank { "17:00" },
                                    dateString = editableDate.ifBlank { parsed.dateString },
                                    location = editableLocation,
                                    content = editableTitle.ifBlank { inputText.trim() },
                                    collaborators = editableCollaborators,
                                    priority = editablePriority,
                                    inferredType = selectedType,
                                    timestamp = parsed.timestamp
                                )
                                onSubmitEntity(entity)
                                onDismiss()
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_quick_capture_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lưu ngay", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun SuggestAssignChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) NavyDeep else Color(0xFFF1F5F9),
        border = BorderStroke(0.6.dp, if (isSelected) NavyDeep else Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else NavyDeep, modifier = Modifier.size(10.dp))
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else NavyDeep
            )
        }
    }
}

@Composable
private fun QuickPill(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = NavyDeep,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun QuickTypeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NavyDeep else PureWhite,
        border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else NavyDeep,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
private fun ParsedFieldWithPeoplePicker(
    icon: ImageVector,
    label: String,
    value: String,
    placeholder: String,
    people: List<PersonEntity>,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val isMatched = people.any { it.fullName.contains(value, ignoreCase = true) && value.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(13.dp))
                Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }

            if (isMatched) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        "Khớp danh bạ",
                        fontSize = 9.sp,
                        color = NavyDeep,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDeep,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (people.isNotEmpty()) {
                                IconButton(onClick = { showDropdown = !showDropdown }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Chọn cán bộ", tint = NavyDeep, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { isEditing = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Done, contentDescription = "Xong", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PureWhite,
                    border = BorderStroke(0.7.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditing = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = value.ifBlank { placeholder },
                            fontSize = 12.sp,
                            color = if (value.isNotBlank()) TextPrimary else TextMuted,
                            fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (people.isNotEmpty()) {
                                Icon(
                                    Icons.Default.ContactPhone,
                                    contentDescription = "Chọn từ danh bạ",
                                    tint = NavyMedium,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { showDropdown = true }
                                )
                            }
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Dropdown Menu of People in Directory
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                if (people.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Chưa có danh bạ cán bộ", fontSize = 11.sp, color = TextMuted) },
                        onClick = { showDropdown = false }
                    )
                } else {
                    people.forEach { p ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(NavyDeep),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(p.fullName.take(1).uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(p.fullName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        if (p.position.isNotBlank()) {
                                            Text(p.position, fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onValueChange(p.fullName)
                                showDropdown = false
                                isEditing = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedFieldRow(
    icon: ImageVector,
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(13.dp))
                Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyDeep,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                ),
                trailingIcon = {
                    IconButton(onClick = { isEditing = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Done, contentDescription = "Xong", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    }
                }
            )
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PureWhite,
                border = BorderStroke(0.7.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value.ifBlank { placeholder },
                        fontSize = 12.sp,
                        color = if (value.isNotBlank()) TextPrimary else TextMuted,
                        fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
