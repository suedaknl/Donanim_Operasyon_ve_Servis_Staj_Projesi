package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.AuthRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.repository.NotificationRepository
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    notificationRepository: NotificationRepository,
    onSplashFinished: (String) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "AlphaAnimation"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000),
        label = "ScaleAnimation"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1500L)

        val currentUser = authRepository.getCurrentUser()
        val savedRole = sessionManager.getUserRole()
        val savedPersonnelId = sessionManager.getPersonnelId()

        if (currentUser != null && (!savedRole.isNullOrBlank() || savedPersonnelId > 0)) {
            try {
                val role = savedRole ?: "PERSONNEL"
                val personnelId = if (role == "PERSONNEL") savedPersonnelId else null
                val fcmToken = FirebaseMessaging.getInstance().token.await()

                notificationRepository.saveToken(
                    uid = currentUser.uid,
                    role = role,
                    personnelId = personnelId,
                    token = fcmToken
                )
            } catch (e: Exception) {}

            val role = savedRole ?: "PERSONNEL"
            if (role == "ADMIN") {
                onSplashFinished("home")
            } else {
                val pId = sessionManager.getPersonnelId()
                if (pId > 0) {
                    onSplashFinished("personnel_main/$pId")
                } else {
                    onSplashFinished("welcome")
                }
            }
        } else {
            onSplashFinished("welcome")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Uygulama Logosu",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Servis Takip",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Sistem hazırlanıyor...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(alphaAnim)
        )

        Spacer(modifier = Modifier.height(16.dp))

        CircularProgressIndicator(
            modifier = Modifier.alpha(alphaAnim),
            color = MaterialTheme.colorScheme.primary
        )
    }
}