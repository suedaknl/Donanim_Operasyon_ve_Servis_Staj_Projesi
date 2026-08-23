package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.overtime

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class DetectOvertimeUseCase @Inject constructor(
    private val workforceRepository: WorkforceRepository
) {
    suspend fun detectAndCreateOvertime(serviceRecord: ServiceRecord) {
        val personnelId = serviceRecord.assignedPersonnelId ?: return

        // İşin tamamlandığı an (Mevcut zaman damgası)
        val completionTime = System.currentTimeMillis()

        // Aynı serviceRecordId için zaten fazla mesai kaydı var mı kontrol et (Duplicate önleme)
        val existingOvertimes = workforceRepository.getOvertimesByServiceId(serviceRecord.id).firstOrNull() ?: emptyList()
        if (existingOvertimes.isNotEmpty()) {
            return
        }

        // İşin tamamlandığı tarihi belirle (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val completionDateStr = dateFormat.format(java.util.Date(completionTime))

        // O personelin o günkü vardiyasını al
        val todayShift = workforceRepository.getTodayShiftForPersonnel(personnelId, completionDateStr) ?: return
        if (todayShift.status == "CANCELLED") return

        // Vardiya bitiş saatini timestamp'e çevir (örn: "17:00" -> timestamp)
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val shiftEndDateTimeStr = "${todayShift.shiftDate} ${todayShift.endTime}"

        val shiftEndMillis = try {
            timeFormat.parse(shiftEndDateTimeStr)?.time ?: return
        } catch (e: Exception) {
            return
        }

        // Eğer iş gerçekten vardiya bitişinden SONRA tamamlandıysa fazla mesai hesapla
        if (completionTime > shiftEndMillis) {
            val durationMillis = completionTime - shiftEndMillis
            val durationMinutes = (durationMillis / (1000 * 60)).toInt()

            if (durationMinutes > 0) {
                val overtime = OvertimeEntity(
                    personnelId = personnelId,
                    serviceRecordId = serviceRecord.id,
                    startTime = shiftEndMillis,
                    endTime = completionTime,
                    durationMinutes = durationMinutes,
                    description = "İş emri #${serviceRecord.id} vardiya bitişinden sonra tamamlandı.",
                    status = "PENDING"
                )
                workforceRepository.insertOvertime(overtime)
            }
        }
    }
}