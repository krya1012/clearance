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

    init(id: UUID = UUID(), name: String, emoji: String, sortOrder: Int, isCore: Bool = false) {
        self.id = id
        self.name = name
        self.emoji = emoji
        self.sortOrder = sortOrder
        self.isCore = isCore
    }

    /// Emoji + name, e.g. "🏋️ Gym".
    var label: String { "\(emoji) \(name)" }

    /// True for every module except Core.
    var isOptional: Bool { !isCore }
}
