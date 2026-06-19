//
//  ChecklistViewModel.swift
//  Clearance
//
//  The single source of truth for the dashboard. Built on the modern
//  Observation framework (`@Observable`) and isolated to the main actor so it
//  can safely drive UIKit haptics and the SwiftData main context under Swift 6
//  strict concurrency.
//
//  Reactivity model
//  ----------------
//  `allItems` is fetched once on init and re-fetched after every mutation.
//  Everything the UI reads — `sections`, `progress`, counts — is *computed*
//  from `allItems` + `selectedChecklist` + `enabledModules`. Because those
//  three are Observation-tracked, changing the selected sequence or toggling a
//  module recomputes the visible list automatically.
//

import Foundation
import Observation
import SwiftData

// MARK: - Display types

/// One named phase within a module section, e.g. "Systems Launch".
struct ChecklistPhase: Identifiable {
    let name: String
    let phaseIndex: Int
    let items: [ChecklistItem]
    var id: String { "\(phaseIndex)-\(name)" }
}

/// One module's worth of content for the selected checklist, grouped into phases.
struct ChecklistSection: Identifiable {
    let module: ModuleType
    let phases: [ChecklistPhase]
    var id: ModuleType { module }
    var allItems: [ChecklistItem] { phases.flatMap(\.items) }
}

// MARK: - ViewModel

@MainActor
@Observable
final class ChecklistViewModel {

    // MARK: - Observed state

    /// The sequence currently shown on the dashboard. Bindable from the UI.
    var selectedChecklist: ChecklistType = .morning

    /// Modules currently active. `.core` is always present.
    private(set) var enabledModules: Set<ModuleType>

    /// All persisted items, kept in sync with the store after each mutation.
    private(set) var allItems: [ChecklistItem] = []

    // MARK: - Dependencies

    @ObservationIgnored private let modelContext: ModelContext
    @ObservationIgnored private let haptics: HapticsManager

    private static let enabledModulesDefaultsKey = "Clearance.enabledModules.v1"

    // MARK: - Init

    init(modelContext: ModelContext, haptics: HapticsManager = HapticsManager()) {
        self.modelContext = modelContext
        self.haptics = haptics
        self.enabledModules = Self.loadEnabledModules()
        SeedData.seedIfNeeded(in: modelContext)
        reloadItems()
        haptics.prepare()
    }

    // MARK: - Derived view state

    /// Sections for the selected checklist, filtered by enabled modules,
    /// grouped by module → phase → items, sorted for display.
    var sections: [ChecklistSection] {
        let visible = allItems.filter {
            $0.associatedChecklist == selectedChecklist
                && enabledModules.contains($0.associatedModule)
        }

        let byModule = Dictionary(grouping: visible, by: \.associatedModule)
        return byModule
            .map { module, items -> ChecklistSection in
                let byPhase = Dictionary(grouping: items, by: \.phase)
                let phases = byPhase
                    .map { phaseName, phaseItems -> ChecklistPhase in
                        let phaseIndex = phaseItems.first?.phaseIndex ?? 0
                        return ChecklistPhase(
                            name: phaseName,
                            phaseIndex: phaseIndex,
                            items: phaseItems.sorted { $0.orderIndex < $1.orderIndex }
                        )
                    }
                    .sorted { $0.phaseIndex < $1.phaseIndex }
                return ChecklistSection(module: module, phases: phases)
            }
            .sorted { $0.module.sortOrder < $1.module.sortOrder }
    }

    /// Active (non-skipped) items in the current view — the basis for progress.
    private var activeItems: [ChecklistItem] {
        sections.flatMap(\.allItems).filter { !$0.isSkipped }
    }

    var totalActiveCount: Int { activeItems.count }
    var completedCount: Int { activeItems.filter(\.isCompleted).count }

    /// Completion ratio (0...1) for the current view; skipped items excluded.
    var progress: Double {
        let total = totalActiveCount
        guard total > 0 else { return 0 }
        return Double(completedCount) / Double(total)
    }

    /// Whether every active item in the current view is complete.
    var isCurrentChecklistComplete: Bool {
        totalActiveCount > 0 && completedCount == totalActiveCount
    }

    func isEnabled(_ module: ModuleType) -> Bool {
        enabledModules.contains(module)
    }

    /// Returns all named phases for a given (module, checklist), in order.
    /// Used by `ItemEditorView` to offer quick-fill suggestions.
    func availablePhases(for module: ModuleType, checklist: ChecklistType) -> [(name: String, phaseIndex: Int)] {
        let groupItems = allItems.filter {
            $0.associatedModule == module && $0.associatedChecklist == checklist
        }
        var seen = Set<String>()
        var result: [(name: String, phaseIndex: Int)] = []
        for item in groupItems.sorted(by: { $0.phaseIndex < $1.phaseIndex }) {
            if !seen.contains(item.phase) {
                seen.insert(item.phase)
                result.append((item.phase, item.phaseIndex))
            }
        }
        return result
    }

    // MARK: - CRUD

    /// Appends a new task into the correct (checklist, module, phase) slot.
    func addItem(
        title: String,
        phase: String,
        module: ModuleType,
        checklist: ChecklistType
    ) {
        let groupItems = allItems.filter {
            $0.associatedChecklist == checklist && $0.associatedModule == module
        }

        // Reuse an existing phase's index or assign the next available index.
        let phaseIndex: Int
        if let match = groupItems.first(where: { $0.phase == phase }) {
            phaseIndex = match.phaseIndex
        } else {
            phaseIndex = (groupItems.map(\.phaseIndex).max() ?? -1) + 1
        }

        // Append at the end of its phase.
        let phaseItems = groupItems.filter { $0.phase == phase }
        let orderIndex = (phaseItems.map(\.orderIndex).max() ?? -1) + 1

        let item = ChecklistItem(
            title: title,
            orderIndex: orderIndex,
            phase: phase,
            phaseIndex: phaseIndex,
            associatedModule: module,
            associatedChecklist: checklist
        )
        modelContext.insert(item)
        save()
        reloadItems()
    }

    /// Updates the mutable fields of an existing task.
    func updateItem(_ item: ChecklistItem, title: String, phase: String? = nil) {
        item.title = title
        if let phase {
            item.phase = phase
            // Sync phaseIndex to match any existing phase with that name.
            let groupItems = allItems.filter {
                $0.associatedChecklist == item.associatedChecklist
                    && $0.associatedModule == item.associatedModule
                    && $0.id != item.id
            }
            if let match = groupItems.first(where: { $0.phase == phase }) {
                item.phaseIndex = match.phaseIndex
            }
        }
        save()
        reloadItems()
    }

    /// Permanently removes a task from the store.
    func deleteItem(_ item: ChecklistItem) {
        modelContext.delete(item)
        save()
        reloadItems()
    }

    // MARK: - Toggle / skip / reset

    /// Toggles completion for a task and fires the appropriate haptic.
    func toggle(_ item: ChecklistItem) {
        let wasComplete = isCurrentChecklistComplete
        item.isCompleted.toggle()
        if item.isCompleted { item.isSkipped = false }
        save()
        haptics.taskToggled(completed: item.isCompleted)
        reloadItems()

        if !wasComplete && isCurrentChecklistComplete {
            haptics.checklistCompleted()
        }
    }

    func setSkipped(_ item: ChecklistItem, skipped: Bool) {
        item.isSkipped = skipped
        if skipped { item.isCompleted = false }
        save()
        haptics.skipped()
        reloadItems()
    }

    func skip(_ item: ChecklistItem) { setSkipped(item, skipped: true) }
    func restore(_ item: ChecklistItem) { setSkipped(item, skipped: false) }

    func setModule(_ module: ModuleType, enabled: Bool) {
        guard module.isOptional else { return }
        if enabled { enabledModules.insert(module) } else { enabledModules.remove(module) }
        persistEnabledModules()
        haptics.moduleToggled()
    }

    func resetAll() {
        for item in allItems {
            item.isCompleted = false
            item.isSkipped = false
        }
        save()
        haptics.reset()
        reloadItems()
    }

    // MARK: - Persistence plumbing

    func reloadItems() {
        let descriptor = FetchDescriptor<ChecklistItem>(
            sortBy: [SortDescriptor(\.orderIndex, order: .forward)]
        )
        allItems = (try? modelContext.fetch(descriptor)) ?? []
    }

    private func save() {
        do {
            try modelContext.save()
        } catch {
            assertionFailure("Clearance: failed to save model context — \(error)")
        }
    }

    private func persistEnabledModules() {
        UserDefaults.standard.set(
            enabledModules.map(\.rawValue),
            forKey: Self.enabledModulesDefaultsKey
        )
    }

    private static func loadEnabledModules() -> Set<ModuleType> {
        guard
            let raw = UserDefaults.standard.array(forKey: enabledModulesDefaultsKey) as? [String]
        else {
            return [.core]
        }
        var modules = Set(raw.compactMap(ModuleType.init(rawValue:)))
        modules.insert(.core)
        return modules
    }
}
