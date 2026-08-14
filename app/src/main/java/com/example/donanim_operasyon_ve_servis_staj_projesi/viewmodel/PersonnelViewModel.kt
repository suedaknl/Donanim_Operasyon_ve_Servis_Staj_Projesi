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
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class PersonnelViewModel(private val repository: PersonnelRepository) : ViewModel() {

    val personnelList: StateFlow<List<Personnel>> = repository.getAllPersonnel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        syncPersonnelData()
    }

    fun syncPersonnelData() {
        viewModelScope.launch {
            try {
                repository.syncAllPersonnel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addPersonnel(personnel: Personnel): Boolean {
        val existingUser = repository.getPersonnelByUsername(personnel.username)
        if (existingUser != null) {
            return false
        }
        repository.insertPersonnel(personnel)
        return true
    }

    suspend fun addPersonnelWithFirebase(personnel: Personnel, context: Context): Result<Unit> {
        val existingUser = repository.getPersonnelByUsername(personnel.username)
        if (existingUser != null) {
            return Result.failure(Exception("Bu kullanıcı adı zaten sistemde kayıtlı."))
        }

        return try {
            val defaultApp = FirebaseApp.getInstance()
            val secondaryAppName = "PersonnelCreationApp"

            var secondaryApp = FirebaseApp.getApps(context).find { it.name == secondaryAppName }
            if (secondaryApp == null) {
                secondaryApp = FirebaseApp.initializeApp(context, defaultApp.options, secondaryAppName)
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)
            val authResult = secondaryAuth.createUserWithEmailAndPassword(personnel.email, personnel.password).await()
            val generatedUid = authResult.user?.uid ?: throw Exception("Firebase UID oluşturulamadı.")

            secondaryAuth.signOut()

            val newPersonnel = personnel.copy(firebaseUid = generatedUid)
            val syncResult = repository.addPersonnelWithFirebaseSync(newPersonnel)

            if (syncResult.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(syncResult.exceptionOrNull() ?: Exception("Veritabanı kaydı sırasında hata oluştu."))
            }

        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Bilinmeyen bir hata oluştu."))
        }
    }

    suspend fun syncPersonnelWithFirebase(email: String, firebaseUid: String): Result<Personnel> {
        val personnel = repository.getPersonnelByEmail(email)

        return if (personnel != null) {
            if (!personnel.isActive) {
                Result.failure(Exception("Hesabınız pasif durumdadır."))
            } else {
                val finalPersonnel = if (personnel.firebaseUid != firebaseUid) {
                    val updatedPersonnel = personnel.copy(firebaseUid = firebaseUid)
                    repository.updatePersonnel(updatedPersonnel)
                    updatedPersonnel
                } else {
                    personnel
                }

                try {
                    repository.testSyncPersonnelToFirestore(finalPersonnel)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Result.success(finalPersonnel)
            }
        } else {
            Result.failure(Exception("Bu hesap sisteme kayıtlı bir personelle eşleşmiyor."))
        }
    }

    // --- GÜNCELLENEN GÜNCELLEME METODU (Firestore Sync Eklendi) ---
    fun updatePersonnel(personnel: Personnel, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Önce Room'da güncelle
                repository.updatePersonnel(personnel)
                // 2. Ardından Firestore'a senkronize et (gender, aktiflik vb. değişiklikler uçsun)
                repository.testSyncPersonnelToFirestore(personnel)

                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
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