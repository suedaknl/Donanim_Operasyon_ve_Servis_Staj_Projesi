package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneUserFormScreen(
    viewModel: ServiceViewModel,
    personnelId: Int,
    onServiceClick: (Int) -> Unit, // YENİ: Kart tıklama callback'i (ServiceId taşır)
    onLogOut: () -> Unit
) {
    LaunchedEffect(personnelId) {
        viewModel.loadRecordsForPersonnel(personnelId)
    }

    val personnelRecords by viewModel.personnelServiceRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atanan İş Emirlerim", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Çıkış Yap")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        if (personnelRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Şu anda size atanmış aktif bir iş emri bulunmuyor.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(personnelRecords) { record ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onServiceClick(record.id) }, // YENİ: Kart tıklanınca detay ekranına yönlendirir
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = record.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("Cihaz: ${record.deviceType} - ${record.deviceModel}")
                            Text("Lokasyon: ${record.location}")
                            Text("Öncelik: ${record.priority}", color = MaterialTheme.colorScheme.error)

                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Durum: ${record.status}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}