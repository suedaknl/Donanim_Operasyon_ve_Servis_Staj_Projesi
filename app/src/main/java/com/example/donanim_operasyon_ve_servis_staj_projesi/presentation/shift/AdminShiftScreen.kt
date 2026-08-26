package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.shift

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ShiftEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.ShiftViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShiftScreen(
    shiftViewModel: ShiftViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateBack: () -> Unit
) {
    val personnelList by personnelViewModel.personnelList.collectAsState()
    val shifts by shiftViewModel.personnelShifts.collectAsState()
    val errorMessage by shiftViewModel.errorMessage.collectAsState()

    var selectedPersonnel by remember { mutableStateOf<Personnel?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedPersonnel) {
        selectedPersonnel?.let {
            shiftViewModel.loadPersonnelShifts(it.id)
        }
    }

    errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            shiftViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vardiya Yönetimi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedPersonnel != null) {
                FloatingActionButton(
                    onClick = {
                        editingShift = null
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Vardiya Ekle")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Personel Seçin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (personnelList.isEmpty()) {
                Text("Kayıtlı personel bulunamadı.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(personnelList) { personnel ->
                        val isSelected = selectedPersonnel?.id == personnel.id
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPersonnel = personnel },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = personnel.fullName, fontWeight = FontWeight.SemiBold)
                                Text(text = personnel.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            if (selectedPersonnel == null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Vardiyalarını görmek için lütfen yukarıdan bir personel seçin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(
                    text = "${selectedPersonnel?.fullName} - Vardiya Listesi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (shifts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Bu personele ait kayıtlı vardiya bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shifts) { shift ->
                            ShiftItemCard(
                                shift = shift,
                                onEdit = {
                                    editingShift = shift
                                    showAddDialog = true
                                },
                                onCancel = {
                                    shiftViewModel.cancelShift(shift)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && selectedPersonnel != null) {
        ShiftFormDialog(
            initialShift = editingShift,
            onDismiss = { showAddDialog = false },
            onSave = { date, start, end ->
                if (editingShift == null) {
                    shiftViewModel.createShift(selectedPersonnel!!.id, date, start, end) { success ->
                        if (success) showAddDialog = false
                    }
                } else {
                    val updated = editingShift!!.copy(shiftDate = date, startTime = start, endTime = end)
                    shiftViewModel.updateShift(updated) { success ->
                        if (success) showAddDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun ShiftItemCard(shift: ShiftEntity, onEdit: () -> Unit, onCancel: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Tarih: ${shift.shiftDate}", fontWeight = FontWeight.Bold)
                Text(text = "Saat: ${shift.startTime} - ${shift.endTime}", style = MaterialTheme.typography.bodyMedium)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (shift.status) {
                        "ACTIVE" -> MaterialTheme.colorScheme.primaryContainer
                        "COMPLETED" -> MaterialTheme.colorScheme.secondaryContainer
                        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = shift.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (shift.status != "CANCELLED" && shift.status != "COMPLETED") {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Cancel, contentDescription = "İptal Et", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftFormDialog(
    initialShift: ShiftEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var dateStr by remember { mutableStateOf(initialShift?.shiftDate ?: "") }
    var startTimeStr by remember { mutableStateOf(initialShift?.startTime ?: "08:00") }
    var endTimeStr by remember { mutableStateOf(initialShift?.endTime ?: "17:00") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialShift == null) "Yeni Vardiya Ekle" else "Vardiya Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = validationError!!,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Tarih Seçici Alanı
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tarih (YYYY-MM-DD)") },
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Tarih Seç") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Başlangıç Saati Seçici Alanı (TimePickerDialog)
                OutlinedTextField(
                    value = startTimeStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Başlangıç Saati (HH:mm)") },
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Saat Seç") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        val parts = startTimeStr.split(":")
                        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                        val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                            },
                            initHour,
                            initMinute,
                            true // 24 saat formatı
                        ).show()
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Bitiş Saati Seçici Alanı (TimePickerDialog)
                OutlinedTextField(
                    value = endTimeStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bitiş Saati (HH:mm)") },
                    trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Saat Seç") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        val parts = endTimeStr.split(":")
                        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
                        val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                            },
                            initHour,
                            initMinute,
                            true // 24 saat formatı
                        ).show()
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (dateStr.isBlank()) {
                    validationError = "Lütfen bir tarih seçin."
                    return@Button
                }
                if (startTimeStr.isBlank() || endTimeStr.isBlank()) {
                    validationError = "Başlangıç ve bitiş saatleri boş olamaz."
                    return@Button
                }
                if (endTimeStr <= startTimeStr) {
                    validationError = "Bitiş saati başlangıç saatinden büyük olmalıdır."
                    return@Button
                }
                validationError = null
                onSave(dateStr, startTimeStr, endTimeStr)
            }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}