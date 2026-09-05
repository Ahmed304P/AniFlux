package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CompleteProfileScreen(
    onComplete: (String, String, String, String) -> Unit
) {
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    
    val defaultBanner = "https://images.unsplash.com/photo-1542451313056-b7c8e626645f?q=80&w=1000&auto=format&fit=crop"
    val defaultAvatar = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&auto=format&fit=crop"

    var userName by remember { mutableStateOf("") }
    var userHandle by remember { mutableStateOf("") }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var handleError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) bannerUri = uri }
    )

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) avatarUri = uri }
    )

    // Dummy list of taken usernames to simulate a database check
    val takenUsernames = listOf("admin", "test", "mohammed", "ahmed", "ali", "user")

    fun handleComplete() {
        if (userName.isBlank()) {
            handleError = "يرجى إدخال اسمك"
            return
        }
        if (userHandle.isBlank()) {
            handleError = "يرجى إدخال اسم المستخدم"
            return
        }
        
        val handleRegex = "^[a-zA-Z0-9_]+$".toRegex()
        if (!handleRegex.matches(userHandle)) {
            handleError = "اسم المستخدم يجب أن يحتوي على أحرف إنجليزية وأرقام فقط"
            return
        }
        
        coroutineScope.launch {
            isCheckingUsername = true
            handleError = null
            
            // Simulate network delay
            delay(800)
            
            val handleToCheck = userHandle.lowercase().trim()
            if (takenUsernames.contains(handleToCheck)) {
                handleError = "اسم المستخدم هذا موجود بالفعل"
                isCheckingUsername = false
            } else {
                isCheckingUsername = false
                val finalBanner = bannerUri?.toString() ?: defaultBanner
                val finalAvatar = avatarUri?.toString() ?: defaultAvatar
                val finalHandle = "@$handleToCheck"
                onComplete(userName.trim(), finalHandle, finalAvatar, finalBanner)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Text(
            text = "إكمال الملف الشخصي",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Banner and Avatar layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .bounceClick { 
                        bannerPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            ) {
                AsyncImage(
                    model = bannerUri ?: defaultBanner,
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Banner",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
                    .clip(CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .bounceClick { 
                        avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = avatarUri ?: defaultAvatar,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("اسمك") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = userHandle,
            onValueChange = { 
                userHandle = it
                if (handleError != null) handleError = null
            },
            label = { Text("اسم المستخدم") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
            isError = handleError != null,
            supportingText = {
                if (handleError != null) {
                    Text(text = handleError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            leadingIcon = {
                Text(
                    text = "@",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                )
            },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "اختر صورة العرض الخاصة بك (الافاتار) وصورة الغلاف (البانر). سيتم حفظ هذه المعلومات في قاعدة البيانات لاحقاً.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { handleComplete() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isCheckingUsername
        ) {
            if (isCheckingUsername) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حفظ ومتابعة",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}