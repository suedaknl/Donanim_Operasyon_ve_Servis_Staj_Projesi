package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import kotlinx.coroutines.flow.Flow

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

    // --- FAZ 2.3 İÇİN EKLENEN SERVİS NOTU FONKSİYONLARI ---

    suspend fun insertServiceNote(note: ServiceNote) {
        serviceDao.insertServiceNote(note)
    }
    suspend fun getClosingSignatureByServiceId(serviceId: Int): ServiceClosingSignature? {
        return serviceDao.getClosingSignature(serviceId)
    }

    fun getNotesForService(serviceRecordId: Int): Flow<List<ServiceNote>> {
        return serviceDao.getNotesForService(serviceRecordId)
    }

    // --- FAZ 2.5 İÇİN EKLENEN FOTOĞRAF FONKSİYONLARI ---

    suspend fun insertServicePhoto(photo: ServicePhoto) {
        serviceDao.insertServicePhoto(photo)
    }

    fun getPhotosForService(serviceRecordId: Int): Flow<List<ServicePhoto>> {
        return serviceDao.getPhotosForService(serviceRecordId)
    }

    suspend fun deleteServicePhoto(photo: ServicePhoto) {
        serviceDao.deleteServicePhoto(photo)
    }

    // --- KAPANIŞ İŞLEMİ (TRANSACTION) İÇİN EKLENEN FONKSİYON ---
    suspend fun completeServiceWork(
        serviceRecord: ServiceRecord,
        personnelId: Int,
        closingNoteText: String,
        signatureUri: String
    ): Result<Unit> {
        return try {
            // 1. YETKİ VE DURUM KONTROLLERİ
            if (serviceRecord.assignedPersonnelId != personnelId) {
                throw Exception("Hata: Bu iş emrini kapatma yetkiniz yok.")
            }
            if (serviceRecord.status != ServiceStatus.ISLEME_BASLANDI) {
                throw Exception("Hata: İş emri kapatılabilmesi için 'İşleme Başlandı' durumunda olmalıdır.")
            }
            if (closingNoteText.isBlank() || signatureUri.isBlank()) {
                throw Exception("Hata: Kapanış notu ve imza zorunludur.")
            }

            // 2. KAPANIŞ NOTU NESNESİ OLUŞTURMA
            val closingNote = ServiceNote(
                serviceRecordId = serviceRecord.id,
                personnelId = personnelId,
                note = closingNoteText,
                noteType = "CLOSING", // Kapanış notu olduğunu belirtiyoruz
                createdAt = System.currentTimeMillis()
            )

            // 3. İMZA NESNESİ OLUŞTURMA
            val signature = ServiceClosingSignature(
                serviceRecordId = serviceRecord.id,
                personnelId = personnelId,
                signatureLocalUri = signatureUri,
                createdAt = System.currentTimeMillis()
            )

            // 4. İŞ EMRİ DURUMUNU GÜNCELLEME
            val updatedRecord = serviceRecord.copy(status = ServiceStatus.TAMAMLANDI)

            // 5. ATOMİK İŞLEMİ ÇAĞIRMA (DAO'daki transaction metodu)
            serviceDao.completeServiceTransaction(updatedRecord, closingNote, signature)

            // İşlem başarılıysa Unit döndür
            Result.success(Unit)
        } catch (e: Exception) {
            // Herhangi bir adımda hata olursa yakala ve UI'a bildir
            Result.failure(e)
        }
    }


}