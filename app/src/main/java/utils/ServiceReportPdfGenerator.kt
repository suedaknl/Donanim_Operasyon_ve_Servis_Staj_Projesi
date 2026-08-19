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
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceClosingSignature
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceNote
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServicePhoto
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceRecord
import java.io.File
import java.io.FileOutputStream

object ServiceReportPdfGenerator {

    fun generatePdf(
        context: Context,
        record: ServiceRecord,
        notes: List<ServiceNote>,
        photos: List<ServicePhoto>,
        signaturePath: String?, // <-- Güncellenen imza kaynağı (Local URI veya Remote URL)
        history: List<Map<String, Any>>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#444444")
                textSize = 10f
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1976D2")
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
            }

            val headerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#333333")
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            val grayLinePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#CCCCCC")
                strokeWidth = 1f
            }

            var yPos = 40f
            val leftMargin = 40f
            val rightMargin = pageWidth - 40f

            fun checkNewPage(neededHeight: Float) {
                if (yPos + neededHeight > pageHeight - 40f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 40f
                }
            }

            // --- BAŞLIK ---
            canvas.drawText("SERDİNÇ SERVİS RAPORU", leftMargin, yPos, titlePaint)
            yPos += 18f
            canvas.drawText("İş Emri ID: #${record.id} | Firma: ${record.companyName}", leftMargin, yPos, headerPaint)
            yPos += 10f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, grayLinePaint)
            yPos += 20f

            // --- İŞ EMRİ BİLGİLERİ (KESİN TEK SÜTUN) ---
            checkNewPage(220f)
            canvas.drawText("İŞ EMRİ BİLGİLERİ", leftMargin, yPos, headerPaint)
            yPos += 18f

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
                if (record.latitude != null && record.longitude != null) "Koordinat: ${record.latitude}, ${record.longitude}" else null
            ).filterNotNull()

            // Tek sütun alt alta yazdırma
            details.forEach { detail ->
                checkNewPage(16f)
                canvas.drawText("• $detail", leftMargin, yPos, textPaint)
                yPos += 15f
            }

            yPos += 10f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, grayLinePaint)
            yPos += 20f

            // --- ARIZA / TALEP ---
            checkNewPage(50f)
            canvas.drawText("ARIZA / TALEP AÇIKLAMASI", leftMargin, yPos, headerPaint)
            yPos += 16f
            canvas.drawText(record.issueDescription.ifBlank { "Açıklama girilmemiş." }, leftMargin, yPos, textPaint)
            yPos += 25f

            // --- SERVİS BİLGİLERİ ---
            checkNewPage(60f)
            canvas.drawText("SERVİS SONUÇ BİLGİLERİ", leftMargin, yPos, headerPaint)
            yPos += 16f
            canvas.drawText("Mevcut Durum: ${record.status}", leftMargin, yPos, textPaint)
            yPos += 14f
            if (!record.rejectionReason.isNullOrBlank()) {
                canvas.drawText("Red Nedeni: ${record.rejectionReason}", leftMargin, yPos, textPaint)
                yPos += 14f
            }

            yPos += 10f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, grayLinePaint)
            yPos += 20f

            // --- SERVİS NOTLARI ---
            checkNewPage(50f)
            canvas.drawText("SERVİS NOTLARI", leftMargin, yPos, headerPaint)
            yPos += 16f
            if (notes.isEmpty()) {
                canvas.drawText("Servis notu bulunmamaktadır.", leftMargin, yPos, textPaint)
                yPos += 20f
            } else {
                notes.forEach { note ->
                    checkNewPage(20f)
                    canvas.drawText("• ${note.note}", leftMargin, yPos, textPaint)
                    yPos += 16f
                }
                yPos += 10f
            }

            // --- FOTOĞRAFLAR ---
            if (photos.isNotEmpty()) {
                checkNewPage(100f)
                canvas.drawText("İŞLEM FOTOĞRAFLARI", leftMargin, yPos, headerPaint)
                yPos += 16f

                photos.forEach { photo ->
                    try {
                        val path = photo.localUri.ifBlank { photo.photoUri }
                        if (!path.isNullOrBlank()) {
                            val originalBitmap = if (path.startsWith("content://")) {
                                val uri = Uri.parse(path)
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bmp = BitmapFactory.decodeStream(inputStream)
                                inputStream?.close()
                                bmp
                            } else if (path.startsWith("file://") || path.startsWith("/data/")) {
                                val cleanPath = path.removePrefix("file://")
                                BitmapFactory.decodeFile(cleanPath)
                            } else {
                                null
                            }

                            if (originalBitmap != null) {
                                val maxWidth = 150f
                                val maxHeight = 120f
                                val scale = minOf(maxWidth / originalBitmap.width, maxHeight / originalBitmap.height)
                                val scaledWidth = (originalBitmap.width * scale).toInt()
                                val scaledHeight = (originalBitmap.height * scale).toInt()
                                val bitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                                checkNewPage(scaledHeight.toFloat() + 30f)
                                canvas.drawText("Kategori: ${photo.photoType ?: photo.photoCategory ?: "DİĞER"}", leftMargin, yPos, textPaint)
                                yPos += 14f
                                canvas.drawBitmap(bitmap, leftMargin, yPos, null)
                                yPos += scaledHeight.toFloat() + 15f
                            }
                        }
                    } catch (e: Exception) {
                        // Fotoğraf yüklenemezse PDF çökmez, atlar
                    }
                }
            }

            // --- İMZA ---
            checkNewPage(110f)
            canvas.drawText("MÜŞTERİ / YETKİLİ İMZASI", leftMargin, yPos, headerPaint)
            yPos += 16f

            if (!signaturePath.isNullOrBlank()) {
                var sigBitmap: Bitmap? = null
                try {
                    if (signaturePath.startsWith("http://") || signaturePath.startsWith("https://")) {
                        val url = java.net.URL(signaturePath)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.doInput = true
                        connection.connect()
                        sigBitmap = BitmapFactory.decodeStream(connection.inputStream)
                    } else if (signaturePath.startsWith("content://")) {
                        val uri = Uri.parse(signaturePath)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        sigBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                    } else {
                        val cleanPath = signaturePath.removePrefix("file://")
                        val file = File(cleanPath)
                        if (file.exists()) {
                            sigBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            val uri = Uri.parse(signaturePath)
                            val inputStream = context.contentResolver.openInputStream(uri)
                            sigBitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                        }
                    }
                } catch (e: Exception) {
                    sigBitmap = null
                }

                if (sigBitmap != null) {
                    val maxWidth = 180f
                    val maxHeight = 80f
                    val scale = minOf(maxWidth / sigBitmap.width, maxHeight / sigBitmap.height)
                    val scaledWidth = (sigBitmap.width * scale).toInt()
                    val scaledHeight = (sigBitmap.height * scale).toInt()
                    val scaledSig = Bitmap.createScaledBitmap(sigBitmap, scaledWidth, scaledHeight, true)

                    checkNewPage(scaledHeight.toFloat() + 30f)
                    canvas.drawBitmap(scaledSig, leftMargin, yPos, null)
                    yPos += scaledHeight.toFloat() + 10f
                    canvas.drawText("Müşteri Onay İmzası", leftMargin, yPos, textPaint)
                    yPos += 20f
                } else {
                    canvas.drawText("İmza görseli yüklenemedi.", leftMargin, yPos, textPaint)
                    yPos += 20f
                }
            } else {
                canvas.drawText("İmza bulunmamaktadır.", leftMargin, yPos, textPaint)
                yPos += 20f
            }

            // --- TARİHÇE ---
            if (history.isNotEmpty()) {
                checkNewPage(60f)
                canvas.drawText("İŞLEM GEÇMİŞİ (TARİHÇE)", leftMargin, yPos, headerPaint)
                yPos += 16f

                history.forEach { h ->
                    checkNewPage(20f)
                    val title = h["title"] as? String ?: "İşlem"
                    val desc = h["description"] as? String ?: ""
                    val time = h["timestamp"] as? String ?: ""
                    canvas.drawText("• [$time] $title: $desc", leftMargin, yPos, textPaint)
                    yPos += 16f
                }
            }

            pdfDocument.finishPage(page)

            val safeCompanyName = record.companyName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(context.cacheDir, "Servis_Raporu_${record.id}_$safeCompanyName.pdf")
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}