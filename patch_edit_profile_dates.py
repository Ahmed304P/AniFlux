import re

with open('app/src/main/java/com/example/ui/screens/EditProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_sig = """fun EditProfileScreen(
    currentName: String,
    currentHandle: String,
    currentAvatar: String,
    currentBanner: String,
    lastHandleChangeDate: Long,
    onSaveProfile: (String, String, String) -> Unit,
    onSaveHandle: (String) -> Unit,"""

new_sig = """fun EditProfileScreen(
    currentName: String,
    currentHandle: String,
    currentAvatar: String,
    currentBanner: String,
    currentLocation: String,
    currentBirthDate: String,
    lastHandleChangeDate: Long,
    onSaveProfile: (String, String, String, String, String) -> Unit,
    onSaveHandle: (String) -> Unit,"""
content = content.replace(old_sig, new_sig)

old_state = """    var fullName by remember { mutableStateOf(currentName) }
    var handle by remember { mutableStateOf(currentHandle.removePrefix("@")) }
    
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }"""

new_state = """    var fullName by remember { mutableStateOf(currentName) }
    var handle by remember { mutableStateOf(currentHandle.removePrefix("@")) }
    var location by remember { mutableStateOf(currentLocation) }
    var birthDate by remember { mutableStateOf(currentBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }"""
content = content.replace(old_state, new_state)

old_save = """            val finalBanner = bannerUri?.toString() ?: currentBanner
            val finalAvatar = avatarUri?.toString() ?: currentAvatar
            
            onSaveProfile(fullName.trim(), finalAvatar, finalBanner)"""

new_save = """            val finalBanner = bannerUri?.toString() ?: currentBanner
            val finalAvatar = avatarUri?.toString() ?: currentAvatar
            
            onSaveProfile(fullName.trim(), finalAvatar, finalBanner, location.trim(), birthDate)"""
content = content.replace(old_save, new_save)


# Update the form section to include location and birthdate
old_form = """                // Username
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
                                
                Spacer(modifier = Modifier.height(40.dp))"""

new_form = """                // Username
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
                    placeholder = "مثال: اليمن، صنعاء",
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
                containerColor = MaterialTheme.colorScheme.surface
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
        }"""
content = content.replace(old_form, new_form)

with open('app/src/main/java/com/example/ui/screens/EditProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("EditProfileScreen updated")
