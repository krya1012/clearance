package com.krya1012.clearance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** One task row: checkbox, title (dimmed/struck-through when done), SKIPPED TODAY badge. */
@Composable
fun ChecklistRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            .clickable(enabled = !item.isSkipped, onClick = onToggle)
            .semantics {
                contentDescription = "${item.title}, $accessibilityValue"
                role = Role.Checkbox
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(isCompleted = item.isCompleted)
        Spacer(modifier = Modifier.width(14.dp))
        Column {
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
