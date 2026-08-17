package com.whitecall.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryDark,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder
)

@Composable
fun WhiteCallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetColors = if (darkTheme) DarkColorScheme else LightColorScheme
    val animationSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 200, easing = FastOutSlowInEasing)

    val animatedPrimary by animateColorAsState(targetColors.primary, animationSpec, label = "primary")
    val animatedBackground by animateColorAsState(targetColors.background, animationSpec, label = "background")
    val animatedSurface by animateColorAsState(targetColors.surface, animationSpec, label = "surface")
    val animatedSurfaceVariant by animateColorAsState(targetColors.surfaceVariant, animationSpec, label = "surfaceVariant")
    val animatedOnBackground by animateColorAsState(targetColors.onBackground, animationSpec, label = "onBackground")
    val animatedOnSurface by animateColorAsState(targetColors.onSurface, animationSpec, label = "onSurface")
    val animatedOnSurfaceVariant by animateColorAsState(targetColors.onSurfaceVariant, animationSpec, label = "onSurfaceVariant")
    val animatedOutline by animateColorAsState(targetColors.outline, animationSpec, label = "outline")
    val animatedPrimaryContainer by animateColorAsState(targetColors.primaryContainer, animationSpec, label = "primaryContainer")
    val animatedOnPrimaryContainer by animateColorAsState(targetColors.onPrimaryContainer, animationSpec, label = "onPrimaryContainer")

    val colorScheme = targetColors.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
        onBackground = animatedOnBackground,
        onSurface = animatedOnSurface,
        onSurfaceVariant = animatedOnSurfaceVariant,
        outline = animatedOutline,
        primaryContainer = animatedPrimaryContainer,
        onPrimaryContainer = animatedOnPrimaryContainer
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = targetColors.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
