package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.personnel.GetPersonnelDetailSummaryUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.personnel.PersonnelDetailSummary
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonnelDetailViewModel @Inject constructor(
    private val personnelRepository: PersonnelRepository,
    private val workforceRepository: WorkforceRepository,
    private val serviceRepository: ServiceRepository,
    private val getPersonnelDetailSummaryUseCase: GetPersonnelDetailSummaryUseCase
) : ViewModel() {

    private val _personnel = MutableStateFlow<Personnel?>(null)
    val personnel: StateFlow<Personnel?> = _personnel.asStateFlow()

    private val _summary = MutableStateFlow<PersonnelDetailSummary?>(null)
    val summary: StateFlow<PersonnelDetailSummary?> = _summary.asStateFlow()

    private val _shifts = MutableStateFlow<List<ShiftEntity>>(emptyList())
    val shifts: StateFlow<List<ShiftEntity>> = _shifts.asStateFlow()

    private val _leaves = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    val leaves: StateFlow<List<LeaveRequestEntity>> = _leaves.asStateFlow()

    private val _overtimes = MutableStateFlow<List<OvertimeEntity>>(emptyList())
    val overtimes: StateFlow<List<OvertimeEntity>> = _overtimes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPersonnelDetail(personnelId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val p = personnelRepository.getPersonnelById(personnelId)
                _personnel.value = p

                // 1. Vardiya Akışı Dinleniyor
                launch {
                    workforceRepository.getShiftsByPersonnel(personnelId).collect { shiftList ->
                        _shifts.value = shiftList
                        updateSummary(personnelId)
                    }
                }

                // 2. İzin Akışı Dinleniyor (APPROVED ve REJECTED dahil ediliyor, PENDING hariç)
                launch {
                    workforceRepository.getLeaveRequestsByPersonnel(personnelId).collect { leaveList ->
                        val concludedLeaves = leaveList.filter {
                            it.status.equals("APPROVED", ignoreCase = true) ||
                                    it.status.equals("REJECTED", ignoreCase = true)
                        }
                        _leaves.value = concludedLeaves
                        updateSummary(personnelId)
                    }
                }

                // 3. Fazla Mesai Akışı Dinleniyor
                launch {
                    workforceRepository.getOvertimesByPersonnel(personnelId).collect { overtimeList ->
                        _overtimes.value = overtimeList
                        updateSummary(personnelId)
                    }
                }

                _isLoading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateSummary(personnelId: Int) {
        try {
            val services = serviceRepository.getRecordsByPersonnelId(personnelId)
            // Uygunluk hesaplamasına yalnızca APPROVED izinler gönderiliyor (REJECTED hariç tutuluyor)
            val approvedLeavesOnly = _leaves.value.filter { it.status.equals("APPROVED", ignoreCase = true) }

            val summaryResult = getPersonnelDetailSummaryUseCase(
                personnelId = personnelId,
                services = services,
                leaves = approvedLeavesOnly,
                shifts = _shifts.value,
                overtimes = _overtimes.value
            )
            _summary.value = summaryResult
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}