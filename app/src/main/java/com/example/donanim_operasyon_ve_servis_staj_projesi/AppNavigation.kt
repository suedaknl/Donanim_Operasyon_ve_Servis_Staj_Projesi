package com.example.donanim_operasyon_ve_servis_staj_projesi

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login.LoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
// import kısımlarını Android Studio'nun otomatik yapmasına izin verebilirsin (Alt + Enter)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            // İŞTE BURAYI DÜZELTTİK: onNavigateToHome yerine onLoginSuccess yazdık
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        // Giriş yaptıktan sonra geri tuşuna basınca tekrar login'e dönmesin diye
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen()
        }
    }
}