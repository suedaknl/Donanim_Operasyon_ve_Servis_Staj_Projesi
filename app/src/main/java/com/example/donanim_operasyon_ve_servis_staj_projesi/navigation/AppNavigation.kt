package com.example.donanim_operasyon_ve_servis_staj_projesi.navigation // Kendi ana paket ismine göre düzenleyebilirsin

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.AdminLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.PersonnelLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.WelcomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login.StandaloneUserFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        // 1. SPLASH EKRANI
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("welcome") {
                        // Splash bittikten sonra geri dönülememesi için backstack'ten sil
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // 2. WELCOME (ROL SEÇİM) EKRANI
        composable("welcome") {
            WelcomeScreen(
                onAdminClick = {
                    navController.navigate("admin_login")
                },
                onPersonnelClick = {
                    navController.navigate("personnel_login")
                }
            )
        }

        // 3. ADMIN LOGIN EKRANI
        composable("admin_login") {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        // Giriş başarılı olunca Welcome ve Login ekranlarını geçmişten tamamen temizle
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // 4. PERSONEL LOGIN EKRANI
        composable("personnel_login") {
            PersonnelLoginScreen(
                onLoginSuccess = {
                    navController.navigate("user_form") {
                        // Giriş başarılı olunca Welcome ve Login ekranlarını geçmişten tamamen temizle
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // 5. ADMIN ANA EKRANI (HomeScreen)
        composable("home") {
            HomeScreen(
                // Not: Eğer HomeScreen içinde bir onLogOut callback'i tanımlıysa burayı aktif edebilirsin.
                // Çıkış yapıldığında tüm session/backstack geçmişini sıfırlayıp Welcome ekranına atar.
                /*
                onLogOut = {
                    navController.navigate("welcome") {
                        popUpTo(0) // "0" tüm geçmişi temizler
                    }
                }
                */
            )
        }

        // 6. PERSONEL FORM EKRANI (StandaloneUserFormScreen)
        composable("user_form") {
            StandaloneUserFormScreen(
                onLogOut = {
                    navController.navigate("welcome") {
                        // Çıkış yapıldığında tüm geçmişi sıfırlayıp Welcome ekranına döndür
                        popUpTo(0)
                    }
                }
            )
        }
    }
}