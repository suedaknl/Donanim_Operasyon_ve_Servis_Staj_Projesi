package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.service.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.service.components.AdminServiceCard
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.service.filters.AdminServiceFilterSheet
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
fun AdminServiceListScreen(
    serviceList: List<ServiceRecord>,
    personnelList: List<Personnel>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    serviceViewModel: ServiceViewModel,
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onLogOut: () -> Unit = {}
) {
    val tabs = listOf("Tümü", "Bekleyen", "Yolda", "İşlemde", "Tamamlanan", "Reddedilen")

    var showFilterSheet by remember { mutableStateOf(false) }

    val pagedList = serviceViewModel.adminPagedServiceRecords.collectAsState().value
    val currentPage = serviceViewModel.adminCurrentPage.collectAsState().value
    val totalPages = serviceViewModel.admintotalPages.collectAsState().value
    val fullFilteredList = serviceViewModel.filteredServiceRecords.collectAsState().value

    val allRecords = serviceViewModel.serviceRecords.collectAsState(initial = emptyList()).value
    val dynamicDeviceTypes = remember(allRecords) { allRecords.map { it.deviceType }.filter { !it.isBlank() }.distinct() }
    val dynamicCompanies = remember(allRecords) { allRecords.map { it.companyName }.filter { !it.isBlank() }.distinct() }
    val dynamicLocations = remember(allRecords) { allRecords.map { it.location }.filter { !it.isBlank() }.distinct() }

    LaunchedEffect(Unit) {
        serviceViewModel.syncAdminData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İş Emirleri Yönetimi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                windowInsets = WindowInsets(top = 0.dp, bottom = 0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentPage == 1) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        placeholder = { Text("Firma, cihaz, seri no, lokasyon...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    val activeCount = serviceViewModel.activeFilterCount
                    FilledIconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.size(52.dp),
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
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrele", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Text(
                    text = "Toplam ${fullFilteredList.size} iş emri",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            } else {
                Text(
                    text = "İş Emirleri Yönetimi (Sayfa $currentPage / $totalPages)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
            }

            if (pagedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu filtreye uygun iş emri bulunamadı.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    items(pagedList) { record ->
                        AdminServiceCard(
                            record = record,
                            personnelList = personnelList,
                            onClick = { onServiceClick(record) }
                        )
                    }
                }
            }

            if (totalPages > 1) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { serviceViewModel.setAdminPage(currentPage - 1) },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki Sayfa")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val pageNumbers = remember(currentPage, totalPages) {
                            when {
                                totalPages <= 4 -> (1..totalPages).toList()
                                currentPage <= 2 -> listOf(1, 2, 3, -1, totalPages)
                                currentPage >= totalPages - 1 -> listOf(1, -1, totalPages - 2, totalPages - 1, totalPages)
                                else -> listOf(1, -1, currentPage, -1, totalPages)
                            }
                        }

                        pageNumbers.forEach { pageNum ->
                            if (pageNum == -1) {
                                Text(
                                    text = "…",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                val isCurrent = pageNum == currentPage
                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { serviceViewModel.setAdminPage(pageNum) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pageNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { serviceViewModel.setAdminPage(currentPage + 1) },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki Sayfa")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showFilterSheet) {
        AdminServiceFilterSheet(
            serviceViewModel = serviceViewModel,
            personnelList = personnelList,
            dynamicDeviceTypes = dynamicDeviceTypes,
            dynamicCompanies = dynamicCompanies,
            dynamicLocations = dynamicLocations,
            onDismiss = { showFilterSheet = false }
        )
    }
}