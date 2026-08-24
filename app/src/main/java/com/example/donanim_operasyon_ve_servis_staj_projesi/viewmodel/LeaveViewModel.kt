package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave.ManageLeaveUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave.CalculateLeaveConflictUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave.LeaveConflictInfo
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val manageLeaveUseCase: ManageLeaveUseCase,
    private val calculateLeaveConflictUseCase: CalculateLeaveConflictUseCase,
    private val workforceRepository: WorkforceRepository // Doğrudan tüm verileri çekmek için eklendi
) : ViewModel() {

    private val _pendingRequests = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    val pendingRequests: StateFlow<List<LeaveRequestEntity>> = _pendingRequests.asStateFlow()

    private val _approvedRequests = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    val approvedRequests: StateFlow<List<LeaveRequestEntity>> = _approvedRequests.asStateFlow()

    private val _rejectedRequests = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    val rejectedRequests: StateFlow<List<LeaveRequestEntity>> = _rejectedRequests.asStateFlow()

    private val _personnelRequests = MutableStateFlow<List<LeaveRequestEntity>>(emptyList())
    val personnelRequests: StateFlow<List<LeaveRequestEntity>> = _personnelRequests.asStateFlow()

    private val _personnelList = MutableStateFlow<List<Personnel>>(emptyList())
    val personnelList: StateFlow<List<Personnel>> = _personnelList.asStateFlow()

    private val _capacityWarning = MutableStateFlow<String?>(null)
    val capacityWarning: StateFlow<String?> = _capacityWarning.asStateFlow()

    private val _pendingApprovalRequestId = MutableStateFlow<Int?>(null)
    val pendingApprovalRequestId: StateFlow<Int?> = _pendingApprovalRequestId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadPendingRequests()
        loadAllEvaluatedRequestsFromRepository()
        loadPersonnelList()
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                manageLeaveUseCase.getPendingRequests().collect { requests ->
                    _pendingRequests.value = requests
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _isLoading.value = false
            }
        }
    }

    // WorkforceRepository üzerinden TÜM izinleri dinleyip status'e göre anında süzüyoruz
    fun loadAllEvaluatedRequestsFromRepository() {
        viewModelScope.launch {
            try {
                workforceRepository.getAllLeaveRequests().collect { allRequests ->
                    _approvedRequests.value = allRequests.filter { it.status.equals("APPROVED", ignoreCase = true) }
                    _rejectedRequests.value = allRequests.filter { it.status.equals("REJECTED", ignoreCase = true) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadPersonnelList() {
        viewModelScope.launch {
            try {
                _personnelList.value = manageLeaveUseCase.getAllPersonnel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun calculateConflict(request: LeaveRequestEntity): LeaveConflictInfo {
        return calculateLeaveConflictUseCase(
            targetRequest = request,
            allApprovedLeaves = _approvedRequests.value,
            allPersonnel = _personnelList.value
        )
    }

    fun loadPersonnelRequests(personnelId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                manageLeaveUseCase.getPersonnelLeaveRequests(personnelId).collect { requests ->
                    _personnelRequests.value = requests
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _isLoading.value = false
            }
        }
    }

    fun createRequest(personnelId: Int, startDate: String, endDate: String, leaveType: String, description: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = manageLeaveUseCase.createLeaveRequest(personnelId, startDate, endDate, leaveType, description)
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "İzin talebi başarıyla oluşturuldu."
                onComplete(true)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage
                onComplete(false)
            }
        }
    }

    fun approveRequest(requestId: Int, adminNote: String? = null, forceApprove: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = manageLeaveUseCase.approveLeaveRequest(
                requestId = requestId,
                adminNote = adminNote,
                forceApprove = forceApprove,
                allRequests = _pendingRequests.value
            )
            _isLoading.value = false

            if (result.capacityWarning) {
                _pendingApprovalRequestId.value = requestId
                _capacityWarning.value = result.message
            } else if (result.success) {
                _successMessage.value = result.message
                _capacityWarning.value = null
                _pendingApprovalRequestId.value = null
                loadPendingRequests()
            } else {
                _errorMessage.value = result.message
            }
        }
    }

    fun confirmApproveDespiteCapacity(adminNote: String? = null) {
        val reqId = _pendingApprovalRequestId.value ?: return
        approveRequest(requestId = reqId, adminNote = adminNote, forceApprove = true)
    }

    fun rejectRequest(requestId: Int, adminNote: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = manageLeaveUseCase.rejectLeaveRequest(requestId, adminNote, _pendingRequests.value)
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "İzin talebi reddedildi."
                loadPendingRequests()
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        _capacityWarning.value = null
        _pendingApprovalRequestId.value = null
    }
}