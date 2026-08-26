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
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PersonnelDao
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repository: AiRepository,
    private val serviceRepository: ServiceRepository,
    private val personnelRepository: PersonnelRepository,
    private val workforceRepository: WorkforceRepository,
    private val personnelDao: PersonnelDao
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

        Log.d("AiViewModel", "sendMessage started with text: $text, role: '$role'")

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

                val normalizedRole = role.trim().uppercase()

                if (normalizedRole == "ADMIN") {
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

                    val activeJobs = allServices.filter { s ->
                        val st = s.status.trim()
                        st.isNotEmpty() &&
                                st != ServiceStatus.TAMAMLANDI &&
                                st != ServiceStatus.IPTAL &&
                                !st.equals("Tamamlandı", ignoreCase = true) &&
                                !st.equals("İptal", ignoreCase = true) &&
                                !s.isArchived
                    }.sortedByDescending { it.id }.take(30)

                    val totalOpen = activeJobs.size
                    val pending = activeJobs.count { it.status == ServiceStatus.BEKLIYOR || it.status.equals("Bekliyor", ignoreCase = true) }
                    val inProgress = activeJobs.count {
                        it.status == ServiceStatus.ISLEME_BASLANDI ||
                                it.status == ServiceStatus.PARCA_BEKLENIYOR ||
                                it.status.equals("İşleme Başlandı", ignoreCase = true) ||
                                statusMatchesInProgress(it.status)
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

                } else if (normalizedRole == "PERSONEL" || normalizedRole == "PERSONNEL") {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    val currentUid = firebaseUser?.uid
                    val currentEmail = firebaseUser?.email

                    val personnelList = try {
                        personnelRepository.getAllPersonnelList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val currentPersonnel = personnelList.find { p ->
                        (!currentUid.isNullOrBlank() && p.firebaseUid == currentUid) ||
                                (!currentEmail.isNullOrBlank() && p.email.equals(currentEmail, ignoreCase = true))
                    } ?: personnelList.firstOrNull()

                    val personnelId = currentPersonnel?.id ?: 1
                    val personnelName = currentPersonnel?.fullName ?: "Personel"

                    val allServices = try {
                        serviceRepository.getAllRecords()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val ownActiveJobs = allServices.filter { j ->
                        val st = j.status.trim()
                        j.assignedPersonnelId == personnelId &&
                                st.isNotEmpty() &&
                                st != ServiceStatus.TAMAMLANDI &&
                                st != ServiceStatus.IPTAL &&
                                !st.equals("Tamamlandı", ignoreCase = true) &&
                                !st.equals("İptal", ignoreCase = true) &&
                                !j.isArchived
                    }.sortedByDescending { it.id }.take(15)

                    val currentActiveJob = ownActiveJobs.firstOrNull { j ->
                        j.status.equals("İşleme Başlandı", ignoreCase = true) ||
                                j.status == ServiceStatus.ISLEME_BASLANDI ||
                                j.status.equals("Yolda", ignoreCase = true) ||
                                j.status == ServiceStatus.YOLDA
                    } ?: ownActiveJobs.firstOrNull()

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val todayStr = dateFormat.format(Date())

                    // Lokasyon, seri no ve cihaz modeli kaldırıldı
                    val currentJobDetail = if (currentActiveJob != null) {
                        """
MEVCUT AKTİF GÖREV:
İş Emri ID: #${currentActiveJob.id}
Firma: ${currentActiveJob.companyName}
Cihaz Tipi: ${currentActiveJob.deviceType}
Öncelik: ${currentActiveJob.priority}
Durum: ${currentActiveJob.status}
Arıza/Talep Açıklaması: ${currentActiveJob.issueDescription ?: "Açıklama girilmemiş."}
                        """.trimIndent()
                    } else {
                        "Şu an atanmış aktif bir göreviniz bulunmuyor."
                    }

                    val otherJobsList = ownActiveJobs.filter { j -> j.id != currentActiveJob?.id }.joinToString("\n") { j ->
                        "- #${j.id} | Firma: ${j.companyName} | Cihaz: ${j.deviceType} | Durum: ${j.status} | Öncelik: ${j.priority}"
                    }.ifEmpty { "Başka aktif görev bulunmuyor." }

                    finalContext = """
ROL: PERSONNEL
PERSONEL: $personnelName
BUGÜN: $todayStr

$currentJobDetail

DİĞER AKTİF İŞLERİM:
$otherJobsList

YÖNERGE: Kullanıcı işlerini sorduğunda lokasyon, seri no veya cihaz modeli okumadan sadece yukarıdaki temel bilgileri (Firma, Cihaz Tipi, Durum, Arıza Açıklaması) kısa ve net bir dille özetle. Öneri ve kontrol adımlarını SADECE kullanıcı özellikle sorduğunda ver.
                    """.trimIndent()

                    Log.d("PersonnelAiContext", "matchedPersonnelId=$personnelId name=$personnelName services=${ownActiveJobs.size}")
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
            } catch (ex: Exception) {
                Log.e("AiViewModel", "sendMessage failed: ${ex.message}", ex)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = ex.localizedMessage ?: "AI servisine ulaşılamadı."
                    )
                }
            }
        }
    }

    private fun statusMatchesInProgress(status: String): Boolean {
        return status.equals("Parça Bekleniyor", ignoreCase = true)
    }
}