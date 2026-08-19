package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMapScreen(
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToServiceDetail: (Int) -> Unit
) {
    val services by serviceViewModel.serviceRecords.collectAsState()
    val personnelLocations by personnelViewModel.personnelLocations.collectAsState()
    val personnelList by personnelViewModel.personnelList.collectAsState() // İsimler için

    var selectedFilter by remember { mutableStateOf("Tümü") }
    var selectedMarkerData by remember { mutableStateOf<Any?>(null) } // ServiceRecord veya Personnel Map
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        personnelViewModel.refreshPersonnelLocations()
    }

    // --- FİLTRELEME ---
    val validServices = services.filter { it.latitude != null && it.longitude != null && it.status != ServiceStatus.IPTAL && it.status != ServiceStatus.TAMAMLANDI }
    val missingLocationCount = services.count { (it.status == ServiceStatus.BEKLIYOR || it.status == ServiceStatus.YOLDA || it.status == ServiceStatus.ISLEME_BASLANDI) && it.latitude == null }
    val validPersonnel = personnelLocations.filter { it["currentLatitude"] != null && it["currentLongitude"] != null }

    val displayServices = if (selectedFilter == "Personeller") emptyList() else validServices
    val displayPersonnel = if (selectedFilter == "İş Emirleri") emptyList() else validPersonnel

    // --- KAMERA BOUNDS (Dinamik ve Akıllı) ---
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }

    if (mapLoaded) {
        LaunchedEffect(displayServices, displayPersonnel) { // Hem işler hem personeller dinlenir
            val boundsBuilder = LatLngBounds.Builder()
            var hasPoints = false

            // 1. Tüm aktif iş emirlerini sınırlara ekle
            displayServices.forEach { s ->
                if (s.latitude != null && s.longitude != null) {
                    boundsBuilder.include(LatLng(s.latitude, s.longitude))
                    hasPoints = true
                }
            }

            // 2. Tüm personelleri sınırlara ekle (Afrika'dakiler hariç, sadece Türkiye/Çorum içindekiler)
            displayPersonnel.forEach { p ->
                val pLat = p["currentLatitude"] as? Double
                val pLon = p["currentLongitude"] as? Double
                if (pLat != null && pLon != null && pLat > 35.0 && pLat < 43.0) { // Sadece Türkiye sınırları içindekiler
                    boundsBuilder.include(LatLng(pLat, pLon))
                    hasPoints = true
                }
            }

            if (hasPoints) {
                try {
                    val bounds = boundsBuilder.build()
                    // Harita tüm noktaları içine alacak şekilde yumuşak bir animasyonla odaklanır ve kenarlardan 150dp pay bırakır
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } catch (e: Exception) {
                    // Tek nokta kalırsa güvenli zoom
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(40.5501, 34.9530), 12f))
                }
            } else {
                // Hiçbir geçerli nokta yoksa Çorum Merkez fallback
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(40.5501, 34.9530), 12f))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saha Haritası", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { personnelViewModel.refreshPersonnelLocations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Filtreler
            ScrollableTabRow(
                selectedTabIndex = listOf("Tümü", "İş Emirleri", "Personeller").indexOf(selectedFilter),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                listOf("Tümü", "İş Emirleri", "Personeller").forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter; selectedMarkerData = null },
                        text = { Text(filter, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (missingLocationCount > 0) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text("Dikkat: Koordinatı eksik $missingLocationCount aktif iş emri haritada gösterilemiyor.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { mapLoaded = true },
                onMapClick = { selectedMarkerData = null },
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                // İş Emirleri Markerları (Kırmızı)
                displayServices.forEach { service ->
                    Marker(
                        state = MarkerState(position = LatLng(service.latitude!!, service.longitude!!)),
                        title = "#${service.id} - ${service.companyName}",
                        snippet = service.status,
                        onClick = { selectedMarkerData = service; true },
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                // Personel Markerları (Mavi)
                displayPersonnel.forEach { p ->
                    Marker(
                        state = MarkerState(position = LatLng(p["currentLatitude"] as Double, p["currentLongitude"] as Double)),
                        title = p["fullName"] as? String ?: "Personel",
                        snippet = LocationHelper.getLocationFreshness(p["lastLocationUpdate"] as? Long),
                        onClick = { selectedMarkerData = p; true },
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
        }
    }

    // --- MARKER DETAY BOTTOM SHEET ---
    if (selectedMarkerData != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMarkerData = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 24.dp)) {
                if (selectedMarkerData is ServiceRecord) {
                    val service = selectedMarkerData as ServiceRecord
                    val assignedName = personnelList.find { it.id == service.assignedPersonnelId }?.fullName ?: "Atanmadı"

                    Text("İş Emri #${service.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(service.companyName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Cihaz: ${service.deviceType}", style = MaterialTheme.typography.bodyMedium)
                    Text("Durum: ${service.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Text("Atanan: $assignedName", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Yakındaki Personeller Algoritması
                    Text("Yakındaki Personeller", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        val sortedPersonnel = personnelLocations.map { p ->
                            val pLat = p["currentLatitude"] as? Double
                            val pLon = p["currentLongitude"] as? Double
                            val dist = if (pLat != null && pLon != null) {
                                LocationHelper.calculateDistanceInMetres(service.latitude!!, service.longitude!!, pLat, pLon)
                            } else null
                            Pair(p, dist)
                        }.sortedBy { it.second ?: Float.MAX_VALUE }

                        items(sortedPersonnel) { (p, dist) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p["fullName"] as? String ?: "", fontWeight = FontWeight.Bold)
                                    val distStr = if (dist != null) LocationHelper.formatDistance(dist) else "Konum bilgisi yok"
                                    val freshness = LocationHelper.getLocationFreshness(p["lastLocationUpdate"] as? Long)
                                    Text("$distStr • $freshness", style = MaterialTheme.typography.labelSmall, color = if (freshness.contains("Eski")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val pId = p["id"] as Int
                                val pUid = p["firebaseUid"] as String
                                if (service.assignedPersonnelId != pId) {
                                    Button(onClick = {
                                        serviceViewModel.reassignService(service.id, pId, pUid)
                                        coroutineScope.launch { sheetState.hide(); selectedMarkerData = null }
                                    }, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(36.dp)) {
                                        Text(if (service.assignedPersonnelId == null) "Ata" else "Yeniden Ata")
                                    }
                                } else {
                                    Text("Atanmış", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        coroutineScope.launch { sheetState.hide() }
                        onNavigateToServiceDetail(service.id)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("İş Detayına Git")
                    }

                } else if (selectedMarkerData is Map<*, *>) {
                    val p = selectedMarkerData as Map<String, Any>
                    val activeJob = validServices.find { it.assignedPersonnelId == p["id"] as Int && (it.status == ServiceStatus.YOLDA || it.status == ServiceStatus.ISLEME_BASLANDI) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(p["fullName"] as? String ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(LocationHelper.getLocationFreshness(p["lastLocationUpdate"] as? Long), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mevcut Durum", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (activeJob != null) {
                                Text("Aktif İş: #${activeJob.id} - ${activeJob.companyName}", style = MaterialTheme.typography.bodyMedium)
                                Text("Durum: ${activeJob.status}", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text("Şu an aktif bir görevi bulunmuyor.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}