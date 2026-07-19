package com.krya1012.clearance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.ui.theme.Layout
import com.krya1012.clearance.util.Haptics
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Core (fixed) + locked modules (toggle/restore only) + reorderable, editable unlocked
 * optional modules + "add" / "browse templates" entry points. Mirrors iOS `ModuleManagerView`.
 *
 * Restore/delete are exposed via a "⋮" overflow menu rather than bidirectional swipe, matching
 * this codebase's established divergence from iOS's swipe actions (Compose's SwipeToDismissBox
 * cleanly supports only one dismiss direction).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleManagerSheet(
    modules: List<ActivityModule>,
    enabledModuleIDs: Set<String>,
    hasDefaultTasks: (ActivityModule) -> Boolean,
    onToggleEnabled: (ActivityModule) -> Unit,
    onEdit: (ActivityModule) -> Unit,
    onRestoreDefaults: (ActivityModule) -> Unit,
    onDelete: (ActivityModule) -> Unit,
    onMoveUnlocked: (from: Int, to: Int) -> Unit,
    onAddModule: () -> Unit,
    onBrowseTemplates: () -> Unit,
    onDismiss: () -> Unit,
) {
    val core = modules.firstOrNull { it.isCore }
    val locked = modules.filter { it.isOptional && it.isLocked }.sortedBy { it.sortOrder }
    val unlocked = modules.filter { it.isOptional && !it.isLocked }.sortedBy { it.sortOrder }
    val headerCount = (if (core != null) 1 else 0) + locked.size

    var moduleToDelete by remember { mutableStateOf<ActivityModule?>(null) }
    val view = LocalView.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Layout.ScreenPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Modules", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onAddModule) { Text("+ Add") }
        }

        val lazyListState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = (from.index - headerCount).coerceIn(0, unlocked.lastIndex.coerceAtLeast(0))
            val toIndex = (to.index - headerCount).coerceIn(0, unlocked.lastIndex.coerceAtLeast(0))
            if (fromIndex != toIndex) {
                Haptics.moduleToggled(view)
                onMoveUnlocked(fromIndex, toIndex)
            }
        }

        LazyColumn(state = lazyListState) {
            if (core != null) {
                item(key = "core") { CoreRow(core) }
            }
            items(locked, key = { it.id }) { module ->
                OptionalModuleRow(
                    module = module,
                    enabled = module.id in enabledModuleIDs,
                    caption = "Locked",
                    canDelete = false,
                    canRestore = hasDefaultTasks(module),
                    onToggleEnabled = { Haptics.moduleToggled(view); onToggleEnabled(module) },
                    onEdit = null,
                    onRestoreDefaults = { Haptics.reset(view); onRestoreDefaults(module) },
                    onDelete = { moduleToDelete = module },
                )
            }
            items(unlocked, key = { it.id }) { module ->
                ReorderableItem(reorderState, key = module.id) { _ ->
                    OptionalModuleRow(
                        module = module,
                        enabled = module.id in enabledModuleIDs,
                        caption = null,
                        canDelete = true,
                        canRestore = hasDefaultTasks(module),
                        onToggleEnabled = { Haptics.moduleToggled(view); onToggleEnabled(module) },
                        onEdit = { onEdit(module) },
                        onRestoreDefaults = { Haptics.reset(view); onRestoreDefaults(module) },
                        onDelete = { moduleToDelete = module },
                        dragHandle = {
                            Text(
                                text = "☰",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .draggableHandle()
                                    .semantics { contentDescription = "Reorder ${module.name}" },
                            )
                        },
                    )
                }
            }
            item(key = "browse-templates") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBrowseTemplates)
                        .padding(horizontal = Layout.ScreenPadding, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Browse templates", style = MaterialTheme.typography.bodyMedium)
                    Text(">")
                }
            }
        }
        Text(
            text = "Toggle on/off to show or hide in checklist and weekly plan. Hidden modules are not deleted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Layout.ScreenPadding, vertical = 8.dp),
        )
    }

    moduleToDelete?.let { module ->
        AlertDialog(
            onDismissRequest = { moduleToDelete = null },
            title = { Text("Delete ${module.name}?") },
            text = { Text("All tasks for this module will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { Haptics.reset(view); onDelete(module); moduleToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { moduleToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CoreRow(core: ActivityModule) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Layout.ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = core.label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        Text(text = "Core", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OptionalModuleRow(
    module: ActivityModule,
    enabled: Boolean,
    caption: String?,
    canDelete: Boolean,
    canRestore: Boolean,
    onToggleEnabled: () -> Unit,
    onEdit: (() -> Unit)?,
    onRestoreDefaults: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Layout.ScreenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = MaterialTheme.colorScheme.primary
        val border = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onToggleEnabled)
                .background(if (enabled) tint else Color.Transparent, CircleShape)
                .border(width = 2.dp, color = if (enabled) tint else border, shape = CircleShape)
                .semantics {
                    contentDescription = "${module.name}, ${if (enabled) "enabled" else "disabled"}"
                    role = Role.Checkbox
                    selected = enabled
                },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = module.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier),
        )
        if (caption != null) {
            Text(text = caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            Text(
                text = "⋮",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { menuExpanded = true }
                    .semantics {
                        contentDescription = "More options for ${module.name}"
                        role = Role.Button
                    },
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (canRestore) {
                    DropdownMenuItem(
                        text = { Text("Restore defaults") },
                        onClick = { menuExpanded = false; onRestoreDefaults() },
                    )
                }
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
        dragHandle?.invoke()
    }
}
