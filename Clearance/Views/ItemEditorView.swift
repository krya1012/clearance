//
//  ItemEditorView.swift
//  PreFlight
//
//  A unified sheet for adding a new task or editing an existing one.
//  • Add mode  — full form: title, checklist, module, phase.
//  • Edit mode — title and phase only (module/checklist stay fixed).
//
//  Phase quick-fill chips show the existing phases for the chosen
//  (checklist, module) pair so the user can slot a task into the right group
//  without having to type the exact name from memory.
//

import SwiftData
import SwiftUI

// MARK: - Mode

enum EditorMode: Identifiable {
    case add(checklist: ChecklistType)
    case edit(ChecklistItem)

    var id: String {
        switch self {
        case .add(let c): return "add-\(c.rawValue)"
        case .edit(let item): return item.id.uuidString
        }
    }

    var isEditing: Bool {
        if case .edit = self { return true }
        return false
    }
}

// MARK: - View

struct ItemEditorView: View {
    let viewModel: ChecklistViewModel
    let mode: EditorMode

    @Environment(\.dismiss) private var dismiss
    @FocusState private var titleFocused: Bool

    @State private var title: String
    @State private var selectedChecklist: ChecklistType
    @State private var selectedModule: ModuleType
    @State private var phaseName: String

    init(viewModel: ChecklistViewModel, mode: EditorMode) {
        self.viewModel = viewModel
        self.mode = mode
        switch mode {
        case .add(let checklist):
            _title = State(initialValue: "")
            _selectedChecklist = State(initialValue: checklist)
            _selectedModule = State(initialValue: .core)
            _phaseName = State(initialValue: "")
        case .edit(let item):
            _title = State(initialValue: item.title)
            _selectedChecklist = State(initialValue: item.associatedChecklist)
            _selectedModule = State(initialValue: item.associatedModule)
            _phaseName = State(initialValue: item.phase)
        }
    }

    private var canSave: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private var availablePhases: [(name: String, phaseIndex: Int)] {
        viewModel.availablePhases(for: selectedModule, checklist: selectedChecklist)
    }

    var body: some View {
        NavigationStack {
            Form {
                taskSection
                if mode.isEditing {
                    readonlySequenceSection
                } else {
                    sequencePickerSection
                }
                phaseSection
            }
            .navigationTitle(mode.isEditing ? "Edit Task" : "New Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        commitSave()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(!canSave)
                }
            }
            .onAppear {
                if !mode.isEditing { titleFocused = true }
            }
        }
    }

    // MARK: - Sections

    private var taskSection: some View {
        Section("Task") {
            TextField(
                "e.g. Drink 1 full glass of clean water",
                text: $title,
                axis: .vertical
            )
            .focused($titleFocused)
            .lineLimit(1...4)
        }
    }

    private var sequencePickerSection: some View {
        Section("Sequence") {
            Picker("Checklist", selection: $selectedChecklist) {
                ForEach(ChecklistType.allCases) { type in
                    Text(type.label).tag(type)
                }
            }
            Picker("Module", selection: $selectedModule) {
                ForEach(ModuleType.allCases) { module in
                    Text(module.label).tag(module)
                }
            }
        }
    }

    private var readonlySequenceSection: some View {
        Section("Sequence") {
            if case .edit(let item) = mode {
                LabeledContent("Checklist", value: item.associatedChecklist.label)
                LabeledContent("Module", value: item.associatedModule.label)
            }
        }
    }

    private var phaseSection: some View {
        Section {
            TextField("Phase name (e.g. Systems Launch)", text: $phaseName)

            if !availablePhases.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(availablePhases, id: \.name) { phase in
                            Button {
                                phaseName = phase.name
                            } label: {
                                Text(phase.name)
                                    .font(.caption.weight(.medium))
                                    .lineLimit(1)
                            }
                            .buttonStyle(.bordered)
                            .tint(phaseName == phase.name ? .accentColor : .secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        } header: {
            Text("Phase")
        } footer: {
            Text("Tap a chip to use an existing phase, or type a new one.")
        }
    }

    // MARK: - Save

    private func commitSave() {
        let trimmed = title.trimmingCharacters(in: .whitespaces)
        switch mode {
        case .add:
            viewModel.addItem(
                title: trimmed,
                phase: phaseName,
                module: selectedModule,
                checklist: selectedChecklist
            )
        case .edit(let item):
            viewModel.updateItem(item, title: trimmed, phase: phaseName)
        }
    }
}

#Preview("Add Task") {
    let container = try! ModelContainer(
        for: ChecklistItem.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    return ItemEditorView(viewModel: vm, mode: .add(checklist: .morning))
}
