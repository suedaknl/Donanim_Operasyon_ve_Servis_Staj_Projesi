package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.overtime.ManageOvertimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OvertimeViewModel @Inject constructor(
    private val manageOvertimeUseCase: ManageOvertimeUseCase
) : ViewModel() {

    private val _personnelOvertimes = MutableStateFlow<List<OvertimeEntity>>(emptyList())
    val personnelOvertimes: StateFlow<List<OvertimeEntity>> = _personnelOvertimes.asStateFlow()

    private val _allOvertimes = MutableStateFlow<List<OvertimeEntity>>(emptyList())
    val allOvertimes: StateFlow<List<OvertimeEntity>> = _allOvertimes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadPersonnelOvertimes(personnelId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                manageOvertimeUseCase.getPersonnelOvertimes(personnelId).collect { list ->
                    _personnelOvertimes.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _isLoading.value = false
            }
        }
    }

    fun loadAllOvertimes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                manageOvertimeUseCase.getAllOvertimes().collect { list ->
                    _allOvertimes.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _isLoading.value = false
            }
        }
    }

    fun createOvertime(personnelId: Int, serviceRecordId: Int?, startTime: Long, endTime: Long, description: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = manageOvertimeUseCase.createOvertime(personnelId, serviceRecordId, startTime, endTime, description)
            _isLoading.value = false
            result.onSuccess {
                onComplete(true)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage
                onComplete(false)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}