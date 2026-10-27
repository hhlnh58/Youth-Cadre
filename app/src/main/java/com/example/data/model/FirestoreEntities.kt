package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val assignedBy: String = "",
    val assignee: String = "",
    val coordinator: String = "",
    val deadline: String = "", // dd/MM/yyyy
    val location: String = "",
    val priority: String = "P2", // Khẩn cấp (P1), Cao (P2), Trung bình (P3), Thấp (P4)
    val status: String = "IN_PROGRESS", // NOT_STARTED, IN_PROGRESS, WAITING_APPROVAL, COMPLETED
    val progress: Long = 0L,
    val notes: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "", // dd/MM/yyyy
    val time: String = "", // HH:mm
    val location: String = "",
    val host: String = "",
    val attendees: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCircular(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val title: String = "",
    val issuer: String = "",
    val issueDate: String = "",
    val category: String = "",
    val summary: String = "",
    val keyRules: List<String> = emptyList(),
    val fullContent: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAdvisory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetOrg: String = "",
    val basis: String = "",
    val content: String = "",
    val actionPlan: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreWorkspace(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val organization: String = "",
    val colorHex: String = "#1B1464",
    val ownerName: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSettlement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workspaceId: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val budgetEstimateCode: String = "",
    val deadline: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val responsiblePerson: String = "",
    val status: String = "PENDING",
    val note: String = "",
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

