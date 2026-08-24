package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.GetAdminWorkAnalysisUseCase
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.service.AnalysisPeriod
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.nativeCanvas
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
    onNavigateToServiceRegistry: () -> Unit,
    onEditServiceClick: (Int) -> Unit,
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
                NavigationDrawerItem(
                    label = { Text("Servis Sicili", fontWeight = FontWeight.Medium) },
                    selected = false,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToServiceRegistry()
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
                                onEditServiceClick = onEditServiceClick,
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

    val analysisUseCase = remember { GetAdminWorkAnalysisUseCase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        // FAB ve alt menü çakışmasını önlemek için alt boşluk (bottom padding) artırıldı
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
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

        item {
            AdminWorkAnalysisSection(
                records = allServices,
                analysisUseCase = analysisUseCase
            )
        }
    }
}

@Composable
fun AdminWorkAnalysisSection(
    records: List<ServiceRecord>,
    analysisUseCase: GetAdminWorkAnalysisUseCase
) {
    var selectedPeriod by remember { mutableStateOf(AnalysisPeriod.WEEKLY) }

    val analysis = remember(records, selectedPeriod) {
        analysisUseCase(records, selectedPeriod)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "İş Analizi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = analysis.periodTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dönem Seçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalysisPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    val buttonColors = if (isSelected) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { selectedPeriod = period },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = buttonColors,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = when (period) {
                                AnalysisPeriod.DAILY -> "Günlük"
                                AnalysisPeriod.WEEKLY -> "Haftalık"
                                AnalysisPeriod.MONTHLY -> "Aylık"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (analysis.totalCreated == 0 && analysis.totalCompleted == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedPeriod) {
                            AnalysisPeriod.DAILY -> "Bugün için iş emri bulunmuyor."
                            AnalysisPeriod.WEEKLY -> "Son 7 günde iş emri bulunmuyor."
                            AnalysisPeriod.MONTHLY -> "Son 30 günde iş emri bulunmuyor."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                        Text("Oluşturulan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF16A34A), RoundedCornerShape(4.dp)))
                        Text("Tamamlanan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val successColor = Color(0xFF16A34A)
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val textColorInt = android.graphics.Color.GRAY // Native Android text paint için

                val maxVal = (analysis.createdCounts + analysis.completedCounts).maxOrNull()?.coerceAtLeast(4) ?: 4

                // X Ekseni Etiketleri ile birlikte Canvas Çizimi
                val textPaint = android.graphics.Paint().apply {
                    textSize = 10.dp.value * LocalDensity.current.density
                    color = android.graphics.Color.DKGRAY
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 20.dp.toPx()
                        val paddingLeft = 24.dp.toPx()
                        val chartWidth = width - paddingLeft
                        val chartHeight = height - paddingBottom

                        val stepX = if (analysis.labels.size > 1) chartWidth / (analysis.labels.size - 1) else chartWidth

                        // Grid Çizgileri
                        for (i in 0..4) {
                            val y = chartHeight * (i / 4f)
                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val getX: (Int) -> Float = { index -> paddingLeft + (index * stepX) }
                        val getY: (Int) -> Float = { value -> chartHeight - (value.toFloat() / maxVal * chartHeight) }

                        val createdPath = Path().apply {
                            analysis.createdCounts.forEachIndexed { index, value ->
                                val x = getX(index)
                                val y = getY(value)
                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }

                        val completedPath = Path().apply {
                            analysis.completedCounts.forEachIndexed { index, value ->
                                val x = getX(index)
                                val y = getY(value)
                                if (index == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = createdPath,
                            color = primaryColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = completedPath,
                            color = successColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        analysis.createdCounts.forEachIndexed { index, value ->
                            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(getX(index), getY(value)))
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(getX(index), getY(value)))
                        }
                        analysis.completedCounts.forEachIndexed { index, value ->
                            drawCircle(color = successColor, radius = 4.dp.toPx(), center = Offset(getX(index), getY(value)))
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(getX(index), getY(value)))
                        }

                        // X Eksen Etiketleri (Native Canvas Text)
                        analysis.labels.forEachIndexed { index, label ->
                            val x = getX(index)
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                x,
                                chartHeight + 16.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WorkStatItem("Toplam İş", "${analysis.totalCreated}")
                    WorkStatItem("Tamamlanan", "${analysis.totalCompleted}")
                    WorkStatItem("Tamamlama %", "%${analysis.completionRate}")
                }
            }
        }
    }
}

@Composable
fun WorkStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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