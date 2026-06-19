//
//  ScheduleEditorView.swift
//  Clearance
//
//  Edits the recurring weekly plan: for each weekday, which activities
//  (Gym/Swim/Judo) you do. The dashboard derives "today" and "tomorrow" from
//  this plan, and you can still override an individual day from the dashboard.
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
                    ForEach(viewModel.optionalModules) { module in
                        Button {
                            viewModel.toggleModuleEnabled(module)
                        } label: {
                            HStack {
                                Text(module.label)
                                    .foregroundStyle(Color.primary)
                                Spacer()
                                if viewModel.enabledModuleIDs.contains(module.id) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.accentColor)
                                        .fontWeight(.semibold)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(module.name)
                        .accessibilityValue(viewModel.enabledModuleIDs.contains(module.id) ? "Active" : "Hidden")
                        .accessibilityAddTraits(.isButton)
                    }

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
                    Text("Active modules")
                } footer: {
                    Text("Hide modules you don't use. Tap \"Manage modules\" to add, rename, or delete sport modules.")
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

                if !viewModel.enabledModules.isEmpty {
                    Section {
                        ForEach(Weekday.displayOrder) { day in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(day.label)
                                    .font(.subheadline.weight(.semibold))

                                HStack(spacing: 8) {
                                    ForEach(viewModel.enabledModules) { module in
                                        ScheduleChip(
                                            module: module,
                                            isOn: viewModel.scheduleActivities(for: day).contains(module.id)
                                        ) {
                                            viewModel.toggleScheduleActivity(module, on: day)
                                        }
                                    }
                                    Spacer(minLength: 0)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    } header: {
                        Text("Weekly plan")
                    } footer: {
                        Text("Mornings grab that day's gear. Evenings unpack what you did today and pack for tomorrow's activity. You can still override any single day from the dashboard.")
                    }
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

private func hourLabel(_ hour: Int) -> String {
    hour == 0 ? "Midnight (12 AM)" : "\(hour) AM"
}

private struct ScheduleChip: View {
    let module: ActivityModule
    let isOn: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(module.emoji)
                Text(module.name)
                    .font(.subheadline.weight(.semibold))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .foregroundStyle(isOn ? Color.white : Color.primary)
            .background(
                Capsule().fill(isOn ? Color.accentColor : Color.gray.opacity(0.18))
            )
        }
        .buttonStyle(.plain)
        .contentShape(Capsule())
        .accessibilityLabel(module.name)
        .accessibilityValue(isOn ? "Scheduled" : "Not scheduled")
        .accessibilityAddTraits(isOn ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview("Schedule Editor") {
    let container = try! ModelContainer(
        for: ChecklistItem.self, ActivityModule.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return ScheduleEditorView(viewModel: vm)
}
