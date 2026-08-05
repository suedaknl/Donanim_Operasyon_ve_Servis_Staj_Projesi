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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonnelScreen(
    viewModel: PersonnelViewModel,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf("") }

    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }

    var role by remember { mutableStateOf("") }
    var roleError by remember { mutableStateOf("") }

    var isActive by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Yeni Personel Ekle", fontWeight = FontWeight.Bold) },
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
                value = fullName, onValueChange = { fullName = it; fullNameError = "" },
                label = { Text("Ad Soyad") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = fullNameError.isNotEmpty(), supportingText = { if (fullNameError.isNotEmpty()) Text(fullNameError, color = MaterialTheme.colorScheme.error) }
            )

            OutlinedTextField(
                value = username, onValueChange = { username = it; usernameError = "" },
                label = { Text("Kullanıcı Adı") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = usernameError.isNotEmpty(), supportingText = { if (usernameError.isNotEmpty()) Text(usernameError, color = MaterialTheme.colorScheme.error) }
            )

            OutlinedTextField(
                value = password, onValueChange = { password = it; passwordError = "" },
                label = { Text("Şifre") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError.isNotEmpty(), supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = MaterialTheme.colorScheme.error) }
            )

            OutlinedTextField(
                value = phoneNumber, onValueChange = { phoneNumber = it; phoneError = "" },
                label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = phoneError.isNotEmpty(), supportingText = { if (phoneError.isNotEmpty()) Text(phoneError, color = MaterialTheme.colorScheme.error) }
            )

            OutlinedTextField(
                value = role, onValueChange = { role = it; roleError = "" },
                label = { Text("Görev (Örn: Saha Personeli)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = roleError.isNotEmpty(), supportingText = { if (roleError.isNotEmpty()) Text(roleError, color = MaterialTheme.colorScheme.error) }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                    if (fullName.isBlank()) { fullNameError = "Bu alan zorunludur."; isValid = false }
                    if (username.isBlank()) { usernameError = "Bu alan zorunludur."; isValid = false }
                    if (password.isBlank()) { passwordError = "Bu alan zorunludur."; isValid = false }
                    if (phoneNumber.isBlank()) { phoneError = "Bu alan zorunludur."; isValid = false }
                    if (role.isBlank()) { roleError = "Bu alan zorunludur."; isValid = false }

                    if (isValid) {
                        isSaving = true
                        coroutineScope.launch {
                            val success = viewModel.addPersonnel(
                                Personnel(
                                    fullName = fullName.trim(),
                                    username = username.trim(),
                                    password = password.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    role = role.trim(),
                                    isActive = isActive
                                )
                            )

                            if (success) {
                                snackbarHostState.showSnackbar("Personel başarıyla eklendi.")
                                delay(500)
                                onNavigateBack()
                            } else {
                                usernameError = "Bu kullanıcı adı zaten kullanılmaktadır."
                                isSaving = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                Text("Kaydet", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}