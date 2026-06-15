//
//  DashboardView.swift
//  PreFlight
//
//  The single screen: a 🌅 Takeoff / 🌌 Landing switch, a progress header, the
//  optional-module quick-toggles, the grouped task stream, and a bottom-anchored
//  "Reset for tomorrow" control.
//

import SwiftData
import SwiftUI

struct DashboardView: View {
    @Bindable var viewModel: ChecklistViewModel
    @Environment(\.colorScheme) private var systemScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showResetConfirmation = false

    private var palette: ChecklistPalette {
        Theme.palette(for: viewModel.selectedChecklist, scheme: systemScheme)
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            palette.background
                .ignoresSafeArea()

            VStack(spacing: 18) {
                sequencePicker

                ProgressHeaderView(
                    checklist: viewModel.selectedChecklist,
                    progress: viewModel.progress,
                    completed: viewModel.completedCount,
                    total: viewModel.totalActiveCount,
                    palette: palette
                )
                .padding(.horizontal, Theme.Layout.screenPadding)

                ModuleToggleView(viewModel: viewModel, palette: palette)
                    .padding(.horizontal, Theme.Layout.screenPadding)

                ChecklistView(viewModel: viewModel, palette: palette)
            }
            .padding(.top, 8)

            resetButton
                .padding(.bottom, 16)
        }
        .preferredColorScheme(viewModel.selectedChecklist == .evening ? .dark : nil)
        .animation(reduceMotion ? nil : Theme.Motion.spring, value: viewModel.selectedChecklist)
        .confirmationDialog(
            "Reset both sequences?",
            isPresented: $showResetConfirmation,
            titleVisibility: .visible
        ) {
            Button("Reset for tomorrow", role: .destructive) {
                withAnimation(reduceMotion ? nil : Theme.Motion.spring) { viewModel.resetAll() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This unchecks every task in both Takeoff and Landing.")
        }
    }

    private var sequencePicker: some View {
        Picker("Sequence", selection: $viewModel.selectedChecklist) {
            ForEach(ChecklistType.allCases) { type in
                Text(type.label).tag(type)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, Theme.Layout.screenPadding)
    }

    private var resetButton: some View {
        Button {
            showResetConfirmation = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "arrow.counterclockwise")
                Text("Reset for tomorrow")
                    .font(.subheadline.weight(.semibold))
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 14)
            .foregroundStyle(palette.primaryText)
            .background(.ultraThinMaterial, in: Capsule())
            .overlay(
                Capsule().strokeBorder(palette.secondaryText.opacity(0.25), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.25), radius: 12, y: 6)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Reset for tomorrow")
        .accessibilityHint("Clears all tasks in both sequences for the next day")
    }
}

#Preview("Dashboard") {
    let container = try! ModelContainer(
        for: ChecklistItem.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return DashboardView(viewModel: vm)
        .modelContainer(container)
}
