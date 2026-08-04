package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceViewModel(private val repository: ServiceRepository) : ViewModel() {

    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getAllRecords()
            _serviceRecords.value = records
        }
    }

    fun insertRecord(record: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecord(record)
            loadRecords()
        }
    }

    // Kaydı silme fonksiyonumuz
    fun deleteRecord(record: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecord(record)
            loadRecords()
        }
    }
}

// ViewModel Factory (Çökme sorununu engelleyen yapı)
class ServiceViewModelFactory(private val repository: ServiceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
}