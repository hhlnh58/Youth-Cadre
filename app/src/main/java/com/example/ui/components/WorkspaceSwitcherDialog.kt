package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WorkspaceEntity
import com.example.ui.theme.*

@Composable
fun WorkspaceSwitcherDialog(
    workspaces: List<WorkspaceEntity>,
    currentWorkspaceId: String,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: (name: String, org: String, desc: String, colorHex: String, ownerName: String) -> Unit,
    onUpdateWorkspace: (WorkspaceEntity) -> Unit = {},
    onDeleteWorkspace: (WorkspaceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreating by remember { mutableStateOf(false) }
    var editingWorkspace by remember { mutableStateOf<WorkspaceEntity?>(null) }
    var newWsName by remember { mutableStateOf("") }
    var newWsOrg by remember { mutableStateOf("Đoàn TNCS Hồ Chí Minh") }
    var newWsDesc by remember { mutableStateOf("") }
    var newWsOwner by remember { mutableStateOf("") }
    var newWsColor by remember { mutableStateOf("#1B1464") }
    var deleteCandidate by remember { mutableStateOf<WorkspaceEntity?>(null) }

    val colorOptions = listOf(
        Pair("#1B1464", "Navy Đoàn"),
        Pair("#2563EB", "Cobalt Hiện đại"),
        Pair("#059669", "Emerald Tuổi trẻ"),
        Pair("#DC2626", "Crimson Tiên phong"),
        Pair("#D97706", "Gold Vinh quang"),
        Pair("#0F172A", "Obsidian Uy nghiêm")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .testTag("workspace_switcher_dialog"),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        ModernYouthCadreLogo(size = 36.dp)
                        Column {
                            Text(
                                "Không gian Điều hành",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NavyHeroDark
                            )
                            Text(
                                "Chuyển đổi hoặc quản lý đơn vị tác nghiệp",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }

                if (!isCreating) {
                    // Option: Tất cả đơn vị
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (currentWorkspaceId.isBlank()) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (currentWorkspaceId.isBlank()) NavyDeep else BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectWorkspace("")
                                onDismiss()
                            }
                            .testTag("workspace_all_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (currentWorkspaceId.isBlank()) NavyDeep else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (currentWorkspaceId.isBlank()) Color.White else NavyDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Tất cả đơn vị",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    "Xem tổng hợp toàn bộ nhiệm vụ và lịch trình",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            if (currentWorkspaceId.isBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Workspaces List
                    Text(
                        "Danh sách đơn vị cơ sở (${workspaces.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    if (workspaces.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Chưa có Không gian nào được tạo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Bấm \"+ Tạo Không gian mới\" để bắt đầu", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(workspaces) { ws ->
                                val isSelected = currentWorkspaceId == ws.id
                                val wsColor = try {
                                    Color(android.graphics.Color.parseColor(ws.colorHex))
                                } catch (_: Exception) {
                                    NavyDeep
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else PureWhite,
                                    border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectWorkspace(ws.id)
                                            onDismiss()
                                        }
                                        .testTag("workspace_item_${ws.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(wsColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                ws.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ws.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                ws.organization.ifBlank { "Đoàn TNCS Hồ Chí Minh" },
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Đang chọn",
                                                tint = NavyDeep,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                editingWorkspace = ws
                                                newWsName = ws.name
                                                newWsOrg = ws.organization
                                                newWsOwner = ws.ownerName
                                                newWsDesc = ws.description
                                                newWsColor = ws.colorHex.ifBlank { "#1B1464" }
                                                isCreating = true
                                            },
                                            modifier = Modifier.size(28.dp).testTag("edit_workspace_${ws.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = NavyDeep, modifier = Modifier.size(16.dp))
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = { deleteCandidate = ws },
                                            modifier = Modifier.size(28.dp).testTag("delete_workspace_${ws.id}")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = CoralUrgent, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Button: Create Workspace
                    Button(
                        onClick = {
                            editingWorkspace = null
                            newWsName = ""
                            newWsOrg = "Đoàn TNCS Hồ Chí Minh"
                            newWsDesc = ""
                            newWsOwner = ""
                            newWsColor = "#1B1464"
                            isCreating = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_create_workspace_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tạo Không gian làm việc mới", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    // Create / Edit Form
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isCreating = false
                                    editingWorkspace = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = NavyDeep)
                            }
                            Text(
                                if (editingWorkspace != null) "Chỉnh sửa Không gian" else "Tạo Không gian mới",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NavyHeroDark
                            )
                        }

                        OutlinedTextField(
                            value = newWsName,
                            onValueChange = { newWsName = it },
                            label = { Text("Tên Không gian / Đơn vị *") },
                            placeholder = { Text("VD: Ban Thường vụ Quận Đoàn") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_ws_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = newWsOrg,
                            onValueChange = { newWsOrg = it },
                            label = { Text("Cơ quan chủ quản") },
                            placeholder = { Text("VD: Đoàn TNCS Hồ Chí Minh") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = newWsOwner,
                            onValueChange = { newWsOwner = it },
                            label = { Text("Cán bộ phụ trách") },
                            placeholder = { Text("VD: Đ/c Bí thư") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = newWsDesc,
                            onValueChange = { newWsDesc = it },
                            label = { Text("Mô tả chức năng nhiệm vụ") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Color picker
                        Text("Màu nhận diện", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            colorOptions.forEach { (hex, _) ->
                                val isSel = newWsColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSel) 2.5.dp else 1.dp,
                                            color = if (isSel) Color.Black else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { newWsColor = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isCreating = false
                                    editingWorkspace = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Hủy", color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (newWsName.isNotBlank()) {
                                        if (editingWorkspace != null) {
                                            onUpdateWorkspace(
                                                editingWorkspace!!.copy(
                                                    name = newWsName.trim(),
                                                    organization = newWsOrg.trim(),
                                                    description = newWsDesc.trim(),
                                                    colorHex = newWsColor,
                                                    ownerName = newWsOwner.trim()
                                                )
                                            )
                                        } else {
                                            onCreateWorkspace(
                                                newWsName.trim(),
                                                newWsOrg.trim(),
                                                newWsDesc.trim(),
                                                newWsColor,
                                                newWsOwner.trim()
                                            )
                                        }
                                        isCreating = false
                                        editingWorkspace = null
                                        onDismiss()
                                    }
                                },
                                enabled = newWsName.isNotBlank(),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("submit_workspace_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
                            ) {
                                Text(
                                    if (editingWorkspace != null) "Lưu Cập Nhật" else "Tạo Ngay",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = {
                Text("Xác nhận xóa Không gian?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Đồng chí có chắc chắn muốn xóa không gian \"${deleteCandidate?.name}\"? Toàn bộ dữ liệu của đơn vị này sẽ được giải phóng.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate?.let { onDeleteWorkspace(it) }
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralUrgent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Xác nhận xóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = PureWhite
        )
    }
}
