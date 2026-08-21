package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.PhotoCategory
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.LocationHelper
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.ServiceReportPdfGenerator
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ServiceDetailScreen(
    viewModel: ServiceViewModel,
    personnelViewModel: PersonnelViewModel,
    serviceId: Int,
    personnelId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToHistory: (String, Int, String) -> Unit,
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
    val personnelRecords by viewModel.personnelServiceRecords.collectAsState()
    val service = serviceRecords.find { it.id == serviceId } ?: personnelRecords.find { it.id == serviceId }

    LaunchedEffect(service?.firestoreId) {
        service?.firestoreId?.let { firestoreId ->
            viewModel.loadRemoteMediaAndNotes(firestoreId)
            viewModel.loadServiceHistory(firestoreId)
        }
    }

    val personnelList by personnelViewModel.personnelList.collectAsState()
    val serviceNotes by viewModel.serviceNotes.collectAsState()
    val servicePhotos by viewModel.servicePhotos.collectAsState()
    val closingSignature = viewModel.serviceClosingSignature.collectAsState().value

    val remotePhotos by viewModel.remotePhotos.collectAsState()
    val remoteSignatures by viewModel.remoteSignatures.collectAsState()
    val remoteNotes by viewModel.remoteNotes.collectAsState()
    val serviceHistory by viewModel.serviceHistory.collectAsState()

    val context = LocalContext.current

    val isCompleted = service?.status == ServiceStatus.TAMAMLANDI
    val isCancelled = service?.status == ServiceStatus.IPTAL
    val isRejected = isCancelled && !service?.rejectionReason.isNullOrBlank()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }

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

    val maxAllowedStep = when (service?.status) {
        ServiceStatus.BEKLIYOR -> 1
        ServiceStatus.YOLDA, ServiceStatus.ISLEME_BASLANDI, ServiceStatus.PARCA_BEKLENIYOR -> 2
        ServiceStatus.TAMAMLANDI -> 3
        ServiceStatus.IPTAL -> 1
        else -> 0
    }

    var adminSelectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Accordion states for Onay page
    var isCompletionInfoExpanded by rememberSaveable { mutableStateOf(false) }
    var isClosingResultExpanded by rememberSaveable { mutableStateOf(false) }
    var isDigitalSignatureExpanded by rememberSaveable { mutableStateOf(false) }
    var isActionsExpanded by rememberSaveable { mutableStateOf(false) }

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
    var showArchiveDialog by remember { mutableStateOf(false) }

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
            CircularProgressIndicator()
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentLocation = locationResult.lastLocation
            }
        }
    }

    if (personnelId != null && !isCompleted && !isCancelled) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && LocationHelper.hasLocationPermission(context)) {
                    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
                    try { fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper()) }
                    catch (e: SecurityException) { e.printStackTrace() }
                } else if (event == Lifecycle.Event.ON_STOP) {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    val distanceMeters = remember(currentLocation, service.latitude, service.longitude) {
        val activeLat = currentLocation?.latitude ?: 40.5139
        val activeLon = currentLocation?.longitude ?: 34.9612

        if (service.latitude != null && service.longitude != null) {
            LocationHelper.calculateDistanceInMetres(
                activeLat, activeLon,
                service.latitude, service.longitude
            )
        } else null
    }

    val assignedPersonnelName =
        service.assignedPersonnelName
            ?: personnelList.find {
                it.id == service.assignedPersonnelId ||
                        it.firebaseUid == service.assignedPersonnelUid
            }?.fullName
            ?: "Atanmadı"

    val isLocked = isCompleted || isCancelled
    val canAddContent = personnelId != null && service.assignedPersonnelId == personnelId && !isLocked && service.status != ServiceStatus.BEKLIYOR

    val combinedNotes = remember(serviceNotes, remoteNotes) {
        val remoteAsObjects = remoteNotes.map { map ->
            ServiceNote(
                serviceRecordId = serviceId,
                personnelId = (map["personnelId"] as? Long)?.toInt() ?: 0,
                note = map["note"] as? String ?: "",
                noteType = map["noteType"] as? String ?: "NORMAL",
                createdAt = (map["createdAt"] as? Long) ?: 0L
            )
        }
        (remoteAsObjects + serviceNotes).distinctBy { it.note to it.createdAt }
    }

    val combinedPhotos = remember(servicePhotos, remotePhotos) {
        val remoteAsObjects = remotePhotos.map { map ->
            val url = map["downloadUrl"] as? String ?: ""
            val type = map["photoType"] as? String ?: "DİĞER"
            ServicePhoto(
                serviceRecordId = serviceId, personnelId = 0, photoType = type, localUri = url, timestamp = (map["timestamp"] as? Long) ?: 0L, photoUri = url, photoCategory = type
            )
        }
        (remoteAsObjects + servicePhotos).distinctBy { it.photoUri.ifBlank { it.localUri } }
    }

    val closingKeywords = listOf("closing", "kapanis", "kapanış", "sonuc", "sonuç", "sonrasi", "sonrası")
    val operationalNotes = combinedNotes.filter { note -> !closingKeywords.any { (note.noteType ?: "").trim().lowercase(Locale.ROOT).contains(it) } }
    val operationalPhotos = combinedPhotos.filter { photo -> !closingKeywords.any { (photo.photoType ?: photo.photoCategory ?: "").trim().lowercase(Locale.ROOT).contains(it) } }
    val closingNoteItem = combinedNotes.firstOrNull { note -> closingKeywords.any { (note.noteType ?: "").trim().lowercase(Locale.ROOT).contains(it) } } ?: combinedNotes.lastOrNull()
    val closingAfterPhotos = combinedPhotos.filter { photo -> closingKeywords.any { (photo.photoType ?: photo.photoCategory ?: "").trim().lowercase(Locale.ROOT).contains(it) } }
    val remoteSignatureUrl = remoteSignatures.firstOrNull()?.get("downloadUrl") as? String
    val effectiveSignaturePath = closingSignature?.signatureLocalUri ?: remoteSignatureUrl

    fun openNavigation(lat: Double, lon: Double) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply { setPackage("com.google.android.apps.maps") }
        try { context.startActivity(mapIntent) } catch (e: Exception) {
            val fallbackUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            try { context.startActivity(fallbackIntent) } catch (ex: Exception) {
                Toast.makeText(context, "Harita uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getEtaMinutes(distMeters: Float): Int {
        return (distMeters / 500f).roundToInt().coerceAtLeast(1)
    }

    @Composable
    fun getFreshnessInfo(timeMillis: Long): Pair<String, Color> {
        val diffMins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timeMillis)
        return when {
            diffMins <= 2 -> Pair("Güncel (${if(diffMins == 0L) "az önce" else "$diffMins dk önce"})", Color(0xFF4CAF50))
            diffMins <= 5 -> Pair("Yakın zamanda güncellendi ($diffMins dk önce)", Color(0xFFFF9800))
            else -> Pair("Konum eski ($diffMins dk önce)", MaterialTheme.colorScheme.error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (personnelId != null) "İş Emri Akışı" else "İş Emri Takibi (Admin)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri Dön") }
                },
                actions = {
                    TextButton(onClick = { onNavigateToHistory(service.firestoreId.orEmpty(), service.id, service.companyName) }) {
                        Text(text = "Geçmiş (${serviceHistory.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        if (personnelId == null) {
            // =================================================================
            // ADMIN EKRANI (Tıklanabilir Sekmeli Yapı)
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        TabIndicator(title = "Bilgi", isSelected = adminSelectedTab == 0, onClick = { adminSelectedTab = 0 })
                        TabIndicator(title = "Görev", isSelected = adminSelectedTab == 1, onClick = { adminSelectedTab = 1 })
                        TabIndicator(title = "İşlem", isSelected = adminSelectedTab == 2, onClick = { adminSelectedTab = 2 })
                        TabIndicator(title = "Onay", isSelected = adminSelectedTab == 3, onClick = { adminSelectedTab = 3 })
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (adminSelectedTab) {
                            0 -> {
                                Text("İş Emri Temel Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
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
                                                Text(text = "Öncelik: ${service.priority}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                        InfoRow(icon = Icons.Default.ReportProblem, label = "Arıza Nedeni", value = service.issueDescription)
                                        InfoRow(icon = Icons.Default.Badge, label = "Atanan Personel", value = assignedPersonnelName)
                                        InfoRow(icon = Icons.Default.Info, label = "Mevcut Durum", value = service.status)
                                    }
                                }
                            }
                            1 -> {
                                Text("Görev Durumu ve Personel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (isRejected) {
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(text = "🔴 İptal / Reddedildi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                            Text(text = "Red Nedeni: ${service.rejectionReason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                            Text(text = "Görevli Personel: $assignedPersonnelName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                } else {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Atanan Personel: $assignedPersonnelName", fontWeight = FontWeight.Bold)
                                            when (service.status) {
                                                ServiceStatus.BEKLIYOR -> Text("Personelin işi kabul etmesi bekleniyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                else -> Text("Görev personel tarafından kabul edildi.", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }

                                if (!isCompleted) {
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                }
                            }
                            2 -> {
                                Text("Saha İşlemleri ve Takip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (service.status == ServiceStatus.BEKLIYOR && !isRejected) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Bu aşamada henüz işlem yapılmadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    if (service.status == ServiceStatus.PARCA_BEKLENIYOR) {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(12.dp)) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(text = "🟡 Parça Bekleniyor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                Text(text = "Personel işlemi durdurdu ve gerekli yedek parça teminini bekliyor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                            }
                                        }
                                    }
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Saha İşlem Notları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (operationalNotes.isEmpty()) {
                                                Text("Eklenmiş işlem notu bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            } else {
                                                operationalNotes.forEach { note -> Text("• ${note.note}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                                            }
                                        }
                                    }
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("İşlem Fotoğrafları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (operationalPhotos.isEmpty()) {
                                                Text("Eklenmiş fotoğraf bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            } else {
                                                val groupedPhotos = operationalPhotos.groupBy { it.photoType ?: it.photoCategory ?: "DİĞER" }
                                                groupedPhotos.forEach { (category, photos) ->
                                                    Text(text = category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        items(photos) { photo ->
                                                            AsyncImage(model = photo.localUri, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).clickable { selectedImageUri = photo.localUri }, contentScale = ContentScale.Crop)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Text("İş Sonucu & Onay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                if (isCompleted) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.PictureAsPdf,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Servis Raporu",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "Tamamlanan iş emrinin servis raporunu görüntüleyebilir veya paylaşabilirsiniz.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val pdfFile = ServiceReportPdfGenerator.generatePdf(
                                                            context = context,
                                                            record = service,
                                                            notes = combinedNotes,
                                                            photos = combinedPhotos,
                                                            signaturePath = effectiveSignaturePath,
                                                            history = serviceHistory
                                                        )
                                                        if (pdfFile != null && pdfFile.exists()) {
                                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(uri, "application/pdf")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            try { context.startActivity(viewIntent) }
                                                            catch (e: Exception) { Toast.makeText(context, "PDF okuyucu bulunamadı.", Toast.LENGTH_SHORT).show() }
                                                        } else {
                                                            Toast.makeText(context, "Servis raporu oluşturulamadı.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("PDF Görüntüle")
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        val pdfFile = ServiceReportPdfGenerator.generatePdf(
                                                            context = context,
                                                            record = service,
                                                            notes = combinedNotes,
                                                            photos = combinedPhotos,
                                                            signaturePath = effectiveSignaturePath,
                                                            history = serviceHistory
                                                        )
                                                        if (pdfFile != null && pdfFile.exists()) {
                                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "application/pdf"
                                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            context.startActivity(Intent.createChooser(shareIntent, "Raporu Paylaş"))
                                                        } else {
                                                            Toast.makeText(context, "Önce PDF üretilmelidir.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Paylaş")
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!isCompleted) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("İş henüz tamamlanmadı. Kapanış verileri bekleniyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    // 2. İş Tamamlama Bilgisi Accordion
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isCompletionInfoExpanded = !isCompletionInfoExpanded }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "İş Tamamlama Bilgisi",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = if (isCompletionInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            AnimatedVisibility(visible = isCompletionInfoExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text("Bu iş emri başarıyla tamamlanmıştır.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                            Text("Tamamlayan Personel: $assignedPersonnelName", style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 3. Kapanış Açıklaması / Sonuç Accordion (İçinde Sonrası Fotoğrafı ile)
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isClosingResultExpanded = !isClosingResultExpanded }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Kapanış Açıklaması / Sonuç",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = if (isClosingResultExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            AnimatedVisibility(visible = isClosingResultExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (closingNoteItem != null) {
                                                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                Text("Kapanış Açıklaması / Sonuç", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                                Text("• ${closingNoteItem.note}", style = MaterialTheme.typography.bodyMedium)
                                                            }
                                                        }
                                                    }
                                                    if (closingAfterPhotos.isNotEmpty()) {
                                                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Text("Sonrası Fotoğrafı", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    items(closingAfterPhotos) { photo ->
                                                                        AsyncImage(model = photo.localUri, contentDescription = "Sonrası Fotoğrafı", modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).clickable { selectedImageUri = photo.localUri }, contentScale = ContentScale.Crop)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 4. Müşteri Dijital İmzası Accordion
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isDigitalSignatureExpanded = !isDigitalSignatureExpanded }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Müşteri Dijital İmzası",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = if (isDigitalSignatureExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            AnimatedVisibility(visible = isDigitalSignatureExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            if (!effectiveSignaturePath.isNullOrBlank()) {
                                                                AsyncImage(model = effectiveSignaturePath, contentDescription = "Dijital İmza", modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Fit)
                                                            } else {
                                                                Text("İmza bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 5. İşlemler Accordion
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isActionsExpanded = !isActionsExpanded }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "İşlemler",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = if (isActionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        AnimatedVisibility(visible = isActionsExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if ((service.status == ServiceStatus.TAMAMLANDI || service.status == ServiceStatus.IPTAL) && !service.isArchived) {
                                                    Button(
                                                        onClick = { showArchiveDialog = true },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Arşivle")
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        val targetId = if (service.id > 0) -service.id else service.id
                                                        onNavigateToEdit(targetId)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("İş Emrini Çoğalt")
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =================================================================
            // PERSONEL EKRANI
            // =================================================================
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
                        StepIndicator(stepNumber = 1, title = "Bilgi", actualStep = maxAllowedStep, selectedStep = currentStep, onClick = { currentStep = 0 })
                        StepIndicator(stepNumber = 2, title = "Görev", actualStep = maxAllowedStep, selectedStep = currentStep, onClick = {
                            if (maxAllowedStep >= 1) currentStep = 1 else coroutineScope.launch { snackbarHostState.showSnackbar("Bu aşama henüz kilitli.") }
                        })
                        StepIndicator(stepNumber = 3, title = "İşlem", actualStep = maxAllowedStep, selectedStep = currentStep, onClick = {
                            if (maxAllowedStep >= 2) currentStep = 2 else coroutineScope.launch { snackbarHostState.showSnackbar("Bu aşamaya geçmek için önce görevi kabul etmelisiniz.") }
                        })
                        StepIndicator(stepNumber = 4, title = "Onay", actualStep = maxAllowedStep, selectedStep = currentStep, onClick = {
                            if (maxAllowedStep >= 3) currentStep = 3 else coroutineScope.launch { snackbarHostState.showSnackbar("Onay aşamasına geçebilmek için önce işlemi tamamlamalısınız.") }
                        })
                    }
                }

                if (isRejected) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
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
                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(text = "İş No: #${service.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(text = service.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        HorizontalDivider()
                                        InfoRow(icon = Icons.Default.Person, label = "Yetkili", value = service.contactPerson ?: "Belirtilmedi")
                                        InfoRow(icon = Icons.Default.Phone, label = "Telefon", value = service.contactPhone ?: "Belirtilmedi")
                                        InfoRow(icon = Icons.Default.Devices, label = "Cihaz", value = "${service.deviceType} (${service.deviceModel})")
                                        InfoRow(icon = Icons.Default.LocationOn, label = "Lokasyon", value = service.location)
                                        InfoRow(icon = Icons.Default.Home, label = "Adres", value = service.address ?: "Belirtilmedi")
                                        InfoRow(icon = Icons.Default.ReportProblem, label = "Arıza Nedeni", value = service.issueDescription)
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
                                                Button(onClick = { viewModel.acceptService(service.id, personnelId!!) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Kabul Et") }
                                                Button(onClick = { rejectionReasonText = ""; rejectError = ""; showRejectDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text("Reddet") }
                                            }
                                        }
                                    }
                                }

                                ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("İş Konumu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                        }

                                        if (service.latitude == null || service.longitude == null) {
                                            Text("Bu iş emri için harita konumu belirlenmemiş.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Column {
                                                Text(service.location, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(service.address ?: "Adres belirtilmemiş", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            HorizontalDivider()
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Mesafe", style = MaterialTheme.typography.labelMedium)
                                                    val distTxt = if (distanceMeters != null) {
                                                        if (distanceMeters < 1000) "${distanceMeters.roundToInt()} m uzaktasınız" else "%.1f km uzaktasınız".format(distanceMeters / 1000.0).replace('.', ',')
                                                    } else "Hesaplanıyor..."
                                                    Text(distTxt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text("Güncel konumunuza göre yaklaşık mesafe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Button(onClick = { openNavigation(service.latitude, service.longitude) }, shape = RoundedCornerShape(12.dp)) {
                                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Yol Tarifi Al")
                                                }
                                            }
                                            if (distanceMeters != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text("Tahmini Varış Süresi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                        Text("Yaklaşık ${getEtaMinutes(distanceMeters)} dk (trafik durumuna göre değişebilir)", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                            if (currentLocation != null) {
                                                val freshness = getFreshnessInfo(currentLocation!!.time)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp), tint = freshness.second)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text("Konum Güncelliği", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                        Text("● ${freshness.first}", style = MaterialTheme.typography.labelSmall, color = freshness.second)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Text("3. İşlem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("İşlemi başlatmak için konuma yaklaşınız.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (!isLocked) {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("İş Konumu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                    Text(service.location, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                }
                                                if (service.latitude != null && service.longitude != null) {
                                                    OutlinedButton(onClick = { openNavigation(service.latitude, service.longitude) }, shape = RoundedCornerShape(8.dp)) {
                                                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Yol Tarifi")
                                                    }
                                                }
                                            }

                                            HorizontalDivider()

                                            if (service.latitude == null || service.longitude == null) {
                                                Text("Bu iş emri için harita konumu belirlenmemiş.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                            } else {
                                                val isVerified = distanceMeters != null && distanceMeters <= ServiceViewModel.SERVICE_START_RADIUS_METERS

                                                Column {
                                                    Text("Güncel Mesafe", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                    val distTxt = if (distanceMeters != null) {
                                                        if (distanceMeters < 1000) "${distanceMeters.roundToInt()} m" else "%.1f km".format(distanceMeters / 1000.0).replace('.', ',')
                                                    } else "Hesaplanıyor..."
                                                    Text("$distTxt uzaktasınız", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if(isVerified) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                                                    Text("iş konumuna olan anlık mesafe", style = MaterialTheme.typography.labelSmall)
                                                }

                                                Column {
                                                    Text("GPS Doğrulama Durumu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                    if (isVerified) {
                                                        Text("✓ İş konumu doğrulandı", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Text("! İş konumuna yaklaşmalısınız", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                        Text("işleme başlamak için 250 m mesafe kuralı geçerlidir.", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }

                                                if (service.status == ServiceStatus.YOLDA) {
                                                    Button(
                                                        onClick = {
                                                            if (service.latitude == null) {
                                                                viewModel.startServiceWork(service.id, personnelId!!)
                                                            } else if (isVerified) {
                                                                viewModel.verifyAndStartServiceWork(service.id, personnelId!!, distanceMeters!!)
                                                            } else {
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("İşleme başlamak için konuma 250m'den daha yakın olmalısınız.") }
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                        enabled = isVerified || service.latitude == null,
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        if (!isVerified && service.latitude != null) Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("İşleme Başla")
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (service.status == ServiceStatus.ISLEME_BASLANDI || service.status == ServiceStatus.PARCA_BEKLENIYOR) {
                                        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(16.dp)) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Button(
                                                    onClick = { onNavigateToClosingForm(service.id, personnelId!!) },
                                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) { Text("Tamamlandı / Kapanış Formunu Doldur") }

                                                if (service.status != ServiceStatus.PARCA_BEKLENIYOR) {
                                                    Button(
                                                        onClick = { viewModel.setParcaBekleniyor(service.id, personnelId!!) },
                                                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                    ) { Text("Parça Değişimi / Parça Bekleniyor") }
                                                } else {
                                                    Text("Parça bekleniyor durumunda. Parça temin edildikten sonra işleme devam edebilirsiniz.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }

                                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Saha İşlem Notları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    if (canAddContent) TextButton(onClick = { showAddNoteDialog = true }) { Text("Not Ekle") }
                                                }
                                                if (operationalNotes.isEmpty()) Text("Henüz ara not eklenmemiş.", style = MaterialTheme.typography.bodySmall)
                                                else operationalNotes.forEach { note -> Text("• ${note.note}", style = MaterialTheme.typography.bodyMedium) }
                                            }
                                        }

                                        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top=8.dp), shape = RoundedCornerShape(16.dp)) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("İşlem Fotoğrafları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    if (canAddContent) TextButton(onClick = { showCategoryDialog = true }) { Text("Fotoğraf Ekle") }
                                                }
                                                if (operationalPhotos.isEmpty()) {
                                                    Text("Henüz fotoğraf eklenmemiş.", style = MaterialTheme.typography.bodySmall)
                                                } else {
                                                    val groupedPhotos = operationalPhotos.groupBy { it.photoType ?: it.photoCategory ?: "DİĞER" }
                                                    groupedPhotos.forEach { (cat, photos) ->
                                                        Text(text = cat, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            items(photos) { photo ->
                                                                AsyncImage(model = photo.localUri, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Text("4. İş Emri Onay & Sonuç", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                if (!isCompleted) {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), shape = RoundedCornerShape(16.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text("İş henüz tamamlanmadı. Lütfen kapanış sürecini bitiriniz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Bu iş emri başarıyla tamamlanmıştır.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Tamamlayan: $assignedPersonnelName")
                                        }
                                    }

                                    if (closingNoteItem != null) {
                                        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp)) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Kapanış Açıklaması / Sonuç", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text("• ${closingNoteItem.note}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }

                                    if (closingAfterPhotos.isNotEmpty()) {
                                        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp)) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Kapanış / Sonrası Fotoğrafı", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(closingAfterPhotos) { photo ->
                                                        AsyncImage(model = photo.localUri, contentDescription = "Sonrası Fotoğrafı", modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
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

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentStep > 0) currentStep-- else onNavigateBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (currentStep > 0) "Geri" else "Çıkış / Geri Dön") }

                    if (currentStep < 3) {
                        val canProceed = currentStep < maxAllowedStep
                        Button(
                            onClick = {
                                if (canProceed) currentStep++
                                else coroutineScope.launch { snackbarHostState.showSnackbar("Lütfen önce mevcut aşamadaki işlemleri tamamlayın.") }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Devam Et") }
                    } else {
                        Button(
                            onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)
                        ) { Text("Çık") }
                    }
                }
            }
        }
    }

    if (showAssignDialog) {
        var selectedPersonnelId by remember { mutableStateOf<Int?>(null) }
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Personel Ata / Yeniden Ata", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (isRejected) {
                        Text("Not: İş emri iptal edilmiş. Yeni veya aynı personeli seçerek tekrar atayabilirsiniz.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(personnelList) { p ->
                            val isRejecter = isRejected && p.id == service.assignedPersonnelId
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedPersonnelId = p.id }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = (selectedPersonnelId == p.id), onClick = { selectedPersonnelId = p.id })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isRejecter) "${p.fullName} (Reddetti)" else p.fullName, color = if (isRejecter) MaterialTheme.colorScheme.error else Color.Unspecified)
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { if (selectedPersonnelId != null) { viewModel.reassignService(service.id, selectedPersonnelId, null) }; showAssignDialog = false }, enabled = selectedPersonnelId != null) { Text("Ata / Kaydet") } },
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
            confirmButton = { Button(onClick = { viewModel.updateStatus(service.id, selectedStatus); showStatusDialog = false }) { Text("Güncelle") } },
            dismissButton = { TextButton(onClick = { showStatusDialog = false }) { Text("İptal") } }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("İş Emrini Reddet") },
            text = {
                OutlinedTextField(value = rejectionReasonText, onValueChange = { rejectionReasonText = it; if (it.isNotBlank()) rejectError = "" }, label = { Text("Red Nedeni (Zorunlu)") }, modifier = Modifier.fillMaxWidth(), isError = rejectError.isNotEmpty(), minLines = 3)
            },
            confirmButton = {
                Button(onClick = {
                    if (rejectionReasonText.isBlank()) rejectError = "Red nedeni bırakılamaz."
                    else { showRejectDialog = false; viewModel.rejectService(service.id, rejectionReasonText.trim(), personnelId!!) }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Reddet Onayla") }
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
                        TextButton(onClick = { pendingCategory = category.name; showCategoryDialog = false; onNavigateToCamera() }, modifier = Modifier.fillMaxWidth()) { Text(category.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
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
            text = { OutlinedTextField(value = newNoteText, onValueChange = { newNoteText = it }, placeholder = { Text("Notunuz...") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (newNoteText.isNotBlank() && personnelId != null) { viewModel.addServiceNote(ServiceNote(serviceRecordId = service.id, personnelId = personnelId, note = newNoteText.trim(), createdAt = System.currentTimeMillis())); showAddNoteDialog = false; newNoteText = "" }
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
            confirmButton = { Button(onClick = { viewModel.deleteRecord(service); showDeleteDialog = false; onNavigateBack() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") } }
        )
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("İş Emrini Arşivle") },
            text = { Text("Bu iş emri aktif listeden kaldırılarak arşive taşınacaktır. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showArchiveDialog = false
                        viewModel.archiveService(service.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Arşivle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun TabIndicator(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StepIndicator(stepNumber: Int, title: String, actualStep: Int, selectedStep: Int, onClick: () -> Unit) {
    val isCompleted = actualStep != -1 && stepNumber - 1 < actualStep
    val isCurrent = actualStep != -1 && stepNumber - 1 == selectedStep

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
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
                Text(text = if (isCompleted) "✓" else "$stepNumber", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}