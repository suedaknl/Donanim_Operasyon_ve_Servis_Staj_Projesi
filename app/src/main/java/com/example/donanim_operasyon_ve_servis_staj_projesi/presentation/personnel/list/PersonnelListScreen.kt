package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.list

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.donanim_operasyon_ve_servis_staj_projesi.data.local.Personnel
import com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.personnel.components.PersonnelCard
import com.example.donanim_operasyon_ve_servis_staj_projesi.viewmodel.PersonnelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonnelListScreen(
    viewModel: PersonnelViewModel,
    onNavigateToAddPersonnel: () -> Unit,
    onNavigateToEditPersonnel: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onPersonnelClick: (Int) -> Unit
) {
    val pagedPersonnelList by viewModel.pagedPersonnelList.collectAsState()
    val totalFilteredList by viewModel.filteredPersonnelList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val availableRoles by viewModel.availableRoles.collectAsState()
    val activeFilterCount by viewModel.activeFilterCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempRole by remember { mutableStateOf(selectedRole) }
    var tempStatus by remember { mutableStateOf(selectedStatus) }

    val context = LocalContext.current
    val sortOptions = listOf("İsim (A-Z)", "İsim (Z-A)", "Aktif Önce", "Pasif Önce")
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personel Yönetimi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddPersonnel) {
                        Icon(Icons.Default.Add, contentDescription = "Personel Ekle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Arama ve Filtre İkonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    placeholder = { Text("Ara: Ad, görev, telefon...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Box {
                    FilledIconButton(
                        onClick = {
                            tempRole = selectedRole
                            tempStatus = selectedStatus
                            showFilterSheet = true
                        },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge { Text(activeFilterCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrele", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Yatay Rol Filtre Chip'leri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableRoles.forEach { role ->
                    val isSelected = selectedRole == role
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateSelectedRole(role) },
                        label = { Text(role, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Toplam Personel Sayısı ve Sıralama
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toplam ${totalFilteredList.size} personel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Sırala: $sortOption", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.updateSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (pagedPersonnelList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Arama veya filtreleme kriterlerine uygun personel bulunamadı.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(pagedPersonnelList) { personnel ->
                        PersonnelCard(
                            personnel = personnel,
                            onCardClick = { onPersonnelClick(personnel.id) },
                            onPhoneClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${personnel.phoneNumber}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Pagination Kontrolleri (Tam Ortalı)
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setPage(currentPage - 1) },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki Sayfa")
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        for (i in 1..totalPages) {
                            val isCurrent = i == currentPage
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(30.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { viewModel.setPage(i) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = i.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { viewModel.setPage(currentPage + 1) },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki Sayfa")
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtrele", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        viewModel.clearAllFilters()
                        showFilterSheet = false
                    }) {
                        Text("Temizle", color = MaterialTheme.colorScheme.error)
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text("Görev / Rol", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableRoles.forEach { role ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tempRole = role },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(role, style = MaterialTheme.typography.bodyMedium)
                                    Checkbox(
                                        checked = tempRole == role,
                                        onCheckedChange = { tempRole = role }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Durum", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Tümü", "Aktif", "Pasif").forEach { status ->
                                val isSelected = tempStatus == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { tempStatus = status },
                                    label = { Text(status) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFilterSheet = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("İptal")
                        }
                        Button(
                            onClick = {
                                viewModel.updateSelectedRole(tempRole)
                                viewModel.updateSelectedStatus(tempStatus)
                                showFilterSheet = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Uygula", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}