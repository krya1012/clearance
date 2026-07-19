package com.krya1012.clearance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.ui.DashboardScreen
import com.krya1012.clearance.ui.theme.ClearanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ClearanceApplication
        setContent {
            var selectedChecklist by remember { mutableStateOf(ChecklistType.MORNING) }
            val itemsFlow = remember { app.database.itemDao().observeAll() }
            val items by itemsFlow.collectAsState(initial = emptyList())
            ClearanceTheme(checklistType = selectedChecklist) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        selectedChecklist = selectedChecklist,
                        onSelectChecklist = { selectedChecklist = it },
                        allItems = items,
                    )
                }
            }
        }
    }
}
