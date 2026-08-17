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
import java.util.Locale

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

    // --- ADMIN GELİŞMİŞ FİLTRE STATE'LERİ (MutableStateFlow olarak ViewModel uyumlu hale getirildi) ---
    private val _adminSelectedStatusTab = MutableStateFlow("Tümü")
    val adminSelectedStatusTabState: StateFlow<String> = _adminSelectedStatusTab.asStateFlow()
    var adminSelectedStatusTab by mutableStateOf("Tümü")
        private set

    private val _adminSearchQuery = MutableStateFlow("")
    var adminSearchQuery by mutableStateOf("")
        private set

    var selectedDateFilter by mutableStateOf("Tümü")
        private set

    var customStartDate by mutableStateOf<Long?>(null)
        private set

    var customEndDate by mutableStateOf<Long?>(null)
        private set

    private val _selectedStatusesFilter = MutableStateFlow(setOf<String>())
    var selectedStatusesFilter = mutableStateOf(setOf<String>())
        private set

    private val _selectedPrioritiesFilter = MutableStateFlow(setOf<String>())
    var selectedPrioritiesFilter = mutableStateOf(setOf<String>())
        private set

    private val _selectedDeviceTypesFilter = MutableStateFlow(setOf<String>())
    var selectedDeviceTypesFilter = mutableStateOf(setOf<String>())
        private set

    private val _selectedPersonnelFilter = MutableStateFlow<String?>("Tümü")
    var selectedPersonnelFilter by mutableStateOf<String?>("Tümü")
        private set

    private val _selectedCompanyFilter = MutableStateFlow<String?>("Tümü")
    var selectedCompanyFilter by mutableStateOf<String?>("Tümü")
        private set

    private val _selectedLocationFilter = MutableStateFlow<String?>("Tümü")
    var selectedLocationFilter by mutableStateOf<String?>("Tümü")
        private set

    private val _selectedAssignmentStatusFilter = MutableStateFlow("Tümü")
    var selectedAssignmentStatusFilter by mutableStateOf("Tümü")
        private set

    private val _selectedSortOption = MutableStateFlow("En yeni")
    var selectedSortOption by mutableStateOf("En yeni")
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

    private fun filterPersonnelRecords(
        records: List<ServiceRecord>,
        query: String,
        priority: String,
        tab: String,
        currentUid: String?
    ): List<ServiceRecord> {
        val lowerQuery = query.trim().lowercase()

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
        _adminSelectedStatusTab.value = status
    }

    fun updateAdminSearchQuery(query: String) {
        adminSearchQuery = query
        _adminSearchQuery.value = query
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
        selectedStatusesFilter.value = statuses
        selectedPrioritiesFilter.value = priorities
        selectedDeviceTypesFilter.value = deviceTypes
        selectedPersonnelFilter = personnel
        selectedCompanyFilter = company
        selectedLocationFilter = location
        selectedAssignmentStatusFilter = assignment
        selectedSortOption = sort

        _selectedStatusesFilter.value = statuses
        _selectedPrioritiesFilter.value = priorities
        _selectedDeviceTypesFilter.value = deviceTypes
        _selectedPersonnelFilter.value = personnel
        _selectedCompanyFilter.value = company
        _selectedLocationFilter.value = location
        _selectedAssignmentStatusFilter.value = assignment
        _selectedSortOption.value = sort
    }

    fun clearAllAdvancedFilters() {
        selectedDateFilter = "Tümü"
        customStartDate = null
        customEndDate = null
        selectedStatusesFilter.value = emptySet()
        selectedPrioritiesFilter.value = emptySet()
        selectedDeviceTypesFilter.value = emptySet()
        selectedPersonnelFilter = "Tümü"
        selectedCompanyFilter = "Tümü"
        selectedLocationFilter = "Tümü"
        selectedAssignmentStatusFilter = "Tümü"
        selectedSortOption = "En yeni"
        adminSearchQuery = ""
        adminSelectedStatusTab = "Tümü"

        _selectedStatusesFilter.value = emptySet()
        _selectedPrioritiesFilter.value = emptySet()
        _selectedDeviceTypesFilter.value = emptySet()
        _selectedPersonnelFilter.value = "Tümü"
        _selectedCompanyFilter.value = "Tümü"
        _selectedLocationFilter.value = "Tümü"
        _selectedAssignmentStatusFilter.value = "Tümü"
        _selectedSortOption.value = "En yeni"
        _adminSearchQuery.value = ""
        _adminSelectedStatusTab.value = "Tümü"
    }

    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedDateFilter != "Tümü") count++
            if (selectedStatusesFilter.value.isNotEmpty()) count++
            if (selectedPrioritiesFilter.value.isNotEmpty()) count++
            if (selectedDeviceTypesFilter.value.isNotEmpty()) count++
            if (!selectedPersonnelFilter.isNullOrBlank() && selectedPersonnelFilter != "Tümü") count++
            if (!selectedCompanyFilter.isNullOrBlank() && selectedCompanyFilter != "Tümü") count++
            if (!selectedLocationFilter.isNullOrBlank() && selectedLocationFilter != "Tümü") count++
            if (selectedAssignmentStatusFilter != "Tümü") count++
            if (selectedSortOption != "En yeni") count++
            return count
        }

    private fun filterAdminRecords(
        records: List<ServiceRecord>,
        tab: String,
        query: String,
        statuses: Set<String>,
        priorities: Set<String>,
        deviceTypes: Set<String>,
        personnel: String?,
        company: String?,
        location: String?,
        assignment: String,
        dateFilter: String,
        start: Long?,
        end: Long?,
        sort: String
    ): List<ServiceRecord> {
        val lowerQuery = query.trim().lowercase(Locale.ROOT)

        val filtered = records.filter { record ->
            // 1. Üst Durum Sekmesi Filtresi (Yolda ve İşlemde durumlarını netleştirme)
            val tabMatch = when (tab) {
                "Tümü" -> true
                "Bekleyen" -> record.status == ServiceStatus.BEKLIYOR
                "Yolda" -> record.status == ServiceStatus.YOLDA
                "İşlemde" -> record.status == ServiceStatus.ISLEME_BASLANDI || record.status == ServiceStatus.PARCA_BEKLENIYOR || record.status == ServiceStatus.YOLDA
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                "Reddedilen" -> record.status == ServiceStatus.IPTAL
                else -> true
            }
            val queryMatch = if (lowerQuery.isBlank()) {
                true
            } else {
                record.companyName.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        record.deviceType.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        record.deviceModel.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        record.location.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        (!record.serialNumber.isNullOrBlank() && record.serialNumber.lowercase(Locale.ROOT).contains(lowerQuery))
            }

            val statusMatch = if (statuses.isEmpty()) true else statuses.contains(record.status)
            val priorityMatch = if (priorities.isEmpty()) true else priorities.contains(record.priority)
            val deviceMatch = if (deviceTypes.isEmpty()) true else deviceTypes.contains(record.deviceType)

            // 6. Personel Filtresi (ID veya UID eşleşmesi)
            val personnelMatch = if (personnel.isNullOrBlank() || personnel == "Tümü") {
                true
            } else {
                record.assignedPersonnelId?.toString() == personnel ||
                        personnel.equals(record.assignedPersonnelUid, ignoreCase = true)
            }

            val companyMatch = if (company.isNullOrBlank() || company == "Tümü") {
                true
            } else {
                record.companyName.equals(company, ignoreCase = true)
            }

            val locationMatch = if (location.isNullOrBlank() || location == "Tümü") {
                true
            } else {
                record.location.equals(location, ignoreCase = true)
            }

            val assignmentMatch = when (assignment) {
                "Atanmış" -> record.assignedPersonnelId != null || !record.assignedPersonnelUid.isNullOrBlank()
                "Atanmamış" -> record.assignedPersonnelId == null && record.assignedPersonnelUid.isNullOrBlank()
                else -> true
            }

            val dateMatch = when (dateFilter) {
                "Tümü" -> true
                else -> true
            }

            tabMatch && queryMatch && statusMatch && priorityMatch && deviceMatch && personnelMatch && companyMatch && locationMatch && assignmentMatch && dateMatch
        }

        return filtered.sortedWith(
            when (sort) {
                "En eski" -> compareBy { it.id }
                "Önceliği yüksek olan" -> compareByDescending {
                    when (it.priority) {
                        "Acil", "Çok Yüksek" -> 3
                        "Yüksek" -> 2
                        "Normal", "Orta" -> 1
                        else -> 0
                    }
                }
                else -> compareByDescending { it.id }
            }
        )
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
        _adminSelectedStatusTab,
        _adminSearchQuery,
        _selectedStatusesFilter,
        _selectedPrioritiesFilter,
        _selectedDeviceTypesFilter,
        _selectedPersonnelFilter,
        _selectedCompanyFilter,
        _selectedLocationFilter,
        _selectedAssignmentStatusFilter,
        MutableStateFlow("Tümü"),
        MutableStateFlow<Long?>(null),
        MutableStateFlow<Long?>(null),
        _selectedSortOption
    ) { args ->
        val records = args[0] as List<ServiceRecord>
        val tab = args[1] as String
        val query = args[2] as String
        val statuses = args[3] as Set<String>
        val priorities = args[4] as Set<String>
        val deviceTypes = args[5] as Set<String>
        val personnel = args[6] as String?
        val company = args[7] as String?
        val location = args[8] as String?
        val assignment = args[9] as String
        val dateFilter = args[10] as String
        val start = args[11] as Long?
        val end = args[12] as Long?
        val sort = args[13] as String

        filterAdminRecords(
            records = records,
            tab = tab,
            query = query,
            statuses = statuses,
            priorities = priorities,
            deviceTypes = deviceTypes,
            personnel = personnel,
            company = company,
            location = location,
            assignment = assignment,
            dateFilter = dateFilter,
            start = start,
            end = end,
            sort = sort
        )
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