package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_notes",
    foreignKeys = [
        ForeignKey(
            entity = ServiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["serviceRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["serviceRecordId"])
    ]
)
data class ServiceNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceRecordId: Int,
    val personnelId: Int,
    val note: String,
    val createdAt: Long,
    val noteType: String = "NORMAL" // "NORMAL" veya "CLOSING" değerlerini alacak
)