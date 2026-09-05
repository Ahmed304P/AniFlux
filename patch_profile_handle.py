import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_sig = """fun ProfileScreen(
    userName: String,
    userAvatar: String,
    userBanner: String,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,"""

new_sig = """fun ProfileScreen(
    userName: String,
    userHandle: String,
    userAvatar: String,
    userBanner: String,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,"""
content = content.replace(old_sig, new_sig)


old_dropdown = """                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("تسجيل الخروج", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onLogoutClick()
                                }
                            )
                        }"""

new_dropdown = """                        DropdownMenu(
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
                        }"""
content = content.replace(old_dropdown, new_dropdown)


old_name = """            // User Name & Handle
            val displayUserName = if (userName.isNotBlank()) userName else "مستخدم جديد"
            Text(
                text = displayUserName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${displayUserName.replace(" ", "_").lowercase()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )"""

new_name = """            // User Name & Handle
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
            )"""
content = content.replace(old_name, new_name)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("ProfileScreen.kt updated!")
