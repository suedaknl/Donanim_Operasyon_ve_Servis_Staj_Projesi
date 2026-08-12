package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import kotlinx.coroutines.launch

@Composable
fun PersonnelLoginScreen(
    viewModel: PersonnelViewModel, // ViewModel parametresi
    onLoginSuccess: (Int) -> Unit, // Başarılı girişte Personnel ID dışarı aktarılıyor
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var username by remember { mutableStateOf(sessionManager.getLastUsername()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Hata State'leri
    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    LoginForm(
        title = "Servis Personeli Girişi",
        username = username,
        password = password,
        rememberMe = rememberMe,
        usernameLabel = "Personel No veya Kullanıcı Adı",
        loginButtonText = "Giriş Yap",
        isPasswordVisible = passwordVisible,
        usernameError = usernameError,
        passwordError = passwordError,
        generalError = generalError,
        onUsernameChange = {
            username = it
            usernameError = ""
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
                coroutineScope.launch {
                    // Mevcut PersonnelViewModel'daki personel listesini alıp eşleşen kullanıcıyı buluyoruz
                    val matchedPersonnel = viewModel.getPersonnelByUsername(username.trim())

                    if (matchedPersonnel != null && matchedPersonnel.password == password.trim()) {
                        if (matchedPersonnel.isActive) {
                            // YENİ EKLENEN KISIM: Beni Hatırla tercihi, Personel Rolü ve Personel ID saklanıyor
                            sessionManager.saveSession(
                                isRememberMe = rememberMe,
                                role = "PERSONNEL",
                                personnelId = matchedPersonnel.id
                            )
                            sessionManager.saveLastUsername(username)
                            onLoginSuccess(matchedPersonnel.id) // Doğru ID route'a aktarılıyor
                        } else {
                            generalError = "Hesabınız pasif durumdadır."
                        }
                    } else {
                        generalError = "Kullanıcı adı veya şifre hatalı."
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}