package com.krya1012.clearance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * The single persisted task entity. `phase`/`phaseIndex` group items into
 * named sub-sections (e.g. "Systems Launch") within their module section.
 */
@Entity(tableName = "items")
data class ChecklistItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val orderIndex: Int,
    val isCompleted: Boolean = false,
    /** Temporarily skipped for the current day. Cleared on reset. */
    val isSkipped: Boolean = false,
    val phase: String = "",
    val phaseIndex: Int = 0,
    /** ActivityModule.id this item belongs to. */
    val associatedModule: String,
    val associatedChecklist: ChecklistType,
)
