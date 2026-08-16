package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel

import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.StandaloneUserFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import kotlin.math.roundToInt

@Composable
fun PersonnelMainScreen(
    personnelId: Int,
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToServiceDetail: (Int) -> Unit,
    onLogOut: () -> Unit,
    onNavigateToCameraForService: (Int, String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // En temiz ve hatasız Sürüklenebilir FAB State'leri
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }

    // Dialog State'leri
    var showNoteDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    val personnelList by personnelViewModel.personnelList.collectAsState()
    val currentPersonnel = personnelList.find { it.id == personnelId }
    val currentFirebaseUid = currentPersonnel?.firebaseUid

    LaunchedEffect(currentFirebaseUid) {
        if (!currentFirebaseUid.isNullOrEmpty()) {
            serviceViewModel.setCurrentPersonnelUid(currentFirebaseUid)
        }
    }

    val filteredPersonnelServices by serviceViewModel.filteredPersonnelServiceRecords.collectAsState()

    val allPersonnelRawServices by serviceViewModel.personnelServiceRecords.collectAsState()
    val activeServicesForFab = remember(allPersonnelRawServices, personnelId) {
        allPersonnelRawServices.filter {
            it.assignedPersonnelId == personnelId &&
                    (it.status == ServiceStatus.BEKLIYOR ||
                            it.status == ServiceStatus.YOLDA ||
                            it.status == ServiceStatus.ISLEME_BASLANDI ||
                            it.status == ServiceStatus.PARCA_BEKLENIYOR)
        }
    }

    LaunchedEffect(personnelId, currentPersonnel) {
        currentPersonnel?.firebaseUid?.let { uid ->
            serviceViewModel.syncMyServices(uid, personnelId)
        }
        serviceViewModel.loadRecordsForPersonnel(personnelId)
    }

    Scaffold(
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
                                    // Scaffold BottomEnd slotunda olduğumuz için pozitif değerler ekranın sağına ve altına kaçmaktır.
                                    // Ekrandan çıkmasını engellemek için basit bir yaslama yapıyoruz.
                                    if (fabOffsetX > 0f) fabOffsetX = 0f
                                    if (fabOffsetY > 0f) fabOffsetY = 0f
                                }
                            )
                        }
                ) {
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false },
                        modifier = Modifier.width(220.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("İş Emirlerine Git", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = { showFabMenu = false; selectedTab = 1 }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hızlı Not Ekle", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                if (activeServicesForFab.isEmpty()) {
                                    Toast.makeText(context, "İşlem yapabileceğiniz aktif iş emriniz bulunmuyor.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showNoteDialog = true
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Fotoğraf Çek", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                if (activeServicesForFab.isEmpty()) {
                                    Toast.makeText(context, "İşlem yapabileceğiniz aktif iş emriniz bulunmuyor.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showPhotoDialog = true
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("AI Asistanı (Yakında)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary) },
                            onClick = {
                                showFabMenu = false
                                Toast.makeText(context, "Yapay zeka asistanı yakında hizmetinizde olacak.", Toast.LENGTH_SHORT).show()
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
                    myServices = filteredPersonnelServices,
                    onNavigateToServiceDetail = onNavigateToServiceDetail,
                    onGoToAssignments = { selectedTab = 1 }
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
                            firebaseUid = currentFirebaseUid,
                            localPersonnelId = personnelId
                        )
                    }
                }
                2 -> PersonnelMapPlaceholder()
                3 -> PersonnelProfileContent(personnel = currentPersonnel, onLogOut = onLogOut)
            }
        }
    }

    // Hızlı Not Ekleme Dialogu
    if (showNoteDialog) {
        var selectedServiceForNote by remember { mutableStateOf<ServiceRecord?>(null) }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Hızlı Not Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedServiceForNote == null) {
                        Text("Lütfen not eklenecek iş emrini seçin:", style = MaterialTheme.typography.bodyMedium)
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
                                        Text("${s.deviceType} (${s.status})", style = MaterialTheme.typography.bodySmall)
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

    // Fotoğraf Çekme Dialogu
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Fotoğraf Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lütfen fotoğraf eklenecek iş emrini seçin:", style = MaterialTheme.typography.bodyMedium)
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
                                    Text("${s.deviceType} (${s.status})", style = MaterialTheme.typography.bodySmall)
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
    myServices: List<ServiceRecord>,
    onNavigateToServiceDetail: (Int) -> Unit,
    onGoToAssignments: () -> Unit
) {
    val assignedCount = myServices.count { it.status == ServiceStatus.BEKLIYOR }
    val acceptedCount = myServices.count { it.status == ServiceStatus.YOLDA }
    val inProgressCount = myServices.count { it.status == ServiceStatus.ISLEME_BASLANDI || it.status == ServiceStatus.PARCA_BEKLENIYOR }
    val completedCount = myServices.count { it.status == ServiceStatus.TAMAMLANDI }

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
                IconButton(onClick = { /* Bildirimler */ }) { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Görev Özeti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onGoToAssignments) { Text("Tümünü Gör") }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard(modifier = Modifier.weight(1f), title = "Atanan", count = assignedCount, color = MaterialTheme.colorScheme.error)
                    SummaryCard(modifier = Modifier.weight(1f), title = "Yolda", count = acceptedCount, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard(modifier = Modifier.weight(1f), title = "İşlemde", count = inProgressCount, color = MaterialTheme.colorScheme.tertiary)
                    SummaryCard(modifier = Modifier.weight(1f), title = "Tamamlanan", count = completedCount, color = Color(0xFF4CAF50))
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

@Composable
fun PersonnelMapPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Map, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Harita Modülü", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("İş konumları ve rota bilgileri yakında\nbu ekranda görüntülenecektir.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PersonnelProfileContent(personnel: Personnel?, onLogOut: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(personnel?.fullName ?: "Personel Adı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(personnel?.role ?: "Saha Personeli", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                ProfileMenuItem(Icons.Default.Email, personnel?.email ?: "E-posta belirtilmedi")
                HorizontalDivider()
                ProfileMenuItem(Icons.Default.Phone, personnel?.phoneNumber ?: "Telefon belirtilmedi")
                HorizontalDivider()
                ProfileMenuItem(Icons.Default.Wc, "Cinsiyet: ${personnel?.gender ?: "Belirtilmedi"}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                ProfileMenuItem(Icons.Default.Lock, "Şifre Değiştir", true) { Toast.makeText(context, "Şifre değiştirme ekranı hazırlanıyor.", Toast.LENGTH_SHORT).show() }
                HorizontalDivider()
                ProfileMenuItem(Icons.Default.HelpOutline, "Destek ve Yardım", true) { Toast.makeText(context, "Destek talebi özelliği yakında eklenecektir.", Toast.LENGTH_SHORT).show() }
                HorizontalDivider()
                ProfileMenuItem(Icons.Default.Edit, "Profil Bilgilerini Güncelle", true) { Toast.makeText(context, "Bilgi güncelleme talebiniz yöneticiye iletildi.", Toast.LENGTH_SHORT).show() }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onLogOut, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Logout, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Çıkış Yap", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, isAction: Boolean = false, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = isAction, onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (isAction) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}