package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.components.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


@Composable
fun ProfileScreen(
    userName: String,
    userHandle: String,
    userAvatar: String,
    userBanner: String,
    userLocation: String,
    userBirthDate: String,
    userJoinDate: String,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Banner & Avatar Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.DarkGray)
            ) {
                if (userBanner.isNotBlank()) {
                    AsyncImage(
                        model = userBanner,
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add Banner",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }
            }

            // Top Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Back", tint = Color.White)
                }

                Row {
                    IconButton(
                        onClick = { /* Share */ },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = "Share", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            var showLogoutDialog by remember { mutableStateOf(false) }
                            
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("تسجيل الخروج", color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showLogoutDialog = true
                                }
                            )
                            
                            if (showLogoutDialog) {
                                AlertDialog(
                                    onDismissRequest = { showLogoutDialog = false },
                                    title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) },
                                    text = { Text("هل أنت متأكد أنك تريد تسجيل الخروج من حسابك؟") },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            showLogoutDialog = false
                                            onLogoutClick()
                                        }) {
                                            Text("نعم، تسجيل الخروج", color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showLogoutDialog = false }) {
                                            Text("إلغاء", color = Color.White)
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = Color.White,
                                    textContentColor = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                if (userAvatar.isNotBlank()) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Add Avatar",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }
            }
        }

        // Profile Info Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Edit Profile Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("تعديل الملف الشخصي", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Name & Handle
            val displayUserName = if (userName.isNotBlank()) userName else "مستخدم جديد"
            val displayHandle = if (userHandle.isNotBlank()) userHandle else "@new_user"
            Text(
                text = displayUserName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = displayHandle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bio
            Text(
                text = "أضف نبذة عنك هنا...",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Details Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (userLocation.isNotBlank()) userLocation else "الموقع غير محدد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (userBirthDate.isNotBlank()) userBirthDate else "تاريخ الميلاد غير محدد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (userJoinDate.isNotBlank()) "تاريخ الانضمام: $userJoinDate" else "تاريخ الانضمام غير محدد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.DarkGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Stats Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Empty Donut Chart
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        drawCircle(
                            color = Color.DarkGray,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 40f)
                        )
                    }
                }

                // Legend
                Column {
                    LegendItem(color = Color(0xFF2D9CDB), label = "أنمياتي المفضلة : 0")
                    LegendItem(color = Color(0xFF27AE60), label = "اشاهدها حاليا : 0")
                    LegendItem(color = Color(0xFFEB5757), label = "ارغب بمشاهدتها : 0")
                    LegendItem(color = Color(0xFF9B51E0), label = "تم مشاهدتها : 0")
                    LegendItem(color = Color(0xFFBB6BD9), label = "اكملها لاحقا : 0")
                    LegendItem(color = Color(0xFFF2994A), label = "لا ارغب بمشاهدتها : 0")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "عدد الحلقات التي تم مشاهدتها : 0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
