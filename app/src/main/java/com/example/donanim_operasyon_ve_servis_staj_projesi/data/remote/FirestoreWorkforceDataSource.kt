package com.example.donanim_operasyon_ve_servis_staj_projesi.data.datasource

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
                doc.toObject(ShiftEntity::class.java)?.copy(
                    firestoreId = doc.id
                )
            } ?: emptyList()
            trySend(shifts)
        }
        awaitClose { listener.remove() }
    }

    fun observePersonnelShifts(personnelId: Int): Flow<List<ShiftEntity>> = callbackFlow {
        val listener = shiftsCollection
            .whereEqualTo("personnelId", personnelId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val shifts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ShiftEntity::class.java)?.copy(
                        firestoreId = doc.id
                    )
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

    fun observePersonnelLeaveRequests(personnelId: Int): Flow<List<LeaveRequestEntity>> = callbackFlow {
        val listener = leavesCollection
            .whereEqualTo("personnelId", personnelId)
            .addSnapshotListener { snapshot, error ->
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

    fun observePersonnelOvertimes(personnelId: Int): Flow<List<OvertimeEntity>> = callbackFlow {
        val listener = overtimesCollection
            .whereEqualTo("personnelId", personnelId)
            .addSnapshotListener { snapshot, error ->
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