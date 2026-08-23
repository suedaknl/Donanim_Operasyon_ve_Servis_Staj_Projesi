package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personnelId: Int,
    val shiftDate: String, // Format: YYYY-MM-DD
    val startTime: String, // Format: HH:mm
    val endTime: String,   // Format: HH:mm
    val status: String = "PLANNED", // PLANNED, ACTIVE, COMPLETED, CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)