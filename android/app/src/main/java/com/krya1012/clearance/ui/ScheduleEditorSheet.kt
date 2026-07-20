package com.krya1012.clearance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.Weekday
import com.krya1012.clearance.ui.theme.Layout
import com.krya1012.clearance.util.Haptics

private fun hourLabel(hour: Int): String = if (hour == 0) "Midnight (12 AM)" else "$hour AM"

/**
 * The schedule sheet: entry point to module management, the auto-reset hour + "Reset now",
 * and the weekly plan grid. Mirrors iOS `ScheduleEditorView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    resetHour: Int,
    onSetResetHour: (Int) -> Unit,
    onResetNow: () -> Unit,
    enabledModules: List<ActivityModule>,
    weeklySchedule: Map<Weekday, Set<String>>,
    onToggleSchedule: (ActivityModule, Weekday) -> Unit,
    onManageModules: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    val view = LocalView.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Layout.ScreenPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Layout.SectionSpacing),
        ) {
            Text("Schedule", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MODULES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onManageModules, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage modules", modifier = Modifier.weight(1f))
                    Text(">")
                }
                Text(
                    text = "Add, rename, or delete sport modules. The weekly grid below shows all installed modules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AUTO-RESET",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = hourLabel(resetHour),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reset time") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (0..6).forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(hourLabel(hour)) },
                                onClick = { onSetResetHour(hour); expanded = false },
                            )
                        }
                    }
                }
                Button(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset now")
                }
                Text(
                    text = "Tasks are cleared automatically on first open after the set time. " +
                        "Tap \"Reset now\" to reset early.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WEEKLY PLAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (enabledModules.isEmpty()) {
                    Text(
                        text = "No modules installed. Tap \"Manage modules\" to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    WeeklyPlanGrid(
                        modules = enabledModules,
                        weeklySchedule = weeklySchedule,
                        onToggle = { module, day ->
                            Haptics.moduleToggled(view)
                            onToggleSchedule(module, day)
                        },
                    )
                }
                Text(
                    text = "Mornings grab gear; evenings unpack today and pack for tomorrow. " +
                        "Override any day from the dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset both sequences?") },
            text = { Text("This unchecks every task in both Takeoff and Landing.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    Haptics.reset(view)
                    onResetNow()
                    onDismiss()
                }) {
                    Text("Reset for tomorrow", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
