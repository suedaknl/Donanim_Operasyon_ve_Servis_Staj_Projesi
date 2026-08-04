package com.example.donanim_operasyon_ve_servis_staj_projesi.data

class ServiceRepository(private val serviceDao: ServiceDao) {
    suspend fun insertRecord(record: ServiceRecord) {
        serviceDao.insertRecord(record)
    }

    // Repository üzerinden silme köprüsü
    suspend fun deleteRecord(record: ServiceRecord) {
        serviceDao.deleteRecord(record)
    }

    suspend fun getAllRecords(): List<ServiceRecord> {
        return serviceDao.getAllRecords()
    }
}