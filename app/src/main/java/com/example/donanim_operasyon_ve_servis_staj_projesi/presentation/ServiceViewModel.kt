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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ServiceViewModel(private val repository: ServiceRepository) : ViewModel() {

    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords

    // Detay ekranında gösterilecek seçili kaydı tutan state
    private val _selectedRecord = MutableStateFlow<ServiceRecord?>(null)
    val selectedRecord: StateFlow<ServiceRecord?> = _selectedRecord.asStateFlow()

    // --- Arama ve Filtreleme State'leri ---
    var searchQuery by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf("Hepsi")
        private set
    // -----------------------------------------------------------

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getAllRecords()
            _serviceRecords.value = records
        }
    }

    // --- Arama ve Filtreleme Güncelleme Fonksiyonları ---
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateSelectedFilter(filter: String) {
        selectedFilter = filter
    }
    // ------------------------------------------------------------------------

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

    // YENİLENDİ: Update işlemi artık I/O thread'inde yapılıp sonrasında listeyi güncelliyor
    fun updateRecord(service: ServiceRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateService(service)
            loadRecords() // EKSİK OLAN SATIR EKLENDİ: Ekranlar anında güncellenecek
        }
    }

    // (Opsiyonel) Durum güncelleme fonksiyonu ve anlık state senkronizasyonu
    fun updateStatus(recordId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(recordId, newStatus)
            loadRecords() // Listeyi tazele

            // Seçili olan kaydı da güncel tut ki detay ekranında anında yansısın
            _selectedRecord.value = _selectedRecord.value?.copy(status = newStatus)
        }
    }

    // Düzenleme ekranı için ID'ye göre tekil kayıt getirme (suspend kaldırıldı)
    suspend fun getServiceById(id: Int): ServiceRecord? {
        return repository.getServiceById(id)
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