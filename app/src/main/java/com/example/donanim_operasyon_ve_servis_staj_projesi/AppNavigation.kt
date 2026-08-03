package com.example.donanim_operasyon_ve_servis_staj_projesi

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login.LoginScreen

@Composable
fun AppNavigation() {
    // Navigasyonu yönetecek olan denetleyiciyi oluşturuyoruz
    val navController = rememberNavController()

    // NavHost, ekranların içinde değişeceği ana çerçevedir.
    // startDestination ile uygulamanın ilk nerede başlayacağını söylüyoruz: "login"
    NavHost(navController = navController, startDestination = "login") {

        // "login" rotasına gidildiğinde LoginScreen'i göster
        composable("login") {
            // LoginScreen'e, başarılı girişte yapması gereken hareketi öğretiyoruz
            LoginScreen(
                onNavigateToHome = {
                    // Giriş başarılı olursa "home" ekranına git
                    navController.navigate("home") {
                        // Geri tuşuna basıldığında tekrar giriş ekranına dönmemesi için,
                        // login ekranını geçmişten siliyoruz (pop up)
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // "home" rotasına gidildiğinde HomeScreen'i göster
        composable("home") {
            HomeScreen()
        }
    }
}