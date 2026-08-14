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

    // --- FAZ 2.5: FOTOĞRAF STATE ---
    private val _servicePhotos = MutableStateFlow<List<ServicePhoto>>(emptyList())
    val servicePhotos: StateFlow<List<ServicePhoto>> = _servicePhotos.asStateFlow()

    private var notesJob: Job? = null
    private var photosJob: Job? = null

    // Reaktif Filtre StateFlow'ları
    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedFilterFlow = MutableStateFlow("Hepsi")
    private val _selectedPriorityFilterFlow = MutableStateFlow("Hepsi")
    private val _selectedTabFlow = MutableStateFlow("Tümü")

    private val _serviceClosingSignature =
        MutableStateFlow<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature?>(
            null
        )
    val serviceClosingSignature = _serviceClosingSignature.asStateFlow()

    // --- HATA MESAJI STATE'İ (İŞ KURALLARI İÇİN) ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // UI State'leri
    var searchQuery by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf("Hepsi")
        private set

    var selectedPriorityFilter by mutableStateOf("Hepsi")
        private set

    var selectedTab by mutableStateOf("Tümü")
        private set

    // --- KAPANIŞ FORMU STATE'LERİ ---
    private val _closingNote = MutableStateFlow("")
    val closingNote = _closingNote.asStateFlow()

    private val _closingSignatureUri = MutableStateFlow<String?>(null)
    val closingSignatureUri = _closingSignatureUri.asStateFlow()

    // UI'ın anlayacağı işlem sonuç state'i
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

    // Ortak Filtreleme Algoritması
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
            val matchesStatus = if (status == "Hepsi") true else record.status == status
            val matchesPriority = if (priority == "Hepsi") true else record.priority == priority

            val matchesTab = when (tab) {
                "Atanmamış" -> record.assignedPersonnelId == null
                "Atanan" -> record.assignedPersonnelId != null && record.status != ServiceStatus.TAMAMLANDI && record.status != ServiceStatus.IPTAL
                "Devam Eden" -> record.status == ServiceStatus.YOLDA || record.status == ServiceStatus.ISLEME_BASLANDI || record.status == ServiceStatus.PARCA_BEKLENIYOR
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                else -> true
            }

            matchesSearch && matchesStatus && matchesPriority && matchesTab
        }
    }

    // Admin Listesi
    val filteredServiceRecords = combine(
        _serviceRecords,
        _searchQueryFlow,
        _selectedFilterFlow,
        _selectedPriorityFilterFlow,
        _selectedTabFlow
    ) { records, query, status, priority, tab ->
        filterRecords(records, query, status, priority, tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personel Listesi
    val filteredPersonnelServiceRecords = combine(
        _personnelServiceRecords,
        _searchQueryFlow,
        _selectedFilterFlow,
        _selectedPriorityFilterFlow
    ) { records, query, status, priority ->
        filterRecords(records, query, status, priority, "Tümü")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Uygulama her açıldığında Firebase'deki iş emirlerini yerel veritabanına (Room) indir
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
            // İş kurallarını doğrulamak için veritabanındaki mevcut kaydı al
            val currentRecord = _serviceRecords.value.find { it.id == record.id }

            if (currentRecord != null) {
                // 1. KURAL: Tamamlanmış (veya İptal) iş emri üzerinde operasyonel güncelleme yapılamaz.
                if (currentRecord.status == ServiceStatus.TAMAMLANDI || currentRecord.status == ServiceStatus.IPTAL) {
                    return@launch
                }

                // 2. KURAL: Personel Atama / Değiştirme
                if (currentRecord.assignedPersonnelId != record.assignedPersonnelId) {
                    // YENİ KURAL: Yalnızca BEKLIYOR durumundaysa atamaya veya mevcut personeli değiştirmeye izin ver
                    val canAssignOrChange = currentRecord.status == ServiceStatus.BEKLIYOR
                    if (!canAssignOrChange) {
                        return@launch // İşleme Başlandı, Yolda vb. ise reddet
                    }
                }
            }

            repository.updateService(record)
            loadRecords() // Ekranın yenilenmesi için
        }
    }

    fun updateStatus(recordId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // İşlemi yapmadan önce mevcut kaydı bul
            val currentRecord = _serviceRecords.value.find { it.id == recordId } ?: return@launch

            // KURAL C: Tamamlanmış iş emri üzerinde durum değişikliği yapılamaz
            if (currentRecord.status == ServiceStatus.TAMAMLANDI) {
                _errorMessage.value = "Tamamlanmış bir iş emrinin durumu değiştirilemez."
                return@launch
            }

            // KURAL A: Aynı personelin aynı anda birden fazla aktif iş almasını engelle
            val activeStatuses = listOf(ServiceStatus.YOLDA, ServiceStatus.ISLEME_BASLANDI)
            if (newStatus in activeStatuses && currentRecord.assignedPersonnelId != null) {

                // Personelin şu anki aktif işlerini kontrol et (Kendisi hariç)
                val activeJobs = _serviceRecords.value.filter {
                    it.assignedPersonnelId == currentRecord.assignedPersonnelId &&
                            it.status in activeStatuses &&
                            it.id != recordId
                }

                if (activeJobs.isNotEmpty()) {
                    _errorMessage.value =
                        "Aynı anda birden fazla işleme başlanamaz veya yola çıkılamaz."
                    return@launch
                }
            }

            // Kurallar başarıyla geçilirse güncellemeyi yap
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

    // --- FAZ 2.5: FOTOĞRAF İŞLEMLERİ ---

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

    // --- KAPANIŞ FORMU FONKSİYONLARI ---

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
            // 1. Arka planda Firebase'den verileri çek ve Room'u güncelle
            repository.syncAllServices()

            // 2. EKRANI YENİLE
            val updatedList = repository.getAllRecords()
            _serviceRecords.value = updatedList
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.syncAllServices()
            val updatedList = repository.getAllRecords()
            _serviceRecords.value = updatedList
        }
    }

    fun syncPersonnelData(uid: String, personnelId: Int) {
        viewModelScope.launch {
            repository.syncServicesFromFirestore(
                personnelUid = uid,
                localPersonnelId = personnelId
            )
        }
    }

    fun syncMyServices(firebaseUid: String, localPersonnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncServicesFromFirestore(
                    personnelUid = firebaseUid,
                    localPersonnelId = localPersonnelId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun submitClosingForm(serviceId: Int, personnelId: Int) {
        viewModelScope.launch {
            // 1. UI'a yükleniyor durumunu bildir
            _closingState.value = ClosingState.Loading

            // 2. Gerekli verileri mevcut State'lerden ve listeden topla
            val currentRecord = getServiceById(serviceId)
            val note = _closingNote.value
            val signature = _closingSignatureUri.value

            if (currentRecord == null || signature == null || note.isBlank()) {
                _closingState.value = ClosingState.Error("Eksik veri: Lütfen formu kontrol edin.")
                return@launch
            }

            // 3. Repository'deki işlemi çağır
            val result = repository.completeServiceWork(
                serviceRecord = currentRecord,
                personnelId = personnelId,
                closingNoteText = note,
                signatureUri = signature
            )

            // 4. Sonucu UI State'ine aktar
            if (result.isSuccess) {
                _closingState.value = ClosingState.Success
                loadRecords() // İşlem bitince listeyi yenile
            } else {
                _closingState.value = ClosingState.Error(result.exceptionOrNull()?.message ?: "İşlem sırasında bir hata oluştu.")
            }
        }
    }
}