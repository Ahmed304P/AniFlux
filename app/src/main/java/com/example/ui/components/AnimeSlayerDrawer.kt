package com.example.ui.components
import androidx.compose.ui.graphics.Brush

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.SocialViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue



@Composable
fun AnimeSlayerDrawerContent(
    selectedTab: Int,
    isLoggedIn: Boolean,
    userName: String,
    userAvatar: String,
    userBanner: String,
    onSelectTab: (Int) -> Unit,
    onCloseDrawer: () -> Unit,
    socialViewModel: SocialViewModel = viewModel(),
    onNotificationClicked: () -> Unit = {}
) {
    val mContext = LocalContext.current


    Surface(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header Profile & Notification Row
            if (isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userBanner)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                                    startY = 100f
                                )
                            )
                    )
                    
                    NotificationBell(
                        viewModel = socialViewModel,
                        onOpenNotifications = {
                        },
                        onCloseDrawer = onCloseDrawer,
                        onNotificationClicked = onNotificationClicked,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(end = 12.dp, top = 12.dp),
                        tint = Color.White
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.bounceClick {
                                onSelectTab(15) // Profile tab
                                onCloseDrawer()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(userAvatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = userName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp)
                ) {
                    NotificationBell(
                        viewModel = socialViewModel,
                        onOpenNotifications = {
                        },
                        onCloseDrawer = onCloseDrawer,
                        onNotificationClicked = onNotificationClicked,
                        modifier = Modifier.align(Alignment.TopEnd),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(
                        modifier = Modifier
                            .padding(top = 36.dp)
                            .align(Alignment.Center)
                            .bounceClick(onClick = {
                                onSelectTab(17) // LoginScreen
                                onCloseDrawer()
                            }),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "انقر هنا لتسجيل الدخول",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1
            DrawerMenuItem(
                icon = Icons.Default.LocalFireDepartment,
                title = "اخر التحديثات",
                isSelected = selectedTab == 0,
                onClick = {
                    onSelectTab(0)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.FormatListBulleted,
                title = "لائحة الانمي",
                isSelected = selectedTab == 1,
                onClick = {
                    onSelectTab(1)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.CalendarToday,
                title = "المواسم",
                isSelected = selectedTab == 5,
                onClick = {
                    onSelectTab(5)
                    onCloseDrawer()
                }
            )

            DrawerSectionDivider()

            // SECTION 3
            DrawerMenuItem(
                icon = Icons.Default.BarChart,
                title = "قائمتي",
                isSelected = selectedTab == 3,
                onClick = {
                    onSelectTab(3)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.BarChart,
                title = "القائمة المخصصة",
                isSelected = selectedTab == 8,
                onClick = {
                    onSelectTab(8)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Favorite,
                title = "أنمياتي المفضلة",
                isSelected = selectedTab == 9,
                onClick = {
                    onSelectTab(9)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Schedule,
                title = "اخر المشاهدات",
                isSelected = selectedTab == 11,
                onClick = {
                    onSelectTab(11)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.FileDownload,
                title = "تحميلاتي",
                isSelected = selectedTab == 12,
                onClick = {
                    onSelectTab(12)
                    onCloseDrawer()
                }
            )

            DrawerSectionDivider()

            // SECTION 4
            DrawerMenuItem(
                icon = Icons.Default.CalendarToday,
                title = "مواعيد نزول الحلقات",
                isSelected = selectedTab == 2,
                onClick = {
                    onSelectTab(2)
                    onCloseDrawer()
                }
            )

            DrawerSectionDivider()

            // SECTION 5
            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "الاعدادات",
                isSelected = selectedTab == 14,
                onClick = {
                    onSelectTab(14)
                    onCloseDrawer()
                }
            )
        }
    }
}

@Composable
private fun DrawerSectionDivider() {
    Divider(
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            // Right Side: Arabic Text Title
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBell(
    viewModel: SocialViewModel,
    onOpenNotifications: () -> Unit,
    onCloseDrawer: () -> Unit,
    onNotificationClicked: () -> Unit = {},
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    Box(modifier = modifier) {
        IconButton(
            onClick = { 
                viewModel.setShouldOpenNotifications(true)
                onCloseDrawer()
            },
            modifier = Modifier.size(40.dp)
        ) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(unreadCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
