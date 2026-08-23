package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OvertimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(overtime: OvertimeEntity)

    @Update
    suspend fun update(overtime: OvertimeEntity)

    @Query("SELECT * FROM overtimes WHERE personnelId = :personnelId ORDER BY createdAt DESC")
    fun getByPersonnel(personnelId: Int): Flow<List<OvertimeEntity>>

    @Query("SELECT * FROM overtimes WHERE serviceRecordId = :serviceId")
    fun getByServiceId(serviceId: Int): Flow<List<OvertimeEntity>>

    @Query("SELECT * FROM overtimes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<OvertimeEntity>>
}