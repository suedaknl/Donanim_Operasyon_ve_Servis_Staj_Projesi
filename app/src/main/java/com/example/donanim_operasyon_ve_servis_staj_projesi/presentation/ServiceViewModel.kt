package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.*
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.CompleteServiceUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.UpdateServiceStatusUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.StartServiceWorkUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.UpdateServiceUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.DeleteServiceUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.ArchiveServiceUseCase

@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val repository: ServiceRepository,
    private val completeServiceUseCase: CompleteServiceUseCase,
    private val updateServiceStatusUseCase: UpdateServiceStatusUseCase,
    private val startServiceWorkUseCase: StartServiceWorkUseCase,
    private val updateServiceUseCase: UpdateServiceUseCase,
    private val deleteServiceUseCase: DeleteServiceUseCase,
    private val archiveServiceUseCase: ArchiveServiceUseCase
) : ViewModel() {

    // --- TEMEL STATE'LER ---
    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords.asStateFlow()

    private val _personnelServiceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val personnelServiceRecords: StateFlow<List<ServiceRecord>> = _personnelServiceRecords.asStateFlow()

    private val _selectedRecord = MutableStateFlow<ServiceRecord?>(null)
    val selectedRecord: StateFlow<ServiceRecord?> = _selectedRecord.asStateFlow()

    private val _serviceNotes = MutableStateFlow<List<ServiceNote>>(emptyList())
    val serviceNotes: StateFlow<List<ServiceNote>> = _serviceNotes.asStateFlow()

    private val _servicePhotos = MutableStateFlow<List<ServicePhoto>>(emptyList())
    val servicePhotos: StateFlow<List<ServicePhoto>> = _servicePhotos.asStateFlow()

    private val _serviceClosingSignature = MutableStateFlow<ServiceClosingSignature?>(null)
    val serviceClosingSignature = _serviceClosingSignature.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // --- PERSONEL AKIŞ STATE'LERİ ---
    var searchQuery by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf("Hepsi")
        private set

    var selectedPriorityFilter by mutableStateOf("Hepsi")
        private set

    var selectedTab by mutableStateOf("Tümü")
        private set

    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedPriorityFilterFlow = MutableStateFlow("Hepsi")
    private val _selectedTabFlow = MutableStateFlow("Tümü")
    private val _currentPersonnelUidFlow = MutableStateFlow<String?>(null)

    fun setCurrentPersonnelUid(uid: String?) {
        _currentPersonnelUidFlow.value = uid
    }

    // --- ADMIN AKIŞ VE FİLTRE STATE'LERİ ---
    private val _adminSelectedStatusTab = MutableStateFlow("Tümü")
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

    // UI'nin doğrudan okuduğu Compose state'leri
    var selectedPersonnelFilter by mutableStateOf<String?>("Tümü")
        private set
    var selectedCompanyFilter by mutableStateOf<String?>("Tümü")
        private set
    var selectedLocationFilter by mutableStateOf<String?>("Tümü")
        private set
    var selectedAssignmentStatusFilter by mutableStateOf("Tümü")
        private set
    var selectedSortOption by mutableStateOf("En yeni")
        private set

    // Filtreleme akışının reaktif olarak dinlediği Flow karşılıkları
    private val _selectedPersonnelFilterFlow = MutableStateFlow<String?>("Tümü")
    private val _selectedCompanyFilterFlow = MutableStateFlow<String?>("Tümü")
    private val _selectedLocationFilterFlow = MutableStateFlow<String?>("Tümü")
    private val _selectedAssignmentStatusFilterFlow = MutableStateFlow("Tümü")
    private val _selectedSortOptionFlow = MutableStateFlow("En yeni")

    // --- PAGINATION STATE'LERİ ---
    private val _adminCurrentPage = MutableStateFlow(1)
    val adminCurrentPage: StateFlow<Int> = _adminCurrentPage.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    val pageSize = 4

    // --- KAPANIŞ VE MEDYA STATE'LERİ ---
    private val _closingNote = MutableStateFlow("")
    val closingNote = _closingNote.asStateFlow()

    private val _closingSignatureUri = MutableStateFlow<String?>(null)
    val closingSignatureUri = _closingSignatureUri.asStateFlow()

    private val _closingAfterPhotoUri = MutableStateFlow<String?>(null)
    val closingAfterPhotoUri = _closingAfterPhotoUri.asStateFlow()

    private val _remotePhotos = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val remotePhotos: StateFlow<List<Map<String, Any>>> = _remotePhotos.asStateFlow()

    private val _remoteSignatures = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val remoteSignatures: StateFlow<List<Map<String, Any>>> = _remoteSignatures.asStateFlow()

    private val _remoteNotes = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val remoteNotes: StateFlow<List<Map<String, Any>>> = _remoteNotes.asStateFlow()

    sealed class ClosingState {
        object Idle : ClosingState()
        object Loading : ClosingState()
        object Success : ClosingState()
        data class Error(val message: String) : ClosingState()
    }

    private val _closingState = MutableStateFlow<ClosingState>(ClosingState.Idle)
    val closingState = _closingState.asStateFlow()

    private val _serviceHistory = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val serviceHistory: StateFlow<List<Map<String, Any>>> = _serviceHistory.asStateFlow()

    private var notesJob: Job? = null
    private var photosJob: Job? = null

    companion object {
        const val SERVICE_START_RADIUS_METERS = 250f
    }

    // --- 1. FILTERED SERVICE RECORDS (TÜM STATE'LERDEN SONRA) ---
    val filteredServiceRecords: StateFlow<List<ServiceRecord>> = combine(
        _serviceRecords,
        _adminSelectedStatusTab,
        _adminSearchQuery,
        _selectedStatusesFilter,
        _selectedPrioritiesFilter
    ) { records, tab, query, statuses, priorities ->
        Triple(records, tab, Triple(query, statuses, priorities))
    }.combine(
        combine(
            _selectedDeviceTypesFilter,
            _selectedPersonnelFilterFlow,
            _selectedCompanyFilterFlow,
            _selectedLocationFilterFlow,
            _selectedAssignmentStatusFilterFlow
        ) { types, personnel, company, location, assignment ->
            Triple(types, personnel, Triple(company, location, assignment))
        }.combine(_selectedSortOptionFlow) { extraFilters, sort ->
            extraFilters to sort
        }
    ) { firstTriple, secondWithSort ->
        val records = firstTriple.first
        val tab = firstTriple.second
        val query = firstTriple.third.first
        val statuses = firstTriple.third.second
        val priorities = firstTriple.third.third

        val secondTriple = secondWithSort.first
        val sort = secondWithSort.second

        val types = secondTriple.first
        val personnel = secondTriple.second
        val company = secondTriple.third.first
        val location = secondTriple.third.second
        val assignment = secondTriple.third.third

        filterAdminRecords(
            records = records,
            tab = tab,
            query = query,
            statuses = statuses,
            priorities = priorities,
            deviceTypes = types,
            personnel = personnel,
            company = company,
            location = location,
            assignment = assignment,
            dateFilter = selectedDateFilter,
            sort = sort
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // --- 2. ADMIN DİNAMİK PAGINATION FLOWS (1. Sayfa 3, Sonrakiler 5 Kayıt) ---
    val admintotalPages: StateFlow<Int> = filteredServiceRecords.map { list ->
        if (list.isEmpty()) 1
        else if (list.size <= 3) 1
        else 1 + (list.size - 3 + 4) / 5
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val adminPagedServiceRecords: StateFlow<List<ServiceRecord>> = combine(
        filteredServiceRecords,
        _adminCurrentPage
    ) { list, page ->
        if (list.isEmpty()) return@combine emptyList()
        val maxPage = if (list.size <= 3) 1 else 1 + (list.size - 3 + 4) / 5
        val safePage = page.coerceIn(1, maxPage)

        val startIndex: Int
        val endIndex: Int

        if (safePage == 1) {
            startIndex = 0
            endIndex = minOf(3, list.size)
        } else {
            startIndex = 3 + (safePage - 2) * 5
            endIndex = minOf(startIndex + 5, list.size)
        }

        if (startIndex < list.size && startIndex < endIndex) {
            list.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAdminPage(page: Int) {
        val max = admintotalPages.value
        if (page in 1..max) {
            _adminCurrentPage.value = page
        }
    }

    // --- 3. PERSONEL FILTERED & PAGINATION FLOWS ---
    val filteredPersonnelServiceRecords = combine(
        _personnelServiceRecords,
        _searchQueryFlow,
        _selectedPriorityFilterFlow,
        _selectedTabFlow,
        _currentPersonnelUidFlow
    ) { records, query, priority, tab, currentUid ->
        filterPersonnelRecords(records, query, priority, tab, currentUid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPages: StateFlow<Int> = filteredPersonnelServiceRecords.map { list ->
        if (list.isEmpty()) 1 else (list.size + pageSize - 1) / pageSize
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val pagedPersonnelServiceRecords: StateFlow<List<ServiceRecord>> = combine(
        filteredPersonnelServiceRecords,
        _currentPage
    ) { list, page ->
        val maxPage = if (list.isEmpty()) 1 else (list.size + pageSize - 1) / pageSize
        val safePage = page.coerceIn(1, maxPage)
        val startIndex = (safePage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(list.size)
        if (startIndex < list.size) list.subList(startIndex, endIndex) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPage(page: Int) {
        val max = totalPages.value
        if (page in 1..max) {
            _currentPage.value = page
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.syncAllServices()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- FİLTRELEME FONKSİYONLARI ---
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
        sort: String
    ): List<ServiceRecord> {
        val lowerQuery = query.trim().lowercase(Locale.ROOT)

        val filtered = records.filter { record ->
            val notArchived = !record.isArchived

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
                "Atanmış" -> record.assignedPersonnelId != null
                "Atanmamış" -> record.assignedPersonnelId == null
                else -> true
            }

            notArchived && tabMatch && queryMatch && statusMatch && priorityMatch && deviceMatch && personnelMatch && companyMatch && locationMatch && assignmentMatch
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

    private fun filterPersonnelRecords(
        records: List<ServiceRecord>,
        query: String,
        priority: String,
        tab: String,
        currentUid: String?
    ): List<ServiceRecord> {
        val lowerQuery = query.trim().lowercase(Locale.ROOT)

        val personnelBaseRecords = records.filter { record ->
            val matchesPersonnel = if (!currentUid.isNullOrEmpty()) {
                record.assignedPersonnelUid == currentUid || record.assignedPersonnelId?.toString() == currentUid
            } else {
                true
            }
            matchesPersonnel && record.status != ServiceStatus.IPTAL
        }

        return personnelBaseRecords.filter { record ->
            val matchesSearch = if (lowerQuery.isEmpty()) true else {
                record.companyName.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        record.deviceType.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        record.serialNumber.lowercase(Locale.ROOT).contains(lowerQuery) ||
                        record.location.lowercase(Locale.ROOT).contains(lowerQuery)
            }
            val matchesPriority = if (priority == "Hepsi") true else record.priority == priority

            val matchesTab = when (tab) {
                "Tümü" -> true
                "Bekleyen", "Atanmış" -> record.status == ServiceStatus.BEKLIYOR
                "Yolda" -> record.status == ServiceStatus.YOLDA
                "İşlemde", "Devam Eden" -> record.status == ServiceStatus.ISLEME_BASLANDI || record.status == ServiceStatus.PARCA_BEKLENIYOR
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                else -> true
            }

            matchesSearch && matchesPriority && matchesTab
        }
    }

    // --- STATE GÜNCELLEME VE SAYFA SIFIRLAMA METOTLARI ---
    fun updateAdminSelectedStatusTab(status: String) {
        adminSelectedStatusTab = status
        _adminSelectedStatusTab.value = status
        _adminCurrentPage.value = 1
    }

    fun updateAdminSearchQuery(query: String) {
        adminSearchQuery = query
        _adminSearchQuery.value = query
        updateSearchQuery(query)
        _adminCurrentPage.value = 1
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

        // Compose/UI state'leri
        selectedStatusesFilter.value = statuses
        selectedPrioritiesFilter.value = priorities
        selectedDeviceTypesFilter.value = deviceTypes
        selectedPersonnelFilter = personnel
        selectedCompanyFilter = company
        selectedLocationFilter = location
        selectedAssignmentStatusFilter = assignment
        selectedSortOption = sort

        // Reaktif filtre Flow'ları
        _selectedStatusesFilter.value = statuses
        _selectedPrioritiesFilter.value = priorities
        _selectedDeviceTypesFilter.value = deviceTypes
        _selectedPersonnelFilterFlow.value = personnel
        _selectedCompanyFilterFlow.value = company
        _selectedLocationFilterFlow.value = location
        _selectedAssignmentStatusFilterFlow.value = assignment
        _selectedSortOptionFlow.value = sort

        _adminCurrentPage.value = 1
    }

    fun clearAllAdvancedFilters() {
        selectedDateFilter = "Tümü"
        customStartDate = null
        customEndDate = null

        // Compose/UI state'lerini temizle
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

        // Reaktif Flow state'lerini de aynı anda temizle
        _adminSearchQuery.value = ""
        _adminSelectedStatusTab.value = "Tümü"
        _selectedStatusesFilter.value = emptySet()
        _selectedPrioritiesFilter.value = emptySet()
        _selectedDeviceTypesFilter.value = emptySet()
        _selectedPersonnelFilterFlow.value = "Tümü"
        _selectedCompanyFilterFlow.value = "Tümü"
        _selectedLocationFilterFlow.value = "Tümü"
        _selectedAssignmentStatusFilterFlow.value = "Tümü"
        _selectedSortOptionFlow.value = "En yeni"

        _adminCurrentPage.value = 1
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

    fun updateSearchQuery(query: String) {
        searchQuery = query
        _searchQueryFlow.value = query
        _currentPage.value = 1
    }

    fun updateSelectedFilter(filter: String) {
        selectedFilter = filter
        _currentPage.value = 1
    }

    fun updateSelectedPriorityFilter(filter: String) {
        selectedPriorityFilter = filter
        _selectedPriorityFilterFlow.value = filter
        _currentPage.value = 1
    }

    fun updateSelectedTab(tab: String) {
        selectedTab = tab
        _selectedTabFlow.value = tab
        _currentPage.value = 1
    }

    // --- REPOSITORY VE VERİ YÖNETİMİ ---
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

    fun syncAdminData() {
        viewModelScope.launch {
            repository.syncAllServices()
            val updatedList = repository.getAllRecords()
            _serviceRecords.value = updatedList
        }
    }

    fun selectRecord(record: ServiceRecord) { _selectedRecord.value = record }
    fun clearSelection() { _selectedRecord.value = null }
    fun getServiceById(id: Int): ServiceRecord? = serviceRecords.value.find { it.id == id }

    fun insertRecord(record: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecord(record)
            loadRecords()
        }
    }

    fun deleteRecord(record: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = deleteServiceUseCase(record)

            if (result.isSuccess) {
                loadRecords()
            } else {
                _errorMessage.value =
                    result.exceptionOrNull()?.message
                        ?: "İş emri silinirken bir hata oluştu."
            }
        }
    }
    fun updateRecord(record: ServiceRecord) {
        viewModelScope.launch {
            val result = updateServiceUseCase(record)

            if (result.isSuccess) {
                loadRecords()
            } else {
                _errorMessage.value =
                    result.exceptionOrNull()?.message
                        ?: "İş emri güncellenirken bir hata oluştu."
            }
        }
    }

    fun updateStatus(recordId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecord = _serviceRecords.value.find { it.id == recordId } ?: return@launch
            if (currentRecord.status == ServiceStatus.TAMAMLANDI) {
                _errorMessage.value = "Tamamlanmış bir iş emrinin durumu değiştirilemez."
                return@launch
            }
            updateServiceStatusUseCase(recordId, newStatus)
            loadRecords()
            _selectedRecord.value = _selectedRecord.value?.copy(status = newStatus)
        }
    }

    fun verifyAndStartServiceWork(recordId: Int, personnelId: Int, distance: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            startServiceWorkUseCase(recordId, personnelId, distance)
            loadRecords()
            loadRecordsForPersonnel(personnelId)
            _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.ISLEME_BASLANDI)
        }
    }

    fun acceptService(recordId: Int, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            updateServiceStatusUseCase(recordId, ServiceStatus.YOLDA)
            loadRecords()
            loadRecordsForPersonnel(personnelId)
            _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.YOLDA)
        }
    }

    fun startServiceWork(recordId: Int, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecord = _serviceRecords.value.find { it.id == recordId } ?: return@launch
            if (currentRecord.status == ServiceStatus.YOLDA || currentRecord.status == ServiceStatus.BEKLIYOR) {
                updateServiceStatusUseCase(recordId, ServiceStatus.ISLEME_BASLANDI)
                loadRecords()
                loadRecordsForPersonnel(personnelId)
                _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.ISLEME_BASLANDI)
            }
        }
    }

    fun setParcaBekleniyor(recordId: Int, personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            updateServiceStatusUseCase(recordId, ServiceStatus.PARCA_BEKLENIYOR)
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
                _selectedRecord.value = _selectedRecord.value?.copy(status = ServiceStatus.IPTAL, rejectionReason = rejectionReason)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "İş reddedilirken bir hata oluştu."
            }
        }
    }

    fun reassignService(recordId: Int, newPersonnelId: Int?, newPersonnelUid: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentRecord = _serviceRecords.value.find { it.id == recordId } ?: return@launch
            if (currentRecord.status == ServiceStatus.TAMAMLANDI) {
                _errorMessage.value = "Tamamlanmış bir iş emri yeniden atanamaz."
                return@launch
            }
            val updatedRecord = currentRecord.copy(
                assignedPersonnelId = newPersonnelId,
                assignedPersonnelUid = newPersonnelUid,
                status = if (currentRecord.status == ServiceStatus.IPTAL) ServiceStatus.BEKLIYOR else currentRecord.status,
                rejectionReason = null
            )
            repository.updateService(updatedRecord)
            loadRecords()
            _selectedRecord.value = updatedRecord
        }
    }

    fun clearAssignedPersonnel(personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAssignedPersonnel(personnelId)
            loadRecords()
        }
    }

    fun archiveService(recordId: Int) {
        viewModelScope.launch {
            val result = archiveServiceUseCase(recordId)

            if (result.isSuccess) {
                loadRecords()

                if (_selectedRecord.value?.id == recordId) {
                    _selectedRecord.value =
                        _selectedRecord.value?.copy(
                            isArchived = true,
                            archivedAt = System.currentTimeMillis()
                        )
                }
            } else {
                _errorMessage.value =
                    result.exceptionOrNull()?.message
                        ?: "İş emri arşivlenirken bir hata oluştu."
            }
        }
    }

    // --- NOTLAR, FOTOĞRAFLAR VE KAPANIŞ İŞLEMLERİ ---
    fun loadServiceNotes(serviceRecordId: Int) {
        notesJob?.cancel()
        notesJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getNotesForService(serviceRecordId).collect { _serviceNotes.value = it }
        }
    }

    fun addServiceNote(note: ServiceNote) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertServiceNote(note) }
    }

    fun loadServicePhotos(serviceRecordId: Int) {
        photosJob?.cancel()
        photosJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getPhotosForService(serviceRecordId).collect { _servicePhotos.value = it }
        }
    }

    fun addServicePhoto(photo: ServicePhoto) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertServicePhoto(photo) }
    }

    fun updateClosingNote(note: String) { _closingNote.value = note }
    fun updateClosingSignatureUri(uri: String?) { _closingSignatureUri.value = uri }
    fun updateClosingAfterPhotoUri(uri: String?) { _closingAfterPhotoUri.value = uri }

    fun resetClosingState() {
        _closingState.value = ClosingState.Idle
        _closingNote.value = ""
        _closingSignatureUri.value = null
        _closingAfterPhotoUri.value = null
    }

    fun loadClosingSignature(serviceId: Int) {
        viewModelScope.launch {
            _serviceClosingSignature.value = try { repository.getClosingSignatureByServiceId(serviceId) } catch (e: Exception) { null }
        }
    }

    fun loadServiceHistory(firestoreId: String) {
        if (firestoreId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.getRemoteHistoryForService(firestoreId).onSuccess { _serviceHistory.value = it }
        }
    }

    fun loadRemoteMediaAndNotes(firestoreId: String) {
        if (firestoreId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.getRemoteNotesForService(firestoreId).onSuccess { _remoteNotes.value = it }
            repository.getRemotePhotosForService(firestoreId).onSuccess { _remotePhotos.value = it }
            repository.getRemoteSignaturesForService(firestoreId).onSuccess { _remoteSignatures.value = it }
        }
    }

    fun syncMyServices(firebaseUid: String, localPersonnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncServicesFromFirestore(firebaseUid, localPersonnelId)
            _personnelServiceRecords.value = repository.getRecordsByPersonnelId(localPersonnelId)
        }
    }

    fun submitClosingForm(serviceId: Int, personnelId: Int) {
        viewModelScope.launch {
            _closingState.value = ClosingState.Loading

            val note = _closingNote.value
            val signature = _closingSignatureUri.value
            val afterPhoto = _closingAfterPhotoUri.value

            if (note.isBlank()) {
                _closingState.value = ClosingState.Error("Kapanış notu eksik.")
                return@launch
            }

            if (signature.isNullOrBlank()) {
                _closingState.value = ClosingState.Error("Dijital imza eksik.")
                return@launch
            }

            if (afterPhoto.isNullOrBlank()) {
                _closingState.value = ClosingState.Error("Sonrası fotoğrafı eksik.")
                return@launch
            }

            val result = completeServiceUseCase(
                serviceId = serviceId,
                personnelId = personnelId,
                closingNoteText = note,
                signatureUri = signature,
                afterPhotoUri = afterPhoto
            )

            if (result.isSuccess) {
                _closingState.value = ClosingState.Success
                loadRecords()
                loadRecordsForPersonnel(personnelId)
            } else {
                _closingState.value = ClosingState.Error(
                    result.exceptionOrNull()?.message ?: "İşlem sırasında bilinmeyen bir hata oluştu."
                )
            }
        }
    }
}