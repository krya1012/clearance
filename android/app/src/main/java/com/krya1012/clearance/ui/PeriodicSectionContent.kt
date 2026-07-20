package com.krya1012.clearance.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krya1012.clearance.data.PeriodicTask
import com.krya1012.clearance.data.Recurrence
import com.krya1012.clearance.ui.theme.Layout

/**
 * "This Week" / "This Month" recurring-task sections, appended after module sections — the
 * Android analog of iOS `ChecklistView.periodicSection`. Unlike module sections, these are
 * always shown (never gated by today/tomorrow schedule), split purely by [PeriodicTask.recurrence].
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.periodicSectionContent(
    weeklyTasks: List<PeriodicTask>,
    monthlyTasks: List<PeriodicTask>,
    onToggle: (PeriodicTask) -> Unit,
    onDelete: (PeriodicTask) -> Unit,
    onAdd: (Recurrence) -> Unit,
) {
    periodicRows(
        key = "weekly",
        title = "THIS WEEK",
        emoji = "📅",
        tasks = weeklyTasks,
        recurrence = Recurrence.WEEKLY,
        onToggle = onToggle,
        onDelete = onDelete,
        onAdd = onAdd,
    )
    periodicRows(
        key = "monthly",
        title = "THIS MONTH",
        emoji = "🗓",
        tasks = monthlyTasks,
        recurrence = Recurrence.MONTHLY,
        onToggle = onToggle,
        onDelete = onDelete,
        onAdd = onAdd,
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.periodicRows(
    key: String,
    title: String,
    emoji: String,
    tasks: List<PeriodicTask>,
    recurrence: Recurrence,
    onToggle: (PeriodicTask) -> Unit,
    onDelete: (PeriodicTask) -> Unit,
    onAdd: (Recurrence) -> Unit,
) {
    stickyHeader(key = "periodic-header-$key") {
        PeriodicHeader(emoji = emoji, title = title, onAdd = { onAdd(recurrence) })
    }
    if (tasks.isEmpty()) {
        item(key = "periodic-empty-$key") {
            Text(
                text = "Tap + to add a task",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Layout.ScreenPadding, vertical = 6.dp),
            )
        }
    } else {
        items(tasks, key = { "periodic-${it.id}" }) { task ->
            PeriodicTaskRow(
                task = task,
                onToggle = { onToggle(task) },
                onDelete = { onDelete(task) },
                modifier = Modifier.padding(horizontal = Layout.ScreenPadding, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun PeriodicHeader(emoji: String, title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Layout.ScreenPadding, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(
            text = "+",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable(onClick = onAdd)
                .semantics {
                    contentDescription = "Add $title task"
                    role = Role.Button
                }
                .padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun PeriodicTaskRow(
    task: PeriodicTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Layout.RowCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription = "${task.title}, ${if (task.isCompleted) "Completed" else "Not completed"}"
                role = Role.Checkbox
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeriodicCheckbox(isCompleted = task.isCompleted)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = task.emoji, modifier = Modifier.width(24.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        Box {
            Text(
                text = "⋮",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { menuExpanded = true }
                    .semantics {
                        contentDescription = "More options for ${task.title}"
                        role = Role.Button
                    },
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuExpanded = false; showDeleteConfirm = true },
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${task.title}?") },
            text = { Text("This task will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PeriodicCheckbox(isCompleted: Boolean) {
    val tint = MaterialTheme.colorScheme.primary
    val border = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .size(Layout.CheckboxSize)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isCompleted) tint else Color.Transparent)
            .border(width = 2.dp, color = if (isCompleted) tint else border, shape = RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Text(
                text = "✓",
                color = Color.Black.copy(alpha = 0.85f),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }
    }
}
