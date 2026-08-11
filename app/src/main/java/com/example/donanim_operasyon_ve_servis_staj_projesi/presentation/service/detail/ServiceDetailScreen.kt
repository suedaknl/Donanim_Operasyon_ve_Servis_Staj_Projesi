package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PhotoCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    viewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    serviceId: Int,
    personnelId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    returnedPhotoUri: String? = null,
    onPhotoSaved: () -> Unit = {},
    onNavigateToCamera: () -> Unit = {},
    onNavigateToClosingForm: (Int, Int) -> Unit = { _, _ -> }
) {
    LaunchedEffect(key1 = serviceId) {
        viewModel.loadServiceNotes(serviceId)
        viewModel.loadServicePhotos(serviceId)
        viewModel.loadClosingSignature(serviceId)
    }

    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val service = serviceRecords.find { it.id == serviceId }

    val personnelList by personnelViewModel.personnelList.collectAsState()
    val serviceNotes by viewModel.serviceNotes.collectAsState()
    val servicePhotos by viewModel.servicePhotos.collectAsState()
    val closingSignature by viewModel.serviceClosingSignature.collectAsState()

    var showAssignDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var noteError by remember { mutableStateOf(false) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var pendingCategory by rememberSaveable { mutableStateOf("") }

    var selectedNoteForDialog by remember { mutableStateOf<ServiceNote?>(null) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    // --- ACCORDION STATE'LERİ ---
    var isInfoExpanded by remember { mutableStateOf(false) }
    var isClosingExpanded by remember { mutableStateOf(false) } // Kapanış bilgileri varsayılan kapalı
    var isIssueExpanded by remember { mutableStateOf(false) }
    var isNotesExpanded by remember { mutableStateOf(false) }
    var isPhotosExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(returnedPhotoUri) {
        if (returnedPhotoUri != null && pendingCategory.isNotEmpty()) {
            val photo = ServicePhoto(
                serviceRecordId = serviceId,
                personnelId = personnelId ?: 0,
                photoType = pendingCategory,
                localUri = returnedPhotoUri,
                timestamp = System.currentTimeMillis(),
                photoUri = returnedPhotoUri,
                photoCategory = pendingCategory
            )
            viewModel.addServicePhoto(photo)
            pendingCategory = ""
            onPhotoSaved()
        }
    }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        personnelList.find { it.id == service.assignedPersonnelId }?.fullName ?: "Personel Bulunamadı"
    } else {
        "Atanmadı"
    }

    val isCompleted = service.status == ServiceStatus.TAMAMLANDI
    val isCancelled = service.status == ServiceStatus.IPTAL

    // Tamamlanmış iş hem personel hem admin için salt okunur operasyonlara tabidir
    val isLocked = isCompleted || isCancelled

    // İçerik ekleme yetkisi (Not, Fotoğraf)
    val canAddContent = personnelId != null &&
            service.assignedPersonnelId == personnelId &&
            !isLocked

    // Personel atama kuralı: Yalnızca BEKLIYOR ve henüz atanmamışsa
    val canAssignPersonnel = personnelId == null &&
            service.status == ServiceStatus.BEKLIYOR &&
            service.assignedPersonnelId == null

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (personnelId != null) "İş Emri Detayı" else "İş Emri Detayları (Admin)", fontWeight = FontWeight.Bold) },
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

            // --- 1. İŞ EMRİ BİLGİLERİ (ACCORDION) ---
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isInfoExpanded = !isInfoExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "İş Emri Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = if (isInfoExpanded) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (isInfoExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }

            // --- 2. İŞLEMLER ALANI (YALNIZCA KİLİTLİ DEĞİLSE GÖRÜNÜR) ---
            if (!isLocked) {
                if (personnelId == null) {
                    // Admin İşlem Butonları
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canAssignPersonnel) {
                                Button(
                                    onClick = { showAssignDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Personel Ata")
                                }
                            }

                            Button(
                                onClick = { showStatusDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Durum Güncelle")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNavigateToEdit(service.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Düzenle")
                            }

                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sil")
                            }
                        }
                    }
                } else {
                    // Personel İşlem Butonları
                    Button(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Durum Güncelle")
                    }
                }
            }

            // --- 3. KAPANIŞ BİLGİLERİ (VARSAYILAN KAPALI & ÖZET GÖRÜNÜM) ---
            if (isCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isClosingExpanded = !isClosingExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Kapanış Bilgileri",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Durum: Tamamlandı",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                // KART KAPALIYKEN ÖZET BİLGİ GÖSTERİMİ
                                if (!isClosingExpanded) {
                                    val assignedPersonnel = personnelList.find { it.id == service.assignedPersonnelId }
                                    Text(
                                        text = "Tamamlayan: ${assignedPersonnel?.fullName ?: "Bilinmiyor"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val signature = closingSignature
                                    if (signature != null) {
                                        val shortDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                        Text(
                                            text = "Tarih: ${shortDateFormat.format(Date(signature.createdAt))}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            Text(text = if (isClosingExpanded) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (isClosingExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val assignedPersonnel = personnelList.find { it.id == service.assignedPersonnelId }
                                val signature = closingSignature

                                Text("Tamamlayan Personel: ${assignedPersonnel?.fullName ?: "Bilinmiyor"}")

                                if (signature != null) {
                                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    val completionDate = Date(signature.createdAt)

                                    Text("Tamamlanma Tarihi: ${dateFormat.format(completionDate)}")
                                    Text("Tamamlanma Saati: ${timeFormat.format(completionDate)}")
                                }

                                val closingNoteObj = serviceNotes.find { it.noteType == "CLOSING" } ?: serviceNotes.lastOrNull()
                                if (closingNoteObj != null) {
                                    Text("Kapanış Notu:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                    Text(closingNoteObj.note)
                                }

                                val afterPhoto = servicePhotos.find { it.photoType == "SONRASI" || it.photoCategory == "SONRASI" }
                                if (afterPhoto != null) {
                                    Text("Sonrası Fotoğrafı:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                    AsyncImage(
                                        model = afterPhoto.localUri,
                                        contentDescription = "Sonrası Fotoğrafı",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                if (signature != null) {
                                    Text("Dijital İmza:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                    AsyncImage(
                                        model = signature.signatureLocalUri,
                                        contentDescription = "Dijital İmza",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. ARIZA AÇIKLAMASI (ACCORDION) ---
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isIssueExpanded = !isIssueExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Arıza Açıklaması", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Text(text = if (isIssueExpanded) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (isIssueExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(service.issueDescription)
                    }
                }
            }

            // --- 5. SERVİS NOTLARI BÖLÜMÜ (ACCORDION) ---
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNotesExpanded = !isNotesExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Servis Notları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canAddContent) {
                                TextButton(onClick = { showAddNoteDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Not Ekle")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Not Ekle")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = if (isNotesExpanded) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isNotesExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        val regularNotes = serviceNotes.filter { it.noteType != "CLOSING" }

                        if (regularNotes.isEmpty()) {
                            Text(
                                text = "Henüz servis notu eklenmemiş.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (personnelId == null) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    regularNotes.forEach { note ->
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
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    regularNotes.forEach { note ->
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
            }

            // --- 6. SERVİS FOTOĞRAFLARI KARTI (ACCORDION) ---
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPhotosExpanded = !isPhotosExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Servis Fotoğrafları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canAddContent) {
                                TextButton(onClick = { showCategoryDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Fotoğraf Ekle")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fotoğraf Ekle")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = if (isPhotosExpanded) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isPhotosExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        if (servicePhotos.isEmpty()) {
                            Text(
                                text = "Henüz fotoğraf eklenmemiş.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val groupedPhotos = servicePhotos.groupBy { it.photoType }
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                groupedPhotos.forEach { (category, photos) ->
                                    Column {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(photos) { photo ->
                                                AsyncImage(
                                                    model = photo.localUri,
                                                    contentDescription = "Servis Fotoğrafı",
                                                    modifier = Modifier
                                                        .size(100.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { selectedImageUri = photo.localUri },
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DİYALOGLAR ---

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Fotoğraf Kategorisi", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PhotoCategory.values().forEach { category ->
                        TextButton(
                            onClick = {
                                pendingCategory = category.name
                                showCategoryDialog = false
                                onNavigateToCamera()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("İptal", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    selectedImageUri?.let { uri ->
        Dialog(
            onDismissRequest = { selectedImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { selectedImageUri = null },
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                        }
                    }
                    AsyncImage(
                        model = uri,
                        contentDescription = "Büyük Servis Fotoğrafı",
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    selectedNoteForDialog?.let { note ->
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(note.createdAt))
        val noteWriterName = personnelList.find { it.id == note.personnelId }?.fullName ?: "Personel ID: ${note.personnelId}"

        AlertDialog(
            onDismissRequest = { selectedNoteForDialog = null },
            title = { Text("Servis Notu Detayı", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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
                    Text(text = note.note, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNoteForDialog = null }) {
                    Text("Kapat")
                }
            }
        )
    }

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
                        // Kural: Sadece BEKLIYOR durumunda ve henüz atanmamışsa atamaya izin ver
                        if (service.status == ServiceStatus.BEKLIYOR && service.assignedPersonnelId == null) {
                            val updatedRecord = service.copy(assignedPersonnelId = selectedPersonnelId)
                            viewModel.updateRecord(updatedRecord)
                        }
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
                                if (selectedStatus == ServiceStatus.TAMAMLANDI && personnelId != null) {
                                    onNavigateToClosingForm(service.id, personnelId)
                                } else {
                                    viewModel.updateStatus(service.id, selectedStatus)
                                }
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