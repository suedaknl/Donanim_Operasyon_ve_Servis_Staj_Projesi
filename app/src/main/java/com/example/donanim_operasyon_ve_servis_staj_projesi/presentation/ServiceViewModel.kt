package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServiceViewModelFactory(private val repository: ServiceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
}

class ServiceViewModel(private val repository: ServiceRepository) : ViewModel() {

    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords.asStateFlow()

    private val _personnelServiceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val personnelServiceRecords: StateFlow<List<ServiceRecord>> =
        _personnelServiceRecords.asStateFlow()

    private val _selectedRecord = MutableStateFlow<ServiceRecord?>(null)
    val selectedRecord: StateFlow<ServiceRecord?> = _selectedRecord.asStateFlow()

    private val _serviceNotes = MutableStateFlow<List<ServiceNote>>(emptyList())
    val serviceNotes: StateFlow<List<ServiceNote>> = _serviceNotes.asStateFlow()

    private val _servicePhotos = MutableStateFlow<List<ServicePhoto>>(emptyList())
    val servicePhotos: StateFlow<List<ServicePhoto>> = _servicePhotos.asStateFlow()

    private var notesJob: Job? = null
    private var photosJob: Job? = null

    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedFilterFlow = MutableStateFlow("Hepsi")
    private val _selectedPriorityFilterFlow = MutableStateFlow("Hepsi")
    private val _selectedTabFlow = MutableStateFlow("Tümü")

    private val _currentPersonnelUidFlow = MutableStateFlow<String?>(null)

    fun setCurrentPersonnelUid(uid: String?) {
        _currentPersonnelUidFlow.value = uid
    }

    private val _serviceClosingSignature =
        MutableStateFlow<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature?>(
            null
        )
    val serviceClosingSignature = _serviceClosingSignature.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    var searchQuery by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf("Hepsi")
        private set

    var selectedPriorityFilter by mutableStateOf("Hepsi")
        private set

    var selectedTab by mutableStateOf("Tümü")
        private set

    // 1. Gelişmiş Filtre State'leri
    var adminSelectedStatusTab by mutableStateOf("Tümü") // "Tümü", "Bekleyen", "Yolda", "İşlemde", "Tamamlanan", "Reddedilen"
        private set

    var adminSearchQuery by mutableStateOf("")
        private set

    var selectedDateFilter by mutableStateOf("Tümü") // "Tümü", "Bugün", "Son 7 gün", "Son 30 gün", "Bu ay", "Özel Aralık"
        private set

    var customStartDate by mutableStateOf<Long?>(null)
        private set

    var customEndDate by mutableStateOf<Long?>(null)
        private set

    var selectedStatusesFilter by mutableStateOf(setOf<String>()) // Çoklu durum seçimi
        private set

    var selectedPrioritiesFilter by mutableStateOf(setOf<String>()) // Öncelik seçimi
        private set

    var selectedDeviceTypesFilter by mutableStateOf(setOf<String>()) // Cihaz türü seçimi
        private set

    var selectedPersonnelFilter by mutableStateOf<String?>("Tümü") // Personel UID/İsim
        private set

    var selectedCompanyFilter by mutableStateOf<String?>("Tümü") // Firma seçimi
        private set

    var selectedLocationFilter by mutableStateOf<String?>("Tümü") // Lokasyon seçimi
        private set

    var selectedAssignmentStatusFilter by mutableStateOf("Tümü") // "Tümü", "Atanmış", "Atanmamış"
        private set

    var selectedSortOption by mutableStateOf("En yeni") // "En yeni", "En eski", "Önceliği yüksek olan"
        private set

    private val _closingNote = MutableStateFlow("")
    val closingNote = _closingNote.asStateFlow()

    private val _closingSignatureUri = MutableStateFlow<String?>(null)
    val closingSignatureUri = _closingSignatureUri.asStateFlow()

    sealed class ClosingState {
        object Idle : ClosingState()
        object Loading : ClosingState()
        object Success : ClosingState()
        data class Error(val message: String) : ClosingState()
    }

    private val _closingState = MutableStateFlow<ClosingState>(ClosingState.Idle)
    val closingState = _closingState.asStateFlow()

    private val _closingAfterPhotoUri = MutableStateFlow<String?>(null)
    val closingAfterPhotoUri = _closingAfterPhotoUri.asStateFlow()

    fun updateClosingAfterPhotoUri(uri: String?) {
        _closingAfterPhotoUri.value = uri
    }

    // Personel iş emirleri için kesin ve hatasız filtreleme mantığı
    private fun filterPersonnelRecords(
        records: List<ServiceRecord>,
        query: String,
        priority: String,
        tab: String,
        currentUid: String?
    ): List<ServiceRecord> {
        val lowerQuery = query.trim().lowercase()

        // 1. Ana Koşul: Oturum açmış personele atanmış olmalı ve IPTAL olmamalı
        val personnelBaseRecords = records.filter { record ->
            val matchesPersonnel = if (!currentUid.isNullOrEmpty()) {
                record.assignedPersonnelUid == currentUid
            } else {
                true
            }
            matchesPersonnel && record.status != ServiceStatus.IPTAL
        }

        return personnelBaseRecords.filter { record ->
            val matchesSearch = if (lowerQuery.isEmpty()) true else {
                record.companyName.lowercase().contains(lowerQuery) ||
                        record.deviceType.lowercase().contains(lowerQuery) ||
                        record.serialNumber.lowercase().contains(lowerQuery) ||
                        record.location.lowercase().contains(lowerQuery)
            }
            val matchesPriority = if (priority == "Hepsi") true else record.priority == priority

            // 2. Sekme (Tab) / Statü Bazlı Kesin Eşleşme
            val matchesTab = when (tab) {
                "Tümü" -> true
                "Atanmış", "Atanan", "Bekleyen" -> record.status == ServiceStatus.BEKLIYOR
                "Yolda" -> record.status == ServiceStatus.YOLDA
                "Devam Eden", "İşlemde" -> record.status == ServiceStatus.ISLEME_BASLANDI || record.status == ServiceStatus.PARCA_BEKLENIYOR
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                else -> true
            }

            matchesSearch && matchesPriority && matchesTab
        }
    }
    fun updateAdminSelectedStatusTab(status: String) {
        adminSelectedStatusTab = status
    }

    fun updateAdminSearchQuery(query: String) {
        adminSearchQuery = query
    }

    fun updateAdvancedFilters(
        dateFilter: String,
        start: Long?,
        end: Long?,
        statuses: Set<String>,
        priorities: Set<String>,
        deviceTypes: Set<String>,
        personnel: String?,
        company: String?,
        location: String?,
        assignment: String,
        sort: String
    ) {
        selectedDateFilter = dateFilter
        customStartDate = start
        customEndDate = end
        selectedStatusesFilter = statuses
        selectedPrioritiesFilter = priorities
        selectedDeviceTypesFilter = deviceTypes
        selectedPersonnelFilter = personnel
        selectedCompanyFilter = company
        selectedLocationFilter = location
        selectedAssignmentStatusFilter = assignment
        selectedSortOption = sort
    }

    fun clearAllAdvancedFilters() {
        selectedDateFilter = "Tümü"
        customStartDate = null
        customEndDate = null
        selectedStatusesFilter = emptySet()
        selectedPrioritiesFilter = emptySet()
        selectedDeviceTypesFilter = emptySet()
        selectedPersonnelFilter = "Tümü"
        selectedCompanyFilter = "Tümü"
        selectedLocationFilter = "Tümü"
        selectedAssignmentStatusFilter = "Tümü"
        selectedSortOption = "En yeni"
        adminSearchQuery = ""
        adminSelectedStatusTab = "Tümü"
    }

    // Aktif filtre sayısını hesaplayan yardımcı fonksiyon
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedDateFilter != "Tümü") count++
            if (selectedStatusesFilter.isNotEmpty()) count++
            if (selectedPrioritiesFilter.isNotEmpty()) count++
            if (selectedDeviceTypesFilter.isNotEmpty()) count++
            if (selectedPersonnelFilter != null && selectedPersonnelFilter != "Tümü") count++
            if (selectedCompanyFilter != null && selectedCompanyFilter != "Tümü") count++
            if (selectedLocationFilter != null && selectedLocationFilter != "Tümü") count++
            if (selectedAssignmentStatusFilter != "Tümü") count++
            if (selectedSortOption != "En yeni") count++
            return count
        }

    private fun filterRecords(
        records: List<ServiceRecord>,
        query: String,
        status: String,
        priority: String,
        tab: String
    ): List<ServiceRecord> {
        val lowerQuery = query.trim().lowercase()
        return records.filter { record ->
            val matchesSearch = if (lowerQuery.isEmpty()) true else {
                record.companyName.lowercase().contains(lowerQuery) ||
                        record.deviceType.lowercase().contains(lowerQuery) ||
                        record.serialNumber.lowercase().contains(lowerQuery) ||
                        record.location.lowercase().contains(lowerQuery)
            }
            val matchesPriority = if (priority == "Hepsi") true else record.priority == priority
            val matchesTab = when (tab) {
                "Atanan", "Atanmış" -> record.status == ServiceStatus.BEKLIYOR
                "Yolda" -> record.status == ServiceStatus.YOLDA
                "Devam Eden", "İşlemde" -> record.status == ServiceStatus.ISLEME_BASLANDI || record.status == ServiceStatus.PARCA_BEKLENIYOR
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                else -> true
            }
            val matchesStatus = if (status == "Hepsi") true else when (status) {
                "Atanan", "Atanmış", "Bekliyor" -> record.status == ServiceStatus.BEKLIYOR
                "Yolda" -> record.status == ServiceStatus.YOLDA
                "Devam Eden", "İşlemde", "İşleme Başlandı" -> record.status == ServiceStatus.ISLEME_BASLANDI || record.status == ServiceStatus.PARCA_BEKLENIYOR
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                else -> true
            }
            matchesSearch && matchesPriority && matchesTab && matchesStatus
        }
    }

    val filteredServiceRecords = combine(
        _serviceRecords,
        _searchQueryFlow,
        _selectedFilterFlow,
        _selectedPriorityFilterFlow,
        _selectedTabFlow
    ) { records, query, status, priority, tab ->
        filterRecords(records, query, status, priority, tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPersonnelServiceRecords = combine(
        _personnelServiceRecords,
        _searchQueryFlow,
        _selectedPriorityFilterFlow,
        _selectedTabFlow,
        _currentPersonnelUidFlow
    ) { records, query, priority, tab, currentUid ->
        filterPersonnelRecords(records, query, priority, tab, currentUid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                repository.syncAllServices()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getAllRecords()
            _serviceRecords.value = records
        }
    }

    fun loadRecordsForPersonnel(personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getRecordsByPersonnelId(personnelId)
            _personnelServiceRecords.value = records
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        _searchQueryFlow.value = query
    }

    fun updateSelectedFilter(filter: String) {
        selectedFilter = filter
        _selectedFilterFlow.value = filter
    }

    fun updateSelectedPriorityFilter(filter: String) {
        selectedPriorityFilter = filter
        _selectedPriorityFilterFlow.value = filter
    }

    fun updateSelectedTab(tab: String) {
        selectedTab = tab
        _selectedTabFlow.value = tab
    }

    fun selectRecord(record: ServiceRecord) {
        _selectedRecord.value = record
    }

    fun clearSelection() {
        _selectedRecord.value = null
    }

    fun insertRecord(record: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecord(record)
            loadRecords()
        }
    }

    fun deleteRecord(record: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecord(record)
            loadRecords()
        }
    }

    fun updateRecord(record: ServiceRecord) {
        viewModelScope.launch {
            val currentRecord = _serviceRecords.value.find { it.id == record.id }

            if (currentRecord != null) {
                if (currentRecord.status == ServiceStatus.TAMAMLANDI) {
                    return@launch
                }

                if (currentRecord.assignedPersonnelId != record.assignedPersonnelId) {
                    val canAssignOrChange = currentRecord.status == ServiceStatus.BEKLIYOR || currentRecord.status == ServiceStatus.IPTAL
                    if (!canAssignOrChange) {
                        return@launch
                    }
                }
            }

            repository.updateService(record)
            loadRecords()
        }
    }

    fun updateStatus(recordId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecord = _serviceRecords.value.find { it.id == recordId } ?: return@launch

            if (currentRecord.status == ServiceStatus.TAMAMLANDI) {
                _errorMessage.value = "Tamamlanmış bir iş emrinin durumu değiştirilemez."
                return@launch
            }

            repository.updateStatus(recordId, newStatus)
            loadRecords()
            _selectedRecord.value = _selectedRecord.value?.copy(status = newStatus)
        }
    }

    fun getServiceById(id: Int): ServiceRecord? {
        return serviceRecords.value.find { it.id == id }
    }

    fun clearAssignedPersonnel(personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAssignedPersonnel(personnelId)
            loadRecords()
        }
    }

    fun loadServiceNotes(serviceRecordId: Int) {
        notesJob?.cancel()
        notesJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getNotesForService(serviceRecordId).collect { notes ->
                    _serviceNotes.value = notes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addServiceNote(note: ServiceNote) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertServiceNote(note)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadServicePhotos(serviceRecordId: Int) {
        photosJob?.cancel()
        photosJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getPhotosForService(serviceRecordId).collect { photos ->
                    _servicePhotos.value = photos
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addServicePhoto(photo: ServicePhoto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertServicePhoto(photo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteServicePhoto(photo: ServicePhoto) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteServicePhoto(photo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateClosingNote(note: String) {
        _closingNote.value = note
    }

    fun updateClosingSignatureUri(uri: String?) {
        _closingSignatureUri.value = uri
    }

    fun resetClosingState() {
        _closingState.value = ClosingState.Idle
        _closingNote.value = ""
        _closingSignatureUri.value = null
        _closingAfterPhotoUri.value = null
    }

    fun loadClosingSignature(serviceId: Int) {
        viewModelScope.launch {
            try {
                val signature = repository.getClosingSignatureByServiceId(serviceId)
                _serviceClosingSignature.value = signature
            } catch (e: Exception) {
                _serviceClosingSignature.value = null
            }
        }
    }

    fun syncAdminData() {
        viewModelScope.launch {
            repository.syncAllServices()
            val updatedList = repository.getAllRecords()
            _serviceRecords.value = updatedList
        }
    }

    fun syncMyServices(firebaseUid: String, localPersonnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncServicesFromFirestore(
                    personnelUid = firebaseUid,
                    localPersonnelId = localPersonnelId
                )
                val updatedRecords = repository.getRecordsByPersonnelId(localPersonnelId)
                _personnelServiceRecords.value = updatedRecords
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptService(recordId: Int, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(recordId, ServiceStatus.YOLDA)
            loadRecords()
            loadRecordsForPersonnel(personnelId)
            _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.YOLDA)
        }
    }

    fun startServiceWork(recordId: Int, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecord = _serviceRecords.value.find { it.id == recordId } ?: return@launch
            if (currentRecord.status == ServiceStatus.YOLDA) {
                repository.updateStatus(recordId, ServiceStatus.ISLEME_BASLANDI)
                loadRecords()
                loadRecordsForPersonnel(personnelId)
                _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.ISLEME_BASLANDI)
            }
        }
    }

    fun setParcaBekleniyor(recordId: Int, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(recordId, ServiceStatus.PARCA_BEKLENIYOR)
            loadRecords()
            loadRecordsForPersonnel(personnelId)
            _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.PARCA_BEKLENIYOR)
        }
    }

    fun rejectService(recordId: Int, rejectionReason: String, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.rejectService(recordId, rejectionReason)
            if (result.isSuccess) {
                loadRecords()
                loadRecordsForPersonnel(personnelId)
                _selectedRecord.value = _selectedRecord.value?.copy(
                    status = ServiceStatus.IPTAL,
                    rejectionReason = rejectionReason
                )
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "İş reddedilirken bir hata oluştu."
            }
        }
    }

    fun submitClosingForm(serviceId: Int, personnelId: Int) {
        viewModelScope.launch {
            _closingState.value = ClosingState.Loading

            val currentRecord = getServiceById(serviceId)
            val note = _closingNote.value
            val signature = _closingSignatureUri.value

            if (currentRecord == null || signature == null || note.isBlank()) {
                _closingState.value = ClosingState.Error("Eksik veri: Lütfen formu kontrol edin.")
                return@launch
            }

            val completedRecord = currentRecord.copy(status = ServiceStatus.TAMAMLANDI)

            val result = repository.completeServiceWork(
                serviceRecord = completedRecord,
                personnelId = personnelId,
                closingNoteText = note,
                signatureUri = signature
            )

            if (result.isSuccess) {
                _closingState.value = ClosingState.Success
                loadRecords()
                loadRecordsForPersonnel(personnelId)
                _selectedRecord.value = completedRecord
            } else {
                _closingState.value = ClosingState.Error(result.exceptionOrNull()?.message ?: "İşlem sırasında bir hata oluştu.")
            }
        }
    }
}