package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPersonnel: () -> Unit,
    onLogOut: () -> Unit
) {
    val showAddDialog = remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aktif Operasyonlar (Admin)", fontWeight = FontWeight.Bold) },
                actions = {
                    // Bu ikon sağ üstteki 3 noktayı temsil ediyor
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }

                    // Bu da 3 noktaya basılınca açılan menü
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Personel Yönetimi") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Personel Yönetimi") },
                            onClick = {
                                expandedMenu = false
                                onNavigateToPersonnel()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Çıkış Yap") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap") },
                            onClick = {
                                expandedMenu = false
                                onLogOut()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "İş Emri Ekle")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Mevcut Arama, Filtreleme ve LazyColumn liste kodların aynen burada kalacak
        }
    }
}