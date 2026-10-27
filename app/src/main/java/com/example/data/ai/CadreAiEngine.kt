package com.example.data.ai

import android.util.Log
import com.example.data.model.FirestoreAdvisory
import com.example.data.model.FirestoreCircular
import com.example.data.model.FirestoreEvent
import com.example.data.model.FirestoreTask
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CadreAiEngine"

object CadreAiEngine {

    data class AiScheduleAction(
        val title: String,
        val date: String,
        val time: String,
        val location: String,
        val host: String,
        val notes: String = ""
    )

    data class AiTaskAction(
        val title: String,
        val assignedBy: String,
        val assignee: String,
        val coordinator: String = "",
        val deadline: String,
        val priority: String = "Cao",
        val description: String = ""
    )

    data class AiAdvisoryAction(
        val title: String,
        val targetOrg: String,
        val basis: String,
        val content: String
    )

    data class CadreAiResponse(
        val rawReply: String,
        val cleanDisplayText: String,
        val scheduleActions: List<AiScheduleAction> = emptyList(),
        val taskActions: List<AiTaskAction> = emptyList(),
        val advisoryActions: List<AiAdvisoryAction> = emptyList()
    )

    // Lazy initialization of Firebase AI Logic model as mandated
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun queryCadreAi(
        userPrompt: String,
        mode: String,
        tasks: List<FirestoreTask>,
        events: List<FirestoreEvent>,
        circulars: List<FirestoreCircular>,
        advisories: List<FirestoreAdvisory>
    ): CadreAiResponse {
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Build rich contextual snapshot from real Firestore repositories
        val contextPrompt = buildString {
            appendLine("=== THÔNG TIN BỐI CẢNH ĐƠN VỊ ĐOÀN HIỆN TẠI (Hôm nay là ngày: $todayStr) ===")
            appendLine("1. KHO VĂN BẢN & HƯỚNG DẪN CỦA ĐOÀN TRONG HỆ THỐNG:")
            if (circulars.isEmpty()) {
                appendLine("- Chưa có văn bản tải lên.")
            } else {
                circulars.forEach { c ->
                    appendLine("- [${c.code}] ${c.title} (${c.category} do ${c.issuer} ban hành). Tóm tắt: ${c.summary}. Điểm cốt lõi: ${c.keyRules.joinToString("; ")}")
                }
            }

            appendLine("\n2. CÁC CÔNG VIỆC, NHIỆM VỤ ĐANG TRIỂN KHAI:")
            if (tasks.isEmpty()) {
                appendLine("- Chưa có công việc nào trong hệ thống.")
            } else {
                tasks.forEach { t ->
                    appendLine("- [${t.priority}][${t.status}] ${t.title} (Giao bởi: ${t.assignedBy}, Phụ trách: ${t.assignee}, Phối hợp: ${t.coordinator}, Hạn: ${t.deadline}, Tiến độ: ${t.progress}%). Ghi chú: ${t.notes.joinToString("; ")}")
                }
            }

            appendLine("\n3. LỊCH TRÌNH CÔNG TÁC & CUỘC HỌP ĐÃ XẾP:")
            if (events.isEmpty()) {
                appendLine("- Hiện chưa có lịch công tác nào.")
            } else {
                events.forEach { e ->
                    appendLine("- Ngày ${e.date} lúc ${e.time}: ${e.title} tại '${e.location}' (Chủ trì: ${e.host}, Thành phần: ${e.attendees}). Ghi chú: ${e.notes}")
                }
            }

            appendLine("\nCHẾ ĐỘ YÊU CẦU: $mode")
            appendLine("NỘI DUNG YÊU CẦU CỦA ĐỒNG CHÍ CÁN BỘ:")
            appendLine(userPrompt)
            appendLine("\nHÃY THỰC HIỆN:")
            appendLine("- Trả lời chuẩn mực tác phong cán bộ Đoàn, chuyên sâu, phân tích đúng tình hình thực tế từ các danh mục trên.")
            appendLine("- Nếu cần tạo lịch, xuất khối: [ACTION_SCHEDULE: {\"title\": \"...\", \"date\": \"dd/MM/yyyy\", \"time\": \"HH:mm\", \"location\": \"...\", \"host\": \"...\", \"notes\": \"...\"}]")
            appendLine("- Nếu cần tạo nhiệm vụ mới, xuất khối: [ACTION_TASK: {\"title\": \"...\", \"assignedBy\": \"...\", \"assignee\": \"...\", \"coordinator\": \"...\", \"deadline\": \"dd/MM/yyyy\", \"priority\": \"Khẩn cấp/Cao/Trung bình/Thấp\", \"description\": \"...\"}]")
            appendLine("- Nếu cần tham mưu tờ trình / đề xuất, xuất khối: [ACTION_ADVISORY: {\"title\": \"...\", \"targetOrg\": \"...\", \"basis\": \"...\", \"content\": \"...\"}]")
        }

        try {
            // Mandated timeout between 30 and 60 seconds
            val response = withTimeout(55_000L) {
                model.generateContent(
                    content {
                        text(
                            "Bạn là Cadre AI - Trợ lý & Cố vấn Chiến lược Đoàn TNCS Hồ Chí Minh đỉnh cao. " +
                            "Bạn có năng lực đọc hiểu toàn bộ kho văn bản chỉ đạo, lịch công tác và cơ sở dữ liệu nhiệm vụ. " +
                            "Khi tham mưu, bạn trích dẫn chính xác số hiệu văn bản, tên cán bộ phụ trách, cảnh báo tiến độ và tự động lên lịch trình công tác hoặc phân công nhiệm vụ chi tiết."
                        )
                        text(contextPrompt)
                    }
                )
            }

            val rawText = response.text ?: "Không có phản hồi từ máy chủ trí tuệ nhân tạo."
            return parseAiResponse(rawText)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gọi Firebase AI Logic", e)
            return CadreAiResponse(
                rawReply = "Lỗi kết nối Cadre AI: ${e.localizedMessage ?: "Vui lòng kiểm tra kết nối mạng và thử lại."}",
                cleanDisplayText = "⚠️ Không thể kết nối với Cadre AI: ${e.localizedMessage ?: "Lỗi xử lý"}. Vui lòng thử lại sau giây lát."
            )
        }
    }

    private fun parseAiResponse(rawText: String): CadreAiResponse {
        val scheduleActions = mutableListOf<AiScheduleAction>()
        val taskActions = mutableListOf<AiTaskAction>()
        val advisoryActions = mutableListOf<AiAdvisoryAction>()

        var cleanText = rawText

        // Extract [ACTION_SCHEDULE: {...}]
        val scheduleRegex = Regex("\\[ACTION_SCHEDULE:\\s*(\\{.*?\\})\\]", RegexOption.DOT_MATCHES_ALL)
        scheduleRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                scheduleActions.add(
                    AiScheduleAction(
                        title = json.optString("title", "Cuộc họp công tác"),
                        date = json.optString("date", "28/10/2026"),
                        time = json.optString("time", "08:30"),
                        location = json.optString("location", "Phòng họp cơ quan"),
                        host = json.optString("host", "Ban Thường vụ"),
                        notes = json.optString("notes", "")
                    )
                )
            } catch (ex: Exception) {
                Log.w(TAG, "Lỗi parse ACTION_SCHEDULE", ex)
            }
        }
        cleanText = scheduleRegex.replace(cleanText, "")

        // Extract [ACTION_TASK: {...}]
        val taskRegex = Regex("\\[ACTION_TASK:\\s*(\\{.*?\\})\\]", RegexOption.DOT_MATCHES_ALL)
        taskRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                taskActions.add(
                    AiTaskAction(
                        title = json.optString("title", "Nhiệm vụ mới"),
                        assignedBy = json.optString("assignedBy", "Thường trực Thành Đoàn"),
                        assignee = json.optString("assignee", "Cán bộ chuyên trách"),
                        coordinator = json.optString("coordinator", ""),
                        deadline = json.optString("deadline", "30/10/2026"),
                        priority = json.optString("priority", "Cao"),
                        description = json.optString("description", "")
                    )
                )
            } catch (ex: Exception) {
                Log.w(TAG, "Lỗi parse ACTION_TASK", ex)
            }
        }
        cleanText = taskRegex.replace(cleanText, "")

        // Extract [ACTION_ADVISORY: {...}]
        val advisoryRegex = Regex("\\[ACTION_ADVISORY:\\s*(\\{.*?\\})\\]", RegexOption.DOT_MATCHES_ALL)
        advisoryRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                advisoryActions.add(
                    AiAdvisoryAction(
                        title = json.optString("title", "Tờ trình tham mưu"),
                        targetOrg = json.optString("targetOrg", "Thường trực Cấp ủy"),
                        basis = json.optString("basis", ""),
                        content = json.optString("content", "")
                    )
                )
            } catch (ex: Exception) {
                Log.w(TAG, "Lỗi parse ACTION_ADVISORY", ex)
            }
        }
        cleanText = advisoryRegex.replace(cleanText, "")

        return CadreAiResponse(
            rawReply = rawText,
            cleanDisplayText = cleanText.trim(),
            scheduleActions = scheduleActions,
            taskActions = taskActions,
            advisoryActions = advisoryActions
        )
    }
}
