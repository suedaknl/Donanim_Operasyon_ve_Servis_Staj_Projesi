package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.leave

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.LeaveViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelLeaveScreen(
    personnelId: Int,
    leaveViewModel: LeaveViewModel,
    onNavigateBack: () -> Unit
) {
    val requests by leaveViewModel.personnelRequests.collectAsState()
    val successMsg by leaveViewModel.successMessage.collectAsState()
    val errorMsg by leaveViewModel.errorMessage.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingRequest by remember { mutableStateOf<LeaveRequestEntity?>(null) }
    var deletingRequest by remember { mutableStateOf<LeaveRequestEntity?>(null) }

    val context = LocalContext.current

    LaunchedEffect(personnelId) {
        leaveViewModel.loadPersonnelRequests(personnelId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İzin Taleplerim", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRequest = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "İzin Talep Et")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Geçmiş Talepler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Henüz oluşturulmuş izin talebiniz bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(requests) { req ->
                        val (statusText, statusColor) = when (req.status) {
                            "APPROVED" -> "Onaylandı" to MaterialTheme.colorScheme.primaryContainer
                            "REJECTED" -> "Reddedildi" to MaterialTheme.colorScheme.errorContainer
                            else -> "Bekliyor" to MaterialTheme.colorScheme.surfaceVariant
                        }

                        val isPending = req.status.equals("PENDING", ignoreCase = true)

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = req.leaveType,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusColor
                                        ) {
                                            Text(
                                                text = statusText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }

                                        // Sadece PENDING durumundakiler için Düzenle ve Sil butonları
                                        if (isPending) {
                                            IconButton(
                                                onClick = {
                                                    editingRequest = req
                                                    showDialog = true
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(
                                                onClick = {
                                                    deletingRequest = req
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                                Text(text = "Tarih: ${req.startDate} / ${req.endDate}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Açıklama: ${req.description ?: req.reason.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (!req.adminNote.isNullOrBlank()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text(text = "Admin Notu: ${req.adminNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        LeaveRequestFormDialog(
            initialRequest = editingRequest,
            onDismiss = {
                showDialog = false
                editingRequest = null
            },
            onSubmit = { start, end, type, desc ->
                if (editingRequest != null) {
                    leaveViewModel.updatePersonnelRequest(
                        editingRequest!!.copy(
                            startDate = start,
                            endDate = end,
                            leaveType = type,
                            description = desc
                        )
                    ) { success ->
                        if (success) {
                            showDialog = false
                            editingRequest = null
                            leaveViewModel.loadPersonnelRequests(personnelId)
                        }
                    }
                } else {
                    leaveViewModel.createRequest(personnelId, start, end, type, desc) { success ->
                        if (success) {
                            showDialog = false
                            leaveViewModel.loadPersonnelRequests(personnelId)
                        }
                    }
                }
            }
        )
    }

    if (deletingRequest != null) {
        AlertDialog(
            onDismissRequest = { deletingRequest = null },
            title = { Text("İzin Talebini Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Bu izin talebini silmek istediğinize emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingRequest?.let { req ->
                            leaveViewModel.deletePersonnelRequest(req) { success ->
                                if (success) {
                                    deletingRequest = null
                                    leaveViewModel.loadPersonnelRequests(personnelId)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRequest = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    successMsg?.let {
        LaunchedEffect(it) {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            leaveViewModel.clearMessages()
        }
    }

    errorMsg?.let {
        LaunchedEffect(it) {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            leaveViewModel.clearMessages()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestFormDialog(
    initialRequest: LeaveRequestEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var startDate by remember { mutableStateOf(initialRequest?.startDate ?: "") }
    var endDate by remember { mutableStateOf(initialRequest?.endDate ?: "") }

    val leaveTypes = listOf(
        "Yıllık İzin",
        "Mazeret İzni",
        "Hastalık / Rapor",
        "Ücretsiz İzin",
        "Evlilik İzni",
        "Doğum / Babalık İzni",
        "Diğer"
    )
    var leaveType by remember { mutableStateOf(initialRequest?.leaveType.takeIf { !it.isNullOrBlank() } ?: leaveTypes[0]) }
    var expanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf(initialRequest?.description ?: initialRequest?.reason ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRequest == null) "Yeni İzin Talebi" else "İzin Talebini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Başlangıç Tarihi (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> startDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d) },
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bitiş Tarihi (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d) },
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = leaveType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("İzin Türü") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        leaveTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    leaveType = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama / Sebep") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(startDate, endDate, leaveType, description) }) {
                Text(if (initialRequest == null) "Gönder" else "Güncelle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}