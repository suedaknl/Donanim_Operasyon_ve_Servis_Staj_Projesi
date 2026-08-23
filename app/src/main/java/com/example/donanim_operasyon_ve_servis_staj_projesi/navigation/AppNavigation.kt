package com.example.donanim_operasyon_ve_servis_staj_projesi.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.AdminLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.PersonnelLoginScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.auth.WelcomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.AddServiceScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.ServiceDetailScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.ServiceHistoryScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.AddPersonnelScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.PersonnelWelcomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.PersonnelMainScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.splash.SplashScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.camera.CameraScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.ClosingFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.AdminMainScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.detail.PersonnelDetailScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.LocationPickerScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.profile.EditProfileScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.leave.AdminLeaveScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.leave.PersonnelLeaveScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.shift.AdminShiftScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.LeaveViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.OvertimeViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.ShiftViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.shift.PersonnelShiftScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.overtime.PersonnelOvertimeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.overtime.AdminOvertimeScreen

@Composable
fun AppNavigation(
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val sharedServiceViewModel: ServiceViewModel = hiltViewModel()
    val sessionManager = remember { SessionManager(context) }
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(
                authRepository = authRepository,
                onSplashFinished = { destination ->
                    navController.navigate(destination) {
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
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("personnel_login") {
            val personnelViewModel: PersonnelViewModel = hiltViewModel()
            PersonnelLoginScreen(
                authRepository = authRepository,
                viewModel = personnelViewModel,
                onLoginSuccess = { personnelId ->
                    navController.navigate("personnel_welcome/$personnelId") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "personnel_welcome/{personnelId}",
            arguments = listOf(navArgument("personnelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: 0
            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            PersonnelWelcomeScreen(
                personnelId = personnelId,
                personnelViewModel = personnelViewModel,
                serviceViewModel = sharedServiceViewModel,
                onNavigateToHome = {
                    navController.navigate("personnel_main/$personnelId") {
                        popUpTo("personnel_welcome/$personnelId") { inclusive = true }
                    }
                }
            )
        }

        composable("closing_form/{serviceId}/{personnelId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0
            val personnelId = backStackEntry.arguments?.getString("personnelId")?.toIntOrNull() ?: 0

            val returnedPhotoUri by backStackEntry.savedStateHandle.getStateFlow<String?>(
                "photo_uri",
                null
            ).collectAsState()

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
            val personnelViewModel: PersonnelViewModel = hiltViewModel()
            AdminMainScreen(
                serviceViewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                adminEmail = authRepository.getCurrentUser()?.email ?: "Sistem Yöneticisi",
                onNavigateToAddService = { navController.navigate(route = "add_service") },
                onServiceClick = { clickedService ->
                    navController.navigate(route = "service_detail/${clickedService.id}")
                },
                onNavigateToAddPersonnel = { navController.navigate(route = "add_personnel") },
                onNavigateToEditPersonnel = { id ->
                    navController.navigate(route = "add_personnel?personnelId=$id")
                },
                onNavigateToPersonnelDetail = { id ->
                    navController.navigate(route = "personnel_detail/$id")
                },
                onNavigateToPersonnel = {},
                onNavigateToShift = {
                    navController.navigate("admin_shift")
                },
                onNavigateToLeave = {
                    navController.navigate("admin_leave")
                },
                onNavigateToOvertime = {
                    navController.navigate("admin_overtime")
                },
                onLogOut = {
                    authRepository.signOut()
                    sessionManager.clearSession()
                    navController.navigate(route = "welcome") {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(route = "admin_overtime") {
            val overtimeViewModel: OvertimeViewModel = hiltViewModel()
            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            AdminOvertimeScreen(
                overtimeViewModel = overtimeViewModel,
                personnelViewModel = personnelViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("personnel_service_detail/{serviceId}/{personnelId}") { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0
            val pId = backStackEntry.arguments?.getString("personnelId")?.toIntOrNull()
            val returnedPhotoUri by backStackEntry.savedStateHandle.getStateFlow<String?>(
                "photo_uri",
                null
            ).collectAsState()
            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                serviceId = serviceId,
                personnelId = pId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("add_service?serviceId=$id") },
                onCreateExtraJob = { sourceId -> navController.navigate("add_service?sourceServiceId=$sourceId") },
                onNavigateToHistory = { firestoreId, sId, companyName ->
                    val encodedCompany = android.net.Uri.encode(companyName)
                    navController.navigate("service_history/$firestoreId/$sId/$encodedCompany")
                },
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

        composable(route = "personnel_detail/{personnelId}") { backStackEntry ->
            val personnelId = backStackEntry.arguments?.getString("personnelId")?.toIntOrNull()
            val personnelViewModel: PersonnelViewModel = hiltViewModel()
            val personnelList by personnelViewModel.personnelList.collectAsState()
            val personnel = personnelList.find { it.id == personnelId }

            if (personnel != null) {
                PersonnelDetailScreen(
                    personnel = personnel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate("add_personnel?personnelId=$id") },
                    onDeletePersonnel = { targetPersonnel ->
                        personnelViewModel.deletePersonnel(targetPersonnel)
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(
            route = "add_service?serviceId={serviceId}&sourceServiceId={sourceServiceId}",
            arguments = listOf(
                navArgument("serviceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("sourceServiceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val serviceIdStr = backStackEntry.arguments?.getString("serviceId")
            val actualServiceId = serviceIdStr?.toIntOrNull()

            val sourceServiceIdStr = backStackEntry.arguments?.getString("sourceServiceId")
            val actualSourceServiceId = sourceServiceIdStr?.toIntOrNull()

            val selectedLat = backStackEntry.savedStateHandle.get<Double>("selected_lat")
            val selectedLon = backStackEntry.savedStateHandle.get<Double>("selected_lon")

            AddServiceScreen(
                viewModel = sharedServiceViewModel,
                serviceId = actualServiceId,
                sourceServiceId = actualSourceServiceId,
                returnedLatitude = selectedLat,
                returnedLongitude = selectedLon,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLocationPicker = { currentLat, currentLon ->
                    val latArg = currentLat ?: 0.0
                    val lonArg = currentLon ?: 0.0
                    val hasArg = if (currentLat != null && currentLon != null) "true" else "false"
                    navController.navigate("location_picker?lat=$latArg&lon=$lonArg&hasLoc=$hasArg")
                },
                onLocationConsumed = {
                    backStackEntry.savedStateHandle.remove<Double>("selected_lat")
                    backStackEntry.savedStateHandle.remove<Double>("selected_lon")
                }
            )
        }

        composable(
            route = "location_picker?lat={lat}&lon={lon}&hasLoc={hasLoc}",
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("lon") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("hasLoc") { type = NavType.StringType; defaultValue = "false" }
            )
        ) { backStackEntry ->
            val hasLoc = backStackEntry.arguments?.getString("hasLoc") == "true"
            val lat = if (hasLoc) backStackEntry.arguments?.getFloat("lat")?.toDouble() else null
            val lon = if (hasLoc) backStackEntry.arguments?.getFloat("lon")?.toDouble() else null

            LocationPickerScreen(
                initialLat = lat,
                initialLon = lon,
                onLocationSelected = { latitude, longitude ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_lat",
                        latitude
                    )
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_lon",
                        longitude
                    )
                },
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
            val personnelViewModel: PersonnelViewModel = hiltViewModel()
            val personnelIdStr = backStackEntry.arguments?.getString("personnelId")
            val actualId = personnelIdStr?.toIntOrNull()

            AddPersonnelScreen(
                viewModel = personnelViewModel,
                personnelId = actualId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_profile/{personnelId}",
            arguments = listOf(navArgument("personnelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: 0
            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            EditProfileScreen(
                personnelId = personnelId,
                viewModel = personnelViewModel,
                onNavigateBack = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_tab", 3)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "personnel_main/{personnelId}",
            arguments = listOf(navArgument("personnelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: 0
            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            val initialTab = backStackEntry.savedStateHandle.get<Int>("selected_tab") ?: 0

            val returnedPhotoUri by backStackEntry.savedStateHandle.getStateFlow<String?>(
                "photo_uri",
                null
            ).collectAsState()
            val photoServiceId = backStackEntry.savedStateHandle.get<Int>("photo_service_id")

            LaunchedEffect(returnedPhotoUri) {
                val uri = returnedPhotoUri

                if (uri != null && photoServiceId != null) {
                    sharedServiceViewModel.addServicePhoto(
                        ServicePhoto(
                            serviceRecordId = photoServiceId,
                            personnelId = personnelId,
                            photoUri = uri,
                            localUri = uri,
                            photoType = "GENEL",
                            photoCategory = "GENEL",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    Toast.makeText(context, "Fotoğraf başarıyla eklendi.", Toast.LENGTH_SHORT)
                        .show()

                    backStackEntry.savedStateHandle.remove<String>("photo_uri")
                    backStackEntry.savedStateHandle.remove<Int>("photo_service_id")
                }
            }

            PersonnelMainScreen(
                personnelId = personnelId,
                initialTab = initialTab,
                serviceViewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,

                onNavigateToServiceDetail = { serviceId ->
                    navController.navigate("personnel_service_detail/$serviceId/$personnelId")
                },

                onNavigateToEditPersonnel = { id ->
                    navController.navigate("edit_profile/$id")
                },

                onNavigateToShift = {
                    navController.navigate("personnel_shift/$personnelId")
                },

                onNavigateToLeave = {
                    navController.navigate("personnel_leave/$personnelId")
                },

                onNavigateToOvertime = {
                    navController.navigate("personnel_overtime/$personnelId")
                },

                onLogOut = {
                    authRepository.signOut()
                    sessionManager.clearSession()
                    navController.navigate("welcome") {
                        popUpTo(0)
                    }
                },

                onNavigateToCameraForService = { serviceId, _ ->
                    backStackEntry.savedStateHandle["photo_service_id"] = serviceId
                    navController.navigate("camera")
                }
            )
        }

        composable("camera") {
            CameraScreen(
                onPhotoCaptured = { uri ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "photo_uri",
                        uri
                    )
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable("service_detail/{serviceId}") { backStackEntry ->
            val serviceId =
                backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0

            val returnedPhotoUri =
                backStackEntry.savedStateHandle.get<String>("photo_uri")

            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            ServiceDetailScreen(
                viewModel = sharedServiceViewModel,
                personnelViewModel = personnelViewModel,
                serviceId = serviceId,
                personnelId = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { id ->
                    navController.navigate("add_service?serviceId=$id")
                },
                onCreateExtraJob = { sourceId ->
                    navController.navigate("add_service?sourceServiceId=$sourceId")
                },
                onNavigateToHistory = { firestoreId, sId, companyName ->
                    val encodedCompany = android.net.Uri.encode(companyName)

                    navController.navigate(
                        "service_history/$firestoreId/$sId/$encodedCompany"
                    )
                },
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

        composable("admin_shift") {
            val shiftViewModel: ShiftViewModel = hiltViewModel()
            val personnelViewModel: PersonnelViewModel = hiltViewModel()

            AdminShiftScreen(
                shiftViewModel = shiftViewModel,
                personnelViewModel = personnelViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "personnel_shift/{personnelId}",
            arguments = listOf(
                navArgument("personnelId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val personnelId =
                backStackEntry.arguments?.getInt("personnelId") ?: 0

            val shiftViewModel: ShiftViewModel = hiltViewModel()

            PersonnelShiftScreen(
                personnelId = personnelId,
                shiftViewModel = shiftViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("admin_leave") {
            val leaveViewModel: LeaveViewModel = hiltViewModel()

            AdminLeaveScreen(
                leaveViewModel = leaveViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "personnel_leave/{personnelId}",
            arguments = listOf(
                navArgument("personnelId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val personnelId =
                backStackEntry.arguments?.getInt("personnelId") ?: 0

            val leaveViewModel: LeaveViewModel = hiltViewModel()

            PersonnelLeaveScreen(
                personnelId = personnelId,
                leaveViewModel = leaveViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "personnel_overtime/{personnelId}",
            arguments = listOf(navArgument("personnelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val personnelId = backStackEntry.arguments?.getInt("personnelId") ?: 0
            val overtimeViewModel: OvertimeViewModel = hiltViewModel()

            PersonnelOvertimeScreen(
                personnelId = personnelId,
                overtimeViewModel = overtimeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            "service_history/{firestoreId}/{serviceId}/{companyName}"
        ) { backStackEntry ->

            val firestoreId =
                backStackEntry.arguments?.getString("firestoreId") ?: ""

            val serviceId =
                backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0

            val companyName =
                backStackEntry.arguments?.getString("companyName") ?: ""

            ServiceHistoryScreen(
                viewModel = sharedServiceViewModel,
                firestoreId = firestoreId,
                serviceId = serviceId,
                companyName = companyName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}