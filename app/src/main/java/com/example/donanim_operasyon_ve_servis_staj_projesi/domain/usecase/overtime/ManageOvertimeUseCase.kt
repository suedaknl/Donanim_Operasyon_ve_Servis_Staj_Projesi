package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.overtime

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ManageOvertimeUseCase @Inject constructor(
    private val workforceRepository: WorkforceRepository
) {
    fun getPersonnelOvertimes(personnelId: Int): Flow<List<OvertimeEntity>> {
        return workforceRepository.getOvertimesByPersonnel(personnelId)
    }

    fun getAllOvertimes(): Flow<List<OvertimeEntity>> {
        return workforceRepository.getAllOvertimes()
    }

    suspend fun createOvertime(
        personnelId: Int,
        serviceRecordId: Int?,
        startTime: Long,
        endTime: Long,
        description: String?
    ): Result<Unit> {
        if (startTime >= endTime) {
            return Result.failure(Exception("Başlangıç zamanı bitiş zamanından önce olmalıdır."))
        }

        val durationMillis = endTime - startTime
        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()

        val overtime = OvertimeEntity(
            personnelId = personnelId,
            serviceRecordId = serviceRecordId,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            description = description,
            status = "PENDING"
        )
        workforceRepository.insertOvertime(overtime)
        return Result.success(Unit)
    }

    suspend fun updateOvertime(overtime: OvertimeEntity): Result<Unit> {
        if (overtime.startTime >= overtime.endTime) {
            return Result.failure(Exception("Başlangıç zamanı bitiş zamanından önce olmalıdır."))
        }
        val durationMillis = overtime.endTime - overtime.startTime
        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()

        val updated = overtime.copy(durationMinutes = durationMinutes)
        workforceRepository.updateOvertime(updated)
        return Result.success(Unit)
    }
}