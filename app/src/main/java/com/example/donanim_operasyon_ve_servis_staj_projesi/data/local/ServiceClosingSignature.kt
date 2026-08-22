package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_closing_signatures",
    foreignKeys = [
        ForeignKey(
            entity = ServiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["serviceRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["serviceRecordId"])]
)
data class ServiceClosingSignature(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val serviceRecordId: Int,

    val personnelId: Int,

    // Yeni sistem:
    // Normalize edilmiş X-Y-Pressure stroke verisinin JSON hali.
    val signatureData: String? = null,

    // Eski PNG sistemiyle uyumluluk için şimdilik kalıyor.
    val signatureLocalUri: String = "",

    val createdAt: Long = System.currentTimeMillis()
)