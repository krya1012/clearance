//
//  SeedData.swift
//  PreFlight
//
//  Provides the default checklist content so the app is useful on first launch.
//  Seeding is idempotent: it only runs when the store is empty.
//

import Foundation
import SwiftData

enum SeedData {

    /// Inserts the default items if (and only if) the store currently has none.
    /// Safe to call on every launch.
    @MainActor
    static func seedIfNeeded(in context: ModelContext) {
        let existingCount = (try? context.fetchCount(FetchDescriptor<ChecklistItem>())) ?? 0
        guard existingCount == 0 else { return }

        for item in defaultItems() {
            context.insert(item)
        }
        try? context.save()
    }

    /// The canonical default routine. Each `orderIndex` is local to its
    /// (checklist, module) group, which is how the UI sorts within a section.
    static func defaultItems() -> [ChecklistItem] {
        var items: [ChecklistItem] = []

        func add(_ titles: [String], module: ModuleType, checklist: ChecklistType) {
            for (index, title) in titles.enumerated() {
                items.append(
                    ChecklistItem(
                        title: title,
                        orderIndex: index,
                        associatedModule: module,
                        associatedChecklist: checklist
                    )
                )
            }
        }

        // 🌅 Takeoff — Morning Core
        add(
            [
                "Hydrate — full glass of water",
                "Make the bed",
                "5-minute mobility / stretch",
                "Box breathing — 5 rounds",
                "Review today's top 3 priorities",
                "Cold shower",
                "Protein-forward breakfast",
            ],
            module: .core,
            checklist: .morning
        )

        // 🌌 Landing — Evening Core
        add(
            [
                "Set tomorrow's top 3 priorities",
                "Tidy desk & reset workspace",
                "Journal — 3 lines",
                "Read 10 pages",
                "Screens off",
                "Lights out by target time",
            ],
            module: .core,
            checklist: .evening
        )

        // 🏋️ Gym — morning session
        add(
            [
                "Pre-workout hydration",
                "Dynamic warm-up",
                "Main lifts — working sets",
                "Accessory work",
                "Cooldown & stretch",
                "Post-workout protein",
            ],
            module: .gym,
            checklist: .morning
        )

        // 🏋️ Gym — evening prep
        add(
            [
                "Pack gym bag for tomorrow",
                "Lay out training kit",
            ],
            module: .gym,
            checklist: .evening
        )

        // 🏊 Swim — morning session
        add(
            [
                "Pack goggles, cap & towel",
                "Shower before entering the pool",
                "Warm-up — 200m easy",
                "Main set",
                "Cooldown — 100m easy",
                "Post-swim shower & rinse kit",
            ],
            module: .swim,
            checklist: .morning
        )

        return items
    }
}
