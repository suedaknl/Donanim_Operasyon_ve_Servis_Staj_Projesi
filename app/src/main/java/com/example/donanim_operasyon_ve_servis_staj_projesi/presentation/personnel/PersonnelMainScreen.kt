package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.*
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map.PersonnelMapScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.profile.PersonnelProfileScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.StandaloneUserFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.service.location.ActiveJobLocationService
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.LocationHelper
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelMainScreen(
    personnelId: Int,
    initialTab: Int = 0,
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToServiceDetail: (Int) -> Unit,
    onNavigateToEditPersonnel: (Int) -> Unit,
    onLogOut: () -> Unit,
    onNavigateToShift: () -> Unit,
    onNavigateToLeave: () -> Unit,
    onNavigateToOvertime: () -> Unit,
    onNavigateToCameraForService: (Int, String) -> Unit = { _, _ -> }
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }

    var showNoteDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    val personnelList by personnelViewModel.personnelList.collectAsState()
    val currentPersonnel = personnelList.find { it.id == personnelId }
    val currentPersonnelUid = currentPersonnel?.firebaseUid

    LaunchedEffect(currentPersonnelUid) {
        if (!currentPersonnelUid.isNullOrEmpty()) {
            serviceViewModel.setCurrentPersonnelUid(currentPersonnelUid)
        }
    }

    val filteredPersonnelServices by serviceViewModel.filteredPersonnelServiceRecords.collectAsState()
    val allPersonnelRawServices by serviceViewModel.personnelServiceRecords.collectAsState()

    // Aktif saha işi kontrolü: Sadece ISLEME_BASLANDI durumundaki iş Foreground Service'i tetikler
    val activeService = remember(allPersonnelRawServices, personnelId) {
        allPersonnelRawServices.find {
            it.assignedPersonnelId == personnelId && it.status == ServiceStatus.ISLEME_BASLANDI
        }
    }

    val activeServicesForFab = remember(allPersonnelRawServices, personnelId) {
        allPersonnelRawServices.filter {
            it.assignedPersonnelId == personnelId &&
                    (it.status == ServiceStatus.ISLEME_BASLANDI ||
                            it.status == ServiceStatus.PARCA_BEKLENIYOR)
        }
    }

    val locationStatus by personnelViewModel.locationStatus.collectAsState()

    var permissionGranted by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionGranted = LocationHelper.hasLocationPermission(context)
        if (!permissionGranted) {
            personnelViewModel.updateLocationStatus("Konum izni gerekli")
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Aktif iş (ISLEME_BASLANDI) durumuna göre Foreground Service'i başlatma veya durdurma
    LaunchedEffect(activeService, permissionGranted) {
        if (permissionGranted) {
            val serviceIntent = Intent(context, ActiveJobLocationService::class.java)
            if (activeService != null && !currentPersonnelUid.isNullOrEmpty()) {
                serviceIntent.action = ActiveJobLocationService.ACTION_START
                serviceIntent.putExtra(ActiveJobLocationService.EXTRA_PERSONNEL_UID, currentPersonnelUid)
                serviceIntent.putExtra(ActiveJobLocationService.EXTRA_SERVICE_ID, activeService.id)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                personnelViewModel.updateLocationStatus("Konum: Saha Takibinde")
            } else {
                serviceIntent.action = ActiveJobLocationService.ACTION_STOP
                context.startService(serviceIntent)
                personnelViewModel.updateLocationStatus("Konum: Beklemede (Aktif İş Yok)")
            }
        }
    }

    // Bileşen kapatıldığında servisi durdurma emniyeti
    DisposableEffect(Unit) {
        onDispose {
            // İhtiyaç halinde ek temizlik yapılabilir
        }
    }

    LaunchedEffect(personnelId, currentPersonnel) {
        currentPersonnel?.firebaseUid?.let { uid ->
            serviceViewModel.syncMyServices(uid, personnelId)
        }
        serviceViewModel.loadRecordsForPersonnel(personnelId)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Personel Menüsü",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Vardiyam") },
                    selected = false,
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToShift()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("İzinlerim") },
                    selected = false,
                    icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToLeave()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Fazla Mesailerim") },
                    selected = false,
                    icon = { Icon(Icons.Default.MoreTime, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToOvertime()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    label = { Text("Destek ve Yardım") },
                    selected = false,
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Destek ekibine yönlendiriliyorsunuz.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Çıkış Yap") },
                    selected = false,
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogOut()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Saha Operasyon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = locationStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (locationStatus.contains("Saha")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, null) }, label = { Text("Ana Sayfa") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(if (selectedTab == 1) Icons.Filled.Assignment else Icons.Outlined.Assignment, null) }, label = { Text("İş Emirleri") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(if (selectedTab == 2) Icons.Filled.Map else Icons.Outlined.Map, null) }, label = { Text("Harita") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, null) }, label = { Text("Profil") })
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    var showFabMenu by remember { mutableStateOf(false) }

                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .offset {
                                IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt())
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        fabOffsetX += dragAmount.x
                                        fabOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        if (fabOffsetX > 0f) fabOffsetX = 0f
                                        if (fabOffsetY > 0f) fabOffsetY = 0f
                                    }
                                )
                            }
                    ) {
                        DropdownMenu(
                            expanded = showFabMenu,
                            onDismissRequest = { showFabMenu = false },
                            modifier = Modifier.width(260.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Hızlı Not Ekle", fontWeight = FontWeight.Medium, color = if (activeServicesForFab.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                                        if (activeServicesForFab.isEmpty()) {
                                            Text("İşleme başladıktan sonra kullanılabilir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.EditNote, null, tint = if (activeServicesForFab.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showFabMenu = false
                                    if (activeServicesForFab.isEmpty()) {
                                        Toast.makeText(context, "Not eklemek için önce bir iş emrinde işleme başlamalısınız.", Toast.LENGTH_LONG).show()
                                    } else {
                                        showNoteDialog = true
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Fotoğraf Çek", fontWeight = FontWeight.Medium, color = if (activeServicesForFab.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                                        if (activeServicesForFab.isEmpty()) {
                                            Text("İşleme başladıktan sonra kullanılabilir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, null, tint = if (activeServicesForFab.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showFabMenu = false
                                    if (activeServicesForFab.isEmpty()) {
                                        Toast.makeText(context, "Fotoğraf eklemek için önce bir iş emrinde işleme başlamalısınız.", Toast.LENGTH_LONG).show()
                                    } else {
                                        showPhotoDialog = true
                                    }
                                }
                            )
                        }

                        FloatingActionButton(
                            onClick = { showFabMenu = !showFabMenu },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(16.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Hızlı İşlem Menüsü",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTab) {
                    0 -> PersonnelHomeContent(
                        personnel = currentPersonnel,
                        rawServices = allPersonnelRawServices,
                        myServices = filteredPersonnelServices,
                        onNavigateToServiceDetail = onNavigateToServiceDetail,
                        onGoToAssignments = { filter ->
                            serviceViewModel.updateSearchQuery("")
                            serviceViewModel.updateSelectedFilter("Hepsi")
                            serviceViewModel.updateSelectedPriorityFilter("Hepsi")

                            when (filter) {
                                "Atanan" -> serviceViewModel.updateSelectedTab("Atanan")
                                "Yolda" -> serviceViewModel.updateSelectedTab("Yolda")
                                "İşlemde" -> serviceViewModel.updateSelectedTab("İşlemde")
                                "Tamamlanan" -> serviceViewModel.updateSelectedTab("Tamamlanan")
                                else -> serviceViewModel.updateSelectedTab("Tümü")
                            }
                            selectedTab = 1
                        }
                    )
                    1 -> {
                        Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                            StandaloneUserFormScreen(
                                serviceList = filteredPersonnelServices,
                                personnelList = personnelList,
                                selectedTab = serviceViewModel.selectedTab,
                                onTabSelected = { serviceViewModel.updateSelectedTab(it) },
                                searchQuery = serviceViewModel.searchQuery,
                                onSearchQueryChange = { serviceViewModel.updateSearchQuery(it) },
                                selectedFilter = serviceViewModel.selectedFilter,
                                onFilterSelected = { serviceViewModel.updateSelectedFilter(it) },
                                selectedPriority = serviceViewModel.selectedPriorityFilter,
                                onPrioritySelected = { serviceViewModel.updateSelectedPriorityFilter(it) },
                                onClearFilters = {
                                    serviceViewModel.updateSearchQuery("")
                                    serviceViewModel.updateSelectedFilter("Hepsi")
                                    serviceViewModel.updateSelectedPriorityFilter("Hepsi")
                                    serviceViewModel.updateSelectedTab("Tümü")
                                },
                                onNavigateToPersonnel = { },
                                onNavigateToAddService = { },
                                onServiceClick = { service -> onNavigateToServiceDetail(service.id) },
                                onLogOut = onLogOut,
                                serviceViewModel = serviceViewModel,
                                firebaseUid = currentPersonnelUid,
                                localPersonnelId = personnelId
                            )
                        }
                    }
                    2 -> PersonnelMapScreen(viewModel = serviceViewModel)
                    3 -> PersonnelProfileScreen(
                        personnel = currentPersonnel,
                        onEditProfile = { id -> onNavigateToEditPersonnel(id) },
                        onLogOut = onLogOut
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
        var selectedServiceForNote by remember { mutableStateOf<ServiceRecord?>(null) }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Hızlı Not Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedServiceForNote == null) {
                        Text("Lütfen not eklenecek aktif işlemi seçin:", style = MaterialTheme.typography.bodyMedium)
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(activeServicesForFab) { s ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedServiceForNote = s },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("#${s.id} - ${s.companyName}", fontWeight = FontWeight.Bold)
                                        Text("${s.deviceType} (${s.status})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Seçilen İş: #${selectedServiceForNote!!.id} - ${selectedServiceForNote!!.companyName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Notunuzu girin") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                if (selectedServiceForNote != null) {
                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                serviceViewModel.addServiceNote(
                                    ServiceNote(
                                        serviceRecordId = selectedServiceForNote!!.id,
                                        personnelId = personnelId,
                                        note = noteText.trim(),
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                Toast.makeText(context, "Not başarıyla eklendi.", Toast.LENGTH_SHORT).show()
                                showNoteDialog = false
                            }
                        },
                        enabled = noteText.isNotBlank()
                    ) { Text("Kaydet") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (selectedServiceForNote != null) {
                        selectedServiceForNote = null
                    } else {
                        showNoteDialog = false
                    }
                }) { Text(if (selectedServiceForNote != null) "Geri" else "İptal") }
            }
        )
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Fotoğraf Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lütfen fotoğraf eklenecek aktif işlemi seçin:", style = MaterialTheme.typography.bodyMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(activeServicesForFab) { s ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        showPhotoDialog = false
                                        onNavigateToCameraForService(s.id, "GENEL")
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("#${s.id} - ${s.companyName}", fontWeight = FontWeight.Bold)
                                    Text("${s.deviceType} (${s.status})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
fun PersonnelHomeContent(
    personnel: Personnel?,
    rawServices: List<ServiceRecord>,
    myServices: List<ServiceRecord>,
    onNavigateToServiceDetail: (Int) -> Unit,
    onGoToAssignments: (String) -> Unit
) {
    val assignedCount = rawServices.count { it.status == ServiceStatus.BEKLIYOR }
    val acceptedCount = rawServices.count { it.status == ServiceStatus.YOLDA }
    val inProgressCount = rawServices.count { it.status == ServiceStatus.ISLEME_BASLANDI || it.status == ServiceStatus.PARCA_BEKLENIYOR }
    val completedCount = rawServices.count { it.status == ServiceStatus.TAMAMLANDI }

    val activeServices = myServices.filter {
        it.status != ServiceStatus.TAMAMLANDI && it.status != ServiceStatus.IPTAL
    }.sortedByDescending { it.id }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(personnel?.fullName ?: "Personel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(personnel?.role ?: "Saha Personeli", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mesai İçinde", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                IconButton(onClick = { }) { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Görev Özeti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onGoToAssignments("Tümü") }) { Text("Tümünü Gör") }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard(
                        modifier = Modifier.weight(1f).clickable { onGoToAssignments("Atanan") },
                        title = "Atanan",
                        count = assignedCount,
                        color = MaterialTheme.colorScheme.error
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f).clickable { onGoToAssignments("Yolda") },
                        title = "Yolda",
                        count = acceptedCount,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard(
                        modifier = Modifier.weight(1f).clickable { onGoToAssignments("İşlemde") },
                        title = "İşlemde",
                        count = inProgressCount,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f).clickable { onGoToAssignments("Tamamlanan") },
                        title = "Tamamlanan",
                        count = completedCount,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Güncel İşler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (activeServices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Şu an bekleyen aktif bir işiniz bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(activeServices) { service ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onNavigateToServiceDetail(service.id) }, shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(service.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(service.priority, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${service.deviceType} - ${service.deviceModel}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(service.date.take(10), style = MaterialTheme.typography.labelSmall)
                                }
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(service.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, count: Int, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}