package com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.WorkforceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.personnel.DeletePersonnelUseCase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PersonnelViewModel @Inject constructor(
    private val repository: PersonnelRepository,
    private val workforceRepository: WorkforceRepository,
    private val deletePersonnelUseCase: DeletePersonnelUseCase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    val personnelList: StateFlow<List<Personnel>> = repository.getAllPersonnel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val serviceRecords: StateFlow<List<ServiceRecord>> = flowOf<List<ServiceRecord>>(emptyList())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val leaveRequests: StateFlow<List<LeaveRequestEntity>> = workforceRepository.getPendingLeaveRequests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedRole = MutableStateFlow("Tümü")
    val selectedRole = _selectedRole.asStateFlow()

    private val _selectedStatus = MutableStateFlow("Tümü")
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _sortOption = MutableStateFlow("İsim (A-Z)")
    val sortOption = _sortOption.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage
    val pageSize = 4

    private val _passwordUpdateState = mutableStateOf<Result<Unit>?>(null)
    val passwordUpdateState: State<Result<Unit>?> = _passwordUpdateState

    val availableRoles: StateFlow<List<String>> =
        personnelList
            .map { list ->
                listOf("Tümü") +
                        list.map { it.role }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                listOf("Tümü")
            )

    val activeFilterCount: StateFlow<Int> =
        combine(
            _selectedRole,
            _selectedStatus
        ) { role, status ->
            var count = 0
            if (role != "Tümü") count++
            if (status != "Tümü") count++
            count
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    val filteredPersonnelList: StateFlow<List<Personnel>> =
        combine(
            personnelList,
            _searchQuery,
            _selectedRole,
            _selectedStatus,
            _sortOption
        ) { list, query, role, status, sort ->
            var result = list

            if (query.isNotBlank()) {
                result = result.filter { personnel ->
                    personnel.fullName.contains(query, ignoreCase = true) ||
                            personnel.role.contains(query, ignoreCase = true) ||
                            personnel.phoneNumber.contains(query, ignoreCase = true) ||
                            personnel.username.contains(query, ignoreCase = true)
                }
            }

            if (role != "Tümü") {
                result = result.filter { it.role.equals(role, ignoreCase = true) }
            }

            if (status != "Tümü") {
                val active = status == "Aktif"
                result = result.filter { it.isActive == active }
            }

            when (sort) {
                "İsim (A-Z)" -> result.sortedBy { it.fullName.lowercase() }
                "İsim (Z-A)" -> result.sortedByDescending { it.fullName.lowercase() }
                "Aktif Önce" -> result.sortedByDescending { it.isActive }
                "Pasif Önce" -> result.sortedBy { it.isActive }
                else -> result
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val totalPages: StateFlow<Int> = filteredPersonnelList.map { list ->
        if (list.isEmpty()) 1 else (list.size + pageSize - 1) / pageSize
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val pagedPersonnelList: StateFlow<List<Personnel>> = combine(
        filteredPersonnelList,
        _currentPage
    ) { list, page ->
        val maxPage = if (list.isEmpty()) 1 else (list.size + pageSize - 1) / pageSize
        val safePage = page.coerceIn(1, maxPage)
        val startIndex = (safePage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(list.size)
        if (startIndex < list.size) {
            list.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updatePassword(currentPass: String, newPass: String) {
        val user = auth.currentUser
        val email = user?.email

        if (email.isNullOrEmpty()) {
            _passwordUpdateState.value = Result.failure(Exception("Oturum açmış kullanıcı bulunamadı."))
            return
        }

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPass)

        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(newPass).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        _passwordUpdateState.value = Result.success(Unit)
                    } else {
                        val errorMsg = translateFirebaseError(updateTask.exception?.message)
                        _passwordUpdateState.value = Result.failure(Exception(errorMsg))
                    }
                }
            } else {
                val errorMsg = translateFirebaseError(reauthTask.exception?.message)
                _passwordUpdateState.value = Result.failure(Exception(errorMsg))
            }
        }
    }

    fun resetPasswordState() {
        _passwordUpdateState.value = null
    }

    private fun translateFirebaseError(message: String?): String {
        return when {
            message.orEmpty().contains("password", ignoreCase = true) && message.orEmpty().contains("6", ignoreCase = true) -> "Yeni şifre en az 6 karakter olmalıdır."
            message.orEmpty().contains("credentials", ignoreCase = true) || message.orEmpty().contains("mismatch", ignoreCase = true) -> "Mevcut şifreniz hatalı."
            else -> "İşlem başarısız oldu. Lütfen tekrar deneyin."
        }
    }

    fun updatePersonnelLocation(lat: Double, lon: Double) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("users").document(userId).update(
                    mapOf(
                        "currentLatitude" to lat,
                        "currentLongitude" to lon,
                        "latitude" to lat,
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

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
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
        viewModelScope.launch {
            deletePersonnelUseCase(personnel)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun updateSelectedRole(role: String) {
        _selectedRole.value = role
        _currentPage.value = 1
    }

    fun updateSelectedStatus(status: String) {
        _selectedStatus.value = status
        _currentPage.value = 1
    }

    fun updateSortOption(option: String) {
        _sortOption.value = option
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        val max = totalPages.value
        if (page in 1..max) {
            _currentPage.value = page
        }
    }

    fun clearAllFilters() {
        _selectedRole.value = "Tümü"
        _selectedStatus.value = "Tümü"
        _searchQuery.value = ""
        _sortOption.value = "İsim (A-Z)"
        _currentPage.value = 1
    }
}