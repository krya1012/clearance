package com.krya1012.clearance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    val colorScheme = when (checklistType) {
        ChecklistType.MORNING -> if (isDark) MorningDark else MorningLight
        ChecklistType.EVENING -> if (isDark) EveningDark else EveningLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClearanceTypography,
        content = content
    )
}
