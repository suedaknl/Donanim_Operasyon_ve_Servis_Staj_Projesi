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
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }
    val personnelFactory = remember { PersonnelViewModelFactory(personnelRepository) }

    val serviceRepository = remember { ServiceRepository(database.serviceDao()) }
    val serviceFactory = remember { ServiceViewModelFactory(serviceRepository) }

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
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)
            PersonnelLoginScreen(
                viewModel = personnelViewModel,
                onLoginSuccess = { personnelId ->
                    navController.navigate("user_form/$personnelId") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // AppNavigation.kt içindeki composable("home") bloğunu bununla değiştirin:

        composable("home") {
            val serviceList by sharedServiceViewModel.filteredServiceRecords.collectAsState()

            // YENİ: Personel isimlerini bulabilmek için listeyi Home'a aktarıyoruz
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)
            val personnelList by personnelViewModel.personnelList.collectAsState()

            HomeScreen(
                serviceList = serviceList,
                personnelList = personnelList, // Yeni Parametre
                selectedTab = sharedServiceViewModel.selectedTab, // Yeni Parametre
                onTabSelected = { sharedServiceViewModel.updateSelectedTab(it) }, // Yeni Parametre
                searchQuery = sharedServiceViewModel.searchQuery,
                onSearchQueryChange = { sharedServiceViewModel.updateSearchQuery(it) },
                selectedFilter = sharedServiceViewModel.selectedFilter,
                onFilterSelected = { filterValue ->
                    sharedServiceViewModel.updateSelectedFilter(filterValue ?: "Hepsi")
                },
                selectedPriority = sharedServiceViewModel.selectedPriorityFilter,
                onPrioritySelected = { priorityValue ->
                    sharedServiceViewModel.updateSelectedPriorityFilter(priorityValue)
                },
                onClearFilters = {
                    sharedServiceViewModel.updateSearchQuery("")
                    sharedServiceViewModel.updateSelectedFilter("Hepsi")
                    sharedServiceViewModel.updateSelectedPriorityFilter("Hepsi")
                },
                onNavigateToPersonnel = { navController.navigate("personnel_list") },
                onNavigateToAddService = { navController.navigate("add_service") },
                onServiceClick = { service -> navController.navigate("service_detail/${service.id}") },
                onLogOut = {
                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }

        // Admin Detay Rotası
        composable(
            route = "service_detail/{serviceId}",
            arguments = listOf(navArgument("serviceId") { type = NavType.IntType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: return@composable
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                serviceId = serviceId,
                personnelId = null, // Admin modunda personel ID null geçer
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("add_service?serviceId=$id") }
            )
        }

        composable("personnel_list") {
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            PersonnelListScreen(
                viewModel = personnelViewModel,
                serviceViewModel = sharedServiceViewModel,
                onNavigateToAddPersonnel = { navController.navigate("add_personnel") },
                onNavigateToEditPersonnel = { id -> navController.navigate("add_personnel?personnelId=$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

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
                serviceId = actualServiceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Personel Ana Ekranı Rotası
        composable(
            route = "user_form/{personnelId}",
            arguments = listOf(navArgument("personnelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: 0

            StandaloneUserFormScreen(
                viewModel = sharedServiceViewModel,
                personnelId = personnelId,
                onServiceClick = { serviceId ->
                    // YENİ: Personel kartına basınca güvenli personel detay rotasına yönlendiriliyor
                    navController.navigate("personnel_service_detail/$serviceId/$personnelId")
                },
                onLogOut = {
                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }

        // YENİ: Personel Güvenli Detay Ekranı Rotası
        composable(
            route = "personnel_service_detail/{serviceId}/{personnelId}",
            arguments = listOf(
                navArgument("serviceId") { type = NavType.IntType },
                navArgument("personnelId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: return@composable
            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: return@composable
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                serviceId = serviceId,
                personnelId = personnelId, // Personel ID gönderilerek güvenlik kontrolü tetiklenir
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { /* Personel düzenleme yapamaz */ }
            )
        }
    }
}