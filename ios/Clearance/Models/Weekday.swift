//
//  Weekday.swift
//  Clearance
//
//  Days of the week, aligned with `Calendar`'s 1...7 (Sunday = 1) numbering so a
//  `Date` maps straight onto a case. Used by the weekly activity schedule.
//

import Foundation

enum Weekday: Int, CaseIterable, Identifiable, Codable, Sendable {
    case sunday = 1
    case monday
    case tuesday
    case wednesday
    case thursday
    case friday
    case saturday

    var id: Int { rawValue }

    /// The weekday for a given date in the current calendar.
    static func of(_ date: Date, calendar: Calendar = .current) -> Weekday {
        Weekday(rawValue: calendar.component(.weekday, from: date)) ?? .monday
    }

    /// Full name, e.g. "Wednesday".
    var label: String {
        switch self {
        case .sunday: "Sunday"
        case .monday: "Monday"
        case .tuesday: "Tuesday"
        case .wednesday: "Wednesday"
        case .thursday: "Thursday"
        case .friday: "Friday"
        case .saturday: "Saturday"
        }
    }

    /// Three-letter abbreviation, e.g. "Wed".
    var short: String { String(label.prefix(3)) }

    /// Cases ordered Monday → Sunday for display (most people's mental week).
    static var displayOrder: [Weekday] {
        [.monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday]
    }
}
