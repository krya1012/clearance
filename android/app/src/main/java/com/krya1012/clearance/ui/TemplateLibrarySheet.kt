package com.krya1012.clearance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krya1012.clearance.data.TemplateEntry
import com.krya1012.clearance.ui.theme.Layout

/**
 * Browse pre-built module templates to quickly re-add a deleted sport module (or add one on
 * first customization). Mirrors iOS `TemplateLibraryView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateLibrarySheet(
    templates: List<TemplateEntry>,
    isInstalled: (TemplateEntry) -> Boolean,
    onInstall: (TemplateEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            Text(
                text = "Template Library",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = Layout.ScreenPadding, vertical = 8.dp),
            )
            LazyColumn {
                items(templates, key = { it.id }) { entry ->
                    TemplateRow(entry = entry, installed = isInstalled(entry), onInstall = { onInstall(entry) })
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(entry: TemplateEntry, installed: Boolean, onInstall: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.ScreenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = entry.emoji, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(text = entry.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (installed) {
            Text(text = "✓", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        } else {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .clickable(onClick = onInstall)
                    .semantics {
                        contentDescription = "Install ${entry.name}"
                        role = Role.Button
                    },
            )
        }
    }
}
