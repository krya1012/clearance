//
//  DailyLayout.swift
//  Clearance
//
//  Aggregators that compose the three template tiers into a stable daily layout.
//  Neither type is persisted — both are pure computed projections from live state.
//

import Foundation

// MARK: - Daily Layout

/// The fully-resolved layout for one day, composed of three parallel arrays
/// that are always rendered in their own fixed sections.
///
/// Section ordering in the UI:
///   1. Anchors   — constant, every day
///   2. Slots     — constant container rows, swapping missions
///   3. Periodic  — only when explicitly assigned to this day
///
struct DailyLayout {
    let day: DayOfWeek

    /// All anchor templates for this day (pre + post flight combined).
    /// Filter by `.phase` to show only morning or only evening anchors.
    let anchors: [AnchorTemplate]

    /// All weekly slots paired with their resolved mission for `day`.
    /// `mission` is `nil` when the slot has no mapping for this day
    /// → render as a greyed-out "Rest day" placeholder row.
    let slots: [(template: WeeklySlotTemplate, mission: SlotMission?)]

    /// Periodic items explicitly assigned to this day.
    /// Rendered in a separate backlog section, never mixed with anchors or slots.
    let periodicItems: [PeriodicTemplate]

    // MARK: Convenience filters

    var preFlightAnchors: [AnchorTemplate] {
        anchors.filter { $0.phase == .preFlight }
    }

    var postFlightAnchors: [AnchorTemplate] {
        anchors.filter { $0.phase == .postFlight }
    }

    var preFlightSlots: [(template: WeeklySlotTemplate, mission: SlotMission?)] {
        slots.filter { $0.template.phase == .preFlight }
    }

    var postFlightSlots: [(template: WeeklySlotTemplate, mission: SlotMission?)] {
        slots.filter { $0.template.phase == .postFlight }
    }

    /// True when at least one slot has an active mission today.
    var hasActiveMissions: Bool {
        slots.contains { $0.mission != nil }
    }
}

// MARK: - Weekly Plan

/// The top-level container for all seven daily layouts and the unassigned backlog.
struct WeeklyPlan {
    /// One resolved layout per day of the ISO week.
    let layouts: [DayOfWeek: DailyLayout]

    /// Periodic templates that have no assigned day.
    /// Shown in a global "Backlog" section outside the daily view.
    var backlogPool: [PeriodicTemplate]

    subscript(day: DayOfWeek) -> DailyLayout? { layouts[day] }

    /// Days that have at least one active slot mission scheduled.
    var activeDays: [DayOfWeek] {
        DayOfWeek.allCases.filter { layouts[$0]?.hasActiveMissions == true }
    }
}
