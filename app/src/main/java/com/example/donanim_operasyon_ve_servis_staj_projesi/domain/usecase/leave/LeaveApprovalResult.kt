package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave

data class LeaveApprovalResult(
    val success: Boolean,
    val capacityWarning: Boolean,
    val message: String?
)