package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLat: Double?,
    initialLon: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Çorum merkez varsayılan fallback (Operasyon bölgesi)
    val defaultLat = initialLat ?: 40.5501
    val defaultLon = initialLon ?: 34.9530

    var selectedPosition by remember { mutableStateOf(LatLng(defaultLat, defaultLon)) }
    var hasPin by remember { mutableStateOf(initialLat != null && initialLon != null) }

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(LatLng(defaultLat, defaultLon), 13f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Haritadan Konum Seç", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasPin) {
                        Text(
                            text = String.format("Seçilen Koordinat: %.4f, %.4f", selectedPosition.latitude, selectedPosition.longitude),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Lütfen harita üzerinde bir noktaya dokunun.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            if (hasPin) {
                                onLocationSelected(selectedPosition.latitude, selectedPosition.longitude)
                                onNavigateBack()
                            }
                        },
                        enabled = hasPin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("Bu Konumu Kullan")
                    }
                }
            }
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier.padding(padding).fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                selectedPosition = latLng
                hasPin = true
            }
        ) {
            if (hasPin) {
                Marker(
                    state = MarkerState(position = selectedPosition),
                    title = "Seçilen İş Konumu"
                )
            }
        }
    }
}