package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overtimes")
data class OvertimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personnelId: Int,
    val serviceRecordId: Int? = null,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val description: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis()
)