package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "leave_requests",
    indices = [
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class LeaveRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val personnelId: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val leaveType: String = "",
    val reason: String? = null,
    val description: String? = null,
    val adminNote: String? = null,
    val reviewedAt: Long? = null,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)