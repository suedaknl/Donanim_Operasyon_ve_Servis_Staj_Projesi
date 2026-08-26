package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String, role: String) {
        if (text.isBlank()) return

        val userMessage = AiMessage(text = text, sender = AiSender.USER)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val currentHistory = _uiState.value.messages
                val responseText = aiRepository.sendMessage(text, role, currentHistory)

                val assistantMessage = AiMessage(text = responseText, sender = AiSender.ASSISTANT)

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Bir hata oluştu."
                    )
                }
            }
        }
    }
}