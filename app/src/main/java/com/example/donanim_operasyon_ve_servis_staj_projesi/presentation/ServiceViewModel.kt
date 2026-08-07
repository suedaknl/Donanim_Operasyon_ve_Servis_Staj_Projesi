package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceViewModel(private val repository: ServiceRepository) : ViewModel() {

    // Admin tarafı için mevcut akış
    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords

    // --- AŞAMA 2.1: PERSONEL TARAFI İÇİN STATEFLOW ---
    private val _personnelServiceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val personnelServiceRecords: StateFlow<List<ServiceRecord>> = _personnelServiceRecords.asStateFlow()

    private val _selectedRecord = MutableStateFlow<ServiceRecord?>(null)
    val selectedRecord: StateFlow<ServiceRecord?> = _selectedRecord.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf("Hepsi")
        private set

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getAllRecords()
            _serviceRecords.value = records
        }
    }

    // --- AŞAMA 2.1: SADECE BELİRLİ PERSONELE AİT İŞLERİ YÜKLEME ---
    fun loadRecordsForPersonnel(personnelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getRecordsByPersonnelId(personnelId)
            _personnelServiceRecords.value = records
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateSelectedFilter(filter: String) {
        selectedFilter = filter
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

    fun updateRecord(service: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateService(service)
            loadRecords()
        }
    }

    fun updateStatus(recordId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
}

class ServiceViewModelFactory(private val repository: ServiceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
}