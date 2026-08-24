package com.example.donanim_operasyon_ve_servis_staj_projesi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val companyName: String,
    val deviceType: String,
    val deviceModel: String,
    val serialNumber: String,
    val location: String,
    val priority: String,
    val issueDescription: String,
    val status: String,
    val date: String,
    val assignedPersonnelId: Int? = null,
    val assignedPersonnelName: String? = null,
    val contactPerson: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
    val plannedDate: String? = null,
    val rejectionReason: String? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val firestoreId: String? = null,
    val assignedPersonnelUid: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val assignmentType: String = "DIRECT" // <--- İş Havuzu için eklendi ("DIRECT" veya "POOL")
)

object ServiceStatus {
    const val BEKLIYOR = "Bekliyor"
    const val YOLDA = "Yolda"
    const val ISLEME_BASLANDI = "İşleme Başlandı"
    const val PARCA_BEKLENIYOR = "Parça Bekleniyor"
    const val TAMAMLANDI = "Tamamlandı"
    const val IPTAL = "İptal"

    val all = listOf(BEKLIYOR, YOLDA, ISLEME_BASLANDI, PARCA_BEKLENIYOR, TAMAMLANDI, IPTAL)
}