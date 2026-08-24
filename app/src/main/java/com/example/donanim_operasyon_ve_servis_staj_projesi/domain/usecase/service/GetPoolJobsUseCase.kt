package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPoolJobsUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    operator fun invoke(): Flow<List<ServiceRecord>> {
        return serviceRepository.getPoolJobs().map { list ->
            list.sortedWith(
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