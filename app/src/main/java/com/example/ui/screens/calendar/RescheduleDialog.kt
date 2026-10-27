package com.example.ui.screens.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RescheduleDialog(
    event: CalendarEventEntity,
    onDismiss: () -> Unit,
    onConfirmReschedule: (newDate: String, newStartTime: String, newEndTime: String, newLocation: String) -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    var date by remember { mutableStateOf(event.date.ifBlank { todayStr }) }
    var startTime by remember { mutableStateOf(event.startTime.ifBlank { "08:30" }) }
    var endTime by remember { mutableStateOf(event.endTime.ifBlank { "10:30" }) }
    var location by remember { mutableStateOf(event.location) }

    val showDatePicker = {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(date)?.let { cal.time = it }
        } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(newCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showStartTimePicker = {
        var hour = 8
        var minute = 30
        try {
            val parts = startTime.split(":")
            if (parts.size == 2) {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            }
        } catch (_: Exception) {}
        TimePickerDialog(
            context,
            { _, h, m ->
                startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            },
            hour,
            minute,
            true
        ).show()
    }

    val showEndTimePicker = {
        var hour = 10
        var minute = 30
        try {
            val parts = endTime.split(":")
            if (parts.size == 2) {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            }
        } catch (_: Exception) {}
        TimePickerDialog(
            context,
            { _, h, m ->
                endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
            },
            hour,
            minute,
            true
        ).show()
    }

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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(0.8.dp, Color(0xFFBFDBFE))
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    Icons.Default.Update,
                                    contentDescription = null,
                                    tint = NavyDeep,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Đổi Lịch Trình Công Tác",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NavyHeroDark
                            )
                            Text(
                                "Thiết lập ngày, giờ và địa điểm mới",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextMuted)
                    }
                }

                // Event Target Info Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = event.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Lịch cũ: ${event.date} • ${event.startTime} - ${event.endTime.ifBlank { "10:00" }}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Date Selection Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ngày tổ chức mới:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker() },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            TextButton(onClick = showDatePicker) {
                                Text("Chọn", fontWeight = FontWeight.Bold, color = NavyDeep, fontSize = 12.sp)
                            }
                        }
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Hôm nay" to 0,
                            "Ngày mai" to 1,
                            "+3 ngày" to 3,
                            "+1 tuần" to 7
                        ).forEach { (label, offset) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                                        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NavyDeep,
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Time Selection Row: 2 spacious columns
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Khung giờ mới:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Time
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Giờ bắt đầu") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                IconButton(onClick = showStartTimePicker, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Giờ bắt đầu", tint = NavyDeep, modifier = Modifier.size(18.dp))
                                }
                            }
                        )

                        // End Time
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Giờ kết thúc") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                IconButton(onClick = showEndTimePicker, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Giờ kết thúc", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }

                // Location Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Địa điểm mới:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text("Nhập địa điểm hoặc phòng họp...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Hủy", color = TextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (date.isNotBlank() && startTime.isNotBlank()) {
                                onConfirmReschedule(date.trim(), startTime.trim(), endTime.trim(), location.trim())
                                Toast.makeText(context, "Đã đổi lịch thành công!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Vui lòng nhập ngày và giờ hợp lệ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_reschedule_button"),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xác nhận đổi lịch", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
