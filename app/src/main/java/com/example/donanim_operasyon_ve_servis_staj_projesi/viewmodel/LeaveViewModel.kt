package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave.LeaveConflictInfo
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave.ManageLeaveUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val manageLeaveUseCase: ManageLeaveUseCase,
    private val calculateLeaveConflictUseCase:
    com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.leave.CalculateLeaveConflictUseCase,
    private val workforceRepository: WorkforceRepository
) : ViewModel() {

    private val _pendingRequests =
        MutableStateFlow<List<LeaveRequestEntity>>(emptyList())

    val pendingRequests: StateFlow<List<LeaveRequestEntity>> =
        _pendingRequests.asStateFlow()

    private val _approvedRequests =
        MutableStateFlow<List<LeaveRequestEntity>>(emptyList())

    val approvedRequests: StateFlow<List<LeaveRequestEntity>> =
        _approvedRequests.asStateFlow()

    private val _rejectedRequests =
        MutableStateFlow<List<LeaveRequestEntity>>(emptyList())

    val rejectedRequests: StateFlow<List<LeaveRequestEntity>> =
        _rejectedRequests.asStateFlow()

    private val _personnelRequests =
        MutableStateFlow<List<LeaveRequestEntity>>(emptyList())

    val personnelRequests: StateFlow<List<LeaveRequestEntity>> =
        _personnelRequests.asStateFlow()

    private val _personnelList =
        MutableStateFlow<List<Personnel>>(emptyList())

    val personnelList: StateFlow<List<Personnel>> =
        _personnelList.asStateFlow()

    private val _capacityWarning =
        MutableStateFlow<String?>(null)

    val capacityWarning: StateFlow<String?> =
        _capacityWarning.asStateFlow()

    private val _pendingApprovalRequestId =
        MutableStateFlow<Int?>(null)

    val pendingApprovalRequestId: StateFlow<Int?> =
        _pendingApprovalRequestId.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    private val _successMessage =
        MutableStateFlow<String?>(null)

    val successMessage: StateFlow<String?> =
        _successMessage.asStateFlow()

    /*
     * Uzun ömürlü Flow collector'larını takip ediyoruz.
     * Aynı fonksiyon tekrar çağrılırsa eski collector kapatılacak.
     */
    private var pendingRequestsJob: Job? = null
    private var evaluatedRequestsJob: Job? = null
    private var personnelRequestsJob: Job? = null

    init {
        // İlk yükleme yalnızca ViewModel oluşturulurken yapılır.
        loadData()
    }

    fun loadData() {
        loadPendingRequests()
        loadAllEvaluatedRequestsFromRepository()
        loadPersonnelList()
    }

    fun loadPendingRequests() {

        pendingRequestsJob?.cancel()

        pendingRequestsJob = viewModelScope.launch {

            _isLoading.value = true

            try {

                manageLeaveUseCase
                    .getPendingRequests()
                    .collectLatest { requests ->

                        _pendingRequests.value = requests
                        _isLoading.value = false
                    }

            } catch (e: Exception) {

                _errorMessage.value =
                    e.localizedMessage

                _isLoading.value = false
            }
        }
    }

    fun loadAllEvaluatedRequestsFromRepository() {

        evaluatedRequestsJob?.cancel()

        evaluatedRequestsJob = viewModelScope.launch {

            try {

                workforceRepository
                    .getAllLeaveRequests()
                    .collectLatest { allRequests ->

                        _approvedRequests.value =
                            allRequests.filter {
                                it.status.equals(
                                    "APPROVED",
                                    ignoreCase = true
                                )
                            }

                        _rejectedRequests.value =
                            allRequests.filter {
                                it.status.equals(
                                    "REJECTED",
                                    ignoreCase = true
                                )
                            }
                    }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    fun loadPersonnelList() {

        viewModelScope.launch {

            try {

                _personnelList.value =
                    manageLeaveUseCase.getAllPersonnel()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    fun calculateConflict(
        request: LeaveRequestEntity
    ): LeaveConflictInfo {

        return calculateLeaveConflictUseCase(
            targetRequest = request,
            allApprovedLeaves = _approvedRequests.value,
            allPersonnel = _personnelList.value
        )
    }

    fun loadPersonnelRequests(
        personnelId: Int
    ) {

        personnelRequestsJob?.cancel()

        personnelRequestsJob =
            viewModelScope.launch {

                _isLoading.value = true

                try {

                    manageLeaveUseCase
                        .getPersonnelLeaveRequests(personnelId)
                        .collectLatest { requests ->

                            _personnelRequests.value =
                                requests

                            _isLoading.value = false
                        }

                } catch (e: Exception) {

                    _errorMessage.value =
                        e.localizedMessage

                    _isLoading.value = false
                }
            }
    }

    fun createRequest(
        personnelId: Int,
        startDate: String,
        endDate: String,
        leaveType: String,
        description: String,
        onComplete: (Boolean) -> Unit
    ) {

        viewModelScope.launch {

            _isLoading.value = true

            val result =
                manageLeaveUseCase.createLeaveRequest(
                    personnelId,
                    startDate,
                    endDate,
                    leaveType,
                    description
                )

            _isLoading.value = false

            result
                .onSuccess {

                    _successMessage.value =
                        "İzin talebi başarıyla oluşturuldu."

                    onComplete(true)
                }
                .onFailure { e ->

                    _errorMessage.value =
                        e.localizedMessage

                    onComplete(false)
                }
        }
    }

    fun updatePersonnelRequest(
        leaveRequest: LeaveRequestEntity,
        onComplete: (Boolean) -> Unit
    ) {

        viewModelScope.launch {

            _isLoading.value = true

            val result =
                manageLeaveUseCase
                    .updateLeaveRequest(leaveRequest)

            _isLoading.value = false

            result
                .onSuccess {

                    _successMessage.value =
                        "İzin talebi güncellendi."

                    onComplete(true)
                }
                .onFailure { e ->

                    _errorMessage.value =
                        e.localizedMessage

                    onComplete(false)
                }
        }
    }

    fun deletePersonnelRequest(
        leaveRequest: LeaveRequestEntity,
        onComplete: (Boolean) -> Unit
    ) {

        viewModelScope.launch {

            _isLoading.value = true

            val result =
                manageLeaveUseCase
                    .deleteLeaveRequest(leaveRequest)

            _isLoading.value = false

            result
                .onSuccess {

                    _successMessage.value =
                        "İzin talebi silindi."

                    onComplete(true)
                }
                .onFailure { e ->

                    _errorMessage.value =
                        e.localizedMessage

                    onComplete(false)
                }
        }
    }

    fun approveRequest(
        requestId: Int,
        adminNote: String? = null,
        forceApprove: Boolean = false
    ) {

        viewModelScope.launch {

            _isLoading.value = true

            val result =
                manageLeaveUseCase.approveLeaveRequest(
                    requestId = requestId,
                    adminNote = adminNote,
                    forceApprove = forceApprove,
                    allRequests = _pendingRequests.value
                )

            _isLoading.value = false

            if (result.capacityWarning) {

                _pendingApprovalRequestId.value =
                    requestId

                _capacityWarning.value =
                    result.message

            } else if (result.success) {

                _successMessage.value =
                    result.message

                _capacityWarning.value = null
                _pendingApprovalRequestId.value = null

                /*
                 * loadPendingRequests() ÇAĞIRMIYORUZ.
                 * Mevcut Flow veritabanı değişikliğini
                 * otomatik olarak yayınlayacak.
                 */

            } else {

                _errorMessage.value =
                    result.message
            }
        }
    }

    fun confirmApproveDespiteCapacity(
        adminNote: String? = null
    ) {

        val requestId =
            _pendingApprovalRequestId.value
                ?: return

        approveRequest(
            requestId = requestId,
            adminNote = adminNote,
            forceApprove = true
        )
    }

    fun rejectRequest(
        requestId: Int,
        adminNote: String?
    ) {

        viewModelScope.launch {

            _isLoading.value = true

            val result =
                manageLeaveUseCase.rejectLeaveRequest(
                    requestId,
                    adminNote,
                    _pendingRequests.value
                )

            _isLoading.value = false

            result
                .onSuccess {

                    _successMessage.value =
                        "İzin talebi reddedildi."

                    /*
                     * Burada da tekrar
                     * loadPendingRequests()
                     * çağırmıyoruz.
                     */

                }
                .onFailure { e ->

                    _errorMessage.value =
                        e.localizedMessage
                }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        _capacityWarning.value = null
        _pendingApprovalRequestId.value = null
    }

    override fun onCleared() {

        pendingRequestsJob?.cancel()
        evaluatedRequestsJob?.cancel()
        personnelRequestsJob?.cancel()

        pendingRequestsJob = null
        evaluatedRequestsJob = null
        personnelRequestsJob = null

        super.onCleared()
    }
}