import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_details = """                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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

new_details = """                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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

if old_details in content:
    content = content.replace(old_details, new_details)
else:
    # try regex because of whitespace
    content = re.sub(r'Icon\(Icons.Default.LocationOn.*?Text\("تاريخ الانضمام".*?12\.sp\)\n\s*\}', new_details, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("ProfileScreen patched again with regex.")
