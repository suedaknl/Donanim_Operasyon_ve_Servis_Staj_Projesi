package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class LeaveConflictInfo(
    val conflictingLeaves: List<ConflictingLeaveDetail>,
    val totalPersonnelCount: Int,
    val currentlyOnLeaveCount: Int,
    val isCapacityCritical: Boolean
)

data class ConflictingLeaveDetail(
    val personnelName: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String
)

class CalculateLeaveConflictUseCase @Inject constructor() {

    operator fun invoke(
        targetRequest: LeaveRequestEntity,
        allApprovedLeaves: List<LeaveRequestEntity>,
        allPersonnel: List<Personnel>
    ): LeaveConflictInfo {
        val reqStart = parseDateSafe(targetRequest.startDate)
        val reqEnd = parseDateSafe(targetRequest.endDate)

        val conflicting = mutableListOf<ConflictingLeaveDetail>()

        if (reqStart != null && reqEnd != null) {
            for (leave in allApprovedLeaves) {
                if (leave.id == targetRequest.id) continue
                if (leave.status != "APPROVED") continue

                val leaveStart = parseDateSafe(leave.startDate)
                val leaveEnd = parseDateSafe(leave.endDate)

                if (leaveStart != null && leaveEnd != null) {
                    // Çakışma koşulu: existing.startDate <= req.endDate AND existing.endDate >= req.startDate
                    val isOverlap = !leaveStart.after(reqEnd) && !leaveEnd.before(reqStart)
                    if (isOverlap) {
                        val personnelName = allPersonnel.find { it.id == leave.personnelId }?.fullName ?: "Personel #${leave.personnelId}"
                        conflicting.add(
                            ConflictingLeaveDetail(
                                personnelName = personnelName,
                                leaveType = leave.leaveType,
                                startDate = leave.startDate,
                                endDate = leave.endDate
                            )
                        )
                    }
                }
            }
        }

        val totalPersonnel = allPersonnel.size.coerceAtLeast(1)
        val onLeaveCount = conflicting.size
        val isCritical = (onLeaveCount + 1).toDouble() / totalPersonnel.toDouble() >= 0.5

        return LeaveConflictInfo(
            conflictingLeaves = conflicting,
            totalPersonnelCount = totalPersonnel,
            currentlyOnLeaveCount = onLeaveCount,
            isCapacityCritical = isCritical
        )
    }

    private fun parseDateSafe(dateStr: String): Calendar? {
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
            } catch (_: Exception) {
            }
        }
        return null
    }
}