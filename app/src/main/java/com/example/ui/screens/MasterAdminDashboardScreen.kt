package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterAdminDashboardScreen(
    onBack: () -> Unit,
    onNavigateToAnimeManager: () -> Unit,
    onNavigateToExtensionRepos: () -> Unit = {},
    totalAnimeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TopAppBar(
            title = { Text("لوحة تحكم الإدارة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A1A)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Quick Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = "إجمالي المستخدمين", value = "1,245", icon = Icons.Default.People, modifier = Modifier.weight(1f))
                StatCard(title = "إجمالي الأنمي", value = if (totalAnimeCount > 0) totalAnimeCount.toString() else "8", icon = Icons.Default.Movie, modifier = Modifier.weight(1f))
                StatCard(title = "التعليقات", value = "8,920", icon = Icons.Default.Comment, modifier = Modifier.weight(1f))
            }

            Text(
                text = "أدوات الإدارة والتحكم",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AdminFeatureCard(
                        title = "إدارة الأنمي",
                        description = "إضافة، تعديل، حذف وحلقات",
                        icon = Icons.Default.Edit,
                        color = Color(0xFFE91E63),
                        onClick = onNavigateToAnimeManager
                    )
                }
                item {
                    AdminFeatureCard(
                        title = "إدارة المستخدمين",
                        description = "حظر، فك حظر، ونشاطات",
                        icon = Icons.Default.ManageAccounts,
                        color = Color(0xFF2196F3),
                        onClick = {}
                    )
                }
                item {
                    AdminFeatureCard(
                        title = "مراقبة التعليقات",
                        description = "مراجعة شاملة وإخفاء حرق",
                        icon = Icons.Default.AdminPanelSettings,
                        color = Color(0xFFFF9800),
                        onClick = {}
                    )
                }
                item {
                    AdminFeatureCard(
                        title = "إعدادات النظام",
                        description = "إشعارات وصيانة التطبيق",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF4CAF50),
                        onClick = {}
                    )
                }
                item {
                    AdminFeatureCard(
                        title = "مستودعات الإضافات",
                        description = "استيراد وفحص وتفعيل الإضافات",
                        icon = Icons.Default.Extension,
                        color = Color(0xFF9C27B0),
                        onClick = onNavigateToExtensionRepos
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AdminFeatureCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
