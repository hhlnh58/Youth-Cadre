package com.example.data.ai

import com.example.data.model.PersonEntity
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedCadreEntity(
    val title: String,
    val assigner: String = "",          // Người giao việc
    val responsiblePerson: String = "", // Người phụ trách / thực hiện chính
    val timeString: String = "",        // Thời gian / Giờ (e.g., 14:00)
    val dateString: String = "",        // Ngày (dd/MM/yyyy)
    val location: String = "",          // Địa điểm
    val content: String = "",           // Nội dung chi tiết
    val collaborators: String = "",     // Người phối hợp
    val priority: String = "P2",        // P1, P2, P3
    val inferredType: String = "TASK",  // TASK or EVENT
    val timestamp: Long = 0L            // Sắp xếp thời gian chuẩn xác
)

object SmartCadreParser {

    /**
     * Remove Vietnamese accents for resilient fuzzy comparison.
     */
    fun removeAccents(str: String): String {
        val nfd = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfd).replaceAll("").replace("đ", "d").replace("Đ", "D")
    }

    /**
     * Find people from the directory whose names or positions match words in the input.
     * Used for real-time auto-suggest in Quick Capture.
     */
    fun findSuggestedPeople(input: String, people: List<PersonEntity>): List<PersonEntity> {
        val trimmed = input.trim()
        if (trimmed.isBlank() || people.isEmpty()) return emptyList()

        val inputUnaccent = removeAccents(trimmed).lowercase(Locale.getDefault())
        val inputTokens = inputUnaccent.split(Regex("""[\s,;\.\-–—\(\)\/]+""")).filter { it.length >= 2 }

        return people.filter { person ->
            val pNameUnaccent = removeAccents(person.fullName).lowercase(Locale.getDefault())
            val pPosUnaccent = removeAccents(person.position).lowercase(Locale.getDefault())
            val pLastNameUnaccent = pNameUnaccent.split(" ").lastOrNull() ?: ""

            // Exact substring match
            inputUnaccent.contains(pNameUnaccent) ||
            pNameUnaccent.contains(inputUnaccent) ||
            // Last name match if input token is specific
            inputTokens.any { token ->
                (token.length >= 2 && pLastNameUnaccent == token) ||
                (token.length >= 3 && pNameUnaccent.contains(token)) ||
                (pPosUnaccent.isNotBlank() && pPosUnaccent.contains(token))
            }
        }.sortedByDescending { person ->
            val pNameUnaccent = removeAccents(person.fullName).lowercase(Locale.getDefault())
            when {
                inputUnaccent.contains(pNameUnaccent) -> 3
                pNameUnaccent.contains(inputUnaccent) -> 2
                else -> 1
            }
        }.take(6)
    }

    /**
     * Intelligent Vietnamese NLP entity extractor for Youth Cadre tasks and events.
     * Cross-references personnel in the directory (people) to accurately extract:
     * 1. Người giao việc (Assigner)
     * 2. Người thực hiện / Cán bộ phụ trách chính (Responsible Person)
     * 3. Người phối hợp (Collaborators / Coordinator)
     * 4. Thời gian & Hạn chót (Time & Date, formatted and sortable)
     * 5. Địa điểm (Location)
     * 6. Nội dung (Cleaned Content / Task Title)
     */
    fun parse(rawInput: String, people: List<PersonEntity> = emptyList()): ParsedCadreEntity {
        var text = rawInput.trim()
        val cal = Calendar.getInstance()
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
        if (text.isBlank()) {
            return ParsedCadreEntity(title = "", dateString = todayStr, timestamp = cal.timeInMillis)
        }

        var assigner = ""
        var responsiblePerson = ""
        var location = ""
        var collaborators = ""
        var priority = "P2"
        var timeStr = "17:00"
        var dateStr = todayStr

        // 1. EXTRACT NGƯỜI GIAO VIỆC (Assigner)
        // Check Directory first (highest accuracy)
        for (person in people) {
            val fullName = person.fullName.trim()
            val lastName = fullName.split(" ").lastOrNull()?.trim() ?: ""
            if (fullName.isNotBlank()) {
                val assignerRegex = Regex("""(?:Đ\/c|Đồng chí|Bí thư|Phó Bí thư|Thường trực)?\s*$fullName\s*(?:giao việc|giao nhiệm vụ|giao|chỉ đạo|yêu cầu)""", RegexOption.IGNORE_CASE)
                val m = assignerRegex.find(text)
                if (m != null) {
                    assigner = fullName
                    text = text.removeRange(m.range).trim()
                    break
                }
            }
            if (lastName.length >= 2) {
                val shortAssignerRegex = Regex("""(?:Đ\/c|Đồng chí|Bí thư|Phó Bí thư)\s+$lastName\s*(?:giao việc|giao nhiệm vụ|giao|chỉ đạo|yêu cầu)""", RegexOption.IGNORE_CASE)
                val m = shortAssignerRegex.find(text)
                if (m != null) {
                    assigner = fullName
                    text = text.removeRange(m.range).trim()
                    break
                }
            }
        }

        // Generic patterns for Assigner if not found in directory
        if (assigner.isBlank()) {
            val assignerPatterns = listOf(
                Regex("""(?:do|bởi)?\s*(Đ\/c\s+[A-ZÀ-Ỹa-zà-ỹ\s]+?|Đồng chí\s+[A-ZÀ-Ỹa-zà-ỹ\s]+?|Bí thư\s+[A-ZÀ-Ỹa-zà-ỹ\s]*?|Phó Bí thư\s+[A-ZÀ-Ỹa-zà-ỹ\s]*?|Thường trực\s+[A-ZÀ-Ỹa-zà-ỹ\s]*?|Ban Thường vụ\s*[A-ZÀ-Ỹa-zà-ỹ]*?)\s*(?:giao việc|giao nhiệm vụ|giao|chỉ đạo|yêu cầu)""", RegexOption.IGNORE_CASE),
                Regex("""(?:giao bởi|người giao[:\s]+)\s*([A-ZÀ-Ỹa-zà-ỹ\s\.\/]+?)(?:,|;|\.|\s+phối hợp|\s+hạn|\s+trước|\s+tại|$)""", RegexOption.IGNORE_CASE),
                Regex("""([A-ZÀ-Ỹ][a-zà-ỹ]+(?:\s+[A-ZÀ-Ỹ][a-zà-ỹ]+){1,3})\s+(?:giao việc|giao nhiệm vụ)""", RegexOption.IGNORE_CASE)
            )
            for (pat in assignerPatterns) {
                val match = pat.find(text)
                if (match != null) {
                    assigner = match.groupValues[1].trim().trimEnd(',', ';', '.')
                    text = text.removeRange(match.range).trim()
                    break
                }
            }
        }

        // 2. EXTRACT NGƯỜI PHỤ TRÁCH CHÍNH / NHẬN VIỆC (Responsible Person)
        // Check Directory first
        for (person in people) {
            val fullName = person.fullName.trim()
            val lastName = fullName.split(" ").lastOrNull()?.trim() ?: ""
            if (fullName.isNotBlank()) {
                val respRegex = Regex("""(?:giao cho|phân công cho|phân công|giao|cho)\s+(?:Đ\/c\s+|Đồng chí\s+)?$fullName""", RegexOption.IGNORE_CASE)
                val m = respRegex.find(text)
                if (m != null) {
                    responsiblePerson = fullName
                    text = text.removeRange(m.range).trim()
                    break
                }
            }
            if (lastName.length >= 2) {
                val shortRespRegex = Regex("""(?:giao cho|phân công cho|phân công|giao|cho)\s+(?:Đ\/c\s+|Đồng chí\s+)?$lastName\b""", RegexOption.IGNORE_CASE)
                val m = shortRespRegex.find(text)
                if (m != null) {
                    responsiblePerson = fullName
                    text = text.removeRange(m.range).trim()
                    break
                }
            }
        }

        // Generic patterns for Responsible Person if not matched
        if (responsiblePerson.isBlank()) {
            val respPatterns = listOf(
                Regex("""(?:giao cho|phân công cho|giao việc cho|người thực hiện[:\s]+|cán bộ phụ trách[:\s]+)\s*([A-ZÀ-Ỹa-zà-ỹ\s\.\/]+?)(?:,|;|\.|\s+phối hợp|\s+hạn|\s+trước|\s+tại|$)""", RegexOption.IGNORE_CASE),
                Regex("""(?:giao|cho)\s+(Đ\/c\s+[A-ZÀ-Ỹa-zà-ỹ\s]+?|Đồng chí\s+[A-ZÀ-Ỹa-zà-ỹ\s]+?)\s+(?:thực hiện|chủ trì|báo cáo|làm)""", RegexOption.IGNORE_CASE)
            )
            for (pat in respPatterns) {
                val match = pat.find(text)
                if (match != null) {
                    responsiblePerson = match.groupValues[1].trim().trimEnd(',', ';', '.')
                    text = text.removeRange(match.range).trim()
                    break
                }
            }
        }

        // 3. EXTRACT NGƯỜI PHỐI HỢP (Collaborators)
        // Check Directory first
        val matchedCollabs = mutableListOf<String>()
        for (person in people) {
            val fullName = person.fullName.trim()
            val lastName = fullName.split(" ").lastOrNull()?.trim() ?: ""
            if (fullName.isNotBlank()) {
                val collabRegex = Regex("""(?:phối hợp với|phối hợp cùng|kết hợp cùng|cùng với|cùng)\s+(?:Đ\/c\s+|Đồng chí\s+)?$fullName""", RegexOption.IGNORE_CASE)
                val m = collabRegex.find(text)
                if (m != null) {
                    matchedCollabs.add(fullName)
                    text = text.removeRange(m.range).trim()
                }
            } else if (lastName.length >= 2) {
                val shortCollabRegex = Regex("""(?:phối hợp với|phối hợp cùng|kết hợp cùng|cùng với|cùng)\s+(?:Đ\/c\s+|Đồng chí\s+)?$lastName\b""", RegexOption.IGNORE_CASE)
                val m = shortCollabRegex.find(text)
                if (m != null) {
                    matchedCollabs.add(fullName)
                    text = text.removeRange(m.range).trim()
                }
            }
        }

        if (matchedCollabs.isNotEmpty()) {
            collaborators = matchedCollabs.distinct().joinToString(", ")
        } else {
            val collabPatterns = listOf(
                Regex("""(?:phối hợp với|phối hợp cùng|kết hợp cùng|cùng với|người phối hợp[:\s]+)\s*([A-ZÀ-Ỹa-zà-ỹ0-9\s\.\,\/]+?)(?:;|hạn|trước|tại|lúc|vào|$)""", RegexOption.IGNORE_CASE),
                Regex("""(?:đầu mối phối hợp[:\s]+)\s*([A-ZÀ-Ỹa-zà-ỹ0-9\s\.\,\/]+?)(?:;|hạn|trước|tại|lúc|vào|$)""", RegexOption.IGNORE_CASE)
            )
            for (pat in collabPatterns) {
                val match = pat.find(text)
                if (match != null) {
                    collaborators = match.groupValues[1].trim().trimEnd(',', ';', '.')
                    text = text.removeRange(match.range).trim()
                    break
                }
            }
        }

        // 4. EXTRACT ĐỊA ĐIỂM (Location)
        val locationPatterns = listOf(
            Regex("""(?:tại|ở)\s+(Hội trường[^\,\;\.]+|Phòng họp[^\,\;\.]+|UBND[^\,\;\.]+|Công viên[^\,\;\.]+|Cơ quan[^\,\;\.]+|Quận Đoàn[^\,\;\.]+|Huyện Đoàn[^\,\;\.]+|Thành Đoàn[^\,\;\.]+|Đoàn Phường[^\,\;\.]+|Đoàn Xã[^\,\;\.]+|Trường[^\,\;\.]+|Sở[^\,\;\.]+|Chi đoàn[^\,\;\.]+|Nhà văn hóa[^\,\;\.]+|Trung tâm[^\,\;\.]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:địa điểm[:\s]+)\s*([A-ZÀ-Ỹa-zà-ỹ0-9\s\,\-\/]+?)(?:;|hạn|trước|lúc|vào|$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:tại|ở)\s+([A-ZÀ-Ỹ0-9][A-ZÀ-Ỹa-zà-ỹ0-9\s\.\-]+?)(?:;|hạn|trước|lúc|vào|ngày|thứ|$)""", RegexOption.IGNORE_CASE)
        )
        for (pat in locationPatterns) {
            val match = pat.find(text)
            if (match != null) {
                location = match.groupValues[1].trim().trimEnd(',', ';', '.')
                text = text.removeRange(match.range).trim()
                break
            }
        }

        // 5. EXTRACT THỜI GIAN & GIỜ (Hour / Minute)
        // Detect "14h", "14h30", "14:00", "8 giờ", "2h chiều", "7h tối"
        val hourRegex = Regex("""(?:lúc|trước|vào)?\s*(\d{1,2})\s*(?:h|:|giờ)\s*(\d{2})?\s*(sáng|chiều|tối)?""", RegexOption.IGNORE_CASE)
        val hourMatch = hourRegex.find(text)
        var parsedHour = 17
        var parsedMinute = 0
        if (hourMatch != null) {
            var h = hourMatch.groupValues[1].toIntOrNull() ?: 17
            val m = hourMatch.groupValues[2].ifBlank { "00" }.toIntOrNull() ?: 0
            val amPm = hourMatch.groupValues[3].lowercase(Locale.getDefault())

            // Normalize "2h chiều" -> 14, "7h tối" -> 19
            if ((amPm == "chiều" || amPm == "tối") && h in 1..11) {
                h += 12
            } else if (amPm == "sáng" && h == 12) {
                h = 0
            }

            parsedHour = h
            parsedMinute = m
            timeStr = String.format(Locale.getDefault(), "%02d:%02d", parsedHour, parsedMinute)
            text = text.removeRange(hourMatch.range).trim()
        }

        // 6. EXTRACT NGÀY THÁNG NĂM (Date & Deadline)
        val isNextWeek = text.contains("tuần sau", ignoreCase = true)
        val dateRegex = Regex("""(?:ngày\s+)?(\d{1,2})[\/\-](\d{1,2})(?:[\/\-](\d{4}))?""", RegexOption.IGNORE_CASE)
        val textDateRegex = Regex("""ngày\s+(\d{1,2})\s+tháng\s+(\d{1,2})(?:\s+năm\s+(\d{4}))?""", RegexOption.IGNORE_CASE)

        val dateMatch = dateRegex.find(text) ?: textDateRegex.find(text)
        val dateCal = Calendar.getInstance()
        if (dateMatch != null) {
            val d = dateMatch.groupValues[1].toInt()
            val m = dateMatch.groupValues[2].toInt() - 1
            val y = dateMatch.groupValues[3].toIntOrNull() ?: dateCal.get(Calendar.YEAR)
            dateCal.set(y, m, d)
            dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
            text = text.removeRange(dateMatch.range).trim()
        } else {
            val lower = text.lowercase(Locale.getDefault())
            when {
                lower.contains("ngày mai") || lower.contains("sáng mai") || lower.contains("chiều mai") -> {
                    dateCal.add(Calendar.DAY_OF_YEAR, 1)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:ngày mai|sáng mai|chiều mai)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("ngày kia") || lower.contains("ngày mốt") -> {
                    dateCal.add(Calendar.DAY_OF_YEAR, 2)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:ngày kia|ngày mốt)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("hôm nay") -> {
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""hôm nay""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("đầu tuần sau") -> {
                    setNextDayOfWeek(dateCal, Calendar.MONDAY, isNextWeek = true)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""đầu tuần sau""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("cuối tuần") -> {
                    setNextDayOfWeek(dateCal, Calendar.SATURDAY, isNextWeek = isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:cuối tuần này|cuối tuần)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("thứ hai") || lower.contains("thứ 2") -> {
                    setNextDayOfWeek(dateCal, Calendar.MONDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:thứ hai|thứ 2)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("thứ ba") || lower.contains("thứ 3") -> {
                    setNextDayOfWeek(dateCal, Calendar.TUESDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:thứ ba|thứ 3)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("thứ tư") || lower.contains("thứ 4") -> {
                    setNextDayOfWeek(dateCal, Calendar.WEDNESDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:thứ tư|thứ 4)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("thứ năm") || lower.contains("thứ 5") -> {
                    setNextDayOfWeek(dateCal, Calendar.THURSDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:thứ năm|thứ 5)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("thứ sáu") || lower.contains("thứ 6") -> {
                    setNextDayOfWeek(dateCal, Calendar.FRIDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:thứ sáu|thứ 6)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("thứ bảy") || lower.contains("thứ 7") -> {
                    setNextDayOfWeek(dateCal, Calendar.SATURDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:thứ bảy|thứ 7)""", RegexOption.IGNORE_CASE), "").trim()
                }
                lower.contains("chủ nhật") || lower.contains("cn") -> {
                    setNextDayOfWeek(dateCal, Calendar.SUNDAY, isNextWeek)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateCal.time)
                    text = text.replace(Regex("""(?:chủ nhật|cn)""", RegexOption.IGNORE_CASE), "").trim()
                }
            }
        }

        // Clean "tuần sau" leftover
        text = text.replace(Regex("""\s*tuần sau\s*""", RegexOption.IGNORE_CASE), " ").trim()

        // 7. Calculate exact timestamp for sorting
        dateCal.set(Calendar.HOUR_OF_DAY, parsedHour)
        dateCal.set(Calendar.MINUTE, parsedMinute)
        dateCal.set(Calendar.SECOND, 0)
        dateCal.set(Calendar.MILLISECOND, 0)
        val finalTimestamp = dateCal.timeInMillis

        // 8. EXTRACT PRIORITY
        val lowerRemaining = text.lowercase(Locale.getDefault())
        if (lowerRemaining.contains("khẩn cấp") || lowerRemaining.contains("gấp") || lowerRemaining.contains("hỏa tốc")) {
            priority = "P1"
            text = text.replace(Regex("""(?:khẩn cấp|khẩn|gấp|hỏa tốc)""", RegexOption.IGNORE_CASE), "").trim()
        } else if (lowerRemaining.contains("trọng tâm") || lowerRemaining.contains("quan trọng") || lowerRemaining.contains("ưu tiên")) {
            priority = "P2"
            text = text.replace(Regex("""(?:trọng tâm|quan trọng|ưu tiên)""", RegexOption.IGNORE_CASE), "").trim()
        }

        // 9. INFER TYPE (TASK vs EVENT)
        val isEventLike = location.isNotBlank() ||
                rawInput.contains(Regex("""(?:họp|hội nghị|giao ban|đi cơ sở|thăm|tiếp đón|lễ|chuyến đi)""", RegexOption.IGNORE_CASE))
        val inferredType = if (isEventLike) "EVENT" else "TASK"

        // 10. CLEAN CONTENT / TITLE
        var cleanTitle = text
            .replace(Regex("""^[,\;\.\-\s]+"""), "")
            .replace(Regex("""[,\;\.\-\s]+$"""), "")
            .replace(Regex("""\s+trước\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+hạn\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+lúc\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+tại\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+ở\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        if (cleanTitle.isBlank()) {
            cleanTitle = rawInput.take(60).trim()
        }

        return ParsedCadreEntity(
            title = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            assigner = assigner,
            responsiblePerson = responsiblePerson,
            timeString = timeStr,
            dateString = dateStr,
            location = location,
            content = cleanTitle,
            collaborators = collaborators,
            priority = priority,
            inferredType = inferredType,
            timestamp = finalTimestamp
        )
    }

    private fun setNextDayOfWeek(cal: Calendar, targetDayOfWeek: Int, isNextWeek: Boolean = false) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var diff = targetDayOfWeek - currentDay
        if (diff <= 0) {
            diff += 7
        }
        if (isNextWeek && diff < 7) {
            diff += 7
        }
        cal.add(Calendar.DAY_OF_YEAR, diff)
    }
}
