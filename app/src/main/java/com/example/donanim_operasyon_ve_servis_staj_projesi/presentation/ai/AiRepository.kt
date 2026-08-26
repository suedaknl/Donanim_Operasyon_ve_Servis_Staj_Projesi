package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor() {

    suspend fun sendMessage(message: String, role: String, history: List<AiMessage>): String {
        // İleride Firebase Callable Function entegrasyonu buraya gelecek.
        // Şimdilik test amaçlı gecikme ve rol bazlı yanıt simülasyonu:
        delay(1000)

        return if (role.equals("ADMIN", ignoreCase = true)) {
            "Admin AI karar destek bağlantısı hazır. '$message' talebiniz için operasyonel analiz altyapısı devreye alınacaktır."
        } else {
            "Personel AI servis asistanı bağlantısı hazır. '$message' konusundaki teknik servis ve arıza çözüm adımları için buradayım."
        }
    }
}