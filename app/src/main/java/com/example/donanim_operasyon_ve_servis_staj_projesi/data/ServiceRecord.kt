package com.example.donanim_operasyon_ve_servis_staj_projesi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val companyName: String,       // Firma Adı
    val deviceType: String,        // Cihaz Tipi (POS, Tablet, Yazıcı vb.)
    val serialNumber: String,      // Seri Numarası
    val location: String,          // Lokasyon / Şube
    val priority: String,          // Öncelik (Düşük, Normal, Acil)
    val issueDescription: String,  // Arıza Detayı / Açıklama
    val status: String,            // Durum (Bekliyor, İşlemde, Tamamlandı vb.)
    val date: String               // Kayıt Tarihi
)