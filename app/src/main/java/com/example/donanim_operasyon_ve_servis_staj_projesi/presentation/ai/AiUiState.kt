package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)