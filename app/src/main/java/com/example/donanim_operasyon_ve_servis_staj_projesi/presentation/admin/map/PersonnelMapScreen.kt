package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel // Gerekli import
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map.PersonnelMapScreen

@Composable
fun PersonnelMapScreen(
    viewModel: ServiceViewModel = viewModel() // Parametreyi opsiyonel yapıyoruz
) {
    val allServices by viewModel.serviceRecords.collectAsState(initial = emptyList())

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.5501, 34.9530), 10f)
    }

    Scaffold { paddingValues ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState
        ) {
            allServices.forEach { service ->
                if (service.latitude != null && service.longitude != null) {
                    Marker(
                        state = MarkerState(position = LatLng(service.latitude!!, service.longitude!!)),
                        title = service.companyName,
                        snippet = service.issueDescription
                    )
                }
            }
        }
    }
}