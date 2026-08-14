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
    // --- YENİ EKLENEN FİREBASE + ROOM SENKRONİZASYON METODU ---
    suspend fun addPersonnelWithFirebase(personnel: Personnel, context: Context): Result<Unit> {
        // 1. Kullanıcı adı benzersizlik kontrolü (Room üzerinden)
        val existingUser = repository.getPersonnelByUsername(personnel.username)
        if (existingUser != null) {
            return Result.failure(Exception("Bu kullanıcı adı zaten sistemde kayıtlı."))
        }

        return try {
            // 2. SECONDARY FIREBASE APP (Admin oturumunu korumak için)
            val defaultApp = FirebaseApp.getInstance()
            val secondaryAppName = "PersonnelCreationApp"

            var secondaryApp = FirebaseApp.getApps(context).find { it.name == secondaryAppName }
            if (secondaryApp == null) {
                secondaryApp = FirebaseApp.initializeApp(context, defaultApp.options, secondaryAppName)
            }

            // Geçici uygulamanın Auth instance'ını alıyoruz
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)

            // 3. Personeli Firebase Auth üzerinde oluştur ve UID'yi al
            val authResult = secondaryAuth.createUserWithEmailAndPassword(personnel.email, personnel.password).await()
            val generatedUid = authResult.user?.uid ?: throw Exception("Firebase UID oluşturulamadı.")

            // 4. İkincil oturumu kapat (Admin'in ana instance'ı etkilenmez)
            secondaryAuth.signOut()

            // 5. Üretilen gerçek UID'yi Personel nesnesine ekle
            val newPersonnel = personnel.copy(firebaseUid = generatedUid)

            // 6. Firestore ve Room senkronizasyonunu başlat
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