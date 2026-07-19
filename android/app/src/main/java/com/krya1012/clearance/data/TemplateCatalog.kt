package com.krya1012.clearance.data

/** Cadence label shown for a catalog template entry. */
enum class TemplateFrequency {
    DAILY,
    WEEKLY,
    MONTHLY;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

data class TemplateEntry(
    val name: String,
    val emoji: String,
    val activityType: ActivityType,
    val frequency: TemplateFrequency,
    val tagline: String,
) {
    val id: String get() = name
}

/**
 * Pre-built module templates offered from "Manage modules" so a user can
 * quickly re-add a module they previously deleted (or add one from the
 * catalog on first customization). Mirrors iOS `TemplateCatalog.swift`.
 */
object TemplateCatalog {
    val all: List<TemplateEntry> = listOf(
        TemplateEntry("Gym", "🏋️", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Gear-check in, gear-check out"),
        TemplateEntry("Swim", "🏊", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Pool bag packed the night before"),
        TemplateEntry("Judo", "🥋", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Gi ready, dojo access confirmed"),
        TemplateEntry("Cycling", "🚴", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Bike check and lights charged"),
        TemplateEntry("Running", "🏃", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Stride pack laid out the night before"),
        TemplateEntry("Yoga", "🧘", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Mat and props ready to flow"),
        TemplateEntry("Walking", "🚶", ActivityType.SPORT, TemplateFrequency.WEEKLY, "Active recovery — walk and stretch"),
    )
}
