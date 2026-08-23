package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.converter.SignatureConverter
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.signature.SignatureRenderer
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.ServiceReportPdfGenerator
@Composable
fun ServiceApprovalSection(
    service: ServiceRecord,
    assignedPersonnelName: String,
    isCompleted: Boolean,
    combinedNotes: List<ServiceNote>,
    combinedPhotos: List<ServicePhoto>,
    serviceHistory: List<Map<String, Any>>,
    effectiveSignaturePath: String?,
    signatureData: String?,
    onNavigateToEdit: (Int) -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current

    var isCompletionInfoExpanded by rememberSaveable { mutableStateOf(false) }
    var isClosingResultExpanded by rememberSaveable { mutableStateOf(false) }
    var isDigitalSignatureExpanded by rememberSaveable { mutableStateOf(false) }
    var isActionsExpanded by rememberSaveable { mutableStateOf(false) }

    val closingKeywords = listOf(
        "closing",
        "kapanis",
        "kapanış",
        "sonuc",
        "sonuç",
        "sonrasi",
        "sonrası"
    )

    val closingNoteItem = combinedNotes.firstOrNull { note ->
        closingKeywords.any {
            (note.noteType ?: "")
                .trim()
                .lowercase()
                .contains(it)
        }
    } ?: combinedNotes.lastOrNull()

    val closingAfterPhotos = combinedPhotos.filter { photo ->
        closingKeywords.any {
            (photo.photoType ?: photo.photoCategory ?: "")
                .trim()
                .lowercase()
                .contains(it)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "İş Sonucu & Onay",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // ---------------------------------------------------------
        // SERVİS RAPORU
        // ---------------------------------------------------------

        if (isCompleted) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Servis Raporu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Tamamlanan iş emrinin servis raporunu görüntüleyebilir veya paylaşabilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // -------------------------------------------------
                        // PDF GÖRÜNTÜLE
                        // -------------------------------------------------

                        Button(
                            onClick = {

                                val pdfFile = ServiceReportPdfGenerator.generatePdf(
                                    context = context,
                                    record = service,
                                    notes = combinedNotes,
                                    photos = combinedPhotos,
                                    signaturePath = effectiveSignaturePath,
                                    signatureData = signatureData,
                                    history = serviceHistory
                                )

                                if (
                                    pdfFile != null &&
                                    pdfFile.exists()
                                ) {

                                    val uri =
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )

                                    val viewIntent =
                                        Intent(Intent.ACTION_VIEW).apply {

                                            setDataAndType(
                                                uri,
                                                "application/pdf"
                                            )

                                            addFlags(
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        }

                                    try {

                                        context.startActivity(
                                            viewIntent
                                        )

                                    } catch (e: Exception) {

                                        Toast.makeText(
                                            context,
                                            "PDF okuyucu bulunamadı.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Servis raporu oluşturulamadı.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text("PDF Görüntüle")
                        }

                        // -------------------------------------------------
                        // PDF PAYLAŞ
                        // -------------------------------------------------

                        OutlinedButton(
                            onClick = {

                                val pdfFile = ServiceReportPdfGenerator.generatePdf(
                                    context = context,
                                    record = service,
                                    notes = combinedNotes,
                                    photos = combinedPhotos,
                                    signaturePath = effectiveSignaturePath,
                                    signatureData = signatureData,
                                    history = serviceHistory
                                )

                                if (
                                    pdfFile != null &&
                                    pdfFile.exists()
                                ) {

                                    val uri =
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )

                                    val shareIntent =
                                        Intent(Intent.ACTION_SEND).apply {

                                            type = "application/pdf"

                                            putExtra(
                                                Intent.EXTRA_STREAM,
                                                uri
                                            )

                                            addFlags(
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        }

                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            "Raporu Paylaş"
                                        )
                                    )

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Servis raporu oluşturulamadı.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text("Paylaş")
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // İŞ HENÜZ TAMAMLANMADI
        // ---------------------------------------------------------

        if (!isCompleted) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "İş henüz tamamlanmadı. Kapanış verileri bekleniyor.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        } else {

            // -----------------------------------------------------
            // İŞ TAMAMLAMA BİLGİSİ
            // -----------------------------------------------------

            ExpandableSection(
                title = "İş Tamamlama Bilgisi",
                expanded = isCompletionInfoExpanded,
                onExpandedChange = {
                    isCompletionInfoExpanded = it
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = "Bu iş emri başarıyla tamamlanmıştır.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Tamamlayan Personel: $assignedPersonnelName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // -----------------------------------------------------
            // KAPANIŞ AÇIKLAMASI + SONRASI FOTOĞRAFI
            // -----------------------------------------------------

            ExpandableSection(
                title = "Kapanış Açıklaması / Sonuç",
                expanded = isClosingResultExpanded,
                onExpandedChange = {
                    isClosingResultExpanded = it
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {

                if (closingNoteItem != null) {

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text = "Kapanış Açıklaması / Sonuç",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "• ${closingNoteItem.note}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (closingAfterPhotos.isNotEmpty()) {

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                text = "Sonrası Fotoğrafı",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            LazyRow(
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {

                                items(closingAfterPhotos) { photo ->

                                    AsyncImage(
                                        model = photo.localUri,
                                        contentDescription = "Sonrası Fotoğrafı",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    8.dp
                                                )
                                            )
                                            .clickable {
                                                onImageClick(
                                                    photo.localUri
                                                )
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------
            // MÜŞTERİ DİJİTAL İMZASI
            // -----------------------------------------------------

            ExpandableSection(
                title = "Müşteri Dijital İmzası",
                expanded = isDigitalSignatureExpanded,
                onExpandedChange = {
                    isDigitalSignatureExpanded = it
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        val signatureBitmap =
                            remember(signatureData) {

                                if (
                                    !signatureData.isNullOrBlank()
                                ) {

                                    val strokes =
                                        SignatureConverter.fromJson(
                                            signatureData
                                        )

                                    SignatureRenderer
                                        .renderBitmapFromStrokes(
                                            strokes = strokes,
                                            width = 800,
                                            height = 300,
                                            strokeWidth = 8f
                                        )

                                } else {
                                    null
                                }
                            }

                        when {

                            // Yeni X-Y-P sistemi
                            signatureBitmap != null -> {

                                Image(
                                    bitmap =
                                        signatureBitmap.asImageBitmap(),
                                    contentDescription = "Dijital İmza",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            // Eski PNG sistemi fallback
                            !effectiveSignaturePath.isNullOrBlank() -> {

                                AsyncImage(
                                    model = effectiveSignaturePath,
                                    contentDescription = "Dijital İmza",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            else -> {

                                Text(
                                    text = "İmza bulunmuyor.",
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            // -----------------------------------------------------
            // İŞLEMLER
            // -----------------------------------------------------

            ExpandableSection(
                title = "İşlemler",
                expanded = isActionsExpanded,
                onExpandedChange = {
                    isActionsExpanded = it
                }
            ) {

                if (
                    (
                            service.status == ServiceStatus.TAMAMLANDI ||
                                    service.status == ServiceStatus.IPTAL
                            ) &&
                    !service.isArchived
                ) {

                    Button(
                        onClick = onArchiveClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.secondary
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text("Arşivle")
                    }
                }

                Button(
                    onClick = {

                        val targetId =
                            if (service.id > 0) {
                                -service.id
                            } else {
                                service.id
                            }

                        onNavigateToEdit(
                            targetId
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.tertiary
                        ),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text("İş Emrini Çoğalt")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            onNavigateToEdit(
                                service.id
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text("Düzenle")
                    }

                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.error
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text("Sil")
                    }
                }
            }
        }
    }
}
