package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repository: AiRepository,
    private val serviceRepository: ServiceRepository,
    private val personnelRepository: PersonnelRepository,
    private val workforceRepository: WorkforceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun clearConversation() {
        Log.d("AiViewModel", "clearConversation invoked. Resetting messages.")
        _uiState.update { state ->
            state.copy(
                messages = emptyList(),
                errorMessage = null,
                isLoading = false
            )
        }
    }

    fun sendMessage(text: String, role: String, contextOverride: String? = null) {
        if (text.isBlank()) return

        Log.d("AiViewModel", "sendMessage started with text: $text, role: $role")

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
                val history = _uiState.value.messages.dropLast(1)
                var finalContext = ""

                if (role.equals("ADMIN", ignoreCase = true)) {
                    val allServices = try {
                        serviceRepository.getAllRecords()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val personnelList = try {
                        personnelRepository.getAllPersonnelList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val allLeaveRequests = try {
                        workforceRepository.getAllLeaveRequests().first()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val activeJobs = allServices.filter {
                        val st = it.status.trim()
                        st.isNotEmpty() &&
                                st != ServiceStatus.TAMAMLANDI &&
                                st != ServiceStatus.IPTAL &&
                                !st.equals("Tamamlandı", ignoreCase = true) &&
                                !st.equals("İptal", ignoreCase = true) &&
                                !it.isArchived
                    }.sortedByDescending { it.id }.take(30)

                    val totalOpen = activeJobs.size
                    val pending = activeJobs.count { it.status == ServiceStatus.BEKLIYOR || it.status.equals("Bekliyor", ignoreCase = true) }
                    val inProgress = activeJobs.count {
                        it.status == ServiceStatus.ISLEME_BASLANDI ||
                                it.status == ServiceStatus.PARCA_BEKLENIYOR ||
                                it.status.equals("İşleme Başlandı", ignoreCase = true) ||
                                it.status.equals("Parça Bekleniyor", ignoreCase = true)
                    }
                    val onTheWay = activeJobs.count { it.status == ServiceStatus.YOLDA || it.status.equals("Yolda", ignoreCase = true) }

                    val workloads = personnelList.joinToString("\n") { p ->
                        val count = activeJobs.count { it.assignedPersonnelId == p.id }
                        "- ${p.fullName}: $count aktif iş"
                    }

                    val jobDetails = activeJobs.joinToString("\n") { j ->
                        val personnelName = personnelList.find { it.id == j.assignedPersonnelId }?.fullName ?: "Atanmamış"
                        "- #${j.id} | Firma: ${j.companyName} | Öncelik: ${j.priority} | Durum: ${j.status} | Cihaz: ${j.deviceType} | Atanan: $personnelName"
                    }

                    val leaveDetails = allLeaveRequests.joinToString("\n") { l ->
                        val pName = personnelList.find { it.id == l.personnelId }?.fullName ?: "Personel #${l.personnelId}"
                        val statusTr = when(l.status.uppercase()) {
                            "APPROVED" -> "Onaylandı"
                            "REJECTED" -> "Reddedildi"
                            "PENDING" -> "Bekliyor"
                            else -> l.status
                        }
                        "- $pName | Tür: ${l.leaveType} | Başlangıç: ${l.startDate} | Bitiş: ${l.endDate} | Durum: $statusTr"
                    }.ifEmpty { "Kayıtlı izin talebi bulunmuyor." }

                    finalContext = """
OPERASYON ÖZETİ:
Toplam aktif iş: $totalOpen
Bekleyen: $pending
İşlemde: $inProgress
Yolda: $onTheWay

PERSONEL İŞ YÜKÜ:
$workloads

AKTİF İŞLER:
$jobDetails

İZİN TALEPLERİ VE DURUMLARI:
$leaveDetails
                    """.trimIndent()
                }

                val responseText = repository.sendMessage(text, role, history, finalContext)

                Log.d("AiViewModel", "sendMessage succeeded response received")

                val assistantMessage = AiMessage(text = responseText, sender = AiSender.ASSISTANT)

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("AiViewModel", "sendMessage failed: ${e.message}", e)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "AI servisine ulaşılamadı."
                    )
                }
            }
        }
    }
}