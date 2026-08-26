package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leaves: List<LeaveRequestEntity>)

    @Update
    suspend fun update(leaveRequest: LeaveRequestEntity)

    @Query("SELECT * FROM leave_requests WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): LeaveRequestEntity?

    @Transaction
    suspend fun upsertByFirestoreId(leaveRequest: LeaveRequestEntity) {
        if (!leaveRequest.firestoreId.isNullOrBlank()) {
            val existing = getByFirestoreId(leaveRequest.firestoreId)
            if (existing != null) {
                // Yerel Room primary key id'sini KORU
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

    @Query("SELECT * FROM leave_requests WHERE status = 'APPROVED'")
    suspend fun getAllApprovedRequests(): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests WHERE status = 'APPROVED' AND startDate <= :endDate AND endDate >= :startDate")
    suspend fun getApprovedRequestsInDateRange(startDate: String, endDate: String): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests ORDER BY id DESC")
    fun getAll(): Flow<List<LeaveRequestEntity>>
}