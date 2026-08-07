package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serviceList: List<ServiceRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    onNavigateToPersonnel: () -> Unit,
    onNavigateToAddService: () -> Unit,
    onServiceClick: (ServiceRecord) -> Unit,
    onLogOut: () -> Unit
) {
    // Dashboard hesaplamaları (Arama/Filtreden bağımsız, tüm liste üzerinden hesaplanır)
    val totalCount = serviceList.size
    val pendingCount = serviceList.count { it.status == ServiceStatus.BEKLIYOR || it.status == ServiceStatus.PARCA_BEKLENIYOR }
    val inProgressCount = serviceList.count { it.status == ServiceStatus.YOLDA || it.status == ServiceStatus.ISLEME_BASLANDI }
    val completedCount = serviceList.count { it.status == ServiceStatus.TAMAMLANDI }

// Arama ve filtrelemenin birlikte yapıldığı liste
    val filteredList = serviceList.filter { record ->

        // 1. Arama kontrolü (Büyük/küçük harf duyarsız ve boşluksuz)
        val q = searchQuery.trim().lowercase()
        val matchesSearch = if (q.isEmpty()) {
            true
        } else {
            record.companyName.lowercase().contains(q) ||
                    record.deviceType.lowercase().contains(q) ||
                    record.deviceModel.lowercase().contains(q) ||
                    record.serialNumber.lowercase().contains(q)
        }

        // 2. Kategori/Filtre kontrolü (Tüm varyasyonlar desteklendi)
        val matchesFilter = when (selectedFilter) {
            "Hepsi", "Tümü", "", null -> true
            "Bekleyen" -> record.status == ServiceStatus.BEKLIYOR || record.status == ServiceStatus.PARCA_BEKLENIYOR
            "Devam Eden" -> record.status == ServiceStatus.YOLDA || record.status == ServiceStatus.ISLEME_BASLANDI
            "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
            else -> record.status == selectedFilter
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Teknik Servis Paneli", fontWeight = FontWeight.Bold)
                        Text("Yönetim ve Operasyon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPersonnel) {
                        Icon(Icons.Default.Group, contentDescription = "Personel Yönetimi")
                    }
                    IconButton(onClick = onLogOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Çıkış Yap")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddService,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yeni İş Emri") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Firma, cihaz veya seri no ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DashboardCard(title = "Toplam", count = totalCount, color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Dashboard)
                }
                item {
                    DashboardCard(title = "Bekleyen", count = pendingCount, color = Color(0xFFE65100), icon = Icons.Default.HourglassEmpty)
                }
                item {
                    DashboardCard(title = "Devam Eden", count = inProgressCount, color = Color(0xFF0277BD), icon = Icons.Default.Sync)
                }
                item {
                    DashboardCard(title = "Tamamlanan", count = completedCount, color = Color(0xFF2E7D32), icon = Icons.Default.CheckCircle)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filtre grupları
// Filtre grupları (null yerine doğrudan "Hepsi" gönderiyoruz)
                val filters = listOf(
                    "Hepsi" to "Hepsi",
                    "Bekleyen" to "Bekleyen",
                    "Devam Eden" to "Devam Eden",
                    "Tamamlanan" to "Tamamlanan",
                    ServiceStatus.IPTAL to ServiceStatus.IPTAL
                )

                items(filters) { (status, label) ->

                    // Eğer label "Hepsi" ise; null, boş veya "Tümü" olma durumlarında da seçili görünsün
                    val isSelected = if (label == "Hepsi") {
                        selectedFilter.isNullOrEmpty() || selectedFilter == "Hepsi" || selectedFilter == "Tümü"
                    } else {
                        selectedFilter == status
                    }

                    val chipColor = when (label) {
                        "Bekleyen" -> Color(0xFFE65100)
                        "Devam Eden" -> Color(0xFF0277BD)
                        "Tamamlanan" -> Color(0xFF2E7D32)
                        ServiceStatus.IPTAL -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(status) }, // Artık null değil "Hepsi" gidiyor
                        label = {
                            Text(
                                label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    tint = chipColor
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.15f),
                            selectedLabelColor = chipColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ham serviceList yerine filteredList kullanılıyor
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kayıtlı iş emri bulunamadı.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList) { record ->
                        ServiceCardItem(record = record, onClick = { onServiceClick(record) })
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, count: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ServiceCardItem(record: ServiceRecord, onClick: () -> Unit) {
    val statusColor = when (record.status) {
        ServiceStatus.BEKLIYOR -> Color(0xFFE65100)
        ServiceStatus.YOLDA -> Color(0xFFEF6C00)
        ServiceStatus.ISLEME_BASLANDI -> Color(0xFF0277BD)
        ServiceStatus.PARCA_BEKLENIYOR -> Color(0xFF7B1FA2)
        ServiceStatus.TAMAMLANDI -> Color(0xFF2E7D32)
        ServiceStatus.IPTAL -> Color(0xFFC62828)
        else -> Color.Gray
    }

    val priorityColor = when (record.priority) {
        "Yüksek" -> Color(0xFFC62828)
        "Orta" -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.priority,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = priorityColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${record.deviceType} - ${record.deviceModel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}