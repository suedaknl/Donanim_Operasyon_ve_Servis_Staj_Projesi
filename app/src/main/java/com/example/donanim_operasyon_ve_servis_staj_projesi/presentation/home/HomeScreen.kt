package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serviceList: List<ServiceRecord>,
    personnelList: List<Personnel>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    onClearFilters: () -> Unit,
    onNavigateToPersonnel: () -> Unit = {},
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onLogOut: () -> Unit = {},
    serviceViewModel: ServiceViewModel,
    firebaseUid: String? = null,
    localPersonnelId: Int? = null
) {
    val tabs = listOf("Tümü", "Bekleyen", "Yolda", "İşlemde", "Tamamlanan", "Reddedilen")

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dinamik filtre seçenekleri
    val allRecords = serviceViewModel.serviceRecords.collectAsState(initial = emptyList()).value
    val dynamicDeviceTypes = remember(allRecords) { allRecords.map { it.deviceType }.filter { !it.isBlank() }.distinct() }
    val dynamicCompanies = remember(allRecords) { allRecords.map { it.companyName }.filter { !it.isBlank() }.distinct() }
    val dynamicLocations = remember(allRecords) { allRecords.map { it.location }.filter { !it.isBlank() }.distinct() }

    // ModalBottomSheet içindeki geçici filtre state'leri
    var tempSelectedStatuses by remember { mutableStateOf(serviceViewModel.selectedStatusesFilter.value) }
    var tempSelectedPriorities by remember { mutableStateOf(serviceViewModel.selectedPrioritiesFilter.value) }
    var tempSelectedDeviceType by remember { mutableStateOf(serviceViewModel.selectedDeviceTypesFilter.value.firstOrNull()) }
    var tempSelectedPersonnel by remember { mutableStateOf(serviceViewModel.selectedPersonnelFilter) }
    var tempSelectedCompany by remember { mutableStateOf(serviceViewModel.selectedCompanyFilter) }
    var tempSelectedLocation by remember { mutableStateOf(serviceViewModel.selectedLocationFilter) }
    var tempAssignmentStatus by remember { mutableStateOf(serviceViewModel.selectedAssignmentStatusFilter) }
    var tempSortOption by remember { mutableStateOf(serviceViewModel.selectedSortOption) }

    // Arama pencereleri (Dialog) durumları
    var activeDialog by remember { mutableStateOf<String?>(null) } // "personnel", "device", "company", "location"

    LaunchedEffect(Unit) {
        serviceViewModel.syncAdminData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İş Emirleri Yönetimi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(tab, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Arama Kutusu ve Filtre Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Firma, cihaz, seri no, lokasyon...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val activeCount = serviceViewModel.activeFilterCount
                FilledIconButton(
                    onClick = {
                        tempSelectedStatuses = serviceViewModel.selectedStatusesFilter.value
                        tempSelectedPriorities = serviceViewModel.selectedPrioritiesFilter.value
                        tempSelectedDeviceType = serviceViewModel.selectedDeviceTypesFilter.value.firstOrNull()
                        tempSelectedPersonnel = serviceViewModel.selectedPersonnelFilter
                        tempSelectedCompany = serviceViewModel.selectedCompanyFilter
                        tempSelectedLocation = serviceViewModel.selectedLocationFilter
                        tempAssignmentStatus = serviceViewModel.selectedAssignmentStatusFilter
                        tempSortOption = serviceViewModel.selectedSortOption
                        showFilterSheet = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (activeCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (activeCount > 0) {
                                Badge { Text(activeCount.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrele")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            if (serviceList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu filtrelere uygun iş emri bulunamadı.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(serviceList) { record ->
                        val assignedPersonnelName = personnelList.find { it.id.toString() == record.assignedPersonnelId?.toString() || it.firebaseUid == record.assignedPersonnelUid }?.fullName ?: "Atanmadı"

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onServiceClick(record) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = record.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text("Cihaz: ${record.deviceType} - ${record.deviceModel}", style = MaterialTheme.typography.bodyMedium)
                                Text("Lokasyon: ${record.location}", style = MaterialTheme.typography.bodyMedium)
                                Text("Öncelik: ${record.priority}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Atanan Personel", style = MaterialTheme.typography.labelSmall)
                                            Text(assignedPersonnelName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Durum", style = MaterialTheme.typography.labelSmall)
                                            Text(record.status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ModalBottomSheet Gelişmiş Filtre Paneli
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gelişmiş Filtreler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            serviceViewModel.clearAllAdvancedFilters()
                            showFilterSheet = false
                        }) {
                            Text("Filtreleri Temizle", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // 1. Sıralama
                item {
                    Text("Sıralama", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("En yeni", "En eski", "Önceliği yüksek olan").forEach { sortOpt ->
                            FilterChip(
                                selected = tempSortOption == sortOpt,
                                onClick = { tempSortOption = sortOpt },
                                label = { Text(sortOpt) }
                            )
                        }
                    }
                }

                // 2. Çoklu Durum Seçimi
                item {
                    Text("Durumlar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val availableStatuses = listOf(ServiceStatus.BEKLIYOR, ServiceStatus.YOLDA, ServiceStatus.ISLEME_BASLANDI, ServiceStatus.PARCA_BEKLENIYOR, ServiceStatus.TAMAMLANDI, ServiceStatus.IPTAL)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableStatuses.chunked(3).forEach { rowStatuses ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowStatuses.forEach { st ->
                                    FilterChip(
                                        selected = tempSelectedStatuses.contains(st),
                                        onClick = {
                                            tempSelectedStatuses = if (tempSelectedStatuses.contains(st)) tempSelectedStatuses - st else tempSelectedStatuses + st
                                        },
                                        label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Öncelik Filtresi
                item {
                    Text("Öncelik", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Düşük", "Normal", "Yüksek", "Acil").forEach { pri ->
                            FilterChip(
                                selected = tempSelectedPriorities.contains(pri),
                                onClick = {
                                    tempSelectedPriorities = if (tempSelectedPriorities.contains(pri)) tempSelectedPriorities - pri else tempSelectedPriorities + pri
                                },
                                label = { Text(pri) }
                            )
                        }
                    }
                }

                // 4. Atama Durumu
                item {
                    Text("Atama Durumu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tümü", "Atanmış", "Atanmamış").forEach { assignOpt ->
                            FilterChip(
                                selected = tempAssignmentStatus == assignOpt,
                                onClick = { tempAssignmentStatus = assignOpt },
                                label = { Text(assignOpt) }
                            )
                        }
                    }
                }

                // 5. Atanan Personel (Dropdown / Arama Butonu)
                item {
                    Text("Atanan Personel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val selectedPersonnelLabel = if (tempSelectedPersonnel == null || tempSelectedPersonnel == "Tümü") {
                        "Tüm Personeller"
                    } else {
                        personnelList.find { it.id.toString() == tempSelectedPersonnel || it.firebaseUid == tempSelectedPersonnel }?.fullName ?: tempSelectedPersonnel!!
                    }
                    OutlinedButton(
                        onClick = { activeDialog = "personnel" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedPersonnelLabel)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }

                // 6. Cihaz Türü (Dropdown / Arama Butonu)
                item {
                    Text("Cihaz Türü", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val selectedDeviceLabel = tempSelectedDeviceType?.takeIf { it.isNotBlank() } ?: "Tüm Cihaz Türleri"
                    OutlinedButton(
                        onClick = { activeDialog = "device" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedDeviceLabel)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }

                // 7. Firma (Dropdown / Arama Butonu)
                item {
                    Text("Firma", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val selectedCompanyLabel = tempSelectedCompany?.takeIf { it.isNotBlank() && it != "Tümü" } ?: "Tüm Firmalar"
                    OutlinedButton(
                        onClick = { activeDialog = "company" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedCompanyLabel)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }

                // 8. Lokasyon (Dropdown / Arama Butonu)
                item {
                    Text("Lokasyon", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val selectedLocationLabel = tempSelectedLocation?.takeIf { it.isNotBlank() && it != "Tümü" } ?: "Tüm Lokasyonlar"
                    OutlinedButton(
                        onClick = { activeDialog = "location" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedLocationLabel)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }

                // Uygula Butonu
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            serviceViewModel.updateAdvancedFilters(
                                dateFilter = "Tümü",
                                start = null,
                                end = null,
                                statuses = tempSelectedStatuses,
                                priorities = tempSelectedPriorities,
                                deviceTypes = if (tempSelectedDeviceType.isNullOrBlank()) emptySet() else setOf(tempSelectedDeviceType!!),
                                personnel = tempSelectedPersonnel,
                                company = tempSelectedCompany,
                                location = tempSelectedLocation,
                                assignment = tempAssignmentStatus,
                                sort = tempSortOption
                            )
                            showFilterSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Filtreleri Uygula")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // --- ARANABİLİR SEÇİM DİALOGLARI ---
    when (activeDialog) {
        "personnel" -> {
            SearchableSelectorDialog(
                title = "Personel Seç",
                items = listOf("Tümü") + personnelList.map { it.id.toString() },
                itemLabelMapper = { id -> if (id == "Tümü") "Tüm Personeller" else (personnelList.find { it.id.toString() == id }?.fullName ?: id) },
                onItemSelected = { selected -> tempSelectedPersonnel = if (selected == "Tümü") null else selected },
                onDismiss = { activeDialog = null }
            )
        }
        "device" -> {
            SearchableSelectorDialog(
                title = "Cihaz Türü Seç",
                items = listOf("Tümü") + dynamicDeviceTypes,
                itemLabelMapper = { if (it == "Tümü") "Tüm Cihaz Türleri" else it },
                onItemSelected = { selected -> tempSelectedDeviceType = if (selected == "Tümü") null else selected },
                onDismiss = { activeDialog = null }
            )
        }
        "company" -> {
            SearchableSelectorDialog(
                title = "Firma Seç",
                items = listOf("Tümü") + dynamicCompanies,
                itemLabelMapper = { if (it == "Tümü") "Tüm Firmalar" else it },
                onItemSelected = { selected -> tempSelectedCompany = if (selected == "Tümü") null else selected },
                onDismiss = { activeDialog = null }
            )
        }
        "location" -> {
            SearchableSelectorDialog(
                title = "Lokasyon Seç",
                items = listOf("Tümü") + dynamicLocations,
                itemLabelMapper = { if (it == "Tümü") "Tüm Lokasyonlar" else it },
                onItemSelected = { selected -> tempSelectedLocation = if (selected == "Tümü") null else selected },
                onDismiss = { activeDialog = null }
            )
        }
    }
}

// Yardımcı Arama ve Seçim Dialog Bileşeni
@Composable
fun SearchableSelectorDialog(
    title: String,
    items: List<String>,
    itemLabelMapper: (String) -> String = { it },
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredItems = remember(query, items) {
        if (query.isBlank()) items else items.filter { itemLabelMapper(it).contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredItems) { item ->
                        val label = itemLabelMapper(item)
                        ListItem(
                            headlineContent = { Text(label, fontWeight = if (item == "Tümü") FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(item)
                                    onDismiss()
                                }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}