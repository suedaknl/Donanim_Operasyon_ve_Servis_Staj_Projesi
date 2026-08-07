package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    serviceId: Int? = null,
    onNavigateBack: () -> Unit
) {
    // Form state'leri
    var companyName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Orta") } // Varsayılan öncelik

    var showError by remember { mutableStateOf(false) }

    // Düzenleme modunda orijinal kaydı tutmak için
    var existingRecord by remember { mutableStateOf<ServiceRecord?>(null) }

    val priorities = listOf("Düşük", "Orta", "Yüksek")

    // serviceId varsa mevcut verileri çek ve formu doldur (Pre-fill)
    LaunchedEffect(serviceId) {
        if (serviceId != null) {
            val record = viewModel.getServiceById(serviceId)
            if (record != null) {
                existingRecord = record

                companyName = record.companyName
                deviceType = record.deviceType
                deviceModel = record.deviceModel
                serialNumber = record.serialNumber
                location = record.location
                selectedPriority = record.priority
                issueDescription = record.issueDescription
            }
        }
    }

    // Ekran başlığı dinamik olarak belirlenir
    val screenTitle = if (serviceId == null) "Yeni İş Emri Oluştur" else "İş Emri Düzenle"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
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
            // Firma Adı
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it; showError = false },
                label = { Text("Firma Adı") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Cihaz Tipi ve Modeli
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = deviceType,
                    onValueChange = { deviceType = it; showError = false },
                    label = { Text("Cihaz Tipi") },
                    leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it; showError = false },
                    label = { Text("Cihaz Modeli") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Seri No
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it; showError = false },
                label = { Text("Seri No") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Lokasyon
            OutlinedTextField(
                value = location,
                onValueChange = { location = it; showError = false },
                label = { Text("Lokasyon") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Öncelik Seçimi (Renk Kodlu AssistChip / FilterChip)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Öncelik Seviyesi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { priority ->
                        val isSelected = selectedPriority == priority

                        // Önceliğe göre dinamik renkler (Yüksek -> Kırmızı, Orta -> Turuncu, Düşük -> Yeşil)
                        val priorityColor = when (priority) {
                            "Yüksek" -> Color(0xFFC62828)
                            "Orta" -> Color(0xFFEF6C00)
                            else -> Color(0xFF2E7D32)
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority, fontWeight = FontWeight.Medium) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize), tint = priorityColor) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = priorityColor.copy(alpha = 0.15f),
                                selectedLabelColor = priorityColor
                            )
                        )
                    }
                }
            }

            // Arıza Açıklaması
            OutlinedTextField(
                value = issueDescription,
                onValueChange = { issueDescription = it; showError = false },
                label = { Text("Arıza Açıklaması") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            if (showError) {
                Text(
                    text = "Lütfen tüm alanları eksiksiz doldurun.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kaydet Butonu (Ekleme ve Güncelleme Mantığı Bir Arada)
            Button(
                onClick = {
                    if (companyName.isBlank() || deviceType.isBlank() || deviceModel.isBlank() ||
                        serialNumber.isBlank() || location.isBlank() || issueDescription.isBlank()) {
                        showError = true
                    } else {
                        if (existingRecord != null) {
                            // DÜZENLEME MODU: Mevcut kaydın ID, tarih ve durumunu koruyarak kopyala
                            val updatedRecord = existingRecord!!.copy(
                                companyName = companyName.trim(),
                                deviceType = deviceType.trim(),
                                deviceModel = deviceModel.trim(),
                                serialNumber = serialNumber.trim(),
                                location = location.trim(),
                                priority = selectedPriority,
                                issueDescription = issueDescription.trim()
                            )
                            viewModel.updateRecord(updatedRecord)
                        } else {
                            // YENİ EKLEME MODU: Yeni bir ServiceRecord oluştur
                            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                            val newRecord = ServiceRecord(
                                companyName = companyName.trim(),
                                deviceType = deviceType.trim(),
                                deviceModel = deviceModel.trim(),
                                serialNumber = serialNumber.trim(),
                                location = location.trim(),
                                priority = selectedPriority,
                                issueDescription = issueDescription.trim(),
                                status = ServiceStatus.BEKLIYOR, // Sadece yeni kayıtta durumu sıfırdan belirliyoruz
                                date = currentDate
                            )
                            viewModel.insertRecord(newRecord)
                        }

                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kaydet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}