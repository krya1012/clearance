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

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    ForEach(ModuleType.optionalModules) { module in
                        Button {
                            viewModel.toggleModuleEnabled(module)
                        } label: {
                            HStack {
                                Text(module.emoji + " " + module.title)
                                    .foregroundStyle(Color.primary)
                                Spacer()
                                if viewModel.enabledModules.contains(module) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.accentColor)
                                        .fontWeight(.semibold)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(module.title)
                        .accessibilityValue(viewModel.enabledModules.contains(module) ? "Active" : "Hidden")
                        .accessibilityAddTraits(.isButton)
                    }
                } header: {
                    Text("Active modules")
                } footer: {
                    Text("Hide modules you don't use. They won't appear in the activity selector or the weekly plan below.")
                }

                if !viewModel.enabledModules.isEmpty {
                    Section {
                        ForEach(Weekday.displayOrder) { day in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(day.label)
                                    .font(.subheadline.weight(.semibold))

                                HStack(spacing: 8) {
                                    ForEach(ModuleType.optionalModules.filter { viewModel.enabledModules.contains($0) }) { module in
                                        ScheduleChip(
                                            module: module,
                                            isOn: viewModel.scheduleActivities(for: day).contains(module)
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
        }
    }
}

private struct ScheduleChip: View {
    let module: ModuleType
    let isOn: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(module.emoji)
                Text(module.title)
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
        .accessibilityLabel("\(module.title)")
        .accessibilityValue(isOn ? "Scheduled" : "Not scheduled")
        .accessibilityAddTraits(isOn ? [.isButton, .isSelected] : .isButton)
    }
}

#Preview("Schedule Editor") {
    let container = try! ModelContainer(
        for: ChecklistItem.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return ScheduleEditorView(viewModel: vm)
}
