package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class AnalysisPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class AdminWorkAnalysis(
    val labels: List<String>,
    val createdCounts: List<Int>,
    val completedCounts: List<Int>,
    val totalCreated: Int,
    val totalCompleted: Int,
    val completionRate: Int,
    val periodTitle: String
)

class GetAdminWorkAnalysisUseCase @Inject constructor() {

    // Thread-safe olması için parse işlemlerini her çağrıda veya local scope'da yönetiyoruz
    private fun getParseFormats(): List<SimpleDateFormat> {
        val trLocale = Locale("tr", "TR")
        return listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd.MM.yyyy", trLocale),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", trLocale),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("dd.MM.yyyy HH:mm", trLocale)
        )
    }

    operator fun invoke(records: List<ServiceRecord>, period: AnalysisPeriod): AdminWorkAnalysis {
        return when (period) {
            AnalysisPeriod.DAILY -> calculateDaily(records)
            AnalysisPeriod.WEEKLY -> calculateWeekly(records)
            AnalysisPeriod.MONTHLY -> calculateMonthly(records)
        }
    }

    private fun parseRecordDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        for (fmt in getParseFormats()) {
            try {
                val parsed = fmt.parse(trimmed)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    private fun calculateDaily(records: List<ServiceRecord>): AdminWorkAnalysis {
        val labels = listOf("00-04", "04-08", "08-12", "12-16", "16-20", "20-24")
        val createdCounts = MutableList(6) { 0 }
        val completedCounts = MutableList(6) { 0 }

        val todayCalendar = Calendar.getInstance()
        val todayYear = todayCalendar.get(Calendar.YEAR)
        val todayDay = todayCalendar.get(Calendar.DAY_OF_YEAR)

        var matchedCount = 0

        records.forEach { record ->
            val parsedDate = parseRecordDate(record.date)
            if (parsedDate != null) {
                val cal = Calendar.getInstance().apply { time = parsedDate }
                if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                    matchedCount++
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val bucketIndex = when (hour) {
                        in 0..3 -> 0
                        in 4..7 -> 1
                        in 8..11 -> 2
                        in 12..15 -> 3
                        in 16..19 -> 4
                        else -> 5
                    }
                    createdCounts[bucketIndex]++
                    if (record.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true)) {
                        completedCounts[bucketIndex]++
                    }
                }
            }
        }

        if (matchedCount == 0 && records.isNotEmpty()) {
            records.forEach { record ->
                createdCounts[4]++
                if (record.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true)) {
                    completedCounts[4]++
                }
            }
        }

        val totalCreated = createdCounts.sum()
        val totalCompleted = completedCounts.sum()
        val rate = if (totalCreated > 0) ((totalCompleted.toFloat() / totalCreated) * 100).toInt().coerceIn(0, 100) else 0

        return AdminWorkAnalysis(labels, createdCounts, completedCounts, totalCreated, totalCompleted, rate, "Bugünkü Operasyonel Trend")
    }

    private fun calculateWeekly(records: List<ServiceRecord>): AdminWorkAnalysis {
        val labelFormat = SimpleDateFormat("EEE", Locale("tr", "TR"))
        val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val today = Calendar.getInstance()

        val labels = mutableListOf<String>()
        val dateKeys = mutableListOf<String>()

        for (dayOffset in 6 downTo 0) {
            val cal = today.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            labels.add(labelFormat.format(cal.time))
            dateKeys.add(keyFormat.format(cal.time))
        }

        val createdCounts = MutableList(7) { 0 }
        val completedCounts = MutableList(7) { 0 }
        var matchedCount = 0

        records.forEach { record ->
            val parsedDate = parseRecordDate(record.date)
            if (parsedDate != null) {
                val key = keyFormat.format(parsedDate)
                val index = dateKeys.indexOf(key)
                if (index >= 0) {
                    matchedCount++
                    createdCounts[index]++
                    if (record.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true)) {
                        completedCounts[index]++
                    }
                }
            }
        }

        if (matchedCount == 0 && records.isNotEmpty()) {
            records.forEach { record ->
                createdCounts[6]++
                if (record.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true)) {
                    completedCounts[6]++
                }
            }
        }

        val totalCreated = createdCounts.sum()
        val totalCompleted = completedCounts.sum()
        val rate = if (totalCreated > 0) ((totalCompleted.toFloat() / totalCreated) * 100).toInt().coerceIn(0, 100) else 0

        return AdminWorkAnalysis(labels, createdCounts, completedCounts, totalCreated, totalCompleted, rate, "Son 7 Günlük Operasyonel Trend")
    }

    private fun calculateMonthly(records: List<ServiceRecord>): AdminWorkAnalysis {
        val labels = listOf("1. Hafta", "2. Hafta", "3. Hafta", "4. Hafta")
        val createdCounts = MutableList(4) { 0 }
        val completedCounts = MutableList(4) { 0 }

        val today = Calendar.getInstance()
        val thirtyDaysAgo = today.clone() as Calendar
        thirtyDaysAgo.add(Calendar.DAY_OF_YEAR, -30)
        var matchedCount = 0

        records.forEach { record ->
            val parsedDate = parseRecordDate(record.date)
            if (parsedDate != null) {
                val cal = Calendar.getInstance().apply { time = parsedDate }
                if (!cal.before(thirtyDaysAgo) && !cal.after(today)) {
                    matchedCount++
                    val diffMillis = today.timeInMillis - cal.timeInMillis
                    val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceIn(0, 30)
                    val weekIndex = (diffDays / 7).coerceIn(0, 3)
                    val targetWeek = 3 - weekIndex

                    createdCounts[targetWeek]++
                    if (record.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true)) {
                        completedCounts[targetWeek]++
                    }
                }
            }
        }

        if (matchedCount == 0 && records.isNotEmpty()) {
            records.forEach { record ->
                createdCounts[3]++
                if (record.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true)) {
                    completedCounts[3]++
                }
            }
        }

        val totalCreated = createdCounts.sum()
        val totalCompleted = completedCounts.sum()
        val rate = if (totalCreated > 0) ((totalCompleted.toFloat() / totalCreated) * 100).toInt().coerceIn(0, 100) else 0

        return AdminWorkAnalysis(labels, createdCounts, completedCounts, totalCreated, totalCompleted, rate, "Son 30 Günlük Operasyonel Trend")
    }
}