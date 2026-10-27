package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.data.firebase.configureAppCheck
import com.example.data.model.CalendarEventEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.LuminousAtmosphereBackground
import com.example.ui.components.ModernYouthCadreLogo
import com.example.ui.components.QuickCaptureDialog
import com.example.ui.components.UniversalSearchDialog
import com.example.ui.components.WorkspaceSwitcherDialog
import com.example.ui.components.WorkspaceOnboardingView
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.performSignOut
import com.example.ui.screens.cadreai.CadreAiScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.calendar.EventDetailDialog
import com.example.ui.screens.calendar.RescheduleDialog
import com.example.ui.screens.calendar.ScheduleDetailDialog
import com.example.ui.screens.calendar.exportEventToGoogleCalendar
import com.example.ui.screens.hub.HubScreen
import com.example.ui.screens.missioncontrol.MissionControlScreen
import com.example.ui.screens.tasks.TaskDetailDialog
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.CadreViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CadreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure App Check with launch intent before any UI or Firebase operations
        try {
            configureAppCheck(this, intent)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "App Check configuration error handled", t)
        }

        val navigateTo = intent?.getStringExtra("navigate_to")

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()

                if (currentUser == null) {
                    AuthScreen(
                        onAuthSuccess = {
                            // User is signed in, session active
                        }
                    )
                } else {
                    YouthCadreApp(viewModel = viewModel, initialScreen = navigateTo)
                }
            }
        }
    }
}

enum class NavigationScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MISSION_CONTROL("Điều hành", Icons.Default.Dashboard),
    TASKS("Nhiệm vụ", Icons.Default.Checklist),
    CALENDAR("Lịch trình", Icons.Default.CalendarToday),
    CADRE_AI("Cadre AI", Icons.Default.AutoAwesome),
    HUB("Kho tri thức", Icons.Default.FolderShared)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouthCadreApp(
    viewModel: CadreViewModel,
    initialScreen: String? = null
) {
    var currentScreen by remember {
        mutableStateOf(
            when (initialScreen) {
                "TASKS" -> NavigationScreen.TASKS
                "CALENDAR" -> NavigationScreen.CALENDAR
                else -> NavigationScreen.MISSION_CONTROL
            }
        )
    }
    val workspaces by viewModel.workspaces.collectAsState()
    val currentWsId by viewModel.currentWorkspaceId.collectAsState()
    val currentWorkspace = workspaces.find { it.id == currentWsId } ?: workspaces.firstOrNull()
    val currentUser by viewModel.currentUser.collectAsState()
    val people by viewModel.people.collectAsState()
    val portfolios by viewModel.portfolios.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    // Dialog States
    var showQuickCaptureDialog by remember { mutableStateOf(false) }
    var showUniversalSearch by remember { mutableStateOf(false) }
    var showWorkspaceSwitcher by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<TaskEntity?>(null) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var selectedEventForDetail by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var reschedulingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var showCreateEventDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.ui.util.CadreSystemIntegrations.sendTestNotification(context)
        } else {
            android.widget.Toast.makeText(context, "Vui lòng cho phép quyền Thông báo để nhận thông báo từ ứng dụng!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // FAB scroll & screen visibility state
    var isFabVisibleByScroll by remember { mutableStateOf(true) }
    val fabNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isFabVisibleByScroll = false
                } else if (available.y > 12f) {
                    isFabVisibleByScroll = true
                }
                return Offset.Zero
            }
        }
    }

    val isFabAllowedScreen = currentScreen in listOf(
        NavigationScreen.MISSION_CONTROL,
        NavigationScreen.TASKS,
        NavigationScreen.CALENDAR
    )

    // Request Notification Permission on Android 13+
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* result */ }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        com.example.ui.util.CadreSystemIntegrations.scheduleDailyBriefingAlarm(context, 7, 30)
    }

    // Intercept back button to return to Mission Control if on sub-screens
    BackHandler(enabled = currentScreen != NavigationScreen.MISSION_CONTROL) {
        currentScreen = NavigationScreen.MISSION_CONTROL
    }

    LuminousAtmosphereBackground {
        if (workspaces.isEmpty()) {
            WorkspaceOnboardingView(
                onCreateWorkspace = { name, org, desc, colorHex, ownerName ->
                    viewModel.createWorkspace(name, org, desc, colorHex, ownerName)
                }
            )
        } else {
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ModernYouthCadreLogo(size = 38.dp)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "YOUTH CADRE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                            .clickable { showWorkspaceSwitcher = true }
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldSuccess)
                                        )
                                        Text(
                                            currentWorkspace?.name ?: "Tất cả đơn vị",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 135.dp)
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Chuyển đơn vị",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { showUniversalSearch = true }
                                        .testTag("open_universal_search_button"),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color(0xFF334155), modifier = Modifier.size(19.dp))
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { currentScreen = NavigationScreen.CADRE_AI }
                                        .testTag("topbar_ai_button"),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF2563EB), modifier = Modifier.size(19.dp))
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { showAccountDialog = true }
                                        .testTag("topbar_account_button"),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = "Tài khoản", tint = Color(0xFF334155), modifier = Modifier.size(21.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                val navShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(navShape)
                            .testTag("bottom_nav_bar"),
                        shape = navShape,
                        color = PureWhite,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationScreen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) Modifier.background(NavyDeep)
                                            else Modifier.clickable { currentScreen = screen }
                                        )
                                        .clickable { currentScreen = screen }
                                        .testTag("nav_${screen.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isFabAllowedScreen && isFabVisibleByScroll,
                    enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150))
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(bottom = 12.dp, end = 2.dp)
                            .size(62.dp)
                            .clip(CircleShape)
                            .clickable { showQuickCaptureDialog = true }
                            .testTag("quick_capture_fab"),
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(FreshExecutiveFabGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = "Thao tác nhanh",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .nestedScroll(fabNestedScrollConnection)
            ) {
                when (currentScreen) {
                    NavigationScreen.MISSION_CONTROL -> MissionControlScreen(
                        viewModel = viewModel,
                        onNavigateToTasks = { currentScreen = NavigationScreen.TASKS },
                        onNavigateToCalendar = { currentScreen = NavigationScreen.CALENDAR },
                        onNavigateToAi = { currentScreen = NavigationScreen.CADRE_AI },
                        onOpenTaskDetail = { task -> selectedTaskForDetail = task },
                        onOpenEventDetail = { event -> selectedEventForDetail = event },
                        onRescheduleEvent = { event -> reschedulingEvent = event },
                        onQuickCaptureOpen = { showQuickCaptureDialog = true }
                    )
                    NavigationScreen.TASKS -> TasksScreen(
                        viewModel = viewModel,
                        onOpenTaskDetail = { task -> selectedTaskForDetail = task },
                        onCreateTask = { showCreateTaskDialog = true }
                    )
                    NavigationScreen.CALENDAR -> CalendarScreen(
                        viewModel = viewModel,
                        onCreateEvent = { showCreateEventDialog = true },
                        onOpenEventDetail = { event -> selectedEventForDetail = event }
                    )
                    NavigationScreen.CADRE_AI -> CadreAiScreen(viewModel = viewModel)
                    NavigationScreen.HUB -> HubScreen(viewModel = viewModel)
                }
            }
        }
        }
    }

    // Thao tác nhanh (Smart Quick Capture) Dialog
    if (showQuickCaptureDialog) {
        QuickCaptureDialog(
            people = people,
            tasks = tasks,
            onDismiss = { showQuickCaptureDialog = false },
            onSubmitEntity = { entity ->
                viewModel.quickCaptureParsed(entity)
            },
            onCreateSchedule = {
                showQuickCaptureDialog = false
                showCreateEventDialog = true
            },
            onAppendTaskNote = { task, note ->
                viewModel.appendTaskNote(task, note)
            },
            onCreateIndependentNote = { title, content ->
                viewModel.saveNote(title = title, content = content)
            },
            onSaveSettlement = { title, amt, budgetCode, deadline, taskId, taskTitle, resp ->
                viewModel.saveSettlement(
                    title = title,
                    amount = amt,
                    budgetEstimateCode = budgetCode,
                    deadline = deadline,
                    taskId = taskId,
                    taskTitle = taskTitle,
                    responsiblePerson = resp
                )
            }
        )
    }

    // Workspace Switcher Dialog
    if (showWorkspaceSwitcher) {
        WorkspaceSwitcherDialog(
            workspaces = workspaces,
            currentWorkspaceId = currentWsId,
            onSelectWorkspace = { wsId -> viewModel.setWorkspace(wsId) },
            onCreateWorkspace = { name, org, desc, colorHex, ownerName ->
                viewModel.createWorkspace(name, org, desc, colorHex, ownerName)
            },
            onUpdateWorkspace = { ws -> viewModel.updateWorkspace(ws) },
            onDeleteWorkspace = { ws -> viewModel.deleteWorkspace(ws) },
            onDismiss = { showWorkspaceSwitcher = false }
        )
    }

    // Universal Search Dialog
    if (showUniversalSearch) {
        UniversalSearchDialog(
            viewModel = viewModel,
            onDismiss = { showUniversalSearch = false },
            onSelectTask = { task ->
                selectedTaskForDetail = task
            }
        )
    }

    // Account & Firebase Cloud Sync Status Dialog
    if (showAccountDialog) {
        Dialog(onDismissRequest = { showAccountDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ModernYouthCadreLogo(size = 54.dp)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser?.displayName ?: "Cán bộ Đoàn chuyên trách",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = currentUser?.email ?: "Đang đồng bộ",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x0C10B981),
                        border = BorderStroke(0.6.dp, Color(0x3510B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Cơ sở dữ liệu Firebase Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Dữ liệu thật • Tự động đồng bộ thời gian thực", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x0C2563EB),
                        border = BorderStroke(0.6.dp, Color(0x352563EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Cadre AI (Gemini 3.8 Flash)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Đọc hiểu kho dữ liệu • Xếp lịch • Tham mưu", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Sync Now
                    Button(
                        onClick = {
                            viewModel.triggerManualCloudSync()
                            android.widget.Toast.makeText(context, "Đang đồng bộ dữ liệu thời gian thực từ Firebase Cloud...", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_cloud_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đồng bộ dữ liệu đám mây ngay", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Test notification
                    OutlinedButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                com.example.ui.util.CadreSystemIntegrations.sendTestNotification(context)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                        border = BorderStroke(1.dp, Color(0x402563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("test_notification_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kiểm tra thông báo hệ thống", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Daily Briefing notification
                    OutlinedButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                com.example.ui.util.CadreSystemIntegrations.sendDailyBriefingNotification(context)
                                android.widget.Toast.makeText(context, "Đã gửi thông báo điểm báo & việc tồn đọng!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        border = BorderStroke(1.dp, Color(0x40D97706)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("daily_briefing_button")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Báo cáo lịch trình & việc tồn đọng", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Refresh widget
                    OutlinedButton(
                        onClick = {
                            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(context)
                            android.widget.Toast.makeText(context, "Đã gửi lệnh làm mới Widget Cán bộ Đoàn!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyDeep),
                        border = BorderStroke(1.dp, Color(0x301B1464)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("refresh_widget_button")
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Làm mới Widget màn hình chính", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showAccountDialog = false
                            performSignOut(context, credentialManager, scope) {}
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralUrgent),
                        border = BorderStroke(1.dp, Color(0x40EF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đăng xuất tài khoản", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("Đóng", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Task Detail / Edit / Create Dialog
    if (showCreateTaskDialog || selectedTaskForDetail != null) {
        TaskDetailDialog(
            task = selectedTaskForDetail,
            portfolios = portfolios,
            people = people,
            onDismiss = {
                showCreateTaskDialog = false
                selectedTaskForDetail = null
            },
            onSave = { id, title, desc, loc, assigner, resp, collab, priority, status, dueDate, dueTime, portId, projId, waiting, drive, output, progress, attached, notes ->
                viewModel.saveTask(id, title, desc, loc, assigner, resp, collab, priority, status, dueDate, dueTime, portId, projId, waiting, drive, output, progress, attached, notes)
            },
            onDelete = { task ->
                viewModel.deleteTask(task)
            }
        )
    }

    // Schedule Detail Dialog (Read-only view when tapping schedule)
    selectedEventForDetail?.let { event ->
        ScheduleDetailDialog(
            event = event,
            onDismiss = { selectedEventForDetail = null },
            onEdit = { ev ->
                selectedEventForDetail = null
                editingEvent = ev
            },
            onMarkAttended = { ev ->
                viewModel.markEventAttended(ev)
                selectedEventForDetail = null
            },
            onReschedule = { ev ->
                selectedEventForDetail = null
                reschedulingEvent = ev
            },
            onExportGoogleCalendar = { ev ->
                exportEventToGoogleCalendar(context, ev)
            },
            onDelete = { ev ->
                viewModel.deleteCalendarEvent(ev)
                selectedEventForDetail = null
            }
        )
    }

    // Reschedule Dialog (Dedicated modal to update date, start/end time, and location)
    reschedulingEvent?.let { event ->
        RescheduleDialog(
            event = event,
            onDismiss = { reschedulingEvent = null },
            onConfirmReschedule = { newDate, newStart, newEnd, newLoc ->
                viewModel.rescheduleEvent(event, newDate, newStart, newEnd, newLoc)
                reschedulingEvent = null
            }
        )
    }

    // Event Edit / Create Dialog (Form view: only shown when creating new or clicking Chỉnh sửa)
    if (showCreateEventDialog || editingEvent != null) {
        EventDetailDialog(
            event = editingEvent,
            onDismiss = {
                showCreateEventDialog = false
                editingEvent = null
            },
            onSave = { id, title, eventType, date, start, end, loc, dest, contact, phone, purpose, parts ->
                viewModel.saveCalendarEvent(id, title, eventType, date, start, end, loc, dest, contact, phone, purpose, parts)
            },
            onDelete = { event ->
                viewModel.deleteCalendarEvent(event)
            }
        )
    }
}
