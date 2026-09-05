package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListScreen(
    onBack: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    BackHandler { onBack() }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("انشاء قائمة جديدة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "اسم القائمة",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("قائمة جديدة", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            Text(
                text = "${name.length}/50",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "وصف القائمة\n(اختياري)",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 50) description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("(اختياري)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            Text(
                text = "${description.length}/50",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = name.isNotBlank()
            ) {
                Text("انشاء قائمة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
