package com.krya1012.clearance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.TemplateCatalog
import com.krya1012.clearance.ui.DashboardScreen
import com.krya1012.clearance.ui.EditorMode
import com.krya1012.clearance.ui.ItemEditorSheet
import com.krya1012.clearance.ui.ModuleEditSheet
import com.krya1012.clearance.ui.ModuleManagerSheet
import com.krya1012.clearance.ui.ScheduleEditorSheet
import com.krya1012.clearance.ui.TemplateLibrarySheet
import com.krya1012.clearance.ui.theme.ClearanceTheme
import com.krya1012.clearance.util.Haptics
import com.krya1012.clearance.vm.ChecklistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ClearanceApplication
        val viewModel = ViewModelProvider(this, ChecklistViewModel.Factory(app))[ChecklistViewModel::class.java]

        setContent {
            val selectedChecklist by viewModel.selectedChecklist.collectAsState()
            val allModules by viewModel.allModules.collectAsState()
            val enabledModuleIDs by viewModel.enabledModuleIDs.collectAsState()
            val todayActivityIDs by viewModel.todayActivityIDs.collectAsState()
            val tomorrowActivityIDs by viewModel.tomorrowActivityIDs.collectAsState()
            val sections by viewModel.sections.collectAsState()
            val progress by viewModel.progress.collectAsState()
            val completedCount by viewModel.completedCount.collectAsState()
            val totalActiveCount by viewModel.totalActiveCount.collectAsState()
            val weeklySchedule by viewModel.weeklySchedule.collectAsState()
            val resetHour by viewModel.resetHour.collectAsState()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val enabledModules = allModules.filter { it.isOptional && enabledModuleIDs.contains(it.id) }
            val view = LocalView.current

            // Fires only on the false->true transition into 100% complete, mirroring iOS's
            // haptic-on-completion-transition behavior rather than buzzing on every toggle
            // while already complete (e.g. un-skipping an item that was already checked).
            val isChecklistComplete = totalActiveCount > 0 && completedCount == totalActiveCount
            var wasChecklistComplete by remember(selectedChecklist) { mutableStateOf(isChecklistComplete) }
            LaunchedEffect(selectedChecklist, isChecklistComplete) {
                if (isChecklistComplete && !wasChecklistComplete) Haptics.checklistCompleted(view)
                wasChecklistComplete = isChecklistComplete
            }

            // Sheet-visibility state — mirrors iOS DashboardView's showSchedule/editorMode
            // pattern, plus the module-manager screens nested one level deeper (opened from
            // within the schedule sheet, matching iOS's presentation hierarchy).
            var showSchedule by remember { mutableStateOf(false) }
            var showModuleManager by remember { mutableStateOf(false) }
            var showTemplateLibrary by remember { mutableStateOf(false) }
            var showModuleEditor by remember { mutableStateOf(false) }
            var moduleBeingEdited by remember { mutableStateOf<ActivityModule?>(null) }
            var editorMode by remember { mutableStateOf<EditorMode?>(null) }

            ClearanceTheme(checklistType = selectedChecklist) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        selectedChecklist = selectedChecklist,
                        onSelectChecklist = { viewModel.selectedChecklist.value = it },
                        enabledModules = enabledModules,
                        todayActivityIDs = todayActivityIDs,
                        tomorrowActivityIDs = tomorrowActivityIDs,
                        sections = sections,
                        progress = progress,
                        completedCount = completedCount,
                        totalActiveCount = totalActiveCount,
                        onToggleItem = { item ->
                            Haptics.taskToggled(view)
                            viewModel.toggle(item)
                        },
                        onEditItem = { item -> editorMode = EditorMode.Edit(item) },
                        onToggleTodayActivity = { module ->
                            Haptics.moduleToggled(view)
                            viewModel.toggleTodayActivity(module)
                        },
                        onToggleTomorrowActivity = { module ->
                            Haptics.moduleToggled(view)
                            viewModel.toggleTomorrowActivity(module)
                        },
                        onOpenSchedule = { showSchedule = true },
                        onAddTask = { editorMode = EditorMode.Add(selectedChecklist) },
                    )

                    if (showSchedule) {
                        ScheduleEditorSheet(
                            resetHour = resetHour,
                            onSetResetHour = viewModel::setResetHour,
                            onResetNow = { viewModel.resetAll() },
                            enabledModules = enabledModules,
                            weeklySchedule = weeklySchedule,
                            onToggleSchedule = viewModel::toggleScheduleActivity,
                            onManageModules = { showModuleManager = true },
                            onDismiss = { showSchedule = false },
                        )
                    }

                    if (showModuleManager) {
                        ModuleManagerSheet(
                            modules = allModules,
                            enabledModuleIDs = enabledModuleIDs,
                            hasDefaultTasks = viewModel::hasDefaultTasks,
                            onToggleEnabled = viewModel::toggleModuleEnabled,
                            onEdit = { module -> moduleBeingEdited = module; showModuleEditor = true },
                            onRestoreDefaults = viewModel::restoreDefaultTasks,
                            onDelete = viewModel::deleteModule,
                            onMoveUnlocked = viewModel::moveUnlockedModule,
                            onAddModule = { moduleBeingEdited = null; showModuleEditor = true },
                            onBrowseTemplates = { showTemplateLibrary = true },
                            onDismiss = { showModuleManager = false },
                        )
                    }

                    if (showModuleEditor) {
                        ModuleEditSheet(
                            module = moduleBeingEdited,
                            onSave = { name, emoji ->
                                val target = moduleBeingEdited
                                if (target == null) {
                                    viewModel.addModule(name, emoji)
                                } else {
                                    viewModel.updateModule(target, name, emoji)
                                }
                            },
                            onDismiss = { showModuleEditor = false },
                        )
                    }

                    if (showTemplateLibrary) {
                        TemplateLibrarySheet(
                            templates = TemplateCatalog.all,
                            isInstalled = viewModel::isTemplateInstalled,
                            onInstall = viewModel::installTemplate,
                            onDismiss = { showTemplateLibrary = false },
                        )
                    }

                    editorMode?.let { mode ->
                        ItemEditorSheet(
                            mode = mode,
                            allModules = allModules,
                            availablePhases = viewModel::availablePhases,
                            onAdd = viewModel::addItem,
                            onUpdate = viewModel::updateItem,
                            onDismiss = { editorMode = null },
                        )
                    }
                }
            }
        }
    }
}
