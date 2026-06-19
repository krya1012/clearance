//
//  ModuleToggleView.swift
//  Clearance
//
//  Quick-toggle chips for the optional sub-modules (🏋️ Gym, 🏊 Swim). Tapping a
//  chip immediately injects or removes that module's items from the active list.
//

import SwiftData
import SwiftUI

struct ModuleToggleView: View {
    let viewModel: ChecklistViewModel
    let palette: ChecklistPalette

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        HStack(spacing: 10) {
            ForEach(ModuleType.optionalModules) { module in
                ModuleChip(
                    module: module,
                    isOn: viewModel.isEnabled(module),
                    palette: palette
                ) {
                    withAnimation(reduceMotion ? nil : Theme.Motion.spring) {
                        viewModel.setModule(module, enabled: !viewModel.isEnabled(module))
                    }
                }
            }
            Spacer(minLength: 0)
        }
    }
}

private struct ModuleChip: View {
    let module: ModuleType
    let isOn: Bool
    let palette: ChecklistPalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Text(module.emoji)
                Text(module.title)
                    .font(.subheadline.weight(.semibold))
                Image(systemName: isOn ? "checkmark.circle.fill" : "plus.circle")
                    .imageScale(.medium)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
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
        .accessibilityLabel("\(module.title) module")
        .accessibilityValue(isOn ? "On" : "Off")
        .accessibilityHint(isOn ? "Removes these tasks from the list" : "Adds these tasks to the list")
        .accessibilityAddTraits(isOn ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview("Module Toggles") {
    let container = try! ModelContainer(
        for: ChecklistItem.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return ModuleToggleView(viewModel: vm, palette: Theme.palette(for: .evening, scheme: .dark))
        .padding()
        .background(.black)
}
