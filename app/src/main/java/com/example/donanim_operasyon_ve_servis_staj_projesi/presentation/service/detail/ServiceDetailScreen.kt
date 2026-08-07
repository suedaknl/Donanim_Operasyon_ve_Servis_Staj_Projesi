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
    personnelId: Int? = null, // YENİ: Personel oturum kontrolü için opsiyonel ID
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val service = serviceRecords.find { it.id == serviceId }

    val personnelList by personnelViewModel.personnelList.collectAsState()

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

    // GÜVENLİK KONTROLÜ: Eğer ekranı açan bir personel ise ve bu iş emri başka bir personele aitse erişimi engelle!
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

            // --- BUTONLAR BÖLÜMÜ (YETKİ KISITLAMASI) ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Yalnızca Admin veya ileride yetkilendirilen roller için Personel Ata ve Durum Güncelle butonları
                // Şimdiki kurala göre: Personel sadece detayları görür, parça/silme/düzenleme yapamaz.
                // Ancak "Durum Güncelle" butonu sonraki saha operasyonları için korunabilir ya da Admin'e özel tutulabilir.
                // İstekte: "Personel başka personel atayamasın, temel bilgileri değiştirmesin, admin işlemleri görünmesin" denmiştir.

                if (personnelId == null) {
                    // --- ADMIN GÖRÜNÜMÜ (TÜM YETKİLER AKTİF) ---
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
                    // --- PERSONEL GÖRÜNÜMÜ (YETKİLER KISITLANDI) ---
                    // Personel sadece okuyabilir, admin işlemleri (Düzenle, Sil, Personel Ata) görünmez.
                    Text(
                        text = "Saha operasyon yetkileri sonraki aşamalarda eklenecektir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    // --- DİYALOGLAR (Sadece Admin veya ilgili yetkiler tetikleyebilir) ---
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