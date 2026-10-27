package com.example.ui.screens.cadreai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.ModernYouthCadreLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.CadreViewModel
import org.json.JSONObject

@Composable
fun CadreAiScreen(
    viewModel: CadreViewModel
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val currentMode by viewModel.currentAiMode.collectAsState()
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }

    val quickStarters = listOf(
        "Soạn tờ trình xin kinh phí Tháng Thanh niên 2026",
        "Tra cứu Điều lệ Đoàn về nhân sự Ban Chấp hành",
        "Tạo lịch họp Ban Thường vụ tuần tới lúc 8:30",
        "Rà soát việc nào đang chậm tiến độ hoặc chờ duyệt",
        "Tham mưu kế hoạch ra quân Ngày thứ Bảy tình nguyện"
    )

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cadre AI", fontWeight = FontWeight.Black, color = NavyHeroDark, fontSize = 18.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x1810B981),
                            border = BorderStroke(0.6.dp, Color(0x4010B981))
                        ) {
                            Text(
                                "Gemini 3.8",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    Text("Cố vấn chiến lược & Điều phối kho dữ liệu Đoàn", fontSize = 11.sp, color = NavyDeep)
                }
            }

            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Xóa hội thoại", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CleanAiModePill("Cố vấn chung", "ASSISTANT", currentMode == "ASSISTANT") { viewModel.currentAiMode.value = "ASSISTANT" }
            CleanAiModePill("Tham mưu đề xuất", "DRAFTING", currentMode == "DRAFTING") { viewModel.currentAiMode.value = "DRAFTING" }
            CleanAiModePill("Tạo & Xếp lịch", "PLANNER", currentMode == "PLANNER") { viewModel.currentAiMode.value = "PLANNER" }
            CleanAiModePill("Tra cứu kho tài liệu", "REVIEW", currentMode == "REVIEW") { viewModel.currentAiMode.value = "REVIEW" }
            CleanAiModePill("Báo cáo tiến độ", "EXECUTIVE_BRIEF", currentMode == "EXECUTIVE_BRIEF") { viewModel.currentAiMode.value = "EXECUTIVE_BRIEF" }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                CleanMessageBubble(
                    message = msg,
                    onApplySchedule = { title, date, time, location, host, notes ->
                        viewModel.applyScheduleFromAi(title, date, time, location, host, notes)
                    },
                    onApplyTask = { title, assignedBy, assignee, coordinator, deadline, priority, desc ->
                        viewModel.applyTaskFromAi(title, assignedBy, assignee, coordinator, deadline, priority, desc)
                    },
                    onApplyAdvisory = { title, targetOrg, basis, content ->
                        viewModel.applyAdvisoryFromAi(title, targetOrg, basis, content)
                    }
                )
            }

            if (isThinking) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ElectricBlue,
                                strokeWidth = 2.dp
                            )
                            Text("Cadre AI đang rà soát dữ liệu và phân tích...", color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Quick Starters Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickStarters.forEach { prompt ->
                Surface(
                    modifier = Modifier.clickable { viewModel.sendAiPrompt(prompt) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        prompt,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Clean White Input Bar with Deep Navy Send Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 88.dp, top = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text("Hỏi về văn bản, tạo lịch, đề xuất tham mưu...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DeepNavyButtonGradient)
                        .clickable {
                            if (inputPrompt.isNotBlank()) {
                                val text = inputPrompt.trim()
                                inputPrompt = ""
                                viewModel.sendAiPrompt(text)
                            }
                        }
                        .testTag("send_ai_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Gửi", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CleanMessageBubble(
    message: ChatMessageEntity,
    onApplySchedule: (title: String, date: String, time: String, location: String, host: String, notes: String) -> Unit,
    onApplyTask: (title: String, assignedBy: String, assignee: String, coordinator: String, deadline: String, priority: String, desc: String) -> Unit,
    onApplyAdvisory: (title: String, targetOrg: String, basis: String, content: String) -> Unit
) {
    val isUser = message.role == "USER"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) NavyDeep else PureWhite,
            border = if (isUser) BorderStroke(1.dp, NavyDeep) else BorderStroke(1.dp, BorderLight),
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModernYouthCadreLogo(size = 18.dp)
                        Text("Cadre AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                    }
                }

                Text(
                    text = message.message,
                    color = if (isUser) Color.White else TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Render Action Cards if AI proposed schedules, tasks, or advisories
                if (!isUser && message.actionSuggestedJson.isNotBlank()) {
                    val actionsJson = remember(message.actionSuggestedJson) {
                        try {
                            JSONObject(message.actionSuggestedJson)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (actionsJson != null) {
                        // Schedules
                        val schedules = actionsJson.optJSONArray("schedules")
                        if (schedules != null && schedules.length() > 0) {
                            for (i in 0 until schedules.length()) {
                                val s = schedules.getJSONObject(i)
                                val title = s.optString("title", "Cuộc họp mới")
                                val date = s.optString("date", "28/10/2026")
                                val time = s.optString("time", "08:30")
                                val loc = s.optString("location", "Phòng họp")
                                val host = s.optString("host", "Ban Thường vụ")
                                val notes = s.optString("notes", "")

                                var isScheduled by remember { mutableStateOf(false) }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x102563EB),
                                    border = BorderStroke(0.8.dp, Color(0x352563EB)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        }
                                        Text("📅 $date lúc $time | 📍 $loc", fontSize = 11.sp, color = TextSecondary)
                                        if (host.isNotBlank()) {
                                            Text("Chủ trì: $host", fontSize = 11.sp, color = TextMuted)
                                        }

                                        Button(
                                            onClick = {
                                                onApplySchedule(title, date, time, loc, host, notes)
                                                isScheduled = true
                                            },
                                            enabled = !isScheduled,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isScheduled) EmeraldSuccess else ElectricBlue,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().height(34.dp)
                                        ) {
                                            Icon(if (isScheduled) Icons.Default.Check else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isScheduled) "✓ Đã thêm vào lịch công tác" else "Lên lịch công tác ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Tasks
                        val taskList = actionsJson.optJSONArray("tasks")
                        if (taskList != null && taskList.length() > 0) {
                            for (i in 0 until taskList.length()) {
                                val t = taskList.getJSONObject(i)
                                val title = t.optString("title", "Nhiệm vụ")
                                val assignedBy = t.optString("assignedBy", "Lãnh đạo")
                                val assignee = t.optString("assignee", "Cán bộ chuyên trách")
                                val coordinator = t.optString("coordinator", "")
                                val deadline = t.optString("deadline", "30/10/2026")
                                val priority = t.optString("priority", "Cao")
                                val desc = t.optString("description", "")

                                var isAdded by remember { mutableStateOf(false) }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x1010B981),
                                    border = BorderStroke(0.8.dp, Color(0x3510B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Checklist, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        }
                                        Text("👤 Phụ trách: $assignee | Hạn: $deadline", fontSize = 11.sp, color = TextSecondary)

                                        Button(
                                            onClick = {
                                                onApplyTask(title, assignedBy, assignee, coordinator, deadline, priority, desc)
                                                isAdded = true
                                            },
                                            enabled = !isAdded,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAdded) EmeraldSuccess else Color(0xFF059669),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().height(34.dp)
                                        ) {
                                            Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAdded) "✓ Đã lưu vào nhiệm vụ" else "Tạo nhiệm vụ này", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Advisories
                        val advisories = actionsJson.optJSONArray("advisories")
                        if (advisories != null && advisories.length() > 0) {
                            for (i in 0 until advisories.length()) {
                                val adv = advisories.getJSONObject(i)
                                val title = adv.optString("title", "Tờ trình tham mưu")
                                val target = adv.optString("targetOrg", "Thường trực Cấp ủy")
                                val basis = adv.optString("basis", "")
                                val content = adv.optString("content", "")

                                var isSaved by remember { mutableStateOf(false) }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x108B5CF6),
                                    border = BorderStroke(0.8.dp, Color(0x358B5CF6)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        }
                                        Text("Kính gửi: $target", fontSize = 11.sp, color = TextSecondary)

                                        Button(
                                            onClick = {
                                                onApplyAdvisory(title, target, basis, content)
                                                isSaved = true
                                            },
                                            enabled = !isSaved,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSaved) EmeraldSuccess else Color(0xFF7C3AED),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth().height(34.dp)
                                        ) {
                                            Icon(if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isSaved) "✓ Đã lưu bản tham mưu" else "Lưu vào kho tham mưu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun CleanAiModePill(
    label: String,
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NavyDeep else PureWhite,
        border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight),
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}
