package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.personnel

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class PersonnelDetailSummary(
    val totalAssignedJobs: Int,
    val pendingJobs: Int,
    val inProgressJobs: Int,
    val completedJobs: Int,
    val cancelledJobs: Int,
    val completionRate: Float,
    val currentAvailability: String,
    val availabilityReason: String,
    val activeService: ServiceRecord?,
    val recentActivities: List<PersonnelActivityItem>
)

data class PersonnelActivityItem(
    val title: String,
    val date: String,
    val type: String
)

class GetPersonnelDetailSummaryUseCase @Inject constructor() {

    operator fun invoke(
        personnelId: Int,
        services: List<ServiceRecord>,
        leaves: List<LeaveRequestEntity>,
        shifts: List<ShiftEntity>,
        overtimes: List<OvertimeEntity>
    ): PersonnelDetailSummary {
        val personnelServices = services.filter { it.assignedPersonnelId == personnelId }

        val total = personnelServices.size
        val pending = personnelServices.count { it.status.toString().contains("BEKLIYOR", ignoreCase = true) }
        val inProgress = personnelServices.count {
            val st = it.status.toString()
            st.contains("ISLEME", ignoreCase = true) || st.contains("YOLDA", ignoreCase = true) || st.contains("DEVAM", ignoreCase = true)
        }
        val completed = personnelServices.count { it.status.toString().contains("TAMAMLANDI", ignoreCase = true) }
        val cancelled = personnelServices.count { it.status.toString().contains("IPTAL", ignoreCase = true) }

        val rate = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f

        val activeJob = personnelServices.find {
            val st = it.status.toString()
            !st.contains("TAMAMLANDI", ignoreCase = true) && !st.contains("IPTAL", ignoreCase = true)
        }

        val todayCalendar = Calendar.getInstance()
        val currentLeave = leaves.find {
            it.personnelId == personnelId &&
                    it.status.equals("APPROVED", ignoreCase = true) &&
                    isDateInRange(todayCalendar, parseDate(it.startDate), parseDate(it.endDate))
        }

        val availability: String
        val reason: String

        if (currentLeave != null) {
            availability = "İzinli"
            reason = "${currentLeave.startDate} - ${currentLeave.endDate} tarihine kadar izinli"
        } else if (activeJob != null) {
            availability = "Aktif İşte"
            reason = "${activeJob.companyName} üzerinde çalışıyor"
        } else {
            availability = "Mesai Dışı / Uygun"
            reason = "Atanmış aktif iş bulunmuyor"
        }

        val activities = mutableListOf<PersonnelActivityItem>()
        personnelServices.sortedByDescending { it.id }.take(3).forEach {
            activities.add(PersonnelActivityItem(title = "${it.companyName} - ${it.deviceType}", date = it.date, type = "İş"))
        }
        leaves.filter { it.personnelId == personnelId }.sortedByDescending { it.id }.take(2).forEach {
            activities.add(PersonnelActivityItem(title = "${it.leaveType} İzni (${it.status})", date = it.startDate, type = "İzin"))
        }

        return PersonnelDetailSummary(
            totalAssignedJobs = total,
            pendingJobs = pending,
            inProgressJobs = inProgress,
            completedJobs = completed,
            cancelledJobs = cancelled,
            completionRate = rate,
            currentAvailability = availability,
            availabilityReason = reason,
            activeService = activeJob,
            recentActivities = activities
        )
    }

    private fun parseDate(dateStr: String): Calendar? {
        val formats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "yyyy/MM/dd", "dd-MM-yyyy")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(dateStr)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    return cal
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun isDateInRange(current: Calendar?, start: Calendar?, end: Calendar?): Boolean {
        if (current == null || start == null || end == null) return false
        val c = truncateTime(current)
        val s = truncateTime(start)
        val e = truncateTime(end)
        return !c.before(s) && !c.after(e)
    }

    private fun truncateTime(cal: Calendar): Calendar {
        val cloned = cal.clone() as Calendar
        cloned.set(Calendar.HOUR_OF_DAY, 0)
        cloned.set(Calendar.MINUTE, 0)
        cloned.set(Calendar.SECOND, 0)
        cloned.set(Calendar.MILLISECOND, 0)
        return cloned
    }
}