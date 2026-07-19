package com.krya1012.clearance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.ui.theme.Layout
import com.krya1012.clearance.vm.ChecklistPhase
import com.krya1012.clearance.vm.ChecklistSection

/**
 * Module -> phase -> task rows, the Android analog of iOS `ChecklistView`'s per-module
 * `Section`s. Each module gets a sticky header; phase sub-headers are non-interactive.
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.checklistSectionContent(
    sections: List<ChecklistSection>,
    onToggle: (ChecklistItem) -> Unit,
    onEdit: (ChecklistItem) -> Unit,
) {
    sections.forEach { section ->
        stickyHeader(key = "module-${section.module.id}") {
            ModuleHeader(emoji = section.module.emoji, name = section.module.name)
        }
        section.phases.forEach { phase ->
            if (phase.name.isNotEmpty()) {
                item(key = "phase-${section.module.id}-${phase.phaseIndex}") {
                    PhaseSubHeader(phase)
                }
            }
            items(phase.items, key = { it.id }) { item ->
                ChecklistRow(
                    item = item,
                    onToggle = { onToggle(item) },
                    onEdit = { onEdit(item) },
                    modifier = Modifier.padding(horizontal = Layout.ScreenPadding, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ModuleHeader(emoji: String, name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Layout.ScreenPadding, vertical = 4.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PhaseSubHeader(phase: ChecklistPhase) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.ScreenPadding, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Phase ${phase.phaseIndex + 1}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    RoundedCornerShape(50),
                )
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
        Text(
            text = phase.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
