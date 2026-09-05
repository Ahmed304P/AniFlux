import re

with open('app/src/main/java/com/example/ui/screens/CompleteProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_sig = """fun CompleteProfileScreen(
    onComplete: (String, String, String) -> Unit
) {"""

new_sig = """fun CompleteProfileScreen(
    onComplete: (String, String, String, String) -> Unit
) {"""
content = content.replace(old_sig, new_sig)

old_vars = """    var userName by remember { mutableStateOf("") }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }"""

new_vars = """    var userName by remember { mutableStateOf("") }
    var userHandle by remember { mutableStateOf("") }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var handleError by remember { mutableStateOf<String?>(null) }"""
content = content.replace(old_vars, new_vars)

old_handle_complete = """    fun handleComplete() {
        if (userName.isBlank()) {
            usernameError = "يرجى إدخال اسم المستخدم"
            return
        }
        
        coroutineScope.launch {
            isCheckingUsername = true
            usernameError = null
            
            // Simulate network delay
            delay(800)
            
            if (takenUsernames.contains(userName.lowercase().trim())) {
                usernameError = "هذا الاسم موجود بالفعل، يرجى اختيار اسم آخر"
                isCheckingUsername = false
            } else {
                isCheckingUsername = false
                val finalBanner = bannerUri?.toString() ?: defaultBanner
                val finalAvatar = avatarUri?.toString() ?: defaultAvatar
                onComplete(userName.trim(), finalAvatar, finalBanner)
            }
        }
    }"""

new_handle_complete = """    fun handleComplete() {
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
    }"""
content = content.replace(old_handle_complete, new_handle_complete)


old_textfield = """        OutlinedTextField(
            value = userName,
            onValueChange = { 
                userName = it
                if (usernameError != null) usernameError = null
            },
            label = { Text("اسم المستخدم") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
            isError = usernameError != null,
            supportingText = {
                if (usernameError != null) {
                    Text(text = usernameError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))"""

new_textfield = """        OutlinedTextField(
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
        
        Spacer(modifier = Modifier.height(16.dp))"""
content = content.replace(old_textfield, new_textfield)


with open('app/src/main/java/com/example/ui/screens/CompleteProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("CompleteProfileScreen.kt updated!")
