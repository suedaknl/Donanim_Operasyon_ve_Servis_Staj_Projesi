package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)

    @Suppress("FunctionName")
    suspend fun updateShift(shift: ShiftEntity) = update(shift)

    @Query("SELECT * FROM shifts WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): ShiftEntity?

    @Transaction
    suspend fun upsertByFirestoreId(shift: ShiftEntity) {
        if (!shift.firestoreId.isNullOrBlank()) {
            val existing = getByFirestoreId(shift.firestoreId!!)
            if (existing != null) {
                val updated = shift.copy(id = existing.id)
                update(updated)
                return
            }
        }
        insertShift(shift)
    }

    @Query("SELECT * FROM shifts WHERE personnelId = :personnelId ORDER BY id DESC")
    fun getByPersonnel(personnelId: Int): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE shiftDate = :date")
    fun getByDate(date: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE personnelId = :personnelId AND shiftDate = :date LIMIT 1")
    suspend fun getTodayShiftForPersonnel(personnelId: Int, date: String): ShiftEntity?
}