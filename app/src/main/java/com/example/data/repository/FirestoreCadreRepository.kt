package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

import com.google.firebase.auth.FirebaseAuth

class FirestoreCadreRepository(
    private val context: Context,
    customDb: FirebaseFirestore? = null,
    customAuth: FirebaseAuth? = null
) {
    private val db: FirebaseFirestore = customDb ?: try {
        val dbId = context.getString(R.string.firestore_database_id)
        if (dbId.isNotBlank()) FirebaseFirestore.getInstance(dbId) else FirebaseFirestore.getInstance()
    } catch (_: Throwable) {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            FirebaseFirestore.getInstance()
        }
    }

    private val auth: FirebaseAuth = customAuth ?: try {
        FirebaseAuth.getInstance(db.app)
    } catch (_: Throwable) {
        try {
            Firebase.auth
        } catch (_: Throwable) {
            FirebaseAuth.getInstance()
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Cán bộ chưa đăng nhập Google để truy cập dữ liệu đám mây.")
    }

    // TASKS STREAM
    fun observeTasks(): Flow<List<FirestoreTask>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreTask::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveTask(task: FirestoreTask) {
        val uid = requireUserId()
        val tasksRef = db.collection("users").document(uid).collection("tasks")
        val docRef = if (task.id.isNotBlank()) tasksRef.document(task.id) else tasksRef.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to task.title,
            "description" to task.description,
            "assignedBy" to task.assignedBy.ifBlank { "Lãnh đạo đơn vị" },
            "assignee" to task.assignee.ifBlank { "Cán bộ chuyên trách" },
            "coordinator" to task.coordinator,
            "deadline" to task.deadline.ifBlank { "30/10/2026" },
            "location" to task.location,
            "priority" to task.priority,
            "status" to task.status,
            "progress" to task.progress,
            "notes" to task.notes,
            "attachments" to task.attachments,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (task.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (task.id.isBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // EVENTS STREAM
    fun observeEvents(): Flow<List<FirestoreEvent>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/events"
        emitAll(
            db.collection("users").document(uid).collection("events")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreEvent::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveEvent(event: FirestoreEvent) {
        val uid = requireUserId()
        val eventsRef = db.collection("users").document(uid).collection("events")
        val docRef = if (event.id.isNotBlank()) eventsRef.document(event.id) else eventsRef.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to event.title,
            "description" to event.description,
            "date" to event.date,
            "time" to event.time,
            "location" to event.location.ifBlank { "Phòng họp cơ quan" },
            "host" to event.host,
            "attendees" to event.attendees,
            "notes" to event.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (event.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (event.id.isBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteEvent(eventId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("events").document(eventId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // CIRCULARS STREAM
    fun observeCirculars(): Flow<List<FirestoreCircular>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/circulars"
        emitAll(
            db.collection("users").document(uid).collection("circulars")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreCircular::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveCircular(circular: FirestoreCircular) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("circulars")
        val docRef = if (circular.id.isNotBlank()) ref.document(circular.id) else ref.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "code" to circular.code,
            "title" to circular.title,
            "issuer" to circular.issuer,
            "issueDate" to circular.issueDate,
            "category" to circular.category,
            "summary" to circular.summary,
            "keyRules" to circular.keyRules,
            "fullContent" to circular.fullContent,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (circular.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // ADVISORIES STREAM
    fun observeAdvisories(): Flow<List<FirestoreAdvisory>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/advisories"
        emitAll(
            db.collection("users").document(uid).collection("advisories")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreAdvisory::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveAdvisory(advisory: FirestoreAdvisory) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("advisories")
        val docRef = if (advisory.id.isNotBlank()) ref.document(advisory.id) else ref.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to advisory.title,
            "targetOrg" to advisory.targetOrg,
            "basis" to advisory.basis,
            "content" to advisory.content,
            "actionPlan" to advisory.actionPlan,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (advisory.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // WORKSPACES STREAM
    fun observeWorkspaces(): Flow<List<FirestoreWorkspace>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/workspaces"
        emitAll(
            db.collection("users").document(uid).collection("workspaces")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreWorkspace::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveWorkspace(workspace: FirestoreWorkspace) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("workspaces")
        val docRef = if (workspace.id.isNotBlank()) ref.document(workspace.id) else ref.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "name" to workspace.name,
            "description" to workspace.description,
            "organization" to workspace.organization,
            "colorHex" to workspace.colorHex,
            "ownerName" to workspace.ownerName,
            "isDefault" to workspace.isDefault,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (workspace.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorkspace(workspaceId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workspaces").document(workspaceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // SETTLEMENTS STREAM
    fun observeSettlements(): Flow<List<FirestoreSettlement>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/settlements"
        emitAll(
            db.collection("users").document(uid).collection("settlements")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreSettlement::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveSettlement(settlement: FirestoreSettlement) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("settlements")
        val docRef = if (settlement.id.isNotBlank()) ref.document(settlement.id) else ref.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "workspaceId" to settlement.workspaceId,
            "title" to settlement.title,
            "amount" to settlement.amount,
            "budgetEstimateCode" to settlement.budgetEstimateCode,
            "deadline" to settlement.deadline,
            "taskId" to settlement.taskId,
            "taskTitle" to settlement.taskTitle,
            "responsiblePerson" to settlement.responsiblePerson,
            "status" to settlement.status,
            "note" to settlement.note,
            "isCompleted" to settlement.isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (settlement.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSettlement(settlementId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settlements").document(settlementId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // PEOPLE STREAM
    fun observePeople(): Flow<List<FirestorePerson>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/people"
        emitAll(
            db.collection("users").document(uid).collection("people")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestorePerson::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun savePerson(person: FirestorePerson) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("people")
        val docRef = if (person.id.isNotBlank()) ref.document(person.id) else ref.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "workspaceId" to person.workspaceId,
            "fullName" to person.fullName,
            "position" to person.position,
            "unit" to person.unit,
            "email" to person.email,
            "phone" to person.phone,
            "contactChannel" to person.contactChannel,
            "assignedPortfolios" to person.assignedPortfolios,
            "notes" to person.notes,
            "isKeyLeader" to person.isKeyLeader,
            "avatarColorHex" to person.avatarColorHex,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (person.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (person.id.isBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePerson(personId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("people").document(personId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // NOTES STREAM
    fun observeNotes(): Flow<List<FirestoreNote>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/notes"
        emitAll(
            db.collection("users").document(uid).collection("notes")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreNote::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveNote(note: FirestoreNote) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("notes")
        val docRef = if (note.id.isNotBlank()) ref.document(note.id) else ref.document()

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "workspaceId" to note.workspaceId,
            "title" to note.title,
            "content" to note.content,
            "category" to note.category,
            "isPinned" to note.isPinned,
            "tags" to note.tags,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (note.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (note.id.isBlank()) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNote(noteId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Wipe mock data: no sample tasks or sample events seeded
    suspend fun seedInitialDataIfEmpty(uid: String) {
        // Pristine empty state for real user data
    }
}
