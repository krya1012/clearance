//
//  Theme.swift
//  PreFlight
//
//  Centralised design tokens: layout metrics, motion curves, and a per-sequence
//  color palette.
//
//  Appearance strategy: the **Takeoff** (morning) sequence follows the system
//  appearance, while the **Landing** (evening) sequence is intentionally always
//  dark with a *true-black* background for comfortable low-light, nighttime use.
//

import SwiftUI

/// A fully resolved set of colors for a given sequence + color scheme.
struct ChecklistPalette {
    let background: Color
    let surface: Color
    let primaryText: Color
    let secondaryText: Color
    let tint: Color
}

enum Theme {

    enum Layout {
        static let screenPadding: CGFloat = 20
        static let cardCornerRadius: CGFloat = 18
        static let rowCornerRadius: CGFloat = 14
        static let checkboxSize: CGFloat = 28
        static let sectionSpacing: CGFloat = 22
    }

    enum Motion {
        /// Primary spring for sequence/module transitions.
        static let spring = Animation.spring(response: 0.34, dampingFraction: 0.74)
        /// Snappier spring for per-task state changes.
        static let snappy = Animation.snappy(duration: 0.26, extraBounce: 0.08)
    }

    /// Resolves the color palette for a sequence under the current appearance.
    static func palette(for checklist: ChecklistType, scheme: ColorScheme) -> ChecklistPalette {
        switch checklist {
        case .morning:
            switch scheme {
            case .dark:
                ChecklistPalette(
                    background: Color(hex: 0x0E1116),
                    surface: Color(hex: 0x181D24),
                    primaryText: Color(hex: 0xF2F5F7),
                    secondaryText: Color(hex: 0x9AA4AD),
                    tint: Color(hex: 0xFBBF24)
                )
            default:
                ChecklistPalette(
                    background: Color(hex: 0xF4F6F8),
                    surface: .white,
                    primaryText: Color(hex: 0x11181C),
                    secondaryText: Color(hex: 0x5B6770),
                    tint: Color(hex: 0xF59E0B)
                )
            }
        case .evening:
            // Always dark / true black, regardless of system appearance.
            ChecklistPalette(
                background: .black,
                surface: Color(hex: 0x101014),
                primaryText: Color(hex: 0xF5F5F7),
                secondaryText: Color(hex: 0x8E8E93),
                tint: Color(hex: 0x2DD4BF)
            )
        }
    }
}
