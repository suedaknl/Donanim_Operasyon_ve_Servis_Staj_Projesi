package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.service.form

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // Coil eklendi
// Paket isimlerini projenizdeki gerçek yollara göre kontrol edin:
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.utils.SignaturePad
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.utils.rememberSignatureController
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.utils.saveSignatureToInternalStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosingFormScreen(
    viewModel: ServiceViewModel,
    serviceId: Int,
    personnelId: Int,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    returnedPhotoUri: String?,
    onPhotoSaved: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val signatureController = rememberSignatureController()
    val closingNote by viewModel.closingNote.collectAsState()
    val closingState by viewModel.closingState.collectAsState()
    val closingAfterPhotoUri by viewModel.closingAfterPhotoUri.collectAsState()

    var showExitWarning by remember { mutableStateOf(false) }

    LaunchedEffect(returnedPhotoUri) {
        if (returnedPhotoUri != null) {
            viewModel.updateClosingAfterPhotoUri(returnedPhotoUri)
            onPhotoSaved()
        }
    }

    // İşlem durumu takibi
    LaunchedEffect(closingState) {
        when (closingState) {
            is ServiceViewModel.ClosingState.Success -> {
                Toast.makeText(context, "İş Emri Başarıyla Kapatıldı!", Toast.LENGTH_SHORT).show()
                viewModel.resetClosingState()
                onSuccess()
            }
            is ServiceViewModel.ClosingState.Error -> {
                val errorMsg = (closingState as ServiceViewModel.ClosingState.Error).message
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                viewModel.resetClosingState()
            }
            else -> {}
        }
    }

    // Veri kaybını önlemek için Geri Tuşu yakalama
    BackHandler {
        if (closingNote.isNotBlank() || !signatureController.isEmpty || closingAfterPhotoUri != null) {
            showExitWarning = true
        } else {
            onNavigateBack()
        }
    }

    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },
            title = { Text("Çıkmak İstediğinize Emin Misiniz?") },
            text = { Text("Kapanış formuna girdiğiniz veriler (imza, fotoğraf ve not) silinecek.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitWarning = false
                        viewModel.resetClosingState()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Evet, Çık") }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarning = false }) { Text("İptal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İş Kapanış Formu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (closingNote.isNotBlank() || !signatureController.isEmpty || closingAfterPhotoUri != null) {
                            showExitWarning = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "İş No: #$serviceId",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // KAPANIŞ NOTU ALANI
            OutlinedTextField(
                value = closingNote,
                onValueChange = { viewModel.updateClosingNote(it) },
                label = { Text("Kapanış Notu (Zorunlu)") },
                placeholder = { Text("Yapılan işlemleri detaylıca yazınız...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )

            // DİJİTAL İMZA ALANI
            Text("Dijital İmza (Zorunlu)", fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                SignaturePad(
                    controller = signatureController,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { signatureController.clear() }) {
                    Text("İmzayı Temizle")
                }
            }

            // --- SONRASI FOTOĞRAFI ALANI ---
            Text("Sonrası Fotoğrafı (Zorunlu)", fontWeight = FontWeight.Bold)
            if (closingAfterPhotoUri != null) {
                // Fotoğraf Önizlemesi
                AsyncImage(
                    model = closingAfterPhotoUri,
                    contentDescription = "Sonrası Fotoğrafı",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.updateClosingAfterPhotoUri(null) }) {
                        Text("Fotoğrafı Kaldır / Değiştir")
                    }
                }
            } else {
                Button(
                    onClick = onNavigateToCamera,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Kamerayı Aç ve Fotoğraf Ekle")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // İŞİ KAPAT BUTONU VE DOĞRU VALIDASYON
            val isFormValid = closingNote.isNotBlank() && !signatureController.isEmpty && closingAfterPhotoUri != null
            val isLoading = closingState is ServiceViewModel.ClosingState.Loading

            Button(
                onClick = {
                    val bitmap = signatureController.getSignatureBitmap()
                    if (bitmap != null) {
                        val uri = saveSignatureToInternalStorage(context, bitmap)
                        if (uri != null) {
                            viewModel.updateClosingSignatureUri(uri)
                            viewModel.submitClosingForm(serviceId, personnelId)
                        } else {
                            Toast.makeText(context, "İmza dosyası oluşturulamadı!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("İşi Kapat (Tamamlandı)")
                }
            }
        }
    }
}