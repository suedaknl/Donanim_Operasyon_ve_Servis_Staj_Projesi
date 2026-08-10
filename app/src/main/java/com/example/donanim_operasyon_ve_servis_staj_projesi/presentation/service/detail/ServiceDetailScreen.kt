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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    viewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    serviceId: Int,
    personnelId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    // --- Lifecycle / ViewModel Veri Yükleme ---
    LaunchedEffect(serviceId) {
        viewModel.loadServiceNotes(serviceId)
    }

    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val service = serviceRecords.find { it.id == serviceId }

    val personnelList by personnelViewModel.personnelList.collectAsState()

    val serviceNotes by viewModel.serviceNotes.collectAsState()

    var showAssignDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var noteError by remember { mutableStateOf(false) }

    // --- YENİ: Admin not detayı diyalog state'i ---
    var selectedNoteForDialog by remember { mutableStateOf<ServiceNote?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (service == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("İş emri bulunamadı veya silindi.")
        }
        return
    }

    if (personnelId != null && service.assignedPersonnelId != personnelId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Bu iş emrini görüntüleme yetkiniz yok (Başka bir personele atanmış).",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onNavigateBack) {
                    Text("Geri Dön")
                }
            }
        }
        return
    }

    val assignedPersonnelName = if (service.assignedPersonnelId != null) {
        personnelList.find { it.id == service.assignedPersonnelId }?.fullName ?: "Personel Bulunamadı (Silinmiş Olabilir)"
    } else {
        "Atanmadı"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (personnelId != null) "İş Emri Detayı (Personel)" else "İş Emri Detayları", fontWeight = FontWeight.Bold) },
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
                    Text(text = "İş No: #${service.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Firma: ${service.companyName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Yetkili Kişi: ${service.contactPerson ?: "Belirtilmedi"}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yetkili Telefon: ${service.contactPhone ?: "Belirtilmedi"}")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Cihaz Tipi: ${service.deviceType}")
                    Text("Cihaz Modeli: ${service.deviceModel}")
                    Text("Seri No: ${service.serialNumber}")
                    Text("Lokasyon: ${service.location}")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Açık Adres: ${service.address ?: "Belirtilmedi"}")
                    }

                    Text("Oluşturulma Tarihi: ${service.date}")
                    Text("Planlanan Ziyaret: ${service.plannedDate ?: "Belirtilmedi"}")
                    Text("Öncelik: ${service.priority}", color = MaterialTheme.colorScheme.error)

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

            // --- SERVİS NOTLARI BÖLÜMÜ ---
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Servis Notları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        if (personnelId != null &&
                            service.assignedPersonnelId == personnelId &&
                            service.status != ServiceStatus.TAMAMLANDI &&
                            service.status != ServiceStatus.IPTAL
                        ) {
                            TextButton(onClick = { showAddNoteDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Not Ekle")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Not Ekle")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (serviceNotes.isEmpty()) {
                        Text(
                            text = "Henüz servis notu eklenmemiş.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (personnelId == null) {
                            // --- YENİ: ADMIN SERVİS NOTLARI GÖRÜNÜMÜ ---
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                serviceNotes.forEach { note ->
                                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(note.createdAt))
                                    val noteWriterName = personnelList.find { it.id == note.personnelId }?.fullName ?: "Personel ID: ${note.personnelId}"

                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedNoteForDialog = note },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = noteWriterName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = note.note,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- MEVCUT: PERSONEL SERVİS NOTLARI GÖRÜNÜMÜ (Değiştirilmedi) ---
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                serviceNotes.forEach { note ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(note.createdAt))
                                            val noteWriterName = personnelList.find { it.id == note.personnelId }?.fullName ?: "Personel ID: ${note.personnelId}"

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = noteWriterName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = note.note, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- BUTONLAR BÖLÜMÜ ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (personnelId == null) {
                    Button(
                        onClick = { showAssignDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personel Ata")
                    }

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
                        Button(
                            onClick = { onNavigateToEdit(service.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Düzenle")
                        }

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
                } else {
                    val isLocked = service.status == ServiceStatus.TAMAMLANDI || service.status == ServiceStatus.IPTAL

                    if (!isLocked) {
                        Button(
                            onClick = { showStatusDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Durum Güncelle")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Bu iş emri sonlandırıldığı için durumu değiştirilemez.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DİYALOGLAR ---

    // YENİ: ADMIN İÇİN SEÇİLİ NOT DETAYI DİYALOĞU
    selectedNoteForDialog?.let { note ->
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(note.createdAt))
        val noteWriterName = personnelList.find { it.id == note.personnelId }?.fullName ?: "Personel ID: ${note.personnelId}"

        AlertDialog(
            onDismissRequest = { selectedNoteForDialog = null },
            title = { Text("Servis Notu Detayı", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = noteWriterName, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = dateStr, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                    Text(
                        text = note.note,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNoteForDialog = null }) {
                    Text("Kapat")
                }
            }
        )
    }

    // NOT EKLEME DİYALOĞU
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddNoteDialog = false
                newNoteText = ""
                noteError = false
            },
            title = { Text("Servis Notu Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = {
                            newNoteText = it
                            noteError = false
                        },
                        placeholder = { Text("Yapılan işlem, gözlem veya servis notunu girin...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        isError = noteError
                    )
                    if (noteError) {
                        Text(
                            text = "Not boş bırakılamaz.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedNote = newNoteText.trim()
                        if (trimmedNote.isBlank()) {
                            noteError = true
                        } else {
                            if (personnelId != null) {
                                val newNote = ServiceNote(
                                    serviceRecordId = service.id,
                                    personnelId = personnelId,
                                    note = trimmedNote,
                                    createdAt = System.currentTimeMillis()
                                )
                                viewModel.addServiceNote(newNote)
                                showAddNoteDialog = false
                                newNoteText = ""
                            }
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddNoteDialog = false
                    newNoteText = ""
                    noteError = false
                }) {
                    Text("İptal")
                }
            }
        )
    }

    // PERSONEL ATA DİYALOĞU
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

    // DURUM GÜNCELLE DİYALOĞU
    if (showStatusDialog) {
        val availableStatuses = if (personnelId == null) {
            ServiceStatus.all
        } else {
            when (service.status) {
                ServiceStatus.BEKLIYOR -> listOf(ServiceStatus.YOLDA, ServiceStatus.IPTAL)
                ServiceStatus.YOLDA -> listOf(ServiceStatus.ISLEME_BASLANDI, ServiceStatus.IPTAL)
                ServiceStatus.ISLEME_BASLANDI -> listOf(ServiceStatus.PARCA_BEKLENIYOR, ServiceStatus.TAMAMLANDI, ServiceStatus.IPTAL)
                ServiceStatus.PARCA_BEKLENIYOR -> listOf(ServiceStatus.ISLEME_BASLANDI, ServiceStatus.IPTAL)
                else -> emptyList()
            }
        }

        var selectedStatus by remember { mutableStateOf(if (availableStatuses.isNotEmpty()) availableStatuses.first() else service.status) }

        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Durum Güncelle", fontWeight = FontWeight.Bold) },
            text = {
                if (availableStatuses.isEmpty()) {
                    Text("Bu durumdan geçilebilecek başka bir aşama bulunmuyor.")
                } else {
                    Column {
                        availableStatuses.forEach { statusOption ->
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
                }
            },
            confirmButton = {
                if (availableStatuses.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (personnelId != null && service.assignedPersonnelId != personnelId) {
                                showStatusDialog = false
                                return@Button
                            }

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
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // SİLME ONAY DİYALOĞU
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