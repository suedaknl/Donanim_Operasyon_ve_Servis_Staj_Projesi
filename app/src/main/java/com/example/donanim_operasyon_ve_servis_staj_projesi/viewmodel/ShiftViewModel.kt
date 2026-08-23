package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.shift.ManageShiftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val manageShiftUseCase: ManageShiftUseCase
) : ViewModel() {

    private val _personnelShifts = MutableStateFlow<List<ShiftEntity>>(emptyList())
    val personnelShifts: StateFlow<List<ShiftEntity>> = _personnelShifts.asStateFlow()

    private val _todayShift = MutableStateFlow<ShiftEntity?>(null)
    val todayShift: StateFlow<ShiftEntity?> = _todayShift.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadPersonnelShifts(personnelId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                manageShiftUseCase.getPersonnelShifts(personnelId).collect { shifts ->
                    _personnelShifts.value = shifts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Vardiyalar yüklenirken hata oluştu."
                _isLoading.value = false
            }
        }
    }

    fun loadTodayShift(personnelId: Int, date: String) {
        viewModelScope.launch {
            try {
                _todayShift.value = manageShiftUseCase.getTodayShift(personnelId, date)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun createShift(personnelId: Int, shiftDate: String, startTime: String, endTime: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = manageShiftUseCase.createShift(personnelId, shiftDate, startTime, endTime)
            _isLoading.value = false
            result.onSuccess {
                onComplete(true)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage
                onComplete(false)
            }
        }
    }

    fun updateShift(shift: ShiftEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = manageShiftUseCase.updateShift(shift)
            _isLoading.value = false
            result.onSuccess {
                onComplete(true)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage
                onComplete(false)
            }
        }
    }

    fun cancelShift(shift: ShiftEntity) {
        viewModelScope.launch {
            manageShiftUseCase.cancelShift(shift)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}