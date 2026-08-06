package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serviceList: List<ServiceRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onNavigateToPersonnel: () -> Unit,
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onLogOut: () -> Unit
) {
    // 1. Menü açılır/kapanır durumu (State)
    var expandedMenu by remember { mutableStateOf(false) }

    // 2. Arama ve Filtreleme Mantığı
    val filteredList = serviceList.filter { service ->
        val matchesSearch = searchQuery.isBlank() ||
                service.companyName.contains(searchQuery, ignoreCase = true) ||
                service.deviceType.contains(searchQuery, ignoreCase = true) ||
                service.serialNumber.contains(searchQuery, ignoreCase = true)

        val matchesFilter = selectedFilter == "Hepsi" || service.status == selectedFilter

        matchesSearch && matchesFilter
    }

    // 3. Dashboard Özet Hesaplamaları (Tüm listeye göre hesaplanır)
    val totalCount = serviceList.size
    // DÜZELTME: "Bekleyen" kelimesi veritabanındaki karşılığı olan "Bekliyor" ile değiştirildi.
    val pendingCount = serviceList.count { it.status == "Bekliyor" }
    val inProgressCount = serviceList.count { it.status == "Devam Eden" }
    val completedCount = serviceList.count { it.status == "Tamamlanan" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aktif Operasyonlar (Admin)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Personel Yönetimi") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Personel Yönetimi") },
                            onClick = {
                                expandedMenu = false
                                onNavigateToPersonnel()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Çıkış Yap") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap") },
                            onClick = {
                                expandedMenu = false
                                onLogOut()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddService,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni İş Emri")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 5. Renkli Dashboard Kartları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(
                    title = "Toplam",
                    count = totalCount,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Bekleyen", // Kart başlığı görsel UX açısından "Bekleyen" olarak kalıyor
                    count = pendingCount,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Devam Eden",
                    count = inProgressCount,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Biten",
                    count = completedCount,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Arama Çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("İş emri ara (Firma, Cihaz, Seri No)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Filtreleme Çipleri
            // DÜZELTME: "Bekleyen" yerine "Bekliyor" yazıldı.
            val filters = listOf("Hepsi", "Bekliyor", "Devam Eden", "Tamamlanan")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 8. Dinamik İş Emirleri Listesi
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kayıtlı iş emri bulunamadı.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList) { service ->
                        ServiceItemCard(
                            service = service,
                            onClick = { onServiceClick(service) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, count: Int, containerColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ServiceItemCard(service: ServiceRecord, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = service.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // DÜZELTME: "Bekleyen" yerine "Bekliyor" yazıldı.
                val badgeColor = when (service.status) {
                    "Tamamlanan" -> MaterialTheme.colorScheme.tertiaryContainer
                    "Devam Eden" -> MaterialTheme.colorScheme.secondaryContainer
                    "Bekliyor" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val badgeTextColor = when (service.status) {
                    "Tamamlanan" -> MaterialTheme.colorScheme.onTertiaryContainer
                    "Devam Eden" -> MaterialTheme.colorScheme.onSecondaryContainer
                    "Bekliyor" -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = service.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Cihaz: ${service.deviceType} (${service.deviceModel})", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Seri No: ${service.serialNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}