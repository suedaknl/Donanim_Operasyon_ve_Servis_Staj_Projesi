package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetPoolJobsUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    operator fun invoke(): Flow<List<ServiceRecord>> {
        return serviceRepository.getPoolJobs().map { list ->
            val trLocale = Locale.forLanguageTag("tr-TR")
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", trLocale).apply { isLenient = false }
            val dayOnlyFormat = SimpleDateFormat("dd.MM.yyyy", trLocale).apply { isLenient = false }
            val todayMillis = System.currentTimeMillis()

            list.filter { record ->
                val isPoolType = record.assignmentType == "POOL" || record.assignedPersonnelId == null
                val notArchived = !record.isArchived
                val notCompleted = record.status != ServiceStatus.TAMAMLANDI
                val notCancelled = record.status != ServiceStatus.IPTAL

                // Tarih Filtresi (İkinci Güvenlik Katmanı): Geçmiş tarihli işler havuzda görünmemeli
                val targetDateStr = record.plannedDate?.takeIf { it.isNotBlank() } ?: record.date
                val recordDate: Date? = try {
                    dateFormat.parse(targetDateStr.trim()) ?: dayOnlyFormat.parse(targetDateStr.take(10).trim())
                } catch (e: Exception) {
                    null
                }

                // Eğer tarih parse edilebiliyorsa ve bugünden (veya geçmişten) eskiyse filtrele
                // Burada gün bazlı kontrol için tarih karşılaştırması yapıyoruz
                val isNotPast = recordDate == null || recordDate.time >= (todayMillis - 86400000L) // Günlük tolerans payı ile

                isPoolType && notArchived && notCompleted && notCancelled && isNotPast
            }.sortedWith(
                compareBy<ServiceRecord> { record ->
                    when (record.priority) {
                        "Yüksek", "Acil" -> 0
                        "Orta" -> 1
                        else -> 2
                    }
                }.thenBy { record ->
                    record.plannedDate ?: record.date
                }
            )
        }
    }
}