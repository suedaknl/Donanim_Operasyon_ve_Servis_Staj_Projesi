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

@Composable
fun AdminLoginScreen(
    onLoginSuccess: () -> Unit
) {
    // Firebase Auth Repository ve Coroutine Scope
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var username by remember { mutableStateOf(sessionManager.getLastUsername()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Yükleme durumu

    // Hata State'leri
    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }

    // Şifremi Unuttum State'leri
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isResetSuccess by remember { mutableStateOf(false) }

    // UI bileşenlerini Box içine alıyoruz ki "Şifremi Unuttum" butonunu tasarımını bozmadan alta ekleyebilelim
    Box(modifier = Modifier.fillMaxSize()) {

        LoginForm(
            title = "Admin Girişi",
            username = username,
            password = password,
            rememberMe = rememberMe,
            usernameLabel = "Kullanıcı Adı (E-posta)", // Firebase e-posta beklediği için küçük bir ipucu
            loginButtonText = if (isLoading) "Giriş Yapılıyor..." else "Giriş Yap",
            isPasswordVisible = passwordVisible,
            usernameError = usernameError,
            passwordError = passwordError,
            generalError = generalError,
            onUsernameChange = {
                username = it
                usernameError = "" // Kullanıcı yazmaya başlayınca hatayı temizle
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
                // Eğer zaten giriş yapılıyorsa, çoklu tıklamayı engelle
                if (!isLoading) {
                    var isValid = true

                    if (username.isBlank()) {
                        usernameError = "Kullanıcı adı (E-posta) zorunludur."
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
                            // AuthRepository üzerinden Firebase'e giriş isteği
                            val result = authRepository.signInWithEmailAndPassword(username, password)
                            isLoading = false

                            result.fold(
                                onSuccess = {
                                    // Başarılı girişte navigasyonu tetikle
                                    sessionManager.saveSession(isRememberMe = rememberMe, role = "ADMIN")
                                    sessionManager.saveLastUsername(username)
                                    onLoginSuccess()
                                },
                                onFailure = {
                                    // Hatalı giriş durumunda UI'ı bozmadan genel hatayı göster
                                    generalError = "Giriş başarısız: E-posta veya şifre hatalı."
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Şifremi Unuttum Butonu (Ekranın Alt Ortasına Hizalandı)
        TextButton(
            onClick = {
                resetEmail = username // Mevcut e-postayı dialoga aktar
                resetMessage = null
                isResetSuccess = false
                showResetDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Şifremi Unuttum?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // Şifremi Unuttum Dialogu
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!resetLoading) showResetDialog = false
            },
            title = { Text(text = "Şifremi Unuttum", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Kayıtlı e-posta adresinizi girin. Size bir şifre sıfırlama bağlantısı göndereceğiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetMessage = null
                        },
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
                            isResetSuccess = false
                            resetMessage = "Lütfen e-posta adresinizi girin."
                            return@Button
                        }
                        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                            isResetSuccess = false
                            resetMessage = "Geçerli bir e-posta adresi girin."
                            return@Button
                        }

                        resetLoading = true
                        resetMessage = null

                        coroutineScope.launch {
                            val result = authRepository.sendPasswordResetEmail(emailInput)
                            resetLoading = false

                            result.fold(
                                onSuccess = {
                                    isResetSuccess = true
                                    resetMessage = "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi."
                                },
                                onFailure = {
                                    isResetSuccess = false
                                    resetMessage = "İşlem başarısız. Bu e-posta sistemde kayıtlı olmayabilir veya ağ hatası oluştu."
                                }
                            )
                        }
                    },
                    enabled = !resetLoading
                ) {
                    Text(if (resetLoading) "Gönderiliyor..." else "Sıfırlama Linki Gönder")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !resetLoading
                ) {
                    Text("İptal")
                }
            }
        )
    }
}