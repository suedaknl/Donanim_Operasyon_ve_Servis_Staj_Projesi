package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class SortServiceRecordsUseCase @Inject constructor() {

    operator fun invoke(records: List<ServiceRecord>): List<ServiceRecord> {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

        return records.sortedWith(
            compareByDescending<ServiceRecord> { record ->
                // 1. Kriter: Öncelik Ağırlığı (Yüksek ağırlık üstte yer alır)
                getPriorityWeight(record.priority)
            }.thenBy { record ->
                // 2. Kriter: Aynı öncelikte planlanan tarih/saat (Erken tarih üstte yer alır)
                parseDateSafely(record.plannedDate, dateFormat)
            }
        )
    }

    private fun getPriorityWeight(priority: String?): Int {
        return when (priority?.trim()?.lowercase(Locale("tr", "TR"))) {
            "acil" -> 4
            "yüksek" -> 3
            "orta", "normal" -> 2
            "düşük" -> 1
            else -> 0 // Tanımsız veya boş öncelikler en altta kalır
        }
    }

    private fun parseDateSafely(dateStr: String?, format: SimpleDateFormat): Long {
        if (dateStr.isNullOrBlank()) return Long.MAX_VALUE // Tarihi olmayanlar en arkaya düşer
        return try {
            format.parse(dateStr)?.time ?: Long.MAX_VALUE
        } catch (e: Exception) {
            Long.MAX_VALUE // Parse edilemeyenler en arkaya düşer
        }
    }
}