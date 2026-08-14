package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonnelScreen(
    viewModel: PersonnelViewModel,
    personnelId: Int? = null,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var originalPersonnel by remember { mutableStateOf<Personnel?>(null) }

    var fullName by remember { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf("") }

    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }

    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }

    var role by remember { mutableStateOf("") }
    var roleError by remember { mutableStateOf("") }

    // --- CİNSİYET STATE'İ EKLENDİ ---
    var gender by remember { mutableStateOf("ERKEK") }

    var isActive by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val isEditMode = personnelId != null

    // Sayfa açıldığında ID ile veritabanından personeli çek
    LaunchedEffect(personnelId) {
        if (isEditMode && personnelId != null) {
            viewModel.getPersonnelById(personnelId) { personnel ->
                if (personnel != null) {
                    originalPersonnel = personnel
                    fullName = personnel.fullName
                    username = personnel.username
                    email = personnel.email
                    password = personnel.password
                    phoneNumber = personnel.phoneNumber
                    role = personnel.role
                    isActive = personnel.isActive
                    gender = personnel.gender // Kayıtlı cinsiyeti yükle
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Personel bilgisi bulunamadı.")
                        delay(1000)
                        onNavigateBack()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Personel Düzenle" else "Yeni Personel",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; fullNameError = "" },
                label = { Text("Ad Soyad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = fullNameError.isNotEmpty(),
                supportingText = {
                    if (fullNameError.isNotEmpty()) Text(
                        fullNameError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; usernameError = "" },
                label = { Text("Kullanıcı Adı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = usernameError.isNotEmpty(),
                supportingText = {
                    if (usernameError.isNotEmpty()) Text(
                        usernameError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = "" },
                label = { Text("E-posta Adresi") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = emailError.isNotEmpty(),
                supportingText = {
                    if (emailError.isNotEmpty()) Text(
                        emailError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = "" },
                label = { Text("Şifre") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image =
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = passwordError.isNotEmpty(),
                supportingText = {
                    if (passwordError.isNotEmpty()) Text(
                        passwordError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; phoneError = "" },
                label = { Text("Telefon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = phoneError.isNotEmpty(),
                supportingText = {
                    if (phoneError.isNotEmpty()) Text(
                        phoneError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it; roleError = "" },
                label = { Text("Görev (Örn: Saha Personeli)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = roleError.isNotEmpty(),
                supportingText = {
                    if (roleError.isNotEmpty()) Text(
                        roleError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            )

            // --- CİNSİYET SEÇİM UI BİLEŞENİ (Erkek / Kadın) ---
            Text("Cinsiyet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { gender = "ERKEK" }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (gender == "ERKEK"),
                        onClick = { gender = "ERKEK" }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Erkek")
                }

                Row(
                    modifier = Modifier
                        .clickable { gender = "KADIN" }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (gender == "KADIN"),
                        onClick = { gender = "KADIN" }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kadın")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Personel Aktif mi?", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isSaving) return@Button

                    var isValid = true
                    if (fullName.isBlank()) {
                        fullNameError = "Bu alan zorunludur."; isValid = false
                    }
                    if (username.isBlank()) {
                        usernameError = "Bu alan zorunludur."; isValid = false
                    }
                    if (email.isBlank()) {
                        emailError = "Bu alan zorunludur."; isValid = false
                    }
                    if (password.isBlank()) {
                        passwordError = "Bu alan zorunludur."; isValid = false
                    }
                    if (phoneNumber.isBlank()) {
                        phoneError = "Bu alan zorunludur."; isValid = false
                    }
                    if (role.isBlank()) {
                        roleError = "Bu alan zorunludur."; isValid = false
                    }

                    if (isValid) {
                        isSaving = true

                        if (isEditMode && originalPersonnel != null) {
                            val hasChanges = fullName.trim() != originalPersonnel!!.fullName ||
                                    username.trim() != originalPersonnel!!.username ||
                                    email.trim() != originalPersonnel!!.email ||
                                    password.trim() != originalPersonnel!!.password ||
                                    phoneNumber.trim() != originalPersonnel!!.phoneNumber ||
                                    role.trim() != originalPersonnel!!.role ||
                                    isActive != originalPersonnel!!.isActive ||
                                    gender != originalPersonnel!!.gender

                            if (!hasChanges) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Personel bilgilerinde değişiklik yapılmadı.")
                                }
                                isSaving = false
                                return@Button
                            }

                            val updatedPersonnel = originalPersonnel!!.copy(
                                fullName = fullName.trim(),
                                username = username.trim(),
                                email = email.trim(),
                                password = password.trim(),
                                phoneNumber = phoneNumber.trim(),
                                role = role.trim(),
                                isActive = isActive,
                                gender = gender
                            )

                            viewModel.updatePersonnel(updatedPersonnel) { success ->
                                if (success) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Personel bilgileri güncellendi.")
                                        delay(500)
                                        onNavigateBack()
                                    }
                                } else {
                                    usernameError =
                                        "Güncelleme başarısız. Kullanıcı adı sistemde mevcut olabilir."
                                    isSaving = false
                                }
                            }

                        } else if (!isEditMode) {
                            coroutineScope.launch {
                                val newPersonnel = Personnel(
                                    fullName = fullName.trim(),
                                    username = username.trim(),
                                    email = email.trim(),
                                    password = password.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    role = role.trim(),
                                    isActive = isActive,
                                    gender = gender
                                )

                                val result =
                                    viewModel.addPersonnelWithFirebase(newPersonnel, context)

                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("Personel başarıyla eklendi.")
                                    delay(500)
                                    onNavigateBack()
                                } else {
                                    val errorMessage = result.exceptionOrNull()?.message
                                        ?: "Kayıt sırasında bir hata oluştu."

                                    if (errorMessage.contains("kullanıcı adı", ignoreCase = true)) {
                                        usernameError = errorMessage
                                    } else {
                                        snackbarHostState.showSnackbar(errorMessage)
                                    }
                                    isSaving = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                Text(
                    text = if (isEditMode) "GÜNCELLE" else "KAYDET",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}