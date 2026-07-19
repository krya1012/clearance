package com.krya1012.clearance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
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
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.ui.theme.Layout
import com.krya1012.clearance.util.Haptics

/** Which task the sheet is editing — the Android analog of iOS `ItemEditorView.EditorMode`. */
sealed class EditorMode {
    data class Add(val checklist: ChecklistType) : EditorMode()
    data class Edit(val item: ChecklistItem) : EditorMode()
}

/**
 * Add or edit a [ChecklistItem]. In edit mode, checklist/module are read-only once a task
 * exists (matches iOS). Mirrors iOS `ItemEditorView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    mode: EditorMode,
    allModules: List<ActivityModule>,
    availablePhases: (ActivityModule, ChecklistType) -> List<Pair<String, Int>>,
    onAdd: (title: String, phase: String, module: ActivityModule, checklist: ChecklistType) -> Unit,
    onUpdate: (item: ChecklistItem, title: String, phase: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditing = mode is EditorMode.Edit
    var title by remember(mode) {
        mutableStateOf(if (mode is EditorMode.Edit) mode.item.title else "")
    }
    var selectedChecklist by remember(mode) {
        mutableStateOf(
            when (mode) {
                is EditorMode.Add -> mode.checklist
                is EditorMode.Edit -> mode.item.associatedChecklist
            },
        )
    }
    var selectedModuleId by remember(mode) {
        mutableStateOf(
            when (mode) {
                is EditorMode.Add -> allModules.firstOrNull { it.isCore }?.id
                is EditorMode.Edit -> mode.item.associatedModule
            },
        )
    }
    var phaseName by remember(mode) {
        mutableStateOf(if (mode is EditorMode.Edit) mode.item.phase else "")
    }

    val selectedModule = allModules.firstOrNull { it.id == selectedModuleId }
    val canSave = title.trim().isNotEmpty() && (isEditing || selectedModule != null)
    val view = LocalView.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = Layout.ScreenPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEditing) "Edit Task" else "New Task",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (mode is EditorMode.Add) {
                var checklistExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = checklistExpanded, onExpandedChange = { checklistExpanded = it }) {
                    OutlinedTextField(
                        value = selectedChecklist.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Checklist") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = checklistExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = checklistExpanded, onDismissRequest = { checklistExpanded = false }) {
                        ChecklistType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = { selectedChecklist = type; checklistExpanded = false },
                            )
                        }
                    }
                }

                var moduleExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = moduleExpanded, onExpandedChange = { moduleExpanded = it }) {
                    OutlinedTextField(
                        value = selectedModule?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Module") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = moduleExpanded, onDismissRequest = { moduleExpanded = false }) {
                        allModules.forEach { module ->
                            DropdownMenuItem(
                                text = { Text(module.label) },
                                onClick = { selectedModuleId = module.id; moduleExpanded = false },
                            )
                        }
                    }
                }
            } else if (mode is EditorMode.Edit) {
                LabeledReadOnlyRow(label = "Checklist", value = mode.item.associatedChecklist.label)
                LabeledReadOnlyRow(
                    label = "Module",
                    value = allModules.firstOrNull { it.id == mode.item.associatedModule }?.label
                        ?: mode.item.associatedModule,
                )
            }

            OutlinedTextField(
                value = phaseName,
                onValueChange = { phaseName = it },
                label = { Text("Phase name (e.g. Systems Launch)") },
                modifier = Modifier.fillMaxWidth(),
            )
            val phases = selectedModule?.let { availablePhases(it, selectedChecklist) } ?: emptyList()
            if (phases.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    phases.forEach { (name, _) ->
                        PhaseChip(name = name, selected = name == phaseName, onClick = { phaseName = name })
                    }
                }
                Text(
                    text = "Tap a chip to use an existing phase, or type a new one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = canSave,
                    onClick = {
                        val trimmedTitle = title.trim()
                        Haptics.taskToggled(view)
                        when (mode) {
                            is EditorMode.Add -> selectedModule?.let {
                                onAdd(trimmedTitle, phaseName, it, selectedChecklist)
                            }
                            is EditorMode.Edit -> onUpdate(mode.item, trimmedTitle, phaseName)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun LabeledReadOnlyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PhaseChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = name,
        color = tint,
        modifier = Modifier
            .border(width = 1.dp, color = tint, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .background(if (selected) tint.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(50))
            .semantics {
                contentDescription = "Phase: $name"
                role = Role.Button
                this.selected = selected
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
    )
}
