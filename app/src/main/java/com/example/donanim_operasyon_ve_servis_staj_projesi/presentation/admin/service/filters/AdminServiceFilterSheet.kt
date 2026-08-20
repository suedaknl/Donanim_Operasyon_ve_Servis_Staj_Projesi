package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.service.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminServiceFilterSheet(
    serviceViewModel: ServiceViewModel,
    personnelList: List<Personnel>,
    dynamicDeviceTypes: List<String>,
    dynamicCompanies: List<String>,
    dynamicLocations: List<String>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempSelectedStatuses by remember { mutableStateOf(serviceViewModel.selectedStatusesFilter.value) }
    var tempSelectedPriorities by remember { mutableStateOf(serviceViewModel.selectedPrioritiesFilter.value) }
    var tempSelectedDeviceType by remember { mutableStateOf(serviceViewModel.selectedDeviceTypesFilter.value.firstOrNull()) }
    var tempSelectedPersonnel by remember { mutableStateOf(serviceViewModel.selectedPersonnelFilter) }
    var tempSelectedCompany by remember { mutableStateOf(serviceViewModel.selectedCompanyFilter) }
    var tempSelectedLocation by remember { mutableStateOf(serviceViewModel.selectedLocationFilter) }
    var tempAssignmentStatus by remember { mutableStateOf(serviceViewModel.selectedAssignmentStatusFilter) }
    var tempSortOption by remember { mutableStateOf(serviceViewModel.selectedSortOption) }

    var activeDialog by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gelişmiş Filtreler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    serviceViewModel.clearAllAdvancedFilters()
                    onDismiss()
                }) {
                    Text("Filtreleri Temizle", color = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
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
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
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
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Filtreleri Uygula", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

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