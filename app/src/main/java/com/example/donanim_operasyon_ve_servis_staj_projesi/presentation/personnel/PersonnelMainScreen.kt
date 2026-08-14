package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form.StandaloneUserFormScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import java.util.*

@Composable
fun PersonnelMainScreen(
    personnelId: Int,
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToServiceDetail: (Int) -> Unit,
    onLogOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var personnel by remember { mutableStateOf<Personnel?>(null) }
    val context = LocalContext.current

    LaunchedEffect(personnelId) {
        personnelViewModel.getPersonnelById(personnelId) { loadedPersonnel ->
            personnel = loadedPersonnel
        }
    }

    val allServiceList by serviceViewModel.filteredServiceRecords.collectAsState()
    val personnelList by personnelViewModel.personnelList.collectAsState()

    val myServices = remember(allServiceList, personnelId) {
        allServiceList.filter { it.assignedPersonnelId == personnelId }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Ana Sayfa") },
                    label = { Text("Ana Sayfa") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.Assignment else Icons.Outlined.Assignment, contentDescription = "İş Emirleri") },
                    label = { Text("İş Emirleri") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.Map else Icons.Outlined.Map, contentDescription = "Harita") },
                    label = { Text("Harita") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profil") },
                    label = { Text("Profil") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                var showFabMenu by remember { mutableStateOf(false) }
                Box(contentAlignment = Alignment.BottomEnd) {
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false },
                        modifier = Modifier.width(220.dp) // Hata düzeltildi
                    ) {
                        DropdownMenuItem(
                            text = { Text("İş Emirlerine Git", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                selectedTab = 1
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hızlı Not Ekle", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                Toast.makeText(context, "Hızlı not ekleme açılıyor...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Fotoğraf Çek", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                Toast.makeText(context, "Kamera modülü açılıyor...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("AI Asistanı (Yakında)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
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
                    personnel = personnel,
                    myServices = myServices,
                    onNavigateToServiceDetail = onNavigateToServiceDetail,
                    onGoToAssignments = { selectedTab = 1 }
                )
                1 -> {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        StandaloneUserFormScreen(
                            serviceList = myServices,
                            personnelList = personnelList,
                            selectedTab = serviceViewModel.selectedTab,
                            onTabSelected = { serviceViewModel.updateSelectedTab(it) },
                            searchQuery = serviceViewModel.searchQuery,
                            onSearchQueryChange = { serviceViewModel.updateSearchQuery(it) },
                            selectedFilter = serviceViewModel.selectedFilter,
                            onFilterSelected = { filter -> serviceViewModel.updateSelectedFilter(filter) },
                            selectedPriority = serviceViewModel.selectedPriorityFilter,
                            onPrioritySelected = { selectedPriority ->
                                serviceViewModel.updateSelectedPriorityFilter(selectedPriority) // Hata düzeltildi
                            },
                            onClearFilters = {
                                serviceViewModel.updateSearchQuery("")
                                serviceViewModel.updateSelectedFilter("Hepsi")
                                serviceViewModel.updateSelectedPriorityFilter("Hepsi")
                            },
                            onNavigateToPersonnel = { },
                            onNavigateToAddService = { },
                            onServiceClick = { service -> onNavigateToServiceDetail(service.id) },
                            onLogOut = onLogOut,
                            serviceViewModel = serviceViewModel,
                            firebaseUid = null,
                            localPersonnelId = personnelId
                        )
                    }
                }
                2 -> PersonnelMapPlaceholder()
                3 -> PersonnelProfileContent(personnel = personnel, onLogOut = onLogOut)
            }
        }
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

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = personnel?.fullName ?: "Personel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = personnel?.role ?: "Saha Personeli",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mesai İçinde", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                IconButton(onClick = { /* Bildirimler */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Bildirimler", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Görev Özeti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onGoToAssignments) {
                        Text("Tümünü Gör")
                    }
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
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToServiceDetail(service.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = service.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = service.priority,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "${service.deviceType} - ${service.deviceModel}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = service.date.take(10), style = MaterialTheme.typography.labelSmall)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = service.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PersonnelMapPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Map, contentDescription = "Harita", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Harita Modülü",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "İş konumları ve rota bilgileri yakında\nbu ekranda görüntülenecektir.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PersonnelProfileContent(personnel: Personnel?, onLogOut: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profil", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = personnel?.fullName ?: "Personel Adı",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = personnel?.role ?: "Saha Personeli",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                ProfileMenuItem(icon = Icons.Default.Email, title = personnel?.email ?: "E-posta belirtilmedi")
                HorizontalDivider()
                ProfileMenuItem(icon = Icons.Default.Phone, title = personnel?.phoneNumber ?: "Telefon belirtilmedi")
                HorizontalDivider()
                ProfileMenuItem(icon = Icons.Default.Wc, title = "Cinsiyet: ${personnel?.gender ?: "Belirtilmedi"}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                ProfileMenuItem(icon = Icons.Default.Lock, title = "Şifre Değiştir", isAction = true) {
                    Toast.makeText(context, "Şifre değiştirme ekranı hazırlanıyor.", Toast.LENGTH_SHORT).show() // Hata düzeltildi
                }
                HorizontalDivider()
                ProfileMenuItem(icon = Icons.Default.HelpOutline, title = "Destek ve Yardım", isAction = true) {
                    Toast.makeText(context, "Destek talebi özelliği yakında eklenecektir.", Toast.LENGTH_SHORT).show()
                }
                HorizontalDivider()
                ProfileMenuItem(icon = Icons.Default.Edit, title = "Profil Bilgilerini Güncelle", isAction = true) {
                    Toast.makeText(context, "Bilgi güncelleme talebiniz yöneticiye iletildi.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogOut,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Çıkış Yap", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, isAction: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = isAction, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (isAction) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}