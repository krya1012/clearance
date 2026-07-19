package com.krya1012.clearance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistType

/**
 * Provisional flat item list — proves seeding runs end-to-end. No activity
 * gating or module sectioning yet; that lands with the real ViewModel in
 * the next milestone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    selectedChecklist: ChecklistType,
    onSelectChecklist: (ChecklistType) -> Unit,
    allItems: List<ChecklistItem> = emptyList(),
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Clearance") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(16.dp)
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

            val visible = allItems.filter { it.associatedChecklist == selectedChecklist }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { item ->
                    Text(
                        text = "${item.phase}: ${item.title}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
