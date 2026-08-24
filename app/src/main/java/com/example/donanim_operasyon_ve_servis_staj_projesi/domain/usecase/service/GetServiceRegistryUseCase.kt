package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import javax.inject.Inject

class GetServiceRegistryUseCase @Inject constructor() {
    operator fun invoke(records: List<ServiceRecord>, query: String): List<ServiceRecord> {
        val trimmedQuery = query.trim().lowercase()
        if (trimmedQuery.isBlank()) {
            return records.sortedByDescending { it.date }
        }

        return records.filter { record ->
            val company = record.companyName.lowercase()
            val serial = record.serialNumber.lowercase()
            val model = record.deviceModel.lowercase()
            val type = record.deviceType.lowercase()

            company.contains(trimmedQuery) ||
                    serial.contains(trimmedQuery) ||
                    model.contains(trimmedQuery) ||
                    type.contains(trimmedQuery)
        }.sortedByDescending { it.date }
    }
}