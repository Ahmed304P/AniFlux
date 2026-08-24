package com.example.ui.theme
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.viewmodel.AppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue

private val DarkColorScheme = darkColorScheme(
    primary = AniFluxPrimary,
    onPrimary = Color.White,
    primaryContainer = AniFluxPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = AniFluxSecondary,
    onSecondary = Color.White,
    tertiary = AniFluxCyan,
    onTertiary = Color.White,
    background = AniFluxBg,
    onBackground = AniFluxTextPrimary,
    surface = AniFluxSurface,
    onSurface = AniFluxTextPrimary,
    surfaceVariant = AniFluxSurfaceVariant,
    onSurfaceVariant = AniFluxTextSecondary
)

val BlackColorScheme = darkColorScheme(
    primary = Color(0xFFE50914), // Netflix red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB81D24),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF831010),
    onSecondary = Color.White,
    tertiary = Color(0xFF56CCF2),
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

val BlueColorScheme = darkColorScheme(
    primary = Color(0xFF3b82f6), 
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2563eb),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF60a5fa),
    onSecondary = Color.White,
    tertiary = Color(0xFF93c5fd),
    onTertiary = Color.White,
    background = Color(0xFF020617),
    onBackground = Color.White,
    surface = Color(0xFF0f172a),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1e293b),
    onSurfaceVariant = Color(0xFF94a3b8)
)

@Composable
fun AniFluxTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val targetColorScheme = when (appTheme) {
        AppTheme.DEFAULT -> DarkColorScheme
        AppTheme.DARK_BLACK -> BlackColorScheme
        AppTheme.DARK_BLUE -> BlueColorScheme
    }

    // Animate colors for smooth transition
    val primary by animateColorAsState(targetColorScheme.primary, tween(500))
    val background by animateColorAsState(targetColorScheme.background, tween(500))
    val surface by animateColorAsState(targetColorScheme.surface, tween(500))
    val onBackground by animateColorAsState(targetColorScheme.onBackground, tween(500))
    val surfaceVariant by animateColorAsState(targetColorScheme.surfaceVariant, tween(500))
    val onSurfaceVariant by animateColorAsState(targetColorScheme.onSurfaceVariant, tween(500))
    val onSurface by animateColorAsState(targetColorScheme.onSurface, tween(500))
    
    val animatedColorScheme = targetColorScheme.copy(
        primary = primary,
        background = background,
        surface = surface,
        onBackground = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        onSurface = onSurface
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}
