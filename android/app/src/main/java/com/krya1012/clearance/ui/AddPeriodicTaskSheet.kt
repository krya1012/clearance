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
import com.krya1012.clearance.data.Recurrence
import com.krya1012.clearance.ui.theme.Layout

/**
 * Add a new [com.krya1012.clearance.data.PeriodicTask]. Mirrors iOS `AddPeriodicTaskView`:
 * emoji defaults to 📋, falls back to it if left blank, Add is disabled while the trimmed
 * title is empty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPeriodicTaskSheet(
    recurrence: Recurrence,
    onAdd: (title: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📋") }
    val canSave = title.trim().isNotEmpty()
    val fieldLabel = if (recurrence == Recurrence.WEEKLY) "This Week" else "This Month"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = Layout.ScreenPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New Task", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Emoji") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(fieldLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = canSave,
                    onClick = {
                        val trimmedEmoji = emoji.trim().ifEmpty { "📋" }
                        onAdd(title.trim(), trimmedEmoji)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Add") }
            }
        }
    }
}
