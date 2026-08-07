package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun updatePersonnel(personnel: Personnel, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updatePersonnel(personnel)
                // Mevcut listeyi tazeleyen fonksiyonunu buraya ekle (örn: loadPersonnel() veya getPersonnelList())
                // loadPersonnel()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
    fun getPersonnelById(id: Int): Personnel? {
        // personnelList StateFlow/LiveData adın neyse ona göre uyarla
        return personnelList.value.find { it.id == id }
    }

    suspend fun getPersonnelByUsername(username: String): Personnel? {
        return repository.getPersonnelByUsername(username)
    }

    fun deletePersonnel(personnel: Personnel) {
        viewModelScope.launch {
            repository.deletePersonnel(personnel)
        }
    }

    fun getPersonnelById(id: Int, onResult: (Personnel?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val personnel = repository.getPersonnelById(id)
            withContext(Dispatchers.Main) {
                onResult(personnel)
            }
        }
    }
}