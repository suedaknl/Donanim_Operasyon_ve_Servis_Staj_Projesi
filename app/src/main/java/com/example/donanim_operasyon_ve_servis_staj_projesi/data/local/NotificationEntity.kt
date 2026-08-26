package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String = "",
    val recipientUid: String = "",
    val role: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val targetId: String? = null,
    val createdAt: Long = 0L,
    val isRead: Boolean = false
) {
    // Firestore'un nesneyi deserialize edebilmesi için boş constructor zorunludur
    constructor() : this("", "", "", "", "", "", null, 0L, false)
}