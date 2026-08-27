package com.example.donanim_operasyon_ve_servis_staj_projesi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.converter.SignatureConverter
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.signature.SignatureRenderer
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ServiceReportPdfGenerator {

    fun generatePdf(
        context: Context,
        record: ServiceRecord,
        notes: List<ServiceNote>,
        photos: List<ServicePhoto>,
        signaturePath: String?,
        signatureData: String?,
        history: List<Map<String, Any>>
    ): File? {

        return try {
            val pdfDocument = PdfDocument()

            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // ---------------------------------------------------------
            // PAINT & STYLING SETTINGS
            // ---------------------------------------------------------
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#333333")
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1565C0")
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            }

            val sectionHeaderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#0D47A1")
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            val subHeaderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#555555")
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }

            val grayLinePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }

            val primaryLinePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1565C0")
                strokeWidth = 2f
            }

            var yPos = 45f
            val leftMargin = 40f
            val rightMargin = pageWidth - 40f

            // ---------------------------------------------------------
            // SAYFA KONTROLÜ VE OTOMATİK SAYFALAMA
            // ---------------------------------------------------------
            fun checkNewPage(neededHeight: Float) {
                if (yPos + neededHeight > pageHeight - 50f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 45f
                }
            }

            // ---------------------------------------------------------
            // BAŞLIK / ÜST BİLGİ
            // ---------------------------------------------------------
            canvas.drawText("SERDİNÇ TEKNİK SERVİS RAPORU", leftMargin, yPos, titlePaint)
            yPos += 16f

            canvas.drawText("İş Emri #${record.id}  |  Firma: ${record.companyName}", leftMargin, yPos, subHeaderPaint)
            yPos += 8f

            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, primaryLinePaint)
            yPos += 22f

            // ---------------------------------------------------------
            // İŞ EMRİ BİLGİLERİ (TEK SÜTUN - TAŞMA YAPMAYAN DÜZEN)
            // ---------------------------------------------------------
            checkNewPage(180f)
            canvas.drawText("İŞ EMRİ DETAYLARI", leftMargin, yPos, sectionHeaderPaint)
            yPos += 16f

            val details = listOf(
                "İş Emri No: #${record.id}",
                "Firma: ${record.companyName}",
                "Yetkili Kişi: ${record.contactPerson ?: "Belirtilmemiş"}",
                "Yetkili Telefon: ${record.contactPhone ?: "Belirtilmemiş"}",
                "Cihaz Türü / Modeli: ${record.deviceType} - ${record.deviceModel}",
                "Seri No: ${record.serialNumber}",
                "Öncelik: ${record.priority}",
                "Lokasyon: ${record.location}",
                "Açık Adres: ${record.address ?: "Belirtilmemiş"}",
                "Planlanan Tarih: ${record.plannedDate ?: record.date}",
                if (record.latitude != null && record.longitude != null && record.latitude != 0.0) {
                    "Koordinat: ${record.latitude}, ${record.longitude}"
                } else null
            ).filterNotNull()

            details.forEach { detail ->
                checkNewPage(16f)
                canvas.drawText("• $detail", leftMargin, yPos, textPaint)
                yPos += 15f
            }

            yPos += 10f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, grayLinePaint)
            yPos += 20f

            // ---------------------------------------------------------
            // ARIZA / TALEP AÇIKLAMASI
            // ---------------------------------------------------------
            checkNewPage(45f)
            canvas.drawText("ARIZA / TALEP AÇIKLAMASI", leftMargin, yPos, sectionHeaderPaint)
            yPos += 16f

            canvas.drawText(
                record.issueDescription.ifBlank { "Açıklama girilmemiş." },
                leftMargin,
                yPos,
                textPaint
            )
            yPos += 24f

            // ---------------------------------------------------------
            // SERVİS SONUÇ BİLGİLERİ
            // ---------------------------------------------------------
            checkNewPage(55f)
            canvas.drawText("SERVİS SONUÇ BİLGİLERİ", leftMargin, yPos, sectionHeaderPaint)
            yPos += 16f

            canvas.drawText("Mevcut Durum: ${record.status}", leftMargin, yPos, textPaint)
            yPos += 16f

            if (!record.rejectionReason.isNullOrBlank()) {
                canvas.drawText("Red Nedeni: ${record.rejectionReason}", leftMargin, yPos, textPaint)
                yPos += 16f
            }

            yPos += 10f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, grayLinePaint)
            yPos += 20f

            // ---------------------------------------------------------
            // SERVİS NOTLARI
            // ---------------------------------------------------------
            checkNewPage(45f)
            canvas.drawText("SERVİS NOTLARI", leftMargin, yPos, sectionHeaderPaint)
            yPos += 16f

            if (notes.isEmpty()) {
                canvas.drawText("Servis notu bulunmamaktadır.", leftMargin, yPos, textPaint)
                yPos += 20f
            } else {
                notes.forEach { note ->
                    checkNewPage(18f)
                    canvas.drawText("• ${note.note}", leftMargin, yPos, textPaint)
                    yPos += 16f
                }
                yPos += 10f
            }

            // ---------------------------------------------------------
            // FOTOĞRAFLAR (GÜÇLENDİRİLMİŞ ÇÖZÜM)
            // ---------------------------------------------------------
            if (photos.isNotEmpty()) {
                checkNewPage(60f)
                canvas.drawText("İŞLEM FOTOĞRAFLARI", leftMargin, yPos, sectionHeaderPaint)
                yPos += 18f

                photos.forEach { photo ->
                    try {
                        val rawPath = listOf(photo.localUri, photo.photoUri)
                            .firstOrNull { !it.isNullOrBlank() }

                        if (!rawPath.isNullOrBlank()) {
                            val originalBitmap: Bitmap? = when {
                                rawPath.startsWith("content://") -> {
                                    try {
                                        context.contentResolver.openInputStream(Uri.parse(rawPath))?.use {
                                            BitmapFactory.decodeStream(it)
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                rawPath.startsWith("http://") || rawPath.startsWith("https://") -> {
                                    try {
                                        val url = URL(rawPath)
                                        val conn = url.openConnection() as HttpURLConnection
                                        conn.doInput = true
                                        conn.connect()
                                        conn.inputStream.use { BitmapFactory.decodeStream(it) }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                else -> {
                                    val cleanPath = rawPath.removePrefix("file://")
                                    val file = File(cleanPath)
                                    if (file.exists()) {
                                        BitmapFactory.decodeFile(file.absolutePath)
                                    } else {
                                        null
                                    }
                                }
                            }

                            if (originalBitmap != null) {
                                val maxWidth = 180f
                                val maxHeight = 130f
                                val scale = minOf(
                                    maxWidth / originalBitmap.width,
                                    maxHeight / originalBitmap.height
                                )

                                val scaledWidth = (originalBitmap.width * scale).toInt()
                                val scaledHeight = (originalBitmap.height * scale).toInt()

                                val bitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                                checkNewPage(scaledHeight.toFloat() + 35f)

                                val catText = "Kategori: ${photo.photoType ?: photo.photoCategory ?: "GENEL"}"
                                canvas.drawText(catText, leftMargin, yPos, subHeaderPaint)
                                yPos += 14f

                                canvas.drawBitmap(bitmap, leftMargin, yPos, null)
                                yPos += scaledHeight.toFloat() + 18f
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // ---------------------------------------------------------
            // MÜŞTERİ / YETKİLİ İMZASI (GÜÇLENDİRİLMİŞ ÇÖZÜM)
            // ---------------------------------------------------------
            checkNewPage(130f)
            canvas.drawText("MÜŞTERİ / YETKİLİ İMZASI", leftMargin, yPos, sectionHeaderPaint)
            yPos += 16f

            var sigBitmap: Bitmap? = null

            if (!signatureData.isNullOrBlank()) {
                try {
                    val strokes = SignatureConverter.fromJson(signatureData)
                    if (strokes.isNotEmpty()) {
                        sigBitmap = SignatureRenderer.renderBitmapFromStrokes(
                            strokes = strokes,
                            width = 800,
                            height = 300,
                            strokeWidth = 8f
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (sigBitmap == null && !signaturePath.isNullOrBlank()) {
                try {
                    sigBitmap = when {
                        signaturePath.startsWith("http://") || signaturePath.startsWith("https://") -> {
                            val url = URL(signaturePath)
                            val conn = url.openConnection() as HttpURLConnection
                            conn.doInput = true
                            conn.connect()
                            conn.inputStream.use { BitmapFactory.decodeStream(it) }
                        }
                        signaturePath.startsWith("content://") -> {
                            context.contentResolver.openInputStream(Uri.parse(signaturePath))?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                        else -> {
                            val cleanPath = signaturePath.removePrefix("file://")
                            val file = File(cleanPath)
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (sigBitmap != null) {
                val maxWidth = 200f
                val maxHeight = 80f
                val scale = minOf(
                    maxWidth / sigBitmap.width.toFloat(),
                    maxHeight / sigBitmap.height.toFloat()
                )

                val scaledWidth = (sigBitmap.width * scale).toInt()
                val scaledHeight = (sigBitmap.height * scale).toInt()
                val scaledSig = Bitmap.createScaledBitmap(sigBitmap, scaledWidth, scaledHeight, true)

                checkNewPage(scaledHeight.toFloat() + 30f)

                canvas.drawBitmap(scaledSig, leftMargin, yPos, null)
                yPos += scaledHeight.toFloat() + 8f

                canvas.drawText("Müşteri Onay İmzası", leftMargin, yPos, textPaint)
                yPos += 20f
            } else {
                canvas.drawText("İmza bulunmamaktadır.", leftMargin, yPos, textPaint)
                yPos += 20f
            }

            // ---------------------------------------------------------
            // İŞLEM GEÇMİŞİ (TARİHÇE)
            // ---------------------------------------------------------
            if (history.isNotEmpty()) {
                checkNewPage(50f)
                canvas.drawLine(leftMargin, yPos, rightMargin, yPos, grayLinePaint)
                yPos += 15f

                canvas.drawText("İŞLEM GEÇMİŞİ", leftMargin, yPos, sectionHeaderPaint)
                yPos += 16f

                history.forEach { h ->
                    checkNewPage(18f)
                    val title = h["title"] as? String ?: "İşlem"
                    val desc = h["description"] as? String ?: ""
                    val time = h["timestamp"] as? String ?: ""

                    canvas.drawText("• [$time] $title: $desc", leftMargin, yPos, textPaint)
                    yPos += 16f
                }
            }

            // ---------------------------------------------------------
            // PDF DOSYASINI KAYDET
            // ---------------------------------------------------------
            pdfDocument.finishPage(page)

            val safeCompanyName = record.companyName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(context.cacheDir, "Servis_Raporu_${record.id}_$safeCompanyName.pdf")

            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}