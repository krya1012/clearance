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
//  Each day has a set of activities (optional modules) derived from a recurring
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
    let module: ActivityModule
    let phases: [ChecklistPhase]
    var id: UUID { module.id }
    var allItems: [ChecklistItem] { phases.flatMap(\.items) }
}

// MARK: - ViewModel

@MainActor
@Observable
final class ChecklistViewModel {

    // MARK: - Observed state

    /// The sequence currently shown on the dashboard. Bindable from the UI.
    var selectedChecklist: ChecklistType = .morning

    /// All persisted modules, sorted by sortOrder.
    private(set) var allModules: [ActivityModule] = []

    /// All persisted items, kept in sync with the store after each mutation.
    private(set) var allItems: [ChecklistItem] = []

    /// IDs of optional modules scheduled for today.
    private(set) var todayActivityIDs: Set<UUID> = []

    /// IDs of optional modules scheduled for tomorrow.
    private(set) var tomorrowActivityIDs: Set<UUID> = []

    /// IDs of optional modules the user has marked as active (visible).
    private(set) var enabledModuleIDs: Set<UUID> = []

    /// Recurring weekly plan: which module IDs run on each weekday.
    private(set) var weeklySchedule: [Weekday: Set<UUID>] = [:]

    /// Hour (0–6) at which tasks are auto-reset each day. Persisted; default 3 AM.
    var resetHour: Int = 3 {
        didSet { scheduleStore.saveResetHour(resetHour) }
    }

    /// Weekly and monthly recurring tasks (laundry, budget, etc.).
    private(set) var periodicTasks: [PeriodicTask] = []

    // MARK: - Dependencies

    @ObservationIgnored private let modelContext: ModelContext
    @ObservationIgnored private let haptics: HapticsManager
    @ObservationIgnored private let scheduleStore: ScheduleStore
    @ObservationIgnored private let periodicStore: PeriodicTaskStore
    @ObservationIgnored private var overrides: [String: Set<UUID>] = [:]

    // MARK: - Init

    init(modelContext: ModelContext, haptics: HapticsManager = HapticsManager()) {
        self.modelContext = modelContext
        self.haptics = haptics
        let store = ScheduleStore()
        self.scheduleStore = store
        self.periodicStore = PeriodicTaskStore()
        self.overrides = store.loadOverrides()
        self.resetHour = store.loadResetHour()
        SeedData.seedIfNeeded(in: modelContext, scheduleStore: store)
        // Reload schedule after seeding — Rest may have been seeded with Sunday.
        self.weeklySchedule = store.loadSchedule()
        reloadModules()
        reloadItems()
        // Restore saved activation state; default to all-enabled on first launch.
        let optionalIDs = Set(allModules.filter(\.isOptional).map(\.id))
        if let saved = store.loadEnabledModuleIDs() {
            let validIDs = saved.intersection(optionalIDs)
            self.enabledModuleIDs = (validIDs.isEmpty && !optionalIDs.isEmpty) ? optionalIDs : validIDs
        } else {
            self.enabledModuleIDs = optionalIDs
        }
        store.saveEnabledModuleIDs(self.enabledModuleIDs)
        recomputeActivities()
        periodicStore.seedDefaultsIfNeeded()
        periodicTasks = periodicStore.load()
        haptics.prepare()
    }

    // MARK: - Convenience accessors

    /// All optional modules, in display order.
    var optionalModules: [ActivityModule] { allModules.filter(\.isOptional) }

    /// Optional modules the user has activated (visible in checklist + weekly plan).
    var enabledModules: [ActivityModule] { optionalModules.filter { enabledModuleIDs.contains($0.id) } }

    /// The one permanent Core module.
    var coreModule: ActivityModule? { allModules.first(where: \.isCore) }

    // MARK: - Task roles

    private enum TaskRole { case anytime, gearCheck, pack, unload }

    private func moduleForItem(_ item: ChecklistItem) -> ActivityModule? {
        guard let uuid = UUID(uuidString: item.associatedModule) else { return nil }
        return allModules.first { $0.id == uuid }
    }

    private func role(of item: ChecklistItem) -> TaskRole {
        guard let mod = moduleForItem(item) else { return .anytime }
        if mod.isCore { return .anytime }
        if item.associatedChecklist == .morning { return .gearCheck }
        return item.phaseIndex == 0 ? .pack : .unload
    }

    // MARK: - Derived view state

    var sections: [ChecklistSection] {
        let visible = allItems.filter { item in
            guard item.associatedChecklist == selectedChecklist else { return false }
            guard let mod = moduleForItem(item) else { return false }
            if mod.isOptional && !enabledModuleIDs.contains(mod.id) { return false }
            switch role(of: item) {
            case .anytime:   return true
            case .gearCheck: return todayActivityIDs.contains(mod.id)
            case .pack:      return tomorrowActivityIDs.contains(mod.id)
            case .unload:    return todayActivityIDs.contains(mod.id)
            }
        }

        let byModule = Dictionary(grouping: visible, by: \.associatedModule)
        return byModule
            .compactMap { moduleIDString, items -> ChecklistSection? in
                guard let uuid = UUID(uuidString: moduleIDString),
                      let mod = allModules.first(where: { $0.id == uuid })
                else { return nil }
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
                return ChecklistSection(module: mod, phases: phases)
            }
            .sorted { $0.module.sortOrder < $1.module.sortOrder }
    }

    private var activeItems: [ChecklistItem] {
        sections.flatMap(\.allItems).filter { !$0.isSkipped }
    }

    var totalActiveCount: Int { activeItems.count }
    var completedCount: Int { activeItems.filter(\.isCompleted).count }

    var progress: Double {
        let total = totalActiveCount
        guard total > 0 else { return 0 }
        return Double(completedCount) / Double(total)
    }

    var isCurrentChecklistComplete: Bool {
        totalActiveCount > 0 && completedCount == totalActiveCount
    }

    /// Returns all named phases for a given (module, checklist), in order.
    func availablePhases(for module: ActivityModule, checklist: ChecklistType) -> [(name: String, phaseIndex: Int)] {
        let groupItems = allItems.filter {
            $0.associatedModule == module.id.uuidString && $0.associatedChecklist == checklist
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

    func refresh() {
        checkAutoReset()
        recomputeActivities()
        periodicTasks = periodicStore.load()
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
        todayActivityIDs = activities(on: today)
        tomorrowActivityIDs = activities(on: tomorrow)
    }

    private func activities(on date: Date) -> Set<UUID> {
        let raw: Set<UUID>
        if let override = overrides[scheduleStore.dateKey(for: date)] { raw = override }
        else { raw = weeklySchedule[Weekday.of(date)] ?? [] }
        return raw.intersection(enabledModuleIDs)
    }

    func toggleTodayActivity(_ module: ActivityModule) {
        toggleActivity(module, on: Date())
    }

    func toggleTomorrowActivity(_ module: ActivityModule) {
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        toggleActivity(module, on: tomorrow)
    }

    private func toggleActivity(_ module: ActivityModule, on date: Date) {
        guard module.isOptional else { return }
        let key = scheduleStore.dateKey(for: date)
        var set = overrides[key] ?? (weeklySchedule[Weekday.of(date)] ?? [])
        if set.contains(module.id) { set.remove(module.id) } else { set.insert(module.id) }
        overrides[key] = set
        pruneOverrides()
        scheduleStore.saveOverrides(overrides)
        haptics.moduleToggled()
        recomputeActivities()
    }

    private func pruneOverrides() {
        let cal = Calendar.current
        let now = Date()
        let keep = Set([-1, 0, 1]
            .compactMap { cal.date(byAdding: .day, value: $0, to: now) }
            .map { scheduleStore.dateKey(for: $0) })
        overrides = overrides.filter { keep.contains($0.key) }
    }

    // MARK: - Enabled modules

    func toggleModuleEnabled(_ module: ActivityModule) {
        guard module.isOptional else { return }
        if enabledModuleIDs.contains(module.id) { enabledModuleIDs.remove(module.id) }
        else { enabledModuleIDs.insert(module.id) }
        scheduleStore.saveEnabledModuleIDs(enabledModuleIDs)
        haptics.moduleToggled()
        recomputeActivities()
    }

    // MARK: - Weekly schedule

    func scheduleActivities(for day: Weekday) -> Set<UUID> {
        weeklySchedule[day] ?? []
    }

    func toggleScheduleActivity(_ module: ActivityModule, on day: Weekday) {
        guard module.isOptional else { return }
        var set = weeklySchedule[day] ?? []
        if set.contains(module.id) { set.remove(module.id) } else { set.insert(module.id) }
        weeklySchedule[day] = set
        scheduleStore.saveSchedule(weeklySchedule)
        haptics.moduleToggled()
        recomputeActivities()
    }

    // MARK: - Module management

    func reloadModules() {
        let descriptor = FetchDescriptor<ActivityModule>(
            sortBy: [SortDescriptor(\.sortOrder, order: .forward)]
        )
        allModules = (try? modelContext.fetch(descriptor)) ?? []
    }

    func addModule(name: String, emoji: String, activityType: ActivityType = .sport) {
        let sortOrder = (allModules.map(\.sortOrder).max() ?? 0) + 1
        let module = ActivityModule(name: name, emoji: emoji, sortOrder: sortOrder, activityType: activityType)
        modelContext.insert(module)
        save()
        reloadModules()
        enabledModuleIDs.insert(module.id)
        scheduleStore.saveEnabledModuleIDs(enabledModuleIDs)
        haptics.moduleToggled()
    }

    func isTemplateInstalled(_ entry: TemplateEntry) -> Bool {
        allModules.contains { $0.name == entry.name }
    }

    func installTemplate(_ entry: TemplateEntry) {
        guard !isTemplateInstalled(entry) else { return }
        addModule(name: entry.name, emoji: entry.emoji, activityType: entry.activityType)
        guard let module = allModules.first(where: { $0.name == entry.name }) else { return }
        for item in SeedData.defaultItems(modules: allModules)
            where item.associatedModule == module.id.uuidString {
            modelContext.insert(item)
        }
        save()
        reloadItems()
    }

    func updateModule(_ module: ActivityModule, name: String, emoji: String) {
        module.name = name
        module.emoji = emoji
        save()
        reloadModules()
    }

    func hasDefaultTasks(for module: ActivityModule) -> Bool {
        !SeedData.defaultItems(for: module, allModules: allModules).isEmpty
    }

    func restoreDefaultTasks(for module: ActivityModule) {
        allItems
            .filter { $0.associatedModule == module.id.uuidString }
            .forEach { modelContext.delete($0) }
        SeedData.defaultItems(for: module, allModules: allModules)
            .forEach { modelContext.insert($0) }
        save()
        reloadItems()
        haptics.reset()
    }

    func deleteModule(_ module: ActivityModule) {
        guard module.isOptional && !module.isLocked else { return }
        enabledModuleIDs.remove(module.id)
        for key in weeklySchedule.keys { weeklySchedule[key]?.remove(module.id) }
        for key in overrides.keys { overrides[key]?.remove(module.id) }
        scheduleStore.saveEnabledModuleIDs(enabledModuleIDs)
        scheduleStore.saveSchedule(weeklySchedule)
        scheduleStore.saveOverrides(overrides)
        allItems
            .filter { $0.associatedModule == module.id.uuidString }
            .forEach { modelContext.delete($0) }
        modelContext.delete(module)
        save()
        reloadModules()
        reloadItems()
        recomputeActivities()
    }

    // MARK: - Item CRUD

    func addItem(title: String, phase: String, module: ActivityModule, checklist: ChecklistType) {
        let groupItems = allItems.filter {
            $0.associatedChecklist == checklist && $0.associatedModule == module.id.uuidString
        }
        let phaseIndex: Int
        if let match = groupItems.first(where: { $0.phase == phase }) {
            phaseIndex = match.phaseIndex
        } else {
            phaseIndex = (groupItems.map(\.phaseIndex).max() ?? -1) + 1
        }
        let phaseItems = groupItems.filter { $0.phase == phase }
        let orderIndex = (phaseItems.map(\.orderIndex).max() ?? -1) + 1

        let item = ChecklistItem(
            title: title,
            orderIndex: orderIndex,
            phase: phase,
            phaseIndex: phaseIndex,
            associatedModule: module.id.uuidString,
            associatedChecklist: checklist
        )
        modelContext.insert(item)
        save()
        reloadItems()
    }

    func updateItem(_ item: ChecklistItem, title: String, phase: String? = nil) {
        item.title = title
        if let phase {
            item.phase = phase
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

    func deleteItem(_ item: ChecklistItem) {
        modelContext.delete(item)
        save()
        reloadItems()
    }

    func moveItems(in phase: ChecklistPhase, from source: IndexSet, to destination: Int) {
        var sorted = phase.items
        sorted.move(fromOffsets: source, toOffset: destination)
        for (idx, item) in sorted.enumerated() { item.orderIndex = idx }
        save()
        reloadItems()
    }

    func moveModule(from source: IndexSet, to destination: Int) {
        var mutable = optionalModules
        mutable.move(fromOffsets: source, toOffset: destination)
        for (idx, module) in mutable.enumerated() { module.sortOrder = idx + 1 }
        save()
        reloadModules()
    }

    func moveUnlockedModule(from source: IndexSet, to destination: Int) {
        var mutable = optionalModules.filter { !$0.isLocked }
        mutable.move(fromOffsets: source, toOffset: destination)
        for (idx, module) in mutable.enumerated() { module.sortOrder = idx + 1 }
        save()
        reloadModules()
    }

    // MARK: - Toggle / skip / reset

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

    func skip(_ item: ChecklistItem)    { setSkipped(item, skipped: true) }
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

    // MARK: - Periodic tasks (weekly / monthly)

    var weeklyTasks: [PeriodicTask]  { periodicTasks.filter { $0.recurrence == .weekly } }
    var monthlyTasks: [PeriodicTask] { periodicTasks.filter { $0.recurrence == .monthly } }

    func addPeriodicTask(title: String, emoji: String = "📋", recurrence: Recurrence) {
        periodicTasks.append(PeriodicTask(title: title, emoji: emoji, recurrence: recurrence))
        periodicStore.save(periodicTasks)
        haptics.taskToggled(completed: false)
    }

    func togglePeriodicTask(_ task: PeriodicTask) {
        guard let i = periodicTasks.firstIndex(where: { $0.id == task.id }) else { return }
        periodicTasks[i].isCompleted.toggle()
        periodicTasks[i].completedDate = periodicTasks[i].isCompleted ? Date() : nil
        periodicStore.save(periodicTasks)
        haptics.taskToggled(completed: periodicTasks[i].isCompleted)
    }

    func deletePeriodicTask(_ task: PeriodicTask) {
        periodicTasks.removeAll { $0.id == task.id }
        periodicStore.save(periodicTasks)
        haptics.skipped()
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
