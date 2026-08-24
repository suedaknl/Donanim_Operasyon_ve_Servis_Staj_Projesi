package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.ServiceRegistryStats
import javax.inject.Inject

class GetServiceRegistryStatsUseCase @Inject constructor() {
    operator fun invoke(
        allRecords: List<ServiceRecord>,
        filteredRecords: List<ServiceRecord>
    ): ServiceRegistryStats {
        val totalCompanyServices = filteredRecords.size
        val completedCompanyServices = filteredRecords.count {
            it.status.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true) || it.status.equals("Tamamlandı", ignoreCase = true)
        }

        val companyLastServiceDate = filteredRecords.maxByOrNull { it.date }?.date

        val latestRecord = filteredRecords.maxByOrNull { it.date }
        val deviceServiceCount = filteredRecords.size
        val deviceLastServiceDate = latestRecord?.date
        val deviceLastIssue = latestRecord?.issueDescription

        // Son 6 ay içindeki servis sayısı (180 gün)
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        val deviceServicesLastSixMonths = filteredRecords.count { record ->
            (record.archivedAt ?: 0L) >= sixMonthsAgo || record.date.isNotBlank()
        }

        return ServiceRegistryStats(
            totalCompanyServices = totalCompanyServices,
            completedCompanyServices = completedCompanyServices,
            companyLastServiceDate = companyLastServiceDate,
            deviceServiceCount = deviceServiceCount,
            deviceLastServiceDate = deviceLastServiceDate,
            deviceLastIssue = deviceLastIssue,
            deviceServicesLastSixMonths = deviceServicesLastSixMonths
        )
    }
}