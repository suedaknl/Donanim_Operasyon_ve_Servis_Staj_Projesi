package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ServiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ServiceRecord)

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
}