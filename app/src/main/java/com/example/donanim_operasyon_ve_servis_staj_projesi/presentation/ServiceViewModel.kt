package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation

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

    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords

    // Detay ekranında gösterilecek seçili kaydı tutan state
    private val _selectedRecord = MutableStateFlow<ServiceRecord?>(null)
    val selectedRecord: StateFlow<ServiceRecord?> = _selectedRecord.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getAllRecords()
            _serviceRecords.value = records
        }
    }

    // Hangi karta tıklandığını seçme
    fun selectRecord(record: ServiceRecord) {
        _selectedRecord.value = record
    }

    // Detay ekranından çıkış yapıldığında seçimi temizleme
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

    // Yeni: Durum güncelleme fonksiyonu ve anlık state senkronizasyonu
    fun updateStatus(recordId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(recordId, newStatus)
            loadRecords() // Listeyi tazele

            // Seçili olan kaydı da güncel tut ki detay ekranında anında yansısın
            _selectedRecord.value = _selectedRecord.value?.copy(status = newStatus)
        }
    }
}

// Çökme sorununu engelleyen Factory sınıfımız
class ServiceViewModelFactory(private val repository: ServiceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
}