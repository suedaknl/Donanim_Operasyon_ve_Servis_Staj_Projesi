package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.registry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.model.ServiceRegistryStats
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.GetServiceRegistryStatsUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.GetServiceRegistryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServiceRegistryViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val getServiceRegistryUseCase: GetServiceRegistryUseCase,
    private val getServiceRegistryStatsUseCase: GetServiceRegistryStatsUseCase
) : ViewModel() {

    private val _allRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("Tümü")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _selectedSortOption = MutableStateFlow("En yeni")
    val selectedSortOption: StateFlow<String> = _selectedSortOption.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Reaktif Filtrelenmiş Liste
    val filteredRecords: StateFlow<List<ServiceRecord>> = combine(
        _allRecords,
        _searchQuery,
        _selectedStatusFilter,
        _selectedSortOption
    ) { records, query, status, sort ->
        var result = getServiceRegistryUseCase(records, query)

        // Durum Filtresi
        if (status != "Tümü") {
            result = result.filter { record ->
                val s = record.status.trim()
                when (status) {
                    "Bekliyor" -> s.equals(ServiceStatus.BEKLIYOR, ignoreCase = true) || s.equals("Bekliyor", ignoreCase = true)
                    "Yolda" -> s.equals(ServiceStatus.YOLDA, ignoreCase = true) || s.equals("Yolda", ignoreCase = true)
                    "İşlemde" -> s.equals(ServiceStatus.ISLEME_BASLANDI, ignoreCase = true) ||
                            s.equals(ServiceStatus.PARCA_BEKLENIYOR, ignoreCase = true) ||
                            s.equals("İşlemde", ignoreCase = true) ||
                            s.equals("İşlem Başlandı", ignoreCase = true)
                    "Tamamlandı" -> s.equals(ServiceStatus.TAMAMLANDI, ignoreCase = true) || s.equals("Tamamlandı", ignoreCase = true)
                    "İptal" -> s.equals(ServiceStatus.IPTAL, ignoreCase = true) || s.equals("İptal", ignoreCase = true)
                    else -> true
                }
            }
        }

        // Seri Numarası Tekrar Hesaplamaları (Normalize edilmiş)
        val serialCounts = records.groupingBy { it.serialNumber.trim().lowercase() }.eachCount()

        // Sıralama ve Filtreleme Kriteri
        result = when (sort) {
            "En eski" -> result.sortedBy { it.date }
            "En çok tekrar eden cihaz" -> {
                // Sadece birden fazla kez (>=2) kaydı olan tekrar eden cihazları filtrele ve tekrar sayısına göre sırala
                result.filter { record ->
                    val serial = record.serialNumber.trim().lowercase()
                    (serialCounts[serial] ?: 1) > 1
                }.sortedByDescending { record ->
                    serialCounts[record.serialNumber.trim().lowercase()] ?: 1
                }
            }
            "Son 6 ay" -> {
                val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
                result.filter { (it.archivedAt ?: 0L) >= sixMonthsAgo || it.date.isNotBlank() }
            }
            else -> result.sortedByDescending { it.date } // En yeni
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ServiceRegistryStats> = combine(
        _allRecords,
        filteredRecords
    ) { all, filtered ->
        getServiceRegistryStatsUseCase(all, filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceRegistryStats())

    init {
        loadAllRecords()
    }

    fun loadAllRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val records = serviceRepository.getAllRecords()
                _allRecords.value = records
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun setSortOption(sort: String) {
        _selectedSortOption.value = sort
    }

    fun clearFilters() {
        _selectedStatusFilter.value = "Tümü"
        _selectedSortOption.value = "En yeni"
    }
}