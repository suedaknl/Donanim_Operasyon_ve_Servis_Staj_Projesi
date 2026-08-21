package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import javax.inject.Inject

class CompleteServiceUseCase @Inject constructor(
    private val repository: ServiceRepository
) {

    suspend operator fun invoke(
        serviceId: Int,
        personnelId: Int,
        closingNoteText: String,
        signatureUri: String,
        afterPhotoUri: String
    ): Result<Unit> {

        if (closingNoteText.isBlank()) {
            return Result.failure(Exception("Kapanış notu eksik."))
        }

        if (signatureUri.isBlank()) {
            return Result.failure(Exception("Dijital imza eksik."))
        }

        if (afterPhotoUri.isBlank()) {
            return Result.failure(Exception("Sonrası fotoğrafı eksik."))
        }

        val currentRecord = repository.getServiceById(serviceId)
            ?: return Result.failure(Exception("İş emri bulunamadı."))

        val photoEntity = ServicePhoto(
            serviceRecordId = serviceId,
            personnelId = personnelId,
            photoType = "SONRASI",
            localUri = afterPhotoUri,
            timestamp = System.currentTimeMillis(),
            photoUri = afterPhotoUri,
            photoCategory = "SONRASI"
        )

        repository.insertServicePhoto(photoEntity)

        return repository.completeServiceWork(
            serviceRecord = currentRecord,
            personnelId = personnelId,
            closingNoteText = closingNoteText,
            signatureUri = signatureUri
        )
    }
}