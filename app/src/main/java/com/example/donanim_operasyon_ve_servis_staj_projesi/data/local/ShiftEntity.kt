package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val personnelId: Int = 0,
    val shiftDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "PLANNED",
    val createdAt: Long = System.currentTimeMillis()
)