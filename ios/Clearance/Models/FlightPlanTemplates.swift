//
//  FlightPlanTemplates.swift
//  Clearance
//
//  Three mutually exclusive template tiers. Together they guarantee a stable
//  layout row count regardless of which day of week or month is displayed.
//
//  Tier A — AnchorTemplate    : appears every single day (constant count)
//  Tier B — WeeklySlotTemplate: always renders its container; only content swaps
//  Tier C — PeriodicTemplate  : quarantined in a separate backlog section
//

import Foundation

// MARK: - A. Anchor Templates (daily fixed)

/// A non-negotiable routine that appears every day in the same position.
/// Row count is constant → layout never stretches. Maps to Core module behaviour.
struct AnchorTemplate: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    /// Section heading shown in the list, e.g. "Systems Launch".
    let title: String
    let phase: SOPPhase
    let activityType: ActivityType
    let tasks: [TemplateTask]

    init(
        id: UUID = UUID(),
        title: String,
        phase: SOPPhase,
        activityType: ActivityType,
        tasks: [TemplateTask]
    ) {
        self.id           = id
        self.title        = title
        self.phase        = phase
        self.activityType = activityType
        self.tasks        = tasks
    }
}

// MARK: - B. Weekly Slot Templates (fixed container, swapping content)

/// The content assigned to a `WeeklySlotTemplate` for one specific day.
/// When a day has no mission the slot renders a "Rest day" placeholder row
/// so the list height stays identical across the full week.
struct SlotMission: Codable, Hashable, Sendable {
    var title: String
    var activityType: ActivityType
    var tasks: [TemplateTask]
}

/// A fixed layout container whose checklist tasks swap by day of week.
/// The container row is *always* rendered; only the `SlotMission` inside it changes.
/// This permanently eliminates the layout-stretching bug caused by
/// activity modules appearing and disappearing from the list.
struct WeeklySlotTemplate: Identifiable, Hashable, Sendable {
    let id: UUID
    /// Display name of the slot itself, e.g. "Evening Movement".
    let slotName: String
    let phase: SOPPhase
    /// Day → mission. Days absent from the map render as a "Rest day" placeholder.
    let missionMap: [DayOfWeek: SlotMission]

    init(
        id: UUID = UUID(),
        slotName: String,
        phase: SOPPhase,
        missionMap: [DayOfWeek: SlotMission]
    ) {
        self.id         = id
        self.slotName   = slotName
        self.phase      = phase
        self.missionMap = missionMap
    }
}

// MARK: WeeklySlotTemplate — custom Codable
// [DayOfWeek: SlotMission] uses Int keys; JSONEncoder requires String keys.
// Bridge via rawValue string ("1"…"7").

extension WeeklySlotTemplate: Codable {
    private enum CodingKeys: String, CodingKey {
        case id, slotName, phase, missionMap
    }

    init(from decoder: any Decoder) throws {
        let c       = try decoder.container(keyedBy: CodingKeys.self)
        id          = try c.decode(UUID.self,   forKey: .id)
        slotName    = try c.decode(String.self, forKey: .slotName)
        phase       = try c.decode(SOPPhase.self, forKey: .phase)
        let raw     = try c.decode([String: SlotMission].self, forKey: .missionMap)
        missionMap  = Dictionary(uniqueKeysWithValues: raw.compactMap { key, value in
            guard let int = Int(key), let day = DayOfWeek(rawValue: int) else { return nil }
            return (day, value)
        })
    }

    func encode(to encoder: any Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id,       forKey: .id)
        try c.encode(slotName, forKey: .slotName)
        try c.encode(phase,    forKey: .phase)
        let raw = Dictionary(uniqueKeysWithValues: missionMap.map { ("\($0.key.rawValue)", $0.value) })
        try c.encode(raw, forKey: .missionMap)
    }
}

// MARK: - C. Periodic Templates (monthly / occasional, quarantined)

/// A low-frequency task that must never enter the main daily timeline.
/// Rendered only in a separate "This week / This month" backlog section.
/// `assignedDay == nil` means the item floats unscheduled in the backlog pool.
struct PeriodicTemplate: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    let title: String
    let activityType: ActivityType
    /// `.weekly` or `.monthly` — for daily cadence use `AnchorTemplate`.
    let recurrence: Recurrence
    let tasks: [TemplateTask]
    /// Explicitly assigned day. Non-nil items appear in the backlog section
    /// for that day only; never in the main anchor / slot sections.
    let assignedDay: DayOfWeek?

    var isAssigned: Bool { assignedDay != nil }

    init(
        id: UUID = UUID(),
        title: String,
        activityType: ActivityType,
        recurrence: Recurrence,
        tasks: [TemplateTask],
        assignedDay: DayOfWeek? = nil
    ) {
        self.id           = id
        self.title        = title
        self.activityType = activityType
        self.recurrence   = recurrence
        self.tasks        = tasks
        self.assignedDay  = assignedDay
    }
}
