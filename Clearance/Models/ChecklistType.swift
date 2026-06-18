//
//  ChecklistType.swift
//  PreFlight
//
//  The two daily sequences. Stored on `ChecklistItem` as a String-backed,
//  Codable value so SwiftData can persist it directly.
//

import Foundation

enum ChecklistType: String, Codable, CaseIterable, Identifiable, Sendable {
    case morning
    case evening

    var id: String { rawValue }

    /// Operational callsign surfaced in the UI.
    var title: String {
        switch self {
        case .morning: "Takeoff"
        case .evening: "Landing"
        }
    }

    var subtitle: String {
        switch self {
        case .morning: "Morning sequence"
        case .evening: "Evening sequence"
        }
    }

    var emoji: String {
        switch self {
        case .morning: "🌅"
        case .evening: "🌌"
        }
    }

    /// SF Symbol used where an emoji would be too visually noisy.
    var symbolName: String {
        switch self {
        case .morning: "sunrise.fill"
        case .evening: "moon.stars.fill"
        }
    }

    /// Emoji + title, e.g. "🌅 Takeoff".
    var label: String { "\(emoji) \(title)" }
}
