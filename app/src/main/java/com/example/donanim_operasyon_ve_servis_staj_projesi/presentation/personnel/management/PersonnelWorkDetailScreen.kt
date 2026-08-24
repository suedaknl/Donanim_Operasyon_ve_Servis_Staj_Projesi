package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.donanim_operasyon_ve_servis_staj_projesi.domain.usecase.personnel.PersonnelDetailSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelWorkDetailScreen(
    personnelName: String,
    personnelRole: String = "",
    summary: PersonnelDetailSummary?,
    shifts: List<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity> = emptyList(),
    leaves: List<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity> = emptyList(),
    overtimes: List<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity> = emptyList(),
    onNavigateBack: () -> Unit,
    onNavigateToServiceDetail: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Genel", "Mesai & İzin", "Performans")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(personnelName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Medium) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> GeneralTabContent(personnelName = personnelName, personnelRole = personnelRole, summary = summary, onNavigateToServiceDetail = onNavigateToServiceDetail)
                1 -> WorkAndLeaveTabContent(shifts = shifts, leaves = leaves, overtimes = overtimes)
                2 -> PerformanceTabContent(summary = summary, onNavigateToServiceDetail = onNavigateToServiceDetail)
            }
        }
    }
}

@Composable
fun GeneralTabContent(
    personnelName: String,
    personnelRole: String,
    summary: PersonnelDetailSummary?,
    onNavigateToServiceDetail: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = personnelName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (personnelRole.isNotBlank()) {
                        Text(text = personnelRole, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Operasyonel Uygunluk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Durum: ${summary?.currentAvailability ?: "Yükleniyor..."}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(text = summary?.availabilityReason ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        summary?.activeService?.let { active ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Aktif Görev Özeti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(text = "Firma: ${active.companyName}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Lokasyon: ${active.location}", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { onNavigateToServiceDetail(active.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İş Detayını Gör")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkAndLeaveTabContent(
    shifts: List<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity>,
    leaves: List<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity>,
    overtimes: List<com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.OvertimeEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vardiya Bölümü
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Vardiya Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (shifts.isEmpty()) {
                        Text(text = "Kayıtlı vardiya bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        shifts.take(3).forEach { shift ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "• Tarih: ${shift.shiftDate}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${shift.startTime} - ${shift.endTime}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // İzin Bölümü (APPROVED ve REJECTED gösterilir, PENDING hariç)
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "İzin Geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val concludedLeaves = leaves.filter {
                        it.status.equals("APPROVED", ignoreCase = true) ||
                                it.status.equals("REJECTED", ignoreCase = true)
                    }
                    if (concludedLeaves.isEmpty()) {
                        Text(text = "Sonuçlanmış izin kaydı bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        concludedLeaves.take(5).forEach { leave ->
                            val isApproved = leave.status.equals("APPROVED", ignoreCase = true)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "• ${leave.leaveType} İzni", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Surface(
                                        color = if (isApproved) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isApproved) "Onaylandı" else "Reddedildi",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isApproved) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(text = "${leave.startDate} / ${leave.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (!isApproved && !leave.adminNote.isNullOrBlank()) {
                                    Text(
                                        text = "Red Nedeni: ${leave.adminNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fazla Mesai Bölümü
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Fazla Mesai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (overtimes.isEmpty()) {
                        Text(text = "Kayıtlı fazla mesai bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        overtimes.take(3).forEach { ot ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "• Açıklama: ${ot.description}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = ot.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceTabContent(
    summary: PersonnelDetailSummary?,
    onNavigateToServiceDetail: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "İş İstatistikleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WorkStatItem("Toplam", "${summary?.totalAssignedJobs ?: 0}")
                        WorkStatItem("Bekleyen", "${summary?.pendingJobs ?: 0}")
                        WorkStatItem("İşlemde", "${summary?.inProgressJobs ?: 0}")
                        WorkStatItem("Tamamlanan", "${summary?.completedJobs ?: 0}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tamamlama Oranı: %${String.format("%.1f", summary?.completionRate ?: 0f)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { (summary?.completionRate ?: 0f) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
        }

        summary?.activeService?.let { active ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Mevcut Aktif Görev", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(text = "Firma: ${active.companyName}")
                        Text(text = "Cihaz: ${active.deviceType}")
                        Text(text = "Lokasyon: ${active.location}")
                        Button(
                            onClick = { onNavigateToServiceDetail(active.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İş Detayını Gör")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Son Aktiviteler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (summary?.recentActivities.isNullOrEmpty()) {
                        Text(text = "Henüz kayıtlı aktivite bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        summary?.recentActivities?.forEach { act ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• ${act.title}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = act.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}