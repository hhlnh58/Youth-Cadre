package com.example.ui.screens.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEventEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CadreViewModel
import java.text.SimpleDateFormat
import java.util.*

fun parseEventStartTimestamp(dateStr: String, timeStr: String): Long {
    val cleanDate = dateStr.trim()
    val cleanTime = timeStr.trim().ifBlank { "08:00" }
    val formats = listOf("dd/MM/yyyy HH:mm", "yyyy-MM-dd HH:mm", "dd/MM/yyyy", "yyyy-MM-dd")
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
            val target = if (fmt.contains("HH:mm")) "$cleanDate $cleanTime" else cleanDate
            val parsed = sdf.parse(target)
            if (parsed != null) {
                return parsed.time
            }
        } catch (_: Exception) {}
    }
    return 0L
}

fun isEventPassed15Minutes(event: CalendarEventEntity, now: Long = System.currentTimeMillis()): Boolean {
    val startTs = parseEventStartTimestamp(event.date, event.startTime)
    if (startTs == 0L) return false
    return now >= (startTs + 15 * 60 * 1000L)
}

fun isEventUpcoming(event: CalendarEventEntity, now: Long = System.currentTimeMillis()): Boolean {
    if (event.isAttended || event.isCompleted) return false
    return !isEventPassed15Minutes(event, now)
}

fun exportEventToGoogleCalendar(context: Context, event: CalendarEventEntity) {
    val description = buildString {
        if (event.purpose.isNotBlank()) appendLine("Nội dung: ${event.purpose}")
        if (event.contactPerson.isNotBlank()) appendLine("Chủ trì / Liên hệ: ${event.contactPerson} (${event.contactPhone})")
        if (event.participants.isNotBlank()) appendLine("Thành phần: ${event.participants}")
        if (event.destinationUnit.isNotBlank()) appendLine("Đơn vị: ${event.destinationUnit}")
    }.trim()

    var startMs = System.currentTimeMillis()
    var endMs = startMs + 60 * 60 * 1000
    try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val startDateTime = sdf.parse("${event.date} ${event.startTime.ifBlank { "08:00" }}")
        if (startDateTime != null) {
            startMs = startDateTime.time
            endMs = if (event.endTime.isNotBlank()) {
                sdf.parse("${event.date} ${event.endTime}")?.time ?: (startMs + 60 * 60 * 1000)
            } else {
                startMs + 60 * 60 * 1000
            }
        }
    } catch (_: Exception) {}

    // 1. Try Native Calendar Contract Intent
    try {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, event.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        Toast.makeText(context, "Đang kết nối Google Lịch...", Toast.LENGTH_SHORT).show()
        return
    } catch (_: Exception) {
        // Fallback to Web Google Calendar
    }

    // 2. Fallback to Google Calendar Web URL
    try {
        val webSdf = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
        val dateParam = "${webSdf.format(Date(startMs))}/${webSdf.format(Date(endMs))}"
        val webUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                "&text=${Uri.encode(event.title)}" +
                "&dates=$dateParam" +
                "&details=${Uri.encode(description)}" +
                "&location=${Uri.encode(event.location)}"

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(webIntent)
        Toast.makeText(context, "Đang mở Google Lịch qua trình duyệt...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Không thể mở Google Lịch: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun openGoogleCalendarApp(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.calendar")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            Toast.makeText(context, "Đang mở ứng dụng Google Lịch...", Toast.LENGTH_SHORT).show()
            return
        }
    } catch (_: Exception) {}
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "Đang mở Google Lịch qua trình duyệt...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Không thể mở Google Lịch", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CalendarScreen(
    viewModel: CadreViewModel,
    onCreateEvent: () -> Unit,
    onOpenEventDetail: (CalendarEventEntity) -> Unit
) {
    val events by viewModel.calendarEvents.collectAsState()
    val context = LocalContext.current

    // View Modes: DAY (Ngày), WEEK (Tuần), MONTH (Tháng)
    var calendarViewMode by remember { mutableStateOf("DAY") } // DAY, WEEK, MONTH
    var selectedCalendarDate by remember {
        mutableStateOf(Calendar.getInstance())
    }

    val selectedDateStr = remember(selectedCalendarDate) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendarDate.time)
    }

    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }

    // Filter events based on view mode and type filter
    val currentDayEvents = events.filter { it.date == selectedDateStr }
    val filteredDayEvents = remember(currentDayEvents, selectedTypeFilter) {
        if (selectedTypeFilter == null) currentDayEvents
        else currentDayEvents.filter {
            if (selectedTypeFilter == "EVENT") it.eventType == "EVENT" || it.eventType == "EVENT_ACTIVITY"
            else it.eventType == selectedTypeFilter
        }
    }
    val filteredAllEvents = remember(events, selectedTypeFilter) {
        if (selectedTypeFilter == null) events
        else events.filter {
            if (selectedTypeFilter == "EVENT") it.eventType == "EVENT" || it.eventType == "EVENT_ACTIVITY"
            else it.eventType == selectedTypeFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Lịch trình công tác",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = NavyHeroDark
                )
                Text(
                    "Thời gian biểu & Kết nối trực tiếp Google Lịch",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { openGoogleCalendarApp(context) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyMedium.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Google Lịch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                }

                Button(
                    onClick = onCreateEvent,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_event_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-Mode Switcher: Theo Ngày / Theo Tuần / Theo Tháng
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CalendarModeTab(
                    label = "Theo Ngày",
                    icon = Icons.Default.ViewDay,
                    isSelected = calendarViewMode == "DAY",
                    onClick = { calendarViewMode = "DAY" },
                    modifier = Modifier.weight(1f)
                )
                CalendarModeTab(
                    label = "Theo Tuần",
                    icon = Icons.Default.ViewWeek,
                    isSelected = calendarViewMode == "WEEK",
                    onClick = { calendarViewMode = "WEEK" },
                    modifier = Modifier.weight(1f)
                )
                CalendarModeTab(
                    label = "Theo Tháng",
                    icon = Icons.Default.CalendarMonth,
                    isSelected = calendarViewMode == "MONTH",
                    onClick = { calendarViewMode = "MONTH" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Category Filter Chips: Tất cả | Họp giao ban | Đi cơ sở | Sự kiện / Hoạt động
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                null to "Tất cả",
                "MEETING" to "Họp giao ban",
                "FIELD_VISIT" to "Đi cơ sở",
                "EVENT" to "Sự kiện / Hoạt động"
            ).forEach { (typeKey, label) ->
                val isSelected = selectedTypeFilter == typeKey
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTypeFilter = typeKey },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) NavyDeep else PureWhite,
                    border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // View Mode Content
        when (calendarViewMode) {
            "DAY" -> {
                DayScheduleView(
                    selectedCal = selectedCalendarDate,
                    onDateChange = { newCal ->
                        selectedCalendarDate = newCal.clone() as Calendar
                    },
                    events = filteredDayEvents,
                    onOpenDetail = onOpenEventDetail,
                    onExportGoogleCalendar = { event ->
                        exportEventToGoogleCalendar(context, event)
                    },
                    onCreateEvent = onCreateEvent
                )
            }
            "WEEK" -> {
                WeekScheduleView(
                    selectedCal = selectedCalendarDate,
                    onDateChange = { newCal ->
                        selectedCalendarDate = newCal.clone() as Calendar
                    },
                    events = filteredAllEvents,
                    onOpenDetail = onOpenEventDetail,
                    onExportGoogleCalendar = { event ->
                        exportEventToGoogleCalendar(context, event)
                    },
                    onCreateEvent = onCreateEvent
                )
            }
            "MONTH" -> {
                MonthScheduleView(
                    selectedCal = selectedCalendarDate,
                    onDateChange = { newCal ->
                        selectedCalendarDate = newCal.clone() as Calendar
                    },
                    events = filteredAllEvents,
                    onOpenDetail = onOpenEventDetail,
                    onExportGoogleCalendar = { event ->
                        exportEventToGoogleCalendar(context, event)
                    },
                    onCreateEvent = onCreateEvent
                )
            }
        }
    }
}

@Composable
fun CalendarModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) NavyDeep else Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

// ==========================================
// 1. THEO NGÀY (DAY VIEW)
// ==========================================
@Composable
fun DayScheduleView(
    selectedCal: Calendar,
    onDateChange: (Calendar) -> Unit,
    events: List<CalendarEventEntity>,
    onOpenDetail: (CalendarEventEntity) -> Unit,
    onExportGoogleCalendar: (CalendarEventEntity) -> Unit,
    onCreateEvent: () -> Unit
) {
    val dayFormat = SimpleDateFormat("EEEE, 'ngày' dd/MM/yyyy", Locale("vi", "VN"))
    val displayDate = dayFormat.format(selectedCal.time).replaceFirstChar { it.titlecase() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date Navigator
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = selectedCal.clone() as Calendar
                        newCal.add(Calendar.DAY_OF_YEAR, -1)
                        onDateChange(newCal)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ngày trước", tint = NavyDeep)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        displayDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyHeroDark
                    )
                    Text(
                        "${events.size} lịch công tác trong ngày",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onDateChange(Calendar.getInstance()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Hôm nay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    IconButton(
                        onClick = {
                            val newCal = selectedCal.clone() as Calendar
                            newCal.add(Calendar.DAY_OF_YEAR, 1)
                            onDateChange(newCal)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ngày sau", tint = NavyDeep)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (events.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(24.dp))
                    }
                    Text("Không có lịch công tác ngày này", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Đồng chí có thể thêm lịch mới hoặc dùng Thao tác nhanh để ghi nhận", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Button(
                        onClick = onCreateEvent,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tạo lịch công tác", fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    TimelineEventCard(
                        event = event,
                        onClick = { onOpenDetail(event) },
                        onExportGoogleCalendar = { onExportGoogleCalendar(event) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. THEO TUẦN (WEEK VIEW)
// ==========================================
@Composable
fun WeekScheduleView(
    selectedCal: Calendar,
    onDateChange: (Calendar) -> Unit,
    events: List<CalendarEventEntity>,
    onOpenDetail: (CalendarEventEntity) -> Unit,
    onExportGoogleCalendar: (CalendarEventEntity) -> Unit,
    onCreateEvent: () -> Unit
) {
    // Determine the start of the week (Monday)
    val startOfWeekCal = (selectedCal.clone() as Calendar).apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }

    val daysOfWeek = remember(startOfWeekCal) {
        val list = mutableListOf<Calendar>()
        val iter = startOfWeekCal.clone() as Calendar
        for (i in 0..6) {
            list.add(iter.clone() as Calendar)
            iter.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val selectedDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCal.time)
    val selectedDayEvents = events.filter { it.date == selectedDateStr }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week Navigator
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = selectedCal.clone() as Calendar
                        newCal.add(Calendar.WEEK_OF_YEAR, -1)
                        onDateChange(newCal)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tuần trước", tint = NavyDeep)
                }

                Text(
                    "Tuần ${selectedCal.get(Calendar.WEEK_OF_YEAR)} • Tháng ${selectedCal.get(Calendar.MONTH) + 1}/${selectedCal.get(Calendar.YEAR)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyHeroDark
                )

                IconButton(
                    onClick = {
                        val newCal = selectedCal.clone() as Calendar
                        newCal.add(Calendar.WEEK_OF_YEAR, 1)
                        onDateChange(newCal)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Tuần sau", tint = NavyDeep)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 7-Day Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            daysOfWeek.forEachIndexed { index, dayCal ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dayCal.time)
                val dayNumber = dayCal.get(Calendar.DAY_OF_MONTH)
                val isSelected = dateStr == selectedDateStr
                val eventCount = events.count { it.date == dateStr }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDateChange(dayCal) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) NavyDeep else PureWhite,
                    border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            dayNames[index],
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color(0xFF93C5FD) else TextMuted
                        )
                        Text(
                            "$dayNumber",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                        // Event dot indicator
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (eventCount > 0) {
                                        if (isSelected) Color(0xFF67E8F9) else EmeraldSuccess
                                    } else Color.Transparent
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Day Events in Week
        Text(
            "Lịch ngày ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCal.time)} (${selectedDayEvents.size}):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )

        if (selectedDayEvents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có lịch công tác trong ngày đã chọn.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(selectedDayEvents, key = { it.id }) { event ->
                    TimelineEventCard(
                        event = event,
                        onClick = { onOpenDetail(event) },
                        onExportGoogleCalendar = { onExportGoogleCalendar(event) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. THEO THÁNG (MONTH VIEW)
// ==========================================
@Composable
fun MonthScheduleView(
    selectedCal: Calendar,
    onDateChange: (Calendar) -> Unit,
    events: List<CalendarEventEntity>,
    onOpenDetail: (CalendarEventEntity) -> Unit,
    onExportGoogleCalendar: (CalendarEventEntity) -> Unit,
    onCreateEvent: () -> Unit
) {
    val monthCal = (selectedCal.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthName = "Tháng ${selectedCal.get(Calendar.MONTH) + 1}, ${selectedCal.get(Calendar.YEAR)}"
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // First day of month day-of-week (convert to Monday=0..Sunday=6)
    val firstDayDow = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val selectedDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCal.time)
    val selectedDayEvents = events.filter { it.date == selectedDateStr }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Navigator
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = selectedCal.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        onDateChange(newCal)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tháng trước", tint = NavyDeep)
                }

                Text(
                    monthName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NavyHeroDark
                )

                IconButton(
                    onClick = {
                        val newCal = selectedCal.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        onDateChange(newCal)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Tháng sau", tint = NavyDeep)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month Grid Surface
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Day headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { dow ->
                        Text(
                            text = dow,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Days cells
                val totalCells = firstDayDow + daysInMonth
                val totalRows = (totalCells + 6) / 7

                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - firstDayDow + 1

                            if (cellIndex < firstDayDow || dayNum > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).height(36.dp))
                            } else {
                                val cellCal = (monthCal.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_MONTH, dayNum)
                                }
                                val cellDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cellCal.time)
                                val isSelected = cellDateStr == selectedDateStr
                                val hasEvents = events.any { it.date == cellDateStr }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NavyDeep else Color.Transparent)
                                        .clickable { onDateChange(cellCal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                        if (hasEvents) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color(0xFF67E8F9) else EmeraldSuccess)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Lịch ngày $selectedDateStr (${selectedDayEvents.size}):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )

        if (selectedDayEvents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Box(
                    modifier = Modifier.padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có cuộc họp hoặc chuyến công tác nào trong ngày này.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(selectedDayEvents, key = { it.id }) { event ->
                    TimelineEventCard(
                        event = event,
                        onClick = { onOpenDetail(event) },
                        onExportGoogleCalendar = { onExportGoogleCalendar(event) }
                    )
                }
            }
        }
    }
}

// ==========================================
// REUSABLE TIMELINE EVENT CARD WITH GOOGLE CALENDAR
// ==========================================
@Composable
fun TimelineEventCard(
    event: CalendarEventEntity,
    onClick: () -> Unit,
    onExportGoogleCalendar: () -> Unit
) {
    val context = LocalContext.current
    val isFieldVisit = event.eventType == "FIELD_VISIT"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 0.dp
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFieldVisit) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                        border = BorderStroke(0.6.dp, if (isFieldVisit) Color(0xFFFDE68A) else Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = event.startTime.ifBlank { "08:00" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFieldVisit) ChampagneGold else NavyDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    val isEventActivity = event.eventType == "EVENT" || event.eventType == "EVENT_ACTIVITY"
                    val (badgeLabel, badgeColor, badgeBg, badgeBorder) = when {
                        isFieldVisit -> listOf("Đi cơ sở", ChampagneGold, Color(0xFFFEF3C7), Color(0xFFFDE68A))
                        isEventActivity -> listOf("Sự kiện / Hoạt động", Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFFFDE68A))
                        else -> listOf("Họp giao ban", EmeraldSuccess, Color(0xFFDCFCE7), Color(0xFFBBF7D0))
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg as Color,
                        border = BorderStroke(0.6.dp, badgeBorder as Color)
                    ) {
                        Text(
                            text = badgeLabel as String,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor as Color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (event.isAttended) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD1FAE5),
                            border = BorderStroke(0.6.dp, Color(0xFF6EE7B7))
                        ) {
                            Text(
                                text = "✓ Đã tham gia",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isEventPassed15Minutes(event)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "Đã qua lịch",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (event.isRescheduled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(0.6.dp, Color(0xFFBAE6FD))
                        ) {
                            Text(
                                text = "Đã đổi lịch",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Google Calendar Connect Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { onExportGoogleCalendar() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Google Calendar",
                                tint = NavyMedium,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                "Google Lịch",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep
                            )
                        }
                    }
                }
            }

            Text(
                text = event.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )

            if (event.location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = NavyMedium, modifier = Modifier.size(14.dp))
                    Text(
                        event.location,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (event.contactPerson.isNotBlank() || event.participants.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        if (event.contactPerson.isNotBlank()) "Chủ trì: ${event.contactPerson}" else "Thành phần: ${event.participants}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
