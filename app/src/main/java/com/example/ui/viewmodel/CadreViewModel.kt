package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.CadreAiEngine
import com.example.data.ai.ParsedCadreEntity
import com.example.data.ai.SmartCadreParser
import com.example.data.model.*
import com.example.data.repository.CadreRepository
import com.example.data.repository.FirestoreCadreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CadreViewModel"

class CadreViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CadreRepository(application)
    val firestoreRepository = FirestoreCadreRepository(application)

    // Auth State
    val currentUser = MutableStateFlow(
        try {
            Firebase.auth.currentUser
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot read initial Firebase currentUser", t)
            null
        }
    )

    // Current workspace
    private val _currentWorkspaceId = MutableStateFlow("")
    val currentWorkspaceId: StateFlow<String> = _currentWorkspaceId.asStateFlow()

    // Data streams
    val workspaces: StateFlow<List<WorkspaceEntity>> = repository.getAllWorkspaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val people: StateFlow<List<PersonEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getPeople(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolios: StateFlow<List<PortfolioEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getPortfolios(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getProjects(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getTasks(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEventEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getCalendarEvents(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getNotes(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settlements: StateFlow<List<SettlementEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getSettlements(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<DocumentEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getDocuments(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decisions: StateFlow<List<DecisionEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getDecisions(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickLinks: StateFlow<List<QuickLinkEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getQuickLinks(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = _currentWorkspaceId.flatMapLatest {
        repository.getChatMessages(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Firestore streams
    val firestoreTasks = MutableStateFlow<List<FirestoreTask>>(emptyList())
    val firestoreEvents = MutableStateFlow<List<FirestoreEvent>>(emptyList())
    val firestoreCirculars = MutableStateFlow<List<FirestoreCircular>>(emptyList())
    val firestoreAdvisories = MutableStateFlow<List<FirestoreAdvisory>>(emptyList())

    // UI state
    val isAiThinking = MutableStateFlow(false)
    val currentAiMode = MutableStateFlow("ASSISTANT") // ASSISTANT, PLANNER, DRAFTING, REVIEW, EXECUTIVE_BRIEF

    // Filter states
    val taskFilterStatus = MutableStateFlow<String?>(null)
    val taskFilterPriority = MutableStateFlow<String?>(null)
    val taskFilterAssigner = MutableStateFlow<String?>(null)
    val taskFilterPortfolio = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow("")

    init {
        // Observe Firebase Auth changes safely
        try {
            val authListener = FirebaseAuth.AuthStateListener { auth ->
                try {
                    val user = auth.currentUser
                    currentUser.value = user
                    if (user != null) {
                        onUserSignedIn(user.uid)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in auth state listener", t)
                }
            }
            Firebase.auth.addAuthStateListener(authListener)

            Firebase.auth.currentUser?.let {
                onUserSignedIn(it.uid)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize Firebase Auth in CadreViewModel", t)
        }

        // Purge only deprecated mock template workspace
        viewModelScope.launch {
            try {
                repository.clearMockWorkspace()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to clear mock workspace", t)
            }
        }

        // Sync active workspace
        viewModelScope.launch {
            workspaces.collect { wsList ->
                if (wsList.isNotEmpty() && _currentWorkspaceId.value.isNotBlank() && wsList.none { it.id == _currentWorkspaceId.value }) {
                    _currentWorkspaceId.value = wsList.first().id
                }
            }
        }

        // Seed default quick links if empty
        viewModelScope.launch {
            try {
                repository.getQuickLinks("").take(1).collect { existing ->
                    if (existing.isEmpty()) {
                        repository.insertQuickLink(
                            QuickLinkEntity(
                                id = "ql-doanthanhnien",
                                workspaceId = "ws_primary_doan",
                                title = "Cổng TT Đoàn TNCS Hồ Chí Minh",
                                url = "https://doanthanhnien.vn",
                                category = "Cổng thông tin",
                                description = "Tin tức phong trào, chỉ đạo điều hành và sự kiện toàn quốc"
                            )
                        )
                        repository.insertQuickLink(
                            QuickLinkEntity(
                                id = "ql-quanlydoanvien",
                                workspaceId = "ws_primary_doan",
                                title = "Phần mềm Quản lý Đoàn viên",
                                url = "https://quanlydoanvien.doanthanhnien.vn",
                                category = "Hệ thống",
                                description = "Tra cứu, chuyển sinh hoạt Đoàn và cơ sở dữ liệu đoàn viên"
                            )
                        )
                        repository.insertQuickLink(
                            QuickLinkEntity(
                                id = "ql-googledrive",
                                workspaceId = "ws_primary_doan",
                                title = "Google Drive Hồ sơ Công việc",
                                url = "https://drive.google.com",
                                category = "Google Drive",
                                description = "Kho lưu trữ tài liệu, kế hoạch và biểu mẫu dùng chung"
                            )
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Lỗi kiểm tra default quick links", t)
            }
        }
    }

    private fun onUserSignedIn(uid: String) {
        viewModelScope.launch {
            try {
                firestoreRepository.seedInitialDataIfEmpty(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi seed initial data", e)
            }

            // Sync Workspaces
            launch {
                firestoreRepository.observeWorkspaces().catch { e ->
                    Log.e(TAG, "Lỗi observeWorkspaces", e)
                }.collect { fWorkspaces ->
                    fWorkspaces.forEach { fw ->
                        val ws = WorkspaceEntity(
                            id = fw.id,
                            name = fw.name,
                            description = fw.description,
                            organization = fw.organization,
                            colorHex = fw.colorHex,
                            ownerName = fw.ownerName,
                            isDefault = fw.isDefault
                        )
                        repository.insertWorkspace(ws)
                    }
                }
            }

            // Sync Tasks
            launch {
                firestoreRepository.observeTasks().catch { e ->
                    Log.e(TAG, "Lỗi observeTasks", e)
                }.collect { fTasks ->
                    firestoreTasks.value = fTasks
                    val currentWs = _currentWorkspaceId.value.ifBlank {
                        workspaces.value.firstOrNull()?.id ?: "ws_primary_doan"
                    }
                    // Sync to local Room so existing UI stays in lock-step
                    val entities = fTasks.map { ft ->
                        val attachedArr = JSONArray()
                        ft.attachments.forEach { att ->
                            try {
                                if (att.trim().startsWith("{")) {
                                    attachedArr.put(JSONObject(att))
                                } else if (att.isNotBlank()) {
                                    attachedArr.put(JSONObject().apply {
                                        put("id", UUID.randomUUID().toString())
                                        put("name", att)
                                        put("uri", "")
                                        put("type", if (att.endsWith(".pdf", true)) "PDF" else "DOCX")
                                        put("category", "KE_HOACH")
                                        put("size", "1.0 MB")
                                    })
                                }
                            } catch (_: Exception) {}
                        }

                        val notesArr = JSONArray()
                        ft.notes.forEach { n ->
                            try {
                                if (n.trim().startsWith("{")) {
                                    notesArr.put(JSONObject(n))
                                } else if (n.isNotBlank()) {
                                    notesArr.put(JSONObject().apply {
                                        put("id", UUID.randomUUID().toString())
                                        put("date", "")
                                        put("content", n)
                                    })
                                }
                            } catch (_: Exception) {}
                        }

                        TaskEntity(
                            id = ft.id.ifBlank { UUID.randomUUID().toString() },
                            workspaceId = currentWs,
                            title = ft.title,
                            description = ft.description,
                            location = ft.location,
                            assignerName = ft.assignedBy,
                            responsiblePersonName = ft.assignee,
                            collaborators = ft.coordinator,
                            priority = ft.priority,
                            status = ft.status,
                            dueDate = ft.deadline,
                            progressPercent = ft.progress.toInt(),
                            attachedFilesJson = attachedArr.toString(),
                            progressNotesJson = notesArr.toString(),
                            isCompleted = (ft.status == "COMPLETED" || ft.progress >= 100L)
                        )
                    }
                    if (entities.isNotEmpty()) {
                        repository.insertTasks(entities)
                        com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())
                    }
                }
            }

            // Sync Events
            launch {
                firestoreRepository.observeEvents().catch { e ->
                    Log.e(TAG, "Lỗi observeEvents", e)
                }.collect { fEvents ->
                    firestoreEvents.value = fEvents
                    val currentWs = _currentWorkspaceId.value.ifBlank {
                        workspaces.value.firstOrNull()?.id ?: "ws_primary_doan"
                    }
                    fEvents.forEach { fe ->
                        val evEntity = CalendarEventEntity(
                            id = fe.id.ifBlank { UUID.randomUUID().toString() },
                            workspaceId = currentWs,
                            title = fe.title,
                            eventType = "MEETING",
                            date = fe.date,
                            startTime = fe.time,
                            endTime = "",
                            location = fe.location,
                            destinationUnit = fe.host,
                            contactPerson = fe.host,
                            purpose = fe.description,
                            participants = fe.attendees
                        )
                        repository.insertCalendarEvent(evEntity)
                    }
                    if (fEvents.isNotEmpty()) {
                        com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())
                    }
                }
            }

            // Sync Circulars
            launch {
                firestoreRepository.observeCirculars().catch { e ->
                    Log.e(TAG, "Lỗi observeCirculars", e)
                }.collect { fCirculars ->
                    firestoreCirculars.value = fCirculars
                    val currentWs = _currentWorkspaceId.value.ifBlank {
                        workspaces.value.firstOrNull()?.id ?: "ws_primary_doan"
                    }
                    fCirculars.forEach { fc ->
                        val doc = DocumentEntity(
                            id = fc.id.ifBlank { UUID.randomUUID().toString() },
                            workspaceId = currentWs,
                            title = fc.title,
                            docType = fc.category,
                            docNumber = fc.code,
                            issueDate = fc.issueDate,
                            issuingBody = fc.issuer,
                            summary = fc.summary
                        )
                        repository.insertDocument(doc)
                    }
                }
            }

            // Sync Advisories
            launch {
                firestoreRepository.observeAdvisories().catch { e ->
                    Log.e(TAG, "Lỗi observeAdvisories", e)
                }.collect { fAdvisories ->
                    firestoreAdvisories.value = fAdvisories
                }
            }
        }
    }

    // Workspace actions
    fun setWorkspace(workspaceId: String) {
        _currentWorkspaceId.value = workspaceId
    }

    fun createWorkspace(
        name: String,
        org: String,
        desc: String,
        colorHex: String = "#1B1464",
        ownerName: String = ""
    ) {
        viewModelScope.launch {
            val id = "ws-" + UUID.randomUUID().toString().take(8)
            val ws = WorkspaceEntity(
                id = id,
                name = name.trim(),
                organization = org.trim().ifEmpty { "Đoàn TNCS Hồ Chí Minh" },
                description = desc.trim(),
                colorHex = colorHex,
                ownerName = ownerName.trim()
            )
            repository.insertWorkspace(ws)
            _currentWorkspaceId.value = id

            if (currentUser.value != null) {
                try {
                    firestoreRepository.saveWorkspace(
                        FirestoreWorkspace(
                            id = id,
                            name = ws.name,
                            organization = ws.organization,
                            description = ws.description,
                            colorHex = ws.colorHex,
                            ownerName = ws.ownerName,
                            isDefault = false
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi lưu workspace lên Firestore", e)
                }
            }
        }
    }

    fun updateWorkspace(workspace: WorkspaceEntity) {
        viewModelScope.launch {
            repository.updateWorkspace(workspace)
            if (currentUser.value != null) {
                try {
                    firestoreRepository.saveWorkspace(
                        FirestoreWorkspace(
                            id = workspace.id,
                            name = workspace.name,
                            organization = workspace.organization,
                            description = workspace.description,
                            colorHex = workspace.colorHex,
                            ownerName = workspace.ownerName,
                            isDefault = workspace.isDefault
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi cập nhật workspace lên Firestore", e)
                }
            }
        }
    }

    fun deleteWorkspace(workspace: WorkspaceEntity) {
        viewModelScope.launch {
            repository.deleteWorkspace(workspace)
            if (_currentWorkspaceId.value == workspace.id) {
                _currentWorkspaceId.value = ""
            }
            if (currentUser.value != null) {
                try {
                    firestoreRepository.deleteWorkspace(workspace.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi xóa workspace trên Firestore", e)
                }
            }
        }
    }

    // Task actions
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val isNowCompleted = !task.isCompleted
            val newStatus = if (isNowCompleted) "COMPLETED" else "IN_PROGRESS"
            val updated = task.copy(
                isCompleted = isNowCompleted,
                status = newStatus,
                progressPercent = if (isNowCompleted) 100 else 50,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            // Sync to Firestore
            if (currentUser.value != null) {
                try {
                    val fTask = FirestoreTask(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        assignedBy = task.assignerName,
                        assignee = task.responsiblePersonName,
                        coordinator = task.collaborators,
                        deadline = task.dueDate,
                        priority = task.priority,
                        status = newStatus,
                        progress = if (isNowCompleted) 100L else 50L
                    )
                    firestoreRepository.saveTask(fTask)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi đồng bộ Firestore toggleTask", e)
                }
            }
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: String) {
        viewModelScope.launch {
            val isNowCompleted = (newStatus == "COMPLETED")
            val updated = task.copy(
                status = newStatus,
                isCompleted = isNowCompleted,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            if (currentUser.value != null) {
                try {
                    val fTask = FirestoreTask(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        assignedBy = task.assignerName,
                        assignee = task.responsiblePersonName,
                        coordinator = task.collaborators,
                        deadline = task.dueDate,
                        priority = task.priority,
                        status = newStatus,
                        progress = if (isNowCompleted) 100L else task.progressPercent.toLong()
                    )
                    firestoreRepository.saveTask(fTask)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi đồng bộ Firestore updateStatus", e)
                }
            }
        }
    }

    fun saveTask(
        id: String?,
        title: String,
        description: String,
        location: String = "",
        assignerName: String,
        responsiblePersonName: String,
        collaborators: String,
        priority: String,
        status: String,
        dueDate: String,
        dueTime: String,
        portfolioId: String,
        projectId: String,
        waitingForWho: String,
        driveLink: String,
        outputNotes: String,
        progressPercent: Int = 0,
        attachedFilesJson: String = "[]",
        progressNotesJson: String = "[]"
    ) {
        viewModelScope.launch {
            val taskId = id ?: UUID.randomUUID().toString()
            val currentWs = _currentWorkspaceId.value.ifBlank {
                workspaces.value.firstOrNull()?.id ?: "ws_primary_doan"
            }
            val task = TaskEntity(
                id = taskId,
                workspaceId = currentWs,
                title = title,
                description = description,
                location = location,
                assignerName = assignerName,
                responsiblePersonName = responsiblePersonName,
                collaborators = collaborators,
                priority = priority,
                status = status,
                dueDate = dueDate,
                dueTime = dueTime,
                portfolioId = portfolioId,
                projectId = projectId,
                waitingForWho = waitingForWho,
                driveLink = driveLink,
                outputNotes = outputNotes,
                progressPercent = progressPercent,
                attachedFilesJson = attachedFilesJson,
                progressNotesJson = progressNotesJson,
                isCompleted = (status == "COMPLETED" || progressPercent == 100),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertTask(task)

            // Auto-schedule task reminder on system
            if (dueDate.isNotBlank()) {
                com.example.ui.util.CadreSystemIntegrations.scheduleTaskReminder(
                    context = getApplication(),
                    taskId = taskId,
                    taskTitle = title,
                    dueDateStr = dueDate,
                    dueTimeStr = dueTime
                )
            }
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            // Save to Firestore
            if (currentUser.value != null) {
                try {
                    val notesList = mutableListOf<String>()
                    val attachmentsList = mutableListOf<String>()
                    try {
                        val nArr = JSONArray(progressNotesJson)
                        for (i in 0 until nArr.length()) {
                            notesList.add(nArr.get(i).toString())
                        }
                    } catch (_: Exception) {}
                    try {
                        val aArr = JSONArray(attachedFilesJson)
                        for (i in 0 until aArr.length()) {
                            attachmentsList.add(aArr.get(i).toString())
                        }
                    } catch (_: Exception) {}

                    val fTask = FirestoreTask(
                        id = taskId,
                        title = title,
                        description = description,
                        assignedBy = assignerName,
                        assignee = responsiblePersonName,
                        coordinator = collaborators,
                        deadline = dueDate,
                        location = location,
                        priority = priority,
                        status = status,
                        progress = progressPercent.toLong(),
                        notes = notesList,
                        attachments = attachmentsList
                    )
                    firestoreRepository.saveTask(fTask)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi lưu task lên Firestore", e)
                }
            }
        }
    }

    fun updateTaskProgress(task: TaskEntity, progressPercent: Int) {
        viewModelScope.launch {
            val updated = task.copy(
                progressPercent = progressPercent,
                isCompleted = progressPercent == 100,
                status = if (progressPercent == 100) "COMPLETED" else if (task.status == "TODO" && progressPercent > 0) "IN_PROGRESS" else task.status,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            if (currentUser.value != null) {
                try {
                    val fTask = FirestoreTask(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        assignedBy = task.assignerName,
                        assignee = task.responsiblePersonName,
                        coordinator = task.collaborators,
                        deadline = task.dueDate,
                        priority = task.priority,
                        status = updated.status,
                        progress = progressPercent.toLong()
                    )
                    firestoreRepository.saveTask(fTask)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi updateTaskProgress Firestore", e)
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())
            if (currentUser.value != null) {
                try {
                    firestoreRepository.deleteTask(task.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi deleteTask Firestore", e)
                }
            }
        }
    }

    fun appendTaskNote(task: TaskEntity, noteContent: String) {
        if (noteContent.isBlank()) return
        viewModelScope.launch {
            val notesArray = try {
                JSONArray(task.progressNotesJson)
            } catch (e: Exception) {
                JSONArray()
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val newNoteObj = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("date", sdf.format(Date()))
                put("content", noteContent.trim())
            }
            notesArray.put(newNoteObj)
            val updatedNotesJson = notesArray.toString()
            val updated = task.copy(
                progressNotesJson = updatedNotesJson,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            if (currentUser.value != null) {
                try {
                    val notesList = mutableListOf<String>()
                    for (i in 0 until notesArray.length()) {
                        notesList.add(notesArray.get(i).toString())
                    }
                    val fTask = FirestoreTask(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        assignedBy = task.assignerName,
                        assignee = task.responsiblePersonName,
                        coordinator = task.collaborators,
                        deadline = task.dueDate,
                        location = task.location,
                        priority = task.priority,
                        status = task.status,
                        progress = task.progressPercent.toLong(),
                        notes = notesList
                    )
                    firestoreRepository.saveTask(fTask)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi đồng bộ appendTaskNote Firestore", e)
                }
            }
        }
    }

    // Calendar actions
    fun saveCalendarEvent(
        id: String?,
        title: String,
        eventType: String,
        date: String,
        startTime: String,
        endTime: String,
        location: String,
        destinationUnit: String,
        contactPerson: String,
        contactPhone: String,
        purpose: String,
        participants: String,
        syncGoogleCalendar: Boolean = true
    ) {
        viewModelScope.launch {
            val eventId = id ?: UUID.randomUUID().toString()
            val currentWs = _currentWorkspaceId.value.ifBlank {
                workspaces.value.firstOrNull()?.id ?: "ws_primary_doan"
            }
            val event = CalendarEventEntity(
                id = eventId,
                workspaceId = currentWs,
                title = title,
                eventType = eventType,
                date = date,
                startTime = startTime,
                endTime = endTime,
                location = location,
                destinationUnit = destinationUnit,
                contactPerson = contactPerson,
                contactPhone = contactPhone,
                purpose = purpose,
                participants = participants
            )
            repository.insertCalendarEvent(event)

            // Auto-schedule event reminder on system
            if (date.isNotBlank()) {
                com.example.ui.util.CadreSystemIntegrations.scheduleEventReminder(
                    context = getApplication(),
                    eventId = eventId,
                    eventTitle = title,
                    dateStr = date,
                    timeStr = startTime,
                    location = location
                )
            }
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            // Auto-create in Google Calendar
            if (syncGoogleCalendar) {
                try {
                    val desc = buildString {
                        if (purpose.isNotBlank()) appendLine("Nội dung: $purpose")
                        if (contactPerson.isNotBlank()) appendLine("Chủ trì / Liên hệ: $contactPerson ($contactPhone)")
                        if (participants.isNotBlank()) appendLine("Thành phần: $participants")
                        if (destinationUnit.isNotBlank()) appendLine("Đơn vị: $destinationUnit")
                    }.trim()
                    com.example.ui.util.CadreSystemIntegrations.exportEventToGoogleCalendar(
                        context = getApplication(),
                        title = title,
                        description = desc,
                        location = location,
                        dateStr = date,
                        timeStr = startTime,
                        endTimeStr = endTime
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi tự động tạo sự kiện Google Lịch", e)
                }
            }

            // Save to Firestore
            if (currentUser.value != null) {
                try {
                    val fEvent = FirestoreEvent(
                        id = eventId,
                        title = title,
                        description = purpose,
                        date = date,
                        time = startTime,
                        location = location,
                        host = contactPerson.ifBlank { destinationUnit },
                        attendees = participants,
                        notes = "Liên hệ: $contactPhone"
                    )
                    firestoreRepository.saveEvent(fEvent)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi lưu event lên Firestore", e)
                }
            }
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(event)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())
            if (currentUser.value != null) {
                try {
                    firestoreRepository.deleteEvent(event.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi deleteEvent Firestore", e)
                }
            }
        }
    }

    fun markEventAttended(event: CalendarEventEntity) {
        viewModelScope.launch {
            val updated = event.copy(isAttended = true, attendedAt = System.currentTimeMillis())
            repository.updateCalendarEvent(updated)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())
            if (currentUser.value != null) {
                try {
                    val fEvent = FirestoreEvent(
                        id = updated.id,
                        title = updated.title,
                        description = updated.purpose,
                        date = updated.date,
                        time = updated.startTime,
                        location = updated.location,
                        host = updated.contactPerson,
                        attendees = updated.participants,
                        notes = "Đã tham gia lúc ${SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date())}"
                    )
                    firestoreRepository.saveEvent(fEvent)
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi cập nhật trạng thái tham gia Firestore", e)
                }
            }
        }
    }

    fun rescheduleEvent(
        event: CalendarEventEntity,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        newLocation: String
    ) {
        viewModelScope.launch {
            val updated = event.copy(
                date = newDate,
                startTime = newStartTime,
                endTime = newEndTime,
                location = newLocation.ifBlank { event.location },
                isRescheduled = true,
                isAttended = false
            )
            repository.updateCalendarEvent(updated)

            // Reschedule system reminder & update widget
            com.example.ui.util.CadreSystemIntegrations.scheduleEventReminder(
                context = getApplication(),
                eventId = updated.id,
                eventTitle = updated.title,
                dateStr = updated.date,
                timeStr = updated.startTime,
                location = updated.location
            )
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            // Update in Google Calendar
            try {
                com.example.ui.util.CadreSystemIntegrations.exportEventToGoogleCalendar(
                    context = getApplication(),
                    title = updated.title,
                    description = updated.purpose,
                    location = updated.location,
                    dateStr = updated.date,
                    timeStr = updated.startTime,
                    endTimeStr = updated.endTime
                )
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đồng bộ đổi lịch sang Google Lịch", e)
            }

            if (currentUser.value != null) {
                try {
                    val fEvent = FirestoreEvent(
                        id = updated.id,
                        title = updated.title,
                        description = updated.purpose,
                        date = updated.date,
                        time = updated.startTime,
                        location = updated.location,
                        host = updated.contactPerson,
                        attendees = updated.participants,
                        notes = "Đã đổi lịch sang $newDate $newStartTime"
                    )
                    firestoreRepository.saveEvent(fEvent)
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi cập nhật đổi lịch Firestore", e)
                }
            }
        }
    }

    fun triggerManualCloudSync() {
        val user = currentUser.value ?: try { Firebase.auth.currentUser } catch (_: Throwable) { null }
        if (user != null) {
            currentUser.value = user
            onUserSignedIn(user.uid)
        }
    }

    // Thao tác nhanh (Intelligent Quick Capture)
    fun quickCaptureParsed(entity: ParsedCadreEntity) {
        viewModelScope.launch {
            when (entity.inferredType) {
                "EVENT" -> {
                    saveCalendarEvent(
                        id = null,
                        title = entity.title,
                        eventType = "MEETING",
                        date = entity.dateString,
                        startTime = entity.timeString,
                        endTime = "",
                        location = entity.location,
                        destinationUnit = entity.assigner,
                        contactPerson = entity.responsiblePerson.ifBlank { entity.assigner },
                        contactPhone = "",
                        purpose = entity.content,
                        participants = entity.collaborators,
                        syncGoogleCalendar = true
                    )
                }
                "NOTE" -> {
                    val wsId = _currentWorkspaceId.value.ifBlank { "ws_primary_doan" }
                    val note = NoteEntity(
                        workspaceId = wsId,
                        title = entity.title.take(60),
                        content = entity.content,
                        category = "QUICK"
                    )
                    repository.insertNote(note)
                }
                else -> { // TASK
                    saveTask(
                        id = null,
                        title = entity.title,
                        description = entity.content,
                        location = entity.location,
                        assignerName = entity.assigner,
                        responsiblePersonName = entity.responsiblePerson,
                        collaborators = entity.collaborators,
                        priority = entity.priority,
                        status = "IN_PROGRESS",
                        dueDate = entity.dateString,
                        dueTime = entity.timeString,
                        portfolioId = "",
                        projectId = "",
                        waitingForWho = "",
                        driveLink = "",
                        outputNotes = if (entity.location.isNotBlank()) "Địa điểm: ${entity.location}" else "",
                        progressPercent = 0
                    )
                }
            }
        }
    }

    fun quickCapture(input: String, captureType: String) {
        val parsed = SmartCadreParser.parse(input).copy(inferredType = captureType)
        quickCaptureParsed(parsed)
    }

    fun clearAllSampleData() {
        viewModelScope.launch {
            repository.clearMockWorkspace()
        }
    }

    // People actions
    fun savePerson(
        id: String? = null,
        fullName: String,
        position: String,
        unit: String,
        phone: String,
        email: String,
        notes: String = "",
        isKeyLeader: Boolean = false
    ) {
        viewModelScope.launch {
            val wsId = _currentWorkspaceId.value.ifBlank { "ws_primary_doan" }
            val person = PersonEntity(
                id = id ?: UUID.randomUUID().toString(),
                workspaceId = wsId,
                fullName = fullName,
                position = position,
                unit = unit,
                email = email,
                phone = phone,
                notes = notes,
                isKeyLeader = isKeyLeader
            )
            repository.insertPerson(person)
        }
    }

    fun createPerson(
        fullName: String,
        position: String,
        unit: String,
        phone: String,
        email: String,
        notes: String = "",
        isKeyLeader: Boolean = false
    ) = savePerson(
        id = null,
        fullName = fullName,
        position = position,
        unit = unit,
        phone = phone,
        email = email,
        notes = notes,
        isKeyLeader = isKeyLeader
    )

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
        }
    }

    // Document / Circular actions
    fun saveDocument(
        id: String? = null,
        title: String,
        docType: String,
        docNumber: String,
        summary: String,
        fileUrl: String = "",
        versionName: String = "Bản chính thức"
    ) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val doc = DocumentEntity(
                id = id ?: UUID.randomUUID().toString(),
                workspaceId = _currentWorkspaceId.value,
                title = title,
                docType = docType,
                docNumber = docNumber,
                summary = summary,
                issuingBody = "Ban Thường vụ Đoàn",
                issueDate = todayStr,
                versionName = versionName,
                fileUrl = fileUrl
            )
            repository.insertDocument(doc)

            if (currentUser.value != null) {
                try {
                    val fCircular = FirestoreCircular(
                        id = doc.id,
                        code = docNumber,
                        title = title,
                        issuer = "Ban Thường vụ Đoàn",
                        issueDate = todayStr,
                        category = docType,
                        summary = summary,
                        fullContent = summary
                    )
                    firestoreRepository.saveCircular(fCircular)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi lưu document lên Firestore", e)
                }
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }

    // Decisions actions
    fun saveDecision(id: String? = null, title: String, content: String, decisionMaker: String, workDnaLessons: String = "") {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val decision = DecisionEntity(
                id = id ?: UUID.randomUUID().toString(),
                workspaceId = _currentWorkspaceId.value,
                title = title,
                content = content,
                decisionMaker = decisionMaker,
                date = todayStr,
                workDnaLessons = workDnaLessons
            )
            repository.insertDecision(decision)
        }
    }

    fun deleteDecision(decision: DecisionEntity) {
        viewModelScope.launch {
            repository.deleteDecision(decision)
        }
    }

    // Note actions
    fun saveNote(id: String? = null, title: String, content: String, category: String = "QUICK", isPinned: Boolean = false) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = id ?: UUID.randomUUID().toString(),
                workspaceId = _currentWorkspaceId.value,
                title = title,
                content = content,
                category = category,
                isPinned = isPinned
            )
            repository.insertNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Settlement actions (Quyết toán kinh phí)
    fun saveSettlement(
        id: String? = null,
        title: String,
        amount: Long,
        budgetEstimateCode: String,
        deadline: String,
        taskId: String = "",
        taskTitle: String = "",
        responsiblePerson: String = "",
        status: String = "PENDING",
        note: String = "",
        isCompleted: Boolean = false
    ) {
        viewModelScope.launch {
            val settlement = SettlementEntity(
                id = id ?: UUID.randomUUID().toString(),
                workspaceId = _currentWorkspaceId.value.ifBlank { "ws_primary_doan" },
                title = title,
                amount = amount,
                budgetEstimateCode = budgetEstimateCode,
                deadline = deadline,
                taskId = taskId,
                taskTitle = taskTitle,
                responsiblePerson = responsiblePerson,
                status = if (isCompleted) "COMPLETED" else status,
                note = note,
                isCompleted = isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertSettlement(settlement)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())
            com.example.ui.util.CadreSystemIntegrations.scheduleSettlementReminder(
                context = getApplication(),
                settlementId = settlement.id,
                settlementTitle = settlement.title,
                deadlineStr = settlement.deadline
            )

            // Sync to Firestore
            try {
                if (firestoreRepository.getCurrentUserId() != null) {
                    val fSettlement = com.example.data.model.FirestoreSettlement(
                        id = settlement.id,
                        userId = firestoreRepository.getCurrentUserId() ?: "",
                        workspaceId = settlement.workspaceId,
                        title = settlement.title,
                        amount = settlement.amount,
                        budgetEstimateCode = settlement.budgetEstimateCode,
                        deadline = settlement.deadline,
                        taskId = settlement.taskId,
                        taskTitle = settlement.taskTitle,
                        responsiblePerson = settlement.responsiblePerson,
                        status = settlement.status,
                        note = settlement.note,
                        isCompleted = settlement.isCompleted
                    )
                    firestoreRepository.saveSettlement(fSettlement)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi lưu settlement lên Firestore", e)
            }
        }
    }

    fun toggleSettlementCompletion(settlement: SettlementEntity) {
        viewModelScope.launch {
            val isNowCompleted = !settlement.isCompleted
            val updated = settlement.copy(
                isCompleted = isNowCompleted,
                status = if (isNowCompleted) "COMPLETED" else "PENDING",
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSettlement(updated)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            // Sync to Firestore
            try {
                if (firestoreRepository.getCurrentUserId() != null) {
                    val fSettlement = com.example.data.model.FirestoreSettlement(
                        id = updated.id,
                        userId = firestoreRepository.getCurrentUserId() ?: "",
                        workspaceId = updated.workspaceId,
                        title = updated.title,
                        amount = updated.amount,
                        budgetEstimateCode = updated.budgetEstimateCode,
                        deadline = updated.deadline,
                        taskId = updated.taskId,
                        taskTitle = updated.taskTitle,
                        responsiblePerson = updated.responsiblePerson,
                        status = updated.status,
                        note = updated.note,
                        isCompleted = updated.isCompleted
                    )
                    firestoreRepository.saveSettlement(fSettlement)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi update settlement lên Firestore", e)
            }
        }
    }

    fun deleteSettlement(settlement: SettlementEntity) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
            com.example.ui.util.CadreSystemIntegrations.updateAppWidget(getApplication())

            // Sync to Firestore
            try {
                if (firestoreRepository.getCurrentUserId() != null) {
                    firestoreRepository.deleteSettlement(settlement.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xóa settlement trên Firestore", e)
            }
        }
    }

    // Quick links (Create & Update)
    fun saveQuickLink(
        id: String? = null,
        title: String,
        url: String,
        category: String = "Hệ thống",
        desc: String = "",
        isPinned: Boolean = true
    ) {
        viewModelScope.launch {
            val linkId = if (!id.isNullOrBlank()) id else UUID.randomUUID().toString()
            val wsId = _currentWorkspaceId.value.ifBlank { "ws_primary_doan" }
            val link = QuickLinkEntity(
                id = linkId,
                workspaceId = wsId,
                title = title.trim(),
                url = url.trim(),
                category = category.trim().ifEmpty { "Hệ thống" },
                description = desc.trim(),
                isPinned = isPinned
            )
            repository.insertQuickLink(link)
        }
    }

    fun deleteQuickLink(link: QuickLinkEntity) {
        viewModelScope.launch {
            repository.deleteQuickLink(link)
        }
    }

    // ==========================================
    // CADRE AI WITH GEMINI & FIREBASE AI LOGIC
    // ==========================================
    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val currentWs = _currentWorkspaceId.value
        val mode = currentAiMode.value

        viewModelScope.launch {
            // 1. Record user message
            val userMsg = ChatMessageEntity(
                workspaceId = currentWs,
                role = "USER",
                message = prompt,
                mode = mode
            )
            repository.insertChatMessage(userMsg)

            isAiThinking.value = true

            // 2. Query Gemini with live Firestore data and document knowledge
            val aiResponse = CadreAiEngine.queryCadreAi(
                userPrompt = prompt,
                mode = mode,
                tasks = firestoreTasks.value,
                events = firestoreEvents.value,
                circulars = firestoreCirculars.value,
                advisories = firestoreAdvisories.value
            )

            isAiThinking.value = false

            // 3. Serialize actions if any
            val actionPayload = JSONObject()
            if (aiResponse.scheduleActions.isNotEmpty()) {
                val arr = JSONArray()
                aiResponse.scheduleActions.forEach { a ->
                    arr.put(JSONObject().apply {
                        put("type", "SCHEDULE")
                        put("title", a.title)
                        put("date", a.date)
                        put("time", a.time)
                        put("location", a.location)
                        put("host", a.host)
                        put("notes", a.notes)
                    })
                }
                actionPayload.put("schedules", arr)
            }
            if (aiResponse.taskActions.isNotEmpty()) {
                val arr = JSONArray()
                aiResponse.taskActions.forEach { a ->
                    arr.put(JSONObject().apply {
                        put("type", "TASK")
                        put("title", a.title)
                        put("assignedBy", a.assignedBy)
                        put("assignee", a.assignee)
                        put("coordinator", a.coordinator)
                        put("deadline", a.deadline)
                        put("priority", a.priority)
                        put("description", a.description)
                    })
                }
                actionPayload.put("tasks", arr)
            }
            if (aiResponse.advisoryActions.isNotEmpty()) {
                val arr = JSONArray()
                aiResponse.advisoryActions.forEach { a ->
                    arr.put(JSONObject().apply {
                        put("type", "ADVISORY")
                        put("title", a.title)
                        put("targetOrg", a.targetOrg)
                        put("basis", a.basis)
                        put("content", a.content)
                    })
                }
                actionPayload.put("advisories", arr)
            }

            // 4. Save AI Response
            val aiMsg = ChatMessageEntity(
                workspaceId = currentWs,
                role = "CADRE_AI",
                message = aiResponse.cleanDisplayText,
                mode = mode,
                actionSuggestedJson = if (actionPayload.length() > 0) actionPayload.toString() else ""
            )
            repository.insertChatMessage(aiMsg)
        }
    }

    // Apply Actions generated by AI directly into Firestore & Room
    fun applyScheduleFromAi(title: String, date: String, time: String, location: String, host: String, notes: String) {
        saveCalendarEvent(
            id = null,
            title = title,
            eventType = "MEETING",
            date = date,
            startTime = time,
            endTime = "",
            location = location,
            destinationUnit = host,
            contactPerson = host,
            contactPhone = "",
            purpose = notes,
            participants = "Thành phần theo chỉ đạo"
        )
    }

    fun applyTaskFromAi(title: String, assignedBy: String, assignee: String, coordinator: String, deadline: String, priority: String, description: String) {
        saveTask(
            id = null,
            title = title,
            description = description,
            assignerName = assignedBy,
            responsiblePersonName = assignee,
            collaborators = coordinator,
            priority = priority,
            status = "IN_PROGRESS",
            dueDate = deadline,
            dueTime = "17:00",
            portfolioId = "TG-01",
            projectId = "",
            waitingForWho = "",
            driveLink = "",
            outputNotes = "",
            progressPercent = 0
        )
    }

    fun applyAdvisoryFromAi(title: String, targetOrg: String, basis: String, content: String) {
        viewModelScope.launch {
            if (currentUser.value != null) {
                val adv = FirestoreAdvisory(
                    title = title,
                    targetOrg = targetOrg,
                    basis = basis,
                    content = content
                )
                firestoreRepository.saveAdvisory(adv)
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatMessages(_currentWorkspaceId.value)
        }
    }
}
