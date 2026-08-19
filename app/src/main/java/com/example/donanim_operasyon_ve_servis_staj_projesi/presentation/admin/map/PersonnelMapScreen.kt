package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun PersonnelMapScreen(
    viewModel: ServiceViewModel = viewModel(),
    currentPersonnelId: Int? = null,
    currentPersonnelUid: String? = null
) {
    val personnelServices by viewModel.filteredPersonnelServiceRecords.collectAsState(initial = emptyList())

    // Personel sekme filtresi state'i ("Tümü", "Bitmeyen", "Tamamlanan", "Bekleyen", "Yolda", "İşlemde")
    var selectedPersonnelTab by remember { mutableStateOf("Tümü") }

    LaunchedEffect(currentPersonnelId) {
        if (currentPersonnelId != null) {
            viewModel.loadRecordsForPersonnel(currentPersonnelId)
        }
    }
    LaunchedEffect(currentPersonnelUid) {
        if (!currentPersonnelUid.isNullOrEmpty()) {
            viewModel.setCurrentPersonnelUid(currentPersonnelUid)
        }
    }

    // ViewModel'deki sekme filtresini güncelliyoruz
    LaunchedEffect(selectedPersonnelTab) {
        val mappedTab = when (selectedPersonnelTab) {
            "Bitmeyen" -> "İşlemde" // veya ViewModel'in beklediği aktif durum
            else -> selectedPersonnelTab
        }
        viewModel.updateSelectedTab(mappedTab)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.5501, 34.9530), 12f)
    }

    Scaffold(
        topBar = {
            // --- PERSONEL İÇİN "TAMAMLANAN" / "BİTMEYEN" FİLTRELEME SEKMELERİ ---
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("Tümü", "Aktif İşler", "Bekleyen", "Yolda", "İşlemde", "Tamamlananlar")
                    tabs.forEach { tabName ->
                        FilterChip(
                            selected = selectedPersonnelTab == tabName,
                            onClick = { selectedPersonnelTab = tabName },
                            label = { Text(tabName, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState
        ) {
            // Seçilen filtreye (Tamamlanan / Bitmeyen vb.) göre haritada pinler filtrelenir
            personnelServices.forEach { service ->
                if (service.latitude != null && service.longitude != null) {
                    Marker(
                        state = MarkerState(position = LatLng(service.latitude!!, service.longitude!!)),
                        title = service.companyName,
                        snippet = "Durum: ${service.status} - #${service.id}"
                    )
                }
            }
        }
    }
}