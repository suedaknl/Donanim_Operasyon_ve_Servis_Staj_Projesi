package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosingSignatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: ServiceClosingSignature)

    @Query("SELECT * FROM service_closing_signatures WHERE serviceRecordId = :serviceId LIMIT 1")
    fun getSignatureByServiceRecordId(serviceId: Int): Flow<ServiceClosingSignature?>
}