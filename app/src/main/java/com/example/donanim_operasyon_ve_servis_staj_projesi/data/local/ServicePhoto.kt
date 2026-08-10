package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_photos",
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
data class ServicePhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceRecordId: Int,
    val personnelId: Int,
    val photoType: String, // Room tarafında TypeConverter ile uğraşmamak için enum'ın .name String değeri tutulacak
    val localUri: String,
    val timestamp: Long
)