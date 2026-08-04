package com.example.donanim_operasyon_ve_servis_staj_projesi.data

import androidx.room.*

@Dao
interface ServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ServiceRecord)

    @Delete
    suspend fun deleteRecord(record: ServiceRecord)

    // Yeni: Sadece durum alanını ID'ye göre güncelleyen SQL sorgusu
    @Query("UPDATE service_records SET status = :newStatus WHERE id = :recordId")
    suspend fun updateStatus(recordId: Int, newStatus: String)

    @Query("SELECT * FROM service_records")
    suspend fun getAllRecords(): List<ServiceRecord>
}