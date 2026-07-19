//
//  AviationDomain.swift
//  Clearance
//
//  The two live domain types that originated from an earlier, unused
//  aviation-scheduling design layer (DailyLayout/FlightPlanTemplates/etc.,
//  all deleted as dead code — see git history). These two are genuinely
//  load-bearing: ActivityModule.swift, PeriodicTask.swift, and
//  TemplateCatalog.swift all depend on them.
//

import Foundation

/// User-visible energy / domain category for a module.
enum ActivityType: String, Codable, CaseIterable, Sendable {
    case sport
    case work
    case study
    case leisure
}

/// Recurrence cadence for periodic (non-daily) tasks.
enum Recurrence: String, Codable, CaseIterable, Identifiable, Sendable {
    case daily
    case weekly
    case monthly

    var id: String { rawValue }
}
