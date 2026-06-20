//
//  AviationDomain.swift
//  Clearance
//
//  Core domain types for the three-tier aviation scheduling paradigm.
//  All types are compile-time constants; none touches SwiftData.
//

import Foundation

// MARK: - Activity classification

/// User-visible energy / domain category for any scheduled block.
enum ActivityType: String, Codable, CaseIterable, Sendable {
    case sport
    case work
    case study
    case leisure
}

// MARK: - Temporal anchors

/// Which chronological bookend of the day a template belongs to.
enum SOPPhase: String, Codable, CaseIterable, Sendable {
    case preFlight   // morning — mobilisation, gear-check
    case postFlight  // evening — unload, recovery, next-day prep
}

/// Recurrence cadence for periodic (non-daily) templates.
enum Recurrence: String, Codable, CaseIterable, Identifiable, Sendable {
    case daily    // use AnchorTemplate instead; kept for flexible tagging
    case weekly
    case monthly

    var id: String { rawValue }
}

// MARK: - ISO 8601 day index

/// Monday-first week index (ISO 8601). Distinct from the existing `Weekday`
/// (Calendar.component(.weekday), Sunday = 1). Bridge via `DayOfWeek(from:)`.
enum DayOfWeek: Int, Codable, CaseIterable, Identifiable, Sendable {
    case monday = 1, tuesday, wednesday, thursday, friday, saturday, sunday

    var id: Int { rawValue }

    var shortLabel: String {
        switch self {
        case .monday:    "Mon"
        case .tuesday:   "Tue"
        case .wednesday: "Wed"
        case .thursday:  "Thu"
        case .friday:    "Fri"
        case .saturday:  "Sat"
        case .sunday:    "Sun"
        }
    }

    var label: String {
        switch self {
        case .monday:    "Monday"
        case .tuesday:   "Tuesday"
        case .wednesday: "Wednesday"
        case .thursday:  "Thursday"
        case .friday:    "Friday"
        case .saturday:  "Saturday"
        case .sunday:    "Sunday"
        }
    }
}

// MARK: - Checkbox task

/// A single checkbox item inside any template tier.
struct TemplateTask: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    var title: String

    init(id: UUID = UUID(), title: String) {
        self.id    = id
        self.title = title
    }
}
