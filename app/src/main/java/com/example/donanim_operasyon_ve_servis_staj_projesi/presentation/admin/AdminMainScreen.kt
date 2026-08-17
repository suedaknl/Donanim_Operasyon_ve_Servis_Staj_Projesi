package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home.HomeScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.PersonnelListScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AdminMainScreen(
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onNavigateToAddPersonnel: () -> Unit,
    onNavigateToEditPersonnel: (Int) -> Unit,
    onLogOut: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Ana Sayfa") },
                    label = { Text("Ana Sayfa") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.ListAlt else Icons.Outlined.ListAlt, contentDescription = "İş Emirleri") },
                    label = { Text("İş Emirleri") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.People else Icons.Outlined.PeopleOutline, contentDescription = "Personeller") },
                    label = { Text("Personeller") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(if (selectedTab == 3) Icons.Filled.LocationOn else Icons.Outlined.LocationOn, contentDescription = "Konum") },
                    label = { Text("Konum") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(if (selectedTab == 4) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings, contentDescription = "Profil") },
                    label = { Text("Profil") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false },
                        modifier = Modifier.width(220.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Yeni İş Emri Ekle", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                onNavigateToAddService()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Personel Ekle", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showFabMenu = false
                                onNavigateToAddPersonnel()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("AI Asistan (Yakında)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary) },
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
                0 -> AdminDashboardContent(
                    serviceViewModel = serviceViewModel,
                    personnelViewModel = personnelViewModel,
                    onCardClick = { statusTab ->
                        serviceViewModel.updateAdminSelectedStatusTab(statusTab)
                        selectedTab = 1 // İş Emirleri sekmesine yönlendir
                    },
                    onPersonnelCardClick = {
                        selectedTab = 2 // Personeller sekmesine yönlendir
                    }
                )

                1 -> {
                    val serviceList by serviceViewModel.filteredServiceRecords.collectAsState()
                    val personnelList by personnelViewModel.personnelList.collectAsState()

                    HomeScreen(
                        serviceList = serviceList,
                        personnelList = personnelList,
                        selectedTab = serviceViewModel.adminSelectedStatusTab,
                        onTabSelected = { serviceViewModel.updateAdminSelectedStatusTab(it) },
                        searchQuery = serviceViewModel.adminSearchQuery,
                        onSearchQueryChange = { serviceViewModel.updateAdminSearchQuery(it) },
                        selectedFilter = serviceViewModel.selectedFilter,
                        onFilterSelected = { serviceViewModel.updateSelectedFilter(it) },
                        selectedPriority = serviceViewModel.selectedPriorityFilter,
                        onPrioritySelected = { serviceViewModel.updateSelectedPriorityFilter(it) },
                        onClearFilters = {
                            serviceViewModel.updateSearchQuery("")
                            serviceViewModel.updateSelectedFilter("Hepsi")
                            serviceViewModel.updateSelectedPriorityFilter("Hepsi")
                        },
                        onNavigateToPersonnel = { selectedTab = 2 },
                        onNavigateToAddService = onNavigateToAddService,
                        onServiceClick = onServiceClick,
                        onLogOut = onLogOut,
                        serviceViewModel = serviceViewModel,
                        firebaseUid = null,
                        localPersonnelId = null
                    )
                }

                2 -> {
                    PersonnelListScreen(
                        viewModel = personnelViewModel,
                        serviceViewModel = serviceViewModel,
                        onNavigateToAddPersonnel = onNavigateToAddPersonnel,
                        onNavigateToEditPersonnel = onNavigateToEditPersonnel,
                        onNavigateBack = { selectedTab = 0 }
                    )
                }

                3 -> AdminLocationPlaceholder()
                4 -> AdminProfileContent(onLogOut = onLogOut)
            }
        }
    }
}

@Composable
fun AdminDashboardContent(
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onCardClick: (String) -> Unit,
    onPersonnelCardClick: () -> Unit
) {
    val allServices by serviceViewModel.serviceRecords.collectAsState(initial = emptyList())
    val allPersonnel by personnelViewModel.personnelList.collectAsState(initial = emptyList())

    val totalOrders = allServices.size
    val pendingOrders = allServices.count { it.status == ServiceStatus.BEKLIYOR }
    val inProgressOrders = allServices.count {
        it.status == ServiceStatus.ISLEME_BASLANDI ||
                it.status == ServiceStatus.YOLDA ||
                it.status == ServiceStatus.PARCA_BEKLENIYOR
    }
    val completedOrders = allServices.count { it.status == ServiceStatus.TAMAMLANDI }
    val totalPersonnel = allPersonnel.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Yönetici Özeti",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdminSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Toplam İş Emri",
                    count = totalOrders.toString(),
                    icon = Icons.Default.Assignment,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onCardClick("Tümü") }
                )
                AdminSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Personel Sayısı",
                    count = totalPersonnel.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onPersonnelCardClick() }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdminSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Bekleyen",
                    count = pendingOrders.toString(),
                    icon = Icons.Default.PendingActions,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onCardClick("Bekleyen") }
                )
                AdminSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "İşlemde",
                    count = inProgressOrders.toString(),
                    icon = Icons.Default.Autorenew,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onCardClick("İşlemde") }
                )
                AdminSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Tamamlanan",
                    count = completedOrders.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    onClick = { onCardClick("Tamamlanan") }
                )
            }
        }
    }
}

@Composable
fun AdminSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: (() -> Unit)?
) {
    val cardModifier = if (onClick != null) {
        modifier.height(120.dp).clickable { onClick() }
    } else {
        modifier.height(120.dp)
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = count, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminLocationPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.LocationOn, contentDescription = "Konum", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Konum & Harita Takibi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Saha personellerinin anlık konum takibi ve rota\nbilgileri bu sekmede yer alacaktır.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminProfileContent(onLogOut: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val email = currentUser?.email ?: "Sistem Yöneticisi"

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Profil", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(60.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Yetkili Yönetici", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Sistem Yetkisi", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(text = "Tam Erişim", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
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