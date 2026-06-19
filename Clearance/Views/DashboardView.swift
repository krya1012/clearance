//
//  DashboardView.swift
//  Clearance
//
//  The single screen: a top bar with the 🌅/🌌 switch and + button, a progress
//  header, module quick-toggles, and the grouped task stream.
//

import SwiftData
import SwiftUI

struct DashboardView: View {
    @Bindable var viewModel: ChecklistViewModel
    @Environment(\.colorScheme) private var systemScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    @State private var editorMode: EditorMode? = nil
    @State private var showSchedule = false

    private var palette: ChecklistPalette {
        Theme.palette(for: viewModel.selectedChecklist, scheme: systemScheme)
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

                activitySelectors
                    .padding(.horizontal, Theme.Layout.screenPadding)

                ChecklistView(
                    viewModel: viewModel,
                    palette: palette,
                    onEdit: { editorMode = .edit($0) }
                )
            }
            .padding(.top, 8)
        }
        .preferredColorScheme(viewModel.selectedChecklist == .evening ? .dark : nil)
        .animation(
            reduceMotion ? nil : Theme.Motion.spring,
            value: viewModel.selectedChecklist
        )
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active { viewModel.refresh() }
        }
        // Add / edit sheet — one sheet handles both modes via EditorMode.id
        .sheet(item: $editorMode) { mode in
            ItemEditorView(viewModel: viewModel, mode: mode)
        }
        // Weekly schedule editor
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

    /// Morning shows a single "Today" picker; evening splits into what you did
    /// today (drives unpacking) and what you're doing tomorrow (drives packing).
    private var enabledModulesSorted: [ModuleType] {
        ModuleType.optionalModules.filter { viewModel.enabledModules.contains($0) }
    }

    @ViewBuilder private var activitySelectors: some View {
        if viewModel.selectedChecklist == .morning {
            ActivitySelectorView(
                title: "Today",
                modules: enabledModulesSorted,
                selected: viewModel.todayActivities,
                palette: palette
            ) { viewModel.toggleTodayActivity($0) }
        } else {
            VStack(alignment: .leading, spacing: 12) {
                ActivitySelectorView(
                    title: "Done today",
                    modules: enabledModulesSorted,
                    selected: viewModel.todayActivities,
                    palette: palette
                ) { viewModel.toggleTodayActivity($0) }

                ActivitySelectorView(
                    title: "Packing for tomorrow",
                    modules: enabledModulesSorted,
                    selected: viewModel.tomorrowActivities,
                    palette: palette
                ) { viewModel.toggleTomorrowActivity($0) }
            }
        }
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
