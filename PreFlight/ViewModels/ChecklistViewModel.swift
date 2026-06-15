//
//  ChecklistViewModel.swift
//  PreFlight
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
//  module recomputes the visible list automatically, with no manual plumbing.
//

import Foundation
import Observation
import SwiftData

/// A display group: one module's items within the selected checklist.
struct ChecklistSection: Identifiable {
    let module: ModuleType
    let items: [ChecklistItem]
    var id: ModuleType { module }
}

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

    private static let enabledModulesDefaultsKey = "PreFlight.enabledModules.v1"

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
    /// grouped by module and sorted for display.
    var sections: [ChecklistSection] {
        let visible = allItems.filter {
            $0.associatedChecklist == selectedChecklist
                && enabledModules.contains($0.associatedModule)
        }
        return Dictionary(grouping: visible, by: \.associatedModule)
            .map { module, items in
                ChecklistSection(
                    module: module,
                    items: items.sorted { $0.orderIndex < $1.orderIndex }
                )
            }
            .sorted { $0.module.sortOrder < $1.module.sortOrder }
    }

    /// Active (non-skipped) items in the current view — the basis for progress.
    private var activeItems: [ChecklistItem] {
        sections.flatMap(\.items).filter { !$0.isSkipped }
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

    // MARK: - Mutations

    /// Toggles completion for a task, firing a subtle haptic and a celebratory
    /// cue if this completes the whole sequence.
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

    /// Temporarily skip (or restore) an item for the current day.
    func setSkipped(_ item: ChecklistItem, skipped: Bool) {
        item.isSkipped = skipped
        if skipped { item.isCompleted = false }
        save()
        haptics.skipped()
        reloadItems()
    }

    func skip(_ item: ChecklistItem) { setSkipped(item, skipped: true) }
    func restore(_ item: ChecklistItem) { setSkipped(item, skipped: false) }

    /// Enable/disable an optional module. `.core` is fixed on and ignored here.
    /// No DB write is needed — `sections` recompute from `enabledModules`.
    func setModule(_ module: ModuleType, enabled: Bool) {
        guard module.isOptional else { return }
        if enabled {
            enabledModules.insert(module)
        } else {
            enabledModules.remove(module)
        }
        persistEnabledModules()
        haptics.moduleToggled()
    }

    /// Clears completion + skip state for **both** sequences, ready for the
    /// next day. This is the global reset.
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

    /// Re-fetches every item from the store (sorted by `orderIndex`) and
    /// republishes `allItems`, forcing all derived state to recompute.
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
            assertionFailure("PreFlight: failed to save model context — \(error)")
        }
    }

    private func persistEnabledModules() {
        let raw = enabledModules.map(\.rawValue)
        UserDefaults.standard.set(raw, forKey: Self.enabledModulesDefaultsKey)
    }

    private static func loadEnabledModules() -> Set<ModuleType> {
        guard
            let raw = UserDefaults.standard.array(forKey: enabledModulesDefaultsKey) as? [String]
        else {
            return [.core] // first launch: only Core is active
        }
        var modules = Set(raw.compactMap(ModuleType.init(rawValue:)))
        modules.insert(.core) // Core is always active
        return modules
    }
}
