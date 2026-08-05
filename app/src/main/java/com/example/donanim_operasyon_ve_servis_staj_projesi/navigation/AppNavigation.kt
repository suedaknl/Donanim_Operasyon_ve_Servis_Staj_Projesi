package com.example.donanim_operasyon_ve_servis_staj_projesi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.AppDatabase
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.AdminLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.PersonnelLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.WelcomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.StandaloneUserFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.AddPersonnelScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.PersonnelListScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.splash.SplashScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Veritabanı ve Repository bağımlılıkları
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    // Personnel modülü için ortak ViewModel oluşturulması
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }
    val personnelFactory = remember { PersonnelViewModelFactory(personnelRepository) }

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("welcome") {
            WelcomeScreen(
                onAdminClick = { navController.navigate("admin_login") },
                onPersonnelClick = { navController.navigate("personnel_login") }
            )
        }

        composable("admin_login") {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("personnel_login") {
            PersonnelLoginScreen(
                onLoginSuccess = {
                    navController.navigate("user_form") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // --- ADMIN HOME EKRANI ---
        composable("home") {
            HomeScreen(
                onNavigateToPersonnel = {
                    navController.navigate("personnel_list")
                },
                onLogOut = {
                    navController.navigate("welcome") {
                        popUpTo(0)
                    }
                }
            )
        }

        // --- YENİ EKLENEN PERSONEL YÖNETİMİ ROUTE'LARI ---
        composable("personnel_list") {
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            PersonnelListScreen(
                viewModel = personnelViewModel,
                onNavigateToAddPersonnel = {
                    navController.navigate("add_personnel")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("add_personnel") {
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            AddPersonnelScreen(
                viewModel = personnelViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- PERSONEL STANDALONE EKRANI ---
        composable("user_form") {
            StandaloneUserFormScreen(
                onLogOut = {
                    navController.navigate("welcome") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}