package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index // HATA VEREN KISMIN ÇÖZÜMÜ BU SATIR

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
    val signatureLocalUri: String,
    val createdAt: Long = System.currentTimeMillis()
)