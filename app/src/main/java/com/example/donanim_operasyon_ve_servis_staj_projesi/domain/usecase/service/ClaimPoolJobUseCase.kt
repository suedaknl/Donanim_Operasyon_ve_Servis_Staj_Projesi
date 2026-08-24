package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ClaimPoolJobUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    suspend operator fun invoke(
        serviceId: Int,
        firestoreId: String,
        personnelId: Int,
        personnelName: String,
        personnelUid: String,
        hasActiveJob: Boolean,
        plannedDateStr: String?,
        isOnLeave: Boolean
    ): Result<Unit> {
        // 1. Aktif İş Engeli Kontrolü
        if (hasActiveJob) {
            return Result.failure(Exception("Üzerinizde devam eden aktif bir iş bulunmaktadır."))
        }

        // 2. Planlanan Tarih Varlığı ve Geçmiş Zaman Kontrolü (Fail-Close)
        if (plannedDateStr.isNullOrBlank()) {
            return Result.failure(Exception("İş emrinin planlanan tarihi doğrulanamadı."))
        }

        val trLocale = Locale.forLanguageTag("tr-TR")
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", trLocale).apply {
            isLenient = false
        }
        val dayOnlyFormat = SimpleDateFormat("dd.MM.yyyy", trLocale).apply {
            isLenient = false
        }

        val jobDate: Date = try {
            dateFormat.parse(plannedDateStr.trim()) ?: dayOnlyFormat.parse(plannedDateStr.take(10).trim())
            ?: return Result.failure(Exception("İş emrinin planlanan tarihi doğrulanamadı."))
        } catch (e: Exception) {
            return Result.failure(Exception("İş emrinin planlanan tarihi doğrulanamadı."))
        }

        val currentTime = System.currentTimeMillis()
        if (jobDate.time < currentTime) {
            return Result.failure(Exception("Planlanan zamanı geçmiş bir iş emrini üstlenemezsiniz."))
        }

        // 3. Onaylı İzin Çakışma Kontrolü
        if (isOnLeave) {
            return Result.failure(Exception("Bu işin planlanan tarihinde onaylanmış izniniz bulunmaktadır."))
        }

        // 4. Firestore Transaction ile Atomik Claim İşlemi
        if (firestoreId.isBlank()) {
            return Result.failure(Exception("Hata: İş emri Firestore ID bilgisi bulunamadı."))
        }

        val result = serviceRepository.claimPoolJob(
            serviceId = serviceId,
            firestoreId = firestoreId,
            personnelId = personnelId,
            personnelName = personnelName,
            personnelUid = personnelUid
        )

        return if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Bu iş başka bir personel tarafından az önce üstlenildi."))
        }
    }
}