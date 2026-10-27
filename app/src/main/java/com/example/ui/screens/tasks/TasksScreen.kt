package com.example.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NoteEntity
import com.example.data.model.SettlementEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.SleekPriorityBadge
import com.example.ui.components.SleekStatusPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.CadreViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray

@Composable
fun TasksScreen(
    viewModel: CadreViewModel,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    onCreateTask: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val portfolios by viewModel.portfolios.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val currentWorkspaceId by viewModel.currentWorkspaceId.collectAsState()
    val workspaces by viewModel.workspaces.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeSection by remember { mutableStateOf("TASKS") } // TASKS, SETTLEMENTS, NOTES
    var viewMode by remember { mutableStateOf("LIST") } // LIST, KANBAN, WAITING
    var selectedPriorityFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isArchiveMode by remember { mutableStateOf(false) }

    val activeTasks = remember(tasks) { tasks.filter { !it.isCompleted && it.status != "COMPLETED" } }
    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted || it.status == "COMPLETED" } }

    val filteredTasks = remember(tasks, isArchiveMode, selectedPriorityFilter, selectedStatusFilter, searchQuery) {
        val basePool = if (isArchiveMode) completedTasks else activeTasks
        basePool.filter { task ->
            (selectedPriorityFilter == null || task.priority == selectedPriorityFilter) &&
            (selectedStatusFilter == null || task.status == selectedStatusFilter) &&
            (searchQuery.isBlank() ||
             task.title.contains(searchQuery, ignoreCase = true) ||
             task.assignerName.contains(searchQuery, ignoreCase = true) ||
             task.responsiblePersonName.contains(searchQuery, ignoreCase = true) ||
             task.collaborators.contains(searchQuery, ignoreCase = true))
        }
    }

    val activeCount = activeTasks.size
    val p1Count = activeTasks.count { it.priority == "P1" }
    val inProgressCount = activeTasks.count { it.status == "IN_PROGRESS" }
    val completedCount = completedTasks.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Executive Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Nhiệm vụ trọng tâm",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = NavyHeroDark
                )
                Text(
                    "Theo dõi & Điều hành các nhiệm vụ chỉ đạo",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onCreateTask,
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("create_task_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tạo việc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Top Sub-Tab Switcher: Nhiệm vụ công tác vs Quyết toán kinh phí vs Sổ tay Ghi chú
        val taskNotesCount = tasks.count { it.progressNotesJson.isNotBlank() && it.progressNotesJson != "[]" }
        val pendingSettlementsCount = settlements.count { !it.isCompleted }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0).copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Nhiệm vụ
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { activeSection = "TASKS" },
                shape = RoundedCornerShape(9.dp),
                color = if (activeSection == "TASKS") PureWhite else Color.Transparent,
                shadowElevation = if (activeSection == "TASKS") 1.5.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = null,
                        tint = if (activeSection == "TASKS") NavyDeep else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Nhiệm vụ (${tasks.size})",
                        fontSize = 11.5.sp,
                        fontWeight = if (activeSection == "TASKS") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSection == "TASKS") NavyDeep else TextSecondary
                    )
                }
            }

            // Tab 2: Quyết toán
            Surface(
                modifier = Modifier
                    .weight(1.05f)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { activeSection = "SETTLEMENTS" },
                shape = RoundedCornerShape(9.dp),
                color = if (activeSection == "SETTLEMENTS") PureWhite else Color.Transparent,
                shadowElevation = if (activeSection == "SETTLEMENTS") 1.5.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (activeSection == "SETTLEMENTS") Color(0xFFD97706) else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (pendingSettlementsCount > 0) "Quyết toán ($pendingSettlementsCount)" else "Quyết toán (${settlements.size})",
                        fontSize = 11.5.sp,
                        fontWeight = if (activeSection == "SETTLEMENTS") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSection == "SETTLEMENTS") NavyDeep else TextSecondary
                    )
                }
            }

            // Tab 3: Sổ tay Ghi chú
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { activeSection = "NOTES" },
                shape = RoundedCornerShape(9.dp),
                color = if (activeSection == "NOTES") PureWhite else Color.Transparent,
                shadowElevation = if (activeSection == "NOTES") 1.5.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.StickyNote2,
                        contentDescription = null,
                        tint = if (activeSection == "NOTES") NavyDeep else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Sổ tay (${notes.size + taskNotesCount})",
                        fontSize = 11.5.sp,
                        fontWeight = if (activeSection == "NOTES") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSection == "NOTES") NavyDeep else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeSection == "NOTES") {
            CadreNotesTabView(
                viewModel = viewModel,
                tasks = tasks,
                notes = notes,
                onOpenTask = onOpenTaskDetail
            )
        } else if (activeSection == "SETTLEMENTS") {
            CadreSettlementsTabView(
                viewModel = viewModel,
                tasks = tasks,
                settlements = settlements,
                onOpenTask = onOpenTaskDetail
            )
        } else {
            // Metric Overview KPI Cards
            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskKpiCard(
                title = "Đang xử lý",
                count = activeCount,
                accentColor = NavyDeep,
                modifier = Modifier.weight(1f),
                onClick = {
                    isArchiveMode = false
                    selectedStatusFilter = null
                    selectedPriorityFilter = null
                }
            )
            TaskKpiCard(
                title = "Khẩn P1",
                count = p1Count,
                accentColor = CoralUrgent,
                isAlert = p1Count > 0,
                modifier = Modifier.weight(1f),
                onClick = {
                    isArchiveMode = false
                    selectedPriorityFilter = "P1"
                }
            )
            TaskKpiCard(
                title = "Tiến độ",
                count = inProgressCount,
                accentColor = NavyPrimary,
                modifier = Modifier.weight(1f),
                onClick = {
                    isArchiveMode = false
                    selectedStatusFilter = "IN_PROGRESS"
                }
            )
            TaskKpiCard(
                title = "Kho lưu trữ",
                count = completedCount,
                accentColor = EmeraldSuccess,
                modifier = Modifier.weight(1f),
                onClick = {
                    isArchiveMode = true
                    selectedPriorityFilter = null
                    selectedStatusFilter = null
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isArchiveMode) "Tìm trong kho lưu trữ..." else "Tìm theo tên nhiệm vụ, người giao, phụ trách...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_search_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NavyDeep,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // View Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeSegmentChip(
                label = "Danh sách",
                icon = Icons.Default.ViewList,
                isSelected = viewMode == "LIST",
                onClick = { viewMode = "LIST" }
            )
            ModeSegmentChip(
                label = "Kanban Tiến độ",
                icon = Icons.Default.ViewKanban,
                isSelected = viewMode == "KANBAN",
                onClick = { viewMode = "KANBAN" }
            )
            ModeSegmentChip(
                label = "Chờ duyệt / Phối hợp",
                icon = Icons.Default.HourglassTop,
                isSelected = viewMode == "WAITING",
                onClick = { viewMode = "WAITING" }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterBadge(
                label = "Đang xử lý ($activeCount)",
                isActive = !isArchiveMode && selectedPriorityFilter == null && selectedStatusFilter == null,
                onClick = {
                    isArchiveMode = false
                    selectedPriorityFilter = null
                    selectedStatusFilter = null
                }
            )
            FilterBadge(
                label = "Khẩn cấp P1",
                isActive = !isArchiveMode && selectedPriorityFilter == "P1",
                accentColor = CoralUrgent,
                onClick = {
                    isArchiveMode = false
                    selectedPriorityFilter = if (selectedPriorityFilter == "P1") null else "P1"
                }
            )
            FilterBadge(
                label = "Trọng tâm P2",
                isActive = !isArchiveMode && selectedPriorityFilter == "P2",
                accentColor = ChampagneGold,
                onClick = {
                    isArchiveMode = false
                    selectedPriorityFilter = if (selectedPriorityFilter == "P2") null else "P2"
                }
            )
            FilterBadge(
                label = "Kho lưu trữ ($completedCount)",
                isActive = isArchiveMode,
                accentColor = EmeraldSuccess,
                onClick = {
                    isArchiveMode = !isArchiveMode
                    selectedPriorityFilter = null
                    selectedStatusFilter = null
                }
            )
        }

        if (isArchiveMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Kho lưu trữ việc đã hoàn thành ($completedCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                            Text("Bấm 'Khôi phục' để đưa nhiệm vụ trở lại danh sách đang xử lý", fontSize = 10.5.sp, color = Color(0xFF047857))
                        }
                    }
                    TextButton(
                        onClick = { isArchiveMode = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Trở lại", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tasks Body
        when (viewMode) {
            "KANBAN" -> KanbanViewClean(
                tasks = filteredTasks,
                onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                onOpenTaskDetail = onOpenTaskDetail
            )
            "WAITING" -> WaitingForViewClean(
                tasks = filteredTasks.filter { it.status == "WAITING_FOR_OTHERS" || it.waitingForWho.isNotBlank() },
                onOpenTaskDetail = onOpenTaskDetail
            )
            else -> ListViewClean(
                tasks = filteredTasks,
                totalTasksCount = tasks.size,
                currentWorkspaceName = workspaces.firstOrNull { it.id == currentWorkspaceId }?.name ?: "",
                hasActiveFilters = (selectedPriorityFilter != null || selectedStatusFilter != null || searchQuery.isNotBlank()),
                onClearFilters = {
                    selectedPriorityFilter = null
                    selectedStatusFilter = null
                    searchQuery = ""
                },
                onShowAllWorkspaces = { viewModel.setWorkspace("") },
                onSyncCloud = {
                    viewModel.triggerManualCloudSync()
                    android.widget.Toast.makeText(context, "Đang đồng bộ dữ liệu từ Firebase Cloud...", android.widget.Toast.LENGTH_SHORT).show()
                },
                onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                onOpenTaskDetail = onOpenTaskDetail,
                onCreateTask = onCreateTask
            )
        }
        }
    }
}

@Composable
fun TaskKpiCard(
    title: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = if (isAlert) CoralUrgent.copy(alpha = 0.08f) else PureWhite,
        border = BorderStroke(1.dp, if (isAlert) CoralUrgent.copy(alpha = 0.4f) else BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                fontSize = 10.sp,
                color = if (isAlert) CoralUrgent else TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isAlert) CoralUrgent else accentColor
            )
        }
    }
}

@Composable
fun ModeSegmentChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NavyDeep else PureWhite,
        border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else NavyDeep,
                modifier = Modifier.size(15.dp)
            )
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
fun FilterBadge(
    label: String,
    isActive: Boolean,
    accentColor: Color = NavyDeep,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) accentColor.copy(alpha = 0.14f) else PureWhite,
        border = BorderStroke(1.dp, if (isActive) accentColor else BorderLight)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) accentColor else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ListViewClean(
    tasks: List<TaskEntity>,
    totalTasksCount: Int = tasks.size,
    currentWorkspaceName: String = "",
    hasActiveFilters: Boolean = false,
    onClearFilters: () -> Unit = {},
    onShowAllWorkspaces: () -> Unit = {},
    onSyncCloud: () -> Unit = {},
    onToggleComplete: (TaskEntity) -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit,
    onCreateTask: () -> Unit
) {
    if (tasks.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(28.dp))
                }

                if (hasActiveFilters) {
                    Text("Không tìm thấy nhiệm vụ phù hợp", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Không có nhiệm vụ nào khớp với tiêu chí lọc hoặc tìm kiếm.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onClearFilters,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyDeep)
                    ) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xóa bộ lọc để hiện tất cả", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (currentWorkspaceName.isNotBlank() && totalTasksCount == 0) {
                    Text("Chưa có nhiệm vụ trong đơn vị này", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Đang xem đơn vị: '$currentWorkspaceName'. Các nhiệm vụ trước đó có thể thuộc đơn vị khác hoặc toàn hệ thống.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShowAllWorkspaces,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyDeep)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xem tất cả đơn vị", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onSyncCloud,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đồng bộ đám mây", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Chưa có nhiệm vụ nào", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Nếu bạn đã tạo nhiệm vụ trên máy điện thoại, hãy nhấn 'Đồng bộ từ Cloud' để tải về preview ngay.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSyncCloud,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyDeep)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đồng bộ từ Cloud", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onCreateTask,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tạo nhiệm vụ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                ExecutiveTaskCard(
                    task = task,
                    onToggleComplete = { onToggleComplete(task) },
                    onClick = { onOpenTaskDetail(task) }
                )
            }
        }
    }
}

/**
 * Modern Executive Task Card
 * High contrast, left priority accent stripe, entity chips, progress indicator
 */
fun getTaskDeadlineBadge(dueDate: String, dueTime: String, isCompleted: Boolean): Triple<String, Color, Color>? {
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

fun getTaskDocCategoriesSummary(attachedJson: String): String {
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
            "${arr.length()} tệp đính kèm"
        }
    } catch (_: Exception) {
        ""
    }
}

fun getTaskNotesSummary(notesJson: String): Pair<Int, String> {
    if (notesJson.isBlank() || notesJson == "[]") return Pair(0, "")
    return try {
        val arr = JSONArray(notesJson)
        val count = arr.length()
        val latest = if (count > 0) {
            val lastObj = arr.getJSONObject(count - 1)
            lastObj.optString("content", "")
        } else ""
        Pair(count, latest)
    } catch (_: Exception) {
        Pair(0, "")
    }
}

@Composable
fun ExecutiveTaskCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val pColor = when (task.priority) {
        "P1" -> CoralUrgent
        "P2" -> NavyDeep
        "P3" -> EmeraldSuccess
        else -> Color(0xFF64748B)
    }

    val deadlineBadge = getTaskDeadlineBadge(task.dueDate, task.dueTime, task.isCompleted)
    val docSummary = getTaskDocCategoriesSummary(task.attachedFilesJson)
    val (noteCount, latestNoteSnippet) = getTaskNotesSummary(task.progressNotesJson)
    val displayNote = latestNoteSnippet.ifBlank { task.outputNotes }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(
            1.dp,
            if (task.priority == "P1" && !task.isCompleted) CoralUrgent.copy(alpha = 0.45f)
            else BorderLight
        ),
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left Priority Accent Bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (task.isCompleted) TextMuted else pColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Header Row: Priority Badge + Deadline / Urgency Pill (Placed on top so title is never squished)
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
                        if (deadlineBadge != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = deadlineBadge.third,
                                border = BorderStroke(0.6.dp, deadlineBadge.second.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    deadlineBadge.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = deadlineBadge.second,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.dueDate.isNotBlank() && deadlineBadge == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Text(
                                    "${task.dueDate}${if (task.dueTime.isNotBlank()) " " + task.dueTime else ""}",
                                    fontSize = 10.5.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (task.isCompleted) Color(0xFFEFF6FF) else Color(0xFFECFDF5),
                            border = BorderStroke(0.8.dp, if (task.isCompleted) Color(0xFFBFDBFE) else Color(0xFFA7F3D0)),
                            modifier = Modifier.clickable { onToggleComplete() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    if (task.isCompleted) Icons.Default.Undo else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) NavyDeep else EmeraldSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    if (task.isCompleted) "Khôi phục" else "Hoàn thành",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.isCompleted) NavyDeep else Color(0xFF065F46)
                                )
                            }
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
                        fontSize = 14.sp,
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Entity Chips: Người giao, Phụ trách, Tài liệu đính kèm, Hạn chót
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.assignerName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(12.dp))
                                Text(
                                    "Giao: ${task.assignerName.substringBefore('(')}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    if (task.responsiblePersonName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(12.dp))
                                Text(
                                    "Phụ trách: ${task.responsiblePersonName.substringBefore('(')}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDeep
                                )
                            }
                        }
                    }

                    if (task.collaborators.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(0.6.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                                Text(
                                    "Phối hợp: ${task.collaborators}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    if (task.location.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFDF4FF),
                            border = BorderStroke(0.6.dp, Color(0xFFF0ABFC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(12.dp))
                                Text(
                                    task.location,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        }
                    }

                    if (docSummary.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(12.dp))
                                Text(
                                    docSummary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyDeep
                                )
                            }
                        }
                    }

                    if (deadlineBadge == null && task.dueDate.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Text(
                                    "Hạn: ${task.dueDate} ${task.dueTime}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    if (noteCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                                Text(
                                    "$noteCount ghi chú",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    SleekStatusPill(status = task.status)
                }

                if (displayNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB).copy(alpha = 0.85f),
                        border = BorderStroke(0.6.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
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
                if (!task.isCompleted && task.progressPercent > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { task.progressPercent / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (task.progressPercent >= 80) EmeraldSuccess else NavyPrimary,
                            trackColor = Color(0xFFE2E8F0)
                        )
                        Text(
                            "${task.progressPercent}%",
                            fontSize = 10.sp,
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
fun KanbanViewClean(
    tasks: List<TaskEntity>,
    onToggleComplete: (TaskEntity) -> Unit,
    onOpenTaskDetail: (TaskEntity) -> Unit
) {
    val stages = listOf(
        Pair("TODO", "Chưa bắt đầu"),
        Pair("IN_PROGRESS", "Đang xử lý"),
        Pair("WAITING_FOR_OTHERS", "Chờ duyệt / Phối hợp"),
        Pair("COMPLETED", "Đã hoàn tất")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(stages) { (code, label) ->
            val stageTasks = tasks.filter { it.status == code }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val stageColor = when (code) {
                            "COMPLETED" -> EmeraldSuccess
                            "IN_PROGRESS" -> NavyPrimary
                            "WAITING_FOR_OTHERS" -> ChampagneGold
                            else -> TextSecondary
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(stageColor)
                        )
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyHeroDark)
                    }

                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF)) {
                        Text(
                            "${stageTasks.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (stageTasks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Không có nhiệm vụ ở giai đoạn này",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    stageTasks.forEach { task ->
                        ExecutiveTaskCard(
                            task = task,
                            onToggleComplete = { onToggleComplete(task) },
                            onClick = { onOpenTaskDetail(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaitingForViewClean(
    tasks: List<TaskEntity>,
    onOpenTaskDetail: (TaskEntity) -> Unit
) {
    if (tasks.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(26.dp))
                }
                Text("Không có việc nào đang chờ duyệt", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text(
                    "Tất cả các đầu việc đều đang được triển khai thông suốt.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenTaskDetail(task) },
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            SleekPriorityBadge(priority = task.priority)
                        }

                        if (task.waitingForWho.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(13.dp))
                                    Text(
                                        "Đang chờ: ${task.waitingForWho}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (task.assignerName.isNotBlank()) "Giao: ${task.assignerName}" else "",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                "Hạn: ${task.dueDate}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab Sổ tay Ghi chú & Nhật ký công tác
 * Displays both independent notes (NoteEntity) and progress notes linked to tasks.
 */
data class UnifiedNoteItem(
    val id: String,
    val title: String,
    val content: String,
    val timestampStr: String,
    val isTaskLinked: Boolean,
    val task: TaskEntity? = null,
    val noteEntity: NoteEntity? = null
)

@Composable
fun CadreNotesTabView(
    viewModel: CadreViewModel,
    tasks: List<TaskEntity>,
    notes: List<NoteEntity>,
    onOpenTask: (TaskEntity) -> Unit
) {
    var filterType by remember { mutableStateOf("ALL") } // ALL, INDEPENDENT, TASK_LINKED
    var noteSearchQuery by remember { mutableStateOf("") }
    var showCreateNoteModal by remember { mutableStateOf(false) }

    val unifiedNotes = remember(notes, tasks) {
        val list = mutableListOf<UnifiedNoteItem>()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        // 1. Independent notes
        notes.forEach { n ->
            list.add(
                UnifiedNoteItem(
                    id = n.id,
                    title = n.title,
                    content = n.content,
                    timestampStr = sdf.format(Date(n.updatedAt)),
                    isTaskLinked = false,
                    noteEntity = n
                )
            )
        }
        // 2. Task-linked notes
        tasks.forEach { t ->
            if (t.progressNotesJson.isNotBlank() && t.progressNotesJson != "[]") {
                try {
                    val arr = JSONArray(t.progressNotesJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            UnifiedNoteItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                title = "Nhiệm vụ: ${t.title}",
                                content = obj.optString("content", ""),
                                timestampStr = obj.optString("date", ""),
                                isTaskLinked = true,
                                task = t
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
        list
    }

    val filteredList = remember(unifiedNotes, filterType, noteSearchQuery) {
        unifiedNotes.filter { item ->
            val matchesFilter = when (filterType) {
                "INDEPENDENT" -> !item.isTaskLinked
                "TASK_LINKED" -> item.isTaskLinked
                else -> true
            }
            val matchesQuery = noteSearchQuery.isBlank() ||
                item.title.contains(noteSearchQuery, ignoreCase = true) ||
                item.content.contains(noteSearchQuery, ignoreCase = true) ||
                (item.task?.responsiblePersonName?.contains(noteSearchQuery, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Add Note bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = noteSearchQuery,
                onValueChange = { noteSearchQuery = it },
                placeholder = { Text("Tìm trong sổ tay & ghi chú nhiệm vụ...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("note_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyDeep,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Button(
                onClick = { showCreateNoteModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.testTag("add_note_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ghi chú", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterBadge(
                label = "Tất cả (${unifiedNotes.size})",
                isActive = filterType == "ALL",
                onClick = { filterType = "ALL" }
            )
            FilterBadge(
                label = "Ghi chú sổ tay (${unifiedNotes.count { !it.isTaskLinked }})",
                isActive = filterType == "INDEPENDENT",
                accentColor = NavyDeep,
                onClick = { filterType = "INDEPENDENT" }
            )
            FilterBadge(
                label = "Theo nhiệm vụ (${unifiedNotes.count { it.isTaskLinked }})",
                isActive = filterType == "TASK_LINKED",
                accentColor = Color(0xFFD97706),
                onClick = { filterType = "TASK_LINKED" }
            )
        }

        if (filteredList.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                    }
                    Text("Chưa có ghi chú nào", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Text(
                        "Bạn có thể tạo ghi chú riêng lẻ cho sổ tay công tác\nhoặc note tiến độ / ý kiến bổ sung theo từng nhiệm vụ cụ thể.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { showCreateNoteModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tạo ghi chú mới", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    UnifiedNoteCard(
                        item = item,
                        onOpenTask = { item.task?.let(onOpenTask) },
                        onDeleteNote = { item.noteEntity?.let { viewModel.deleteNote(it) } }
                    )
                }
            }
        }
    }

    if (showCreateNoteModal) {
        CreateNoteQuickDialog(
            tasks = tasks,
            onDismiss = { showCreateNoteModal = false },
            onSaveIndependentNote = { title, content ->
                viewModel.saveNote(title = title, content = content)
                showCreateNoteModal = false
            },
            onSaveTaskNote = { task, note ->
                viewModel.appendTaskNote(task, note)
                showCreateNoteModal = false
            }
        )
    }
}

@Composable
fun UnifiedNoteCard(
    item: UnifiedNoteItem,
    onOpenTask: () -> Unit,
    onDeleteNote: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = item.isTaskLinked, onClick = onOpenTask),
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, if (item.isTaskLinked) Color(0xFFFDE68A) else BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isTaskLinked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                            Text(
                                "Ghi chú nhiệm vụ",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.StickyNote2, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(12.dp))
                            Text(
                                "Sổ tay ghi chú",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        item.timestampStr,
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                    if (!item.isTaskLinked) {
                        IconButton(
                            onClick = onDeleteNote,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = if (item.isTaskLinked) NavyDeep else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                item.content,
                fontSize = 12.5.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (item.isTaskLinked && item.task != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.5.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Phụ trách: ${item.task.responsiblePersonName.ifBlank { "Chưa gán" }} • Hạn: ${item.task.dueDate}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Xem việc", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateNoteQuickDialog(
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onSaveIndependentNote: (String, String) -> Unit,
    onSaveTaskNote: (TaskEntity, String) -> Unit
) {
    var mode by remember { mutableStateOf("TASK_NOTE") } // TASK_NOTE, INDEPENDENT
    var selectedTask by remember(tasks) { mutableStateOf<TaskEntity?>(tasks.firstOrNull()) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thêm ghi chú công tác", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NavyHeroDark)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { mode = "TASK_NOTE" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (mode == "TASK_NOTE") NavyDeep else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (mode == "TASK_NOTE") NavyDeep else BorderLight)
                    ) {
                        Text(
                            "Theo nhiệm vụ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == "TASK_NOTE") Color.White else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { mode = "INDEPENDENT" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (mode == "INDEPENDENT") NavyDeep else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (mode == "INDEPENDENT") NavyDeep else BorderLight)
                    ) {
                        Text(
                            "Sổ tay riêng lẻ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == "INDEPENDENT") Color.White else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (mode == "TASK_NOTE") {
                    if (tasks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Chưa có nhiệm vụ nào trong hệ thống. Vui lòng chuyển sang 'Sổ tay riêng lẻ' hoặc tạo nhiệm vụ trước.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(10.dp)
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

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Chọn nhiệm vụ:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        selectedTask?.title ?: "Chọn nhiệm vụ...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                                }
                            }

                            if (isExpanded) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PureWhite,
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                ) {
                                    Column {
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
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                                        .padding(14.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Không tìm thấy nhiệm vụ nào", fontSize = 11.5.sp, color = TextSecondary)
                                                }
                                            } else {
                                                filteredTasksForNote.forEach { t ->
                                                    val isCur = t.id == selectedTask?.id
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedTask = t
                                                                isExpanded = false
                                                            }
                                                            .background(if (isCur) Color(0xFFEFF6FF) else PureWhite)
                                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(t.title, fontSize = 12.sp, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text("${t.responsiblePersonName.ifBlank { "Cán bộ" }} • Hạn ${t.dueDate}", fontSize = 10.sp, color = TextSecondary)
                                                        }
                                                        if (isCur) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(15.dp))
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
                            value = noteContent,
                            onValueChange = { noteContent = it },
                            label = { Text("Nội dung ghi chú / chỉ đạo bổ sung", fontSize = 12.sp) },
                            placeholder = { Text("Nhập nội dung...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4
                        )

                        Button(
                            onClick = {
                                selectedTask?.let { onSaveTaskNote(it, noteContent) }
                            },
                            enabled = selectedTask != null && noteContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lưu vào nhiệm vụ", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Tiêu đề", fontSize = 12.sp) },
                        placeholder = { Text("Ví dụ: Sổ tay tuần, Ý kiến trao đổi...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Nội dung chi tiết", fontSize = 12.sp) },
                        placeholder = { Text("Nhập nội dung...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )

                    Button(
                        onClick = {
                            val t = noteTitle.ifBlank { noteContent.take(40).trim() }
                            onSaveIndependentNote(t, noteContent)
                        },
                        enabled = noteContent.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lưu vào Sổ tay", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CadreSettlementsTabView(
    viewModel: CadreViewModel,
    tasks: List<TaskEntity>,
    settlements: List<SettlementEntity>,
    onOpenTask: (TaskEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("ALL") } // ALL, PENDING, URGENT, COMPLETED
    var showCreateSettlementModal by remember { mutableStateOf(false) }

    fun calculateDaysDiff(deadlineStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val target = sdf.parse(deadlineStr.trim()) ?: return 999L
            val calTarget = Calendar.getInstance().apply {
                time = target
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            val calNow = Calendar.getInstance()
            val diffMs = calTarget.timeInMillis - calNow.timeInMillis
            diffMs / (1000 * 60 * 60 * 24)
        } catch (e: Exception) {
            999L
        }
    }

    val totalCount = settlements.size
    val pendingSettlements = settlements.filter { !it.isCompleted }
    val pendingCount = pendingSettlements.size
    val pendingAmount = pendingSettlements.sumOf { it.amount }
    val overdueCount = pendingSettlements.count { calculateDaysDiff(it.deadline) < 0 }
    val urgentCount = pendingSettlements.count { calculateDaysDiff(it.deadline) in 0..3 }
    val completedCount = settlements.count { it.isCompleted }
    val completedAmount = settlements.filter { it.isCompleted }.sumOf { it.amount }

    val filteredSettlements = remember(settlements, searchQuery, filterTab) {
        settlements.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                s.title.contains(searchQuery, ignoreCase = true) ||
                s.budgetEstimateCode.contains(searchQuery, ignoreCase = true) ||
                s.responsiblePerson.contains(searchQuery, ignoreCase = true) ||
                s.taskTitle.contains(searchQuery, ignoreCase = true)

            val days = calculateDaysDiff(s.deadline)
            val matchesFilter = when (filterTab) {
                "PENDING" -> !s.isCompleted
                "URGENT" -> !s.isCompleted && (days <= 3)
                "COMPLETED" -> s.isCompleted
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedWith(
            compareBy<SettlementEntity> { it.isCompleted }
                .thenBy { calculateDaysDiff(it.deadline) }
        )
    }

    val moneyFormat = remember { DecimalFormat("#,### đ") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // KPI Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Cần quyết toán
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, Color(0xFFFEF3C7))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Cần quyết toán", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        "$pendingCount mục",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFB45309)
                    )
                    Text(
                        moneyFormat.format(pendingAmount),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 2: Quá hạn / Gấp
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, if (overdueCount > 0) Color(0xFFFECACA) else BorderLight)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Quá hạn / Gấp", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        "${overdueCount + urgentCount} mục",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (overdueCount > 0) CoralUrgent else if (urgentCount > 0) Color(0xFFD97706) else TextPrimary
                    )
                    Text(
                        if (overdueCount > 0) "⚠️ $overdueCount quá hạn" else if (urgentCount > 0) "⚡ $urgentCount gấp" else "✓ Đúng hạn",
                        fontSize = 10.5.sp,
                        color = if (overdueCount > 0) CoralUrgent else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Card 3: Đã hoàn tất
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, Color(0xFFD1FAE5))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Đã quyết toán", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        "$completedCount mục",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldSuccess
                    )
                    Text(
                        moneyFormat.format(completedAmount),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Search Bar & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm nội dung, dự trù số, cán bộ...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyDeep,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                ),
                singleLine = true,
                modifier = Modifier.weight(1f).height(46.dp)
            )

            Button(
                onClick = { showCreateSettlementModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quyết toán", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterBadge(
                label = "Tất cả (${settlements.size})",
                isActive = filterTab == "ALL",
                onClick = { filterTab = "ALL" }
            )
            FilterBadge(
                label = "Cần quyết toán ($pendingCount)",
                isActive = filterTab == "PENDING",
                accentColor = Color(0xFFD97706),
                onClick = { filterTab = "PENDING" }
            )
            FilterBadge(
                label = "Quá hạn / Gấp (${overdueCount + urgentCount})",
                isActive = filterTab == "URGENT",
                accentColor = CoralUrgent,
                onClick = { filterTab = "URGENT" }
            )
            FilterBadge(
                label = "Đã quyết toán ($completedCount)",
                isActive = filterTab == "COMPLETED",
                accentColor = EmeraldSuccess,
                onClick = { filterTab = "COMPLETED" }
            )
        }

        // Settlements List
        if (filteredSettlements.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                    }
                    Text(
                        if (searchQuery.isNotBlank()) "Không tìm thấy quyết toán phù hợp" else "Chưa có mục quyết toán kinh phí nào",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (searchQuery.isNotBlank()) "Thử nhập từ khóa khác để tìm kiếm." else "Ghi chép các khoản dự trù cần quyết toán, theo dõi thời hạn và chứng từ thanh quyết toán để tránh trễ hạn quy định.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 17.sp
                    )
                    Button(
                        onClick = { showCreateSettlementModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhập quyết toán ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredSettlements, key = { it.id }) { item ->
                    val diff = calculateDaysDiff(item.deadline)
                    SettlementCard(
                        settlement = item,
                        daysDiff = diff,
                        moneyFormat = moneyFormat,
                        onToggleComplete = { viewModel.toggleSettlementCompletion(item) },
                        onDelete = { viewModel.deleteSettlement(item) },
                        onOpenTask = {
                            val linkedTask = tasks.find { it.id == item.taskId }
                            if (linkedTask != null) {
                                onOpenTask(linkedTask)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreateSettlementModal) {
        CreateSettlementDialog(
            tasks = tasks,
            onDismiss = { showCreateSettlementModal = false },
            onSave = { title, amount, budgetCode, deadline, taskId, taskTitle, resp ->
                viewModel.saveSettlement(
                    title = title,
                    amount = amount,
                    budgetEstimateCode = budgetCode,
                    deadline = deadline,
                    taskId = taskId,
                    taskTitle = taskTitle,
                    responsiblePerson = resp
                )
                showCreateSettlementModal = false
            }
        )
    }
}

@Composable
fun SettlementCard(
    settlement: SettlementEntity,
    daysDiff: Long,
    moneyFormat: DecimalFormat,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onOpenTask: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(
            1.dp,
            if (settlement.isCompleted) Color(0xFFD1FAE5)
            else if (daysDiff < 0) Color(0xFFFECACA)
            else if (daysDiff <= 3) Color(0xFFFDE68A)
            else BorderLight
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Checkbox, Title, Countdown Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Checkbox toggle
                Surface(
                    shape = CircleShape,
                    color = if (settlement.isCompleted) EmeraldSuccess else Color(0xFFF1F5F9),
                    border = BorderStroke(1.5.dp, if (settlement.isCompleted) EmeraldSuccess else Color(0xFF94A3B8)),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleComplete)
                ) {
                    if (settlement.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Hoàn tất",
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        settlement.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (settlement.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (settlement.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                // Countdown badge
                SettlementDeadlineBadge(daysDiff = daysDiff, isCompleted = settlement.isCompleted)
            }

            // Financial & Estimate details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Amount
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = if (settlement.isCompleted) TextMuted else Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    Text(
                        moneyFormat.format(settlement.amount),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.5.sp,
                        color = if (settlement.isCompleted) TextSecondary else Color(0xFFB45309)
                    )
                }

                // Budget Estimate code
                if (settlement.budgetEstimateCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(11.dp))
                            Text(
                                "DT: ${settlement.budgetEstimateCode}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

            // Bottom metadata row: Deadline date, Officer, Linked Task, Delete action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deadline
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                        Text(settlement.deadline, fontSize = 11.sp, color = TextSecondary)
                    }

                    // Responsible Person
                    if (settlement.responsiblePerson.isNotBlank()) {
                        Text("•", fontSize = 10.sp, color = TextMuted)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(12.dp))
                            Text(
                                settlement.responsiblePerson,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa quyết toán", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Linked Task Pill (if applicable)
            if (settlement.taskId.isNotBlank() && settlement.taskTitle.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenTask)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(13.dp))
                        Text(
                            "Nhiệm vụ: ${settlement.taskTitle}",
                            fontSize = 11.sp,
                            color = NavyDeep,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementDeadlineBadge(daysDiff: Long, isCompleted: Boolean) {
    if (isCompleted) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFD1FAE5),
            border = BorderStroke(0.6.dp, Color(0xFFA7F3D0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(11.dp))
                Text("Đã quyết toán", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
            }
        }
    } else if (daysDiff < 0) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEE2E2),
            border = BorderStroke(0.6.dp, Color(0xFFFECACA))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(11.dp))
                Text("Quá hạn ${-daysDiff} ngày", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = CoralUrgent)
            }
        }
    } else if (daysDiff == 0L) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                Text("Hạn hôm nay", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
            }
        }
    } else if (daysDiff in 1..3) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFFBEB),
            border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                Text("Còn $daysDiff ngày (Gấp)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                Text("Còn $daysDiff ngày", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            }
        }
    }
}

@Composable
fun CreateSettlementDialog(
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, budgetCode: String, deadline: String, taskId: String, taskTitle: String, resp: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var budgetCode by remember { mutableStateOf("") }
    var deadline by remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 14)
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time))
    }
    var responsiblePerson by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var isTaskDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Thêm mục quyết toán", fontWeight = FontWeight.Black, fontSize = 15.sp, color = NavyHeroDark)
                            Text("Kinh phí dự trù & thời hạn quyết toán", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderLight, thickness = 0.8.dp)

                // 1. Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nội dung quyết toán *", fontSize = 12.sp) },
                    placeholder = { Text("Ví dụ: Kinh phí in tài liệu Đại hội...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDeep,
                        unfocusedBorderColor = BorderLight
                    ),
                    singleLine = false,
                    maxLines = 2
                )

                // 2. Amount
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                        label = { Text("Số tiền (VNĐ) *", fontSize = 12.sp) },
                        placeholder = { Text("VD: 5000000", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = { Text("đ", fontWeight = FontWeight.Bold, color = NavyDeep, modifier = Modifier.padding(end = 12.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDeep,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    val parsedAmt = amountText.toLongOrNull() ?: 0L
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
                                modifier = Modifier.clickable { amountText = (parsedAmt + addVal).toString() }
                            ) {
                                Text(label, fontSize = 11.sp, color = NavyDeep, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // 3. Budget Estimate Code & Deadline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = budgetCode,
                        onValueChange = { budgetCode = it },
                        label = { Text("Dự trù số *", fontSize = 11.sp) },
                        placeholder = { Text("DT-04/ĐTN", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDeep,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Hạn quyết toán *", fontSize = 11.sp) },
                        placeholder = { Text("dd/MM/yyyy", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDeep,
                            unfocusedBorderColor = BorderLight
                        )
                    )
                }

                // Quick Date Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                    listOf("+7 ngày" to 7, "+14 ngày" to 14, "+30 ngày" to 30).forEach { (lbl, d) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                val c = Calendar.getInstance()
                                c.add(Calendar.DAY_OF_YEAR, d)
                                deadline = sdf.format(c.time)
                            }
                        ) {
                            Text(lbl, fontSize = 10.5.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // 4. Responsible Person
                OutlinedTextField(
                    value = responsiblePerson,
                    onValueChange = { responsiblePerson = it },
                    label = { Text("Cán bộ tạm ứng / Chịu trách nhiệm", fontSize = 12.sp) },
                    placeholder = { Text("Họ tên cán bộ...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDeep,
                        unfocusedBorderColor = BorderLight
                    )
                )

                // 5. Linked Task Dropdown
                if (tasks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gắn với nhiệm vụ trọng tâm (không bắt buộc):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isTaskDropdownExpanded = !isTaskDropdownExpanded }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    selectedTask?.title ?: "— Không gắn nhiệm vụ cụ thể —",
                                    fontSize = 11.5.sp,
                                    color = if (selectedTask != null) TextPrimary else TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(if (isTaskDropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (isTaskDropdownExpanded) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PureWhite,
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp)
                            ) {
                                LazyColumn {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedTask = null
                                                    isTaskDropdownExpanded = false
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
                                                    selectedTask = t
                                                    if (responsiblePerson.isBlank() && t.responsiblePersonName.isNotBlank()) {
                                                        responsiblePerson = t.responsiblePersonName
                                                    }
                                                    isTaskDropdownExpanded = false
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

                // Action Buttons
                val canSubmit = title.isNotBlank() && amountText.isNotBlank()
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

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val amt = amountText.toLongOrNull() ?: 0L
                                onSave(
                                    title.trim(),
                                    amt,
                                    budgetCode.trim(),
                                    deadline.trim(),
                                    selectedTask?.id ?: "",
                                    selectedTask?.title ?: "",
                                    responsiblePerson.trim()
                                )
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Lưu quyết toán", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

