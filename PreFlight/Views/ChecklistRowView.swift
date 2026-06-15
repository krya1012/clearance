//
//  ChecklistRowView.swift
//  PreFlight
//
//  A single, tactile checklist row: a large checkbox target on the left and a
//  clear task title. Completed rows dim and strike through; skipped rows dim
//  further and show a "skipped" badge. Toggling is handled by the enclosing
//  Button in `ChecklistView`, so the whole row is one big tap target.
//

import SwiftUI

struct ChecklistRowView: View {
    let item: ChecklistItem
    let palette: ChecklistPalette

    var body: some View {
        HStack(spacing: 14) {
            checkbox

            VStack(alignment: .leading, spacing: 3) {
                Text(item.title)
                    .font(.body.weight(.medium))
                    .foregroundStyle(titleColor)
                    .strikethrough(item.isCompleted, color: palette.secondaryText)

                if item.isSkipped {
                    Text("SKIPPED TODAY")
                        .font(.caption2.weight(.bold))
                        .tracking(1)
                        .foregroundStyle(palette.secondaryText)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
        .background(
            RoundedRectangle(cornerRadius: Theme.Layout.rowCornerRadius, style: .continuous)
                .fill(palette.surface)
        )
        .opacity(rowOpacity)
        .contentShape(Rectangle())
        .animation(Theme.Motion.snappy, value: item.isCompleted)
        .animation(Theme.Motion.spring, value: item.isSkipped)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(item.title)
        .accessibilityValue(accessibilityValue)
        .accessibilityAddTraits(item.isCompleted ? [.isButton, .isSelected] : .isButton)
    }

    private var checkbox: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(item.isCompleted ? palette.tint : Color.clear)
                .overlay(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .strokeBorder(checkboxBorder, lineWidth: 2)
                )
                .frame(width: Theme.Layout.checkboxSize, height: Theme.Layout.checkboxSize)

            if item.isCompleted {
                Image(systemName: "checkmark")
                    .font(.system(size: 15, weight: .heavy))
                    .foregroundStyle(Color.black.opacity(0.85))
                    .transition(.scale.combined(with: .opacity))
            }
        }
    }

    private var checkboxBorder: Color {
        item.isCompleted ? palette.tint : palette.secondaryText.opacity(0.5)
    }

    private var titleColor: Color {
        (item.isCompleted || item.isSkipped) ? palette.secondaryText : palette.primaryText
    }

    private var rowOpacity: Double {
        if item.isSkipped { return 0.55 }
        return item.isCompleted ? 0.85 : 1.0
    }

    private var accessibilityValue: String {
        if item.isSkipped { return "Skipped today" }
        return item.isCompleted ? "Completed" : "Not completed"
    }
}

#Preview("Checklist Rows") {
    let palette = Theme.palette(for: .evening, scheme: .dark)
    return VStack(spacing: 10) {
        ChecklistRowView(
            item: ChecklistItem(title: "Hydrate — full glass of water", orderIndex: 0,
                                associatedModule: .core, associatedChecklist: .morning),
            palette: palette
        )
        ChecklistRowView(
            item: ChecklistItem(title: "Box breathing — 5 rounds", orderIndex: 1, isCompleted: true,
                                associatedModule: .core, associatedChecklist: .morning),
            palette: palette
        )
        ChecklistRowView(
            item: ChecklistItem(title: "Cold shower", orderIndex: 2, isSkipped: true,
                                associatedModule: .core, associatedChecklist: .morning),
            palette: palette
        )
    }
    .padding()
    .background(palette.background)
}
