//
//  ModuleManagerView.swift
//  Clearance
//
//  Sheet for adding, renaming, or deleting optional sport modules.
//  Core module is always shown at the top and cannot be edited or deleted.
//

import SwiftData
import SwiftUI

struct ModuleManagerView: View {
    let viewModel: ChecklistViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var addSheetVisible = false
    @State private var moduleToEdit: ActivityModule? = nil
    @State private var moduleToDelete: ActivityModule? = nil
    @State private var showDeleteConfirm = false

    var body: some View {
        NavigationStack {
            List {
                if let core = viewModel.coreModule {
                    Section {
                        HStack {
                            Text(core.label)
                            Spacer()
                            Text("Core")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(.secondary)
                        }
                    } header: {
                        Text("Fixed")
                    }
                }

                Section {
                    ForEach(viewModel.optionalModules) { module in
                        Button {
                            moduleToEdit = module
                        } label: {
                            HStack {
                                Text(module.label)
                                    .foregroundStyle(Color.primary)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(Color.secondary)
                                    .imageScale(.small)
                            }
                        }
                        .buttonStyle(.plain)
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                moduleToDelete = module
                                showDeleteConfirm = true
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                } header: {
                    Text("Optional modules")
                } footer: {
                    Text("Tap a module to rename it. Swipe left to delete. Drag to reorder.")
                }
            }
            .navigationTitle("Modules")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        addSheetVisible = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Add module")
                }
            }
            .sheet(isPresented: $addSheetVisible) {
                ModuleEditSheet(viewModel: viewModel, module: nil)
            }
            .sheet(item: $moduleToEdit) { module in
                ModuleEditSheet(viewModel: viewModel, module: module)
            }
            .confirmationDialog(
                "Delete \(moduleToDelete?.name ?? "module")?",
                isPresented: $showDeleteConfirm,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive) {
                    if let mod = moduleToDelete {
                        viewModel.deleteModule(mod)
                    }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("All tasks for this module will be permanently removed.")
            }
        }
    }
}

// MARK: - Add / Edit sheet

private struct ModuleEditSheet: View {
    let viewModel: ChecklistViewModel
    let module: ActivityModule?

    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var emoji: String
    @FocusState private var nameFocused: Bool

    init(viewModel: ChecklistViewModel, module: ActivityModule?) {
        self.viewModel = viewModel
        self.module = module
        _name = State(initialValue: module?.name ?? "")
        _emoji = State(initialValue: module?.emoji ?? "🏃")
    }

    private var isEditing: Bool { module != nil }
    private var canSave: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        NavigationStack {
            Form {
                Section("Emoji") {
                    TextField("🏃", text: $emoji)
                        .font(.title2)
                }
                Section("Name") {
                    TextField("e.g. Cycling", text: $name)
                        .focused($nameFocused)
                }
            }
            .navigationTitle(isEditing ? "Edit Module" : "New Module")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let e = emoji.trimmingCharacters(in: .whitespaces).isEmpty ? "🏃" : emoji
                        let n = name.trimmingCharacters(in: .whitespaces)
                        if let mod = module {
                            viewModel.updateModule(mod, name: n, emoji: e)
                        } else {
                            viewModel.addModule(name: n, emoji: e)
                        }
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(!canSave)
                }
            }
            .onAppear { if !isEditing { nameFocused = true } }
        }
    }
}
