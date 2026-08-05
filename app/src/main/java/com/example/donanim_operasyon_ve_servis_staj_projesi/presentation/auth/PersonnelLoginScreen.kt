package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun PersonnelLoginScreen(
    onLoginSuccess: () -> Unit
) {
    // Ekranın kendi içinde tuttuğu State'ler
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Tüm UI işlemleri ve giriş alanları ortak LoginForm'a devrediliyor
    LoginForm(
        title = "Servis Personeli Girişi",
        username = username,
        password = password,
        rememberMe = rememberMe,
        usernameLabel = "Personel No veya Kullanıcı Adı",
        loginButtonText = "Giriş Yap",
        isPasswordVisible = passwordVisible,
        onUsernameChange = { username = it },
        onPasswordChange = { password = it },
        onRememberMeChange = { rememberMe = it },
        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
        onLoginClick = {
            // İleride eklenecek doğrulama işlemleri (Validation) buraya gelebilir.
            // Şimdilik doğrudan başarılı sayıp callback'i tetikliyoruz.
            onLoginSuccess()
        },
        modifier = Modifier.fillMaxSize()
    )
}