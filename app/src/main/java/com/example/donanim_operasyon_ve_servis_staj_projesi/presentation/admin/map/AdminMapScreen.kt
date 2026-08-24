package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.LocationHelper
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMapScreen(
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToServiceDetail: (Int) -> Unit
) {
    val allServices by serviceViewModel.serviceRecords.collectAsState()
    val allPersonnel by personnelViewModel.personnelList.collectAsState()

    // --- ANA SEKMELER STATE (0: Aktif İşler, 1: Tamamlanan, 2: Personeller) ---
    var selectedMapTab by remember { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // --- AKTİF İŞLER FİLTRE STATELERİ ---
    var afStatus by remember { mutableStateOf("Tümü") }
    var afAssignment by remember { mutableStateOf("Tümü") }
    var afPriority by remember { mutableStateOf("Tümü") }
    var afPersonnel by remember { mutableStateOf("Tümü") }
    var afCompany by remember { mutableStateOf("Tümü") }
    var afDevice by remember { mutableStateOf("Tümü") }
    var afLocation by remember { mutableStateOf("Tümü") }

    // --- TAMAMLANAN FİLTRE STATELERİ ---
    var cfPersonnel by remember { mutableStateOf("Tümü") }
    var cfCompany by remember { mutableStateOf("Tümü") }
    var cfDevice by remember { mutableStateOf("Tümü") }
    var cfLocation by remember { mutableStateOf("Tümü") }
    var cfPriority by remember { mutableStateOf("Tümü") }
    var cfSort by remember { mutableStateOf("En yeni") }

    // --- PERSONEL FİLTRE STATELERİ ---
    var pfStatus by remember { mutableStateOf("Tüm Personeller") }

    // --- DETAY KART STATELERİ ---
    var selectedActiveService by remember { mutableStateOf<ServiceRecord?>(null) }
    var selectedCompletedService by remember { mutableStateOf<ServiceRecord?>(null) }
    var showNearbyPersonnelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMapTab) {
        selectedActiveService = null
        selectedCompletedService = null
    }

    // --- VERİ FİLTRELEME MANTIĞI ---
    val baseActiveServices = allServices.filter {
        if (afStatus == "İptal / Reddedilen") {
            it.status == ServiceStatus.IPTAL
        } else {
            it.status in listOf(ServiceStatus.BEKLIYOR, ServiceStatus.YOLDA, ServiceStatus.ISLEME_BASLANDI, ServiceStatus.PARCA_BEKLENIYOR)
        }
    }
    val filteredActiveServices = baseActiveServices.filter { s ->
        val matchStatus = if (afStatus == "Tümü" || afStatus == "İptal / Reddedilen") true else {
            when (afStatus) {
                "Bekleyen" -> s.status == ServiceStatus.BEKLIYOR
                "Yolda" -> s.status == ServiceStatus.YOLDA
                "İşlemde" -> s.status == ServiceStatus.ISLEME_BASLANDI
                "Parça Bekleniyor" -> s.status == ServiceStatus.PARCA_BEKLENIYOR
                else -> true
            }
        }
        val matchAssignment = when (afAssignment) {
            "Atanmış" -> s.assignedPersonnelId != null
            "Atanmamış" -> s.assignedPersonnelId == null
            else -> true
        }
        val matchPriority = if (afPriority == "Tümü") true else s.priority == afPriority
        val matchPersonnel = if (afPersonnel == "Tümü") true else allPersonnel.find { it.id == s.assignedPersonnelId }?.fullName == afPersonnel
        val matchCompany = if (afCompany == "Tümü") true else s.companyName == afCompany
        val matchDevice = if (afDevice == "Tümü") true else s.deviceType == afDevice
        val matchLocation = if (afLocation == "Tümü") true else s.location == afLocation

        matchStatus && matchAssignment && matchPriority && matchPersonnel && matchCompany && matchDevice && matchLocation
    }

    val filteredCompletedServices = allServices.filter { it.status == ServiceStatus.TAMAMLANDI }.filter { s ->
        val matchPersonnel = if (cfPersonnel == "Tümü") true else allPersonnel.find { it.id == s.assignedPersonnelId }?.fullName == cfPersonnel
        val matchCompany = if (cfCompany == "Tümü") true else s.companyName == cfCompany
        val matchDevice = if (cfDevice == "Tümü") true else s.deviceType == cfDevice
        val matchLocation = if (cfLocation == "Tümü") true else s.location == cfLocation
        val matchPriority = if (cfPriority == "Tümü") true else s.priority == cfPriority

        matchPersonnel && matchCompany && matchDevice && matchLocation && matchPriority
    }.sortedWith(if (cfSort == "En eski") compareBy { it.id } else compareByDescending { it.id })

    val filteredPersonnel = allPersonnel.filter { p ->
        when (pfStatus) {
            "Aktif işi olan" -> baseActiveServices.any { it.assignedPersonnelId == p.id }
            "Aktif işi olmayan" -> baseActiveServices.none { it.assignedPersonnelId == p.id }
            else -> true
        }
    }

    val missingActiveCount = filteredActiveServices.count { it.latitude == null || it.longitude == null }
    val missingCompletedCount = filteredCompletedServices.count { it.latitude == null || it.longitude == null }

    val activeFilterCount = listOf(afStatus, afAssignment, afPriority, afPersonnel, afCompany, afDevice, afLocation).count { it != "Tümü" }
    val completedFilterCount = listOf(cfPersonnel, cfCompany, cfDevice, cfLocation, cfPriority).count { it != "Tümü" } + if(cfSort != "En yeni") 1 else 0
    val personnelFilterCount = if (pfStatus != "Tüm Personeller") 1 else 0

    val currentFilterCount = when (selectedMapTab) {
        0 -> activeFilterCount
        1 -> completedFilterCount
        else -> personnelFilterCount
    }

    val defaultLocation = LatLng(40.5499, 34.9537)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(defaultLocation, 12f) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = true)
        ) {
            when (selectedMapTab) {
                0 -> {
                    filteredActiveServices.filter { it.latitude != null && it.longitude != null }.forEach { service ->
                        val latLng = LatLng(service.latitude!!, service.longitude!!)
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "#${service.id} - ${service.companyName}",
                            snippet = "${service.status} • ${service.priority}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                            onClick = { selectedActiveService = service; true }
                        )
                    }
                }
                1 -> {
                    filteredCompletedServices.filter { it.latitude != null && it.longitude != null }.forEach { service ->
                        val latLng = LatLng(service.latitude!!, service.longitude!!)
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "#${service.id} - ${service.companyName}",
                            snippet = "Tamamlandı",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            onClick = { selectedCompletedService = service; true }
                        )
                    }
                }
                2 -> {
                    // ViewModel'den gelen canlı Firestore konum listesini dinliyoruz
                    val liveLocations by personnelViewModel.personnelLocations.collectAsState()

                    liveLocations.forEach { locData ->
                        val lat = (locData["currentLatitude"] as? Double) ?: (locData["latitude"] as? Double)
                        val lon = (locData["currentLongitude"] as? Double) ?: (locData["longitude"] as? Double)
                        val name = (locData["fullName"] as? String) ?: (locData["username"] as? String) ?: "Personel"
                        val lastUpdate = (locData["lastUpdated"] as? Long) ?: 0L
                        val pId = (locData["id"] as? Int)
                        val pUid = (locData["firebaseUid"] as? String)

                        if (lat != null && lon != null) {
                            val latLng = LatLng(lat, lon)

                            val timeStr = if (lastUpdate > 0) {
                                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastUpdate))
                            } else {
                                "Bilinmiyor"
                            }

                            // Personelin ISLEME_BASLANDI durumundaki aktif işini buluyoruz
                            val personnelActiveJob = allServices.find { s ->
                                (s.assignedPersonnelId == pId || (!pUid.isNullOrEmpty() && s.assignedPersonnelUid == pUid)) &&
                                        s.status == ServiceStatus.ISLEME_BASLANDI
                            }

                            val siteStatusSnippet = if (personnelActiveJob != null && personnelActiveJob.latitude != null && personnelActiveJob.longitude != null) {
                                val distance = LocationHelper.calculateDistanceInMetres(
                                    lat, lon,
                                    personnelActiveJob.latitude!!, personnelActiveJob.longitude!!
                                )
                                if (distance <= ServiceViewModel.SERVICE_START_RADIUS_METERS) {
                                    "İş Sahasında (${distance.toInt()}m) • Güncelleme: $timeStr"
                                } else {
                                    "İş Sahası Dışında (${LocationHelper.formatDistance(distance)}) • Güncelleme: $timeStr"
                                }
                            } else {
                                "Aktif Saha İşi Yok • Güncelleme: $timeStr"
                            }

                            Marker(
                                state = MarkerState(position = latLng),
                                title = name,
                                snippet = siteStatusSnippet,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    TabRow(
                        selectedTabIndex = selectedMapTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(selected = selectedMapTab == 0, onClick = { selectedMapTab = 0 }) {
                            Text("Aktif İşler", modifier = Modifier.padding(vertical = 12.dp), fontWeight = if(selectedMapTab==0) FontWeight.Bold else FontWeight.Normal)
                        }
                        Tab(selected = selectedMapTab == 1, onClick = { selectedMapTab = 1 }) {
                            Text("Tamamlanan", modifier = Modifier.padding(vertical = 12.dp), fontWeight = if(selectedMapTab==1) FontWeight.Bold else FontWeight.Normal)
                        }
                        Tab(selected = selectedMapTab == 2, onClick = { selectedMapTab = 2 }) {
                            Text("Personeller", modifier = Modifier.padding(vertical = 12.dp), fontWeight = if(selectedMapTab==2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (currentFilterCount > 0) "Filtre ($currentFilterCount)" else "Filtrele")
                        }

                        IconButton(
                            onClick = { serviceViewModel.syncAdminData() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            val missingCount = if (selectedMapTab == 0) missingActiveCount else if (selectedMapTab == 1) missingCompletedCount else 0
            AnimatedVisibility(visible = missingCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val label = if (selectedMapTab == 0) "aktif" else "tamamlanan"
                    Text(
                        text = "Koordinatı eksik $missingCount $label iş haritada gösterilemiyor.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        AnimatedVisibility(visible = selectedActiveService != null, modifier = Modifier.align(Alignment.BottomCenter)) {
            selectedActiveService?.let { service ->
                val assignedName = allPersonnel.find { it.id == service.assignedPersonnelId }?.fullName ?: "Atanmadı"
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(service.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedActiveService = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null) }
                        }
                        Text("${service.deviceType} • ${service.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Öncelik: ${service.priority}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        Text("Personel: $assignedName", style = MaterialTheme.typography.labelMedium)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onNavigateToServiceDetail(service.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Text("İş Detayına Git")
                            }
                            OutlinedButton(onClick = { showNearbyPersonnelDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Text("Yakın Personeller")
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = selectedCompletedService != null, modifier = Modifier.align(Alignment.BottomCenter)) {
            selectedCompletedService?.let { service ->
                val assignedName = allPersonnel.find { it.id == service.assignedPersonnelId }?.fullName ?: "Bilinmiyor"
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(service.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedCompletedService = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null) }
                        }
                        Text("Lokasyon: ${service.location}", style = MaterialTheme.typography.bodyMedium)
                        Text("Tamamlayan: $assignedName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Tarih: ${service.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { onNavigateToServiceDetail(service.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                            Text("İş Detayına Git / Kapanış İncele")
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Gelişmiş Filtreleme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        when (selectedMapTab) {
                            0 -> { afStatus="Tümü"; afAssignment="Tümü"; afPriority="Tümü"; afPersonnel="Tümü"; afCompany="Tümü"; afDevice="Tümü"; afLocation="Tümü" }
                            1 -> { cfPersonnel="Tümü"; cfCompany="Tümü"; cfDevice="Tümü"; cfLocation="Tümü"; cfPriority="Tümü"; cfSort="En yeni" }
                            2 -> { pfStatus="Tüm Personeller" }
                        }
                    }) { Text("Temizle", color = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider()

                when (selectedMapTab) {
                    0 -> {
                        MapDropdownFilter("Durum", afStatus, listOf("Tümü", "Bekleyen", "Yolda", "İşlemde", "Parça Bekleniyor", "İptal / Reddedilen")) { afStatus = it }
                        MapDropdownFilter("Atama Durumu", afAssignment, listOf("Tümü", "Atanmış", "Atanmamış")) { afAssignment = it }
                        MapDropdownFilter("Öncelik", afPriority, listOf("Tümü", "Düşük", "Orta", "Yüksek", "Acil")) { afPriority = it }
                        MapDropdownFilter("Atanan Personel", afPersonnel, listOf("Tümü") + allPersonnel.map { it.fullName }) { afPersonnel = it }
                        MapDropdownFilter("Firma", afCompany, listOf("Tümü") + allServices.map { it.companyName }.distinct()) { afCompany = it }
                        MapDropdownFilter("Cihaz Türü", afDevice, listOf("Tümü") + allServices.map { it.deviceType }.distinct()) { afDevice = it }
                        MapDropdownFilter("Lokasyon", afLocation, listOf("Tümü") + allServices.map { it.location }.distinct()) { afLocation = it }
                    }
                    1 -> {
                        MapDropdownFilter("Sıralama (Tarih)", cfSort, listOf("En yeni", "En eski")) { cfSort = it }
                        MapDropdownFilter("Tamamlayan Personel", cfPersonnel, listOf("Tümü") + allPersonnel.map { it.fullName }) { cfPersonnel = it }
                        MapDropdownFilter("Firma", cfCompany, listOf("Tümü") + allServices.map { it.companyName }.distinct()) { cfCompany = it }
                        MapDropdownFilter("Cihaz Türü", cfDevice, listOf("Tümü") + allServices.map { it.deviceType }.distinct()) { cfDevice = it }
                        MapDropdownFilter("Lokasyon", cfLocation, listOf("Tümü") + allServices.map { it.location }.distinct()) { cfLocation = it }
                        MapDropdownFilter("Öncelik", cfPriority, listOf("Tümü", "Düşük", "Orta", "Yüksek", "Acil")) { cfPriority = it }
                    }
                    2 -> {
                        MapDropdownFilter("Personel Durumu", pfStatus, listOf("Tüm Personeller", "Aktif işi olan", "Aktif işi olmayan")) { pfStatus = it }
                    }
                }

                Button(onClick = { showFilterSheet = false }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Uygula", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }

    if (showNearbyPersonnelDialog && selectedActiveService != null) {
        val s = selectedActiveService!!
        val nearbyList = emptyList<Pair<Personnel, Float>>()

        AlertDialog(
            onDismissRequest = { showNearbyPersonnelDialog = false },
            title = { Text("Yakındaki Personeller", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (nearbyList.isEmpty()) {
                        item { Text("Bu projede personel anlık harita koordinatları yerel modelde tutulmadığı için bu liste boş döndü.", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNearbyPersonnelDialog = false }) { Text("Kapat") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDropdownFilter(label: String, selectedValue: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onValueChange(opt); expanded = false }
                )
            }
        }
    }
}