package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.overtime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.OvertimeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelOvertimeScreen(
    personnelId: Int,
    overtimeViewModel: OvertimeViewModel,
    onNavigateBack: () -> Unit
) {
    val overtimes by overtimeViewModel.personnelOvertimes.collectAsState()

    LaunchedEffect(personnelId) {
        overtimeViewModel.loadPersonnelOvertimes(personnelId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fazla Mesailerim", fontWeight = FontWeight.Bold) },
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
            Text("Geçmiş Fazla Mesai Kayıtları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (overtimes.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Kayıtlı fazla mesai bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(overtimes) { item ->
                        val startStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.startTime))
                        val endStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.endTime))

                        val (statusText, statusColor) = when (item.status) {
                            "APPROVED" -> "Onaylandı" to MaterialTheme.colorScheme.primaryContainer
                            "REJECTED" -> "Reddedildi" to MaterialTheme.colorScheme.errorContainer
                            else -> "Onay Bekliyor" to MaterialTheme.colorScheme.surfaceVariant
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "${item.durationMinutes} Dakika", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusColor
                                    ) {
                                        Text(
                                            text = statusText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (item.serviceRecordId != null) {
                                    Text(text = "İş Emri ID: #${item.serviceRecordId}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(text = "Başlangıç: $startStr", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Bitiş: $endStr", style = MaterialTheme.typography.bodyMedium)

                                if (!item.description.isNullOrBlank()) {
                                    Text(text = "Açıklama: ${item.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}