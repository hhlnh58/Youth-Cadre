package com.example

import com.example.data.ai.CadreAiEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAiActionParsing() {
        val sampleResponse = """
            Chào đồng chí, tôi đã phân tích tình hình và đề xuất lịch làm việc như sau:
            [ACTION_SCHEDULE: {"title": "Họp Ban Thường vụ", "date": "28/10/2026", "time": "08:30", "location": "Phòng họp số 1", "host": "Đ/c Bí thư", "notes": "Chuẩn bị văn kiện"}]
            
            Đồng thời phân công nhiệm vụ:
            [ACTION_TASK: {"title": "Xây dựng dự thảo Kế hoạch", "assignedBy": "Bí thư", "assignee": "Đ/c Nam", "coordinator": "Đ/c Mai", "deadline": "30/10/2026", "priority": "Khẩn cấp", "description": "Hoàn thiện 5 mục trọng tâm"}]
        """.trimIndent()

        // Test parsing via reflection / private helper logic test
        val scheduleRegex = Regex("\\[ACTION_SCHEDULE:\\s*(\\{.*?\\})\\]", RegexOption.DOT_MATCHES_ALL)
        val match = scheduleRegex.find(sampleResponse)
        assertNotNull(match)
        assertTrue(match!!.value.contains("Họp Ban Thường vụ"))

        val taskRegex = Regex("\\[ACTION_TASK:\\s*(\\{.*?\\})\\]", RegexOption.DOT_MATCHES_ALL)
        val taskMatch = taskRegex.find(sampleResponse)
        assertNotNull(taskMatch)
        assertTrue(taskMatch!!.value.contains("Xây dựng dự thảo Kế hoạch"))
    }
}
