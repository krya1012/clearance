package com.krya1012.clearance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import com.krya1012.clearance.data.Weekday

/**
 * Day (Mon–Sun) × module toggle grid over the persistent weekly schedule — distinct from the
 * dashboard's per-day override toggles. A manual Column/Row grid, mirroring iOS
 * `WeeklyPlanGrid`'s manual VStack/HStack (not a LazyVGrid).
 */
@Composable
fun WeeklyPlanGrid(
    modules: List<ActivityModule>,
    weeklySchedule: Map<Weekday, Set<String>>,
    onToggle: (ActivityModule, Weekday) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(40.dp))
            modules.forEach { module ->
                Text(
                    text = module.emoji,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider()
        Weekday.displayOrder.forEachIndexed { index, day ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = day.short,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(40.dp),
                )
                modules.forEach { module ->
                    val scheduled = module.id in (weeklySchedule[day] ?: emptySet())
                    DayModuleToggle(
                        module = module,
                        day = day,
                        scheduled = scheduled,
                        onClick = { onToggle(module, day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (index != Weekday.displayOrder.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun DayModuleToggle(
    module: ActivityModule,
    day: Weekday,
    scheduled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    val border = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${module.name} on ${day.label}"
                role = Role.Checkbox
                selected = scheduled
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (scheduled) tint else Color.Transparent)
                .border(width = 2.dp, color = if (scheduled) tint else border, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (scheduled) {
                Text(text = "✓", color = Color.Black.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
