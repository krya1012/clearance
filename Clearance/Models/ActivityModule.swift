//
//  ActivityModule.swift
//  Clearance
//
//  A user-managed activity module. One module is always Core (isCore = true)
//  and cannot be renamed or deleted. All others are optional sport modules
//  the user can create, rename, or remove.
//

import Foundation
import SwiftData

@Model
final class ActivityModule {

    @Attribute(.unique) var id: UUID
    var name: String
    var emoji: String
    var sortOrder: Int
    /// True for the one permanent Core module. False for all optional modules.
    var isCore: Bool
    /// True for modules that cannot be deleted or renamed (e.g. Rest).
    /// Independent of isCore — a locked module is still activity-gated.
    var isLocked: Bool = false
    /// Stored as String so SwiftData handles it as a basic column with a safe default.
    var activityTypeRaw: String = "sport"

    /// Domain category — sport / work / study / leisure.
    var activityType: ActivityType {
        get { ActivityType(rawValue: activityTypeRaw) ?? .sport }
        set { activityTypeRaw = newValue.rawValue }
    }

    init(id: UUID = UUID(), name: String, emoji: String, sortOrder: Int,
         isCore: Bool = false, isLocked: Bool = false, activityType: ActivityType = .sport) {
        self.id = id
        self.name = name
        self.emoji = emoji
        self.sortOrder = sortOrder
        self.isCore = isCore
        self.isLocked = isLocked
        self.activityTypeRaw = activityType.rawValue
    }

    /// Emoji + name, e.g. "🏋️ Gym".
    var label: String { "\(emoji) \(name)" }

    /// True for every module except Core.
    var isOptional: Bool { !isCore }
}
