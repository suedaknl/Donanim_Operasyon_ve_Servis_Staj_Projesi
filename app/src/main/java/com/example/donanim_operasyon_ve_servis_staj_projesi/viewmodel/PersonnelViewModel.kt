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

    init {
        // ViewModel ilk ayağa kalktığında Firebase'deki personelleri Room ile senkronize et
        syncPersonnelData()
    }

    // --- FAZ 3: FİREBASE -> ROOM PERSONEL SENKRONİZASYONU ---
    fun syncPersonnelData() {
        viewModelScope.launch {
            try {
                repository.syncAllPersonnel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    suspend fun syncPersonnelWithFirebase(email: String, firebaseUid: String): Result<Personnel> {
        val personnel = repository.getPersonnelByEmail(email)

        return if (personnel != null) {
            if (!personnel.isActive) {
                Result.failure(Exception("Hesabınız pasif durumdadır."))
            } else {
                // İlk kez giriş yapıyorsa veya UID eksikse Room'a yaz
                val finalPersonnel = if (personnel.firebaseUid != firebaseUid) {
                    val updatedPersonnel = personnel.copy(firebaseUid = firebaseUid)
                    repository.updatePersonnel(updatedPersonnel)
                    updatedPersonnel
                } else {
                    personnel
                }

                // ---- FAZ 4: FIRESTORE WRITE TESTİ BAŞLANGICI ----
                try {
                    repository.testSyncPersonnelToFirestore(finalPersonnel)
                    println("FIRESTORE TEST: Veri başarıyla gönderildi!")
                } catch (e: Exception) {
                    println("FIRESTORE TEST HATASI: ${e.message}")
                }
                // ---- FAZ 4: FIRESTORE WRITE TESTİ BİTİŞİ ----

                Result.success(finalPersonnel)
            }
        } else {
            Result.failure(Exception("Bu hesap sisteme kayıtlı bir personelle eşleşmiyor."))
        }
    }

    fun updatePersonnel(personnel: Personnel, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updatePersonnel(personnel)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun getPersonnelById(id: Int): Personnel? {
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