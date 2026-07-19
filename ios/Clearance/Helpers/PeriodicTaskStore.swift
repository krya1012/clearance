//
//  PeriodicTaskStore.swift
//  Clearance
//
//  UserDefaults persistence for PeriodicTask, following the ScheduleStore pattern.
//  Completions auto-reset when load() is called after a week or month boundary.
//

import Foundation

@MainActor
final class PeriodicTaskStore {

    private let defaults: UserDefaults
    private static let key = "Clearance.periodicTasks.v1"
    private static let seedVersionKey = "Clearance.periodicSeedVersion.v1"
    private static let currentSeedVersion = 2

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func seedDefaultsIfNeeded() {
        let stored = defaults.integer(forKey: Self.seedVersionKey)
        guard stored < Self.currentSeedVersion else { return }
        let existing = load()
        let existingTitles = Set(existing.map(\.title))
        let seeds: [PeriodicTask] = [
            PeriodicTask(title: "Laundry",  emoji: "🧺", recurrence: .weekly),
            PeriodicTask(title: "Shopping", emoji: "🛒", recurrence: .weekly),
            PeriodicTask(title: "Budget",   emoji: "💰", recurrence: .monthly),
            PeriodicTask(title: "Post",     emoji: "📬", recurrence: .monthly),
        ]
        let toAdd = seeds.filter { !existingTitles.contains($0.title) }
        if !toAdd.isEmpty { save(existing + toAdd) }
        defaults.set(Self.currentSeedVersion, forKey: Self.seedVersionKey)
    }

    func load() -> [PeriodicTask] {
        guard
            let data = defaults.data(forKey: Self.key),
            var tasks = try? JSONDecoder().decode([PeriodicTask].self, from: data)
        else { return [] }

        let cal = Calendar.current
        let now = Date()
        let currentWeek  = cal.component(.weekOfYear, from: now)
        let currentYear  = cal.component(.year, from: now)
        let currentMonth = cal.component(.month, from: now)

        for i in tasks.indices {
            guard tasks[i].isCompleted, let completedDate = tasks[i].completedDate else { continue }
            switch tasks[i].recurrence {
            case .weekly:
                let w = cal.component(.weekOfYear, from: completedDate)
                let y = cal.component(.year, from: completedDate)
                if w != currentWeek || y != currentYear {
                    tasks[i].isCompleted = false
                    tasks[i].completedDate = nil
                }
            case .monthly:
                let m = cal.component(.month, from: completedDate)
                let y = cal.component(.year, from: completedDate)
                if m != currentMonth || y != currentYear {
                    tasks[i].isCompleted = false
                    tasks[i].completedDate = nil
                }
            case .daily:
                break
            }
        }

        return tasks
    }

    func save(_ tasks: [PeriodicTask]) {
        guard let data = try? JSONEncoder().encode(tasks) else { return }
        defaults.set(data, forKey: Self.key)
    }
}
