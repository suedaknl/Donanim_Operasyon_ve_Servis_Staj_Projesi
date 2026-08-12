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
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.camera.CameraScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.ClosingFormScreen
import androidx.compose.ui.platform.LocalContext
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository

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
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }


    NavHost(navController = navController, startDestination = "splash") {

        // 1. SPLASH GÜNCELLEMESİ
        composable("splash") {
            SplashScreen(
                onSplashFinished = { destination ->
                    navController.navigate(destination) {
                        // Splash ekranını backstack'ten tamamen sil
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

        composable("closing_form/{serviceId}/{personnelId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0
            val personnelId = backStackEntry.arguments?.getString("personnelId")?.toIntOrNull() ?: 0

            val returnedPhotoUri by backStackEntry.savedStateHandle.getStateFlow<String?>("photo_uri", null).collectAsState()

            ClosingFormScreen(
                viewModel = sharedServiceViewModel,
                serviceId = serviceId,
                personnelId = personnelId,
                returnedPhotoUri = returnedPhotoUri,
                onPhotoSaved = {
                    backStackEntry.savedStateHandle.remove<String>("photo_uri")
                },
                onNavigateToCamera = {
                    navController.navigate("camera")
                },
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            // Çıkış işlemi için gerekli referanslar (Eğer dosyanın en üstünde tanımladıysan bu 3 satırı silebilirsin)
            val context = androidx.compose.ui.platform.LocalContext.current
            val sessionManager = androidx.compose.runtime.remember { com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager(context) }
            val authRepository = androidx.compose.runtime.remember { com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository() }

            val serviceList by sharedServiceViewModel.filteredServiceRecords.collectAsState()

            val personnelViewModel: PersonnelViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = personnelFactory)
            val personnelList by personnelViewModel.personnelList.collectAsState()

            HomeScreen(
                serviceList = serviceList,
                personnelList = personnelList,
                selectedTab = sharedServiceViewModel.selectedTab,
                onTabSelected = { sharedServiceViewModel.updateSelectedTab(it) },
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
                    // YENİ EKLENEN KISIM: Firebase'den çıkış yap ve yerel oturum tercihlerini sil
                    authRepository.signOut()
                    sessionManager.clearSession()

                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }

        composable("personnel_service_detail/{serviceId}/{personnelId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0
            val pId = backStackEntry.arguments?.getString("personnelId")?.toIntOrNull()
            val returnedPhotoUri by backStackEntry.savedStateHandle.getStateFlow<String?>("photo_uri", null).collectAsState()
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                serviceId = serviceId,
                personnelId = pId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("add_service?serviceId=$id") },
                returnedPhotoUri = returnedPhotoUri,
                onPhotoSaved = {
                    backStackEntry.savedStateHandle.remove<String>("photo_uri")
                },
                onNavigateToCamera = {
                    navController.navigate("camera")
                },
                onNavigateToClosingForm = { sId, personnel ->
                    navController.navigate("closing_form/$sId/$personnel")
                }
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

        composable(
            route = "user_form/{personnelId}",
            arguments = listOf(androidx.navigation.navArgument("personnelId") { type = androidx.navigation.NavType.IntType })
        ) { backStackEntry ->
            // Çıkış işlemi için gerekli referanslar (Eğer dosyanın en üstünde tanımladıysan bu 3 satırı silebilirsin)
            val context = androidx.compose.ui.platform.LocalContext.current
            val sessionManager = androidx.compose.runtime.remember { com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager(context) }
            val authRepository = androidx.compose.runtime.remember { com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository() }

            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: 0

            StandaloneUserFormScreen(
                viewModel = sharedServiceViewModel,
                personnelId = personnelId,
                onServiceClick = { serviceId ->
                    navController.navigate("personnel_service_detail/$serviceId/$personnelId")
                },
                onLogOut = {
                    // YENİ EKLENEN KISIM: Firebase'den çıkış yap ve yerel oturum tercihlerini sil
                    authRepository.signOut()
                    sessionManager.clearSession()

                    navController.navigate("welcome") { popUpTo(0) }
                }
            )
        }

        composable("camera") {
            CameraScreen(
                onPhotoCaptured = { uri ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("photo_uri", uri)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable("service_detail/{serviceId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0
            val returnedPhotoUri = backStackEntry.savedStateHandle.get<String>("photo_uri")
            val personnelViewModel: PersonnelViewModel = viewModel(factory = personnelFactory)

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                serviceId = serviceId,
                personnelId = null,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("add_service?serviceId=$id") },
                returnedPhotoUri = returnedPhotoUri,
                onPhotoSaved = {
                    backStackEntry.savedStateHandle.remove<String>("photo_uri")
                },
                onNavigateToCamera = {
                    navController.navigate("camera")
                },
                onNavigateToClosingForm = { _, _ ->
                }
            )
        }
    }
}