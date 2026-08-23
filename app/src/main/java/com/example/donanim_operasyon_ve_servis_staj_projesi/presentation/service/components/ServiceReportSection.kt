package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.detail.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.ServiceReportPdfGenerator

@Composable
fun ServiceReportSection(
    service: ServiceRecord,
    combinedNotes: List<ServiceNote>,
    combinedPhotos: List<ServicePhoto>,
    serviceHistory: List<Map<String, Any>>,
    effectiveSignaturePath: String?,
    signatureData: String?
) {
    val context = LocalContext.current

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

                // -----------------------------------------------------
                // PDF GÖRÜNTÜLE
                // -----------------------------------------------------

                Button(
                    onClick = {

                        val pdfFile =
                            ServiceReportPdfGenerator.generatePdf(
                                context = context,
                                record = service,
                                notes = combinedNotes,
                                photos = combinedPhotos,
                                signaturePath = effectiveSignaturePath,

                                // X-Y-P verisi artık null gönderilmiyor
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

                // -----------------------------------------------------
                // PDF PAYLAŞ
                // -----------------------------------------------------

                OutlinedButton(
                    onClick = {

                        val pdfFile =
                            ServiceReportPdfGenerator.generatePdf(
                                context = context,
                                record = service,
                                notes = combinedNotes,
                                photos = combinedPhotos,
                                signaturePath = effectiveSignaturePath,

                                // Burada da gerçek X-Y-P verisi gönderiliyor
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
                                "Önce PDF üretilmelidir.",
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