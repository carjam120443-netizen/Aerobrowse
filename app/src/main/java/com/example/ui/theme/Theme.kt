package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAeroPalette = staticCompositionLocalOf {
    AeroThemePreset.SKY_AERO.toAeroPalette()
}

@Composable
fun AeroBrowseTheme(
    preset: AeroThemePreset = AeroThemePreset.SKY_AERO,
    glassIntensity: Float = 0.85f,
    content: @Composable () -> Unit,
) {
    val targetPalette = preset.toAeroPalette(glassIntensity)

    val animatedPrimary = animateColorAsState(targetPalette.colorScheme.primary, tween(320), label = "primary").value
    val animatedSecondary = animateColorAsState(targetPalette.colorScheme.secondary, tween(320), label = "secondary").value
    val animatedSurface = animateColorAsState(targetPalette.glassSurface, tween(320), label = "glassSurface").value
    val animatedElevated = animateColorAsState(targetPalette.glassSurfaceElevated, tween(320), label = "glassElevated").value

    val palette = targetPalette.copy(
        colorScheme = targetPalette.colorScheme.copy(
            primary = animatedPrimary,
            secondary = animatedSecondary,
        ),
        glassSurface = animatedSurface,
        glassSurfaceElevated = animatedElevated,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !preset.isDark
            controller.isAppearanceLightNavigationBars = !preset.isDark
        }
    }

    CompositionLocalProvider(LocalAeroPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
