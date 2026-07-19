//
//  Extensions.swift
//  Clearance
//
//  Small, focused helpers shared across the UI layer.
//

import SwiftUI

extension Color {
    /// Creates a color from a 24-bit RGB hex literal, e.g. `Color(hex: 0xF59E0B)`.
    init(hex: UInt, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}
