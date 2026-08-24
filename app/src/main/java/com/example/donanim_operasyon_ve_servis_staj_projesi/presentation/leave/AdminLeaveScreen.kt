package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.leave

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeaveScreen(
    leaveViewModel: LeaveViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedMainTab by remember { mutableIntStateOf(0) }
    var activeFilter by remember { mutableStateOf("Tümü") }

    val pendingRequests by leaveViewModel.pendingRequests.collectAsState()
    val approvedRequests by leaveViewModel.approvedRequests.collectAsState()
    val personnelList by leaveViewModel.personnelList.collectAsState()

    val capacityWarning by leaveViewModel.capacityWarning.collectAsState()
    val successMsg by leaveViewModel.successMessage.collectAsState()
    val errorMsg by leaveViewModel.errorMessage.collectAsState()

    var rejectTargetId by remember { mutableStateOf<Int?>(null) }
    var adminNoteInput by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val today = Calendar.getInstance()

    LaunchedEffect(Unit) {
        leaveViewModel.loadData()
    }

    val filteredApproved = approvedRequests.filter { leave ->
        val start = parseDate(leave.startDate)
        val end = parseDate(leave.endDate)
        val statusLabel = getLeaveStatusLabel(start, end, today)

        when (activeFilter) {
            "Şu An İzinli" -> statusLabel == "Şu An İzinli"
            "Yaklaşan" -> statusLabel == "Yaklaşan"
            "Geçmiş" -> statusLabel == "Geçmiş"
            else -> true
        }
    }

    val currentlyOnLeaveCount = approvedRequests.count { leave ->
        getLeaveStatusLabel(parseDate(leave.startDate), parseDate(leave.endDate), today) == "Şu An İzinli"
    }
    val upcomingCount = approvedRequests.count { leave ->
        getLeaveStatusLabel(parseDate(leave.startDate), parseDate(leave.endDate), today) == "Yaklaşan"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İzin Yönetimi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedMainTab) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = { Text("Bekleyen Talepler (${pendingRequests.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = { Text("Aktif & Yaklaşan (${approvedRequests.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedMainTab == 0) {
                if (pendingRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Bekleyen izin talebi bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingRequests) { req ->
                            val personnel = personnelList.find { it.id == req.personnelId }
                            val personnelName = personnel?.fullName ?: "Personel #${req.personnelId}"
                            val conflictInfo = leaveViewModel.calculateConflict(req)

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = personnelName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "İzin Türü: ${req.leaveType}")
                                    Text(text = "Tarih: ${req.startDate} / ${req.endDate}")

                                    if (!req.description.isNullOrBlank()) {
                                        Text(text = "Açıklama: ${req.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    Text(
                                        text = if (conflictInfo.conflictingLeaves.isEmpty()) "Bu tarih aralığında başka onaylı izin bulunmuyor."
                                        else "Çakışan Onaylı İzinler (${conflictInfo.conflictingLeaves.size} personel):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (conflictInfo.conflictingLeaves.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )

                                    conflictInfo.conflictingLeaves.forEach { conflict ->
                                        Text(
                                            text = "• ${conflict.personnelName} · ${conflict.leaveType} (${conflict.startDate} - ${conflict.endDate})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Kapasite Özeti: Toplam: ${conflictInfo.totalPersonnelCount} | Mevcut İzinli: ${conflictInfo.currentlyOnLeaveCount} | Onaylanırsa İzinli: ${conflictInfo.currentlyOnLeaveCount + 1}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    if (conflictInfo.isCapacityCritical) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Text("Bu izin onaylanırsa ekip kapasitesi kritik seviyeye düşecek.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { leaveViewModel.approveRequest(req.id) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Onayla")
                                        }
                                        Button(
                                            onClick = {
                                                rejectTargetId = req.id
                                                adminNoteInput = ""
                                                showRejectDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Reddet")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryMetricCard("Şu An İzinli", currentlyOnLeaveCount.toString(), Modifier.weight(1f))
                        SummaryMetricCard("Yaklaşan", upcomingCount.toString(), Modifier.weight(1f))
                        SummaryMetricCard("Toplam Onaylı", approvedRequests.size.toString(), Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tümü", "Şu An İzinli", "Yaklaşan", "Geçmiş").forEach { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }

                    if (filteredApproved.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Bu filtreye uygun onaylı izin bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredApproved) { leave ->
                                val personnel = personnelList.find { it.id == leave.personnelId }
                                val personnelName = personnel?.fullName ?: "Personel #${leave.personnelId}"
                                val start = parseDate(leave.startDate)
                                val end = parseDate(leave.endDate)
                                val statusLabel = getLeaveStatusLabel(start, end, today)
                                val totalDays = calculateDays(start, end)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(text = personnelName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "${leave.leaveType} · $totalDays gün", style = MaterialTheme.typography.bodyMedium)
                                            Text(text = "${leave.startDate} - ${leave.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            color = when (statusLabel) {
                                                "Şu An İzinli" -> MaterialTheme.colorScheme.errorContainer
                                                "Yaklaşan" -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
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

    if (capacityWarning != null) {
        AlertDialog(
            onDismissRequest = { leaveViewModel.clearMessages() },
            title = { Text("Personel Kapasitesi Uyarısı", fontWeight = FontWeight.Bold) },
            text = { Text(capacityWarning!!) },
            confirmButton = {
                Button(onClick = {
                    leaveViewModel.confirmApproveDespiteCapacity()
                }) {
                    Text("Yine de Onayla")
                }
            },
            dismissButton = {
                TextButton(onClick = { leaveViewModel.clearMessages() }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (showRejectDialog && rejectTargetId != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("İzin Talebini Reddet") },
            text = {
                OutlinedTextField(
                    value = adminNoteInput,
                    onValueChange = { adminNoteInput = it },
                    label = { Text("Admin Açıklaması / Red Nedeni") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        leaveViewModel.rejectRequest(rejectTargetId!!, adminNoteInput)
                        showRejectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reddet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    successMsg?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            leaveViewModel.clearMessages()
            leaveViewModel.loadData()
        }
    }

    errorMsg?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            leaveViewModel.clearMessages()
        }
    }
}

@Composable
fun SummaryMetricCard(title: String, count: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun parseDate(dateStr: String): Calendar? {
    val formats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "yyyy/MM/dd", "dd-MM-yyyy")
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.isLenient = false
            val date = sdf.parse(dateStr)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                return cal
            }
        } catch (_: Exception) {
        }
    }
    return null
}

private fun getLeaveStatusLabel(start: Calendar?, end: Calendar?, today: Calendar): String {
    if (start == null || end == null) return "Bilinmiyor"

    val cleanToday = truncateTime(today)
    val cleanStart = truncateTime(start)
    val cleanEnd = truncateTime(end)

    return when {
        !cleanToday.before(cleanStart) && !cleanToday.after(cleanEnd) -> "Şu An İzinli"
        cleanToday.before(cleanStart) -> "Yaklaşan"
        else -> "Geçmiş"
    }
}

private fun truncateTime(cal: Calendar): Calendar {
    val cloned = cal.clone() as Calendar
    cloned.set(Calendar.HOUR_OF_DAY, 0)
    cloned.set(Calendar.MINUTE, 0)
    cloned.set(Calendar.SECOND, 0)
    cloned.set(Calendar.MILLISECOND, 0)
    return cloned
}

private fun calculateDays(start: Calendar?, end: Calendar?): Long {
    if (start == null || end == null) return 0
    val diffMillis = Math.abs(end.timeInMillis - start.timeInMillis)
    return TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS) + 1
}