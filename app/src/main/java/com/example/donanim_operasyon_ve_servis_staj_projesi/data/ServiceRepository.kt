package com.example.donanim_operasyon_ve_servis_staj_projesi.data

class ServiceRepository(private val serviceDao: ServiceDao) {

    suspend fun insertRecord(record: ServiceRecord) {
        serviceDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: ServiceRecord) {
        serviceDao.deleteRecord(record)
    }

    // Yeni: Durum güncelleme köprüsü
    suspend fun updateStatus(recordId: Int, newStatus: String) {
        serviceDao.updateStatus(recordId, newStatus)
    }

    suspend fun getAllRecords(): List<ServiceRecord> {
        return serviceDao.getAllRecords()
    }
}