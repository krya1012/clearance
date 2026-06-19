//
//  ModuleType.swift
//  Clearance
//
//  Groups a checklist item into a "module". `.core` is always active; the
//  remaining modules are optional and can be toggled on/off by the user.
//

import Foundation

enum ModuleType: String, Codable, CaseIterable, Identifiable, Sendable {
    case core
    case gym
    case swim
    case judo

    var id: String { rawValue }

    /// Core is mandatory and cannot be disabled; everything else is opt-in.
    var isOptional: Bool { self != .core }

    /// Display order within a checklist (Core first, then optional modules).
    var sortOrder: Int {
        switch self {
        case .core: 0
        case .gym: 1
        case .swim: 2
        case .judo: 3
        }
    }

    var title: String {
        switch self {
        case .core: "Core"
        case .gym: "Gym"
        case .swim: "Swim"
        case .judo: "Judo"
        }
    }

    var emoji: String {
        switch self {
        case .core: "🎯"
        case .gym: "🏋️"
        case .swim: "🏊"
        case .judo: "🥋"
        }
    }

    var symbolName: String {
        switch self {
        case .core: "target"
        case .gym: "dumbbell.fill"
        case .swim: "figure.pool.swim"
        case .judo: "figure.martial.arts"
        }
    }

    /// Emoji + title, e.g. "🏋️ Gym".
    var label: String { "\(emoji) \(title)" }

    /// The optional modules a user can enable/disable, in display order.
    static var optionalModules: [ModuleType] {
        allCases
            .filter(\.isOptional)
            .sorted { $0.sortOrder < $1.sortOrder }
    }
}
