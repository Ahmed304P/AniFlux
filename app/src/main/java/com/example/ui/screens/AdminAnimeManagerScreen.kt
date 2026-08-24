package com.example.ui.screens
import androidx.compose.material.icons.filled.Search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Anime
import com.example.ui.viewmodel.AnimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnimeManagerScreen(
    onBack: () -> Unit,
    onEditAnime: (String?) -> Unit, // null means Add New
    viewModel: AnimeViewModel,
    modifier: Modifier = Modifier
) {
    val animeList by viewModel.allAnime.collectAsState() // Using recommended list as a mock for all anime
    

    var searchQuery by remember { mutableStateOf("") }
    val filteredList = animeList.filter { 
        it.titleAr.contains(searchQuery, true) || it.titleEn.contains(searchQuery, true) 
    }

    Scaffold(

        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("إدارة الأنمي", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ابحث عن أنمي...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color(0xFFE91E63),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditAnime(null) },
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة أنمي جديد")
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredList, key = { it.id }) { anime ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clickable { onEditAnime(anime.id) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(anime.titleAr, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${anime.currentEpisodes} حلقة - ${anime.status}", color = Color.Gray, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFFFFD700))
                    }
                }
            }
        }
    }
}
