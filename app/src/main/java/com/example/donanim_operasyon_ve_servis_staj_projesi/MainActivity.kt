package com.example.donanim_operasyon_ve_servis_staj_projesi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.donanim_operasyon_ve_servis_staj_projesi.navigation.AppNavigation
import com.example.donanim_operasyon_ve_servis_staj_projesi.ui.theme.Donanim_Operasyon_ve_Servis_Staj_ProjesiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.NotificationRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Donanim_Operasyon_ve_Servis_Staj_ProjesiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authRepository = authRepository,
                        notificationRepository = notificationRepository
                    )
                }
            }
        }
    }
}