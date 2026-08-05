package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personnel_table")
data class Personnel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fullName: String,
    val username: String,
    val password: String, // Gerçek projelerde şifre hash'lenerek tutulmalıdır
    val phoneNumber: String,
    val role: String,
    val isActive: Boolean
)