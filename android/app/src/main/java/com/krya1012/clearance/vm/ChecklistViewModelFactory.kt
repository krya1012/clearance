package com.krya1012.clearance.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.krya1012.clearance.ClearanceApplication
import com.krya1012.clearance.util.VibratorHaptics

/** Builds [ChecklistViewModel] from [ClearanceApplication]'s app-scoped singletons. */
class ChecklistViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = context.applicationContext as ClearanceApplication
        return ChecklistViewModel(
            moduleDao = app.database.moduleDao(),
            itemDao = app.database.itemDao(),
            scheduleStore = app.scheduleStore,
            periodicTaskStore = app.periodicTaskStore,
            haptics = VibratorHaptics(app),
        ) as T
    }
}
