package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service

import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceScreen(
    viewModel: ServiceViewModel,
    serviceId: Int? = null,
    returnedLatitude: Double? = null,
    returnedLongitude: Double? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLocationPicker: (Double?, Double?) -> Unit = { _, _ -> },
    onLocationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current

    // Tüm form state'leri rememberSaveable ile korundu (Haritaya gidip gelince silinmez)
    var companyName by rememberSaveable { mutableStateOf("") }
    var deviceType by rememberSaveable { mutableStateOf("") }
    var deviceModel by rememberSaveable { mutableStateOf("") }
    var serialNumber by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var issueDescription by rememberSaveable { mutableStateOf("") }
    var selectedPriority by rememberSaveable { mutableStateOf("Orta") }

    // İletişim, Adres ve Koordinat State'leri de rememberSaveable yapıldı
    var contactPerson by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var plannedDate by rememberSaveable { mutableStateOf("") }

    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }

    var showError by remember { mutableStateOf(false) }
    var showLocationWarningDialog by remember { mutableStateOf(false) }

    // Kiritk Düzeltme: Haritadan dönüldüğünde DB'den eski (boş) verilerin tekrar çekilip
    // kullanıcı seçimlerinin ezilmesini engellemek için kontrol flag'i
    var isInitialized by rememberSaveable { mutableStateOf(false) }

    val priorities = listOf("Düşük", "Orta", "Yüksek")

    // Çoğaltma (Duplicate) kontrolü: serviceId negatif gönderildiyse bu bir çoğaltma işlemidir
    val isDuplicating = serviceId != null && serviceId < 0
    val actualServiceId = if (isDuplicating && serviceId != null) -serviceId else serviceId

    // Haritadan dönen koordinatları yakalama ve Reverse Geocoding ile Adresi Otomatik Doldurma
    LaunchedEffect(returnedLatitude, returnedLongitude) {
        if (returnedLatitude != null && returnedLongitude != null) {
            latitude = returnedLatitude
            longitude = returnedLongitude

            // Android Geocoder Main Thread'i kilitlememesi için IO thread'ine alındı
            launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale("tr", "TR"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(returnedLatitude, returnedLongitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val resolvedAddress = addresses[0].getAddressLine(0)
                                if (!resolvedAddress.isNullOrBlank()) {
                                    address = resolvedAddress
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(returnedLatitude, returnedLongitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val resolvedAddress = addresses[0].getAddressLine(0)
                            if (!resolvedAddress.isNullOrBlank()) {
                                address = resolvedAddress
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onLocationConsumed()
        }
    }

    // SADECE İLK AÇILIŞTA (isInitialized == false) DB'den verileri çeker
    // Böylece haritadan dönünce koordinatlar sıfırlanmaz.
    LaunchedEffect(actualServiceId) {
        if (!isInitialized) {
            if (actualServiceId != null && actualServiceId != 0) {
                val record = viewModel.getServiceById(actualServiceId)
                if (record != null) {
                    companyName = record.companyName
                    deviceType = record.deviceType
                    deviceModel = record.deviceModel
                    serialNumber = record.serialNumber
                    location = record.location
                    selectedPriority = record.priority
                    issueDescription = record.issueDescription

                    contactPerson = record.contactPerson ?: ""
                    contactPhone = record.contactPhone ?: ""
                    address = record.address ?: ""
                    plannedDate = record.plannedDate ?: ""

                    // Eğer çoğaltma değilse koordinatları da getir (Çoğaltmaysa yeni konum gerekebilir)
                    latitude = record.latitude
                    longitude = record.longitude
                }
            }
            isInitialized = true
        }
    }

    val screenTitle = when {
        isDuplicating -> "İş Emrini Çoğalt"
        serviceId == null || serviceId == 0 -> "Yeni İş Emri Oluştur"
        else -> "İş Emri Düzenle"
    }

    // Kaydetme işlemini gerçekleştiren ortak fonksiyon
    val performSave: () -> Unit = {
        val finalCompany = companyName.trim()
        val finalDevice = deviceType.trim()
        val finalModel = deviceModel.trim()
        val finalSerial = serialNumber.trim()
        val finalLocation = location.trim()
        val finalIssue = issueDescription.trim()
        val finalContactPerson = contactPerson.trim().takeIf { it.isNotBlank() }
        val finalContactPhone = contactPhone.trim().takeIf { it.isNotBlank() }
        val finalAddress = address.trim().takeIf { it.isNotBlank() }
        val finalPlannedDate = plannedDate.trim().takeIf { it.isNotBlank() }

        // Düzenleme modunda veritabanından güncel kaydı bularak güvenli güncelleme yaparız
        if (actualServiceId != null && actualServiceId != 0 && !isDuplicating) {
            val existing = viewModel.getServiceById(actualServiceId)
            if (existing != null) {
                val updatedRecord = existing.copy(
                    companyName = finalCompany,
                    deviceType = finalDevice,
                    deviceModel = finalModel,
                    serialNumber = finalSerial,
                    location = finalLocation,
                    priority = selectedPriority,
                    issueDescription = finalIssue,
                    contactPerson = finalContactPerson,
                    contactPhone = finalContactPhone,
                    address = finalAddress,
                    plannedDate = finalPlannedDate,
                    latitude = latitude,
                    longitude = longitude
                )
                viewModel.updateRecord(updatedRecord)
            }
        } else {
            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val newRecord = ServiceRecord(
                companyName = finalCompany,
                deviceType = finalDevice,
                deviceModel = finalModel,
                serialNumber = finalSerial,
                location = finalLocation,
                priority = selectedPriority,
                issueDescription = finalIssue,
                status = ServiceStatus.BEKLIYOR,
                date = currentDate,
                contactPerson = finalContactPerson,
                contactPhone = finalContactPhone,
                address = finalAddress,
                plannedDate = finalPlannedDate,
                latitude = latitude,
                longitude = longitude
            )
            viewModel.insertRecord(newRecord)
        }
        onNavigateBack()
    }

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
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it; showError = false },
                label = { Text("Firma Adı") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contactPerson,
                onValueChange = { contactPerson = it },
                label = { Text("Yetkili Kişi") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Yetkili Telefon") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

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

            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it; showError = false },
                label = { Text("Seri No") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it; showError = false },
                label = { Text("Lokasyon") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("İş Konumu (Harita)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            if (latitude != null && longitude != null) {
                                Text("📍 Konum seçildi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                                Text(String.format(Locale.US, "%.4f, %.4f", latitude, longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Henüz harita konumu seçilmedi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        Button(
                            onClick = { onNavigateToLocationPicker(latitude, longitude) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (latitude != null) "Konumu Değiştir" else "Haritadan Seç")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Açık Adres (Otomatik / Manuel)") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = plannedDate,
                onValueChange = { plannedDate = it },
                label = { Text("Planlanan Ziyaret Tarihi / Saati") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

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
                    text = "Lütfen tüm zorunlu alanları eksiksiz doldurun.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (companyName.isBlank() || deviceType.isBlank() || deviceModel.isBlank() ||
                        serialNumber.isBlank() || location.isBlank() || issueDescription.isBlank()) {
                        showError = true
                    } else if (latitude == null || longitude == null) {
                        showLocationWarningDialog = true
                    } else {
                        performSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isDuplicating) "Yeni İş Emri Oluştur" else "Kaydet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLocationWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLocationWarningDialog = false },
            title = { Text("Konum Seçilmedi", fontWeight = FontWeight.Bold) },
            text = { Text("Konum seçilmedi. Bu iş emri haritada gösterilemeyecek ve GPS doğrulaması uygulanamayacak. Yine de devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationWarningDialog = false
                        performSave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yine de Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationWarningDialog = false }) {
                    Text("Geri Dön & Seç")
                }
            }
        )
    }
}