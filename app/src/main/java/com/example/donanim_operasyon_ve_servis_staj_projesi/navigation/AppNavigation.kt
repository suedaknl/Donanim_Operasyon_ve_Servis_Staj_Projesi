package com.example.donanim_operasyon_ve_servis_staj_projesi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login.LoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login.StandaloneUserFormScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            // LoginScreen artık iki farklı yönlendirme bekliyor
            LoginScreen(
                onAdminLogin = {
                    navController.navigate("admin_home") {
                        // Admin giriş yapınca Login ekranını geçmişten siliyoruz
                        popUpTo("login") { inclusive = true }
                    }
                },
                onUserLogin = {
                    navController.navigate("user_form") {
                        // Personel form ekranına geçince Login ekranını geçmişten siliyoruz
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 1. Yol: Adminlerin Yönlendirildiği Sayfa (Dünkü ekranımız)
        composable("admin_home") {
            HomeScreen()
        }

        // 2. Yol: Saha Personelinin Yönlendirildiği Sayfa (Tam Ekran Form)
        composable("user_form") {
            StandaloneUserFormScreen(
                onLogOut = {
                    // Personel işini bitirip çıkış yapmak isterse login ekranına geri döner
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}