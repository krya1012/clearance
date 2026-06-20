//
//  ChecklistItem+Aviation.swift
//  Clearance
//
//  Bridging extensions that map existing SwiftData records and ViewModel state
//  onto the aviation domain types. No stored properties are added or changed;
//  all existing views continue to compile and behave identically.
//

import Foundation

// MARK: - ChecklistItem → aviation domain

extension ChecklistItem {

    /// Maps the item's checklist sequence to an SOPPhase.
    var sopPhase: SOPPhase {
        associatedChecklist == .morning ? .preFlight : .postFlight
    }

    /// Derives an `ActivityType` from the owning module's character.
    /// Pass the owning `ActivityModule` so Core vs optional is known.
    func activityType(in module: ActivityModule) -> ActivityType {
        // Core items are daily lifestyle anchors → leisure.
        // Optional modules are currently all sport. Extend this when
        // Work / Study templates are introduced (check module.name or a future tag).
        module.isCore ? .leisure : .sport
    }

    /// Projects this Core-module item as an `AnchorTemplate` task.
    /// Returns `nil` for items that belong to optional activity modules.
    func asTemplateTask() -> TemplateTask {
        TemplateTask(id: id, title: title)
    }
}

// MARK: - ActivityModule → aviation domain

extension ActivityModule {

    /// Builds a `SlotMission` for this module on a given day,
    /// using the provided pre-projected `TemplateTask` array.
    func asSlotMission(tasks: [TemplateTask] = []) -> SlotMission {
        SlotMission(title: name, activityType: activityType, tasks: tasks)
    }
}

// MARK: - DayOfWeek ↔ Weekday bridge

extension DayOfWeek {
    /// Converts from Calendar-aligned `Weekday` (Sunday=1) to
    /// ISO 8601 `DayOfWeek` (Monday=1).
    init(from weekday: Weekday) {
        switch weekday {
        case .monday:    self = .monday
        case .tuesday:   self = .tuesday
        case .wednesday: self = .wednesday
        case .thursday:  self = .thursday
        case .friday:    self = .friday
        case .saturday:  self = .saturday
        case .sunday:    self = .sunday
        }
    }
}

extension Weekday {
    /// ISO 8601 projection of this Calendar weekday.
    var dayOfWeek: DayOfWeek { DayOfWeek(from: self) }

    /// Converts from ISO 8601 `DayOfWeek` (Monday=1) to
    /// Calendar-aligned `Weekday` (Sunday=1). Used when querying `ScheduleStore`.
    init(dayOfWeek: DayOfWeek) {
        switch dayOfWeek {
        case .monday:    self = .monday
        case .tuesday:   self = .tuesday
        case .wednesday: self = .wednesday
        case .thursday:  self = .thursday
        case .friday:    self = .friday
        case .saturday:  self = .saturday
        case .sunday:    self = .sunday
        }
    }
}

// MARK: - ChecklistViewModel → DailyLayout / WeeklyPlan

extension ChecklistViewModel {

    /// Builds a read-only `DailyLayout` for `day` from current SwiftData state
    /// and ScheduleStore data. Does not mutate any stored state.
    func dailyLayout(for day: DayOfWeek) -> DailyLayout {
        let weekday = Weekday(dayOfWeek: day)
        let activeModuleIDs = scheduleActivities(for: weekday)

        // Anchor templates — Core module items projected as TemplateTasks
        let anchors: [AnchorTemplate] = {
            guard let core = coreModule else { return [] }
            // Group by (phase, phaseName) to preserve the existing section structure
            let coreItems = allItems
                .filter { $0.associatedModule == core.id.uuidString }
                .sorted { ($0.phaseIndex, $0.orderIndex) < ($1.phaseIndex, $1.orderIndex) }

            // Collect unique phase groups; each becomes one AnchorTemplate
            var seen = Set<String>()
            var result: [AnchorTemplate] = []
            for item in coreItems {
                let key = "\(item.associatedChecklist.rawValue)-\(item.phase)"
                if seen.insert(key).inserted {
                    let phaseTasks = coreItems
                        .filter { $0.phase == item.phase && $0.associatedChecklist == item.associatedChecklist }
                        .map { $0.asTemplateTask() }
                    result.append(AnchorTemplate(
                        id: UUID(),
                        title: item.phase,
                        phase: item.sopPhase,
                        activityType: .leisure,
                        tasks: phaseTasks
                    ))
                }
            }
            return result
        }()

        // Weekly slot templates — one slot per enabled optional module
        // The slot always appears; mission is non-nil only on scheduled days.
        let slots: [(template: WeeklySlotTemplate, mission: SlotMission?)] = enabledModules.map { module in
            let tasks = allItems
                .filter { $0.associatedModule == module.id.uuidString }
                .sorted { ($0.phaseIndex, $0.orderIndex) < ($1.phaseIndex, $1.orderIndex) }
                .map { $0.asTemplateTask() }

            let mission: SlotMission? = activeModuleIDs.contains(module.id)
                ? module.asSlotMission(tasks: tasks)
                : nil

            let template = WeeklySlotTemplate(
                id: module.id,
                slotName: "\(module.label) Slot",
                phase: .postFlight,  // activity modules are evening-anchored by default
                missionMap: mission.map { [day: $0] } ?? [:]
            )
            return (template, mission)
        }

        // Periodic items — none in the current schema; reserved for future templates
        return DailyLayout(day: day, anchors: anchors, slots: slots, periodicItems: [])
    }

    /// Synthesises the complete `WeeklyPlan` across all seven ISO days.
    func weeklyPlan() -> WeeklyPlan {
        let layouts = Dictionary(
            uniqueKeysWithValues: DayOfWeek.allCases.map { day in (day, dailyLayout(for: day)) }
        )
        return WeeklyPlan(layouts: layouts, backlogPool: [])
    }
}
