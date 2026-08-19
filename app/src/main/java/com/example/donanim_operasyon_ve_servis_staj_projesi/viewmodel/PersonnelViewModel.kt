package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PersonnelViewModel(private val repository: PersonnelRepository) : ViewModel() {

    // Firebase servislerini tanımlıyoruz
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val personnelList: StateFlow<List<Personnel>> = repository.getAllPersonnel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _locationStatus = MutableStateFlow("Konum Bekleniyor...")
    val locationStatus = _locationStatus.asStateFlow()

    private val _personnelLocations = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val personnelLocations = _personnelLocations.asStateFlow()

    init {
        syncPersonnelData()
        refreshPersonnelLocations()
    }

    fun refreshPersonnelLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getLivePersonnelLocations()
            result.onSuccess { data ->
                _personnelLocations.value = data
            }
        }
    }

    fun updateLocationStatus(status: String) {
        _locationStatus.value = status
    }

    // PERSONELİN KENDİ KONUMUNU GÜNCELLEMESİ (Firebase'e yazar)
    fun updatePersonnelLocation(lat: Double, lon: Double) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId).update(
                    mapOf(
                        "currentLatitude" to lat,
                        "currentLongitude" to lon,
                        "latitude" to lat, // İki tarafın da uyumlu olması için
                        "longitude" to lon,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    _locationStatus.value = "Konum: Aktif"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ADMIN TARAFI İÇİN KONUM GÜNCELLEME (Repository üzerinden)
    fun updateCurrentLocation(uid: String, lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateLocation(uid, lat, lon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        if (existingUser != null) return false
        repository.insertPersonnel(personnel)
        return true
    }

    suspend fun addPersonnelWithFirebase(personnel: Personnel, context: Context): Result<Unit> {
        val existingUser = repository.getPersonnelByUsername(personnel.username)
        if (existingUser != null) return Result.failure(Exception("Kullanıcı zaten kayıtlı."))

        return try {
            val defaultApp = FirebaseApp.getInstance()
            val secondaryAppName = "PersonnelCreationApp"
            var secondaryApp = FirebaseApp.getApps(context).find { it.name == secondaryAppName }
            if (secondaryApp == null) secondaryApp = FirebaseApp.initializeApp(context, defaultApp.options, secondaryAppName)

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)
            val authResult = secondaryAuth.createUserWithEmailAndPassword(personnel.email, personnel.password).await()
            val generatedUid = authResult.user?.uid ?: throw Exception("UID alınamadı.")

            secondaryAuth.signOut()

            val newPersonnel = personnel.copy(firebaseUid = generatedUid)
            repository.addPersonnelWithFirebaseSync(newPersonnel)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPersonnelWithFirebase(email: String, firebaseUid: String): Result<Personnel> {
        val personnel = repository.getPersonnelByEmail(email)
        return if (personnel != null) {
            if (!personnel.isActive) Result.failure(Exception("Hesap pasif."))
            else {
                val finalPersonnel = if (personnel.firebaseUid != firebaseUid) {
                    val updated = personnel.copy(firebaseUid = firebaseUid)
                    repository.updatePersonnel(updated)
                    updated
                } else personnel

                repository.testSyncPersonnelToFirestore(finalPersonnel)
                Result.success(finalPersonnel)
            }
        } else Result.failure(Exception("Kullanıcı bulunamadı."))
    }

    fun updatePersonnel(personnel: Personnel, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updatePersonnel(personnel)
                repository.testSyncPersonnelToFirestore(personnel)
                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun getPersonnelById(id: Int): Personnel? = personnelList.value.find { it.id == id }

    fun getPersonnelById(id: Int, onResult: (Personnel?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val personnel = repository.getPersonnelById(id)
            withContext(Dispatchers.Main) { onResult(personnel) }
        }
    }

    fun deletePersonnel(personnel: Personnel) {
        viewModelScope.launch { repository.deletePersonnel(personnel) }
    }
}