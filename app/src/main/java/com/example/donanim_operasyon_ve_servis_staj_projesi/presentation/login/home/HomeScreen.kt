package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.AppDatabase
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val title = "Aktif Operasyonlar ve Servisler"

    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = ServiceRepository(database.serviceDao())
    val viewModel: ServiceViewModel = viewModel(factory = ServiceViewModelFactory(repository))

    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val showAddDialog = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog.value = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Kayıt Ekle")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (serviceRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz bir iş emri bulunmuyor.\nSağ alttaki butondan ekleyebilirsiniz.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        items(serviceRecords) { record ->
                            ServiceRecordCard(
                                record = record,
                                onDelete = { viewModel.deleteRecord(record) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        if (showAddDialog.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AddServiceForm(
                    onDismiss = { showAddDialog.value = false },
                    onSave = { newRecord ->
                        viewModel.insertRecord(newRecord)
                        showAddDialog.value = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddServiceForm(onDismiss: () -> Unit, onSave: (ServiceRecord) -> Unit) {
    var companyName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var issueDesc by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Yeni İş Emri Oluştur",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Firma Adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = deviceType,
                onValueChange = { deviceType = it },
                label = { Text("Cihaz Tipi (POS, Tablet vb.)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Lokasyon / Şube") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = priority,
                onValueChange = { priority = it },
                label = { Text("Öncelik (Acil, Normal)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = issueDesc,
                onValueChange = { issueDesc = it },
                label = { Text("Arıza Açıklaması") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val newRecord = ServiceRecord(
                            companyName = companyName,
                            deviceType = deviceType,
                            serialNumber = "SN-${(1000..9999).random()}",
                            location = location,
                            priority = priority,
                            issueDescription = issueDesc,
                            status = "Bekliyor",
                            date = "04.08.2026"
                        )
                        onSave(newRecord)
                    }
                ) {
                    Text("Kaydet")
                }
            }
        }
    }
}

@Composable
fun ServiceRecordCard(record: ServiceRecord, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.companyName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // Silme butonu eklendi
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Kaydı Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Cihaz: ${record.deviceType} | Seri No: ${record.serialNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = "Lokasyon: ${record.location} | Öncelik: ${record.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.issueDescription,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Durum: ${record.status}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}