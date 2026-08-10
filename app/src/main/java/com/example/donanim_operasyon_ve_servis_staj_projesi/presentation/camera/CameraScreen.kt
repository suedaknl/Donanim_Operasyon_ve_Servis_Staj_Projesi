package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.donanim_operasyon_ve_servis_staj_projesi.utils.FileHelper

@Composable
fun CameraScreen(
    onPhotoCaptured: (String) -> Unit, // Başarılı çekimde URI dönecek
    onCancel: () -> Unit // İptal edildiğinde veya geri dönüldüğünde tetiklenecek
) {
    val context = LocalContext.current
    var hasCamPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Runtime (Çalışma Zamanı) İzin Yönetimi
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCamPermission = isGranted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCamPermission = true
            permissionRequested = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!permissionRequested) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (!hasCamPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Kamera izni verilmedi. Fotoğraf çekebilmek için ayarlardan izin vermelisiniz.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) { Text("Geri Dön") }
        }
    } else {
        CameraPreviewContent(onPhotoCaptured = onPhotoCaptured, onCancel = onCancel)
    }
}

@Composable
private fun CameraPreviewContent(
    onPhotoCaptured: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // LifecycleCameraController ile kamera arka planda açık kalmaz, yaşam döngüsüne bağlanır
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val photoFile = FileHelper.createPhotoFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    cameraController.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                                Toast.makeText(context, "Fotoğraf başarıyla kaydedildi.", Toast.LENGTH_SHORT).show()
                                onPhotoCaptured(savedUri.toString())
                            }
                            override fun onError(exc: ImageCaptureException) {
                                Toast.makeText(context, "Hata: ${exc.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Fotoğraf Çek", modifier = Modifier.size(32.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Kapatma / Geri Butonu
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "İptal", tint = Color.White)
            }
        }
    }
}