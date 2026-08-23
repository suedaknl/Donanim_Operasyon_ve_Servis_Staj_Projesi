package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.service.list.AdminServiceListScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.list.PersonnelListScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.map.AdminMapScreen
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.archive.AdminArchiveScreen
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onNavigateToAddPersonnel: () -> Unit,
    onNavigateToEditPersonnel: (Int) -> Unit,
    onNavigateToPersonnelDetail: (Int) -> Unit,
    adminEmail: String,
    onNavigateToPersonnel: () -> Unit,
    onNavigateToShift: () -> Unit,
    onNavigateToLeave: () -> Unit,
    onNavigateToOvertime: () -> Unit,
    onLogOut: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var serviceSubScreen by rememberSaveable { mutableStateOf("list") }
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    var showFabMenu by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val minX = with(density) { -(configuration.screenWidthDp.dp - 80.dp).toPx() }
    val maxX = 0f
    val minY = with(density) { -(configuration.screenHeightDp.dp - 180.dp).toPx() }
    val maxY = 0f

    val filteredServices by serviceViewModel.filteredServiceRecords.collectAsState()
    val personnelList by personnelViewModel.personnelList.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Yönetici Menüsü",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Personel Yönetimi", fontWeight = FontWeight.Medium) },
                    selected = false,
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToPersonnel()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Vardiya Yönetimi", fontWeight = FontWeight.Medium) },
                    selected = false,
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToShift()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("İzin Talepleri", fontWeight = FontWeight.Medium) },
                    selected = false,
                    icon = { Icon(Icons.Default.EventBusy, contentDescription = null) },
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToLeave()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Fazla Mesai", fontWeight = FontWeight.Medium) },
                    selected = false,
                    icon = { Icon(Icons.Default.MoreTime, contentDescription = null) },
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToOvertime()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            serviceSubScreen = "list"
                        },
                        icon = { Icon(if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Ana Sayfa") },
                        label = { Text("Ana Sayfa") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            serviceSubScreen = "list"
                        },
                        icon = { Icon(if (selectedTab == 1) Icons.Filled.ListAlt else Icons.Outlined.ListAlt, contentDescription = "İş Emirleri") },
                        label = { Text("İş Emirleri") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            serviceSubScreen = "list"
                        },
                        icon = { Icon(if (selectedTab == 2) Icons.Filled.People else Icons.Outlined.PeopleOutline, contentDescription = "Personeller") },
                        label = { Text("Personeller") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            serviceSubScreen = "list"
                        },
                        icon = { Icon(if (selectedTab == 3) Icons.Filled.LocationOn else Icons.Outlined.LocationOn, contentDescription = "Konum") },
                        label = { Text("Konum") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = {
                            selectedTab = 4
                            serviceSubScreen = "list"
                        },
                        icon = { Icon(if (selectedTab == 4) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings, contentDescription = "Profil") },
                        label = { Text("Profil") }
                    )
                }
            },
            floatingActionButton = {
                if ((selectedTab == 0 || selectedTab == 1) && serviceSubScreen == "list") {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(minX, maxX)
                                    fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(minY, maxY)
                                }
                            }
                    ) {
                        if (selectedTab == 0) {
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
                        } else if (selectedTab == 1) {
                            FloatingActionButton(
                                onClick = { onNavigateToAddService() },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Yeni İş Emri Ekle",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
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
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onCardClick = { statusTab ->
                            serviceViewModel.updateAdminSelectedStatusTab(statusTab)
                            selectedTab = 1
                            serviceSubScreen = "list"
                        },
                        onPersonnelCardClick = {
                            selectedTab = 2
                            serviceSubScreen = "list"
                        }
                    )

                    1 -> {
                        if (serviceSubScreen == "list") {
                            AdminServiceListScreen(
                                serviceList = filteredServices,
                                personnelList = personnelList,
                                selectedTab = serviceViewModel.adminSelectedStatusTab,
                                onTabSelected = { tab -> serviceViewModel.updateAdminSelectedStatusTab(tab) },
                                searchQuery = serviceViewModel.adminSearchQuery,
                                onSearchQueryChange = { query -> serviceViewModel.updateAdminSearchQuery(query) },
                                serviceViewModel = serviceViewModel,
                                onNavigateToAddService = onNavigateToAddService,
                                onServiceClick = onServiceClick,
                                onOpenArchive = { serviceSubScreen = "archive" },
                                onLogOut = onLogOut
                            )
                        } else if (serviceSubScreen == "archive") {
                            AdminArchiveScreen(
                                serviceViewModel = serviceViewModel,
                                personnelList = personnelList,
                                onServiceClick = onServiceClick,
                                onBackClick = { serviceSubScreen = "list" }
                            )
                        }
                    }

                    2 -> {
                        PersonnelListScreen(
                            viewModel = personnelViewModel,
                            onNavigateToAddPersonnel = onNavigateToAddPersonnel,
                            onNavigateToEditPersonnel = { id ->
                                onNavigateToEditPersonnel(id)
                            },
                            onNavigateBack = { selectedTab = 0 },
                            onPersonnelClick = { personnelId ->
                                onNavigateToPersonnelDetail(personnelId)
                            }
                        )
                    }

                    3 -> AdminMapScreen(
                        serviceViewModel = serviceViewModel,
                        personnelViewModel = personnelViewModel,
                        onNavigateToServiceDetail = { serviceId ->
                            val targetService = serviceViewModel.serviceRecords.value.find { it.id == serviceId }
                            if (targetService != null) {
                                onServiceClick(targetService)
                            }
                        }
                    )

                    4 -> AdminProfileContent(
                        email = adminEmail,
                        onLogOut = onLogOut
                    )
                }
            }
        }
    }
}

@Composable
fun AdminDashboardContent(
    serviceViewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    onOpenDrawer: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menüyü Aç",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Yönetici Özeti",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
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
fun AdminProfileContent(
    email: String,
    onLogOut: () -> Unit
) {
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