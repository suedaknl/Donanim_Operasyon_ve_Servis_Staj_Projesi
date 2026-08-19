package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
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
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneUserFormScreen(
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
    onNavigateToPersonnel: () -> Unit,
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onLogOut: () -> Unit,
    serviceViewModel: ServiceViewModel,
    firebaseUid: String? = null,
    localPersonnelId: Int? = null
) {
    val statusOptions = listOf("Hepsi") + ServiceStatus.all
    val priorityOptions = listOf("Hepsi", "Düşük", "Normal", "Yüksek", "Acil")
    val tabs = listOf("Tümü", "Atanmamış", "Atanan", "Yolda", "İşlemde", "Tamamlanan")

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempSelectedFilter by remember { mutableStateOf(selectedFilter) }
    var tempSelectedPriority by remember { mutableStateOf(selectedPriority) }

    LaunchedEffect(Unit) {
        serviceViewModel.syncAdminData()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val safeTabIndex = kotlin.math.max(0, tabs.indexOf(selectedTab))

            ScrollableTabRow(
                selectedTabIndex = safeTabIndex,
                edgePadding = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = safeTabIndex == index,
                        onClick = { onTabSelected(tab) },
                        text = { Text(tab, fontWeight = if (safeTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

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
                    placeholder = { Text("Firma, cihaz, seri no...") },
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

                val isFilterActive = selectedFilter != "Hepsi" || selectedPriority != "Hepsi"
                FilledIconButton(
                    onClick = {
                        tempSelectedFilter = selectedFilter
                        tempSelectedPriority = selectedPriority
                        showFilterSheet = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isFilterActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtrele")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            if (serviceList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedFilter != "Hepsi" || selectedPriority != "Hepsi") {
                            "Arama veya filtreleme kriterlerine uygun iş emri bulunamadı."
                        } else {
                            "Bu kategoride iş emri bulunmuyor."
                        },
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(serviceList) { record ->
                        val assignedPersonnelName = personnelList.find { it.id == record.assignedPersonnelId }?.fullName ?: "Atanmadı"

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
                                    Text(
                                        text = record.companyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text("Cihaz: ${record.deviceType} - ${record.deviceModel}", style = MaterialTheme.typography.bodyMedium)
                                Text("Lokasyon: ${record.location}", style = MaterialTheme.typography.bodyMedium)
                                Text("Öncelik: ${record.priority}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (record.assignedPersonnelId != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Atanan Personel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(assignedPersonnelName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Durum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtreler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        onClearFilters()
                        showFilterSheet = false
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
                        Text("Durum Filtresi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            statusOptions.chunked(3).forEach { rowOpts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowOpts.forEach { opt ->
                                        FilterChip(
                                            selected = tempSelectedFilter == opt,
                                            onClick = { tempSelectedFilter = opt },
                                            label = { Text(opt, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // Eğer satırda 3'ten az eleman varsa boşluk doldurma (esnememesi için weight eklendi)
                                    repeat(3 - rowOpts.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Öncelik Filtresi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            priorityOptions.chunked(3).forEach { rowOpts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowOpts.forEach { pri ->
                                        FilterChip(
                                            selected = tempSelectedPriority == pri,
                                            onClick = { tempSelectedPriority = pri },
                                            label = { Text(pri, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowOpts.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
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
                                onFilterSelected(tempSelectedFilter)
                                onPrioritySelected(tempSelectedPriority)
                                showFilterSheet = false
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
    }
}