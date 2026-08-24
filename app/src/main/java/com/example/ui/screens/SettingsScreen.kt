package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.bounceClick
import com.example.ui.viewmodel.AppTheme
import com.example.ui.theme.AniFluxBg
import com.example.ui.theme.AniFluxPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    currentSkipTime: Int,
    onSkipTimeSelected: (Int) -> Unit,
    onOpenMenu: () -> Unit
) {
    var showDisclaimer by remember { mutableStateOf(false) }
    var showSkipTimeDialog by remember { mutableStateOf(false) }
    val skipOptions = listOf(5, 10, 15, 20, 30, 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "الاعدادات", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // --- Section 1: Appearance ---
            item {
                SectionTitle(title = "تصميم التطبيق")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeCard(
                        title = "الافتراضي",
                        bgColor = AniFluxBg,
                        primaryColor = AniFluxPrimary,
                        isSelected = currentTheme == AppTheme.DEFAULT,
                        onClick = { onThemeSelected(AppTheme.DEFAULT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeCard(
                        title = "الأسود الغامق",
                        bgColor = Color(0xFF000000),
                        primaryColor = Color(0xFFE50914),
                        isSelected = currentTheme == AppTheme.DARK_BLACK,
                        onClick = { onThemeSelected(AppTheme.DARK_BLACK) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeCard(
                        title = "الأزرق الغامق",
                        bgColor = Color(0xFF020617),
                        primaryColor = Color(0xFF3B82F6),
                        isSelected = currentTheme == AppTheme.DARK_BLUE,
                        onClick = { onThemeSelected(AppTheme.DARK_BLUE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Section 2: Player Settings ---
            item {
                SectionTitle(title = "إعدادات المشغل")
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsItemRow(
                            icon = Icons.Default.PlayCircle,
                            title = "اختيار المشغل",
                            subtitle = "الوضع الافتراضي",
                            onClick = { /* Do nothing or open player selection */ }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        SettingsItemRow(
                            icon = Icons.Default.FastForward,
                            title = "وقت التقديم",
                            subtitle = "$currentSkipTime ثواني",
                            onClick = { showSkipTimeDialog = true }
                        )
                    }
                }
            }

            // --- Section 3: About / Disclaimer ---
            item {
                SectionTitle(title = "حول")
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        title = "إخلاء مسؤولية",
                        subtitle = "اضغط لقراءة التفاصيل",
                        onClick = { showDisclaimer = true }
                    )
                }
            }
        }
    }

    // --- Dialogs ---
    if (showDisclaimer) {
        Dialog(onDismissRequest = { showDisclaimer = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { showDisclaimer = false },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 12.dp)
                    )
                    Text(
                        text = "إخلاء مسؤولية",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "نحن لسنا مرتبطين بأي وسيلة مع محتوى الفيديو . هذا التطبيق يعمل كفهرس وقاعدة بيانات للأنمي الموجود بشكل علني على الانترنت. جميع الانميات والعلامات التجارية والشعارات والصور و الترجمة هي ملك لأصحابها الشرعيين.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }

    if (showSkipTimeDialog) {
        Dialog(onDismissRequest = { showSkipTimeDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "اختر وقت التقديم",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    skipOptions.forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSkipTimeSelected(seconds)
                                    showSkipTimeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSkipTime == seconds,
                                onClick = {
                                    onSkipTimeSelected(seconds)
                                    showSkipTimeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (seconds == 10) "$seconds ثواني (افتراضي)" else "$seconds ثواني",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = if (currentSkipTime == seconds) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
fun SettingsItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ThemeCard(
    title: String,
    bgColor: Color,
    primaryColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val scale = if (isSelected) 1.02f else 1f

    Column(
        modifier = modifier.bounceClick { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) borderColor else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // UI Representation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(primaryColor))
                    Box(modifier = Modifier.height(6.dp).width(24.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha=0.2f)))
                }
                
                // Content mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
                
                // Primary Accent Mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(primaryColor)
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(bgColor, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
