package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.components.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.AnimeComment
import kotlinx.coroutines.delay

@Composable
fun AnimeCommentsScreen(
    viewModel: SocialViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeThreadId by viewModel.activeThreadCommentId.collectAsState()
    
    BackHandler(enabled = true) {
        if (activeThreadId != null) {
            viewModel.setActiveThread(null)
        } else {
            onBack()
        }
    }
    
    var lastActiveThreadId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeThreadId) {
        if (activeThreadId != null) {
            lastActiveThreadId = activeThreadId
        }
    }
    val currentThreadId = activeThreadId ?: lastActiveThreadId

    AnimatedContent(
        targetState = activeThreadId != null,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        label = "SocialScreenTransition",
        modifier = modifier
    ) { isThread ->
        if (isThread && currentThreadId != null) {
            AnimeThreadScreen(
                viewModel = viewModel,
                parentCommentId = currentThreadId,
                onBack = { viewModel.setActiveThread(null) }
            )
        } else {
            MainCommentsContent(
                viewModel = viewModel,
                onBack = onBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCommentsContent(
    viewModel: SocialViewModel,
    onBack: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var isSpoilerMode by remember { mutableStateOf(false) }
    val spoilerIconColor by animateColorAsState(targetValue = if (isSpoilerMode) Color.Red else Color.Gray, animationSpec = tween(300))
    
    val allComments by viewModel.comments.collectAsState()
    val mainComments = allComments.filter { it.parentId == null }
    val targetScrollCommentId by viewModel.targetScrollCommentId.collectAsState()
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(targetScrollCommentId) {
        if (targetScrollCommentId != null) {
            val commentId = targetScrollCommentId!!
            val index = mainComments.indexOfFirst { it.id == commentId }
            if (index != -1) {
                listState.animateScrollToItem(index)
                delay(2000)
                viewModel.clearHighlight(commentId)
                viewModel.setTargetScrollComment(null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "التعليقات (${mainComments.size})", 
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sort options */ }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            CommentInputBar(
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                isSpoilerMode = isSpoilerMode,
                onSpoilerToggle = { isSpoilerMode = !isSpoilerMode },
                onSend = {
                    if(commentText.isNotBlank()) {
                        viewModel.addComment(commentText, isSpoilerMode)
                        commentText = ""
                        isSpoilerMode = false
                    }
                },
                spoilerIconColor = spoilerIconColor
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = mainComments, key = { it.id }) { comment ->
                val replyCount = allComments.count { it.parentId == comment.id }
                CommentItem(
                    comment = comment,
                    replyCount = replyCount,
                    onLikeClick = { viewModel.toggleLike(comment.id) },
                    onDislikeClick = { viewModel.toggleDislike(comment.id) },
                    onReplyClick = { viewModel.setActiveThread(comment.id) },
                    showReplyButton = true
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeThreadScreen(
    viewModel: SocialViewModel,
    parentCommentId: Int,
    onBack: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var isSpoilerMode by remember { mutableStateOf(false) }
    val spoilerIconColor by animateColorAsState(targetValue = if (isSpoilerMode) Color.Red else Color.Gray, animationSpec = tween(300))
    
    val allComments by viewModel.comments.collectAsState()
    val parentComment = allComments.find { it.id == parentCommentId }
    val threadReplies = allComments.filter { it.parentId == parentCommentId }
    val targetScrollCommentId by viewModel.targetScrollCommentId.collectAsState()
    val peerReplyTo by viewModel.peerReplyTo.collectAsState()
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(peerReplyTo) {
        if (peerReplyTo != null) {
            commentText = "@${peerReplyTo!!.username} "
        }
    }
    
    LaunchedEffect(targetScrollCommentId) {
        if (targetScrollCommentId != null && targetScrollCommentId != parentCommentId) {
            val commentId = targetScrollCommentId!!
            val index = threadReplies.indexOfFirst { it.id == commentId }
            if (index != -1) {
                listState.animateScrollToItem(index)
                delay(2000)
                viewModel.clearHighlight(commentId)
                viewModel.setTargetScrollComment(null)
            }
        } else if (targetScrollCommentId == parentCommentId) {
            listState.animateScrollToItem(0)
            delay(2000)
            viewModel.clearHighlight(parentCommentId)
            viewModel.setTargetScrollComment(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "الردود", 
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            CommentInputBar(
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                isSpoilerMode = isSpoilerMode,
                onSpoilerToggle = { isSpoilerMode = !isSpoilerMode },
                onSend = {
                    if(commentText.isNotBlank()) {
                        val replyingToUsername = peerReplyTo?.username
                        val replyingToCommentId = peerReplyTo?.id
                        viewModel.addComment(
                            text = commentText, 
                            isSpoiler = isSpoilerMode, 
                            parentId = parentCommentId,
                            replyingToUsername = replyingToUsername,
                            replyingToCommentId = replyingToCommentId
                        )
                        commentText = ""
                        isSpoilerMode = false
                    }
                },
                spoilerIconColor = spoilerIconColor
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (parentComment != null) {
                item {
                    val totalReplies = allComments.count { it.parentId == parentComment.id }
                    CommentItem(
                        comment = parentComment,
                        replyCount = totalReplies,
                        onLikeClick = { viewModel.toggleLike(parentComment.id) },
                        onDislikeClick = { viewModel.toggleDislike(parentComment.id) },
                        onReplyClick = { viewModel.setPeerReplyTo(parentComment) },
                        showReplyButton = true
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 2.dp)
                }
            }
            items(items = threadReplies, key = { it.id }) { reply ->
                CommentItem(
                    comment = reply,
                    replyCount = 0,
                    onLikeClick = { viewModel.toggleLike(reply.id) },
                    onDislikeClick = { viewModel.toggleDislike(reply.id) },
                    onReplyClick = { viewModel.setPeerReplyTo(reply) },
                    showReplyButton = true,
                    isThreadReply = true
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            }
        }
    }
}

@Composable
fun CommentInputBar(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    isSpoilerMode: Boolean,
    onSpoilerToggle: () -> Unit,
    onSend: () -> Unit,
    spoilerIconColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSpoilerToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Spoiler Toggle",
                    tint = spoilerIconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = onCommentTextChange,
                        placeholder = { Text("أضف تعليقاً...", color = Color.Gray, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(color = Color.White)
                    )
                    IconButton(onClick = onSend) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: AnimeComment,
    replyCount: Int,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    showReplyButton: Boolean = true,
    isThreadReply: Boolean = false
) {
    var isRevealed by remember { mutableStateOf(false) }
    val highlightColor by animateColorAsState(
        targetValue = if (comment.isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(1000)
    )
    
    var likeScale by remember { mutableStateOf(1f) }
    val animatedLikeScale by animateFloatAsState(
        targetValue = likeScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    var dislikeScale by remember { mutableStateOf(1f) }
    val animatedDislikeScale by animateFloatAsState(
        targetValue = dislikeScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    var replyScale by remember { mutableStateOf(1f) }
    val animatedReplyScale by animateFloatAsState(
        targetValue = replyScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    val likeColor = if (comment.isLiked) Color.Red else Color.Gray
    val dislikeColor = if (comment.isDisliked) Color.Yellow else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightColor)
            .padding(if (isThreadReply) PaddingValues(start = 32.dp, end = 16.dp, top = 16.dp, bottom = 16.dp) else PaddingValues(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(comment.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (isThreadReply) 32.dp else 40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.username,
                        color = Color.LightGray,
                        fontSize = if (isThreadReply) 13.sp else 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = comment.timeAgo,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnimatedContent(
                    targetState = comment.isSpoiler && !isRevealed,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "SpoilerRevealAnimation"
                ) { isHidden ->
                    if (isHidden) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRevealed = true }
                                .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "التعليق يحتوي على حرق, اضغط هنا للمشاهدة",
                                color = Color.Red,
                                fontSize = if (isThreadReply) 13.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = comment.text,
                            color = Color.White,
                            fontSize = if (isThreadReply) 14.sp else 15.sp,
                            lineHeight = if (isThreadReply) 20.sp else 22.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.clickable { 
                                likeScale = 1.3f
                                onLikeClick()
                            }
                        ) {
                            LaunchedEffect(likeScale) {
                                if (likeScale > 1f) { delay(100); likeScale = 1f }
                            }
                            Icon(
                                Icons.Default.ThumbUp, 
                                contentDescription = "Like", 
                                tint = likeColor, 
                                modifier = Modifier.size(18.dp).scale(animatedLikeScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${comment.likes}", color = likeColor, fontSize = 14.sp)
                        }
                        
                        // Dislike
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.clickable { 
                                dislikeScale = 1.3f
                                onDislikeClick()
                            }
                        ) {
                            LaunchedEffect(dislikeScale) {
                                if (dislikeScale > 1f) { delay(100); dislikeScale = 1f }
                            }
                            Icon(
                                Icons.Default.ThumbDown, 
                                contentDescription = "Dislike", 
                                tint = dislikeColor, 
                                modifier = Modifier.size(18.dp).scale(animatedDislikeScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${comment.dislikes}", color = dislikeColor, fontSize = 14.sp)
                        }
                        
                        // Reply
                        if (showReplyButton) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.clickable {
                                    replyScale = 1.3f
                                    onReplyClick()
                                }
                            ) {
                                LaunchedEffect(replyScale) {
                                    if (replyScale > 1f) { delay(100); replyScale = 1f }
                                }
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Reply", tint = Color.Gray, modifier = Modifier.size(18.dp).scale(animatedReplyScale))
                                Spacer(modifier = Modifier.width(6.dp))
                                if (!isThreadReply) {
                                    Text("$replyCount", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                    }
                }
            }
        }
    }
}
