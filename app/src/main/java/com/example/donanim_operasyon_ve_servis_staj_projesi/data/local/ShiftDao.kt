package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE personnelId = :personnelId ORDER BY shiftDate DESC")
    fun getByPersonnel(personnelId: Int): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE shiftDate = :date ORDER BY startTime ASC")
    fun getByDate(date: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE personnelId = :personnelId AND shiftDate = :date LIMIT 1")
    suspend fun getTodayShiftForPersonnel(personnelId: Int, date: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE shiftDate = :date")
    suspend fun getAllForDate(date: String): List<ShiftEntity>
}