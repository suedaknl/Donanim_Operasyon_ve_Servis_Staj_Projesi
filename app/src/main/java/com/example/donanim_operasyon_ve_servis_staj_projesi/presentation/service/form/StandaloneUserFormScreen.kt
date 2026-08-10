package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneUserFormScreen(
    viewModel: ServiceViewModel,
    personnelId: Int,
    onServiceClick: (Int) -> Unit,
    onLogOut: () -> Unit
) {
    // 1. Veritabanından sadece bu personele atanmış işleri çeker (GÜVENLİK)
    LaunchedEffect(personnelId) {
        viewModel.loadRecordsForPersonnel(personnelId)
    }

    // 2. ViewModel'den gelen ARAMA ve DETAYLI FİLTRE uygulanmış GÜVENLİ liste
    val basePersonnelRecords by viewModel.filteredPersonnelServiceRecords.collectAsState()

    // 3. UI State (Sekmeler ve BottomSheet için)
    var selectedTab by remember { mutableStateOf("Tümü") }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tabs = listOf("Tümü", "Bekleyen", "Devam Eden", "Tamamlanan")
    val statusOptions = listOf("Hepsi") + ServiceStatus.all
    val priorityOptions = listOf("Hepsi", "Düşük", "Normal", "Yüksek", "Acil")

    // Mevcut filtre/arama state'leri
    val searchQuery = viewModel.searchQuery
    val selectedStatus = viewModel.selectedFilter
    val selectedPriority = viewModel.selectedPriorityFilter

    // 4. GÜVENLİ Sekme (Tab) Ayrıştırması
    val finalRecords = remember(basePersonnelRecords, selectedTab) {
        basePersonnelRecords.filter { record ->
            when (selectedTab) {
                "Bekleyen" -> record.status == ServiceStatus.BEKLIYOR
                "Devam Eden" -> record.status == ServiceStatus.YOLDA ||
                        record.status == ServiceStatus.ISLEME_BASLANDI ||
                        record.status == ServiceStatus.PARCA_BEKLENIYOR
                "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
                else -> true // "Tümü"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atanan İş Emirlerim", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Çıkış Yap")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // --- ÜST SEKMELER (TABS) ---
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // --- ARAMA ÇUBUĞU VE FİLTRE İKONU ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Firma, cihaz, seri no veya lokasyon...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val isFilterActive = selectedStatus != "Hepsi" || selectedPriority != "Hepsi"
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

            // --- İŞ EMRİ LİSTESİ ---
            if (finalRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty() || selectedStatus != "Hepsi" || selectedPriority != "Hepsi" || selectedTab != "Tümü") {
                            "Arama veya filtreleme kriterlerine uygun iş emri bulunamadı."
                        } else {
                            "Şu anda size atanmış aktif bir iş emri bulunmuyor."
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
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(finalRecords) { record ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onServiceClick(record.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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

                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Durum: ${record.status}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- FİLTRE BOTTOM SHEET ---
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Detaylı Filtreleme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                HorizontalDivider()

                Text("Durum", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusOptions) { status ->
                        FilterChip(
                            selected = (selectedStatus == status),
                            onClick = { viewModel.updateSelectedFilter(status) },
                            label = { Text(status) }
                        )
                    }
                }

                Text("Öncelik", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(priorityOptions) { priority ->
                        FilterChip(
                            selected = (selectedPriority == priority),
                            onClick = { viewModel.updateSelectedPriorityFilter(priority) },
                            label = { Text(priority) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateSearchQuery("")
                        viewModel.updateSelectedFilter("Hepsi")
                        viewModel.updateSelectedPriorityFilter("Hepsi")
                        selectedTab = "Tümü"
                        showFilterSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Filtreleri Temizle")
                }
            }
        }
    }
}