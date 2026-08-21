package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.personnel

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import javax.inject.Inject

class DeletePersonnelUseCase @Inject constructor(
    private val personnelRepository: PersonnelRepository,
    private val serviceRepository: ServiceRepository
) {

    suspend operator fun invoke(personnel: Personnel): Result<Unit> {
        return try {
            val assignedServices =
                serviceRepository.getRecordsByPersonnelId(personnel.id)

            assignedServices.forEach { service ->

                val isHistorical =
                    service.status == ServiceStatus.TAMAMLANDI ||
                            service.status == ServiceStatus.IPTAL

                if (!isHistorical) {
                    val updatedService = service.copy(
                        assignedPersonnelId = null,
                        assignedPersonnelUid = null,
                        status = ServiceStatus.BEKLIYOR,
                        rejectionReason = null
                    )

                    serviceRepository.updateService(updatedService)
                }
            }

            personnelRepository.deletePersonnel(personnel)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}