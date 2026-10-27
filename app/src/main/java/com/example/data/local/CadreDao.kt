package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CadreDao {
    // Workspaces
    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Delete
    suspend fun deleteWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteWorkspaceById(id: String)

    @Query("DELETE FROM workspaces WHERE id = 'ws-doanthanhnien-default'")
    suspend fun clearMockWorkspace()

    @Query("DELETE FROM workspaces")
    suspend fun clearAllWorkspaces()

    // People
    @Query("SELECT * FROM people WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY isKeyLeader DESC, fullName ASC")
    fun getPeople(workspaceId: String): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    // Portfolios
    @Query("SELECT * FROM portfolios WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY code ASC")
    fun getPortfolios(workspaceId: String): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(portfolio: PortfolioEntity)

    @Delete
    suspend fun deletePortfolio(portfolio: PortfolioEntity)

    // Projects
    @Query("SELECT * FROM projects WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY startDate DESC")
    fun getProjects(workspaceId: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    // Tasks
    @Query("SELECT * FROM tasks WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY isCompleted ASC, priority ASC, dueDate ASC")
    fun getTasks(workspaceId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority ASC, dueDate ASC")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM calendar_events ORDER BY date ASC, startTime ASC")
    suspend fun getAllCalendarEventsSync(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // Calendar & Field Visits
    @Query("SELECT * FROM calendar_events WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY date ASC, startTime ASC")
    fun getCalendarEvents(workspaceId: String): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity)

    @Update
    suspend fun updateCalendarEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteCalendarEvent(event: CalendarEventEntity)

    // Notes
    @Query("SELECT * FROM notes WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotes(workspaceId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Documents
    @Query("SELECT * FROM documents WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY issueDate DESC")
    fun getDocuments(workspaceId: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    // Decisions
    @Query("SELECT * FROM decisions WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY date DESC")
    fun getDecisions(workspaceId: String): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntity)

    @Delete
    suspend fun deleteDecision(decision: DecisionEntity)

    // Settlements (Quyết toán kinh phí)
    @Query("SELECT * FROM settlements WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY isCompleted ASC, deadline ASC, createdAt DESC")
    fun getSettlements(workspaceId: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getAllSettlementsSync(): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)

    @Delete
    suspend fun deleteSettlement(settlement: SettlementEntity)

    // Quick Links
    @Query("SELECT * FROM quick_links WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY isPinned DESC, title ASC")
    fun getQuickLinks(workspaceId: String): Flow<List<QuickLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickLink(quickLink: QuickLinkEntity)

    @Delete
    suspend fun deleteQuickLink(quickLink: QuickLinkEntity)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE (:workspaceId = '' OR :workspaceId = 'ALL' OR workspaceId = :workspaceId OR workspaceId = '' OR workspaceId = 'ws_primary_doan' OR workspaceId IS NULL) ORDER BY timestamp ASC")
    fun getChatMessages(workspaceId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE workspaceId = :workspaceId")
    suspend fun clearChatMessages(workspaceId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("DELETE FROM calendar_events")
    suspend fun clearAllCalendarEvents()

    @Query("DELETE FROM people")
    suspend fun clearAllPeople()

    @Query("DELETE FROM documents")
    suspend fun clearAllDocuments()

    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()

    @Query("DELETE FROM portfolios")
    suspend fun clearAllPortfolios()

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    @Query("DELETE FROM decisions")
    suspend fun clearAllDecisions()
}
