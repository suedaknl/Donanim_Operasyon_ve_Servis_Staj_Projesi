package com.example.donanim_operasyon_ve_servis_staj_projesi.repository

import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkforceRepository @Inject constructor(
    private val shiftDao: ShiftDao,
    private val leaveRequestDao: LeaveRequestDao,
    private val overtimeDao: OvertimeDao
) {
    // --- Shift Operations ---
    suspend fun insertShift(shift: ShiftEntity) = shiftDao.insertShift(shift)
    suspend fun updateShift(shift: ShiftEntity) = shiftDao.updateShift(shift)
    fun getShiftsByPersonnel(personnelId: Int): Flow<List<ShiftEntity>> = shiftDao.getByPersonnel(personnelId)
    fun getShiftsByDate(date: String): Flow<List<ShiftEntity>> = shiftDao.getByDate(date)
    suspend fun getTodayShiftForPersonnel(personnelId: Int, date: String): ShiftEntity? = shiftDao.getTodayShiftForPersonnel(personnelId, date)

    // --- Leave Request Operations ---
    suspend fun insertLeaveRequest(leaveRequest: LeaveRequestEntity) = leaveRequestDao.insert(leaveRequest)
    suspend fun updateLeaveRequest(leaveRequest: LeaveRequestEntity) = leaveRequestDao.update(leaveRequest)
    fun getLeaveRequestsByPersonnel(personnelId: Int): Flow<List<LeaveRequestEntity>> = leaveRequestDao.getByPersonnel(personnelId)
    fun getPendingLeaveRequests(): Flow<List<LeaveRequestEntity>> = leaveRequestDao.getPendingRequests()
    suspend fun getApprovedLeavesInRange(startDate: String, endDate: String) = leaveRequestDao.getApprovedRequestsInDateRange(startDate, endDate)
    fun getAllLeaveRequests(): Flow<List<LeaveRequestEntity>> = leaveRequestDao.getAll()

    // --- Overtime Operations ---
    suspend fun insertOvertime(overtime: OvertimeEntity) = overtimeDao.insert(overtime)
    suspend fun updateOvertime(overtime: OvertimeEntity) = overtimeDao.update(overtime)
    fun getOvertimesByPersonnel(personnelId: Int): Flow<List<OvertimeEntity>> = overtimeDao.getByPersonnel(personnelId)
    fun getOvertimesByServiceId(serviceId: Int): Flow<List<OvertimeEntity>> = overtimeDao.getByServiceId(serviceId)
    fun getAllOvertimes(): Flow<List<OvertimeEntity>> = overtimeDao.getAll()
}