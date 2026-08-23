package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.leave

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current

    LaunchedEffect(personnelId) {
        leaveViewModel.loadPersonnelRequests(personnelId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İzin Taleplerim", fontWeight = FontWeight.Bold) },
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
                onClick = { showDialog = true },
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = req.leaveType, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusColor
                                    ) {
                                        Text(
                                            text = statusText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(text = "Tarih: ${req.startDate} / ${req.endDate}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Açıklama: ${req.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
            onDismiss = { showDialog = false },
            onSubmit = { start, end, type, desc ->
                leaveViewModel.createRequest(personnelId, start, end, type, desc) { success ->
                    if (success) {
                        showDialog = false
                        leaveViewModel.loadPersonnelRequests(personnelId)
                    }
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
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    val leaveTypes = listOf(
        "Yıllık İzin",
        "Mazeret İzni",
        "Hastalık / Rapor",
        "Ücretsiz İzin",
        "Evlilik İzni",
        "Doğum / Babalık İzni",
        "Diğer"
    )
    var leaveType by remember { mutableStateOf(leaveTypes[0]) }
    var expanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni İzin Talebi", fontWeight = FontWeight.Bold) },
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
                Text("Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}