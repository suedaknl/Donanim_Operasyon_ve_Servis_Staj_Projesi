package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_feedbacks")
data class ServiceFeedback(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceId: Int,
    val firestoreId: String? = null,
    val quality: Int = 0,
    val staff: Int = 0,
    val speed: Int = 0,
    val rating: Int,
    val comment: String?,
    val timestamp: Long = System.currentTimeMillis()
)