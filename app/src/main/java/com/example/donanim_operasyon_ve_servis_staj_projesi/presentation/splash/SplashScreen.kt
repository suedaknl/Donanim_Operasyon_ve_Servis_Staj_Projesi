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
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.SessionManager

@Composable
fun SplashScreen(
    // DİKKAT: Artık yönlendirilecek rotayı (String) dışarı iletiyor
    onSplashFinished: (String) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }

    // Animasyonların tetikleyicisi olarak kullanılacak state
    var startAnimation by remember { mutableStateOf(false) }

    // Opaklık (Fade) Animasyonu
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "AlphaAnimation"
    )

    // Büyüme (Scale) Animasyonu
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000),
        label = "ScaleAnimation"
    )

    // Ekran açıldığı an animasyonu başlatıp 2 saniye bekler, sonra duruma göre yönlendirir
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000L)

        // --- YENİ EKLENEN KISIM: Auth ve Beni Hatırla Kontrolü ---
        val currentUser = authRepository.getCurrentUser()
        val isRememberMe = sessionManager.isRememberMe()

        if (currentUser != null && isRememberMe) {
            // Firebase Auth geçerli ve kullanıcı beni hatırla demiş
            val role = sessionManager.getUserRole()
            if (role == "ADMIN") {
                onSplashFinished("home")
            } else if (role == "PERSONNEL") {
                val pId = sessionManager.getPersonnelId()
                onSplashFinished("user_form/$pId")
            } else {
                onSplashFinished("welcome")
            }
        } else {
            // Kullanıcı oturumu Firebase'de açık kalmış ama cihazda "Beni Hatırla" DEMEMİŞ.
            // Güvenlik gereği oturumu temizliyoruz.
            if (currentUser != null && !isRememberMe) {
                authRepository.signOut()
                sessionManager.clearSession()
            }
            // Standart giriş seçim ekranına yönlendir
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
        // Ortadaki Büyük İkon
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

        // Büyük Başlık
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

        // Hazırlanıyor yazısı (Buna sadece fade efekti uyguladık)
        Text(
            text = "Sistem hazırlanıyor...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(alphaAnim)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Opsiyonel olarak bir yükleme animasyonu (Tasarıma ufak bir hareket katar)
        CircularProgressIndicator(
            modifier = Modifier.alpha(alphaAnim),
            color = MaterialTheme.colorScheme.primary
        )
    }
}