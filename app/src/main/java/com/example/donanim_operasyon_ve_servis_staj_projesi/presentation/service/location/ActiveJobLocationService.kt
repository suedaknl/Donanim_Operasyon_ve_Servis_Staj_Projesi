package com.example.donanim_operasyon_ve_servis_staj_projesi.service.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.donanim_operasyon_ve_servis_staj_projesi.MainActivity
import com.example.donanim_operasyon_ve_servis_staj_projesi.R
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.remote.FirestorePersonnelDataSource
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActiveJobLocationService : Service() {

    @Inject
    lateinit var personnelDataSource: FirestorePersonnelDataSource

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var personnelUid: String? = null
    private var serviceId: Int = 0

    companion object {
        const val ACTION_START = "ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_LOCATION_SERVICE"
        const val EXTRA_PERSONNEL_UID = "EXTRA_PERSONNEL_UID"
        const val EXTRA_SERVICE_ID = "EXTRA_SERVICE_ID"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "active_job_location"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("ACTIVE_JOB_LOCATION_SERVICE", "ACTIVE_JOB_LOCATION_SERVICE onCreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val lat = location.latitude
                    val lng = location.longitude
                    android.util.Log.d("ACTIVE_JOB_LOCATION_SERVICE", "ACTIVE_JOB_LOCATION_SERVICE UPDATE lat=$lat lng=$lng")

                    if (!personnelUid.isNullOrEmpty()) {
                        serviceScope.launch {
                            personnelDataSource.updatePersonnelLocation(personnelUid!!, lat, lng)
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                personnelUid = intent.getStringExtra(EXTRA_PERSONNEL_UID)
                serviceId = intent.getIntExtra(EXTRA_SERVICE_ID, 0)
                android.util.Log.d("ACTIVE_JOB_LOCATION_SERVICE", "ACTIVE_JOB_LOCATION_SERVICE START activeJob=$serviceId")

                startForeground(NOTIFICATION_ID, createNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                android.util.Log.d("ACTIVE_JOB_LOCATION_SERVICE", "ACTIVE_JOB_LOCATION_SERVICE STOP")
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000L).apply {
            setMinUpdateIntervalMillis(15000L)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Konum Takibi Aktif",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aktif saha görevi sırasında konumunuz güncelleniyor."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Konum Takibi Aktif")
            .setContentText("Aktif saha görevi sırasında konumunuz güncelleniyor.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
        android.util.Log.d("ACTIVE_JOB_LOCATION_SERVICE", "ACTIVE_JOB_LOCATION_SERVICE onDestroy")
    }
}