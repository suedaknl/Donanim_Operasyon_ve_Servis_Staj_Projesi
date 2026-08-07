package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord

class ServiceRepository(private val serviceDao: ServiceDao) {

    suspend fun insertRecord(record: ServiceRecord) {
        serviceDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: ServiceRecord) {
        serviceDao.deleteRecord(record)
    }

    suspend fun updateStatus(recordId: Int, newStatus: String) {
        serviceDao.updateStatus(recordId, newStatus)
    }

    suspend fun getAllRecords(): List<ServiceRecord> {
        return serviceDao.getAllRecords()
    }

    suspend fun updateService(service: ServiceRecord) {
        serviceDao.updateService(service)
    }

    suspend fun getServiceById(id: Int): ServiceRecord? {
        return serviceDao.getServiceById(id)
    }

    suspend fun clearAssignedPersonnel(personnelId: Int) {
        serviceDao.clearAssignedPersonnel(personnelId)
    }

    // --- AŞAMA 2.1 İÇİN EKLENEN REPOSITORY FONKSİYONU ---
    suspend fun getRecordsByPersonnelId(personnelId: Int): List<ServiceRecord> {
        return serviceDao.getRecordsByPersonnelId(personnelId)
    }
}