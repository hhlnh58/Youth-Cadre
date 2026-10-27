package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun WorkspaceOnboardingView(
    onCreateWorkspace: (name: String, org: String, desc: String, colorHex: String, ownerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var wsName by remember { mutableStateOf("") }
    var wsOrg by remember { mutableStateOf("Đoàn TNCS Hồ Chí Minh") }
    var wsCadreLevel by remember { mutableStateOf("Đoàn cơ sở") }
    var wsOwner by remember { mutableStateOf("") }
    var wsDesc by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#1B1464") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetTemplates = listOf(
        Triple("Ban Thường vụ Đoàn", "Chỉ đạo và điều hành công tác Đoàn và phong trào thanh thiếu nhi", "#1B1464"),
        Triple("Chi đoàn Cơ quan", "Công tác chuyên môn và hoạt động xung kích chi đoàn", "#2563EB"),
        Triple("Đoàn Trường / Sinh viên", "Phong trào học sinh - sinh viên và hoạt động tình nguyện hè", "#059669"),
        Triple("Ban Tuyên giáo & Chuyên môn", "Công tác tuyên truyền, giáo dục chính trị tư tưởng đoàn viên", "#D97706"),
        Triple("Đoàn Doanh nghiệp", "Xung kích lao động sáng tạo, phát triển kinh tế", "#DC2626")
    )

    val colorOptions = listOf(
        Pair("#1B1464", "Navy Truyền thống"),
        Pair("#2563EB", "Cobalt Hiện đại"),
        Pair("#059669", "Emerald Tuổi trẻ"),
        Pair("#DC2626", "Crimson Tiên phong"),
        Pair("#D97706", "Gold Vinh quang"),
        Pair("#0F172A", "Obsidian Uy nghiêm")
    )

    val levels = listOf("Chi đoàn", "Đoàn cơ sở", "Ban Chuyên môn", "Quận / Huyện Đoàn", "Tỉnh / Thành Đoàn")

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Emblem & Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = NavyDeep,
                border = BorderStroke(1.2.dp, Color(0x3538BDF8)),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .background(DeepNavyGradient)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ModernYouthCadreLogo(size = 76.dp)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "HỆ THỐNG ĐIỀU HÀNH TÁC NGHIỆP CÁN BỘ ĐOÀN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = Color(0xFFA5B4FC),
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Khởi tạo Không gian Làm việc",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Chào mừng Đồng chí! Vui lòng thiết lập Không gian làm việc (Workspace) để bắt đầu quản trị toàn bộ nhiệm vụ, lịch họp và kho tri thức.",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Presets Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp))
                        Text(
                            "Chọn mẫu gợi ý nhanh cho đơn vị",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NavyHeroDark
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetTemplates.forEach { (name, desc, color) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (wsName == name) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (wsName == name) NavyDeep else BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        wsName = name
                                        wsDesc = desc
                                        selectedColorHex = color
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(color)))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text(desc, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                    }
                                    if (wsName == name) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Form Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Thông tin Không gian làm việc",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyHeroDark
                    )

                    // 1. Tên Workspace
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tên Không gian làm việc *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        OutlinedTextField(
                            value = wsName,
                            onValueChange = {
                                wsName = it
                                errorMessage = null
                            },
                            placeholder = { Text("VD: Ban Thường vụ Huyện Đoàn Cần Giờ", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_ws_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyDeep,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }

                    // 2. Cơ quan chủ quản
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cơ quan / Tổ chức quản lý *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        OutlinedTextField(
                            value = wsOrg,
                            onValueChange = { wsOrg = it },
                            placeholder = { Text("VD: Đoàn TNCS Hồ Chí Minh", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyDeep,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }

                    // 3. Cấp bộ Đoàn
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cấp bộ Đoàn", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            levels.take(3).forEach { lvl ->
                                val isSel = wsCadreLevel == lvl
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) NavyDeep else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { wsCadreLevel = lvl }
                                ) {
                                    Text(
                                        lvl,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Cán bộ phụ trách
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Họ tên & Chức vụ Cán bộ điều hành", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        OutlinedTextField(
                            value = wsOwner,
                            onValueChange = { wsOwner = it },
                            placeholder = { Text("VD: Đ/c Nguyễn Văn A - Bí thư Đoàn cơ sở", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyDeep,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }

                    // 5. Màu nhận diện
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Màu sắc nhận diện đơn vị", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorOptions.forEach { (hex, _) ->
                                val isSel = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSel) 3.dp else 1.dp,
                                            color = if (isSel) Color.Black else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 6. Mô tả nhiệm vụ
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mô tả phương châm / nhiệm vụ trọng tâm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        OutlinedTextField(
                            value = wsDesc,
                            onValueChange = { wsDesc = it },
                            placeholder = { Text("VD: Chỉ đạo phong trào thanh niên xung kích đổi mới sáng tạo...", fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyDeep,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = CoralUrgent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (wsName.isBlank()) {
                                errorMessage = "Vui lòng nhập Tên Không gian làm việc!"
                                return@Button
                            }
                            onCreateWorkspace(
                                wsName.trim(),
                                wsOrg.trim(),
                                wsDesc.trim(),
                                selectedColorHex,
                                wsOwner.trim()
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyDeep,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_create_ws_button")
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Khởi tạo Không gian & Bắt đầu làm việc",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
