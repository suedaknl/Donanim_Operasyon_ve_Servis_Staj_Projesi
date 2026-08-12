package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel

@Composable
fun PersonnelLoginScreen(
    viewModel: PersonnelViewModel,
    onLoginSuccess: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf(sessionManager.getLastUsername()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }

    // Şifremi Unuttum State'leri
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isResetSuccess by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LoginForm(
            title = "Servis Personeli Girişi",
            username = email,
            password = password,
            rememberMe = rememberMe,
            usernameLabel = "E-posta Adresi",
            loginButtonText = if (isLoading) "Giriş Yapılıyor..." else "Giriş Yap",
            isPasswordVisible = passwordVisible,
            usernameError = emailError,
            passwordError = passwordError,
            generalError = generalError,
            onUsernameChange = {
                email = it
                emailError = ""
                generalError = ""
            },
            onPasswordChange = {
                password = it
                passwordError = ""
                generalError = ""
            },
            onRememberMeChange = { rememberMe = it },
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
            onLoginClick = {
                if (!isLoading) {
                    var isValid = true
                    val trimmedEmail = email.trim()

                    if (trimmedEmail.isBlank()) {
                        emailError = "E-posta adresi zorunludur."
                        isValid = false
                    }
                    if (password.isBlank()) {
                        passwordError = "Şifre zorunludur."
                        isValid = false
                    }

                    if (isValid) {
                        isLoading = true
                        generalError = ""

                        coroutineScope.launch {
                            // 1. Firebase Auth Doğrulaması (Şifre KESİNLİKLE burada doğrulanıyor)
                            val authResult = authRepository.signInWithEmailAndPassword(trimmedEmail, password)

                            authResult.fold(
                                onSuccess = { firebaseUser ->
                                    // 2. Firebase başarılı. Şimdi Room ile eşleştirme zamanı:
                                    val syncResult = viewModel.syncPersonnelWithFirebase(trimmedEmail, firebaseUser.uid)
                                    isLoading = false

                                    syncResult.fold(
                                        onSuccess = { matchedPersonnel ->
                                            // 3. Eşleşme başarılı. Oturumu kaydet ve içeri al.
                                            sessionManager.saveSession(
                                                isRememberMe = rememberMe,
                                                role = "PERSONNEL",
                                                personnelId = matchedPersonnel.id
                                            )
                                            sessionManager.saveLastUsername(trimmedEmail)
                                            onLoginSuccess(matchedPersonnel.id)
                                        },
                                        onFailure = { syncError ->
                                            // Eşleşme yoksa veya hesap pasifse, Firebase oturumunu güvenle kapat ve hata göster.
                                            authRepository.signOut()
                                            generalError = syncError.message ?: "Personel kaydı bulunamadı."
                                        }
                                    )
                                },
                                onFailure = {
                                    isLoading = false
                                    generalError = "Giriş başarısız: E-posta veya şifre hatalı."
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Şifremi Unuttum Butonu
        TextButton(
            onClick = {
                resetEmail = email
                resetMessage = null
                isResetSuccess = false
                showResetDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Şifremi Unuttum?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }

    // Şifremi Unuttum Dialogu (Admin ile birebir aynı)
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!resetLoading) showResetDialog = false },
            title = { Text("Şifremi Unuttum", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Kayıtlı e-posta adresinizi girin. Size bir şifre sıfırlama bağlantısı göndereceğiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it; resetMessage = null },
                        label = { Text("E-posta Adresi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !resetLoading
                    )
                    resetMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = if (isResetSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailInput = resetEmail.trim()
                        if (emailInput.isBlank()) {
                            isResetSuccess = false; resetMessage = "Lütfen e-posta adresinizi girin."; return@Button
                        }
                        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                            isResetSuccess = false; resetMessage = "Geçerli bir e-posta adresi girin."; return@Button
                        }

                        resetLoading = true; resetMessage = null
                        coroutineScope.launch {
                            val result = authRepository.sendPasswordResetEmail(emailInput)
                            resetLoading = false
                            result.fold(
                                onSuccess = { isResetSuccess = true; resetMessage = "Şifre sıfırlama bağlantısı gönderildi." },
                                onFailure = { isResetSuccess = false; resetMessage = "İşlem başarısız veya e-posta kayıtlı değil." }
                            )
                        }
                    },
                    enabled = !resetLoading
                ) { Text(if (resetLoading) "Gönderiliyor..." else "Sıfırlama Linki Gönder") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }, enabled = !resetLoading) { Text("İptal") } }
        )
    }
}