//
//  ScheduleStore.swift
//  Clearance
//
//  Persists the recurring weekly activity plan and any per-day overrides in
//  `UserDefaults`. The schedule maps each weekday to the optional modules
//  (Gym/Swim/Judo) you do that day; overrides let you tweak a specific date
//  without changing the recurring plan.
//

import Foundation

@MainActor
final class ScheduleStore {

    private let defaults: UserDefaults
    private let calendar: Calendar
    private let scheduleKey = "Clearance.weeklySchedule.v1"
    private let overridesKey = "Clearance.activityOverrides.v1"
    private let enabledModulesKey = "Clearance.enabledModules.v1"
    private let autoResetHourKey  = "Clearance.autoResetHour.v1"
    private let lastAutoResetKey  = "Clearance.lastAutoReset.v1"

    init(defaults: UserDefaults = .standard, calendar: Calendar = .current) {
        self.defaults = defaults
        self.calendar = calendar
    }

    /// Stable per-day key, e.g. "2026-06-19".
    func dateKey(for date: Date) -> String {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    // MARK: - Weekly schedule

    func loadSchedule() -> [Weekday: Set<ModuleType>] {
        guard
            let data = defaults.data(forKey: scheduleKey),
            let raw = try? JSONDecoder().decode([String: [String]].self, from: data)
        else { return Self.defaultSchedule }

        var result: [Weekday: Set<ModuleType>] = [:]
        for (key, values) in raw {
            guard let n = Int(key), let day = Weekday(rawValue: n) else { continue }
            result[day] = Set(values.compactMap(ModuleType.init(rawValue:)).filter(\.isOptional))
        }
        return result
    }

    func saveSchedule(_ schedule: [Weekday: Set<ModuleType>]) {
        let raw = Dictionary(uniqueKeysWithValues: schedule.map {
            ("\($0.key.rawValue)", $0.value.map(\.rawValue))
        })
        if let data = try? JSONEncoder().encode(raw) {
            defaults.set(data, forKey: scheduleKey)
        }
    }

    // MARK: - Per-day overrides

    func loadOverrides() -> [String: Set<ModuleType>] {
        guard
            let data = defaults.data(forKey: overridesKey),
            let raw = try? JSONDecoder().decode([String: [String]].self, from: data)
        else { return [:] }
        return raw.mapValues { Set($0.compactMap(ModuleType.init(rawValue:)).filter(\.isOptional)) }
    }

    func saveOverrides(_ overrides: [String: Set<ModuleType>]) {
        let raw = overrides.mapValues { $0.map(\.rawValue) }
        if let data = try? JSONEncoder().encode(raw) {
            defaults.set(data, forKey: overridesKey)
        }
    }

    // MARK: - Enabled modules

    func loadEnabledModules() -> Set<ModuleType> {
        guard let data = defaults.data(forKey: enabledModulesKey),
              let raw = try? JSONDecoder().decode([String].self, from: data)
        else { return Set(ModuleType.optionalModules) }
        return Set(raw.compactMap(ModuleType.init(rawValue:)).filter(\.isOptional))
    }

    func saveEnabledModules(_ modules: Set<ModuleType>) {
        if let data = try? JSONEncoder().encode(modules.map(\.rawValue)) {
            defaults.set(data, forKey: enabledModulesKey)
        }
    }

    // MARK: - Auto-reset

    func loadResetHour() -> Int {
        defaults.object(forKey: autoResetHourKey) == nil
            ? 3
            : defaults.integer(forKey: autoResetHourKey)
    }

    func saveResetHour(_ hour: Int) {
        defaults.set(hour, forKey: autoResetHourKey)
    }

    func loadLastAutoReset() -> Date {
        (defaults.object(forKey: lastAutoResetKey) as? Date) ?? .distantPast
    }

    func saveLastAutoReset(_ date: Date) {
        defaults.set(date, forKey: lastAutoResetKey)
    }

    // MARK: - Defaults

    /// A friendly starting plan that matches a common routine; fully editable.
    static var defaultSchedule: [Weekday: Set<ModuleType>] {
        [.wednesday: [.swim], .thursday: [.gym]]
    }
}
