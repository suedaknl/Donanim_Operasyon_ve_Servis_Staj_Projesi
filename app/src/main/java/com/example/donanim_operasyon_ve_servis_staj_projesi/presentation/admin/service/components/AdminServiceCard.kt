package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.admin.service.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus

@Composable
fun AdminServiceCard(
    record: ServiceRecord,
    personnelList: List<Personnel>,
    onClick: () -> Unit
) {
    val assignedPersonnelName =
        record.assignedPersonnelName
            ?: personnelList.find {
                it.id == record.assignedPersonnelId ||
                        it.firebaseUid == record.assignedPersonnelUid
            }?.fullName
            ?: "Atanmadı"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = record.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Cihaz: ${record.deviceType} - ${record.deviceModel}", style = MaterialTheme.typography.bodyMedium)
            Text("Lokasyon: ${record.location}", style = MaterialTheme.typography.bodyMedium)
            Text("Öncelik: ${record.priority}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)

            if (record.status == ServiceStatus.IPTAL && !record.rejectionReason.isNullOrBlank()) {
                Text(
                    "Red Nedeni: ${record.rejectionReason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Atanan Personel", style = MaterialTheme.typography.labelSmall)
                        Text(assignedPersonnelName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Durum", style = MaterialTheme.typography.labelSmall)
                        Text(record.status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}