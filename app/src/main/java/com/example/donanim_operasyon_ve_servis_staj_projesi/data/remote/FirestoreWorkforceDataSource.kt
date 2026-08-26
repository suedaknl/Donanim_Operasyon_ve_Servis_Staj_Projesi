package com.example.donanim_operasyon_ve_servis_staj_projesi.data.datasource

import android.util.Log
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreWorkforceDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val shiftsCollection = firestore.collection("shifts")
    private val leavesCollection = firestore.collection("leave_requests")
    private val overtimesCollection = firestore.collection("overtimes")

    companion object {
        private const val TAG = "FirestoreWorkforceDS"
    }

    // ==========================================
    // SHIFTS (Vardiya)
    // ==========================================

    fun observeAllShifts(): Flow<List<ShiftEntity>> = callbackFlow {
        val listener = shiftsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val shifts = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val rawPersonnelId = doc.get("personnelId")
                    val parsedPersonnelId = when (rawPersonnelId) {
                        is Number -> rawPersonnelId.toInt()
                        is String -> rawPersonnelId.toIntOrNull() ?: 0
                        else -> 0
                    }

                    if (parsedPersonnelId <= 0) {
                        return@mapNotNull null
                    }

                    ShiftEntity(
                        id = 0,
                        firestoreId = doc.id,
                        personnelId = parsedPersonnelId,
                        shiftDate = doc.getString("shiftDate") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        status = doc.getString("status") ?: "PLANNED",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(shifts)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveShift(shift: ShiftEntity): String {
        val docRef = if (!shift.firestoreId.isNullOrBlank()) {
            shiftsCollection.document(shift.firestoreId!!)
        } else {
            shiftsCollection.document()
        }
        val dataToSave = shift.copy(firestoreId = docRef.id)
        docRef.set(dataToSave).await()
        return docRef.id
    }

    suspend fun updateShift(shift: ShiftEntity) {
        if (!shift.firestoreId.isNullOrBlank()) {
            shiftsCollection.document(shift.firestoreId!!).set(shift).await()
        }
    }

    suspend fun deleteShift(firestoreId: String) {
        if (firestoreId.isNotBlank()) {
            shiftsCollection.document(firestoreId).delete().await()
        }
    }

    // ==========================================
    // LEAVE REQUESTS (İzin Talepleri)
    // ==========================================

    fun observeLeaveRequests(): Flow<List<LeaveRequestEntity>> = callbackFlow {
        val listener = leavesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val leaves = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(LeaveRequestEntity::class.java)?.copy(
                    firestoreId = doc.id
                )
            } ?: emptyList()
            trySend(leaves)
        }
        awaitClose { listener.remove() }
    }

    suspend fun createLeaveRequest(leave: LeaveRequestEntity): String {
        val docRef = if (!leave.firestoreId.isNullOrBlank()) {
            leavesCollection.document(leave.firestoreId!!)
        } else {
            leavesCollection.document()
        }
        val dataToSave = leave.copy(firestoreId = docRef.id)
        docRef.set(dataToSave).await()
        return docRef.id
    }

    suspend fun updateLeaveRequest(leave: LeaveRequestEntity) {
        if (!leave.firestoreId.isNullOrBlank()) {
            leavesCollection.document(leave.firestoreId!!).set(leave).await()
        }
    }

    suspend fun deleteLeaveRequest(firestoreId: String) {
        if (firestoreId.isNotBlank()) {
            leavesCollection.document(firestoreId).delete().await()
        }
    }

    // ==========================================
    // OVERTIMES (Fazla Mesai)
    // ==========================================

    fun observeOvertimes(): Flow<List<OvertimeEntity>> = callbackFlow {
        val listener = overtimesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val overtimes = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(OvertimeEntity::class.java)?.copy(
                    firestoreId = doc.id
                )
            } ?: emptyList()
            trySend(overtimes)
        }
        awaitClose { listener.remove() }
    }

    suspend fun createOvertime(overtime: OvertimeEntity): String {
        val docRef = if (!overtime.firestoreId.isNullOrBlank()) {
            overtimesCollection.document(overtime.firestoreId!!)
        } else {
            overtimesCollection.document()
        }
        val dataToSave = overtime.copy(firestoreId = docRef.id)
        docRef.set(dataToSave).await()
        return docRef.id
    }

    suspend fun updateOvertime(overtime: OvertimeEntity) {
        if (!overtime.firestoreId.isNullOrBlank()) {
            overtimesCollection.document(overtime.firestoreId!!).set(overtime).await()
        }
    }
}