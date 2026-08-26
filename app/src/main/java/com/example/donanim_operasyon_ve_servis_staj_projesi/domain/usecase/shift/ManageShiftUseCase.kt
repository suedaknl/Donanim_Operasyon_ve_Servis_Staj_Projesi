package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.shift

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageShiftUseCase @Inject constructor(
    private val workforceRepository: WorkforceRepository
) {
    fun getPersonnelShifts(personnelId: Int): Flow<List<ShiftEntity>> {
        return workforceRepository.getShiftsByPersonnel(personnelId)
    }

    suspend fun getTodayShift(personnelId: Int, date: String): ShiftEntity? {
        return workforceRepository.getTodayShiftForPersonnel(personnelId, date)
    }

    private suspend fun hasApprovedLeaveOnDate(personnelId: Int, date: String): Boolean {
        val approvedLeaves = workforceRepository.getApprovedLeavesInRange(date, date)
        return approvedLeaves.any { leave ->
            leave.personnelId == personnelId && leave.status == "APPROVED" &&
                    leave.startDate <= date && leave.endDate >= date
        }
    }

    suspend fun createShift(
        personnelId: Int,
        shiftDate: String,
        startTime: String,
        endTime: String
    ): Result<Unit> {
        if (startTime.isBlank() || endTime.isBlank()) {
            return Result.failure(Exception("Başlangıç ve bitiş saatleri boş olamaz."))
        }
        if (startTime >= endTime) {
            return Result.failure(Exception("Bitiş saati başlangıçtan önce veya aynı olamaz."))
        }

        if (hasApprovedLeaveOnDate(personnelId, shiftDate)) {
            return Result.failure(Exception("Bu personelin seçilen tarihte onaylanmış izni bulunmaktadır. Vardiya oluşturulamaz."))
        }

        val shift = ShiftEntity(
            personnelId = personnelId,
            shiftDate = shiftDate,
            startTime = startTime,
            endTime = endTime,
            status = "PLANNED"
        )
        workforceRepository.insertShift(shift)
        return Result.success(Unit)
    }

    suspend fun updateShift(shift: ShiftEntity): Result<Unit> {
        if (shift.startTime.isBlank() || shift.endTime.isBlank()) {
            return Result.failure(Exception("Başlangıç ve bitiş saatleri boş olamaz."))
        }
        if (shift.startTime >= shift.endTime) {
            return Result.failure(Exception("Bitiş saati başlangıçtan önce veya aynı olamaz."))
        }

        if (hasApprovedLeaveOnDate(shift.personnelId, shift.shiftDate)) {
            return Result.failure(Exception("Bu personelin seçilen tarihte onaylanmış izni bulunmaktadır. Vardiya güncellenemez."))
        }

        workforceRepository.updateShift(shift)
        return Result.success(Unit)
    }

    suspend fun cancelShift(shift: ShiftEntity): Result<Unit> {
        val cancelledShift = shift.copy(status = "CANCELLED")
        workforceRepository.updateShift(cancelledShift)
        return Result.success(Unit)
    }

    suspend fun deleteShift(shift: ShiftEntity): Result<Unit> {
        workforceRepository.deleteShift(shift)
        return Result.success(Unit)
    }
}