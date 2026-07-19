package com.krya1012.clearance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A user-managed activity module. One module is always Core (isCore = true)
 * and cannot be renamed or deleted. All others are optional modules the user
 * can create, rename, or remove.
 */
@Entity(tableName = "modules")
data class ActivityModule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val sortOrder: Int,
    val isCore: Boolean = false,
    /** Independent of isCore — a locked module cannot be renamed/deleted but is still activity-gated. */
    val isLocked: Boolean = false,
    val activityTypeRaw: String = ActivityType.SPORT.name,
) {
    val activityType: ActivityType get() = ActivityType.fromRawValue(activityTypeRaw)

    /** Emoji + name, e.g. "🏋️ Gym". */
    val label: String get() = "$emoji $name"

    /** True for every module except Core. */
    val isOptional: Boolean get() = !isCore
}
