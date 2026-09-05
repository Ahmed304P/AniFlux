import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_sig = """fun ProfileScreen(
    userName: String,
    userHandle: String,
    userAvatar: String,
    userBanner: String,
    onLogoutClick: () -> Unit,"""

new_sig = """fun ProfileScreen(
    userName: String,
    userHandle: String,
    userAvatar: String,
    userBanner: String,
    userLocation: String,
    userBirthDate: String,
    userJoinDate: String,
    onLogoutClick: () -> Unit,"""
content = content.replace(old_sig, new_sig)

old_details = """            // Details Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("الموقع", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تاريخ الميلاد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تاريخ الانضمام", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }"""

new_details = """            // Details Row
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
            }"""
content = content.replace(old_details, new_details)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("ProfileScreen updated")
