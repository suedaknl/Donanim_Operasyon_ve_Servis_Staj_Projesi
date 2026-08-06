package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceScreen(
    viewModel: ServiceViewModel,
    onNavigateBack: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Orta") } // Varsayılan öncelik

    var showError by remember { mutableStateOf(false) }

    val priorities = listOf("Düşük", "Orta", "Yüksek")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni İş Emri Oluştur", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = companyName, onValueChange = { companyName = it; showError = false },
                label = { Text("Firma Adı") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = deviceType, onValueChange = { deviceType = it; showError = false },
                    label = { Text("Cihaz Tipi") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = deviceModel, onValueChange = { deviceModel = it; showError = false },
                    label = { Text("Cihaz Modeli") }, modifier = Modifier.weight(1f), singleLine = true
                )
            }

            OutlinedTextField(
                value = serialNumber, onValueChange = { serialNumber = it; showError = false },
                label = { Text("Seri No") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            OutlinedTextField(
                value = location, onValueChange = { location = it; showError = false },
                label = { Text("Lokasyon") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Text("Öncelik", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                priorities.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = { Text(priority) }
                    )
                }
            }

            OutlinedTextField(
                value = issueDescription, onValueChange = { issueDescription = it; showError = false },
                label = { Text("Arıza Açıklaması") }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 5
            )

            if (showError) {
                Text(text = "Lütfen tüm alanları doldurun.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (companyName.isBlank() || deviceType.isBlank() || deviceModel.isBlank() ||
                        serialNumber.isBlank() || location.isBlank() || issueDescription.isBlank()) {
                        showError = true
                    } else {
                        // Otomatik Tarih Oluşturma
                        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                        val newRecord = ServiceRecord(
                            companyName = companyName.trim(),
                            deviceType = deviceType.trim(),
                            deviceModel = deviceModel.trim(),
                            serialNumber = serialNumber.trim(),
                            location = location.trim(),
                            priority = selectedPriority,
                            issueDescription = issueDescription.trim(),
                            status = ServiceStatus.BEKLIYOR, // Otomatik Bekliyor
                            date = currentDate // Otomatik Tarih
                        )
                        viewModel.insertRecord(newRecord)
                        onNavigateBack() // Dashboard'a dön
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kaydet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}