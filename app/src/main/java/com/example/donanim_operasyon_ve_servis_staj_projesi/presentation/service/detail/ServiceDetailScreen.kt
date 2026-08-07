package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    viewModel: ServiceViewModel,
    serviceId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val serviceList by viewModel.serviceRecords.collectAsState()
    val service = serviceList.find { it.id == serviceId }

    // Dialog state'leri
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showStatusDialog by rememberSaveable { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf("") }

    // Snackbar (Bildirim mesajı) state'leri
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val statusOptions = listOf("Bekliyor", "Yolda", "İşleme Başlandı", "Parça Bekleniyor", "Tamamlandı", "İptal")

    if (service == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val statusColor = when (service.status) {
        "Bekliyor" -> Color(0xFFFF9800)
        "Yolda" -> Color(0xFF2196F3)
        "İşleme Başlandı" -> Color(0xFF9C27B0)
        "Parça Bekleniyor" -> Color(0xFFFBC02D)
        "Tamamlandı" -> Color(0xFF4CAF50)
        "İptal" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("İş Emri Detayı", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri Dön")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "İş No: #${service.id}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = statusColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = service.status,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DetailRow(label = "Firma", value = service.companyName)
                    DetailRow(label = "Cihaz Tipi", value = service.deviceType)
                    DetailRow(label = "Cihaz Modeli", value = service.deviceModel)
                    DetailRow(label = "Seri No", value = service.serialNumber)
                    DetailRow(label = "Lokasyon", value = service.location)
                    DetailRow(label = "Öncelik", value = service.priority)
                    DetailRow(label = "Oluşturulma Tarihi", value = service.date)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Arıza Açıklaması", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = service.issueDescription,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    selectedStatus = service.status
                    showStatusDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Durum Güncelle")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToEdit(service.id) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Düzenle")
                }

                Button(
                    onClick = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Durum Güncelleme Diyaloğu
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("İş Emri Durumunu Güncelle", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    statusOptions.forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStatus = status }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedStatus == status),
                                onClick = { selectedStatus = status }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = status, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStatus == service.status) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("İş emrinin durumu zaten aynı.")
                            }
                        } else {
                            val updatedRecord = service.copy(status = selectedStatus)
                            viewModel.updateRecord(updatedRecord)

                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("İş emri durumu güncellendi.")
                            }
                        }
                        showStatusDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStatusDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Silme Onay Diyaloğu
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("İş Emrini Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Bu iş emrini silmek istediğinize emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteRecord(service)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}