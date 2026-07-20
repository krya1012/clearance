package com.krya1012.clearance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.ui.theme.Layout

/**
 * TODAY / DONE TODAY / PACKING FOR TOMORROW rows — the Android analog of iOS
 * `ChecklistView.activitySection`. Emitted as the first content in the dashboard's
 * LazyColumn so it scrolls away with the task list rather than staying fixed above it.
 */
fun LazyListScope.activitySelectorSection(
    checklist: ChecklistType,
    enabledModules: List<ActivityModule>,
    todayActivityIDs: Set<String>,
    tomorrowActivityIDs: Set<String>,
    onToggleToday: (ActivityModule) -> Unit,
    onToggleTomorrow: (ActivityModule) -> Unit,
) {
    if (enabledModules.isEmpty()) return

    if (checklist == ChecklistType.MORNING) {
        activityRows("TODAY", enabledModules, todayActivityIDs, onToggleToday)
    } else {
        activityRows("DONE TODAY", enabledModules, todayActivityIDs, onToggleToday)
        activityRows("PACKING FOR TOMORROW", enabledModules, tomorrowActivityIDs, onToggleTomorrow)
    }
}

private fun LazyListScope.activityRows(
    title: String,
    modules: List<ActivityModule>,
    selectedIds: Set<String>,
    onToggle: (ActivityModule) -> Unit,
) {
    item(key = "activity-header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Layout.ScreenPadding, vertical = 4.dp),
        )
    }
    items(modules, key = { "activity-$title-${it.id}" }) { module ->
        ActivityRow(
            module = module,
            selected = module.id in selectedIds,
            onClick = { onToggle(module) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun ActivityRow(
    module: ActivityModule,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = module.name
                role = Role.Checkbox
                this.selected = selected
            }
            .padding(horizontal = Layout.ScreenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = module.emoji, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = module.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        val tint = MaterialTheme.colorScheme.primary
        val border = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) tint else Color.Transparent)
                .border(width = 2.dp, color = if (selected) tint else border, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Text(text = "✓", color = Color.Black.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
