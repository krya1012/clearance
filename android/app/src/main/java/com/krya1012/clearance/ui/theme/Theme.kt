package com.krya1012.clearance.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.krya1012.clearance.data.ChecklistType

private val MorningDark = darkColorScheme(
    primary = MorningAccentDark,
    secondary = MorningAccentDark,
    background = MorningBackgroundDark,
    surface = MorningSurfaceDark,
    onBackground = MorningOnBackgroundDark,
    onSurface = MorningOnBackgroundDark,
)

private val MorningLight = lightColorScheme(
    primary = MorningAccentLight,
    secondary = MorningAccentLight,
    background = MorningBackgroundLight,
    surface = MorningSurfaceLight,
    onBackground = MorningOnBackgroundLight,
    onSurface = MorningOnBackgroundLight,
)

// True-black only in dark mode — same as iOS, no longer forced regardless of
// the system's appearance setting.
private val EveningDark = darkColorScheme(
    primary = EveningAccentDark,
    secondary = EveningAccentDark,
    background = EveningBackgroundDark,
    surface = EveningSurfaceDark,
    onBackground = EveningOnBackgroundDark,
    onSurface = EveningOnBackgroundDark,
)

private val EveningLight = lightColorScheme(
    primary = EveningAccentLight,
    secondary = EveningAccentLight,
    background = EveningBackgroundLight,
    surface = EveningSurfaceLight,
    onBackground = EveningOnBackgroundLight,
    onSurface = EveningOnBackgroundLight,
)

private const val ThemeTransitionMillis = 220

/**
 * Follows the device's system light/dark appearance setting — same as the
 * rest of Android — for both sequences. No custom day/night logic.
 */
@Composable
fun ClearanceTheme(
    checklistType: ChecklistType,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val target = when (checklistType) {
        ChecklistType.MORNING -> if (isDark) MorningDark else MorningLight
        ChecklistType.EVENING -> if (isDark) EveningDark else EveningLight
    }

    // Cross-fades the palette when switching Takeoff/Landing instead of snapping instantly.
    // Kept short (220ms) since Compose has no established Reduce-Motion signal in this
    // codebase yet (see android/README.md's known-limitations note).
    val animSpec = tween<Color>(durationMillis = ThemeTransitionMillis)
    val colorScheme = target.copy(
        primary = animateColorAsState(target.primary, animSpec, label = "primary").value,
        secondary = animateColorAsState(target.secondary, animSpec, label = "secondary").value,
        background = animateColorAsState(target.background, animSpec, label = "background").value,
        surface = animateColorAsState(target.surface, animSpec, label = "surface").value,
        onBackground = animateColorAsState(target.onBackground, animSpec, label = "onBackground").value,
        onSurface = animateColorAsState(target.onSurface, animSpec, label = "onSurface").value,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClearanceTypography,
        content = content
    )
}
