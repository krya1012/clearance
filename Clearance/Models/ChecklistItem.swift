//
//  ChecklistItem.swift
//  Clearance
//
//  The single persisted entity. Adding `phase` and `phaseIndex` allows items
//  to be grouped into named sub-sections ("Systems Launch", "Refueling", …)
//  within their module section.
//
//  SwiftData handles the lightweight migration for the two new columns
//  automatically; existing rows receive the default values.
//

import Foundation
import SwiftData

@Model
final class ChecklistItem {

    /// Stable identity. Useful for diffable UI and de-duplicating seeds.
    @Attribute(.unique) var id: UUID

    /// Human-readable task, e.g. "Drink 1 full glass of clean water".
    var title: String

    /// Position of the item *within its (checklist, module, phase) group*.
    var orderIndex: Int

    /// Whether the task has been completed for the current day.
    var isCompleted: Bool

    /// Temporarily skipped for the current day (swipe action). Cleared on reset.
    var isSkipped: Bool

    /// Named sub-section within the module, e.g. "Systems Launch".
    var phase: String

    /// Ordering index for the phase itself within its (checklist, module) group.
    var phaseIndex: Int

    /// The module this item is grouped under. Persisted as a String raw value.
    var associatedModule: ModuleType

    /// The checklist (sequence) this item belongs to. Persisted as a String raw value.
    var associatedChecklist: ChecklistType

    init(
        id: UUID = UUID(),
        title: String,
        orderIndex: Int,
        isCompleted: Bool = false,
        isSkipped: Bool = false,
        phase: String = "",
        phaseIndex: Int = 0,
        associatedModule: ModuleType,
        associatedChecklist: ChecklistType
    ) {
        self.id = id
        self.title = title
        self.orderIndex = orderIndex
        self.isCompleted = isCompleted
        self.isSkipped = isSkipped
        self.phase = phase
        self.phaseIndex = phaseIndex
        self.associatedModule = associatedModule
        self.associatedChecklist = associatedChecklist
    }
}
