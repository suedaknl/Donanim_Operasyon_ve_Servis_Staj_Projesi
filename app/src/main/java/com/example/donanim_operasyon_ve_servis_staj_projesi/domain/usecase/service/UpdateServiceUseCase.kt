package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import javax.inject.Inject

class UpdateServiceUseCase @Inject constructor(
    private val repository: ServiceRepository
) {

    suspend operator fun invoke(record: ServiceRecord): Result<Unit> {
        return try {
            val currentRecord = repository.getServiceById(record.id)
                ?: return Result.failure(Exception("İş emri bulunamadı."))

            if (currentRecord.status == ServiceStatus.TAMAMLANDI) {
                return Result.failure(
                    Exception("Tamamlanmış bir iş emrinin detayları değiştirilemez.")
                )
            }

            repository.updateService(record)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}