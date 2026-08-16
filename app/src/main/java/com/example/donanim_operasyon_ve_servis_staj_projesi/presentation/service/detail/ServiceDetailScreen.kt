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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import android.widget.Toast

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

    val context = LocalContext.current

    val isCompleted = service?.status == ServiceStatus.TAMAMLANDI
    val isCancelled = service?.status == ServiceStatus.IPTAL
    val isRejected = isCancelled && !service?.rejectionReason.isNullOrBlank()

    // 4 Aşamalı Step State'i rememberSaveable ile korunuyor (Fotoğraf dönüşlerinde sıfırlanmaz)
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    // Ekran recomposition veya kamera dönüşlerinde statüye göre adımın güvenli recover edilmesi
    LaunchedEffect(service?.status) {
        if (service != null && personnelId != null) {
            when (service.status) {
                ServiceStatus.BEKLIYOR -> currentStep = 0
                ServiceStatus.YOLDA -> currentStep = 1
                ServiceStatus.ISLEME_BASLANDI, ServiceStatus.PARCA_BEKLENIYOR -> currentStep = 2
                ServiceStatus.TAMAMLANDI -> currentStep = 3
                ServiceStatus.IPTAL -> currentStep = 1
            }
        }
    }

    var showAssignDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var pendingCategory by rememberSaveable { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var rejectError by remember { mutableStateOf("") }

    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

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

    val assignedPersonnelName = if (service.assignedPersonnelId != null) {
        personnelList.find { it.id == service.assignedPersonnelId }?.fullName ?: "Personel Bulunamadı"
    } else {
        "Atanmadı"
    }

    val isLocked = isCompleted || isCancelled
    val canAddContent = personnelId != null &&
            service.assignedPersonnelId == personnelId &&
            !isLocked &&
            service.status != ServiceStatus.BEKLIYOR

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (personnelId != null) "İş Emri Akışı" else "İş Emri Detayları (Admin)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri Dön")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        if (personnelId == null) {
            // ADMIN EKRANI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRejected) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "🔴 Reddedildi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(text = "Red Nedeni: ${service.rejectionReason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "İş No: #${service.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Surface(
                                color = when (service.priority) {
                                    "Yüksek", "Acil" -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Öncelik: ${service.priority}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(text = service.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        HorizontalDivider()

                        InfoRow(icon = Icons.Default.Person, label = "Yetkili", value = service.contactPerson ?: "Belirtilmedi")
                        InfoRow(icon = Icons.Default.Phone, label = "Telefon", value = service.contactPhone ?: "Belirtilmedi")
                        InfoRow(icon = Icons.Default.Devices, label = "Cihaz", value = "${service.deviceType} (${service.deviceModel})")
                        InfoRow(icon = Icons.Default.ConfirmationNumber, label = "Seri No", value = service.serialNumber)
                        InfoRow(icon = Icons.Default.LocationOn, label = "Lokasyon", value = service.location)
                        InfoRow(icon = Icons.Default.Home, label = "Adres", value = service.address ?: "Belirtilmedi")
                        InfoRow(icon = Icons.Default.Badge, label = "Atanan Personel", value = assignedPersonnelName)
                        InfoRow(icon = Icons.Default.Info, label = "Mevcut Durum", value = service.status)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAssignDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (service.assignedPersonnelId != null) "Yeniden Ata" else "Personel Ata")
                        }
                        Button(
                            onClick = { showStatusDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Durum Güncelle")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigateToEdit(service.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Düzenle")
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sil")
                        }
                    }
                }
            }
        } else {
            // PERSONEL EKRANI (4 AŞAMALI AKIŞ)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepIndicator(stepNumber = 1, title = "Bilgi", currentStep = currentStep, onClick = { currentStep = 0 })
                        StepIndicator(stepNumber = 2, title = "Görev", currentStep = currentStep, onClick = { currentStep = 1 })
                        StepIndicator(stepNumber = 3, title = "İşlem", currentStep = currentStep, onClick = { currentStep = 2 })
                        StepIndicator(stepNumber = 4, title = "Onay", currentStep = currentStep, onClick = { currentStep = 3 })
                    }
                }

                if (isRejected) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "🔴 Reddedildi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(text = "Red Nedeni: ${service.rejectionReason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (currentStep) {
                            0 -> {
                                Text("1. İş Emri Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(text = "İş No: #${service.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(text = service.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        HorizontalDivider()
                                        InfoRow(icon = Icons.Default.Person, label = "Yetkili", value = service.contactPerson ?: "Belirtilmedi")
                                        InfoRow(icon = Icons.Default.Phone, label = "Telefon", value = service.contactPhone ?: "Belirtilmedi")
                                        InfoRow(icon = Icons.Default.Devices, label = "Cihaz", value = "${service.deviceType} (${service.deviceModel})")
                                        InfoRow(icon = Icons.Default.ConfirmationNumber, label = "Seri No", value = service.serialNumber)
                                        InfoRow(icon = Icons.Default.LocationOn, label = "Lokasyon", value = service.location)
                                        InfoRow(icon = Icons.Default.Home, label = "Adres", value = service.address ?: "Belirtilmedi")
                                        InfoRow(icon = Icons.Default.PriorityHigh, label = "Öncelik", value = service.priority)
                                        InfoRow(icon = Icons.Default.DateRange, label = "Tarih", value = service.date)
                                    }
                                }
                            }
                            1 -> {
                                Text("2. Görev Değerlendirme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (service.status == ServiceStatus.BEKLIYOR) {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text("Size yeni bir görev atandı. Lütfen inceleyip karar veriniz.", style = MaterialTheme.typography.bodyMedium)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Button(
                                                    onClick = { viewModel.acceptService(service.id, personnelId) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Kabul Et")
                                                }
                                                Button(
                                                    onClick = { rejectionReasonText = ""; rejectError = ""; showRejectDialog = true },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Reddet")
                                                }
                                            }
                                        }
                                    }
                                } else if (isRejected) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Bu görevi reddettiniz. Akış sonlandırılmıştır.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Görev kabul edildi (Yolda). 'İşlem' aşamasına geçebilirsiniz.", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            2 -> {
                                Text("3. Saha İşlemleri ve Takip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (!isLocked) {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("Mevcut Durum: ${service.status}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                            if (service.status == ServiceStatus.YOLDA) {
                                                Button(
                                                    onClick = { viewModel.startServiceWork(service.id, personnelId) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("İşleme Başla")
                                                }
                                            } else if (service.status == ServiceStatus.ISLEME_BASLANDI) {
                                                Button(
                                                    onClick = { onNavigateToClosingForm(service.id, personnelId) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text("Tamamlandı / Kapanış Formunu Doldur")
                                                }
                                                Button(
                                                    onClick = { viewModel.setParcaBekleniyor(service.id, personnelId) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                ) {
                                                    Text("Parça Değişimi / Parça Bekleniyor")
                                                }
                                            } else if (service.status == ServiceStatus.PARCA_BEKLENIYOR) {
                                                Text("Parça bekleniyor durumunda. Parça temin edildikten sonra işleme devam edebilirsiniz.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                                Button(
                                                    onClick = { onNavigateToClosingForm(service.id, personnelId) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text("Kapanış Formunu Doldur ve İşi Tamamla")
                                                }
                                            }
                                        }
                                    }
                                }

                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Servis Notları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            if (canAddContent) {
                                                TextButton(onClick = { showAddNoteDialog = true }) { Text("Not Ekle") }
                                            }
                                        }
                                        val regularNotes = serviceNotes.filter { it.noteType != "CLOSING" }
                                        if (regularNotes.isEmpty()) {
                                            Text("Henüz not eklenmemiş.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            regularNotes.forEach { note ->
                                                Text("• ${note.note}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                        }
                                    }
                                }

                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Servis Fotoğrafları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            if (canAddContent) {
                                                TextButton(onClick = { showCategoryDialog = true }) { Text("Fotoğraf Ekle") }
                                            }
                                        }
                                        if (servicePhotos.isEmpty()) {
                                            Text("Henüz fotoğraf eklenmemiş.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            val groupedPhotos = servicePhotos.groupBy { it.photoType }
                                            groupedPhotos.forEach { (category, photos) ->
                                                Text(text = category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(photos) { photo ->
                                                        AsyncImage(
                                                            model = photo.localUri,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).clickable { selectedImageUri = photo.localUri },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Text("4. İş Emri Onaylandı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Bu iş emri başarıyla tamamlanmıştır.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        val assigned = personnelList.find { it.id == service.assignedPersonnelId }
                                        Text("Tamamlayan: ${assigned?.fullName ?: "Bilinmiyor"}")
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentStep > 0) currentStep-- else onNavigateBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (currentStep > 0) "Geri" else "Çıkış / Geri Dön")
                    }

                    if (currentStep < 3) {
                        val canProceed = when (currentStep) {
                            0 -> true
                            1 -> service.status != ServiceStatus.BEKLIYOR && !isRejected
                            2 -> service.status == ServiceStatus.TAMAMLANDI
                            else -> false
                        }
                        Button(
                            onClick = {
                                if (canProceed) currentStep++ else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Lütfen önce mevcut aşamayı tamamlayın.") }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Devam Et")
                        }
                    } else {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("İşlemi Tamamla & Çık")
                        }
                    }
                }
            }
        }
    }

    // DİYALOGLAR
    if (showAssignDialog) {
        val availablePersonnelForAssignment = personnelList.filter { p ->
            p.id != service.assignedPersonnelId || !isRejected
        }
        var selectedPersonnelId by remember { mutableStateOf<Int?>(null) }
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Personel Ata / Yeniden Ata", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (isRejected) {
                        Text("Not: Bu işi daha önce reddeden personel listeden çıkarılmıştır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(availablePersonnelForAssignment) { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedPersonnelId = p.id }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (selectedPersonnelId == p.id), onClick = { selectedPersonnelId = p.id })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(p.fullName)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPersonnelId != null) {
                            val updated = service.copy(
                                assignedPersonnelId = selectedPersonnelId,
                                status = ServiceStatus.BEKLIYOR,
                                rejectionReason = null
                            )
                            viewModel.updateRecord(updated)
                        }
                        showAssignDialog = false
                    },
                    enabled = selectedPersonnelId != null
                ) { Text("Ata / Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showAssignDialog = false }) { Text("İptal") } }
        )
    }

    if (showStatusDialog) {
        val availableStatuses = ServiceStatus.all
        var selectedStatus by remember { mutableStateOf(service.status) }
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Durum Güncelle (Admin)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    availableStatuses.forEach { st ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedStatus = st }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = (selectedStatus == st), onClick = { selectedStatus = st })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(st)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateStatus(service.id, selectedStatus); showStatusDialog = false }) {
                    Text("Güncelle")
                }
            },
            dismissButton = { TextButton(onClick = { showStatusDialog = false }) { Text("İptal") } }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("İş Emrini Reddet") },
            text = {
                OutlinedTextField(
                    value = rejectionReasonText,
                    onValueChange = { rejectionReasonText = it; if (it.isNotBlank()) rejectError = "" },
                    label = { Text("Red Nedeni (Zorunlu)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = rejectError.isNotEmpty(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonText.isBlank()) {
                            rejectError = "Red nedeni boş bırakılamaz."
                        } else {
                            showRejectDialog = false
                            viewModel.rejectService(service.id, rejectionReasonText.trim(), personnelId ?: 0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reddet Onayla") }
            },
            dismissButton = { TextButton(onClick = { showRejectDialog = false }) { Text("İptal") } }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Fotoğraf Kategorisi Seçin", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PhotoCategory.values().forEach { category ->
                        TextButton(
                            onClick = { pendingCategory = category.name; showCategoryDialog = false; onNavigateToCamera() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(category.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("İptal", color = MaterialTheme.colorScheme.error) } }
        )
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Servis Notu Ekle") },
            text = {
                OutlinedTextField(value = newNoteText, onValueChange = { newNoteText = it }, placeholder = { Text("Notunuz...") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (newNoteText.isNotBlank() && personnelId != null) {
                        viewModel.addServiceNote(ServiceNote(serviceRecordId = service.id, personnelId = personnelId, note = newNoteText.trim(), createdAt = System.currentTimeMillis()))
                        showAddNoteDialog = false
                        newNoteText = ""
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showAddNoteDialog = false }) { Text("İptal") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Sil") },
            text = { Text("Silmek istediğinize emin misiniz?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteRecord(service); showDeleteDialog = false; onNavigateBack() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") } }
        )
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StepIndicator(stepNumber: Int, title: String, currentStep: Int, onClick: () -> Unit) {
    val isCompleted = stepNumber - 1 < currentStep
    val isCurrent = stepNumber - 1 == currentStep

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isCompleted) "✓" else "$stepNumber",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}