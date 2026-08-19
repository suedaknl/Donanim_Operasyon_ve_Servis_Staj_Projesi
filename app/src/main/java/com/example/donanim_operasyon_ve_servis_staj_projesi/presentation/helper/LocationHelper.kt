package com.example.donanim_operasyon_ve_servis_staj_projesi.utils

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    fun calculateDistanceInMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    // YENİ EKLENDİ: Mesafeyi kullanıcı dostu (m veya km) stringe çevirir
    fun formatDistance(meters: Float): String {
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", meters / 1000f)
        }
    }

    // LocationHelper.kt içerisine eklenecek:
    fun getLocationFreshness(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Konum bilgisi yok"
        val diffMinutes = (System.currentTimeMillis() - timestamp) / 60000
        return when {
            diffMinutes < 2 -> "Güncel"
            diffMinutes < 5 -> "Yakın zamanda güncellendi"
            else -> "Eski konum ($diffMinutes dk önce)"
        }
    }

    // YENİ EKLENDİ: Google Maps veya cihazdaki varsayılan harita uygulamasını açar
    fun openDirections(context: Context, lat: Double, lon: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lon")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Google Maps yoksa güvenli fallback: Jenerik harita intenti
            val genericUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            val genericIntent = Intent(Intent.ACTION_VIEW, genericUri)
            try {
                context.startActivity(genericIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(context, "Cihazınızda harita uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}