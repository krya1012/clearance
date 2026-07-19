package com.krya1012.clearance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.ui.theme.Layout

private const val DefaultModuleEmoji = "🏃"

/**
 * Add (`module == null`) or edit an [ActivityModule]'s emoji + name. Mirrors iOS
 * `ModuleEditSheet` — a raw emoji text field, not a picker/grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditSheet(
    module: ActivityModule?,
    onSave: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var emoji by remember { mutableStateOf(module?.emoji ?: DefaultModuleEmoji) }
    var name by remember { mutableStateOf(module?.name ?: "") }
    val canSave = name.trim().isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = Layout.ScreenPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (module == null) "New Module" else "Edit Module",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Emoji") },
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Cycling") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = canSave,
                    onClick = {
                        val trimmedEmoji = emoji.trim().ifEmpty { DefaultModuleEmoji }
                        onSave(name.trim(), trimmedEmoji)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}
