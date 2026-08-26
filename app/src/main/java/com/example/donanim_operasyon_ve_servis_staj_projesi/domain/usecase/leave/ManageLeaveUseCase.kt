package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageLeaveUseCase @Inject constructor(
    private val workforceRepository: WorkforceRepository,
    private val personnelRepository: PersonnelRepository
) {
    fun getPersonnelLeaveRequests(personnelId: Int): Flow<List<LeaveRequestEntity>> {
        return workforceRepository.getLeaveRequestsByPersonnel(personnelId)
    }

    fun getPendingRequests(): Flow<List<LeaveRequestEntity>> {
        return workforceRepository.getPendingLeaveRequests()
    }

    suspend fun getAllPersonnel(): List<Personnel> {
        return personnelRepository.getAllPersonnelList()
    }

    suspend fun createLeaveRequest(
        personnelId: Int,
        startDate: String,
        endDate: String,
        leaveType: String,
        description: String
    ): Result<Unit> {
        if (startDate.isBlank() || endDate.isBlank() || description.isBlank()) {
            return Result.failure(Exception("Tüm alanlar doldurulmalıdır."))
        }
        if (startDate > endDate) {
            return Result.failure(Exception("Başlangıç tarihi bitiş tarihinden sonra olamaz."))
        }

        val request = LeaveRequestEntity(
            personnelId = personnelId,
            startDate = startDate,
            endDate = endDate,
            leaveType = leaveType,
            description = description,
            status = "PENDING"
        )
        workforceRepository.insertLeaveRequest(request)
        return Result.success(Unit)
    }

    suspend fun updateLeaveRequest(leaveRequest: LeaveRequestEntity): Result<Unit> {
        if (leaveRequest.startDate.isBlank() || leaveRequest.endDate.isBlank()) {
            return Result.failure(Exception("Tarih alanları boş olamaz."))
        }
        if (leaveRequest.startDate > leaveRequest.endDate) {
            return Result.failure(Exception("Başlangıç tarihi bitiş tarihinden sonra olamaz."))
        }
        if (!leaveRequest.status.equals("PENDING", ignoreCase = true)) {
            return Result.failure(Exception("Sadece bekleyen (PENDING) izin talepleri düzenlenebilir."))
        }

        workforceRepository.updateLeaveRequest(leaveRequest)
        return Result.success(Unit)
    }

    suspend fun deleteLeaveRequest(leaveRequest: LeaveRequestEntity): Result<Unit> {
        if (!leaveRequest.status.equals("PENDING", ignoreCase = true)) {
            return Result.failure(Exception("Sadece bekleyen (PENDING) izin talepleri silinebilir."))
        }
        workforceRepository.deleteLeaveRequest(leaveRequest)
        return Result.success(Unit)
    }

    suspend fun approveLeaveRequest(
        requestId: Int,
        adminNote: String?,
        forceApprove: Boolean,
        allRequests: List<LeaveRequestEntity>
    ): LeaveApprovalResult {
        val targetRequest = allRequests.find { it.id == requestId }
            ?: return LeaveApprovalResult(false, false, "İzin talebi bulunamadı.")

        val allPersonnel = personnelRepository.getAllPersonnelList()
        val totalPersonnelCount = allPersonnel.size

        if (totalPersonnelCount == 0) {
            return LeaveApprovalResult(false, false, "Sistemde kayıtlı personel bulunamadı.")
        }

        if (!forceApprove) {
            val overlappingLeaves = workforceRepository.getApprovedLeavesInRange(
                targetRequest.startDate,
                targetRequest.endDate
            )

            val personnelIdsOnLeave = overlappingLeaves.map { it.personnelId }.toMutableSet()
            personnelIdsOnLeave.add(targetRequest.personnelId)

            val leaveCount = personnelIdsOnLeave.size
            val capacityLimit = totalPersonnelCount * 0.5

            if (leaveCount > capacityLimit) {
                return LeaveApprovalResult(
                    success = false,
                    capacityWarning = true,
                    message = "Uyarı: Bu tarih aralığında izinli personel sayısı toplam kapasitenin %50'sini aşıyor (%$leaveCount / $totalPersonnelCount). Yine de onaylamak istiyor musunuz?"
                )
            }
        }

        val updatedRequest = targetRequest.copy(
            status = "APPROVED",
            adminNote = adminNote,
            reviewedAt = System.currentTimeMillis()
        )
        workforceRepository.updateLeaveRequest(updatedRequest)
        return LeaveApprovalResult(success = true, capacityWarning = false, message = "İzin talebi onaylandı.")
    }

    suspend fun rejectLeaveRequest(
        requestId: Int,
        adminNote: String?,
        allRequests: List<LeaveRequestEntity>
    ): Result<Unit> {
        val targetRequest = allRequests.find { it.id == requestId }
            ?: return Result.failure(Exception("İzin talebi bulunamadı."))

        val updatedRequest = targetRequest.copy(
            status = "REJECTED",
            adminNote = adminNote,
            reviewedAt = System.currentTimeMillis()
        )
        workforceRepository.updateLeaveRequest(updatedRequest)
        return Result.success(Unit)
    }
}