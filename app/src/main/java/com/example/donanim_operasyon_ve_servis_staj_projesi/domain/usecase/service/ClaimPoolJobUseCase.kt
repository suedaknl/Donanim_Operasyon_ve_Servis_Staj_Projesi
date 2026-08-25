package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import java.text.SimpleDateFormat
import java.util.Calendar
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
        isOnLeave: Boolean,
        poolAssignmentDeadline: Long? = null
    ): Result<Unit> {
        // 1. Aktif İş Engeli Kontrolü
        if (hasActiveJob) {
            return Result.failure(Exception("Üzerinizde devam eden aktif bir iş bulunmaktadır."))
        }

        // 2. Havuz Son Atama (Deadline) Kontrolü
        val currentTime = System.currentTimeMillis()
        if (poolAssignmentDeadline != null && currentTime > poolAssignmentDeadline) {
            return Result.failure(Exception("Bu işin havuzda kalma süresi doldu. Yönetici ataması bekleniyor."))
        }

        // 3. Planlanan Tarih Varlığı ve Geçmiş Gün Kontrolü
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

        // Gün bazlı karşılaştırma (Bugünün başlangıcı vs İşin günü)
        val calendarToday = Calendar.getInstance(trLocale).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val jobCalendar = Calendar.getInstance(trLocale).apply {
            time = jobDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (jobCalendar.timeInMillis < calendarToday.timeInMillis) {
            return Result.failure(Exception("Planlanan tarihi geçmiş olan iş emri üstlenilemez."))
        }

        // 4. Onaylı İzin Çakışma Kontrolü
        if (isOnLeave) {
            return Result.failure(Exception("Bu işin planlanan tarihinde onaylanmış izniniz bulunmaktadır."))
        }

        // 5. Firestore Transaction ile Atomik Claim İşlemi
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