package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    // Kullanıcının girdiği verileri tutan state'ler
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var passwordVisible by mutableStateOf(false)
        private set

    // Metin değiştikçe çağrılacak fonksiyonlar
    fun onUsernameChanged(newValue: String) {
        username = newValue
    }

    fun onPasswordChanged(newValue: String) {
        password = newValue
    }

    fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
    }

    // Giriş yap butonuna basıldığında çalışacak mantık (Şimdilik sahte kontrol)
    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            onError("Kullanıcı adı ve şifre boş bırakılamaz!")
            return
        }

        // Sahte bir servis kontrolü (İleride buraya gerçek API bağlanacak)
        if (username == "admin" && password == "123456") {
            onSuccess()
        } else {
            onError("Hatalı kullanıcı adı veya şifre!")
        }
    }
}