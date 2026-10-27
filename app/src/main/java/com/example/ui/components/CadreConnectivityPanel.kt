package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.util.CadreSystemIntegrations
import com.example.ui.viewmodel.CadreViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CadreConnectivityHubCard(
    viewModel: CadreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    var lastPickedContact by remember { mutableStateOf<String?>(null) }
    var lastPickedFile by remember { mutableStateOf<String?>(null) }
    var lastPickedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 1. Permission for Notifications on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CadreSystemIntegrations.sendTestNotification(context)
        } else {
            Toast.makeText(context, "Chưa cấp quyền thông báo hệ thống!", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Contact Picker Launcher (Using Phone.CONTENT_URI to avoid security permissions)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            val contact = CadreSystemIntegrations.resolveContact(context, uri)
            if (contact != null) {
                lastPickedContact = "${contact.name} (${contact.phone})"
                viewModel.createPerson(
                    fullName = contact.name,
                    position = "Cán bộ phối hợp",
                    unit = "Đơn vị cơ sở",
                    phone = contact.phone,
                    email = "",
                    notes = "Đồng bộ từ Danh bạ máy",
                    isKeyLeader = false
                )
                Toast.makeText(context, "Đã thêm đồng chí ${contact.name} vào danh bạ đơn vị!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 3. Document / File Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = CadreSystemIntegrations.queryFileName(context, it)
            lastPickedFile = fileName
            Toast.makeText(context, "Đã chọn tệp: $fileName", Toast.LENGTH_LONG).show()
        }
    }

    // 4. Photo Picker Launcher (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            lastPickedImageUri = it
            val fileName = CadreSystemIntegrations.queryFileName(context, it)
            lastPickedFile = "Ảnh: $fileName"
            Toast.makeText(context, "Đã chọn hình ảnh hoạt động: $fileName", Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("connectivity_hub_card"),
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
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
                            .clip(CircleShape)
                            .background(NavyDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "KẾT NỐI TOÀN DIỆN",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NavyHeroDark,
                                letterSpacing = 0.3.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(0.6.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    "4/4 Sẵn sàng",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Google Lịch • Nhắc nhở • Danh bạ máy • Tệp & Hình ảnh",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Mở rộng",
                        tint = NavyDeep
                    )
                }
            }

            // 4 Grid Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntegrationStatusPill(
                    icon = Icons.Default.CalendarToday,
                    title = "Google Lịch",
                    status = "Sẵn sàng",
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        CadreSystemIntegrations.openGoogleCalendarApp(context)
                    }
                )
                IntegrationStatusPill(
                    icon = Icons.Default.NotificationsActive,
                    title = "Nhắc nhở",
                    status = "Hoạt động",
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            CadreSystemIntegrations.sendTestNotification(context)
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntegrationStatusPill(
                    icon = Icons.Default.Contacts,
                    title = "Danh bạ máy",
                    status = if (lastPickedContact != null) "Đã đồng bộ" else "Kết nối",
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(pickIntent)
                    }
                )
                IntegrationStatusPill(
                    icon = Icons.Default.AttachFile,
                    title = "Tệp & Ảnh",
                    status = if (lastPickedFile != null) "Đã chọn" else "Bộ nhớ",
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }

            // Expanded action center
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = BorderLight, thickness = 0.8.dp)

                    // 1. Google Lịch action
                    IntegrationActionRow(
                        icon = Icons.Default.CalendarToday,
                        title = "Xuất lịch họp mẫu vào Google Lịch",
                        description = "Đồng bộ tức thì sự kiện vào Google Calendar trên máy",
                        buttonText = "Xuất Lịch",
                        buttonColor = Color(0xFF2563EB)
                    ) {
                        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(tomorrow.time)
                        CadreSystemIntegrations.exportEventToGoogleCalendar(
                            context = context,
                            title = "Họp Ban Thường vụ chuẩn bị Tháng Thanh niên",
                            description = "Chủ trì: Đ/c Bí thư. Thành phần: Các đồng chí Ủy viên Ban Thường vụ.",
                            location = "Hội trường cơ quan Đoàn",
                            dateStr = dateStr,
                            timeStr = "08:30"
                        )
                    }

                    // 2. Nhắc nhở action
                    IntegrationActionRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Bắn thông báo nhắc nhở tức thì",
                        description = "Kiểm tra chuông, rung và thông báo hệ thống Android",
                        buttonText = "Gửi Thử",
                        buttonColor = Color(0xFFD97706)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            CadreSystemIntegrations.sendTestNotification(context)
                        }
                    }

                    // 3. Danh bạ máy action
                    IntegrationActionRow(
                        icon = Icons.Default.Contacts,
                        title = "Chọn cán bộ từ Danh bạ điện thoại",
                        description = lastPickedContact ?: "Trích xuất tên và số điện thoại cán bộ vào hồ sơ",
                        buttonText = "Mở Danh Bạ",
                        buttonColor = Color(0xFF059669)
                    ) {
                        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(pickIntent)
                    }

                    // 4. Widget Màn hình chính action
                    IntegrationActionRow(
                        icon = Icons.Default.Widgets,
                        title = "Đồng bộ Widget Màn hình chính Android",
                        description = "Đẩy dữ liệu lịch trình và nhiệm vụ khẩn ra Widget ngoài màn hình",
                        buttonText = "Làm Mới Widget",
                        buttonColor = NavyDeep
                    ) {
                        CadreSystemIntegrations.updateAppWidget(context)
                        Toast.makeText(context, "✅ Đã đồng bộ dữ liệu ra Widget màn hình chính!", Toast.LENGTH_SHORT).show()
                    }

                    // 4. Tệp & Hình ảnh actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn Tệp Văn bản", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn Ảnh Hoạt động", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (lastPickedFile != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3E8FF),
                            border = BorderStroke(0.6.dp, Color(0xFFD8B4FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(14.dp))
                                Text("Đã kết nối: $lastPickedFile", fontSize = 11.sp, color = Color(0xFF581C87), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrationStatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text(status, fontSize = 9.sp, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun IntegrationActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(0.8.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = buttonColor, modifier = Modifier.size(20.dp))
                Column {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(description, fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
