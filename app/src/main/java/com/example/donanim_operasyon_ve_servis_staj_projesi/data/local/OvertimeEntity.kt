package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overtimes")
data class OvertimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val personnelId: Int = 0,
    val serviceRecordId: Int? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationMinutes: Int = 0,
    val description: String? = null,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)