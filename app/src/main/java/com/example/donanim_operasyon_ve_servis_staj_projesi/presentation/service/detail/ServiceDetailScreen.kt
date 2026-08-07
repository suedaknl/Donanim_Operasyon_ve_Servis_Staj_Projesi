package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    viewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    serviceId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val service = serviceRecords.find { it.id == serviceId }

    // Personel listesi state'i
    val personnelList by personnelViewModel.personnelList.collectAsState()

    // Diyalog ve Snackbar Kontrol State'leri
    var showAssignDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (service == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("İş emri bulunamadı veya silindi.")
        }
        return
    }

    // Atanan personelin ismini bulma mantığı
    val assignedPersonnelName = if (service.assignedPersonnelId != null) {
        personnelList.find { it.id == service.assignedPersonnelId }?.fullName ?: "Personel Bulunamadı (Silinmiş Olabilir)"
    } else {
        "Atanmadı"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("İş Emri Detayları", fontWeight = FontWeight.Bold) },
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

            // --- BİLGİ KARTI ---
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Firma: ${service.companyName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Text("Cihaz: ${service.deviceType} - ${service.deviceModel}")
                    Text("Seri No: ${service.serialNumber}")
                    Text("Lokasyon: ${service.location}")
                    Text("Tarih: ${service.date}")
                    Text("Öncelik: ${service.priority}", color = MaterialTheme.colorScheme.error)

                    // MEVCUT DURUM BİLGİSİ
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mevcut Durum: ${service.status}",
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    // ATANAN PERSONEL BİLGİSİ
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Atanan Personel: $assignedPersonnelName",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // --- ARIZA AÇIKLAMASI KARTI ---
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Arıza Açıklaması", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(service.issueDescription)
                }
            }

            // --- BUTONLAR BÖLÜMÜ ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // 1. PERSONEL ATA BUTONU
                Button(
                    onClick = { showAssignDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personel Ata")
                }

                // 2. DURUM GÜNCELLE BUTONU
                Button(
                    onClick = { showStatusDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Durum Güncelle")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // 3. DÜZENLE BUTONU
                    Button(
                        onClick = { onNavigateToEdit(service.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Düzenle")
                    }

                    // 4. SİL BUTONU
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sil")
                    }
                }
            }
        }
    }

    // --- DİYALOGLAR ---

    // 1. PERSONEL ATA DİYALOĞU
    if (showAssignDialog) {
        var selectedPersonnelId by remember { mutableStateOf(service.assignedPersonnelId) }
        val activePersonnelList = personnelList.filter { it.isActive }

        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Personel Ata", fontWeight = FontWeight.Bold) },
            text = {
                if (activePersonnelList.isEmpty()) {
                    Text("Şu anda sistemde atanabilir aktif personel bulunmuyor.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(activePersonnelList) { personnel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPersonnelId = personnel.id }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedPersonnelId == personnel.id),
                                    onClick = { selectedPersonnelId = personnel.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = personnel.fullName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedRecord = service.copy(assignedPersonnelId = selectedPersonnelId)
                        viewModel.updateRecord(updatedRecord)
                        showAssignDialog = false
                    }
                ) {
                    Text("Ata")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // 2. DURUM GÜNCELLE DİYALOĞU
    if (showStatusDialog) {
        var selectedStatus by remember { mutableStateOf(service.status) }

        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Durum Güncelle", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ServiceStatus.all.forEach { statusOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStatus = statusOption }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedStatus == statusOption),
                                onClick = { selectedStatus = statusOption }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = statusOption)
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
                            viewModel.updateStatus(service.id, selectedStatus)
                        }
                        showStatusDialog = false
                    }
                ) {
                    Text("Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // 3. SİLME ONAY DİYALOĞU
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("İş Emrini Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Bu iş emrini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(service)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}