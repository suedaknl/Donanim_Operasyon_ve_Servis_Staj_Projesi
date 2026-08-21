package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import javax.inject.Inject

class UpdateServiceStatusUseCase @Inject constructor(
    private val repository: ServiceRepository
) {

    suspend operator fun invoke(
        recordId: Int,
        newStatus: String
    ) {
        repository.updateStatus(recordId, newStatus)
    }
}