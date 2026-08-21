package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import javax.inject.Inject

class StartServiceWorkUseCase @Inject constructor(
    private val repository: ServiceRepository
) {

    suspend operator fun invoke(
        recordId: Int,
        personnelId: Int,
        distance: Float
    ) {
        repository.verifyAndStartServiceWork(
            recordId = recordId,
            personnelId = personnelId,
            distance = distance
        )
    }
}