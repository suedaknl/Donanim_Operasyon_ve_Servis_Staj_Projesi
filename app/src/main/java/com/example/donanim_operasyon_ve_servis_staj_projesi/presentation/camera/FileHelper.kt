package com.example.donanim_operasyon_ve_servis_staj_projesi.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object FileHelper {
    fun createPhotoFile(context: Context): File {
        // Benzersiz bir dosya adı oluştur (Örn: JPEG_20260810_145107)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val fileName = "JPEG_${timeStamp}_"

        // Uygulamanın sadece kendisinin erişebileceği internal (dahili) klasör oluşturuluyor
        val storageDir = context.getDir("service_photos", Context.MODE_PRIVATE)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        // .jpg formatında boş bir dosya yaratıp döndürüyoruz
        return File.createTempFile(fileName, ".jpg", storageDir)
    }
}