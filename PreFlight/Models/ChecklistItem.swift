//
//  ChecklistItem.swift
//  PreFlight
//
//  The single persisted entity. A checklist item belongs to exactly one
//  module (Core / Gym / Swim) and one checklist (Morning / Evening).
//

import Foundation
import SwiftData

@Model
final class ChecklistItem {

    /// Stable identity. Useful for diffable UI and de-duplicating seeds.
    @Attribute(.unique) var id: UUID

    /// Human-readable task, e.g. "Hydrate — full glass of water".
    var title: String

    /// Position of the item *within its (checklist, module) group*.
    var orderIndex: Int

    /// Whether the task has been completed for the current day.
    var isCompleted: Bool

    /// Temporarily skipped for the current day (swipe action). Cleared on reset.
    var isSkipped: Bool

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
        associatedModule: ModuleType,
        associatedChecklist: ChecklistType
    ) {
        self.id = id
        self.title = title
        self.orderIndex = orderIndex
        self.isCompleted = isCompleted
        self.isSkipped = isSkipped
        self.associatedModule = associatedModule
        self.associatedChecklist = associatedChecklist
    }
}
