//
//  ModuleManagerView.swift
//  Clearance
//

import SwiftData
import SwiftUI

struct ModuleManagerView: View {
    let viewModel: ChecklistViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var addSheetVisible = false
    @State private var moduleToEdit: ActivityModule?
    @State private var moduleToDelete: ActivityModule?
    @State private var showDeleteConfirm = false
    @State private var moduleToRestore: ActivityModule?
    @State private var showRestoreConfirm = false
    @State private var showTemplateLibrary = false

    var body: some View {
        NavigationStack {
            List {
                // ── Fixed section: Core (always on) + locked modules (toggleable) ──
                if let core = viewModel.coreModule {
                    let lockedModules = viewModel.optionalModules.filter(\.isLocked)
                    if !lockedModules.isEmpty || true {
                        Section {
                            // Core — no toggle, always on
                            HStack {
                                Text(core.label)
                                Spacer()
                                Text("Core")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.secondary)
                            }

                            // Locked optional modules (e.g. Rest) — toggleable, not editable/deletable
                            ForEach(lockedModules) { module in
                                lockedRow(module)
                                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                        if viewModel.hasDefaultTasks(for: module) {
                                            Button {
                                                moduleToRestore = module
                                                showRestoreConfirm = true
                                            } label: {
                                                Label("Restore", systemImage: "arrow.counterclockwise")
                                            }
                                            .tint(.blue)
                                        }
                                    }
                            }
                        } header: {
                            Text("Fixed")
                        }
                    }
                }

                // ── Optional section: user modules, toggleable + editable ──
                Section {
                    ForEach(viewModel.optionalModules.filter { !$0.isLocked }) { module in
                        optionalRow(module)
                            .swipeActions(edge: .leading, allowsFullSwipe: false) {
                                if viewModel.hasDefaultTasks(for: module) {
                                    Button {
                                        moduleToRestore = module
                                        showRestoreConfirm = true
                                    } label: {
                                        Label("Restore", systemImage: "arrow.counterclockwise")
                                    }
                                    .tint(.blue)
                                }
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    moduleToDelete = module
                                    showDeleteConfirm = true
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                    }
                    .onMove { viewModel.moveUnlockedModule(from: $0, to: $1) }

                    Button {
                        showTemplateLibrary = true
                    } label: {
                        HStack {
                            Text("Browse templates")
                                .foregroundStyle(Color.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(Color.secondary)
                                .imageScale(.small)
                        }
                    }
                    .buttonStyle(.plain)
                } header: {
                    Text("Optional modules")
                } footer: {
                    Text("Toggle on/off to show or hide in checklist and weekly plan. Hidden modules are not deleted.")
                }
                .environment(\.editMode, .constant(.active))
            }
            .navigationTitle("Modules")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
                ToolbarItem(placement: .primaryAction) {
                    Button { addSheetVisible = true } label: {
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
            .sheet(isPresented: $showTemplateLibrary) {
                TemplateLibraryView(viewModel: viewModel)
            }
            .confirmationDialog(
                "Delete \(moduleToDelete?.name ?? "module")?",
                isPresented: $showDeleteConfirm,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive) {
                    if let mod = moduleToDelete { viewModel.deleteModule(mod) }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("All tasks for this module will be permanently removed.")
            }
            .confirmationDialog(
                "Restore \(moduleToRestore?.name ?? "module") to defaults?",
                isPresented: $showRestoreConfirm,
                titleVisibility: .visible
            ) {
                Button("Restore default tasks", role: .destructive) {
                    if let mod = moduleToRestore { viewModel.restoreDefaultTasks(for: mod) }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This replaces all current tasks for this module with the original defaults.")
            }
        }
    }

    // MARK: - Row builders

    /// Locked module row (e.g. Rest): toggle on left, "Locked" badge on right.
    private func lockedRow(_ module: ActivityModule) -> some View {
        HStack(spacing: 12) {
            toggleButton(module)
            Text(module.label)
                .foregroundStyle(Color.primary)
            Spacer()
            Text("Locked")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
        }
        .contentShape(Rectangle())
    }

    /// Optional unlocked row: toggle on left, edit chevron on right.
    private func optionalRow(_ module: ActivityModule) -> some View {
        HStack(spacing: 12) {
            toggleButton(module)
            Text(module.label)
                .foregroundStyle(Color.primary)
            Spacer()
            Button { moduleToEdit = module } label: {
                Image(systemName: "chevron.right")
                    .foregroundStyle(Color.secondary)
                    .imageScale(.small)
            }
            .buttonStyle(.plain)
        }
        .contentShape(Rectangle())
    }

    private func toggleButton(_ module: ActivityModule) -> some View {
        let isOn = viewModel.enabledModuleIDs.contains(module.id)
        return Button {
            viewModel.toggleModuleEnabled(module)
        } label: {
            Image(systemName: isOn ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(isOn ? Color.accentColor : Color.secondary.opacity(0.4))
                .font(.system(size: 22))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(module.name)
        .accessibilityValue(isOn ? "Active" : "Hidden")
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
