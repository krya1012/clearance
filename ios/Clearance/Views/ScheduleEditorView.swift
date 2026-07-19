//
//  ScheduleEditorView.swift
//  Clearance
//
//  Edits the recurring weekly plan: for each weekday, which activities
//  (Gym/Swim/Judo/Cycling/Running) you do. The weekly plan is shown as a
//  compact day × module grid so all days and modules are visible at once.
//

import SwiftData
import SwiftUI

struct ScheduleEditorView: View {
    @Bindable var viewModel: ChecklistViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showResetConfirmation = false
    @State private var showModuleManager = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Button {
                        showModuleManager = true
                    } label: {
                        HStack {
                            Text("Manage modules")
                                .foregroundStyle(Color.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(Color.secondary)
                                .imageScale(.small)
                        }
                    }
                    .buttonStyle(.plain)
                } header: {
                    Text("Modules")
                } footer: {
                    Text("Add, rename, or delete sport modules. The weekly grid below shows all installed modules.")
                }

                Section {
                    Picker("Reset time", selection: $viewModel.resetHour) {
                        ForEach(0..<7) { hour in
                            Text(hourLabel(hour)).tag(hour)
                        }
                    }
                    Button("Reset now", role: .destructive) {
                        showResetConfirmation = true
                    }
                    .accessibilityLabel("Reset both sequences now")
                    .accessibilityHint("Unchecks every task in both Takeoff and Landing")
                } header: {
                    Text("Auto-reset")
                } footer: {
                    Text("Tasks are cleared automatically on first open after the set time. Tap \"Reset now\" to reset early.")
                }
                .confirmationDialog(
                    "Reset both sequences?",
                    isPresented: $showResetConfirmation,
                    titleVisibility: .visible
                ) {
                    Button("Reset for tomorrow", role: .destructive) {
                        viewModel.resetAll()
                        dismiss()
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This unchecks every task in both Takeoff and Landing.")
                }

                let weeklyModules = viewModel.enabledModules
                Section {
                    if weeklyModules.isEmpty {
                        Text("No modules installed. Tap \"Manage modules\" to add one.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    } else {
                        WeeklyPlanGrid(viewModel: viewModel, modules: weeklyModules)
                            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
                    }
                } header: {
                    Text("Weekly plan")
                } footer: {
                    Text("Mornings grab gear; evenings unpack today and pack for tomorrow. Override any day from the dashboard.")
                }
            }
            .navigationTitle("Schedule")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
            .sheet(isPresented: $showModuleManager) {
                ModuleManagerView(viewModel: viewModel)
            }
        }
    }
}

// MARK: - Weekly plan grid

private struct WeeklyPlanGrid: View {
    let viewModel: ChecklistViewModel
    let modules: [ActivityModule]

    var body: some View {
        VStack(spacing: 0) {
            headerRow
            Divider()
            ForEach(Weekday.displayOrder) { day in
                dayRow(day)
                if day.id != Weekday.displayOrder.last?.id {
                    Divider().opacity(0.35)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private var headerRow: some View {
        HStack(spacing: 0) {
            Text("")
                .frame(width: 48)
            ForEach(modules) { module in
                Text(module.emoji)
                    .font(.body)
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(module.name)
            }
        }
        .padding(.vertical, 8)
        .foregroundStyle(.secondary)
    }

    private func dayRow(_ day: Weekday) -> some View {
        HStack(spacing: 0) {
            Text(day.short)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.primary)
                .frame(width: 48, alignment: .leading)

            ForEach(modules) { module in
                let isOn = viewModel.scheduleActivities(for: day).contains(module.id)
                Button {
                    viewModel.toggleScheduleActivity(module, on: day)
                } label: {
                    Image(systemName: isOn ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundStyle(isOn ? Color.accentColor : Color.secondary.opacity(0.35))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(module.name) on \(day.label)")
                .accessibilityValue(isOn ? "Scheduled" : "Not scheduled")
                .accessibilityAddTraits(isOn ? [.isButton, .isSelected] : .isButton)
            }
        }
    }
}

// MARK: - Helpers

private func hourLabel(_ hour: Int) -> String {
    hour == 0 ? "Midnight (12 AM)" : "\(hour) AM"
}

#Preview("Schedule Editor") {
    let container = try! ModelContainer(
        for: ChecklistItem.self, ActivityModule.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return ScheduleEditorView(viewModel: vm)
}
