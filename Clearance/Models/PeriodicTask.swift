//
//  PeriodicTask.swift
//  Clearance
//

import Foundation

/// A recurring task that resets at the start of each week or month.
/// Persisted via PeriodicTaskStore (UserDefaults), not SwiftData.
struct PeriodicTask: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    var title: String
    var emoji: String
    var recurrence: Recurrence
    var isCompleted: Bool
    var completedDate: Date?

    init(id: UUID = UUID(), title: String, emoji: String = "📋", recurrence: Recurrence) {
        self.id = id
        self.title = title
        self.emoji = emoji
        self.recurrence = recurrence
        self.isCompleted = false
        self.completedDate = nil
    }
}
