package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(100f) }

    LaunchedEffect(Unit) {
        val delay = (index * 50).coerceAtMost(500)
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, delayMillis = delay)
        )
    }
    
    LaunchedEffect(Unit) {
        val delay = (index * 50).coerceAtMost(500)
        animatedTranslationY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, delayMillis = delay)
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = animatedAlpha.value
            translationY = animatedTranslationY.value
        }
    ) {
        content()
    }
}
