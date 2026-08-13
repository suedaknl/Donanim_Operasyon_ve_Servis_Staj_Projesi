package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ServiceRecord)

    // --- KAPANIŞ İŞLEMİ İÇİN EKSİK OLAN METOT EKLENDİ ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: ServiceClosingSignature)

    // ATOMİK İŞLEM: Ya hepsi gerçekleşir, ya hiçbiri!
    @Transaction
    suspend fun completeServiceTransaction(
        updatedRecord: ServiceRecord,
        closingNote: ServiceNote,
        signature: ServiceClosingSignature
    ) {
        updateService(updatedRecord)
        insertServiceNote(closingNote)
        insertSignature(signature)
    }

    @Delete
    suspend fun deleteRecord(record: ServiceRecord)

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun getServiceById(id: Int): ServiceRecord?

    @Query("SELECT * FROM service_records")
    suspend fun getAllRecords(): List<ServiceRecord>

    @Update
    suspend fun updateService(service: ServiceRecord)

    @Query("UPDATE service_records SET status = :newStatus WHERE id = :recordId")
    suspend fun updateStatus(recordId: Int, newStatus: String)

    @Query("UPDATE service_records SET assignedPersonnelId = NULL WHERE assignedPersonnelId = :personnelId")
    suspend fun clearAssignedPersonnel(personnelId: Int)

    // --- AŞAMA 2.1 İÇİN EKLENEN DAO METODU ---
    @Query("SELECT * FROM service_records WHERE assignedPersonnelId = :personnelId")
    suspend fun getRecordsByPersonnelId(personnelId: Int): List<ServiceRecord>

    // --- FAZ 2.3 İÇİN EKLENEN SERVİS NOTU METOTLARI ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceNote(note: ServiceNote)

    @Query("""
        SELECT * FROM service_notes 
        WHERE serviceRecordId = :serviceRecordId 
        ORDER BY createdAt DESC
    """)
    fun getNotesForService(serviceRecordId: Int): Flow<List<ServiceNote>>

    // --- FAZ 2.5 İÇİN EKLENEN FOTOĞRAF METOTLARI ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicePhoto(photo: ServicePhoto)

    @Query("""
        SELECT * FROM service_photos 
        WHERE serviceRecordId = :serviceRecordId 
        ORDER BY timestamp DESC
    """)
    fun getPhotosForService(serviceRecordId: Int): Flow<List<ServicePhoto>>

    @Delete
    suspend fun deleteServicePhoto(photo: ServicePhoto)

    @Query("SELECT * FROM service_closing_signatures WHERE serviceRecordId = :serviceId LIMIT 1")
    suspend fun getClosingSignature(serviceId: Int): ServiceClosingSignature?

    // --- FIRESTORE SENKRONİZASYONU İÇİN EKLENEN ---
    @Query("SELECT * FROM service_records WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getServiceByFirestoreId(firestoreId: String): ServiceRecord?

}