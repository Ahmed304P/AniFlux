package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentName: String,
    currentHandle: String,
    currentAvatar: String,
    currentBanner: String,
    currentLocation: String,
    currentBirthDate: String,
    lastHandleChangeDate: Long,
    onSaveProfile: (String, String, String, String, String) -> Unit,
    onSaveHandle: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    var fullName by remember { mutableStateOf(currentName) }
    var handle by remember { mutableStateOf(currentHandle.removePrefix("@")) }
    var location by remember { mutableStateOf(currentLocation) }
    var birthDate by remember { mutableStateOf(currentBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) bannerUri = uri }
    )

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) avatarUri = uri }
    )

    val takenUsernames = listOf("admin", "test", "mohammed", "ahmed", "ali", "user")
    
    var handleError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    val currentTime = System.currentTimeMillis()
    val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
    val canChangeHandle = (currentTime - lastHandleChangeDate) >= thirtyDaysMillis
    
    val remainingDays = if (!canChangeHandle) {
        val diff = thirtyDaysMillis - (currentTime - lastHandleChangeDate)
        (diff / (1000 * 60 * 60 * 24)).toInt() + 1
    } else 0

    fun handleSave() {
        if (fullName.isBlank()) {
            return
        }
        
        coroutineScope.launch {
            isSaving = true
            handleError = null
            
            // Check handle change
            val newHandle = handle.lowercase().trim()
            val currentHandleNoAt = currentHandle.removePrefix("@").lowercase().trim()
            
            if (newHandle != currentHandleNoAt && canChangeHandle) {
                if (newHandle.isBlank()) {
                    handleError = "يرجى إدخال اسم المستخدم"
                    isSaving = false
                    return@launch
                }
                val handleRegex = "^[a-zA-Z0-9_]+$".toRegex()
                if (!handleRegex.matches(newHandle)) {
                    handleError = "يجب أن يحتوي على أحرف إنجليزية وأرقام فقط"
                    isSaving = false
                    return@launch
                }
                
                delay(800) // Simulate network
                if (takenUsernames.contains(newHandle)) {
                    handleError = "اسم المستخدم هذا موجود بالفعل"
                    isSaving = false
                    return@launch
                }
                
                onSaveHandle("@$newHandle")
            }
            
            val finalBanner = bannerUri?.toString() ?: currentBanner
            val finalAvatar = avatarUri?.toString() ?: currentAvatar
            
            onSaveProfile(fullName.trim(), finalAvatar, finalBanner, location.trim(), birthDate)
            isSaving = false
            onBackClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "تعديل الملف الشخصي",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { handleSave() }) {
                        Text("حفظ", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = Color.White
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Banner & Avatar Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Banner Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color.DarkGray)
                        .bounceClick { bannerPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    if (bannerUri != null || currentBanner.isNotBlank()) {
                        AsyncImage(
                            model = bannerUri ?: currentBanner,
                            contentDescription = "Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Banner",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(40.dp)
                        )
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
                        .bounceClick { avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    if (avatarUri != null || currentAvatar.isNotBlank()) {
                        AsyncImage(
                            model = avatarUri ?: currentAvatar,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Avatar",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Full Name
                ProfileTextField(
                    label = "الاسم الكامل",
                    value = fullName,
                    onValueChange = { if (it.length <= 25) fullName = it },
                    placeholder = "",
                    charCount = "${fullName.length}/25"
                )

                // Username
                ProfileTextField(
                    label = "اسم المستخدم",
                    value = handle,
                    onValueChange = { 
                        if (canChangeHandle) {
                            handle = it
                            if (handleError != null) handleError = null
                        }
                    },
                    placeholder = "",
                    helperText = if (!canChangeHandle) "يمكنك تغيير اسم المستخدم بعد $remainingDays يوماً" else "يمكن تغييره مرة كل 30 يوم",
                    errorText = handleError,
                    enabled = canChangeHandle,
                    prefix = "@"
                )
                
                // Location
                ProfileTextField(
                    label = "الموقع الجغرافي",
                    value = location,
                    onValueChange = { if (it.length <= 30) location = it },
                    placeholder = "",
                    charCount = "${location.length}/30"
                )
                
                // Birth Date
                Box(modifier = Modifier.fillMaxWidth()) {
                    ProfileTextField(
                        label = "تاريخ الميلاد",
                        value = birthDate.ifEmpty { "غير محدد" },
                        onValueChange = {},
                        placeholder = "YYYY-MM-DD",
                        enabled = true
                    )
                    // Invisible overlay to intercept clicks
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { showDatePicker = true }
                            .background(Color.Transparent)
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
                            birthDate = format.format(java.util.Date(millis))
                        }
                        showDatePicker = false
                    }) {
                        Text("موافق", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                },
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = Color.White,
                        headlineContentColor = Color.White,
                        weekdayContentColor = Color.LightGray,
                        subheadContentColor = Color.LightGray,
                        yearContentColor = Color.White,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                        dayContentColor = Color.White,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    helperText: String? = null,
    errorText: String? = null,
    charCount: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    prefix: String? = null
) {
    Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            prefix = { if (prefix != null) Text(prefix, color = if(enabled) MaterialTheme.colorScheme.onSurface else Color.Gray, fontWeight = FontWeight.Bold) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = if (errorText != null) MaterialTheme.colorScheme.error else Color.White,
                unfocusedIndicatorColor = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                disabledIndicatorColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.Gray,
                cursorColor = MaterialTheme.colorScheme.primary,
                errorIndicatorColor = MaterialTheme.colorScheme.error
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = errorText != null
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (charCount != null) {
                Text(
                    text = charCount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp
                )
            } else if (helperText != null) {
                Text(
                    text = helperText,
                    color = if (!enabled && prefix == "@") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
