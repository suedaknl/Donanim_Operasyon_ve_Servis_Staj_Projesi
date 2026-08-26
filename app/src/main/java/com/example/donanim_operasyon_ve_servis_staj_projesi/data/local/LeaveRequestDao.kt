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
interface LeaveRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leaveRequest: LeaveRequestEntity): Long

    @Update
    suspend fun update(leaveRequest: LeaveRequestEntity)

    @Delete
    suspend fun delete(leaveRequest: LeaveRequestEntity)

    @Query("DELETE FROM leave_requests WHERE firestoreId NOT IN (:firestoreIds)")
    suspend fun deleteNotInFirestoreIds(firestoreIds: Set<String>)

    @Query("SELECT * FROM leave_requests WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): LeaveRequestEntity?

    @Transaction
    suspend fun upsertByFirestoreId(leaveRequest: LeaveRequestEntity) {
        if (!leaveRequest.firestoreId.isNullOrBlank()) {
            val existing = getByFirestoreId(leaveRequest.firestoreId!!)
            if (existing != null) {
                val updated = leaveRequest.copy(id = existing.id)
                update(updated)
                return
            }
        }
        insert(leaveRequest)
    }

    @Query("SELECT * FROM leave_requests WHERE personnelId = :personnelId ORDER BY id DESC")
    fun getByPersonnel(personnelId: Int): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY id DESC")
    fun getPendingRequests(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE status = 'APPROVED' AND startDate <= :endDate AND endDate >= :startDate")
    suspend fun getApprovedRequestsInDateRange(startDate: String, endDate: String): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests ORDER BY id DESC")
    fun getAll(): Flow<List<LeaveRequestEntity>>
}