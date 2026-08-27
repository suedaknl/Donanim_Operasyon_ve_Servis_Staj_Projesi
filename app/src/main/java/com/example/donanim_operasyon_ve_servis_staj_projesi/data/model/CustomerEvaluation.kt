package com.example.donanim_operasyon_ve_servis_staj_projesi.data.model

data class CustomerEvaluation(
    val id: String = "",
    val serviceRecordId: String = "",
    val rating: Int = 0,
    val quality: Int = 0,
    val staff: Int = 0,
    val speed: Int = 0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)