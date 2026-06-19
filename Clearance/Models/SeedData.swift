//
//  SeedData.swift
//  Clearance
//
//  Versioned seed data. Bump `currentVersion` whenever the canonical checklist
//  content changes — on next launch the old items and modules are deleted and
//  a fresh canonical set is inserted.
//

import Foundation
import SwiftData

enum SeedData {

    private static let currentVersion = 7
    private static let versionKey = "Clearance.seedVersion.v1"

    // MARK: - Entry point

    @MainActor
    static func seedIfNeeded(in context: ModelContext) {
        let storedVersion = UserDefaults.standard.integer(forKey: versionKey)
        guard storedVersion < currentVersion else { return }

        // Clear stale UserDefaults module keys so old UUIDs don't survive the re-seed.
        ScheduleStore().clearModuleKeys()

        // Delete all existing items and modules before re-seeding.
        let existingItems = (try? context.fetch(FetchDescriptor<ChecklistItem>())) ?? []
        existingItems.forEach { context.delete($0) }
        let existingModules = (try? context.fetch(FetchDescriptor<ActivityModule>())) ?? []
        existingModules.forEach { context.delete($0) }

        // Insert modules first so their IDs are stable.
        let modules = defaultModules()
        modules.forEach { context.insert($0) }
        try? context.save()

        // Insert tasks referencing module IDs.
        defaultItems(modules: modules).forEach { context.insert($0) }
        try? context.save()
        UserDefaults.standard.set(currentVersion, forKey: versionKey)
    }

    // MARK: - Default modules

    static func defaultModules() -> [ActivityModule] {
        [
            ActivityModule(name: "Core",  emoji: "🎯", sortOrder: 0, isCore: true),
            ActivityModule(name: "Gym",   emoji: "🏋️", sortOrder: 1),
            ActivityModule(name: "Swim",  emoji: "🏊", sortOrder: 2),
            ActivityModule(name: "Judo",    emoji: "🥋", sortOrder: 3),
            ActivityModule(name: "Cycling", emoji: "🚴", sortOrder: 4),
            ActivityModule(name: "Running", emoji: "🏃", sortOrder: 5),
        ]
    }

    // MARK: - Canonical task content

    static func defaultItems(modules: [ActivityModule]) -> [ChecklistItem] {
        guard
            let core    = modules.first(where: \.isCore),
            let gym     = modules.first(where: { $0.name == "Gym" }),
            let swim    = modules.first(where: { $0.name == "Swim" }),
            let judo    = modules.first(where: { $0.name == "Judo" }),
            let cycling = modules.first(where: { $0.name == "Cycling" }),
            let running = modules.first(where: { $0.name == "Running" })
        else { return [] }

        var items: [ChecklistItem] = []

        func add(
            _ titles: [String],
            checklist: ChecklistType,
            module: ActivityModule,
            phase: String,
            phaseIndex: Int
        ) {
            for (index, title) in titles.enumerated() {
                items.append(ChecklistItem(
                    title: title,
                    orderIndex: index,
                    phase: phase,
                    phaseIndex: phaseIndex,
                    associatedModule: module.id.uuidString,
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
            checklist: .morning, module: core,
            phase: "Systems Launch", phaseIndex: 0
        )

        add(
            [
                "Eat structured breakfast (complex carbs + protein)",
                "Take scheduled morning vitamins / supplements",
            ],
            checklist: .morning, module: core,
            phase: "Refueling", phaseIndex: 1
        )

        add(
            [
                "Check weather app & dress in appropriate layers",
                "Collect pockets loadout: Phone, Wallet, Keys, Headphones",
                "Grab work / study bag (Laptop, Charger, Documents)",
            ],
            checklist: .morning, module: core,
            phase: "Pre-Exit Avionics", phaseIndex: 2
        )

        add(
            [
                "Verify stove, iron, and appliances are completely OFF",
                "Turn off all lights",
                "Close all windows securely",
                "Lock the main entry door",
            ],
            checklist: .morning, module: core,
            phase: "Cabin Secure", phaseIndex: 3
        )

        // ─────────────────────────────────────────
        // 🌅  MORNING — Gym
        // ─────────────────────────────────────────

        add(
            [
                "Status Check: Confirm the Gym 'Dry-Pack' was packed last night",
                "Hydration: Fill water bottle / shaker with cold water and stow it",
                "Fuel: Add a pre-workout snack (banana, bar) or supplement scoop",
                "Power: Confirm headphones and fitness tracker are charged",
                "Access: Confirm Gym Membership Card / Key Fob is in the bag",
                "Grab the Gym 'Dry-Pack' on your way out the door",
            ],
            checklist: .morning, module: gym,
            phase: "Final Gear Check — Before Exit", phaseIndex: 0
        )

        // ─────────────────────────────────────────
        // 🌅  MORNING — Swim
        // ─────────────────────────────────────────

        add(
            [
                "Status Check: Confirm the Swim 'Acwa-Pack' was packed last night",
                "Optics: Confirm goggles and swim cap are inside the bag",
                "Access: Confirm Pool Pass / Access Card is inside the bag",
                "Hydration: Fill water bottle with cold water and stow it",
                "Dry Layer: Confirm body towel and waterproof dry-bag are packed",
                "Grab the Swim 'Acwa-Pack' on your way out the door",
            ],
            checklist: .morning, module: swim,
            phase: "Final Gear Check — Before Exit", phaseIndex: 0
        )

        // ─────────────────────────────────────────
        // 🥋  MORNING — Judo
        // ─────────────────────────────────────────

        add(
            [
                "Status Check: Confirm the Judo 'Dojo-Pack' was packed last night",
                "Gi Check: Confirm clean gi (jacket + trousers) and belt are inside",
                "Footwear: Confirm flip-flops / zori for off-mat walking are packed",
                "Hydration: Fill water bottle with cold water and stow it",
                "Access: Confirm club membership / dojo access card is in the bag",
                "Grab the Judo 'Dojo-Pack' on your way out the door",
            ],
            checklist: .morning, module: judo,
            phase: "Final Gear Check — Before Exit", phaseIndex: 0
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
            checklist: .evening, module: core,
            phase: "Flight Deck Debrief", phaseIndex: 0
        )

        add(
            [
                "Turn off bright overhead lights; switch to warm lamps / candles",
                "Set phone to Night Shift / Do Not Disturb (stop scrolling)",
                "Open bedroom window slightly (target: 18–19 °C)",
            ],
            checklist: .evening, module: core,
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
            checklist: .evening, module: core,
            phase: "Engine Shutdown — T minus 30 min", phaseIndex: 2
        )

        // ─────────────────────────────────────────
        // 🏋️  EVENING — Gym
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
            checklist: .evening, module: gym,
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
            checklist: .evening, module: gym,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        // ─────────────────────────────────────────
        // 🏊  EVENING — Swim
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
            checklist: .evening, module: swim,
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
            checklist: .evening, module: swim,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        // ─────────────────────────────────────────
        // 🥋  EVENING — Judo
        // ─────────────────────────────────────────

        add(
            [
                "Gi: Pack a clean, dry gi — jacket and trousers",
                "Belt: Pack your belt (rolled, not knotted)",
                "Off-Mat Footwear: Pack flip-flops / zori for walking off the mat",
                "Hygiene: Pack a small towel, body wash, and shampoo for the post-session shower",
                "Body Care: Pack finger/toe tape and any joint supports (knee, ankle)",
                "Hydration: Prep water bottle with cold water",
                "Access: Place club membership / dojo access card into the bag's secure pocket",
            ],
            checklist: .evening, module: judo,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Gi Strip: Remove the sweaty gi → hang to air out or drop into the laundry",
                "Belt Care: Air out the belt → hang it up (traditionally never washed)",
                "Footwear Airing: Take out flip-flops → wipe down and leave to dry",
                "Towel Extraction: Remove the used towel → place in laundry",
                "Hydration Reset: Empty and rinse the water bottle to prevent odors",
            ],
            checklist: .evening, module: judo,
            phase: "Post-Workout Unload & Reset — Evening Return", phaseIndex: 1
        )

        // ─────────────────────────────────────────
        // 🚴  MORNING — Cycling
        // ─────────────────────────────────────────

        add(
            [
                "Status Check: Confirm the Cycling 'Velo-Pack' was packed last night",
                "Safety: Confirm helmet is in or strapped to the bag",
                "Visibility: Confirm front and rear lights are charged and clipped on",
                "Security: Confirm bike lock and key / combo are in the bag",
                "Hydration: Fill water bottles and slot them in the bike cages",
                "Fuel: Add an energy gel or bar for rides over 45 min",
                "Tyres: Quick squeeze-check both tyres for correct pressure",
                "Grab the Cycling 'Velo-Pack' on your way out the door",
            ],
            checklist: .morning, module: cycling,
            phase: "Final Gear Check — Before Exit", phaseIndex: 0
        )

        // ─────────────────────────────────────────
        // 🏃  MORNING — Running
        // ─────────────────────────────────────────

        add(
            [
                "Status Check: Confirm the Running 'Stride-Pack' was packed last night",
                "Footwear: Confirm running shoes are in the bag",
                "Visibility: Confirm reflective vest or LED clip-on blinker are packed (if low light)",
                "Electronics: Confirm GPS watch and headphones are charged",
                "Hydration: Fill handheld bottle or hydration vest reservoir for runs over 60 min",
                "Fuel: Add an energy gel for runs over 45 min",
                "Grab the Running 'Stride-Pack' on your way out the door",
            ],
            checklist: .morning, module: running,
            phase: "Final Gear Check — Before Exit", phaseIndex: 0
        )

        // ─────────────────────────────────────────
        // 🚴  EVENING — Cycling
        // ─────────────────────────────────────────

        add(
            [
                "Apparel: Pack cycling jersey and bib shorts / tights",
                "Base layer: Pack moisture-wicking base layer if cold weather expected",
                "Socks: Pack 1 pair of clean cycling socks",
                "Footwear: Pack cycling shoes; check cleat bolts are tight",
                "Gloves: Pack cycling gloves",
                "Eye protection: Pack sunglasses or clear lenses for low-light riding",
                "Electronics: Charge bike computer / GPS, front light, and rear light overnight",
                "Security: Confirm bike lock and key / combo are in the bag",
            ],
            checklist: .evening, module: cycling,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Apparel: Remove sweaty jersey, bib shorts, and socks → drop into laundry bin",
                "Footwear: Wipe down cycling shoes; air out on a rack",
                "Bike wipe-down: Remove road grime from frame and drivetrain with a damp cloth",
                "Lights: Detach front and rear lights → connect to chargers",
                "Water bottles: Empty, rinse, and leave open to dry",
                "Helmet: Wipe inside padding; place on shelf (never hang by straps)",
            ],
            checklist: .evening, module: cycling,
            phase: "Post-Ride Unload & Reset — Evening Return", phaseIndex: 1
        )

        // ─────────────────────────────────────────
        // 🏃  EVENING — Running
        // ─────────────────────────────────────────

        add(
            [
                "Apparel: Pack moisture-wicking running top and shorts or tights",
                "Socks: Pack 1 pair of technical running socks",
                "Footwear: Pack clean running shoes (check laces and sole wear)",
                "Outerwear: Pack a lightweight wind jacket if tomorrow's forecast is cold or wet",
                "Visibility: Pack reflective vest or LED clip-on blinker",
                "Electronics: Charge GPS watch and wireless headphones overnight",
                "Hydration: Prep handheld bottle or fill hydration vest if a long run is planned",
            ],
            checklist: .evening, module: running,
            phase: "Collect & Pack — Evening Before", phaseIndex: 0
        )

        add(
            [
                "Apparel: Remove sweaty top, shorts, and socks → drop into laundry bin",
                "Footwear: Pull out insoles; leave shoes and insoles open on a rack to dry",
                "GPS Watch: Sync the run to your training app → place watch on charger",
                "Headphones: Connect headphones to charging cable",
                "Hydration: Empty and rinse water bottle / vest reservoir; leave open to air dry",
            ],
            checklist: .evening, module: running,
            phase: "Post-Run Unload & Reset — Evening Return", phaseIndex: 1
        )

        return items
    }
}
