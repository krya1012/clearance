//
//  DashboardView.swift
//  Clearance
//
//  The single screen: a top bar with the 🌅/🌌 switch, schedule, reorder, and
//  add buttons; a progress header; module quick-toggles; and the task stream.
//

import SwiftData
import SwiftUI

struct DashboardView: View {
    @Bindable var viewModel: ChecklistViewModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    @State private var editorMode: EditorMode? = nil
    @State private var showSchedule = false
    @State private var appColorScheme: ColorScheme = DashboardView.timeBasedScheme()

    private var palette: ChecklistPalette {
        Theme.palette(for: viewModel.selectedChecklist, scheme: appColorScheme)
    }

    /// Day (7 AM–9 PM) = light, night = dark.
    private static func timeBasedScheme() -> ColorScheme {
        let hour = Calendar.current.component(.hour, from: Date())
        return (hour >= 7 && hour < 21) ? .light : .dark
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            palette.background
                .ignoresSafeArea()

            VStack(spacing: 18) {
                topBar

                ProgressHeaderView(
                    checklist: viewModel.selectedChecklist,
                    progress: viewModel.progress,
                    completed: viewModel.completedCount,
                    total: viewModel.totalActiveCount,
                    palette: palette
                )
                .padding(.horizontal, Theme.Layout.screenPadding)

                ChecklistView(
                    viewModel: viewModel,
                    palette: palette,
                    onEdit: { editorMode = .edit($0) }
                )
            }
            .padding(.top, 8)
        }
        .preferredColorScheme(appColorScheme)
        .animation(
            reduceMotion ? nil : Theme.Motion.spring,
            value: viewModel.selectedChecklist
        )
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                appColorScheme = DashboardView.timeBasedScheme()
                viewModel.refresh()
            }
        }
        .sheet(item: $editorMode) { mode in
            ItemEditorView(viewModel: viewModel, mode: mode)
        }
        .sheet(isPresented: $showSchedule) {
            ScheduleEditorView(viewModel: viewModel)
        }
    }

    // MARK: - Sub-views

    private var topBar: some View {
        HStack(spacing: 12) {
            Picker("Sequence", selection: $viewModel.selectedChecklist) {
                ForEach(ChecklistType.allCases) { type in
                    Text(type.label).tag(type)
                }
            }
            .pickerStyle(.segmented)

            Button {
                showSchedule = true
            } label: {
                Image(systemName: "calendar")
                    .font(.title3)
                    .foregroundStyle(palette.tint)
            }
            .accessibilityLabel("Edit weekly schedule")

            Button {
                editorMode = .add(checklist: viewModel.selectedChecklist)
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.title3)
                    .foregroundStyle(palette.tint)
            }
            .accessibilityLabel("Add new task")
        }
        .padding(.horizontal, Theme.Layout.screenPadding)
    }

}

#Preview("Dashboard") {
    let container = try! ModelContainer(
        for: ChecklistItem.self, ActivityModule.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return DashboardView(viewModel: vm)
        .modelContainer(container)
}
