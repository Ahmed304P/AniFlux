package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnimeComment(
    val id: Int,
    val username: String,
    val avatarUrl: String,
    val timeAgo: String,
    val text: String,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val parentId: Int? = null,
    val replyingToUsername: String? = null,
    val replyingToCommentId: Int? = null,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isHighlighted: Boolean = false,
    val isSpoiler: Boolean = false
)

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val targetCommentId: Int,
    val parentThreadId: Int? = null,
    val isRead: Boolean = false
)

class SocialViewModel : ViewModel() {

    private val _comments = MutableStateFlow<List<AnimeComment>>(listOf(
        AnimeComment(
            1, "Suspicious", "https://i.pravatar.cc/150?img=1", "قبل ساعتين",
            "انا بنت والصراحة وقت شفت الشخصيات حسيت اللي شعرها بنفسجي حلوة ففتحتها لقيتها ولد 🤣🤣💔\n\nيا عمي هاذا يشبه كل شي الا الرجال"
        ),
        AnimeComment(
            2, "mohamad ali", "https://i.pravatar.cc/150?img=11", "قبل يوم",
            "امممم"
        ),
        AnimeComment(
            3, "طالعه من الباك وبدي انمي يرجع لي الشغف", "https://i.pravatar.cc/150?img=5", "قبل يوم",
            "اقترحوا علي🤗",
            likes = 4, dislikes = 0
        ),
        AnimeComment(
            4, "mini✨🌸", "https://i.pravatar.cc/150?img=0", "قبل يوم",
            "هل الأنمي مثلي لسة ما شاهدته",
            likes = 10, dislikes = 2
        )
    ))
    val comments: StateFlow<List<AnimeComment>> = _comments.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()
    
    private val _targetScrollCommentId = MutableStateFlow<Int?>(null)
    val targetScrollCommentId = _targetScrollCommentId.asStateFlow()

    private val _shouldOpenComments = MutableStateFlow(false)
    val shouldOpenComments = _shouldOpenComments.asStateFlow()
    
    private val _activeThreadCommentId = MutableStateFlow<Int?>(null)
    val activeThreadCommentId = _activeThreadCommentId.asStateFlow()
    
    private val _peerReplyTo = MutableStateFlow<AnimeComment?>(null)
    val peerReplyTo = _peerReplyTo.asStateFlow()
    
    fun setActiveThread(commentId: Int?) {
        _activeThreadCommentId.value = commentId
        _peerReplyTo.value = null
    }

    fun setPeerReplyTo(comment: AnimeComment?) {
        _peerReplyTo.value = comment
    }

    fun addComment(text: String, isSpoiler: Boolean, parentId: Int? = null, replyingToUsername: String? = null, replyingToCommentId: Int? = null) {
        val newId = (_comments.value.maxOfOrNull { it.id } ?: 0) + 1
        val newComment = AnimeComment(
            id = newId,
            username = "أنت",
            avatarUrl = "https://i.pravatar.cc/150?img=12",
            timeAgo = "الآن",
            text = text,
            parentId = parentId,
            replyingToUsername = replyingToUsername,
            replyingToCommentId = replyingToCommentId,
            isSpoiler = isSpoiler
        )
        _comments.update { listOf(newComment) + it }
        
        if (parentId != null) {
            // Rule 1: Master Notification Engine - Parent Rule
            val parentComment = _comments.value.find { it.id == parentId }
            if (parentComment != null && parentComment.username != "أنت") {
                addNotification("رد جديد", "هناك رد جديد على تعليقك", newId, parentId)
            }
            
            // Rule 2: Peer-to-Peer Rule
            if (replyingToCommentId != null && replyingToCommentId != parentId) {
                val peerComment = _comments.value.find { it.id == replyingToCommentId }
                if (peerComment != null && peerComment.username != "أنت") {
                    addNotification("رد جديد", "شخص ما رد عليك", newId, parentId)
                }
            }
        }
        
        _peerReplyTo.value = null
    }

    fun setShouldOpenComments(open: Boolean) {
        _shouldOpenComments.value = open
    }
    
    private val _shouldOpenNotifications = MutableStateFlow(false)
    val shouldOpenNotifications = _shouldOpenNotifications.asStateFlow()
    
    fun setShouldOpenNotifications(open: Boolean) {
        _shouldOpenNotifications.value = open
    }
    
    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun toggleLike(commentId: Int) {
        _comments.update { list ->
            list.map { comment ->
                if (comment.id == commentId) {
                    val wasLiked = comment.isLiked
                    val wasDisliked = comment.isDisliked
                    
                    val newIsLiked = !wasLiked
                    val newIsDisliked = if (newIsLiked) false else wasDisliked
                    
                    val newLikes = if (newIsLiked) comment.likes + 1 else comment.likes - 1
                    val newDislikes = if (wasDisliked && newIsLiked) comment.dislikes - 1 else comment.dislikes
                    
                    if (newIsLiked && comment.username != "أنت") {
                        addNotification("إعجاب جديد", if (comment.parentId != null) "شخص ما أعجب بردك" else "شخص ما أعجب بتعليقك", commentId, comment.parentId)
                    }
                    
                    comment.copy(
                        isLiked = newIsLiked,
                        isDisliked = newIsDisliked,
                        likes = newLikes,
                        dislikes = newDislikes
                    )
                } else {
                    comment
                }
            }
        }
    }

    fun toggleDislike(commentId: Int) {
        _comments.update { list ->
            list.map { comment ->
                if (comment.id == commentId) {
                    val wasLiked = comment.isLiked
                    val wasDisliked = comment.isDisliked
                    
                    val newIsDisliked = !wasDisliked
                    val newIsLiked = if (newIsDisliked) false else wasLiked
                    
                    val newDislikes = if (newIsDisliked) comment.dislikes + 1 else comment.dislikes - 1
                    val newLikes = if (wasLiked && newIsDisliked) comment.likes - 1 else comment.likes
                    
                    comment.copy(
                        isLiked = newIsLiked,
                        isDisliked = newIsDisliked,
                        likes = newLikes,
                        dislikes = newDislikes
                    )
                } else {
                    comment
                }
            }
        }
    }

    private var notificationIdCounter = 1

    private fun addNotification(title: String, message: String, targetCommentId: Int, parentThreadId: Int? = null) {
        val newNotif = NotificationItem(
            id = notificationIdCounter++,
            title = title,
            message = message,
            targetCommentId = targetCommentId,
            parentThreadId = parentThreadId
        )
        _notifications.update { listOf(newNotif) + it }
    }
    
    fun markNotificationsAsRead() {
        _notifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }
    
    fun setTargetScrollComment(commentId: Int?, parentThreadId: Int? = null) {
        if (parentThreadId != null) {
            _activeThreadCommentId.value = parentThreadId
        }
        _targetScrollCommentId.value = commentId
        if (commentId != null) {
            _comments.update { list ->
                list.map { if (it.id == commentId) it.copy(isHighlighted = true) else it.copy(isHighlighted = false) }
            }
        } else {
            _comments.update { list -> list.map { it.copy(isHighlighted = false) } }
        }
    }
    
    fun clearHighlight(commentId: Int) {
        _comments.update { list ->
            list.map { if (it.id == commentId) it.copy(isHighlighted = false) else it }
        }
    }
}
