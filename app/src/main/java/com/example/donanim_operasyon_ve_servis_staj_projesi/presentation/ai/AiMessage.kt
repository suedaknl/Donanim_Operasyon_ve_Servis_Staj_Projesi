package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

enum class AiSender {
    USER,
    ASSISTANT
}

data class AiMessage(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val sender: AiSender,
    val timestamp: Long = System.currentTimeMillis()
)