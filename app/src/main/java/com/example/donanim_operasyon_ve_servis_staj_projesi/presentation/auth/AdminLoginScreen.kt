package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun AdminLoginScreen(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Hata State'leri
    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }

    LoginForm(
        title = "Admin Girişi",
        username = username,
        password = password,
        rememberMe = rememberMe,
        usernameLabel = "Kullanıcı Adı",
        loginButtonText = "Giriş Yap",
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
            var isValid = true

            if (username.isBlank()) {
                usernameError = "Kullanıcı adı zorunludur."
                isValid = false
            }
            if (password.isBlank()) {
                passwordError = "Şifre zorunludur."
                isValid = false
            }

            if (isValid) {
                // Şimdilik sabit admin doğrulaması
                if (username == "admin" && password == "1234") {
                    onLoginSuccess()
                } else {
                    generalError = "Kullanıcı adı veya şifre hatalı."
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}