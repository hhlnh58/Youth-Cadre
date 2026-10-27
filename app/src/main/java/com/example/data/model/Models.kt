package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val organization: String = "Đoàn TNCS Hồ Chí Minh",
    val colorHex: String = "#497BFF",
    val ownerName: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val fullName: String,
    val position: String, // Bí thư, Phó Bí thư, Trưởng ban, Phó Trưởng ban, Tổ trưởng, Cán bộ, Chuyên viên, Đầu mối phối hợp
    val unit: String, // Ban Tuyên giáo, Ban Phong trào, Đoàn Cơ sở, Văn phòng Đoàn...
    val email: String = "",
    val phone: String = "",
    val contactChannel: String = "Zalo / Điện thoại",
    val assignedPortfolios: String = "", // Comma separated
    val notes: String = "",
    val isKeyLeader: Boolean = false,
    val avatarColorHex: String = "#497BFF"
)

@Entity(tableName = "portfolios")
data class PortfolioEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val name: String,
    val code: String,
    val description: String = "",
    val leadPersonName: String = "",
    val yearTarget: String = "Năm 2026 - 2027",
    val colorHex: String = "#497BFF",
    val iconName: String = "flag"
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val portfolioId: String = "",
    val name: String,
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "IN_PROGRESS", // PLANNING, IN_PROGRESS, REVIEW, COMPLETED
    val progressPercentage: Int = 0,
    val leaderName: String = "",
    val year: String = "2026"
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val portfolioId: String = "",
    val projectId: String = "",
    val title: String,
    val description: String = "",
    val assignerName: String = "", // Người giao: e.g. Bí thư Thành đoàn, Tổ trưởng
    val responsiblePersonName: String = "", // Người phụ trách chính
    val collaborators: String = "", // Người phối hợp
    val approverName: String = "", // Người duyệt / trình
    val priority: String = "P2", // P1 (Khẩn cấp), P2 (Cao), P3 (Bình thường), P4 (Thấp)
    val status: String = "TODO", // TODO, IN_PROGRESS, WAITING_FOR_OTHERS, SUBMITTED_FOR_APPROVAL, COMPLETED, CANCELLED
    val taskType: String = "THUONG_XUYEN", // THUONG_XUYEN, DINH_KY, DOT_XUAT, TRONG_TAM
    val dueDate: String = "", // YYYY-MM-DD or readable
    val dueTime: String = "17:00",
    val location: String = "", // Địa điểm / Đơn vị thực hiện
    val checklistsJson: String = "[]", // Serialized list of items
    val waitingForWho: String = "", // Đang chờ ai phản hồi / góp ý
    val waitingSince: String = "",
    val driveLink: String = "",
    val outputNotes: String = "",
    val tags: String = "",
    val progressPercent: Int = 0, // 0 - 100%
    val attachedFilesJson: String = "[]", // List of attached documents (Word, PDF...)
    val progressNotesJson: String = "[]", // List of progress updates timeline
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

data class AttachedDocItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uri: String = "",
    val type: String = "PDF", // PDF, DOCX, XLSX
    val category: String = "KE_HOACH", // KE_HOACH, CONG_VAN, TO_TRINH, BAO_CAO, THONG_BAO, PHIEU_CHUYEN, HUONG_DAN, QUYET_DINH, KICH_BAN
    val size: String = ""
)

data class ProgressNoteItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // dd/MM/yyyy HH:mm
    val content: String
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val eventType: String = "MEETING", // MEETING, FIELD_VISIT, WORK_SESSION, EVENT, DEADLINE
    val date: String, // YYYY-MM-DD
    val startTime: String = "08:00",
    val endTime: String = "10:00",
    val location: String = "",
    val participants: String = "",
    val destinationUnit: String = "", // Cho đi cơ sở
    val contactPerson: String = "",
    val contactPhone: String = "",
    val purpose: String = "",
    val checklistJson: String = "[]",
    val isCompleted: Boolean = false,
    val isAttended: Boolean = false,
    val attendedAt: Long = 0L,
    val isRescheduled: Boolean = false
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String, // Nội dung quyết toán
    val amount: Long, // Số tiền (VNĐ)
    val budgetEstimateCode: String = "", // Dự trù kinh phí số mấy (e.g. 05/DT-ĐTN)
    val deadline: String = "", // Hạn quyết toán (dd/MM/yyyy hoặc YYYY-MM-DD)
    val taskId: String = "", // Gắn vào nhiệm vụ nào nếu có
    val taskTitle: String = "",
    val responsiblePerson: String = "", // Cán bộ phụ trách chứng từ
    val status: String = "PENDING", // PENDING (Chờ quyết toán), PROCESSING (Đang xử lý), COMPLETED (Đã quyết toán)
    val note: String = "", // Ghi chú thêm
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val content: String,
    val category: String = "QUICK", // QUICK, MEETING, FIELD, PERSONAL
    val isPinned: Boolean = false,
    val tags: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val docType: String = "KE_HOACH", // KE_HOACH, CONG_VAN, BAO_CAO, TO_TRINH, HUONG_DAN, KICH_BAN, BAI_PHAT_BIEU
    val docNumber: String = "",
    val issueDate: String = "",
    val issuingBody: String = "",
    val versionName: String = "Bản chính thức", // Dự thảo 1, Bản tiếp thu, Bản trình ký, Bản chính thức
    val summary: String = "",
    val fileUrl: String = "",
    val isAiIndexed: Boolean = true,
    val securityLevel: String = "Nội bộ"
)

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val content: String,
    val date: String = "",
    val decisionMaker: String = "Hội nghị Ban Thường vụ",
    val scope: String = "Toàn Đoàn",
    val status: String = "DANG_HIEU_LUC", // DANG_HIEU_LUC, HET_HIEU_LUC, THAY_THE
    val workDnaLessons: String = "" // Bài học kinh nghiệm, lưu ý điều hành
)

@Entity(tableName = "quick_links")
data class QuickLinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val url: String,
    val category: String = "Hệ thống", // Cổng thông tin, Biểu mẫu, Google Drive, Dashboard
    val description: String = "",
    val isPinned: Boolean = true
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val role: String, // USER, CADRE_AI
    val message: String,
    val mode: String = "ASSISTANT", // ASSISTANT, PLANNER, RESEARCH, DRAFTING, REVIEW, EXECUTIVE_BRIEF
    val timestamp: Long = System.currentTimeMillis(),
    val actionSuggestedJson: String = "" // Optional JSON for quick tasks or actions
)

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)
