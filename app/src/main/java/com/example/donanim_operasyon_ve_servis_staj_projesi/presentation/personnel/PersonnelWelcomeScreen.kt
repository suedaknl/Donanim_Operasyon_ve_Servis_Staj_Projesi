package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.ServiceStatus
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import java.util.Calendar

@Composable
fun PersonnelWelcomeScreen(
    personnelId: Int,
    personnelViewModel: PersonnelViewModel,
    serviceViewModel: ServiceViewModel,
    onNavigateToHome: () -> Unit
) {
    var personnel by remember { mutableStateOf<Personnel?>(null) }

    LaunchedEffect(personnelId) {
        personnelViewModel.getPersonnelById(personnelId) { loadedPersonnel ->
            personnel = loadedPersonnel
        }
    }

    val serviceRecords by serviceViewModel.serviceRecords.collectAsState()

    val activeServiceCount = remember(serviceRecords, personnelId) {
        serviceRecords.count { record ->
            record.assignedPersonnelId == personnelId &&
                    record.status != ServiceStatus.TAMAMLANDI &&
                    record.status != ServiceStatus.IPTAL
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "İyi günler"
            else -> "İyi akşamlar"
        }
    }

    // --- İSMİN SADECE İLK KISMINI ALMA ---
    val titleSuffix = if (personnel?.gender == "KADIN") "Hanım" else "Bey"
    val rawFullName = personnel?.fullName?.trim() ?: "Personel"
    val firstName = rawFullName.split(" ").firstOrNull() ?: rawFullName
    val displayName = if (firstName.isNotBlank()) "$firstName $titleSuffix" else "Değerli Personelimiz"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "$greeting $displayName 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeServiceCount > 0) {
                                Text(
                                    text = "Bugün size atanmış aktif iş emri sayısı:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$activeServiceCount",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Şu anda atanmış aktif bir iş emriniz bulunmuyor.\n\nYeni bir görev atandığında buradan takip edebilirsiniz.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onNavigateToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (activeServiceCount > 0) "İş Emirlerini Görüntüle" else "Ana Sayfaya Git",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}