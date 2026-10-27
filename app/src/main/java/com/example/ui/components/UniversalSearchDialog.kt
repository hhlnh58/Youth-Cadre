package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TaskEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CadreViewModel

@Composable
fun UniversalSearchDialog(
    viewModel: CadreViewModel,
    onDismiss: () -> Unit,
    onSelectTask: (TaskEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val tasks by viewModel.tasks.collectAsState()
    val people by viewModel.people.collectAsState()
    val events by viewModel.calendarEvents.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val matchedTasks = if (query.isBlank()) emptyList() else tasks.filter {
        it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) || it.assignerName.contains(query, ignoreCase = true)
    }

    val matchedPeople = if (query.isBlank()) emptyList() else people.filter {
        it.fullName.contains(query, ignoreCase = true) || it.position.contains(query, ignoreCase = true) || it.unit.contains(query, ignoreCase = true)
    }

    val matchedEvents = if (query.isBlank()) emptyList() else events.filter {
        it.title.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true) || it.destinationUnit.contains(query, ignoreCase = true)
    }

    val matchedDocs = if (query.isBlank()) emptyList() else documents.filter {
        it.title.contains(query, ignoreCase = true) || it.summary.contains(query, ignoreCase = true) || it.docNumber.contains(query, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Search Input Header
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Tìm kiếm công việc, lãnh đạo, tài liệu, lịch...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue) },
                    trailingIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextSecondary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("universal_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (query.isBlank()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ManageSearch, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(48.dp))
                            Text("Nhập từ khóa để tìm kiếm trên toàn hệ thống", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tasks matches
                        if (matchedTasks.isNotEmpty()) {
                            item {
                                Text("Nhiệm vụ (${matchedTasks.size})", fontWeight = FontWeight.Bold, color = ElectricBlue, fontSize = 13.sp)
                            }
                            items(matchedTasks) { t ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectTask(t); onDismiss() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Checklist, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(t.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Hạn: ${t.dueDate} ${t.dueTime} • Giao: ${t.assignerName}", color = TextSecondary, fontSize = 11.sp)
                                        }
                                        PriorityBadge(priority = t.priority)
                                    }
                                }
                            }
                        }

                        // People matches
                        if (matchedPeople.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Nhân sự (${matchedPeople.size})", fontWeight = FontWeight.Bold, color = ChampagneGold, fontSize = 13.sp)
                            }
                            items(matchedPeople) { p ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(p.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${p.position} • ${p.unit}", color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Event matches
                        if (matchedEvents.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Lịch công tác (${matchedEvents.size})", fontWeight = FontWeight.Bold, color = ElectricBlue, fontSize = 13.sp)
                            }
                            items(matchedEvents) { ev ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ev.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${ev.date} ${ev.startTime} • ${ev.location}", color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Document matches
                        if (matchedDocs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Hồ sơ & Văn bản (${matchedDocs.size})", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 13.sp)
                            }
                            items(matchedDocs) { d ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(d.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${d.docType} • ${d.versionName}", color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
