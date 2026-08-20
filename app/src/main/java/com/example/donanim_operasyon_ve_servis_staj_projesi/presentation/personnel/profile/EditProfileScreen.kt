package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    personnelId: Int,
    viewModel: PersonnelViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val personnelList by viewModel.personnelList.collectAsState()
    val personnel = remember(personnelList, personnelId) {
        personnelList.find { it.id == personnelId }
    }

    var fullName by remember(personnel) { mutableStateOf(personnel?.fullName ?: "") }
    var email by remember(personnel) { mutableStateOf(personnel?.email ?: "") }
    var phoneNumber by remember(personnel) { mutableStateOf(personnel?.phoneNumber ?: "") }

    // Normalize initial gender value
    var selectedGender by remember(personnel) {
        val rawGender = personnel?.gender?.trim().orEmpty()
        val normalized = when {
            rawGender.equals("Erkek", ignoreCase = true) -> "Erkek"
            rawGender.equals("Kadın", ignoreCase = true) -> "Kadın"
            else -> ""
        }
        mutableStateOf(normalized)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilimi Düzenle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        if (personnel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Ad Soyad") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-posta") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Telefon") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Cinsiyet Seçim Alanı
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Cinsiyet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Erkek", "Kadın").forEach { genderOption ->
                            val isSelected = selectedGender.equals(genderOption, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedGender = genderOption },
                                label = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(genderOption, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Yönetici Bilgilendirme Kartı
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Rol, görev ve hesap durumu yalnızca yönetici tarafından değiştirilebilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (fullName.isBlank() || email.isBlank() || phoneNumber.isBlank()) {
                            Toast.makeText(context, "Lütfen zorunlu alanları doldurun.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedGender.isBlank()) {
                            Toast.makeText(context, "Lütfen cinsiyet seçiniz.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val updatedPersonnel = personnel.copy(
                            fullName = fullName.trim(),
                            email = email.trim(),
                            phoneNumber = phoneNumber.trim(),
                            gender = selectedGender.trim()
                        )

                        viewModel.updatePersonnel(updatedPersonnel) { success ->
                            if (success) {
                                Toast.makeText(context, "Profiliniz başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Güncelleme başarısız oldu.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Değişiklikleri Kaydet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}