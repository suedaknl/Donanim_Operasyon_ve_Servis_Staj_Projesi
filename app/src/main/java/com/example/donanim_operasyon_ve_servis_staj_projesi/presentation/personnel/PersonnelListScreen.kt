package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.ServiceViewModel // YENİ EKLENEN IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelListScreen(
    viewModel: PersonnelViewModel,
    serviceViewModel: ServiceViewModel, // YENİ EKLENEN PARAMETRE
    onNavigateToAddPersonnel: () -> Unit,
    onNavigateToEditPersonnel: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val personnelList by viewModel.personnelList.collectAsState()
    var personnelToDelete by remember { mutableStateOf<Personnel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personel Yönetimi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddPersonnel) {
                Icon(Icons.Default.Add, contentDescription = "Personel Ekle")
            }
        }
    ) { paddingValues ->
        if (personnelList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz kayıtlı personel bulunmuyor.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(personnelList) { personnel ->
                    PersonnelCard(
                        personnel = personnel,
                        onDeleteClick = { personnelToDelete = personnel },
                        onEditClick = { onNavigateToEditPersonnel(personnel.id) }
                    )
                }
            }
        }
    }

    personnelToDelete?.let { personnel ->
        AlertDialog(
            onDismissRequest = { personnelToDelete = null },
            title = { Text("Personeli Sil", fontWeight = FontWeight.Bold) },
            text = { Text("${personnel.fullName} adlı personeli silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        // 1. ÖNCE: Bu personele ait iş emirlerindeki atamayı (assignedPersonnelId) temizle
                        serviceViewModel.clearAssignedPersonnel(personnel.id)

                        // 2. SONRA: Personeli veritabanından tamamen sil
                        viewModel.deletePersonnel(personnel)

                        personnelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { personnelToDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun PersonnelCard(
    personnel: Personnel,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = personnel.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Kullanıcı Adı: ${personnel.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))

                Text(text = "Telefon: ${personnel.phoneNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))

                Text(text = "Görev: ${personnel.role}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = if (personnel.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (personnel.isActive) "Aktif" else "Pasif",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (personnel.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Düzenle", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}