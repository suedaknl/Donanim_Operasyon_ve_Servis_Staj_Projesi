package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelManagementScreen(
    personnelViewModel: PersonnelViewModel,
    onNavigateBack: () -> Unit,
    onPersonnelWorkClick: (Int) -> Unit
) {
    val personnelList by personnelViewModel.personnelList.collectAsState()
    val services by personnelViewModel.serviceRecords.collectAsState(initial = emptyList())
    val leaves by personnelViewModel.leaveRequests.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

    // Gerçek veriler üzerinden operasyonel durumların hesaplanması
    val personnelUiModels = personnelList.map { p ->
        val activeJobs = services.filter { it.assignedPersonnelId == p.id && !it.status.toString().contains("TAMAMLANDI") && !it.status.toString().contains("IPTAL") }
        val isLeave = leaves.any { it.personnelId == p.id && it.status.equals("APPROVED", true) && it.startDate <= todayStr && it.endDate >= todayStr }

        val status = when {
            isLeave -> "İzinli"
            activeJobs.isNotEmpty() -> "Aktif İşte"
            !p.isActive -> "Pasif"
            else -> "Uygun"
        }

        PersonnelUiModel(
            personnel = p,
            availabilityStatus = status,
            activeJobCount = activeJobs.size
        )
    }

    val filteredList = personnelUiModels.filter {
        it.personnel.fullName.contains(searchQuery, ignoreCase = true) ||
                it.personnel.role.contains(searchQuery, ignoreCase = true)
    }

    val totalCount = personnelList.size
    val activeCount = personnelList.count { it.isActive }
    val leaveCount = personnelUiModels.count { it.availabilityStatus == "İzinli" }
    val workingCount = personnelUiModels.count { it.availabilityStatus == "Aktif İşte" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personel Yönetimi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Üst Kompakt Operasyonel Özet (4 Kriter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ManagementStatBox(modifier = Modifier.weight(1f), title = "Toplam", count = totalCount.toString())
                ManagementStatBox(modifier = Modifier.weight(1f), title = "Aktif", count = activeCount.toString(), color = Color(0xFF16A34A))
                ManagementStatBox(modifier = Modifier.weight(1f), title = "İzinli", count = leaveCount.toString(), color = Color(0xFFD97706))
                ManagementStatBox(modifier = Modifier.weight(1f), title = "Sahada", count = workingCount.toString(), color = MaterialTheme.colorScheme.primary)
            }

            // Arama Çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { Text("Personel ara (Ad, görev)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Personel Çalışma Listesi
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { model ->
                    val p = model.personnel
                    val statusColor = when (model.availabilityStatus) {
                        "İzinli" -> Color(0xFFD97706)
                        "Aktif İşte" -> MaterialTheme.colorScheme.primary
                        "Pasif" -> Color(0xFFDC2626)
                        else -> Color(0xFF16A34A)
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonnelWorkClick(p.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = p.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = p.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (p.isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (p.isActive) "Aktif" else "Pasif",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (p.isActive) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = statusColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = model.availabilityStatus,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (model.activeJobCount > 0) {
                                    Text(
                                        text = "Aktif İş: ${model.activeJobCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

data class PersonnelUiModel(
    val personnel: com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel,
    val availabilityStatus: String,
    val activeJobCount: Int
)

@Composable
fun ManagementStatBox(modifier: Modifier = Modifier, title: String, count: String, color: Color = MaterialTheme.colorScheme.primary) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}