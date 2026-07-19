package com.krya1012.clearance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.ui.theme.Layout
import com.krya1012.clearance.vm.ChecklistSection

/** The single dashboard screen: top bar, progress header, and the scrollable checklist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    selectedChecklist: ChecklistType,
    onSelectChecklist: (ChecklistType) -> Unit,
    enabledModules: List<ActivityModule>,
    todayActivityIDs: Set<String>,
    tomorrowActivityIDs: Set<String>,
    sections: List<ChecklistSection>,
    progress: Double,
    completedCount: Int,
    totalActiveCount: Int,
    onToggleItem: (ChecklistItem) -> Unit,
    onToggleTodayActivity: (ActivityModule) -> Unit,
    onToggleTomorrowActivity: (ActivityModule) -> Unit,
    // Schedule editing and task add land in Milestone 5; the top-bar entry points
    // already exist here so their layout position matches the iOS/plan spec.
    onOpenSchedule: () -> Unit = {},
    onAddTask: () -> Unit = {},
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TopBar(
                selectedChecklist = selectedChecklist,
                onSelectChecklist = onSelectChecklist,
                onOpenSchedule = onOpenSchedule,
                onAddTask = onAddTask,
            )

            ProgressHeaderView(
                checklist = selectedChecklist,
                progress = progress,
                completed = completedCount,
                total = totalActiveCount,
                modifier = Modifier.padding(horizontal = Layout.ScreenPadding),
            )

            if (sections.isEmpty() && enabledModules.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    activitySelectorSection(
                        checklist = selectedChecklist,
                        enabledModules = enabledModules,
                        todayActivityIDs = todayActivityIDs,
                        tomorrowActivityIDs = tomorrowActivityIDs,
                        onToggleToday = onToggleTodayActivity,
                        onToggleTomorrow = onToggleTomorrowActivity,
                    )
                    checklistSectionContent(sections = sections, onToggle = onToggleItem)
                    item(key = "fab-spacer") {
                        Box(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    selectedChecklist: ChecklistType,
    onSelectChecklist: (ChecklistType) -> Unit,
    onOpenSchedule: () -> Unit,
    onAddTask: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            ChecklistType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedChecklist == type,
                    onClick = { onSelectChecklist(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ChecklistType.entries.size),
                ) {
                    Text(type.label)
                }
            }
        }
        IconButton(onClick = onOpenSchedule) {
            Text(text = "📅") // calendar glyph — opens the schedule sheet (Milestone 5)
        }
        IconButton(onClick = onAddTask) {
            Text(text = "+", style = MaterialTheme.typography.headlineSmall) // add-task entry point (Milestone 5)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = Layout.ScreenPadding),
        ) {
            Text(text = "☑️", style = MaterialTheme.typography.displaySmall)
            Text(text = "Nothing scheduled", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Enable a module above to add tasks,\nor tap + to create one.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
