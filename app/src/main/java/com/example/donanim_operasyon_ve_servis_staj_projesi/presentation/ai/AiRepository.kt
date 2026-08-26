package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor() {

    private val functions: FirebaseFunctions = Firebase.functions("europe-west1")

    suspend fun sendMessage(
        message: String,
        role: String,
        history: List<AiMessage>,
        context: String = ""
    ): String {
        Log.d("AiRepository", "askAi started. Message length: ${message.length}, role: $role, contextLength: ${context.length}")

        val formattedHistory = history.map { item ->
            mapOf(
                "role" to if (item.sender == AiSender.ASSISTANT) "assistant" else "user",
                "content" to item.text
            )
        }

        val data = hashMapOf(
            "message" to message,
            "role" to role.uppercase(),
            "history" to formattedHistory,
            "context" to context
        )

        try {
            Log.d("AiRepository", "Calling askAiAssistant callable function...")
            val result = functions
                .getHttpsCallable("askAiAssistant")
                .call(data)
                .await()

            Log.d("AiRepository", "Callable success")

            val responseMap = result.getData() as? Map<*, *>
            val answer = responseMap?.get("answer") as? String

            if (answer.isNullOrBlank()) {
                Log.e("AiRepository", "Callable success but answer is blank or null")
                throw Exception("Sunucudan geçerli bir yanıt alınamadı.")
            }

            return answer
        } catch (e: FirebaseFunctionsException) {
            Log.e(
                "AiRepository",
                "Callable failed FirebaseFunctionsException code=${e.code} message=${e.message} details=${e.details}",
                e
            )
            throw Exception(e.message ?: "AI servis hatası oluştu.")
        } catch (e: Exception) {
            Log.e("AiRepository", "Callable failed with unexpected exception: ${e.message}", e)
            throw e
        }
    }
}