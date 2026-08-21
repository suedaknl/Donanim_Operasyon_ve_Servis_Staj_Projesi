package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.InfoRow

@Composable
fun ServiceInfoSection(
    service: ServiceRecord,
    assignedPersonnelName: String
) {
    Text("İş Emri Temel Bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "İş No: #${service.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Surface(
                    color = when (service.priority) {
                        "Yüksek", "Acil" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Öncelik: ${service.priority}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Text(text = service.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            InfoRow(icon = Icons.Default.Person, label = "Yetkili", value = service.contactPerson ?: "Belirtilmedi")
            InfoRow(icon = Icons.Default.Phone, label = "Telefon", value = service.contactPhone ?: "Belirtilmedi")
            InfoRow(icon = Icons.Default.Devices, label = "Cihaz", value = "${service.deviceType} (${service.deviceModel})")
            InfoRow(icon = Icons.Default.ConfirmationNumber, label = "Seri No", value = service.serialNumber)
            InfoRow(icon = Icons.Default.LocationOn, label = "Lokasyon", value = service.location)
            InfoRow(icon = Icons.Default.Home, label = "Adres", value = service.address ?: "Belirtilmedi")
            InfoRow(icon = Icons.Default.ReportProblem, label = "Arıza Nedeni", value = service.issueDescription)
            InfoRow(icon = Icons.Default.Badge, label = "Atanan Personel", value = assignedPersonnelName)
            InfoRow(icon = Icons.Default.Info, label = "Mevcut Durum", value = service.status)
        }
    }
}