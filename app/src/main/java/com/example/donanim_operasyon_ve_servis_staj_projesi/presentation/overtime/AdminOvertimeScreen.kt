package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.overtime

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.OvertimeViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOvertimeScreen(
    overtimeViewModel: OvertimeViewModel,
    personnelViewModel: PersonnelViewModel,
    onNavigateBack: () -> Unit
) {
    val overtimes by overtimeViewModel.allOvertimes.collectAsState()
    val personnelList by personnelViewModel.personnelList.collectAsState()

    // Ekran ilk açıldığında yalnızca bir kez veri akışını başlatır.
    LaunchedEffect(Unit) {
        overtimeViewModel.loadAllOvertimes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fazla Mesai Takibi",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

            Text(
                text = "Tüm Fazla Mesai Kayıtları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (overtimes.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kayıtlı fazla mesai bulunmuyor.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        items = overtimes,
                        key = { item -> item.id }
                    ) { item ->

                        val personnel = personnelList.find {
                            it.id == item.personnelId
                        }

                        val personnelName =
                            personnel?.fullName
                                ?: "Personel #${item.personnelId}"

                        val startStr = remember(item.startTime) {
                            SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                Locale.getDefault()
                            ).format(
                                Date(item.startTime)
                            )
                        }

                        val endStr = remember(item.endTime) {
                            SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                Locale.getDefault()
                            ).format(
                                Date(item.endTime)
                            )
                        }

                        val statusText = when (item.status) {
                            "APPROVED" -> "Onaylandı"
                            "REJECTED" -> "Reddedildi"
                            else -> "Onay Bekliyor"
                        }

                        val statusColor = when (item.status) {
                            "APPROVED" ->
                                MaterialTheme.colorScheme.primaryContainer

                            "REJECTED" ->
                                MaterialTheme.colorScheme.errorContainer

                            else ->
                                MaterialTheme.colorScheme.surfaceVariant
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = personnelName,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusColor
                                    ) {

                                        Text(
                                            text = statusText,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 2.dp
                                            ),
                                            style =
                                                MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text =
                                        "Süre: ${item.durationMinutes} dakika",
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (item.serviceRecordId != null) {
                                    Text(
                                        text =
                                            "İş Emri ID: #${item.serviceRecordId}",
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "Başlangıç: $startStr",
                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Bitiş: $endStr",
                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )

                                if (!item.description.isNullOrBlank()) {
                                    Text(
                                        text =
                                            "Açıklama: ${item.description}",
                                        style =
                                            MaterialTheme.typography.bodySmall,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (item.status == "PENDING") {

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {

                                        Button(
                                            onClick = {
                                                overtimeViewModel
                                                    .approveOvertime(item)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape =
                                                RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Onayla")
                                        }

                                        Button(
                                            onClick = {
                                                overtimeViewModel
                                                    .rejectOvertime(item)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor =
                                                        MaterialTheme.colorScheme.error
                                                ),
                                            shape =
                                                RoundedCornerShape(8.dp)
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
    }
}