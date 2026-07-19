package com.krya1012.clearance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Android analog of iOS `ChecklistType` — replaced by the real shared
 * data model once the persistence layer lands.
 */
enum class ChecklistType(val label: String) {
    MORNING("🌅 Takeoff"),
    EVENING("🌌 Landing"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    selectedChecklist: ChecklistType,
    onSelectChecklist: (ChecklistType) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Clearance") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                ChecklistType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedChecklist == type,
                        onClick = { onSelectChecklist(type) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ChecklistType.entries.size
                        )
                    ) {
                        Text(type.label)
                    }
                }
            }
        }
    }
}
