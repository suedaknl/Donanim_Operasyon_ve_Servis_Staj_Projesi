package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus

@Composable
fun ServiceProcessSection(
    service: ServiceRecord,
    isRejected: Boolean,
    adminActualStep: Int,
    operationalNotes: List<ServiceNote>,
    operationalPhotos: List<ServicePhoto>,
    onImageClick: (String) -> Unit
) {
    Text("Saha İşlemleri ve Takip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    if (service.status == ServiceStatus.BEKLIYOR && !isRejected) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Bu aşamada henüz işlem yapılmadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        if (service.status == ServiceStatus.PARCA_BEKLENIYOR) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "🟡 Parça Bekleniyor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(text = "Personel işlemi durdurdu ve gerekli yedek parça teminini bekliyor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Saha İşlem Notları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                if (operationalNotes.isEmpty()) {
                    Text("Eklenmiş işlem notu bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    operationalNotes.forEach { note -> Text("• ${note.note}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("İşlem Fotoğrafları", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                if (operationalPhotos.isEmpty()) {
                    Text("Eklenmiş fotoğraf bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val groupedPhotos = operationalPhotos.groupBy { it.photoType ?: it.photoCategory ?: "DİĞER" }
                    groupedPhotos.forEach { (category, photos) ->
                        Text(text = category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photos) { photo ->
                                AsyncImage(model = photo.localUri, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).clickable { onImageClick(photo.localUri) }, contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
        }
    }
}