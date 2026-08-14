package com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PersonnelDao
import kotlinx.coroutines.flow.Flow
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestoreServiceDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServiceRepository(
    private val serviceDao: ServiceDao,
    private val personnelDao: PersonnelDao, // PersonnelDao eklendi (UID eşlemesi için)
    private val firestoreDataSource: FirestoreServiceDataSource = FirestoreServiceDataSource()
) {

    suspend fun insertRecord(record: ServiceRecord) {
        // 1. Seçilen personelin Room ID'sinden gerçek Firebase UID değerini bul
        val personnelUid = record.assignedPersonnelId?.let { personnelId ->
            try {
                val personnel = personnelDao.getPersonnelById(personnelId)
                personnel?.firebaseUid
            } catch (e: Exception) {
                null
            }
        }

        // 2. assignedPersonnelUid alanını eşlenen Firebase UID ile doldur
        val recordWithPersonnel = record.copy(assignedPersonnelUid = personnelUid)

        // 3. Önce Room'a kaydet (Single Source of Truth)
        serviceDao.insertRecord(recordWithPersonnel)

        // 4. Firestore'a otomatik gönder ve dönen document ID'yi Room kaydına firestoreId olarak güncelle
        withContext(Dispatchers.IO) {
            try {
                // Room'a en son eklenen veya ilgili kaydı bulmak için (veya eşleşen kaydı seçmek)
                val allRecords = serviceDao.getAllRecords()
                val savedRecord = allRecords.find {
                    it.companyName == recordWithPersonnel.companyName &&
                            it.serialNumber == recordWithPersonnel.serialNumber &&
                            it.firestoreId == null
                } ?: recordWithPersonnel

                val result = firestoreDataSource.saveServiceRecord(savedRecord)

                result.onSuccess { firestoreId ->
                    val updatedRecord = savedRecord.copy(firestoreId = firestoreId)
                    serviceDao.updateService(updatedRecord)
                }
            } catch (e: Exception) {
                // Firestore hatası yerel kaydı bozmaz, uygulama çökmEz
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteRecord(record: ServiceRecord) {
        // 1. Önce Room'dan sil
        serviceDao.deleteRecord(record)

        // 2. Firebase'den sil (firestoreId varsa)
        withContext(Dispatchers.IO) {
            try {
                val firestoreId = record.firestoreId
                if (!firestoreId.isNullOrEmpty()) {
                    firestoreDataSource.deleteService(firestoreId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateStatus(recordId: Int, newStatus: String) {
        // 1. Önce Room'daki status güncellensin
        serviceDao.updateStatus(recordId, newStatus)

        // 2. Güncellenen kaydı alarak firestoreId değerine ulaşıyoruz ve Firebase'i güncelliyoruz
        withContext(Dispatchers.IO) {
            try {
                val record = serviceDao.getServiceById(recordId)
                val firestoreId = record?.firestoreId

                if (!firestoreId.isNullOrEmpty()) {
                    firestoreDataSource.updateServiceStatus(firestoreId, newStatus)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    suspend fun getAllRecords(): List<ServiceRecord> {
        return serviceDao.getAllRecords()
    }

    suspend fun updateService(service: ServiceRecord) {
        // 1. Eğer atanmış bir personel varsa, o personelin gerçek Firebase UID'sini bulalım
        val personnelUid = service.assignedPersonnelId?.let { personnelId ->
            try {
                personnelDao.getPersonnelById(personnelId)?.firebaseUid
            } catch (e: Exception) {
                null
            }
        }

        // 2. Service nesnesini güncellenmiş UID ile kopyalayalım
        val serviceToUpdate = service.copy(assignedPersonnelUid = personnelUid)

        // 3. Önce Room'da güncelle
        serviceDao.updateService(serviceToUpdate)

        // 4. Firebase'de güncelle (firestoreId varsa)
        withContext(Dispatchers.IO) {
            try {
                if (!serviceToUpdate.firestoreId.isNullOrEmpty()) {
                    firestoreDataSource.updateService(serviceToUpdate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

            // 2. KAPANIŞ NOTu NESNESİ OLUŞTURMA
            val closingNote = ServiceNote(
                serviceRecordId = serviceRecord.id,
                personnelId = personnelId,
                note = closingNoteText,
                noteType = "CLOSING",
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FAZ 3: ADMİN İÇİN TÜM FİREBASE SERVİSLERİNİ ROOM'A SENKRONİZE ETME ---
    suspend fun syncAllServices() {
        println("SYNC BAŞLADI: Firebase'den iş emirleri çekiliyor...")
        val result = firestoreDataSource.getAllServices()

        result.onSuccess { remoteServices ->
            println("SYNC BAŞARILI: Firebase'den ${remoteServices.size} adet iş emri geldi!")

            val allPersonnel = try {
                personnelDao.getAllPersonnelList()
            } catch (e: Exception) {
                emptyList()
            }

            val allLocalServices = try {
                serviceDao.getAllRecords()
            } catch (e: Exception) {
                emptyList()
            }

            remoteServices.forEach { remoteService ->
                val firestoreId = remoteService.firestoreId
                if (!firestoreId.isNullOrEmpty()) {
                    val existingLocalService = serviceDao.getServiceByFirestoreId(firestoreId)

                    val matchedPersonnelId: Int? = remoteService.assignedPersonnelUid?.let { uid ->
                        allPersonnel.find { it.firebaseUid == uid }?.id
                    }

                    if (existingLocalService != null) {
                        val serviceToUpdate = remoteService.copy(
                            id = existingLocalService.id,
                            assignedPersonnelId = matchedPersonnelId ?: existingLocalService.assignedPersonnelId
                        )
                        serviceDao.updateService(serviceToUpdate)
                        println("SYNC: ${firestoreId} güncellendi.")
                    } else {
                        val ghostRecord = allLocalServices.find {
                            it.firestoreId == null &&
                                    it.companyName == remoteService.companyName &&
                                    it.serialNumber == remoteService.serialNumber
                        }

                        if (ghostRecord != null) {
                            val serviceToUpdate = remoteService.copy(
                                id = ghostRecord.id,
                                assignedPersonnelId = matchedPersonnelId
                            )
                            serviceDao.updateService(serviceToUpdate)
                            println("SYNC: Hayalet kayıt eşleştirildi (${firestoreId}).")
                        } else {
                            val serviceToInsert = remoteService.copy(
                                assignedPersonnelId = matchedPersonnelId
                            )
                            serviceDao.insertRecord(serviceToInsert)
                            println("SYNC: Yeni kayıt Room'a eklendi (${firestoreId}).")
                        }
                    }
                } else {
                    println("SYNC UYARI: Firebase'deki bir kaydın firestoreId'si null!")
                }
            }
        }.onFailure {
            println("SYNC HATASI: Firebase'den veri çekilemedi. Hata: ${it.message}")
        }
    }
    // --- FIRESTORE SENKRONİZASYON FONKSİYONU (PERSONEL) ---
    suspend fun syncServicesFromFirestore(
        personnelUid: String,
        localPersonnelId: Int
    ) {
        val result = firestoreDataSource.getServicesForPersonnel(personnelUid)

        result.onSuccess { remoteServices ->
            remoteServices.forEach { remoteService ->
                val firestoreId = remoteService.firestoreId
                if (!firestoreId.isNullOrEmpty()) {
                    val existingLocalService = serviceDao.getServiceByFirestoreId(firestoreId)

                    val serviceToSave = remoteService.copy(
                        assignedPersonnelId = localPersonnelId,
                        id = existingLocalService?.id ?: 0
                    )

                    if (existingLocalService != null) {
                        serviceDao.updateService(serviceToSave)
                    } else {
                        serviceDao.insertRecord(serviceToSave)
                    }
                }
            }
        }
    }
}