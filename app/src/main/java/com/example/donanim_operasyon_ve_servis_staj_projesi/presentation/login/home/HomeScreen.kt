package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = ServiceRepository(database.serviceDao())
    val viewModel: ServiceViewModel = viewModel(factory = ServiceViewModelFactory(repository))

    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val selectedRecord by viewModel.selectedRecord.collectAsState()
    val showAddDialog = remember { mutableStateOf(false) }

    // Arama ve Filtreleme State'leri
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Hepsi") }

    // Filtreleme ve Arama Mantığı
    val filteredRecords = serviceRecords.filter { record ->
        val matchesSearch = record.companyName.contains(searchQuery, ignoreCase = true) ||
                record.serialNumber.contains(searchQuery, ignoreCase = true) ||
                record.deviceType.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Bekleyen" -> record.status == ServiceStatus.BEKLIYOR
            "Tamamlanan" -> record.status == ServiceStatus.TAMAMLANDI
            "Parça Bekleyen" -> record.status == ServiceStatus.PARCA_BEKLENIYOR
            else -> true
        }

        matchesSearch && matchesFilter
    }

    if (selectedRecord != null) {
        DetailScreen(
            record = selectedRecord!!,
            onBack = { viewModel.clearSelection() },
            onStatusChange = { newStatus ->
                viewModel.updateStatus(selectedRecord!!.id, newStatus)
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            val activeCount = serviceRecords.count { it.status != ServiceStatus.TAMAMLANDI }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Aktif Operasyonlar ve Servisler",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toplam $activeCount aktif servis",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog.value = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Kayıt Ekle")
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // 1. Dashboard Özet Kartları
                    DashboardSummary(records = serviceRecords)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Arama Çubuğu (Search Bar)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Firma, Cihaz veya Seri No Ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Filtreleme Çipleri (FilterChips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hepsi", "Bekleyen", "Tamamlanan", "Parça Bekleyen").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Liste veya Boş Durum
                    if (filteredRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aradığınız kriterlere uygun iş emri bulunamadı.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredRecords) { record ->
                                ServiceRecordCard(
                                    record = record,
                                    onCardClick = { viewModel.selectRecord(record) },
                                    onDeleteClick = { viewModel.deleteRecord(record) }
                                )
                            }
                        }
                    }
                }
            }

            if (showAddDialog.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AddServiceForm(
                        onDismiss = { showAddDialog.value = false },
                        onSave = { newRecord ->
                            viewModel.insertRecord(newRecord)
                            showAddDialog.value = false
                        }
                    )
                }
            }
        }
    }
}

// 📊 Dashboard Özet Kartları Bileşeni
@Composable
fun DashboardSummary(records: List<ServiceRecord>) {
    val totalCount = records.size
    val pendingCount = records.count { it.status == ServiceStatus.BEKLIYOR }
    val completedCount = records.count { it.status == ServiceStatus.TAMAMLANDI }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(title = "Toplam", count = totalCount.toString(), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        SummaryCard(title = "Bekleyen", count = pendingCount.toString(), color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
        SummaryCard(title = "Tamamlanan", count = completedCount.toString(), color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
    }
}

@Composable
fun SummaryCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ServiceRecordCard(
    record: ServiceRecord,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = record.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = record.location, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = record.deviceType, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "#${record.serialNumber}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityBadge(priority = record.priority)
                    StatusBadge(status = record.status)
                }
                Text(text = record.date, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
        }
    }
}

// 📄 Detay Ekranı
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    record: ServiceRecord,
    onBack: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record.companyName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
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
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow("Firma Adı", record.companyName)
                    DetailRow("Lokasyon / Şube / Adres", record.location)
                    DetailRow("Cihaz Modeli / Tipi", record.deviceType)
                    DetailRow("Seri Numarası", record.serialNumber)
                    DetailRow("Öncelik Seviyesi", record.priority)
                    DetailRow("Arıza Açıklaması", record.issueDescription)
                    DetailRow("Kayıt Tarihi", record.date)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Servis Durumunu Güncelle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            StatusDropdown(
                currentStatus = record.status,
                onStatusSelected = { newStatus -> onStatusChange(newStatus) }
            )
        }
    }
}

@Composable
fun StatusDropdown(
    currentStatus: String,
    onStatusSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ServiceStatus.all.forEach { status ->
            val isSelected = status == currentStatus

            OutlinedButton(
                onClick = { onStatusSelected(status) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        ServiceStatus.TAMAMLANDI -> Color(0xFF4CAF50)
        ServiceStatus.IPTAL -> Color(0xFFF44336)
        ServiceStatus.PARCA_BEKLENIYOR -> Color(0xFFFF9800)
        else -> Color(0xFF2196F3)
    }

    val icon = when (status) {
        ServiceStatus.TAMAMLANDI -> Icons.Default.CheckCircle
        ServiceStatus.IPTAL -> Icons.Default.Clear
        ServiceStatus.PARCA_BEKLENIYOR -> Icons.Default.Settings
        else -> Icons.Default.Info
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = status, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddServiceForm(onDismiss: () -> Unit, onSave: (ServiceRecord) -> Unit) {
    var companyName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var issueDesc by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(text = "Yeni İş Emri Oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Firma Adı *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = deviceType, onValueChange = { deviceType = it }, label = { Text("Cihaz Tipi / Modeli *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("Seri Numarası (Boş bırakılırsa otomatik üretilir)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Lokasyon / Şube *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Öncelik Seviyesi:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
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

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = issueDesc, onValueChange = { issueDesc = it }, label = { Text("Arıza Açıklaması *") }, modifier = Modifier.fillMaxWidth())

            if (validationMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = validationMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("İptal") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val missingFields = mutableListOf<String>()
                        if (companyName.isBlank()) missingFields.add("Firma Adı")
                        if (deviceType.isBlank()) missingFields.add("Cihaz Tipi")
                        if (location.isBlank()) missingFields.add("Lokasyon")
                        if (issueDesc.isBlank()) missingFields.add("Arıza Açıklaması")

                        if (missingFields.isNotEmpty()) {
                            validationMessage = if (missingFields.size == 1) {
                                "${missingFields[0]} girilmedi!"
                            } else {
                                "${missingFields.joinToString(", ")} girilmedi!"
                            }
                        } else {
                            validationMessage = ""
                            val finalSerial = if (serialNumber.isBlank()) "SN-${(1000..9999).random()}" else serialNumber

                            val newRecord = ServiceRecord(
                                companyName = companyName,
                                deviceType = deviceType,
                                serialNumber = finalSerial,
                                location = location,
                                priority = priority,
                                issueDescription = issueDesc,
                                status = ServiceStatus.BEKLIYOR,
                                date = "04.08.2026"
                            )
                            onSave(newRecord)
                        }
                    }
                ) { Text("Kaydet") }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when (priority.lowercase()) {
        "kritik", "acil" -> Color(0xFFE53935)
        "yüksek" -> Color(0xFFFB8C00)
        "düşük" -> Color(0xFF757575)
        else -> Color(0xFF43A047)
    }

    val icon = when (priority.lowercase()) {
        "kritik", "acil" -> Icons.Default.Warning
        "yüksek" -> Icons.Default.KeyboardArrowUp
        "düşük" -> Icons.Default.KeyboardArrowDown
        else -> Icons.Default.Done
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = priority, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}