//
//  ChecklistView.swift
//  Clearance
//
//  The scrollable task stream, grouped by module section and phase sub-header.
//  Swipe right → Edit.  Swipe left → Skip / Restore + Delete.
//  Edit-mode (pencil toggle on dashboard) → drag handles for reordering.
//

import SwiftData
import SwiftUI

struct ChecklistView: View {
    let viewModel: ChecklistViewModel
    let palette: ChecklistPalette
    var onEdit: (ChecklistItem) -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var addingPeriodic: Recurrence?

    var body: some View {
        Group {
            if viewModel.sections.isEmpty && viewModel.periodicTasks.isEmpty {
                emptyState
            } else {
                list
            }
        }
        .animation(
            reduceMotion ? nil : Theme.Motion.spring,
            value: viewModel.sections.map(\.id)
        )
        .sheet(item: $addingPeriodic) { recurrence in
            AddPeriodicTaskView(recurrence: recurrence, viewModel: viewModel)
        }
    }

    // MARK: - List

    private var list: some View {
        List {
            activitySection

            ForEach(viewModel.sections) { section in
                Section {
                    ForEach(section.phases) { phase in
                        if !phase.name.isEmpty {
                            ForEach([phase.id], id: \.self) { _ in
                                phaseSubHeader(phase)
                            }
                            .moveDisabled(true)
                        }
                        ForEach(phase.items, id: \.id) { item in
                            row(for: item)
                        }
                        .onMove { viewModel.moveItems(in: phase, from: $0, to: $1) }
                    }
                } header: {
                    sectionHeader(section.module)
                }
                .environment(\.editMode, .constant(.active))
            }

            periodicSection(title: "This Week", tasks: viewModel.weeklyTasks, recurrence: .weekly)
            periodicSection(title: "This Month", tasks: viewModel.monthlyTasks, recurrence: .monthly)

            Color.clear
                .frame(height: 88)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .listSectionSpacing(Theme.Layout.sectionSpacing)
        .scrollContentBackground(.hidden)
        .scrollIndicators(.hidden)
    }

    // MARK: - Activity selector (lives inside the list so it scrolls with tasks)

    @ViewBuilder private var activitySection: some View {
        let modules = viewModel.enabledModules
        if !modules.isEmpty {
            if viewModel.selectedChecklist == .morning {
                activityRows("TODAY", modules: modules,
                             selected: viewModel.todayActivityIDs,
                             onToggle: viewModel.toggleTodayActivity)
            } else {
                activityRows("DONE TODAY", modules: modules,
                             selected: viewModel.todayActivityIDs,
                             onToggle: viewModel.toggleTodayActivity)
                activityRows("PACKING FOR TOMORROW", modules: modules,
                             selected: viewModel.tomorrowActivityIDs,
                             onToggle: viewModel.toggleTomorrowActivity)
            }
        }
    }

    @ViewBuilder
    private func activityRows(
        _ title: String,
        modules: [ActivityModule],
        selected: Set<UUID>,
        onToggle: @escaping (ActivityModule) -> Void
    ) -> some View {
        Section {
            ForEach(modules) { module in
                Button {
                    onToggle(module)
                } label: {
                    HStack(spacing: 10) {
                        Text(module.emoji)
                            .font(.body)
                            .frame(width: 24, alignment: .center)
                        Text(module.name)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(palette.primaryText)
                        Spacer(minLength: 0)
                        Image(systemName: selected.contains(module.id)
                              ? "checkmark.circle.fill" : "circle")
                            .font(.system(size: 20))
                            .foregroundStyle(selected.contains(module.id)
                                             ? palette.tint
                                             : palette.secondaryText.opacity(0.5))
                    }
                    .padding(.vertical, 10)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(
                    top: 0, leading: Theme.Layout.screenPadding,
                    bottom: 0, trailing: Theme.Layout.screenPadding))
            }
        } header: {
            Text(title)
                .font(.caption2.weight(.bold))
                .tracking(1.2)
                .foregroundStyle(palette.secondaryText)
                .padding(.bottom, 4)
        }
    }

    // MARK: - Task row

    private func row(for item: ChecklistItem) -> some View {
        Button {
            guard !item.isSkipped else { return }
            viewModel.toggle(item)
        } label: {
            ChecklistRowView(item: item, palette: palette)
        }
        .buttonStyle(.plain)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(
            top: 5, leading: Theme.Layout.screenPadding,
            bottom: 5, trailing: Theme.Layout.screenPadding
        ))
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            Button { onEdit(item) } label: {
                Label("Edit", systemImage: "pencil")
            }
            .tint(.indigo)
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive) {
                viewModel.deleteItem(item)
            } label: {
                Label("Delete", systemImage: "trash")
            }

            if item.isSkipped {
                Button {
                    viewModel.restore(item)
                } label: {
                    Label("Restore", systemImage: "arrow.uturn.backward")
                }
                .tint(.blue)
            } else {
                Button {
                    viewModel.skip(item)
                } label: {
                    Label("Skip today", systemImage: "moon.zzz.fill")
                }
                .tint(.orange)
            }
        }
    }

    // MARK: - Periodic sections

    @ViewBuilder
    private func periodicSection(title: String, tasks: [PeriodicTask], recurrence: Recurrence) -> some View {
        Section {
            if tasks.isEmpty {
                Text("Tap + to add a task")
                    .font(.subheadline)
                    .foregroundStyle(palette.secondaryText.opacity(0.5))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(
                        top: 6, leading: Theme.Layout.screenPadding,
                        bottom: 6, trailing: Theme.Layout.screenPadding))
            } else {
                ForEach(tasks) { task in
                    periodicRow(task)
                }
            }
        } header: {
            HStack(spacing: 8) {
                Text(recurrence == .weekly ? "📅" : "🗓")
                Text(title.uppercased())
                    .font(.footnote.weight(.bold))
                    .tracking(1.5)
                Spacer()
                Button {
                    addingPeriodic = recurrence
                } label: {
                    Image(systemName: "plus")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(palette.tint)
                }
                .accessibilityLabel("Add \(title) task")
            }
            .foregroundStyle(palette.secondaryText)
            .padding(.horizontal, Theme.Layout.screenPadding)
            .padding(.vertical, 4)
        }
    }

    private func periodicRow(_ task: PeriodicTask) -> some View {
        Button {
            viewModel.togglePeriodicTask(task)
        } label: {
            HStack(spacing: 14) {
                // Square checkbox — mirrors ChecklistRowView
                ZStack {
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .fill(task.isCompleted ? palette.tint : Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 9, style: .continuous)
                                .strokeBorder(
                                    task.isCompleted ? palette.tint : palette.secondaryText.opacity(0.5),
                                    lineWidth: 2
                                )
                        )
                        .frame(width: Theme.Layout.checkboxSize, height: Theme.Layout.checkboxSize)
                    if task.isCompleted {
                        Image(systemName: "checkmark")
                            .font(.system(size: 15, weight: .heavy))
                            .foregroundStyle(Color.black.opacity(0.85))
                    }
                }

                Text(task.emoji)
                    .font(.body)

                Text(task.title)
                    .font(.body.weight(.medium))
                    .foregroundStyle(task.isCompleted ? palette.secondaryText : palette.primaryText)
                    .strikethrough(task.isCompleted, color: palette.secondaryText)

                Spacer(minLength: 0)
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 16)
            .background(
                RoundedRectangle(cornerRadius: Theme.Layout.rowCornerRadius, style: .continuous)
                    .fill(palette.surface)
            )
            .opacity(task.isCompleted ? 0.85 : 1.0)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(
            top: 5, leading: Theme.Layout.screenPadding,
            bottom: 5, trailing: Theme.Layout.screenPadding))
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                viewModel.deletePeriodicTask(task)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    // MARK: - Headers

    private func phaseSubHeader(_ phase: ChecklistPhase) -> some View {
        HStack(spacing: 8) {
            Text("Phase \(phase.phaseIndex + 1)")
                .font(.caption2.weight(.heavy))
                .foregroundStyle(palette.tint)
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(Capsule().fill(palette.tint.opacity(0.14)))

            Text(phase.name)
                .font(.caption.weight(.semibold))
                .tracking(0.4)
                .foregroundStyle(palette.secondaryText)
                .lineLimit(1)

            Spacer()
        }
        .padding(.horizontal, Theme.Layout.screenPadding)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 10, leading: 0, bottom: 2, trailing: 0))
        .accessibilityHidden(true)
    }

    private func sectionHeader(_ module: ActivityModule) -> some View {
        HStack(spacing: 8) {
            Text(module.emoji)
            Text(module.name.uppercased())
                .font(.footnote.weight(.bold))
                .tracking(1.5)
            Spacer()
        }
        .foregroundStyle(palette.secondaryText)
        .padding(.horizontal, Theme.Layout.screenPadding)
        .padding(.vertical, 4)
    }

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: 10) {
            Image(systemName: "checklist")
                .font(.system(size: 40, weight: .semibold))
            Text("Nothing scheduled")
                .font(.headline)
            Text("Enable a module above to add tasks,\nor tap + to create one.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(palette.secondaryText)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, Theme.Layout.screenPadding)
    }
}

#Preview("Checklist with phases") {
    let container = try! ModelContainer(
        for: ChecklistItem.self, ActivityModule.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return ChecklistView(viewModel: vm, palette: Theme.palette(for: .morning, scheme: .light)) { _ in }
        .background(Color(hex: 0xF4F6F8))
}
