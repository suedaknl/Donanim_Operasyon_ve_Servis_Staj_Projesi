package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import javax.inject.Inject

class ArchiveServiceUseCase @Inject constructor(
    private val repository: ServiceRepository
) {
    suspend operator fun invoke(recordId: Int): Result<Unit> {
        return try {
            val record = repository.getServiceById(recordId)
                ?: return Result.failure(Exception("İş emri bulunamadı."))

            if (record.status != ServiceStatus.TAMAMLANDI && record.status != ServiceStatus.IPTAL) {
                return Result.failure(Exception("Sadece tamamlanmış veya iptal edilmiş işler arşivlenebilir."))
            }

            val archivedRecord = record.copy(
                isArchived = true,
                archivedAt = System.currentTimeMillis()
            )

            repository.updateService(archivedRecord)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}