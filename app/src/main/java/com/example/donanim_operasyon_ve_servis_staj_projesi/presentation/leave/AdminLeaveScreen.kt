package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.leave

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.LeaveRequestEntity
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.LeaveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeaveScreen(
    leaveViewModel: LeaveViewModel,
    onNavigateBack: () -> Unit
) {
    val pendingRequests by leaveViewModel.pendingRequests.collectAsState()
    val capacityWarning by leaveViewModel.capacityWarning.collectAsState()
    val successMsg by leaveViewModel.successMessage.collectAsState()
    val errorMsg by leaveViewModel.errorMessage.collectAsState()

    var rejectTargetId by remember { mutableStateOf<Int?>(null) }
    var adminNoteInput by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        leaveViewModel.loadPendingRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bekleyen İzin Talepleri", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pendingRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Bekleyen izin talebi bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingRequests) { req ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Personel ID: #${req.personnelId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = "İzin Türü: ${req.leaveType}")
                                Text(text = "Tarih: ${req.startDate} / ${req.endDate}")
                                Text(text = "Açıklama: ${req.description}", style = MaterialTheme.typography.bodySmall)

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
        }
    }

    // %50 Kapasite Uyarısı Dialogu
    if (capacityWarning != null) {
        AlertDialog(
            onDismissRequest = { leaveViewModel.clearMessages() },
            title = { Text("Personel Kapasitesi Uyarısı", fontWeight = FontWeight.Bold) },
            text = { Text(capacityWarning!!) },
            confirmButton = {
                Button(onClick = {
                    leaveViewModel.confirmApproveDespiteCapacity()
                    leaveViewModel.loadPendingRequests()
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

    // Reddetme Notu Dialogu
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
                        leaveViewModel.loadPendingRequests()
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
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            leaveViewModel.clearMessages()
            leaveViewModel.loadPendingRequests()
        }
    }
}