package com.example.data.repository

import android.content.Context
import com.example.data.local.CadreDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CadreRepository(context: Context) {
    private val database = CadreDatabase.getDatabase(context)
    private val dao = database.cadreDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            checkAndSeedData()
        }
    }

    // Workspaces
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>> = dao.getAllWorkspaces()
    suspend fun insertWorkspace(workspace: WorkspaceEntity) = dao.insertWorkspace(workspace)
    suspend fun updateWorkspace(workspace: WorkspaceEntity) = dao.updateWorkspace(workspace)
    suspend fun deleteWorkspace(workspace: WorkspaceEntity) = dao.deleteWorkspace(workspace)

    // People
    fun getPeople(workspaceId: String): Flow<List<PersonEntity>> = dao.getPeople(workspaceId)
    suspend fun insertPerson(person: PersonEntity) = dao.insertPerson(person)
    suspend fun updatePerson(person: PersonEntity) = dao.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = dao.deletePerson(person)

    // Portfolios
    fun getPortfolios(workspaceId: String): Flow<List<PortfolioEntity>> = dao.getPortfolios(workspaceId)
    suspend fun insertPortfolio(portfolio: PortfolioEntity) = dao.insertPortfolio(portfolio)
    suspend fun deletePortfolio(portfolio: PortfolioEntity) = dao.deletePortfolio(portfolio)

    // Projects
    fun getProjects(workspaceId: String): Flow<List<ProjectEntity>> = dao.getProjects(workspaceId)
    suspend fun insertProject(project: ProjectEntity) = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(project: ProjectEntity) = dao.deleteProject(project)

    // Tasks
    fun getTasks(workspaceId: String): Flow<List<TaskEntity>> = dao.getTasks(workspaceId)
    suspend fun getTaskById(taskId: String): TaskEntity? = dao.getTaskById(taskId)
    suspend fun insertTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun insertTasks(tasks: List<TaskEntity>) = dao.insertTasks(tasks)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = dao.deleteTask(task)
    suspend fun clearAllTasks() = dao.clearAllTasks()

    // Calendar
    fun getCalendarEvents(workspaceId: String): Flow<List<CalendarEventEntity>> = dao.getCalendarEvents(workspaceId)
    suspend fun insertCalendarEvent(event: CalendarEventEntity) = dao.insertCalendarEvent(event)
    suspend fun updateCalendarEvent(event: CalendarEventEntity) = dao.updateCalendarEvent(event)
    suspend fun deleteCalendarEvent(event: CalendarEventEntity) = dao.deleteCalendarEvent(event)
    suspend fun clearAllCalendarEvents() = dao.clearAllCalendarEvents()

    // Notes
    fun getNotes(workspaceId: String): Flow<List<NoteEntity>> = dao.getNotes(workspaceId)
    suspend fun insertNote(note: NoteEntity) = dao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = dao.deleteNote(note)

    // Documents
    fun getDocuments(workspaceId: String): Flow<List<DocumentEntity>> = dao.getDocuments(workspaceId)
    suspend fun insertDocument(document: DocumentEntity) = dao.insertDocument(document)
    suspend fun deleteDocument(document: DocumentEntity) = dao.deleteDocument(document)

    // Decisions
    fun getDecisions(workspaceId: String): Flow<List<DecisionEntity>> = dao.getDecisions(workspaceId)
    suspend fun insertDecision(decision: DecisionEntity) = dao.insertDecision(decision)
    suspend fun deleteDecision(decision: DecisionEntity) = dao.deleteDecision(decision)

    // Settlements
    fun getSettlements(workspaceId: String): Flow<List<SettlementEntity>> = dao.getSettlements(workspaceId)
    suspend fun getAllSettlementsSync(): List<SettlementEntity> = dao.getAllSettlementsSync()
    suspend fun insertSettlement(settlement: SettlementEntity) = dao.insertSettlement(settlement)
    suspend fun updateSettlement(settlement: SettlementEntity) = dao.updateSettlement(settlement)
    suspend fun deleteSettlement(settlement: SettlementEntity) = dao.deleteSettlement(settlement)

    // Sync helpers
    suspend fun getAllPeopleSync(): List<PersonEntity> = dao.getAllPeopleSync()
    suspend fun getAllNotesSync(): List<NoteEntity> = dao.getAllNotesSync()
    suspend fun getAllTasksSync(): List<TaskEntity> = dao.getAllTasksSync()
    suspend fun getAllCalendarEventsSync(): List<CalendarEventEntity> = dao.getAllCalendarEventsSync()

    // Quick Links
    fun getQuickLinks(workspaceId: String): Flow<List<QuickLinkEntity>> = dao.getQuickLinks(workspaceId)
    suspend fun insertQuickLink(quickLink: QuickLinkEntity) = dao.insertQuickLink(quickLink)
    suspend fun deleteQuickLink(quickLink: QuickLinkEntity) = dao.deleteQuickLink(quickLink)

    // Chat
    fun getChatMessages(workspaceId: String): Flow<List<ChatMessageEntity>> = dao.getChatMessages(workspaceId)
    suspend fun insertChatMessage(message: ChatMessageEntity) = dao.insertChatMessage(message)
    suspend fun clearChatMessages(workspaceId: String) = dao.clearChatMessages(workspaceId)

    suspend fun deleteWorkspaceById(id: String) = dao.deleteWorkspaceById(id)

    private suspend fun checkAndSeedData() {
        // Clear old template mock/default workspaces so user creates their own
        dao.clearMockWorkspace()
    }

    suspend fun clearMockWorkspace() {
        dao.clearMockWorkspace()
    }
}
