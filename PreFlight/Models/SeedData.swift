//
//  SeedData.swift
//  PreFlight
//
//  Versioned seed data. Bump `currentVersion` whenever the canonical checklist
//  content changes — on next launch the old items are deleted and the fresh set
//  is inserted.
//

import Foundation
import SwiftData

enum SeedData {

    private static let currentVersion = 2
    private static let versionKey = "PreFlight.seedVersion.v1"

    // MARK: - Entry point

    @MainActor
    static func seedIfNeeded(in context: ModelContext) {
        let storedVersion = UserDefaults.standard.integer(forKey: versionKey)
        guard storedVersion < currentVersion else { return }

        // Delete all existing items before re-seeding.
        let existing = (try? context.fetch(FetchDescriptor<ChecklistItem>())) ?? []
        existing.forEach { context.delete($0) }

        for item in defaultItems() {
            context.insert(item)
        }
        try? context.save()
        UserDefaults.standard.set(currentVersion, forKey: versionKey)
    }

    // MARK: - Canonical content (v2)

    static func defaultItems() -> [ChecklistItem] {
        var items: [ChecklistItem] = []

        /// Appends a block of items to a given (checklist, module, phase).
        func add(
            _ titles: [String],
            checklist: ChecklistType,
            module: ModuleType,
            phase: String,
            phaseIndex: Int
        ) {
            for (index, title) in titles.enumerated() {
                items.append(ChecklistItem(
                    title: title,
                    orderIndex: index,
                    phase: phase,
                    phaseIndex: phaseIndex,
                    associatedModule: module,
                    associatedChecklist: checklist
                ))
            }
        }

        // ─────────────────────────────────────────
        // 🌅  MORNING — Core
        // ─────────────────────────────────────────

        add(
            [
                "Drink 1 full glass of clean water",
                "Open blinds / let daylight in",
                "5-minute morning joint mobility stretch",
                "Wash face & brush teeth",
            ],
            checklist: .morning, module: .core,
            phase: "Systems Launch", phaseIndex: 0
        )

        add(
            [
                "Eat structured breakfast (complex carbs + protein)",
                "Take scheduled morning vitamins / supplements",
            ],
            checklist: .morning, module: .core,
            phase: "Refueling", phaseIndex: 1
        )

        add(
            [
                "Check weather app & dress in appropriate layers",
                "Collect pockets loadout: Phone, Wallet, Keys, Headphones",
                "Grab work / study bag (Laptop, Charger, Documents)",
            ],
            checklist: .morning, module: .core,
            phase: "Pre-Exit Avionics", phaseIndex: 2
        )

        add(
            [
                "Verify stove, iron, and appliances are completely OFF",
                "Turn off all lights",
                "Close all windows securely",
                "Lock the main entry door",
            ],
            checklist: .morning, module: .core,
            phase: "Cabin Secure", phaseIndex: 3
        )

        // ─────────────────────────────────────────
        // 🌅  MORNING — Gym module (injected when Gym is ON)
        // ─────────────────────────────────────────

        add(
            [
                "Grab the packed Gym 'Dry-Pack'",
            ],
            checklist: .morning, module: .gym,
            phase: "Pre-Exit Avionics", phaseIndex: 0
        )

        // ─────────────────────────────────────────
        // 🌅  MORNING — Swim module (injected when Swim is ON)
        // ─────────────────────────────────────────

        add(
            [
                "Grab the packed Swim 'Acwa-Pack'",
            ],
            checklist: .morning, module: .swim,
            phase: "Pre-Exit Avionics", phaseIndex: 0
        )

        // ─────────────────────────────────────────
        // 🌌  EVENING — Core
        // ─────────────────────────────────────────

        add(
            [
                "Clean the kitchen sink & wipe down workspace",
                "Check calendar — write down Top 3 tasks for tomorrow",
                "Run Gym / Swim — Post-Workout Unload (clear out dirty gear)",
                "Run Gym / Swim — Pre-Packing (pack tomorrow's sports bags)",
                "Lay out tomorrow's casual / work clothes",
            ],
            checklist: .evening, module: .core,
            phase: "Flight Deck Debrief", phaseIndex: 0
        )

        add(
            [
                "Turn off bright overhead lights; switch to warm lamps / candles",
                "Set phone to Night Shift / Do Not Disturb (stop scrolling)",
                "Open bedroom window slightly (target: 18–19 °C)",
            ],
            checklist: .evening, module: .core,
            phase: "Dimming the Lights — T minus 60 min", phaseIndex: 1
        )

        add(
            [
                "Take a warm shower or bath (lowers core body temp)",
                "Floss and brush teeth",
                "10 minutes: fiction reading, journaling, or breathing exercises",
                "Verify morning alarm is set accurately",
                "Put on eye mask / insert earplugs if needed",
                "Kill all lights → Complete System Shutdown",
            ],
            checklist: .evening, module: .core,
            phase: "Engine Shutdown — T minus 30 min", phaseIndex: 2
        )

        // ─────────────────────────────────────────
        // 🏋️  EVENING — Gym module
        // ─────────────────────────────────────────

        add(
            [
                "Pack 1 training t-shirt (breathable)",
                "Pack 1 pair of athletic shorts or track pants",
                "Pack 1 pair of clean gym socks",
                "Pack clean indoor training sneakers",
                "Pack 1 small microfiber towel (for gym benches)",
                "Pack deodorant / body spray",
                "Ensure wireless headphones are 100% charged",
                "Put Gym Membership Card / QR Code in bag pocket",
                "Prep water bottle / shaker (pre-measured pre-workout or protein)",
            ],
            checklist: .evening, module: .gym,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Remove sweaty training t-shirt and shorts → laundry bin",
                "Remove used workout towel → laundry bin",
                "Take out training shoes → place on rack to air out",
                "Take out shaker bottle → wash with soap immediately",
                "Put headphones back on the charger",
            ],
            checklist: .evening, module: .gym,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        // ─────────────────────────────────────────
        // 🏊  EVENING — Swim module
        // ─────────────────────────────────────────

        add(
            [
                "Pack swimming trunks / jammers",
                "Pack swim cap (silicone or Lycra)",
                "Pack swim goggles (check anti-fog layer is clean)",
                "Pack rubber pool slides / flip-flops (hygiene in showers/deck)",
                "Pack shower gel / body wash and loofah (mandatory pre/post wash)",
                "Pack 1 large, highly absorbent body towel",
                "Pack 1 heavy-duty waterproof dry-bag (to hold wet items)",
                "Verify Pool Medical Certificate / Access Pass is in the bag",
            ],
            checklist: .evening, module: .swim,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Remove wet swimming trunks → rinse with fresh water, hang to dry",
                "Remove wet body towel → hang to dry or toss in laundry bin",
                "Remove pool slides → wipe down or leave to dry face down",
                "Rinse goggles and swim cap with fresh water (no soap), air-dry flat",
                "Open empty waterproof dry-bag fully to air out (prevent mold)",
            ],
            checklist: .evening, module: .swim,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        return items
    }
}
