package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyName: String,
    val deviceType: String,
    val deviceModel: String, // Yeni eklenen alan
    val serialNumber: String,
    val location: String,
    val priority: String = "Normal",
    val issueDescription: String,
    val status: String = "Bekliyor",
    val date: String
)

// Durum sabitlerini tek bir yerden yönetmek için object yapısı
object ServiceStatus {
    const val BEKLIYOR = "Bekliyor"
    const val YOLDA = "Yolda"
    const val ISLEME_BASLANDI = "İşleme Başlandı"
    const val PARCA_BEKLENIYOR = "Parça Bekleniyor"
    const val TAMAMLANDI = "Tamamlandı"
    const val IPTAL = "İptal"

    val all = listOf(BEKLIYOR, YOLDA, ISLEME_BASLANDI, PARCA_BEKLENIYOR, TAMAMLANDI, IPTAL)
}