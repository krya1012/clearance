//
//  SeedData.swift
//  Clearance
//
//  Versioned seed data. Bump `currentVersion` whenever the canonical checklist
//  content changes — on next launch the old items are deleted and the fresh set
//  is inserted.
//

import Foundation
import SwiftData

enum SeedData {

    private static let currentVersion = 3
    private static let versionKey = "Clearance.seedVersion.v1"

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

    // MARK: - Canonical content

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
                "Apparel: Pack 1 training t-shirt and 1 pair of athletic shorts/pants",
                "Undergarments: Pack 1 pair of clean gym socks and fresh underwear",
                "Footwear: Pack clean indoor training sneakers (check soles for dirt)",
                "Hygiene Barrier: Pack 1 small microfiber towel (to layer over gym benches)",
                "Electronics: Pack wireless headphones (verify battery is charged) and fitness tracker",
                "Access: Place Gym Membership Card / Key Fob into the bag's secure pocket",
                "Nutrition/Hydration: Prep water bottle or shaker with pre-measured supplement powder",
            ],
            checklist: .evening, module: .gym,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Laundry Strip: Extract sweaty t-shirt, shorts, and socks → drop directly into the laundry bin",
                "Towel Extraction: Remove the used bench towel → place in laundry",
                "Footwear Airing: Take out training sneakers → place on an open rack to air out",
                "Shaker Sanitize: Empty and wash the shaker bottle with soap immediately to prevent odors",
                "Electronics Dock: Connect headphones to the charging cable",
            ],
            checklist: .evening, module: .gym,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        // ─────────────────────────────────────────
        // 🏊  EVENING — Swim module
        // ─────────────────────────────────────────

        add(
            [
                "Swimwear: Pack swimming trunks / jammers",
                "Head/Eye Gear: Pack swim cap and swim goggles (verify anti-fog lenses are clear)",
                "Deck Footwear: Pack rubber pool slides / flip-flops (for shower and deck hygiene)",
                "Shower Kit: Pack body wash/soap, shampoo, and a loofah",
                "Drying Layer: Pack 1 large, highly absorbent body towel",
                "Moisture Barrier: Pack 1 heavy-duty waterproof dry-bag (to isolate wet items later)",
                "Access: Verify Pool Pass / Access Card is inside the bag",
            ],
            checklist: .evening, module: .swim,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Wet Extraction: Pull wet swimming trunks out of the dry-bag → rinse with fresh water and hang up to dry immediately",
                "Towel Drying: Remove the wet body towel → hang on a drying rack or toss into the laundry",
                "Optics Care: Rinse goggles and swim cap with clean, fresh water (no soap) → lay flat to air dry",
                "Footwear Sanitize: Take out pool slides → wipe down and leave to dry",
                "Dry-Bag Airing: Turn the empty waterproof dry-bag inside out or leave it completely open to prevent mold and mildew growth",
            ],
            checklist: .evening, module: .swim,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        return items
    }
}
