package com.krya1012.clearance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.krya1012.clearance.ui.DashboardScreen
import com.krya1012.clearance.ui.theme.ClearanceTheme
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

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val enabledModules = allModules.filter { it.isOptional && enabledModuleIDs.contains(it.id) }

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
                        onToggleItem = viewModel::toggle,
                        onToggleTodayActivity = viewModel::toggleTodayActivity,
                        onToggleTomorrowActivity = viewModel::toggleTomorrowActivity,
                    )
                }
            }
        }
    }
}
