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
import androidx.compose.ui.graphics.Color
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
    var expandedMenu by remember { mutableStateOf(false) }

    // 1. Güncellenmiş Filtreleme Mantığı
    val filteredList = serviceList.filter { service ->
        val matchesSearch = searchQuery.isBlank() ||
                service.companyName.contains(searchQuery, ignoreCase = true) ||
                service.deviceType.contains(searchQuery, ignoreCase = true) ||
                service.serialNumber.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Bekleyen" -> service.status == "Bekliyor" || service.status == "Parça Bekleniyor"
            "Devam Eden" -> service.status == "Yolda" || service.status == "İşleme Başlandı"
            "Tamamlanan" -> service.status == "Tamamlandı"
            else -> true // "Hepsi" ve diğer durumlar için
        }

        matchesSearch && matchesFilter
    }

    // 2. Güncellenmiş Dashboard Sayaç Hesaplamaları
    val totalCount = serviceList.size
    val bekleyenCount = serviceList.count { it.status == "Bekliyor" || it.status == "Parça Bekleniyor" }
    val devamEdenCount = serviceList.count { it.status == "Yolda" || it.status == "İşleme Başlandı" }
    val tamamlananCount = serviceList.count { it.status == "Tamamlandı" }

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

            // 3. Renkleri ve Değişkenleri Güncellenmiş Dashboard Kartları
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
                    title = "Bekleyen",
                    count = bekleyenCount,
                    containerColor = Color(0xFFFF9800), // Turuncu
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Devam Eden",
                    count = devamEdenCount,
                    containerColor = Color(0xFF2196F3), // Mavi
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Tamamlanan",
                    count = tamamlananCount,
                    containerColor = Color(0xFF4CAF50), // Yeşil
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            // 4. Güncellenmiş Filtreleme Çipleri
            val filters = listOf("Hepsi", "Bekleyen", "Devam Eden", "Tamamlanan")
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
fun DashboardCard(title: String, count: Int, containerColor: Color, modifier: Modifier = Modifier) {
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
                fontWeight = FontWeight.SemiBold,
                color = if (containerColor != MaterialTheme.colorScheme.primaryContainer) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (containerColor != MaterialTheme.colorScheme.primaryContainer) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
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

                // 5. Liste Öğelerindeki Rozet Renklerinin Yeni Durumlara Göre Eşlenmesi
                val badgeColor = when (service.status) {
                    "Bekliyor" -> Color(0xFFFF9800)
                    "Yolda" -> Color(0xFF2196F3)
                    "İşleme Başlandı" -> Color(0xFF9C27B0)
                    "Parça Bekleniyor" -> Color(0xFFFBC02D)
                    "Tamamlandı" -> Color(0xFF4CAF50)
                    "İptal" -> Color(0xFFF44336)
                    else -> Color.Gray
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = service.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
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