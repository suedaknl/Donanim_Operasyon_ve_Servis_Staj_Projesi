package com.example.donanim_operasyon_ve_servis_staj_projesi.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.datasource.FirestoreWorkforceDataSource
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkforceRepository @Inject constructor(
    private val firestoreDataSource: FirestoreWorkforceDataSource,
    private val shiftDao: ShiftDao,
    private val leaveRequestDao: LeaveRequestDao,
    private val overtimeDao: OvertimeDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // Firestore izin talepleri değişikliklerini dinleyip upsert ile senkronize ediyoruz
        repositoryScope.launch {
            try {
                firestoreDataSource.observeLeaveRequests().collectLatest { remoteLeaves ->
                    remoteLeaves.forEach { leave ->
                        leaveRequestDao.upsertByFirestoreId(leave)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Firestore vardiya değişikliklerini dinleyip upsert ile senkronize ediyoruz (Realtime Sync)
        repositoryScope.launch {
            try {
                firestoreDataSource.observeAllShifts().collectLatest { remoteShifts ->
                    remoteShifts.forEach { shift ->
                        shiftDao.upsertByFirestoreId(shift)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Shift Operations ---
    suspend fun insertShift(shift: ShiftEntity) {
        val tempFirestoreId = if (shift.firestoreId.isNullOrBlank()) {
            java.util.UUID.randomUUID().toString()
        } else {
            shift.firestoreId!!
        }
        val preAssignedShift = shift.copy(firestoreId = tempFirestoreId)

        shiftDao.upsertByFirestoreId(preAssignedShift)

        val firestoreId = firestoreDataSource.saveShift(preAssignedShift)
        if (firestoreId != tempFirestoreId) {
            shiftDao.upsertByFirestoreId(preAssignedShift.copy(firestoreId = firestoreId))
        }
    }

    suspend fun updateShift(shift: ShiftEntity) {
        firestoreDataSource.updateShift(shift)
        shiftDao.update(shift)
    }

    fun getShiftsByPersonnel(personnelId: Int): Flow<List<ShiftEntity>> {
        return shiftDao.getByPersonnel(personnelId)
    }

    fun getShiftsByDate(date: String): Flow<List<ShiftEntity>> = shiftDao.getByDate(date)

    suspend fun getTodayShiftForPersonnel(personnelId: Int, date: String): ShiftEntity? =
        shiftDao.getTodayShiftForPersonnel(personnelId, date)

    // --- Leave Request Operations ---
    suspend fun insertLeaveRequest(leaveRequest: LeaveRequestEntity) {
        val tempFirestoreId = if (leaveRequest.firestoreId.isNullOrBlank()) {
            java.util.UUID.randomUUID().toString()
        } else {
            leaveRequest.firestoreId!!
        }

        val preAssignedLeave = leaveRequest.copy(firestoreId = tempFirestoreId)
        leaveRequestDao.upsertByFirestoreId(preAssignedLeave)

        val firestoreId = firestoreDataSource.createLeaveRequest(preAssignedLeave)
        if (firestoreId != tempFirestoreId) {
            leaveRequestDao.upsertByFirestoreId(preAssignedLeave.copy(firestoreId = firestoreId))
        }
    }

    suspend fun updateLeaveRequest(leaveRequest: LeaveRequestEntity) {
        firestoreDataSource.updateLeaveRequest(leaveRequest)
        leaveRequestDao.update(leaveRequest)
    }

    fun getLeaveRequestsByPersonnel(personnelId: Int): Flow<List<LeaveRequestEntity>> =
        leaveRequestDao.getByPersonnel(personnelId)

    fun getPendingLeaveRequests(): Flow<List<LeaveRequestEntity>> =
        leaveRequestDao.getPendingRequests()

    suspend fun getApprovedLeavesInRange(startDate: String, endDate: String) =
        leaveRequestDao.getApprovedRequestsInDateRange(startDate, endDate)

    fun getAllLeaveRequests(): Flow<List<LeaveRequestEntity>> =
        leaveRequestDao.getAll()

    // --- Overtime Operations ---
    suspend fun insertOvertime(overtime: OvertimeEntity) {
        val firestoreId = firestoreDataSource.createOvertime(overtime)
        overtimeDao.insert(overtime.copy(firestoreId = firestoreId))
    }

    suspend fun updateOvertime(overtime: OvertimeEntity) {
        firestoreDataSource.updateOvertime(overtime)
        overtimeDao.update(overtime)
    }

    fun getOvertimesByPersonnel(personnelId: Int): Flow<List<OvertimeEntity>> =
        overtimeDao.getByPersonnel(personnelId)

    fun getOvertimesByServiceId(serviceId: Int): Flow<List<OvertimeEntity>> =
        overtimeDao.getByServiceId(serviceId)

    fun getAllOvertimes(): Flow<List<OvertimeEntity>> =
        overtimeDao.getAll()
}