package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AnimeExtensionEntity
import com.example.data.local.ExtensionRepoEntity
import com.example.data.local.ExtensionSyncStateEntity
import com.example.ui.theme.AniFluxCyan
import com.example.ui.viewmodel.AnimeViewModel
import com.example.util.ExtensionEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExtensionReposScreen(
    onBack: () -> Unit,
    viewModel: AnimeViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val repos by viewModel.extensionRepos.collectAsStateWithLifecycle()
    val allExtensions by viewModel.allExtensions.collectAsStateWithLifecycle()
    val enabledExtensions by viewModel.enabledExtensions.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImportingRepo.collectAsStateWithLifecycle()
    val verifyingExtensionId by viewModel.verifyingExtensionId.collectAsStateWithLifecycle()
    val syncingExtensionId by viewModel.syncingExtensionId.collectAsStateWithLifecycle()
    val extensionSyncStates by viewModel.extensionSyncStates.collectAsStateWithLifecycle()

    var repoUrlInput by remember { mutableStateOf("") }
    var selectedRepoFilterId by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, ENABLED, DISABLED, COMPATIBLE, INCOMPATIBLE
    var searchQuery by remember { mutableStateOf("") }
    var repoPendingDelete by remember { mutableStateOf<ExtensionRepoEntity?>(null) }

    val filteredExtensions = remember(allExtensions, selectedRepoFilterId, statusFilter, searchQuery) {
        allExtensions.filter { ext ->
            val matchesRepo = selectedRepoFilterId == null || ext.repoId == selectedRepoFilterId
            val matchesStatus = when (statusFilter) {
                "ENABLED" -> ext.isEnabled
                "DISABLED" -> !ext.isEnabled
                "COMPATIBLE" -> ext.isCompatible && ext.securityVerified
                "INCOMPATIBLE" -> !ext.isCompatible || !ext.securityVerified
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                ext.name.contains(q, ignoreCase = true) ||
                ext.description.contains(q, ignoreCase = true) ||
                ext.sourceUrl.contains(q, ignoreCase = true) ||
                ext.repoName.contains(q, ignoreCase = true) ||
                ext.version.contains(q, ignoreCase = true)

            matchesRepo && matchesStatus && matchesSearch
        }
    }

    val compatibleCount = remember(allExtensions) {
        allExtensions.count { it.isCompatible && it.securityVerified }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة مستودعات الإضافات",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "محرك AniFlux Universal Engine v3.5 الشامل • دعم جميع صيغ الإضافات (v1–v3)",
                            color = Color(0xFFAB47BC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    if (repos.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.refreshAllRepositories { _, msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            enabled = !isImporting,
                            modifier = Modifier.testTag("refresh_all_repos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "تحديث جميع المستودعات",
                                tint = AniFluxCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Summary Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminExtStatMiniCard(
                        title = "المستودعات",
                        value = "${repos.size}",
                        color = Color(0xFFAB47BC),
                        modifier = Modifier.weight(1f)
                    )
                    AdminExtStatMiniCard(
                        title = "إجمالي الإضافات",
                        value = "${allExtensions.size}",
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    AdminExtStatMiniCard(
                        title = "متوافقة وآمنة",
                        value = "$compatibleCount",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    AdminExtStatMiniCard(
                        title = "مفعّلة للجمهور",
                        value = "${enabledExtensions.size}",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Repository URL Input & Import Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFAB47BC).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color(0xFFCE93D8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "إضافة واستيراد مستودع إضافات خارجي",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تُستورد جميع الإضافات بحالة (معطلة افتراضياً) وتخضع لفحص التوافق والأمان قبل التفعيل",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = repoUrlInput,
                            onValueChange = { repoUrlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extension_repo_url_input"),
                            label = { Text("رابط مستودع الإضافات الخارجي (HTTPS JSON)", fontSize = 12.sp) },
                            placeholder = { Text("https://.../index.min.json", color = Color.DarkGray, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFFCE93D8))
                            },
                            trailingIcon = {
                                if (repoUrlInput.isNotEmpty()) {
                                    IconButton(onClick = { repoUrlInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFAB47BC),
                                unfocusedBorderColor = Color(0xFF333842),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF141414),
                                unfocusedContainerColor = Color(0xFF141414),
                                cursorColor = Color(0xFFAB47BC)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Preset Repositories Row for fast one-tap testing or importing
                        Text(
                            text = "مستودعات معتمدة جاهزة للاستيراد والفحص السريع:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ExtensionEngine.presetRepositories) { preset ->
                                val alreadyAdded = repos.any { it.url.equals(preset.url, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (alreadyAdded) Color(0xFF4CAF50).copy(alpha = 0.14f) else Color(0xFF262A34),
                                    border = BorderStroke(
                                        1.dp,
                                        if (alreadyAdded) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFF3A3F4B)
                                    ),
                                    modifier = Modifier.clickable {
                                        repoUrlInput = preset.url
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (alreadyAdded) Icons.Default.CheckCircle else Icons.Default.AddLink,
                                            contentDescription = null,
                                            tint = if (alreadyAdded) Color(0xFF81C784) else Color(0xFFCE93D8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = preset.titleAr,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.importExtensionRepository(repoUrlInput) { success, message ->
                                    if (success) {
                                        repoUrlInput = ""
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            },
                            enabled = !isImporting && repoUrlInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_extension_repo_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0),
                                disabledContainerColor = Color(0xFF38283C)
                            )
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جارٍ جلب وفحص إضافات المستودع...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استيراد المستودع وفحص التوافق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 3. Multi-Repository Selector & Management Section
            if (repos.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المستودعات المضافة (${repos.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedRepoFilterId != null) {
                                TextButton(onClick = { selectedRepoFilterId = null }) {
                                    Text("عرض جميع المستودعات", color = AniFluxCyan, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                val isAllSelected = selectedRepoFilterId == null
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isAllSelected) Color(0xFF9C27B0).copy(alpha = 0.25f) else Color(0xFF1E1E1E),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAllSelected) Color(0xFFCE93D8) else Color(0xFF2C2C2C)
                                    ),
                                    modifier = Modifier
                                        .height(86.dp)
                                        .clickable { selectedRepoFilterId = null }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (isAllSelected) Color(0xFFCE93D8) else Color.Gray,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "كل المستودعات",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${allExtensions.size} إضافة",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            items(repos, key = { it.repoId }) { repo ->
                                val isSelected = selectedRepoFilterId == repo.repoId
                                val repoEnabledCount = allExtensions.count { it.repoId == repo.repoId && it.isEnabled }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(0xFF9C27B0).copy(alpha = 0.22f) else Color(0xFF1E1E1E),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFCE93D8) else Color(0xFF2C2C2C)
                                    ),
                                    modifier = Modifier
                                        .widthIn(min = 210.dp, max = 260.dp)
                                        .height(86.dp)
                                        .clickable {
                                            selectedRepoFilterId = if (isSelected) null else repo.repoId
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = repo.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = repo.url,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${repo.extensionCount} إضافات • $repoEnabledCount مفعّلة",
                                                color = Color(0xFFCE93D8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.refreshSingleRepository(repo.url) { _, msg ->
                                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "تحديث المستودع",
                                                    tint = AniFluxCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { repoPendingDelete = repo },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف المستودع",
                                                    tint = Color(0xFFE57373),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Extensions Search & Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extensions_admin_search_input"),
                        placeholder = { Text("ابحث في الإضافات بالاسم أو الوصف أو الرابط...", fontSize = 12.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AniFluxCyan,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        )
                    )

                    val filterOptions = listOf(
                        "ALL" to "الكل (${allExtensions.size})",
                        "ENABLED" to "المفعّلة (${allExtensions.count { it.isEnabled }})",
                        "DISABLED" to "المعطّلة (${allExtensions.count { !it.isEnabled }})",
                        "COMPATIBLE" to "المتوافقة ($compatibleCount)",
                        "INCOMPATIBLE" to "غير المتوافقة / المحظورة (${allExtensions.size - compatibleCount})"
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterOptions) { (key, label) ->
                            val selected = statusFilter == key
                            FilterChip(
                                selected = selected,
                                onClick = { statusFilter = key },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.28f),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E1E1E),
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = Color(0xFF333333),
                                    selectedBorderColor = Color(0xFFCE93D8)
                                )
                            )
                        }
                    }
                }
            }

            // 5. Extensions List or Empty State
            if (allExtensions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExtensionOff,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لم يتم استيراد أي مستودع إضافات بعد",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "أدخل رابط مستودع خارجي في الحقل أعلاه أو اختر أحد المستودعات المعتمدة لجلب الإضافات وفحص توافقها وإدارتها من مكان واحد.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredExtensions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد إضافات مطابقة لخيارات البحث أو التصفية الحالية",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredExtensions, key = { it.extensionId }) { ext ->
                    val syncState = extensionSyncStates.find { it.extensionId == ext.extensionId }
                    AdminExtensionItemCard(
                        extension = ext,
                        syncState = syncState,
                        isVerifying = verifyingExtensionId == ext.extensionId,
                        isSyncing = syncingExtensionId == ext.extensionId || syncState?.syncStatus == "SYNCING",
                        onToggleEnabled = { targetEnabled ->
                            viewModel.toggleExtensionEnabled(ext.extensionId, targetEnabled) { _, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onVerifyCompatibility = {
                            viewModel.verifyExtensionCompatibility(ext.extensionId) { _, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onSyncContent = {
                            viewModel.syncSingleExtensionContent(ext.extensionId) { _, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onCopyLink = { url ->
                            clipboardManager.setText(AnnotatedString(url))
                            scope.launch { snackbarHostState.showSnackbar("تم نسخ رابط الإضافة") }
                        }
                    )
                }
            }
        }
    }

    if (repoPendingDelete != null) {
        val target = repoPendingDelete!!
        AlertDialog(
            onDismissRequest = { repoPendingDelete = null },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text("حذف المستودع", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف مستودع \"${target.name}\"؟ سيتم إزالة جميع الإضافات التابعة له وإخفاء المفعّل منها فوراً من واجهة المستخدمين.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repoPendingDelete = null
                        if (selectedRepoFilterId == target.repoId) {
                            selectedRepoFilterId = null
                        }
                        viewModel.deleteExtensionRepository(target.repoId, target.name) { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    }
                ) {
                    Text("حذف نهائياً", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { repoPendingDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun AdminExtStatMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(78.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = Color.LightGray,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminExtensionItemCard(
    extension: AnimeExtensionEntity,
    syncState: ExtensionSyncStateEntity?,
    isVerifying: Boolean,
    isSyncing: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onVerifyCompatibility: () -> Unit,
    onSyncContent: () -> Unit,
    onCopyLink: (String) -> Unit
) {
    val canBeEnabled = extension.isCompatible &&
        extension.securityVerified &&
        extension.compatibilityStatus == ExtensionEngine.STATUS_COMPATIBLE

    val statusColor = when (extension.compatibilityStatus) {
        ExtensionEngine.STATUS_COMPATIBLE -> Color(0xFF4CAF50)
        ExtensionEngine.STATUS_BLOCKED_UNSAFE_CODE -> Color(0xFFE53935)
        ExtensionEngine.STATUS_INCOMPATIBLE_ENGINE -> Color(0xFFFF9800)
        else -> Color(0xFFFFB300)
    }

    val statusLabelAr = when (extension.compatibilityStatus) {
        ExtensionEngine.STATUS_COMPATIBLE -> "متوافق وقابل للتشغيل"
        ExtensionEngine.STATUS_BLOCKED_UNSAFE_CODE -> "محظور أمنياً (كود غير موثوق)"
        ExtensionEngine.STATUS_INCOMPATIBLE_ENGINE -> "غير متوافق مع محرك التطبيق"
        ExtensionEngine.STATUS_UNREACHABLE_SOURCE -> "المصدر لا يستجيب حالياً"
        else -> "غير متوافق (بيانات ناقصة)"
    }

    val statusIcon = when (extension.compatibilityStatus) {
        ExtensionEngine.STATUS_COMPATIBLE -> Icons.Default.VerifiedUser
        ExtensionEngine.STATUS_BLOCKED_UNSAFE_CODE -> Icons.Default.GppBad
        ExtensionEngine.STATUS_INCOMPATIBLE_ENGINE -> Icons.Default.WarningAmber
        else -> Icons.Default.ErrorOutline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extension_card_${extension.extensionId}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                extension.isEnabled -> Color(0xFF4CAF50).copy(alpha = 0.6f)
                !canBeEnabled -> statusColor.copy(alpha = 0.35f)
                else -> Color(0xFF2C2C2C)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Extension Title, Version, Language & Enable/Disable Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (extension.iconUrl.isNotBlank()) {
                            coil.compose.SubcomposeAsyncImage(
                                model = extension.iconUrl,
                                contentDescription = extension.name,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = extension.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المعرّف: ${extension.pkgName}",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Version Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2A2E39)
                            ) {
                                Text(
                                    text = "الإصدار v${extension.version}",
                                    color = AniFluxCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            // Language Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2A2E39)
                            ) {
                                Text(
                                    text = extension.lang.uppercase(Locale.US),
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            // Repo Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF9C27B0).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = extension.repoName,
                                    color = Color(0xFFCE93D8),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .widthIn(max = 120.dp)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Independent Enable / Disable Switch
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = extension.isEnabled,
                        onCheckedChange = { target ->
                            onToggleEnabled(target)
                        },
                        modifier = Modifier.testTag("extension_switch_${extension.extensionId}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = if (canBeEnabled) Color.LightGray else Color(0xFFE57373),
                            uncheckedTrackColor = Color(0xFF2C2C2C)
                        )
                    )
                    Text(
                        text = if (extension.isEnabled) "مفعّلة للجمهور" else "معطّلة",
                        color = if (extension.isEnabled) Color(0xFF81C784) else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = extension.description,
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Extension URL Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141414))
                    .clickable { onCopyLink(extension.sourceUrl) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = AniFluxCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رابط الإضافة: ${extension.sourceUrl}",
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "نسخ الرابط",
                    tint = Color.Gray,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compatibility & Security Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حالة التوافق: $statusLabelAr",
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "محرك v${extension.engineVersion}",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = extension.compatibilityReasonAr,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المحول (Adapter): ${extension.adapterId} • بيئة التشغيل: ${extension.runtimeEnvironment} • الترقيم: ${extension.paginationType}",
                        color = AniFluxCyan.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (extension.securityDetailsAr.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "الفحص الأمني: ${extension.securityDetailsAr}",
                            color = Color.LightGray.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 9-Stage Real Verification Pipeline Report Section
            val verificationStages = remember(extension.verificationStagesJson) {
                ExtensionEngine.parseStagesFromJson(extension.verificationStagesJson)
            }
            var showStagesExpanded by remember { mutableStateOf(false) }

            if (verificationStages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val passedStagesCount = verificationStages.count { it.passed }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131922),
                    border = BorderStroke(1.dp, Color(0xFF2E3B4E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStagesExpanded = !showStagesExpanded }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = AniFluxCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تقرير الاختبار والتحقق الحقيقي ($passedStagesCount/${verificationStages.size} مراحل ناجحة)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (showStagesExpanded) "إخفاء المراحل ▲" else "عرض الـ 9 مراحل ▼",
                                color = AniFluxCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showStagesExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            verificationStages.forEach { st ->
                                val stColor = if (st.passed) Color(0xFF66BB6A) else Color(0xFFEF5350)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (st.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = stColor,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = st.stageTitleAr,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${st.latencyMs}ms",
                                                color = Color.Gray,
                                                fontSize = 9.sp
                                            )
                                        }
                                        Text(
                                            text = st.detailsAr,
                                            color = Color.LightGray.copy(alpha = 0.85f),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (extension.isEnabled || syncState != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF141820),
                    border = BorderStroke(1.dp, AniFluxCyan.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = AniFluxCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "جلب جميع الصفحات والدمج الذكي بدون تكرار",
                                    color = AniFluxCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (syncState != null && syncState.lastSyncedAt > 0L) {
                                val formattedTime = remember(syncState.lastSyncedAt) {
                                    SimpleDateFormat("HH:mm • yyyy/MM/dd", Locale.US).format(Date(syncState.lastSyncedAt))
                                }
                                Text(
                                    text = "آخر مزامنة: $formattedTime",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (syncState != null) {
                            Text(
                                text = "الصفحات المجلوبة: ${syncState.syncedPagesCount}/${maxOf(syncState.totalPagesCount, 1)} (${syncState.paginationMode}) • الأنميات المجلوبة: ${syncState.syncedAnimeCount} • المدمجة بدون تكرار: ${syncState.mergedAnimeCount} • الحلقات: ${syncState.syncedEpisodesCount} • السيرفرات: ${syncState.syncedServersCount}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (syncState.lastErrorAr.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = syncState.lastErrorAr,
                                    color = Color(0xFFFFB74D),
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Text(
                                text = "الإضافة مفعّلة وجاهزة لجلب جميع الصفحات ودمج أعمال الأنمي والحلقات والسيرفرات في قاعدة البيانات الموحدة.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Verify Compatibility & Live Test Button + Sync Content Button + Package Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الصيغة: ${extension.extensionFormat}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (extension.isEnabled && canBeEnabled) {
                        OutlinedButton(
                            onClick = onSyncContent,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("sync_extension_content_${extension.extensionId}")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Color(0xFF81C784),
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("جارٍ الجلب...", color = Color(0xFF81C784), fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "مزامنة كل الصفحات",
                                    color = Color(0xFF81C784),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onVerifyCompatibility,
                        enabled = !isVerifying,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, AniFluxCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = AniFluxCyan,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جارٍ الفحص (9 مراحل)...", color = AniFluxCyan, fontSize = 11.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = AniFluxCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فحص شامل (9 مراحل)",
                                color = AniFluxCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
