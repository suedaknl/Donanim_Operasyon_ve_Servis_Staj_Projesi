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
    val tabs = listOf("Tümü", "Atanmamış", "Atanan", "Devam Eden", "Tamamlanan")

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        serviceViewModel.syncAdminData()
    }

    // DİKKAT: Üstteki kafa karıştırıcı TopAppBar ve çakışan FAB bu ekrandan tamamen temizlendi.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
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
                    onClick = { showFilterSheet = true },
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
                        .fillMaxSize()
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
                        .fillMaxSize()
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
}