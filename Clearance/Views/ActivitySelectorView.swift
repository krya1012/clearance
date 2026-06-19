//
//  ActivitySelectorView.swift
//  Clearance
//
//  A labelled row of activity chips (🏋️ Gym, 🏊 Swim, 🥋 Judo). Used on the
//  dashboard to pick "today" (morning) or "done today" / "tomorrow" (evening).
//  Tapping a chip toggles that activity, which injects or removes the matching
//  gear-check / pack / unload tasks from the active list.
//

import SwiftUI

struct ActivitySelectorView: View {
    let title: String
    var modules: [ActivityModule] = []
    let selected: Set<UUID>
    let palette: ChecklistPalette
    let onToggle: (ActivityModule) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title.uppercased())
                .font(.caption2.weight(.bold))
                .tracking(1.2)
                .foregroundStyle(palette.secondaryText)

            HStack(spacing: 8) {
                ForEach(modules) { module in
                    ActivityChip(
                        module: module,
                        isOn: selected.contains(module.id),
                        palette: palette
                    ) {
                        onToggle(module)
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ActivityChip: View {
    let module: ActivityModule
    let isOn: Bool
    let palette: ChecklistPalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                Text(module.emoji)
                Text(module.name)
                    .font(.subheadline.weight(.semibold))
                Image(systemName: isOn ? "checkmark.circle.fill" : "plus.circle")
                    .imageScale(.small)
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 9)
            .foregroundStyle(isOn ? Color.black.opacity(0.85) : palette.primaryText)
            .background(
                Capsule().fill(isOn ? palette.tint : palette.surface)
            )
            .overlay(
                Capsule().strokeBorder(
                    palette.secondaryText.opacity(isOn ? 0 : 0.25),
                    lineWidth: 1
                )
            )
        }
        .buttonStyle(.plain)
        .contentShape(Capsule())
        .accessibilityLabel("\(module.name) activity")
        .accessibilityValue(isOn ? "On" : "Off")
        .accessibilityAddTraits(isOn ? [.isButton, .isSelected] : .isButton)
    }
}
