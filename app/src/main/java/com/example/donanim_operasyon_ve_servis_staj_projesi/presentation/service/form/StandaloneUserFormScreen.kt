package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.components.PersonnelJobPoolSection

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
    firebaseUid: String?,
    getOperationalStatus: (ServiceRecord) -> String,
    localPersonnelId: Int
) {
    val context = LocalContext.current
    val pagedList by serviceViewModel.pagedPersonnelServiceRecords.collectAsState()
    val currentPage by serviceViewModel.currentPage.collectAsState()
    val totalPages by serviceViewModel.totalPages.collectAsState()
    val poolJobs by serviceViewModel.poolJobs.collectAsState()

    // Giriş yapan personelin güncel bilgilerini buluyoruz (Bulunamazsa local ID üzerinden güvenli fallback)
    val currentPersonnel = personnelList.find { it.id == localPersonnelId || it.firebaseUid == firebaseUid }

    var innerSelectedTab by remember { mutableStateOf("Bana Atananlar") }
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Başlık
        Text(
            text = "İş Emirleri",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Primary Tab Row: [Bana Atananlar] | [İş Havuzu]
        TabRow(
            selectedTabIndex = if (innerSelectedTab == "Bana Atananlar") 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = innerSelectedTab == "Bana Atananlar",
                onClick = { innerSelectedTab = "Bana Atananlar" },
                text = { Text("Bana Atananlar (${serviceList.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = innerSelectedTab == "İş Havuzu",
                onClick = { innerSelectedTab = "İş Havuzu" },
                text = { Text("İş Havuzu (${poolJobs.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arama Çubuğu + Filtre Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("İş emri ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .size(52.dp)
                    .padding(top = 4.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Filtreler")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (innerSelectedTab == "İş Havuzu") {
            Text(
                text = "Toplam ${poolJobs.size} havuz görevi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            PersonnelJobPoolSection(
                poolJobs = poolJobs,
                getOperationalStatus = getOperationalStatus, // <--- Eksik virgül eklendi
                onClaimJob = { service: ServiceRecord ->
                    Log.d("POOL_CLAIM", "Button clicked for serviceId: ${service.id}, firestoreId: ${service.firestoreId}")

                    val personnelId = currentPersonnel?.id ?: localPersonnelId
                    val personnelName = currentPersonnel?.fullName ?: "Personel"
                    val personnelUid = currentPersonnel?.firebaseUid ?: (firebaseUid ?: "")

                    if (personnelId <= 0) {
                        Toast.makeText(context, "Hata: Personel ID bulunamadı.", Toast.LENGTH_SHORT).show()
                        Log.e("POOL_CLAIM", "Error: personnelId is invalid ($personnelId)")
                        return@PersonnelJobPoolSection
                    }

                    // Aktif iş kontrolü (Üzerinde ISLEME_BASLANDI veya YOLDA olan iş var mı?)
                    val hasActiveJob = serviceList.any {
                        it.status == ServiceStatus.ISLEME_BASLANDI || it.status == ServiceStatus.YOLDA
                    }

                    Log.d("POOL_CLAIM", "Calling viewmodel claimPoolJob -> personnelId: $personnelId, name: $personnelName")

                    // ViewModel üzerinden gerçek claimPoolJob fonksiyonunu tetikliyoruz
                    serviceViewModel.claimPoolJob(
                        serviceId = service.id,
                        firestoreId = service.firestoreId ?: "",
                        personnelId = personnelId,
                        personnelName = personnelName,
                        personnelUid = personnelUid,
                        hasActiveJob = hasActiveJob,
                        plannedDateStr = service.plannedDate ?: service.date,
                        isOnLeave = false
                    )
                }
            )
        } else {
            Text(
                text = if (totalPages > 1) "Toplam ${serviceList.size} iş emri (Sayfa $currentPage / $totalPages)" else "Toplam ${serviceList.size} iş emri",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (pagedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(
                            text = "Bu kriterlere uygun iş emri bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    items(pagedList) { service ->
                        val (statusBg, statusFg) = when (service.status) {
                            ServiceStatus.TAMAMLANDI -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
                            ServiceStatus.YOLDA -> Color(0xFFDBEAFE) to Color(0xFF2563EB)
                            ServiceStatus.ISLEME_BASLANDI, ServiceStatus.PARCA_BEKLENIYOR -> Color(0xFFFEF3C7) to Color(0xFFD97706)
                            else -> Color(0xFFF1F5F9) to Color(0xFF475569)
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onServiceClick(service) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = service.companyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = service.priority,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${service.deviceType} - ${service.deviceModel}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = service.date.take(10),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        color = statusBg,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = service.status,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = statusFg
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { serviceViewModel.setPage(currentPage - 1) },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki Sayfa")
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        for (i in 1..totalPages) {
                            val isCurrent = i == currentPage
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(30.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { serviceViewModel.setPage(i) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = i.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { serviceViewModel.setPage(currentPage + 1) },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki Sayfa")
                        }
                    }
                }
            }
        }
    }

    // Filtre BottomSheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("İş Emri Filtreleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onClearFilters(); showFilterSheet = false }) {
                        Text("Temizle", color = MaterialTheme.colorScheme.error)
                    }
                }

                Text("Durum Filtresi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                val filterOptions = listOf("Tümü", "Bekleyen", "Yolda", "İşlemde", "Tamamlanan")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filterOptions.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uygula")
                }
            }
        }
    }
}