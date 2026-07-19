//
//  TemplateCatalog.swift
//  Clearance
//

import Foundation

enum TemplateFrequency: String, CaseIterable, Identifiable, Sendable {
    case daily, weekly, monthly
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

struct TemplateEntry: Identifiable, Sendable {
    let name: String
    let emoji: String
    let activityType: ActivityType
    let frequency: TemplateFrequency
    let tagline: String
    var id: String { name }
}

enum TemplateCatalog {
    static let all: [TemplateEntry] = [
        .init(name: "Gym",     emoji: "🏋️", activityType: .sport, frequency: .weekly, tagline: "Gear-check in, gear-check out"),
        .init(name: "Swim",    emoji: "🏊",  activityType: .sport, frequency: .weekly, tagline: "Pool bag packed the night before"),
        .init(name: "Judo",    emoji: "🥋",  activityType: .sport, frequency: .weekly, tagline: "Gi ready, dojo access confirmed"),
        .init(name: "Cycling", emoji: "🚴",  activityType: .sport, frequency: .weekly, tagline: "Bike check and lights charged"),
        .init(name: "Running", emoji: "🏃",  activityType: .sport, frequency: .weekly, tagline: "Stride pack laid out the night before"),
        .init(name: "Yoga",    emoji: "🧘",  activityType: .sport, frequency: .weekly, tagline: "Mat and props ready to flow"),
        .init(name: "Walking", emoji: "🚶",  activityType: .sport, frequency: .weekly, tagline: "Active recovery — walk and stretch"),
    ]
}

extension ActivityType {
    var label: String {
        switch self {
        case .sport:   "Sport"
        case .work:    "Work"
        case .study:   "Study"
        case .leisure: "Leisure"
        }
    }

    var emoji: String {
        switch self {
        case .sport:   "🏅"
        case .work:    "💼"
        case .study:   "📚"
        case .leisure: "🎮"
        }
    }
}
