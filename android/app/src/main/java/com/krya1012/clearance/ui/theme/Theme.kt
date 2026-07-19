package com.krya1012.clearance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.krya1012.clearance.ui.ChecklistType

private val MorningColorScheme = lightColorScheme(
    primary = MorningAccent,
    secondary = MorningAccentVariant,
    background = MorningBackground,
    surface = MorningSurface,
    onBackground = MorningOnBackground,
    onSurface = MorningOnBackground,
)

// Landing forces true-black regardless of system appearance, matching
// `.preferredColorScheme(.dark)` on the iOS dashboard.
private val EveningColorScheme = darkColorScheme(
    primary = EveningAccent,
    secondary = EveningAccent,
    background = EveningBackground,
    surface = EveningSurface,
    onBackground = EveningOnBackground,
    onSurface = EveningOnBackground,
)

@Composable
fun ClearanceTheme(
    checklistType: ChecklistType,
    content: @Composable () -> Unit
) {
    val colorScheme = when (checklistType) {
        ChecklistType.MORNING -> MorningColorScheme
        ChecklistType.EVENING -> EveningColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClearanceTypography,
        content = content
    )
}
