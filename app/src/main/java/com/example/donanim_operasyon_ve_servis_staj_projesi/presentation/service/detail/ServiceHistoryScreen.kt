package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    viewModel: ServiceViewModel,
    firestoreId: String,
    serviceId: Int,
    companyName: String,
    onNavigateBack: () -> Unit
) {
    val serviceHistory by viewModel.serviceHistory.collectAsState()

    LaunchedEffect(firestoreId) {
        if (firestoreId.isNotBlank()) {
            viewModel.loadServiceHistory(firestoreId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İşlem Geçmişi", fontWeight = FontWeight.Bold) },
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
            // Bilgi Başlığı
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "İş #$serviceId • $companyName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Bu iş emrine ait tüm kronolojik işlem adımları aşağıda listelenmektedir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timeline Bileşeni
            ServiceHistoryTimelineSection(historyList = serviceHistory)
        }
    }
}