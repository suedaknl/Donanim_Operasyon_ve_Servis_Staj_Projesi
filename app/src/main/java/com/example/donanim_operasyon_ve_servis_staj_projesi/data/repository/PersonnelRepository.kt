package com.example.donanim_operasyon_ve_servis_staj_projesi.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PersonnelDao
import kotlinx.coroutines.flow.Flow
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestorePersonnelDataSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PersonnelRepository @Inject constructor(
    private val personnelDao: PersonnelDao
) {
    private val firestoreDataSource: FirestorePersonnelDataSource = FirestorePersonnelDataSource()

    fun getAllPersonnel(): Flow<List<Personnel>> {
        return personnelDao.getAllPersonnel()
    }

    suspend fun insertPersonnel(personnel: Personnel) {
        personnelDao.insertPersonnel(personnel)
    }

    suspend fun updatePersonnel(personnel: Personnel) {
        personnelDao.updatePersonnel(personnel)
    }

    suspend fun deletePersonnel(personnel: Personnel) {
        personnelDao.deletePersonnel(personnel)
    }

    // Kullanıcı adını veritabanında arayan fonksiyon
    suspend fun getPersonnelByUsername(username: String): Personnel? {
        return personnelDao.getPersonnelByUsername(username)
    }

    suspend fun getPersonnelById(id: Int): Personnel? {
        return personnelDao.getPersonnelById(id)
    }

    suspend fun getPersonnelByEmail(email: String): Personnel? {
        return personnelDao.getPersonnelByEmail(email)
    }

    suspend fun testSyncPersonnelToFirestore(personnel: Personnel): Result<Unit> {
        return firestoreDataSource.savePersonnel(personnel)
    }

    suspend fun testGetPersonnelFromFirestore(uid: String): Result<Personnel?> {
        return firestoreDataSource.getPersonnel(uid)
    }

    suspend fun updateLocation(uid: String, lat: Double, lon: Double) {
        firestoreDataSource.updatePersonnelLocation(uid, lat, lon)
    }

    // --- YENİ EKLENEN FONKSİYON: Admin Haritası İçin Canlı Konumları Çekme ---
    suspend fun getLivePersonnelLocations(): Result<List<Map<String, Any>>> {
        return firestoreDataSource.getPersonnelLocations()
    }

    // --- PERSONEL OLUŞTURMA: FIRESTORE VE ROOM SENKRONİZASYONU ---
    suspend fun addPersonnelWithFirebaseSync(personnel: Personnel): Result<Unit> {
        return try {
            // 1. Önce Firestore'a kaydet (gender dahil otomatik kaydedilir)
            val firestoreResult = firestoreDataSource.savePersonnel(personnel)

            if (firestoreResult.isSuccess) {
                // 2. Firestore kaydı başarılıysa Room'a ekle
                personnelDao.insertPersonnel(personnel)
                Result.success(Unit)
            } else {
                Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Firestore'a kayıt başarısız oldu."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FAZ 3: FİREBASE'DEKİ PERSONELLERİ ROOM'A SENKRONİZE ETME ---
    suspend fun syncAllPersonnel() {
        val result = firestoreDataSource.getAllPersonnel()
        result.onSuccess { remotePersonnelList ->
            remotePersonnelList.forEach { remotePersonnel ->
                val firebaseUid = remotePersonnel.firebaseUid
                if (!firebaseUid.isNullOrEmpty()) {
                    val existingPersonnel = personnelDao.getPersonnelByFirebaseUid(firebaseUid)
                    if (existingPersonnel != null) {
                        val personnelToUpdate = remotePersonnel.copy(
                            id = existingPersonnel.id,
                            password = existingPersonnel.password,
                            gender = remotePersonnel.gender // Gender bilgisi senkronizasyonda korunuyor
                        )
                        personnelDao.updatePersonnel(personnelToUpdate)
                    } else {
                        personnelDao.insertPersonnel(remotePersonnel)
                    }
                }
            }
        }
    }
}