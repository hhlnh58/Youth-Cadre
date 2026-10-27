package com.example.ui.screens.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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

@Composable
fun ScheduleDetailDialog(
    event: CalendarEventEntity,
    onDismiss: () -> Unit,
    onEdit: (CalendarEventEntity) -> Unit,
    onMarkAttended: (CalendarEventEntity) -> Unit,
    onReschedule: (CalendarEventEntity) -> Unit,
    onExportGoogleCalendar: (CalendarEventEntity) -> Unit,
    onDelete: (CalendarEventEntity) -> Unit
) {
    val context = LocalContext.current
    val isPassed = isEventPassed15Minutes(event)
    val isFieldVisit = event.eventType == "FIELD_VISIT"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Status Badge & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Attendance / Passed / Upcoming status badge
                        if (event.isAttended) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD1FAE5),
                                border = BorderStroke(0.8.dp, Color(0xFF6EE7B7))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                                    Text("ĐÃ THAM GIA", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF047857))
                                }
                            }
                        } else if (isPassed) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                    Text("ĐÃ QUA LỊCH", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(0.8.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(14.dp))
                                    Text("SẮP DIỄN RA", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                                }
                            }
                        }

                        // Event Type badge
                        val isEventActivity = event.eventType == "EVENT" || event.eventType == "EVENT_ACTIVITY"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFieldVisit) Color(0xFFFEF3C7) else if (isEventActivity) Color(0xFFFEF3C7) else Color(0xFFF0FDF4),
                            border = BorderStroke(0.8.dp, if (isFieldVisit || isEventActivity) Color(0xFFFDE68A) else Color(0xFFBBF7D0))
                        ) {
                            Text(
                                text = if (isFieldVisit) "Đi cơ sở" else if (isEventActivity) "Sự kiện / Hoạt động" else "Họp giao ban",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFieldVisit) ChampagneGold else if (isEventActivity) Color(0xFFD97706) else EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (event.isRescheduled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE0F2FE),
                                border = BorderStroke(0.8.dp, Color(0xFFBAE6FD))
                            ) {
                                Text(
                                    "Đã đổi lịch",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted)
                    }
                }

                // Event Title
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NavyHeroDark,
                    lineHeight = 24.sp
                )

                Divider(color = BorderLight, thickness = 0.8.dp)

                // Key Info Card (Date, Time, Location)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date & Time Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(17.dp))
                            }
                            Column {
                                Text("Thời gian", fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${event.date} • ${event.startTime.ifBlank { "08:00" }} - ${event.endTime.ifBlank { "10:00" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Location Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(17.dp))
                            }
                            Column {
                                Text("Địa điểm", fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                Text(
                                    text = event.location.ifBlank { "Cơ quan Đoàn" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Host / Contact Person
                        if (event.contactPerson.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(17.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Chủ trì / Đầu mối liên hệ", fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${event.contactPerson}${if (event.contactPhone.isNotBlank()) " (${event.contactPhone})" else ""}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                if (event.contactPhone.isNotBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${event.contactPhone.trim()}"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Phone, contentDescription = "Gọi", tint = NavyDeep, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Participants
                        if (event.participants.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                                }
                                Column {
                                    Text("Thành phần tham gia", fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = event.participants,
                                        fontSize = 12.5.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Purpose / Details Content
                if (event.purpose.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nội dung / Mục đích công tác:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAFAFA),
                            border = BorderStroke(0.8.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = event.purpose,
                                fontSize = 12.5.sp,
                                color = TextPrimary,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Divider(color = BorderLight, thickness = 0.8.dp)

                // Action Buttons:
                // Primary Row: "Đã tham gia" (if not attended) & "Đổi lịch" & "Chỉnh sửa"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!event.isAttended) {
                            Button(
                                onClick = {
                                    onMarkAttended(event)
                                    Toast.makeText(context, "Đã xác nhận tham gia lịch trình!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("mark_attended_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Đã tham gia", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onReschedule(event)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reschedule_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đổi lịch", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onEdit(event)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_event_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chỉnh sửa", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Secondary Row: Google Lịch, Xóa, Đóng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onExportGoogleCalendar(event) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Lịch", fontSize = 11.sp, color = NavyDeep, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                onDelete(event)
                                Toast.makeText(context, "Đã xóa lịch trình", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = CoralUrgent, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Xóa", color = CoralUrgent, fontSize = 12.sp)
                            }

                            TextButton(onClick = onDismiss) {
                                Text("Đóng", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
