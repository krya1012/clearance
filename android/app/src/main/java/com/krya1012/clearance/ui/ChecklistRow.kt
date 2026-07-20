package com.krya1012.clearance.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
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
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.ui.theme.Layout

/**
 * One task row: checkbox, title (dimmed/struck-through when done), SKIPPED TODAY badge, and a
 * "⋮" overflow menu (Skip/Restore/Delete). Tap toggles completion; long-press opens the edit
 * sheet ([onEdit]).
 *
 * The delete-confirmation dialog is kept as local per-row state (unlike `ModuleManagerSheet`,
 * which hoists it to the sheet level) since there's no parent sheet here to coordinate with —
 * each row's dialog is fully self-contained. Haptics are intentionally not fired here; they stay
 * owned by the caller (`MainActivity`), matching how `onToggle`/`onEdit` are already handled.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onSkip: () -> Unit = {},
    onRestore: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val rowOpacity = when {
        item.isSkipped -> 0.55f
        item.isCompleted -> 0.85f
        else -> 1f
    }
    val titleColor = if (item.isCompleted || item.isSkipped) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val accessibilityValue = when {
        item.isSkipped -> "Skipped today"
        item.isCompleted -> "Completed"
        else -> "Not completed"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Layout.RowCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .alpha(rowOpacity)
            .combinedClickable(
                enabled = !item.isSkipped,
                onClick = onToggle,
                onLongClick = onEdit,
            )
            .semantics {
                contentDescription = "${item.title}, $accessibilityValue"
                role = Role.Checkbox
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(isCompleted = item.isCompleted)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = titleColor,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
            )
            if (item.isSkipped) {
                Text(
                    text = "SKIPPED TODAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Box {
            Text(
                text = "⋮",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { menuExpanded = true }
                    .semantics {
                        contentDescription = "More options for ${item.title}"
                        role = Role.Button
                    },
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!item.isSkipped) {
                    DropdownMenuItem(
                        text = { Text("Skip for today") },
                        onClick = { menuExpanded = false; onSkip() },
                    )
                }
                if (item.isSkipped) {
                    DropdownMenuItem(
                        text = { Text("Restore") },
                        onClick = { menuExpanded = false; onRestore() },
                    )
                }
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
            title = { Text("Delete ${item.title}?") },
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
private fun Checkbox(isCompleted: Boolean) {
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
