//
//  ScheduleStore.swift
//  Clearance
//
//  Persists the recurring weekly activity plan, per-day overrides, enabled
//  module IDs, and auto-reset settings in UserDefaults. All module references
//  are stored as UUID strings (ActivityModule.id).
//

import Foundation

@MainActor
final class ScheduleStore {

    private let defaults: UserDefaults
    private let calendar: Calendar

    // v2 keys — use UUID strings instead of enum rawValues
    private let scheduleKey       = "Clearance.weeklySchedule.v2"
    private let overridesKey      = "Clearance.activityOverrides.v2"
    private let enabledModulesKey = "Clearance.enabledModules.v2"
    // auto-reset keys (not module-specific, no change needed)
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

    // MARK: - Weekly schedule  ([Weekday: Set<UUID>])

    func loadSchedule() -> [Weekday: Set<UUID>] {
        guard
            let data = defaults.data(forKey: scheduleKey),
            let raw = try? JSONDecoder().decode([String: [String]].self, from: data)
        else { return [:] }
        var result: [Weekday: Set<UUID>] = [:]
        for (key, values) in raw {
            guard let n = Int(key), let day = Weekday(rawValue: n) else { continue }
            result[day] = Set(values.compactMap { UUID(uuidString: $0) })
        }
        return result
    }

    func saveSchedule(_ schedule: [Weekday: Set<UUID>]) {
        let raw = Dictionary(uniqueKeysWithValues: schedule.map {
            ("\($0.key.rawValue)", $0.value.map { $0.uuidString })
        })
        if let data = try? JSONEncoder().encode(raw) {
            defaults.set(data, forKey: scheduleKey)
        }
    }

    // MARK: - Per-day overrides  ([dateKey: Set<UUID>])

    func loadOverrides() -> [String: Set<UUID>] {
        guard
            let data = defaults.data(forKey: overridesKey),
            let raw = try? JSONDecoder().decode([String: [String]].self, from: data)
        else { return [:] }
        return raw.mapValues { Set($0.compactMap { UUID(uuidString: $0) }) }
    }

    func saveOverrides(_ overrides: [String: Set<UUID>]) {
        let raw = overrides.mapValues { $0.map { $0.uuidString } }
        if let data = try? JSONEncoder().encode(raw) {
            defaults.set(data, forKey: overridesKey)
        }
    }

    // MARK: - Enabled module IDs

    /// Returns `nil` when the key has never been written (first launch — caller
    /// should default to enabling all optional modules).
    func loadEnabledModuleIDs() -> Set<UUID>? {
        guard
            let data = defaults.data(forKey: enabledModulesKey),
            let raw = try? JSONDecoder().decode([String].self, from: data)
        else { return nil }
        return Set(raw.compactMap { UUID(uuidString: $0) })
    }

    func saveEnabledModuleIDs(_ ids: Set<UUID>) {
        if let data = try? JSONEncoder().encode(ids.map { $0.uuidString }) {
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
}
