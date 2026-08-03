package com.example.donanim_operasyon_ve_servis_staj_projesi.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Servis taleplerini tutacak basit bir veri yapısı (Model)
data class ServiceTask(
    val id: Int,
    val title: String,
    val description: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Ekranda göstereceğimiz sahte (dummy) veri listesi
    val taskList = listOf(
        ServiceTask(1, "Yazıcı Arızası", "Muhasebe departmanı 3. kat yazıcı kağıt sıkıştırıyor.", "Beklemede"),
        ServiceTask(2, "Ağ Bağlantı Sorunu", "Toplantı odasında Wi-Fi sinyali çok zayıf.", "İşlemde"),
        ServiceTask(3, "Yeni Monitör Kurulumu", "İK departmanına 2 adet yeni monitör kurulacak.", "Beklemede"),
        ServiceTask(4, "Sunucu Bakımı", "Ana sunucu odasında rutin soğutma sistemi kontrolü.", "Tamamlandı")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aktif Operasyonlar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        // LazyColumn, ekrandaki listeleri kaydırılabilir (scrollable) şekilde göstermemizi sağlar
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(taskList) { task ->
                TaskCard(task = task)
            }
        }
    }
}

// Listedeki her bir elemanın (kartın) nasıl görüneceğini belirleyen fonksiyon
@Composable
fun TaskCard(task: ServiceTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Duruma göre renk değiştirecek ufak bir etiket mantığı da ekleyelim
            val statusColor = when(task.status) {
                "Beklemede" -> MaterialTheme.colorScheme.error
                "İşlemde" -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }

            Text(
                text = "Durum: ${task.status}",
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}