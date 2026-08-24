package com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model

data class ServiceRegistryStats(
    val totalCompanyServices: Int = 0,
    val completedCompanyServices: Int = 0,
    val companyLastServiceDate: String? = null,
    val deviceServiceCount: Int = 0,
    val deviceLastServiceDate: String? = null,
    val deviceLastIssue: String? = null,
    val deviceServicesLastSixMonths: Int = 0
)