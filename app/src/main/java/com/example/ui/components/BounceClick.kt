package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// Kept the same name to avoid refactoring hundreds of files, but it's now just a regular clickable
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClick(
    scaleDown: Float = 1f, // ignored
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.combinedClickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(),
        onClick = onClick,
        onLongClick = onLongClick
    )
}
