package com.example.donanim_operasyon_ve_servis_staj_projesi.data

import androidx.room.*

@Dao
interface ServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ServiceRecord)

    // Silme fonksiyonunu ekledik
    @Delete
    suspend fun deleteRecord(record: ServiceRecord)

    @Query("SELECT * FROM service_records")
    suspend fun getAllRecords(): List<ServiceRecord>
}