//
//  ChecklistViewModel.swift
//  Clearance
//
//  The single source of truth for the dashboard. Built on the modern
//  Observation framework (`@Observable`) and isolated to the main actor so it
//  can safely drive UIKit haptics and the SwiftData main context under Swift 6
//  strict concurrency.
//
//  Activity model
//  --------------
//  Each day has a set of activities (Gym/Swim/Judo) derived from a recurring
//  weekly schedule, optionally overridden per date. Those activities drive what
//  the checklist shows:
//    • Morning  — for each of *today's* activities, the "grab & final check".
//    • Evening  — for each of *today's* activities, the post-session UNLOAD;
//                 for each of *tomorrow's* activities, the PACK-for-tomorrow.
//  This lets a single evening unpack one sport (done today) while packing a
//  different sport (tomorrow) — e.g. unpack Swim, pack Gym.
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

    /// All persisted items, kept in sync with the store after each mutation.
    private(set) var allItems: [ChecklistItem] = []

    /// The recurring weekly plan: which optional modules run on each weekday.
    private(set) var weeklySchedule: [Weekday: Set<ModuleType>] = [:]

    /// Activities for today (drives morning gear-check + evening unload).
    private(set) var todayActivities: Set<ModuleType> = []

    /// Activities for tomorrow (drives evening packing).
    private(set) var tomorrowActivities: Set<ModuleType> = []

    /// Optional modules the user has marked as active (relevant). Core is always on.
    private(set) var enabledModules: Set<ModuleType> = []

    /// Hour (0–6) at which tasks are auto-reset each day. Persisted; default 3 AM.
    var resetHour: Int = 3 {
        didSet { scheduleStore.saveResetHour(resetHour) }
    }

    // MARK: - Dependencies

    @ObservationIgnored private let modelContext: ModelContext
    @ObservationIgnored private let haptics: HapticsManager
    @ObservationIgnored private let scheduleStore: ScheduleStore
    @ObservationIgnored private var overrides: [String: Set<ModuleType>] = [:]

    // MARK: - Init

    init(modelContext: ModelContext, haptics: HapticsManager = HapticsManager()) {
        self.modelContext = modelContext
        self.haptics = haptics
        let store = ScheduleStore()
        self.scheduleStore = store
        self.weeklySchedule = store.loadSchedule()
        self.overrides = store.loadOverrides()
        self.enabledModules = store.loadEnabledModules()
        self.resetHour = store.loadResetHour()
        SeedData.seedIfNeeded(in: modelContext)
        reloadItems()
        recomputeActivities()
        haptics.prepare()
    }

    // MARK: - Task roles

    /// What a task represents, derived from its checklist/module/phase. Drives
    /// which activity selection (today vs tomorrow) gates the task.
    private enum TaskRole { case anytime, gearCheck, pack, unload }

    private func role(of item: ChecklistItem) -> TaskRole {
        if item.associatedModule == .core { return .anytime }
        if item.associatedChecklist == .morning { return .gearCheck }
        // Evening module tasks: phase 0 = "Collect & Pack" (tomorrow), else unload (today).
        return item.phaseIndex == 0 ? .pack : .unload
    }

    // MARK: - Derived view state

    /// Sections for the selected checklist, gated by today's / tomorrow's
    /// activities, grouped by module → phase → items, sorted for display.
    var sections: [ChecklistSection] {
        let visible = allItems.filter { item in
            guard item.associatedChecklist == selectedChecklist else { return false }
            if item.associatedModule.isOptional && !enabledModules.contains(item.associatedModule) {
                return false
            }
            switch role(of: item) {
            case .anytime:   return true
            case .gearCheck: return todayActivities.contains(item.associatedModule)
            case .pack:      return tomorrowActivities.contains(item.associatedModule)
            case .unload:    return todayActivities.contains(item.associatedModule)
            }
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

    // MARK: - Activities (today / tomorrow)

    /// Re-derive today's / tomorrow's activities for the current date — call when
    /// the app returns to the foreground so the day rolls over correctly.
    func refresh() {
        checkAutoReset()
        recomputeActivities()
    }

    private func checkAutoReset() {
        let now = Date()
        let cal = Calendar.current
        var c = cal.dateComponents([.year, .month, .day], from: now)
        c.hour = resetHour; c.minute = 0; c.second = 0
        guard let todayThreshold = cal.date(from: c) else { return }
        let threshold = now >= todayThreshold
            ? todayThreshold
            : cal.date(byAdding: .day, value: -1, to: todayThreshold) ?? todayThreshold
        guard scheduleStore.loadLastAutoReset() < threshold else { return }
        resetAll(silent: true)
        scheduleStore.saveLastAutoReset(now)
    }

    private func recomputeActivities() {
        let today = Date()
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: today) ?? today
        todayActivities = activities(on: today)
        tomorrowActivities = activities(on: tomorrow)
    }

    private func activities(on date: Date) -> Set<ModuleType> {
        let raw: Set<ModuleType>
        if let override = overrides[scheduleStore.dateKey(for: date)] { raw = override }
        else { raw = weeklySchedule[Weekday.of(date)] ?? [] }
        return raw.intersection(enabledModules)
    }

    func toggleTodayActivity(_ module: ModuleType) {
        toggleActivity(module, on: Date())
    }

    func toggleTomorrowActivity(_ module: ModuleType) {
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        toggleActivity(module, on: tomorrow)
    }

    private func toggleActivity(_ module: ModuleType, on date: Date) {
        guard module.isOptional else { return }
        let key = scheduleStore.dateKey(for: date)
        var set = overrides[key] ?? (weeklySchedule[Weekday.of(date)] ?? [])
        if set.contains(module) { set.remove(module) } else { set.insert(module) }
        overrides[key] = set
        pruneOverrides()
        scheduleStore.saveOverrides(overrides)
        haptics.moduleToggled()
        recomputeActivities()
    }

    /// Drops override entries that aren't for yesterday/today/tomorrow so the
    /// store doesn't grow without bound.
    private func pruneOverrides() {
        let cal = Calendar.current
        let now = Date()
        let keep = Set([-1, 0, 1]
            .compactMap { cal.date(byAdding: .day, value: $0, to: now) }
            .map { scheduleStore.dateKey(for: $0) })
        overrides = overrides.filter { keep.contains($0.key) }
    }

    // MARK: - Enabled modules

    func toggleModuleEnabled(_ module: ModuleType) {
        guard module.isOptional else { return }
        if enabledModules.contains(module) { enabledModules.remove(module) }
        else { enabledModules.insert(module) }
        scheduleStore.saveEnabledModules(enabledModules)
        haptics.moduleToggled()
        recomputeActivities()
    }

    // MARK: - Weekly schedule

    func scheduleActivities(for day: Weekday) -> Set<ModuleType> {
        weeklySchedule[day] ?? []
    }

    func toggleScheduleActivity(_ module: ModuleType, on day: Weekday) {
        guard module.isOptional else { return }
        var set = weeklySchedule[day] ?? []
        if set.contains(module) { set.remove(module) } else { set.insert(module) }
        weeklySchedule[day] = set
        scheduleStore.saveSchedule(weeklySchedule)
        haptics.moduleToggled()
        recomputeActivities()
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

    func resetAll(silent: Bool = false) {
        for item in allItems {
            item.isCompleted = false
            item.isSkipped = false
        }
        save()
        if !silent { haptics.reset() }
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
}
