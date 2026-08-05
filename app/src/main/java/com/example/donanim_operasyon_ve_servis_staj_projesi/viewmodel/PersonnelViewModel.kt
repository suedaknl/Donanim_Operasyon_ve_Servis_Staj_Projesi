package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonnelViewModel(private val repository: PersonnelRepository) : ViewModel() {

    val personnelList: StateFlow<List<Personnel>> = repository.getAllPersonnel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Ekleme işlemi artık suspend ve Boolean döndürüyor (Benzersizlik kontrolü için)
    suspend fun addPersonnel(personnel: Personnel): Boolean {
        val existingUser = repository.getPersonnelByUsername(personnel.username)

        if (existingUser != null) {
            // Kullanıcı adı zaten mevcut, kayıt başarısız
            return false
        }

        // Kullanıcı adı boşta, kaydı tamamla
        repository.insertPersonnel(personnel)
        return true
    }

    fun updatePersonnel(personnel: Personnel) {
        viewModelScope.launch {
            repository.updatePersonnel(personnel)
        }
    }

    fun deletePersonnel(personnel: Personnel) {
        viewModelScope.launch {
            repository.deletePersonnel(personnel)
        }
    }
}