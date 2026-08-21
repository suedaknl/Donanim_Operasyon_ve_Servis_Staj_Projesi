package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import javax.inject.Inject

class DeleteServiceUseCase @Inject constructor(
    private val repository: ServiceRepository
) {

    suspend operator fun invoke(record: ServiceRecord): Result<Unit> {
        return try {
            repository.deleteRecord(record)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}