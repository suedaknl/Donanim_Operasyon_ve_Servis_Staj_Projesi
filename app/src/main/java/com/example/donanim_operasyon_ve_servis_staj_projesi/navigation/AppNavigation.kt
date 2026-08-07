package com.example.donanim_operasyon_ve_servis_staj_projesi.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.AppDatabase
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.AdminLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.PersonnelLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.WelcomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.AddServiceScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.ServiceDetailScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.StandaloneUserFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.AddPersonnelScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.PersonnelListScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.splash.SplashScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.repository.PersonnelRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModelFactory
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModelFactory
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Veritabanı ve Repository bağımlılıkları
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    // Personnel modülü için ortak ViewModel oluşturulması
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }
    val personnelFactory = remember { PersonnelViewModelFactory(personnelRepository) }

    // Service modülü için Repository ve Factory oluşturulması
    val serviceRepository = remember { ServiceRepository(database.serviceDao()) }
    val serviceFactory = remember { ServiceViewModelFactory(serviceRepository) }

    // --- MİMARİ DÜZELTME: SHARED VIEWMODEL ---
    val sharedServiceViewModel: ServiceViewModel = viewModel(factory = serviceFactory)

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
            val serviceList by sharedServiceViewModel.serviceRecords.collectAsState()

            HomeScreen(
                serviceList = serviceList,
                searchQuery = sharedServiceViewModel.searchQuery,
                onSearchQueryChange = { sharedServiceViewModel.updateSearchQuery(it) },
                selectedFilter = sharedServiceViewModel.selectedFilter,
                onFilterSelected = { filterValue ->
                    sharedServiceViewModel.updateSelectedFilter(filterValue ?: "")
                },
                onNavigateToPersonnel = {
                    navController.navigate("personnel_list")
                },
                onNavigateToAddService = {
                    navController.navigate("add_service")
                },
                onServiceClick = { service ->
                    navController.navigate("service_detail/${service.id}")
                },
                onLogOut = {
                    navController.navigate("welcome") {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(
            route = "service_detail/{serviceId}",
            arguments = listOf(navArgument("serviceId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: return@composable

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                serviceId = serviceId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { id ->
                    navController.navigate("add_service?serviceId=$id")
                }
            )
        }


        // --- PERSONEL YÖNETİMİ ROUTE'LARI ---
        composable("personnel_list") {
            // DÜZELTME: Factory eklendi, artık çökme olmayacak
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            PersonnelListScreen(
                viewModel = personnelViewModel,
                onNavigateToAddPersonnel = { navController.navigate("add_personnel") },
                onNavigateToEditPersonnel = { id -> navController.navigate("add_personnel?personnelId=$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- İŞ EMRİ EKLEME VE DÜZENLEME ROTASI ---
        composable(
            route = "add_service?serviceId={serviceId}",
            arguments = listOf(
                navArgument("serviceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val serviceIdStr = backStackEntry.arguments?.getString("serviceId")
            val actualServiceId = serviceIdStr?.toIntOrNull()

            AddServiceScreen(
                viewModel = sharedServiceViewModel,
                // DÜZELTME: -1 yerine doğrudan actualServiceId veriyoruz (null ise null gider)
                serviceId = actualServiceId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Çökmeleri önlemek için güvenli String tabanlı argüman tanımı (Ekleme ve Düzenleme için Tek Rota)
        composable(
            route = "add_personnel?personnelId={personnelId}",
            arguments = listOf(
                navArgument("personnelId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)
            val personnelIdStr = backStackEntry.arguments?.getString("personnelId")
            val actualId = personnelIdStr?.toIntOrNull()

            AddPersonnelScreen(
                viewModel = personnelViewModel,
                personnelId = actualId,
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