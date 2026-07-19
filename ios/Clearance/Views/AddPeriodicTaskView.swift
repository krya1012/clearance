//
//  AddPeriodicTaskView.swift
//  Clearance
//

import SwiftUI

struct AddPeriodicTaskView: View {
    let recurrence: Recurrence
    let viewModel: ChecklistViewModel

    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var emoji = "📋"

    private var sectionLabel: String {
        recurrence == .weekly ? "This Week" : "This Month"
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Emoji") {
                    TextField("📋", text: $emoji)
                        .font(.title2)
                }
                Section(sectionLabel) {
                    TextField("Task name", text: $title)
                        .autocorrectionDisabled(false)
                        .submitLabel(.done)
                        .onSubmit { submit() }
                }
            }
            .navigationTitle("New Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { submit() }
                        .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }

    private func submit() {
        let trimmed = title.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        let e = emoji.trimmingCharacters(in: .whitespaces).isEmpty ? "📋" : emoji
        viewModel.addPeriodicTask(title: trimmed, emoji: e, recurrence: recurrence)
        dismiss()
    }
}
