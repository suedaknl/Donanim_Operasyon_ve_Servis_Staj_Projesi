package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.login // Kendi paket yapına göre güncelle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.AppDatabase
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.ServiceRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneUserFormScreen(
    onLogOut: () -> Unit
) {
    // Veritabanı ve ViewModel Bağlantısı (Dünkü yapı ile aynı)
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = ServiceRepository(database.serviceDao())
    val viewModel: ServiceViewModel = viewModel(factory = ServiceViewModelFactory(repository))

    // Form State'leri
    var companyName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var issueDesc by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf("") }

    // Başarı Dialog State'i
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Yeni İş Emri Oluştur", fontWeight = FontWeight.Bold)
                        Text("Saha Personeli Portali", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Ekran küçükse kaydırılabilir olsun
        ) {
            Text(
                text = "Lütfen arızalı donanım veya yeni kurulum talebi için aşağıdaki bilgileri eksiksiz doldurun.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Firma Adı *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = deviceType, onValueChange = { deviceType = it }, label = { Text("Cihaz Tipi / Modeli *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("Seri Numarası (Boş bırakılırsa otomatik üretilir)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Lokasyon / Şube *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Öncelik Seviyesi:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Normal", "Yüksek", "Kritik").forEach { level ->
                    FilterChip(
                        selected = priority == level,
                        onClick = { priority = level },
                        label = { Text(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = issueDesc,
                onValueChange = { issueDesc = it },
                label = { Text("Arıza Açıklaması *") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            if (validationMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = validationMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val missingFields = mutableListOf<String>()
                    if (companyName.isBlank()) missingFields.add("Firma Adı")
                    if (deviceType.isBlank()) missingFields.add("Cihaz Tipi")
                    if (location.isBlank()) missingFields.add("Lokasyon")
                    if (issueDesc.isBlank()) missingFields.add("Arıza Açıklaması")

                    if (missingFields.isNotEmpty()) {
                        validationMessage = "Lütfen zorunlu alanları doldurun: ${missingFields.joinToString(", ")}"
                    } else {
                        validationMessage = ""
                        val finalSerial = if (serialNumber.isBlank()) "SN-${(1000..9999).random()}" else serialNumber
                        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

                        val newRecord = ServiceRecord(
                            companyName = companyName,
                            deviceType = deviceType,
                            serialNumber = finalSerial,
                            location = location,
                            priority = priority,
                            issueDescription = issueDesc,
                            status = ServiceStatus.BEKLIYOR,
                            date = currentDate
                        )

                        // Veritabanına kaydet
                        viewModel.insertRecord(newRecord)

                        // Başarı mesajını göster
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("İş Emrini Kaydet", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }

    // Başarılı Kayıt Dialog Ekranı
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Kullanıcı dışarı tıklayarak kapatamasın, butona bassın */ },
            title = { Text("Başarılı!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = { Text("Yeni iş emri başarıyla oluşturuldu ve Admin paneline iletildi.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        // Formu temizle
                        companyName = ""
                        deviceType = ""
                        serialNumber = ""
                        location = ""
                        priority = "Normal"
                        issueDesc = ""
                    }
                ) {
                    Text("Tamam")
                }
            }
        )
    }
}