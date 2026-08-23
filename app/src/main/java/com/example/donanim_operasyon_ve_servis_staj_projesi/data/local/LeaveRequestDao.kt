package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leaveRequest: LeaveRequestEntity)

    @Update
    suspend fun update(leaveRequest: LeaveRequestEntity)

    @Query("SELECT * FROM leave_requests WHERE personnelId = :personnelId ORDER BY createdAt DESC")
    fun getByPersonnel(personnelId: Int): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingRequests(): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE status = 'APPROVED' AND startDate <= :endDate AND endDate >= :startDate")
    suspend fun getApprovedRequestsInDateRange(startDate: String, endDate: String): List<LeaveRequestEntity>

    @Query("SELECT * FROM leave_requests ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LeaveRequestEntity>>
}