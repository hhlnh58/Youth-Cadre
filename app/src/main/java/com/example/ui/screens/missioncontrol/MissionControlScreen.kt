package com.example.ui.screens.missioncontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.net.Uri
import com.example.data.model.CalendarEventEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuickLinkEntity
import com.example.data.model.SettlementEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.*
import com.example.ui.screens.calendar.exportEventToGoogleCalendar
import com.example.ui.screens.hub.AddLinkDialog
import com.example.ui.screens.tasks.getTaskNotesSummary
import com.example.ui.theme.*
import com.example.ui.viewmodel.CadreViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray

fun calculateSettlementDaysDiff(deadlineStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val target = sdf.parse(deadlineStr.trim()) ?: return 999L
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            time = target
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (targetCal.timeInMillis - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)
    } catch (_: Exception) {
        999L
    }
}

@Composable
fun MissionControlScreen(
    viewModel: CadreViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToAi: () -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    onOpenEventDetail: (CalendarEventEntity) -> Unit,
    onRescheduleEvent: (CalendarEventEntity) -> Unit,
    onQuickCaptureOpen: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val events by viewModel.calendarEvents.collectAsState()
    val quickLinks by viewModel.quickLinks.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current

    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    val uncompletedTasks = tasks.filter { !it.isCompleted }
    val todayTasks = uncompletedTasks.filter { it.dueDate == todayStr }
    val p1Tasks = uncompletedTasks.filter { it.priority == "P1" }
    val waitingTasks = uncompletedTasks.filter { it.status == "WAITING_FOR_OTHERS" }
    val todayEvents = events.filter { it.date == todayStr }

    val pendingSettlements = remember(settlements) {
        settlements.filter { !it.isCompleted }.sortedWith(
            compareBy<SettlementEntity> { calculateSettlementDaysDiff(it.deadline) }
        ).take(5)
    }

    val latestNotes = remember(notes) {
        notes.sortedWith(
            compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt }
        ).take(5)
    }

    val moneyFormat = remember { DecimalFormat("#,### đ") }

    var showSmartBriefingDialog by remember { mutableStateOf(false) }
    var showQuickLinkDialog by remember { mutableStateOf(false) }
    var editingQuickLink by remember { mutableStateOf<QuickLinkEntity?>(null) }
    var deleteCandidateLink by remember { mutableStateOf<QuickLinkEntity?>(null) }
    var viewingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val currentWsId by viewModel.currentWorkspaceId.collectAsState()
    val workspaces by viewModel.workspaces.collectAsState()
    val currentWorkspace = workspaces.find { it.id == currentWsId } ?: workspaces.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // 1. Executive Hero Banner (Deep Navy Gradient)
        item {
            ExecutiveHeroBanner(
                todayTasksCount = todayTasks.size,
                todayEventsCount = todayEvents.size,
                waitingCount = waitingTasks.size,
                uncompletedTasks = uncompletedTasks,
                events = events,
                workspaceName = currentWorkspace?.name ?: "Tất cả đơn vị",
                onOpenBriefing = { showSmartBriefingDialog = true },
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToCalendar = onNavigateToCalendar,
                onOpenTaskDetail = onOpenTaskDetail,
                onOpenEventDetail = onOpenEventDetail,
                onMarkEventAttended = { viewModel.markEventAttended(it) },
                onRescheduleEvent = onRescheduleEvent
            )
        }


        // 3. Executive Quick Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickCommandPill(
                    icon = Icons.Default.Bolt,
                    label = "Thao tác nhanh",
                    accentColor = NavyDeep,
                    modifier = Modifier.weight(1.3f),
                    onClick = onQuickCaptureOpen
                )
                QuickCommandPill(
                    icon = Icons.Default.AutoAwesome,
                    label = "Cadre AI",
                    accentColor = NavyMedium,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAi
                )
                QuickCommandPill(
                    icon = Icons.Default.AddCircleOutline,
                    label = "Giao việc",
                    accentColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
        }

        // 3. 2x2 Metric Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinimalMetricCard(
                    count = "${uncompletedTasks.size}",
                    label = "Việc cần xử lý",
                    icon = Icons.Default.Checklist,
                    accentColor = NavyMedium,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
                MinimalMetricCard(
                    count = "${p1Tasks.size}",
                    label = "Khẩn cấp P1",
                    icon = Icons.Default.PriorityHigh,
                    accentColor = CoralUrgent,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinimalMetricCard(
                    count = "${waitingTasks.size}",
                    label = "Chờ duyệt",
                    icon = Icons.Default.HourglassTop,
                    accentColor = ChampagneGold,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
                MinimalMetricCard(
                    count = "${todayEvents.size}",
                    label = "Lịch hôm nay",
                    icon = Icons.Default.CalendarToday,
                    accentColor = EmeraldSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCalendar
                )
            }
        }

        // 4. Nhiệm vụ trọng tâm Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleekSectionHeader(
                    title = "Nhiệm vụ trọng tâm",
                    count = uncompletedTasks.size,
                    actionText = "Xem tất cả",
                    onActionClick = onNavigateToTasks
                )

                val focusTasks = (p1Tasks + uncompletedTasks.filter { it.priority != "P1" }).take(5)
                if (focusTasks.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(24.dp))
                            }
                            Text("Chưa có nhiệm vụ tồn đọng", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Đơn vị hiện không có nhiệm vụ trễ hạn hoặc chưa khởi tạo nhiệm vụ mới.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = onQuickCaptureOpen,
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thao tác nhanh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    focusTasks.forEach { task ->
                        ExecutiveDashboardTaskRow(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onClick = { onOpenTaskDetail(task) }
                        )
                    }
                }
            }
        }

        // 5. Lịch trình hôm nay Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleekSectionHeader(
                    title = "Lịch trình hôm nay",
                    count = todayEvents.size,
                    actionText = "Xem lịch",
                    onActionClick = onNavigateToCalendar
                )

                if (todayEvents.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Box(
                            modifier = Modifier.padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Hôm nay chưa có lịch họp hoặc chuyến đi cơ sở nào.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    todayEvents.forEach { event ->
                        ExecutiveEventRow(
                            event = event,
                            onClick = { onOpenEventDetail(event) },
                            onExportGoogle = { exportEventToGoogleCalendar(context, event) }
                        )
                    }
                }
            }
        }

        // 6. Nội dung cần quyết toán Section (Tối đa 5 cái)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleekSectionHeader(
                    title = "Nội dung cần quyết toán",
                    count = pendingSettlements.size,
                    actionText = "Xem tất cả",
                    onActionClick = onNavigateToTasks
                )

                if (pendingSettlements.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Box(
                            modifier = Modifier.padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Không có nội dung tồn đọng cần quyết toán kinh phí.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingSettlements.forEach { settlement ->
                            val diff = calculateSettlementDaysDiff(settlement.deadline)
                            SettlementDashboardCard(
                                settlement = settlement,
                                daysDiff = diff,
                                moneyFormat = moneyFormat,
                                onToggleComplete = { viewModel.toggleSettlementCompletion(settlement) },
                                onClick = onNavigateToTasks
                            )
                        }
                    }
                }
            }
        }

        // 7. Ghi chú mới nhất Section (Tối đa 5 cái)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleekSectionHeader(
                    title = "Ghi chú mới nhất",
                    count = notes.size,
                    actionText = "+ Thêm",
                    onActionClick = onQuickCaptureOpen
                )

                if (latestNotes.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Box(
                            modifier = Modifier.padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chưa có ghi chú nào. Chạm Thao tác nhanh để ghi chép nhanh.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        latestNotes.forEach { note ->
                            NoteDashboardCard(
                                note = note,
                                onClick = { viewingNote = note }
                            )
                        }
                    }
                }
            }
        }

        // 8. Cadre AI Strategic Callout
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onNavigateToAi)
                    .testTag("open_ai_prompt_card"),
                shape = RoundedCornerShape(18.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DeepNavyButtonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cadre AI Cố vấn Chiến lược", fontWeight = FontWeight.Bold, color = NavyHeroDark, fontSize = 14.sp)
                        Text("\"Tham mưu phân bổ lịch trình & rà soát nhiệm vụ hôm nay\"", color = NavyMedium, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 7. Liên kết tác nghiệp nhanh Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SleekSectionHeader(
                    title = "Liên kết tác nghiệp nhanh",
                    count = quickLinks.size,
                    actionText = "+ Thêm",
                    onActionClick = {
                        editingQuickLink = null
                        showQuickLinkDialog = true
                    }
                )

                if (quickLinks.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(22.dp))
                            }
                            Text("Chưa có liên kết tác nghiệp", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("Lưu các đường link Cổng thông tin, Google Drive hoặc hệ thống văn bản để mở nhanh.", color = TextSecondary, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = {
                                    editingQuickLink = null
                                    showQuickLinkDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thêm liên kết nhanh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickLinks.forEach { link ->
                            QuickLinkDashboardCard(
                                link = link,
                                onClick = {
                                    try {
                                        val uri = Uri.parse(if (!link.url.startsWith("http://") && !link.url.startsWith("https://")) "https://${link.url}" else link.url)
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                onEdit = {
                                    editingQuickLink = link
                                    showQuickLinkDialog = true
                                },
                                onDelete = {
                                    deleteCandidateLink = link
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQuickLinkDialog) {
        AddLinkDialog(
            initialLink = editingQuickLink,
            onDismiss = {
                showQuickLinkDialog = false
                editingQuickLink = null
            },
            onSave = { id, title, url, cat, desc ->
                viewModel.saveQuickLink(id = id, title = title, url = url, category = cat, desc = desc)
                showQuickLinkDialog = false
                editingQuickLink = null
            }
        )
    }

    if (deleteCandidateLink != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidateLink = null },
            title = { Text("Xác nhận xóa liên kết?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Đồng chí có chắc chắn muốn xóa liên kết \"${deleteCandidateLink?.title}\"?", fontSize = 13.sp, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidateLink?.let { viewModel.deleteQuickLink(it) }
                        deleteCandidateLink = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralUrgent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Xác nhận xóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateLink = null }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = PureWhite
        )
    }

    if (viewingNote != null) {
        ExecutiveNoteViewDialog(
            note = viewingNote!!,
            onDismiss = { viewingNote = null },
            onPinToggle = { note ->
                viewModel.saveNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    category = note.category,
                    isPinned = !note.isPinned
                )
                viewingNote = note.copy(isPinned = !note.isPinned)
            },
            onDelete = { note ->
                viewModel.deleteNote(note)
                viewingNote = null
            }
        )
    }

    // Detailed Executive Strategic Briefing Dialog
    if (showSmartBriefingDialog) {
        ExecutiveBriefingDialog(
            todayTasks = todayTasks,
            p1Tasks = p1Tasks,
            waitingTasks = waitingTasks,
            uncompletedTasks = uncompletedTasks,
            todayEvents = todayEvents,
            allEvents = events,
            workspaceName = currentWorkspace?.name ?: "Tất cả đơn vị",
            onDismiss = { showSmartBriefingDialog = false },
            onPlanWithAi = {
                showSmartBriefingDialog = false
                viewModel.currentAiMode.value = "PLANNER"
                viewModel.sendAiPrompt("Dựa vào Bản tin Điều hành Cán bộ Đoàn hôm nay, hãy lập kế hoạch phân bổ thời gian và lộ trình xử lý nhiệm vụ trọng tâm theo từng khung giờ sáng, chiều, tối.")
                onNavigateToAi()
            },
            onOpenTask = { task ->
                showSmartBriefingDialog = false
                onOpenTaskDetail(task)
            },
            onNavigateToCalendar = {
                showSmartBriefingDialog = false
                onNavigateToCalendar()
            }
        )
    }
}

@Composable
fun QuickCommandPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NavyHeroDark,
                maxLines = 1
            )
        }
    }
}

@Composable
fun HeroStatPill(
    count: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Text(
                text = count,
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                color = color
            )
        }
    }
}

@Composable
fun ExecutiveHeroBanner(
    todayTasksCount: Int,
    todayEventsCount: Int,
    waitingCount: Int,
    uncompletedTasks: List<TaskEntity>,
    events: List<CalendarEventEntity>,
    workspaceName: String,
    onOpenBriefing: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    onOpenEventDetail: (CalendarEventEntity) -> Unit,
    onMarkEventAttended: (CalendarEventEntity) -> Unit,
    onRescheduleEvent: (CalendarEventEntity) -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greeting, greetingIcon) = when (hour) {
        in 5..11 -> Pair("Chào buổi sáng, Đồng chí!", Icons.Default.WbSunny)
        in 12..17 -> Pair("Chào buổi chiều, Đồng chí!", Icons.Default.WbSunny)
        else -> Pair("Chào buổi tối, Đồng chí!", Icons.Default.NightsStay)
    }

    val dayFormat = SimpleDateFormat("EEEE, 'ngày' dd/MM/yyyy", Locale("vi", "VN"))
    val todayVietnamese = dayFormat.format(Date()).replaceFirstChar { it.titlecase() }
    val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    // 1. Highlight Top Mission (Priority P1 first, or today's task, or next task)
    val topMission = uncompletedTasks.find { it.priority == "P1" }
        ?: uncompletedTasks.find { it.dueDate == todayStr }
        ?: uncompletedTasks.firstOrNull()

    // 2. Highlight Next Schedule (Filter: Only upcoming, not attended, not passed > 15m)
    val now = System.currentTimeMillis()
    val upcomingEvents = events
        .filter { com.example.ui.screens.calendar.isEventUpcoming(it, now) }
        .sortedBy { com.example.ui.screens.calendar.parseEventStartTimestamp(it.date, it.startTime) }
    val nextSchedule = upcomingEvents.firstOrNull()

    val HeroBlueGradient = Brush.linearGradient(
        listOf(
            Color(0xFF0A2540),
            Color(0xFF104382),
            Color(0xFF1A56A8),
            Color(0xFF0284C7)
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = NavyDeep,
        border = BorderStroke(1.2.dp, Color(0x60BAE6FD)),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeroBlueGradient)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header: Clean Date Chip + Live Status Badge (No duplicate logo or unit pill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFFBAE6FD),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = todayVietnamese,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Text(
                                "HỆ THỐNG TRỰC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Greeting & Subtitle with Greeting Icon
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = greetingIcon,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Không gian làm việc & điều hành công tác cán bộ Đoàn",
                        fontSize = 12.5.sp,
                        color = Color(0xFFF0F9FF),
                        fontWeight = FontWeight.Medium
                    )
                }

                // === HERO CARD 1: CÔNG VIỆC TRỌNG TÂM CẦN XỬ LÝ (Crisp White Surface) ===
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = BorderStroke(
                        1.dp,
                        if (topMission?.priority == "P1") Color(0xFFFCA5A5) else Color(0xFFBAE6FD)
                    ),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (topMission != null) onOpenTaskDetail(topMission) else onNavigateToTasks()
                            }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (topMission?.priority == "P1") Color(0xFFFEE2E2) else Color(0xFFEFF6FF),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = if (topMission?.priority == "P1") Color(0xFFDC2626) else Color(0xFF2563EB),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    "NHIỆM VỤ TRỌNG TÂM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (topMission?.priority == "P1") Color(0xFFB91C1C) else Color(0xFF1D4ED8),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            if (topMission != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (topMission.priority == "P1") Color(0xFFDC2626) else Color(0xFFEFF6FF),
                                    border = BorderStroke(0.6.dp, if (topMission.priority == "P1") Color(0xFFB91C1C) else Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        if (topMission.priority == "P1") "P1 • KHẨN CẤP" else "ƯU TIÊN ${topMission.priority}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (topMission.priority == "P1") Color.White else Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (topMission != null) {
                            Text(
                                text = topMission.title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        "Hạn: ${topMission.dueDate.ifBlank { "Chưa đặt" }}${if (topMission.dueTime.isNotBlank()) " • " + topMission.dueTime else ""}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (uncompletedTasks.size > 1) {
                                    Text(
                                        "+${uncompletedTasks.size - 1} việc khác →",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Tuyệt vời! Không còn việc tồn đọng cần xử lý.",
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // === HERO CARD 2: LỊCH TRÌNH CÔNG TÁC SẮP TỚI (Crisp White Surface) ===
                if (nextSchedule != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenEventDetail(nextSchedule) }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFECFDF5),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Event,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "LỊCH TRÌNH SẮP TỚI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF047857),
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(0.6.dp, Color(0xFFA7F3D0))
                                ) {
                                    Text(
                                        if (nextSchedule.date == todayStr) "HÔM NAY" else nextSchedule.date,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = nextSchedule.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            nextSchedule.startTime.ifBlank { "08:30" },
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            nextSchedule.location.ifBlank { "Cơ quan Đoàn" },
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF475569),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    "Chi tiết →",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }

                            // Quick Action Buttons: "Đã tham gia" & "Đổi lịch"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onMarkEventAttended(nextSchedule) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Đã tham gia", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { onRescheduleEvent(nextSchedule) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Đổi lịch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCalendar() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFECFDF5),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Không có lịch trình sắp tới",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Các lịch trình đã hoàn thành, đã tham gia hoặc đã qua giờ.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "Xem lịch →",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                        }
                    }
                }

                // Micro Stats Row (4 stats: Việc hôm nay, Lịch hôm nay, P1 khẩn, Chờ duyệt)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeroStatPill(
                        count = "$todayTasksCount",
                        label = "Việc hôm",
                        icon = Icons.Default.Checklist,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTasks
                    )
                    HeroStatPill(
                        count = "$todayEventsCount",
                        label = "Lịch hôm",
                        icon = Icons.Default.Event,
                        color = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCalendar
                    )
                    HeroStatPill(
                        count = "${uncompletedTasks.count { it.priority == "P1" }}",
                        label = "Khẩn P1",
                        icon = Icons.Default.PriorityHigh,
                        color = Color(0xFFF87171),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTasks
                    )
                    HeroStatPill(
                        count = "$waitingCount",
                        label = "Chờ duyệt",
                        icon = Icons.Default.HourglassTop,
                        color = Color(0xFFFDE047),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTasks
                    )
                }

                // BẢN TIN ĐIỀU HÀNH CÔNG TÁC (NO RED TAG, NO TÁC CHIẾN, ELEVATED & CRISP)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhite,
                    border = BorderStroke(1.2.dp, Color(0xFFBAE6FD)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenBriefing() }
                        .testTag("open_executive_briefing_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "BẢN TIN ĐIỀU HÀNH CÔNG TÁC",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F3D7D),
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    "Báo cáo giao ban tổng hợp, nhiệm vụ & lịch trình chi tiết",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ArrowForwardIos,
                                    contentDescription = "Xem",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveBriefingDialog(
    todayTasks: List<TaskEntity>,
    p1Tasks: List<TaskEntity>,
    waitingTasks: List<TaskEntity>,
    uncompletedTasks: List<TaskEntity>,
    todayEvents: List<CalendarEventEntity>,
    allEvents: List<CalendarEventEntity>,
    workspaceName: String,
    onDismiss: () -> Unit,
    onPlanWithAi: () -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val dayFormat = SimpleDateFormat("EEEE, 'ngày' dd/MM/yyyy", Locale("vi", "VN"))
    val todayVietnamese = dayFormat.format(Date()).replaceFirstChar { it.titlecase() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavyGradient)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ModernYouthCadreLogo(size = 36.dp)
                            Column {
                                Text(
                                    "BẢN TIN ĐIỀU HÀNH CÔNG TÁC",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    "$todayVietnamese • $workspaceName",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFBAE6FD)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 2. Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Metric Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BriefingMetricCard("Tổng việc", "${uncompletedTasks.size}", NavyDeep, Modifier.weight(1f))
                        BriefingMetricCard("Khẩn P1", "${p1Tasks.size}", CoralUrgent, Modifier.weight(1f))
                        BriefingMetricCard("Họp hôm nay", "${todayEvents.size}", EmeraldSuccess, Modifier.weight(1f))
                        BriefingMetricCard("Chờ duyệt", "${waitingTasks.size}", ChampagneGold, Modifier.weight(1f))
                    }

                    // Section 1: Nhiệm vụ trọng tâm
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(16.dp))
                            Text("1. NHIỆM VỤ TRỌNG TÂM CẦN XỬ LÝ (${uncompletedTasks.size})", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = NavyHeroDark)
                        }

                        val focusList = (p1Tasks + uncompletedTasks.filter { it.priority != "P1" }).take(5)
                        if (focusList.isNotEmpty()) {
                            focusList.forEach { task ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (task.priority == "P1") CoralUrgent.copy(alpha = 0.4f) else BorderLight),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenTask(task) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SleekPriorityBadge(priority = task.priority)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                task.title,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (task.responsiblePersonName.isNotBlank()) {
                                                    Text(
                                                        "Phụ trách: ${task.responsiblePersonName.substringBefore('(').trim()}",
                                                        fontSize = 10.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                                if (task.dueDate.isNotBlank()) {
                                                    Text(
                                                        "Hạn: ${task.dueDate}${if (task.dueTime.isNotBlank()) " " + task.dueTime else ""}",
                                                        fontSize = 10.sp,
                                                        color = if (task.priority == "P1") CoralUrgent else TextSecondary,
                                                        fontWeight = if (task.priority == "P1") FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Không có việc khẩn tồn đọng. Tất cả chỉ tiêu hoàn thành tốt!",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Section 2: Lịch trình công tác
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(16.dp))
                            Text("2. LỊCH HỌP & CÔNG TÁC (${allEvents.size})", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = NavyHeroDark)
                        }

                        val eventList = allEvents.take(4)
                        if (eventList.isNotEmpty()) {
                            eventList.forEach { ev ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToCalendar() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                ev.startTime.ifBlank { "08:30" },
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NavyDeep,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ev.title,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "Địa điểm: ${ev.location.ifBlank { "Cơ quan Đoàn" }} • Đầu mối: ${ev.contactPerson.ifBlank { ev.participants.ifBlank { "Ban Thường vụ" } }}",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Chưa có lịch họp hay công tác mới nào được ghi nhận.",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Section 3: Hồ sơ chờ phê duyệt
                    if (waitingTasks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                Text("3. HỒ SƠ & VIỆC CHỜ PHỐI HỢP (${waitingTasks.size})", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = NavyHeroDark)
                            }

                            waitingTasks.take(3).forEach { task ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(0.8.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(15.dp))
                                        Text(
                                            task.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Ý kiến tham mưu từ Cố vấn AI
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(15.dp))
                                Text("KHUYẾN NGHỊ ĐIỀU HÀNH TỪ CỐ VẤN AI", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                            }
                            Text(
                                "• Ưu tiên giải quyết dứt điểm các nhiệm vụ P1 trước 11:30 sáng.",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                "• Rà soát văn kiện và thông báo triệu tập trước cuộc họp 30 phút.",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                "• Đôn đốc các cơ sở Đoàn hoàn thành đúng tiến độ báo cáo tuần.",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }

                // 3. Footer Action Buttons
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Đóng", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPlanWithAi,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lập lịch với AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BriefingMetricCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(count, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
            Text(title, fontSize = 9.5.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

fun getTaskUrgencyBadge(dueDate: String, dueTime: String, isCompleted: Boolean): Triple<String, Color, Color>? {
    if (dueDate.isBlank() || isCompleted) return null
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return try {
        val date = sdf.parse(dueDate) ?: return null
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dueCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffDays = ((dueCal.timeInMillis - todayCal.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        when {
            diffDays < 0 -> Triple("Quá hạn ${-diffDays} ngày", CoralUrgent, Color(0xFFFEF2F2))
            diffDays == 0 -> Triple("Hôm nay ${dueTime.ifBlank { "" }}".trim(), Color(0xFFD97706), Color(0xFFFFFBEB))
            diffDays == 1 -> Triple("Ngày mai", Color(0xFF2563EB), Color(0xFFEFF6FF))
            diffDays in 2..3 -> Triple("Còn $diffDays ngày", Color(0xFF0D9488), Color(0xFFF0FDFA))
            else -> Triple(dueDate, TextSecondary, Color(0xFFF1F5F9))
        }
    } catch (_: Exception) {
        null
    }
}

fun getAttachedFilesCategorySummary(attachedJson: String): String {
    if (attachedJson.isBlank() || attachedJson == "[]") return ""
    return try {
        val arr = JSONArray(attachedJson)
        val categories = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val cat = arr.getJSONObject(i).optString("category", "")
            val title = when (cat) {
                "KE_HOACH" -> "Kế hoạch"
                "CONG_VAN" -> "Công văn"
                "TO_TRINH" -> "Tờ trình"
                "BAO_CAO" -> "Báo cáo"
                "THONG_BAO" -> "Thông báo"
                "PHIEU_CHUYEN" -> "Phiếu chuyển"
                "HUONG_DAN" -> "Hướng dẫn"
                "QUYET_DINH" -> "Quyết định"
                "KICH_BAN" -> "Kịch bản"
                else -> ""
            }
            if (title.isNotBlank() && !categories.contains(title)) {
                categories.add(title)
            }
        }
        if (categories.isNotEmpty()) {
            "${arr.length()} tệp (${categories.take(2).joinToString(", ")})"
        } else {
            "${arr.length()} tệp"
        }
    } catch (_: Exception) {
        ""
    }
}

@Composable
fun ExecutiveDashboardTaskRow(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "P1" -> CoralUrgent
        "P2" -> NavyDeep
        "P3" -> EmeraldSuccess
        else -> Color(0xFF64748B)
    }

    val urgency = getTaskUrgencyBadge(task.dueDate, task.dueTime, task.isCompleted)
    val docSummary = getAttachedFilesCategorySummary(task.attachedFilesJson)
    val (noteCount, latestNoteSnippet) = getTaskNotesSummary(task.progressNotesJson)
    val displayNote = latestNoteSnippet.ifBlank { task.outputNotes }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("executive_task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(
            1.dp,
            if (task.priority == "P1" && !task.isCompleted) CoralUrgent.copy(alpha = 0.35f) else BorderLight
        ),
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left priority stripe
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight()
                    .background(if (task.isCompleted) TextMuted else priorityColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Top Header Row: Priority Badge + Urgency / Deadline Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SleekPriorityBadge(priority = task.priority)
                        if (urgency != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = urgency.third,
                                border = BorderStroke(0.6.dp, urgency.second.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    urgency.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = urgency.second,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (urgency == null && task.dueDate.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "${task.dueDate}${if (task.dueTime.isNotBlank()) " " + task.dueTime else ""}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Task Content Row: Checkbox + Full Width Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onToggleComplete),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleComplete() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldSuccess,
                                uncheckedColor = Color(0xFF94A3B8)
                            )
                        )
                    }

                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sub Info Row: Assigner, Responsible person, Attached files, Deadline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.assignerName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(11.dp))
                                    Text(
                                        task.assignerName.substringBefore('(').trim(),
                                        fontSize = 10.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (task.responsiblePersonName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(11.dp))
                                    Text(
                                        task.responsiblePersonName.substringBefore('(').trim(),
                                        fontSize = 10.sp,
                                        color = NavyDeep,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (docSummary.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(0.6.dp, Color(0xFFBBF7D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(11.dp))
                                    Text(
                                        docSummary,
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF166534),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (noteCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                                    Text(
                                        "$noteCount ghi chú",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (urgency == null && task.dueDate.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "${task.dueDate}${if (task.dueTime.isNotBlank()) " " + task.dueTime else ""}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Note callout if task has notes
                if (displayNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB).copy(alpha = 0.85f),
                        border = BorderStroke(0.6.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                            Text(
                                text = "Ghi chú: $displayNote",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Progress Bar if in progress
                if (task.progressPercent in 1..99 && !task.isCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { task.progressPercent / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NavyDeep,
                            trackColor = Color(0xFFE2E8F0)
                        )
                        Text(
                            "${task.progressPercent}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveEventRow(
    event: CalendarEventEntity,
    onClick: () -> Unit,
    onExportGoogle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
            ) {
                Text(
                    text = event.startTime.ifBlank { "08:30" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.location.ifBlank { "Cơ quan Đoàn" },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (event.isAttended) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        "✓ Đã tham gia",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (com.example.ui.screens.calendar.isEventPassed15Minutes(event)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        "Đã qua",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.clickable { onExportGoogle() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(13.dp))
                    Text("Lịch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                }
            }
        }
    }
}

@Composable
fun QuickLinkDashboardCard(
    link: QuickLinkEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryIcon = when (link.category) {
        "Google Drive" -> Icons.Default.CloudQueue
        "Cổng thông tin" -> Icons.Default.Public
        "Biểu mẫu" -> Icons.Default.Description
        "Hệ thống" -> Icons.Default.Computer
        else -> Icons.Default.Link
    }
    val categoryColor = when (link.category) {
        "Google Drive" -> Color(0xFF0F9D58)
        "Cổng thông tin" -> NavyDeep
        "Biểu mẫu" -> Color(0xFFD97706)
        "Hệ thống" -> Color(0xFF2563EB)
        else -> NavyDeep
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(categoryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(18.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = link.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (link.category.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = categoryColor.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = link.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = link.description.ifEmpty { link.url },
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit)
                        .testTag("edit_quick_link_${link.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = NavyDeep, modifier = Modifier.size(14.dp))
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .testTag("delete_quick_link_${link.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = CoralUrgent, modifier = Modifier.size(15.dp))
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClick)
                        .testTag("open_quick_link_${link.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Mở", tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
fun SettlementDashboardCard(
    settlement: SettlementEntity,
    daysDiff: Long,
    moneyFormat: DecimalFormat,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val deadlineBadge = when {
        settlement.isCompleted -> Triple("Đã quyết toán", EmeraldSuccess, Color(0xFFD1FAE5))
        daysDiff < 0 -> Triple("Quá hạn ${-daysDiff} ngày", CoralUrgent, Color(0xFFFEE2E2))
        daysDiff == 0L -> Triple("Hạn hôm nay", CoralUrgent, Color(0xFFFEF3C7))
        daysDiff in 1..3 -> Triple("Còn $daysDiff ngày", Color(0xFFD97706), Color(0xFFFEF3C7))
        else -> Triple(settlement.deadline.ifBlank { "Chưa có hạn" }, NavyDeep, Color(0xFFEFF6FF))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, if (daysDiff <= 0 && !settlement.isCompleted) CoralUrgent.copy(alpha = 0.4f) else BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = settlement.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = deadlineBadge.third
                    ) {
                        Text(
                            text = deadlineBadge.first,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = deadlineBadge.second,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = moneyFormat.format(settlement.amount),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        color = Color(0xFFB45309)
                    )
                    if (settlement.budgetEstimateCode.isNotBlank()) {
                        Text(
                            text = "• Dự trù: ${settlement.budgetEstimateCode}",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    if (settlement.responsiblePerson.isNotBlank()) {
                        Text(
                            text = "• ${settlement.responsiblePerson}",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Quick Complete button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECFDF5))
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Quyết toán xong", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun NoteDashboardCard(
    note: NoteEntity,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val timeFormatted = remember(note.updatedAt) { sdf.format(Date(note.updatedAt)) }
    val (catLabel, catColor, catBg) = when (note.category) {
        "MEETING" -> Triple("Cuộc họp", NavyDeep, Color(0xFFEFF6FF))
        "FIELD" -> Triple("Cơ sở", Color(0xFF0F766E), Color(0xFFF0FDFA))
        "PERSONAL" -> Triple("Cá nhân", Color(0xFF6D28D9), Color(0xFFF5F3FF))
        else -> Triple("Ghi chép", Color(0xFF0369A1), Color(0xFFF0F9FF))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, if (note.isPinned) Color(0xFFFDE68A) else BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(catBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (note.isPinned) Icons.Default.PushPin else Icons.Default.StickyNote2,
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = note.title.ifBlank { "Ghi chú không tiêu đề" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (note.isPinned) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = catBg
                    ) {
                        Text(
                            text = catLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Text(
                    text = note.content.ifBlank { "Không có nội dung chi tiết" },
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Xem chi tiết →",
                        fontSize = 10.sp,
                        color = NavyDeep,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutiveNoteViewDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onPinToggle: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val timeFormatted = remember(note.updatedAt) { sdf.format(Date(note.updatedAt)) }
    val (catLabel, catColor, catBg) = when (note.category) {
        "MEETING" -> Triple("Cuộc họp", NavyDeep, Color(0xFFEFF6FF))
        "FIELD" -> Triple("Cơ sở", Color(0xFF0F766E), Color(0xFFF0FDFA))
        "PERSONAL" -> Triple("Cá nhân", Color(0xFF6D28D9), Color(0xFFF5F3FF))
        else -> Triple("Ghi chép", Color(0xFF0369A1), Color(0xFFF0F9FF))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catBg
                        ) {
                            Text(
                                text = catLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = timeFormatted,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = note.title.ifBlank { "Ghi chú công tác" },
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = NavyHeroDark
                )

                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.6.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.content.ifBlank { "Không có nội dung ghi chép." },
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onPinToggle(note) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (note.isPinned) Color(0xFFD97706) else BorderLight),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (note.isPinned) Color(0xFFD97706) else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (note.isPinned) "Bỏ ghim" else "Ghim lên đầu",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (note.isPinned) Color(0xFFD97706) else TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { onDelete(note) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = CoralUrgent, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Đóng", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

