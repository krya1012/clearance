package com.krya1012.clearance.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Versioned seed data. Bump `CURRENT_VERSION` whenever the canonical checklist
 * content changes — on next launch the old items and modules are migrated
 * additively into a fresh canonical set. Mirrors iOS `SeedData.swift`.
 */
object SeedData {

    private const val CURRENT_VERSION = 12
    private val VERSION_KEY = intPreferencesKey("Clearance.seedVersion.v1")

    // MARK: - Entry point

    suspend fun seedIfNeeded(
        context: Context,
        moduleDao: ActivityModuleDao,
        itemDao: ChecklistItemDao,
        scheduleStore: ScheduleStore,
    ) {
        val storedVersion = context.clearanceDataStore.data.first()[VERSION_KEY] ?: 0
        if (storedVersion >= CURRENT_VERSION) return

        // Remove deprecated module types that are no longer part of the app.
        val deprecatedNames = setOf("Work", "Study", "Cooking", "Leisure")
        for (module in moduleDao.getAll().filter { it.name in deprecatedNames }) {
            itemDao.deleteByModule(module.id)
            moduleDao.delete(module)
        }

        // Rename "Rest" → "Walking" and remove locked status.
        moduleDao.getAll().firstOrNull { it.name == "Rest" }?.let { rest ->
            moduleDao.update(rest.copy(name = "Walking", isLocked = false))
        }

        // Index existing modules by name so user-created modules survive.
        val existingByName = moduleDao.getAll().associateBy { it.name }

        // Track which module IDs already have at least one task.
        val modulesWithItems = itemDao.getModulesWithItems().toSet()

        // For each seed module: reuse existing (same name) or insert new.
        val resolvedModules = mutableListOf<ActivityModule>()
        val newOptionalIDs = mutableSetOf<String>()

        for (seedModule in defaultModules()) {
            val existing = existingByName[seedModule.name]
            if (existing != null) {
                // Keep existing id; sync sortOrder, emoji, and lock status to latest seed values.
                val updated = existing.copy(
                    sortOrder = seedModule.sortOrder,
                    emoji = seedModule.emoji,
                    isLocked = seedModule.isLocked,
                )
                moduleDao.update(updated)
                resolvedModules.add(updated)
            } else {
                moduleDao.insert(seedModule)
                resolvedModules.add(seedModule)
                if (!seedModule.isCore) newOptionalIDs.add(seedModule.id)
            }
        }

        // Insert seed items only for modules that currently have no items.
        val itemsToInsert = defaultItems(resolvedModules).filter { it.associatedModule !in modulesWithItems }
        itemDao.insertAll(itemsToInsert)

        // Enable any newly-inserted optional modules without touching existing prefs.
        if (newOptionalIDs.isNotEmpty()) {
            val current = scheduleStore.loadEnabledModuleIDs()
                ?: resolvedModules.filter { !it.isCore }.map { it.id }.toSet()
            scheduleStore.saveEnabledModuleIDs(current + newOptionalIDs)

            // Schedule Walking on Sunday by default.
            val walking = resolvedModules.firstOrNull { it.name == "Walking" }
            if (walking != null && newOptionalIDs.contains(walking.id)) {
                val schedule = scheduleStore.loadSchedule().toMutableMap()
                schedule[Weekday.SUNDAY] = (schedule[Weekday.SUNDAY] ?: emptySet()) + walking.id
                scheduleStore.saveSchedule(schedule)
            }
        }

        context.clearanceDataStore.edit { it[VERSION_KEY] = CURRENT_VERSION }
    }

    // MARK: - Default modules

    fun defaultModules(): List<ActivityModule> = listOf(
        ActivityModule(name = "Core", emoji = "🎯", sortOrder = 0, isCore = true),
        ActivityModule(name = "Gym", emoji = "🏋️", sortOrder = 1),
        ActivityModule(name = "Swim", emoji = "🏊", sortOrder = 2),
        ActivityModule(name = "Judo", emoji = "🥋", sortOrder = 3),
        ActivityModule(name = "Cycling", emoji = "🚴", sortOrder = 4),
        ActivityModule(name = "Running", emoji = "🏃", sortOrder = 5),
        ActivityModule(name = "Yoga", emoji = "🧘", sortOrder = 6),
        ActivityModule(name = "Walking", emoji = "🚶", sortOrder = 7),
    )

    // MARK: - Canonical task content

    fun defaultItems(module: ActivityModule, allModules: List<ActivityModule>): List<ChecklistItem> =
        defaultItems(allModules).filter { it.associatedModule == module.id }

    fun defaultItems(modules: List<ActivityModule>): List<ChecklistItem> {
        val core = modules.firstOrNull { it.isCore } ?: return emptyList()
        val gym = modules.firstOrNull { it.name == "Gym" }
        val swim = modules.firstOrNull { it.name == "Swim" }
        val judo = modules.firstOrNull { it.name == "Judo" }
        val cycling = modules.firstOrNull { it.name == "Cycling" }
        val running = modules.firstOrNull { it.name == "Running" }
        val yoga = modules.firstOrNull { it.name == "Yoga" }
        val walking = modules.firstOrNull { it.name == "Walking" }

        val items = mutableListOf<ChecklistItem>()

        fun add(
            titles: List<String>,
            checklist: ChecklistType,
            module: ActivityModule?,
            phase: String,
            phaseIndex: Int,
        ) {
            if (module == null) return
            titles.forEachIndexed { index, title ->
                items.add(
                    ChecklistItem(
                        title = title,
                        orderIndex = index,
                        phase = phase,
                        phaseIndex = phaseIndex,
                        associatedModule = module.id,
                        associatedChecklist = checklist,
                    )
                )
            }
        }

        // ─────────────────────────────────────────
        // 🌅  MORNING — Core
        // ─────────────────────────────────────────

        add(
            listOf(
                "Drink 1 full glass of clean water",
                "Open blinds / let daylight in",
                "5-minute morning joint mobility stretch",
                "Wash face & brush teeth",
            ),
            checklist = ChecklistType.MORNING, module = core,
            phase = "Systems Launch", phaseIndex = 0
        )

        add(
            listOf(
                "Eat structured breakfast (complex carbs + protein)",
                "Take scheduled morning vitamins / supplements",
            ),
            checklist = ChecklistType.MORNING, module = core,
            phase = "Refueling", phaseIndex = 1
        )

        add(
            listOf(
                "Check weather app & dress in appropriate layers",
                "Collect pockets loadout: Phone, Wallet, Keys, Headphones",
                "Grab work / study bag (Laptop, Charger, Documents)",
            ),
            checklist = ChecklistType.MORNING, module = core,
            phase = "Pre-Exit Avionics", phaseIndex = 2
        )

        add(
            listOf(
                "Verify stove, iron, and appliances are completely OFF",
                "Turn off all lights",
                "Close all windows securely",
                "Lock the main entry door",
            ),
            checklist = ChecklistType.MORNING, module = core,
            phase = "Cabin Secure", phaseIndex = 3
        )

        // ─────────────────────────────────────────
        // 🌅  MORNING — Gym
        // ─────────────────────────────────────────

        add(
            listOf(
                "Status Check: Confirm the Gym 'Dry-Pack' was packed last night",
                "Hydration: Fill water bottle / shaker with cold water and stow it",
                "Power: Confirm headphones and fitness tracker are charged",
                "Access: Confirm Gym Membership Card / Key Fob is in the bag",
                "Grab the Gym 'Dry-Pack' on your way out the door",
            ),
            checklist = ChecklistType.MORNING, module = gym,
            phase = "Final Gear Check — Before Exit", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🌅  MORNING — Swim
        // ─────────────────────────────────────────

        add(
            listOf(
                "Status Check: Confirm the Swim 'Acwa-Pack' was packed last night",
                "Optics: Confirm goggles and swim cap are inside the bag",
                "Access: Confirm Pool Pass / Access Card is inside the bag",
                "Hydration: Fill water bottle with cold water and stow it",
                "Dry Layer: Confirm body towel and waterproof dry-bag are packed",
                "Grab the Swim 'Acwa-Pack' on your way out the door",
            ),
            checklist = ChecklistType.MORNING, module = swim,
            phase = "Final Gear Check — Before Exit", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🥋  MORNING — Judo
        // ─────────────────────────────────────────

        add(
            listOf(
                "Status Check: Confirm the Judo 'Dojo-Pack' was packed last night",
                "Gi Check: Confirm clean gi (jacket + trousers) and belt are inside",
                "Footwear: Confirm flip-flops / zori for off-mat walking are packed",
                "Hydration: Fill water bottle with cold water and stow it",
                "Access: Confirm club membership / dojo access card is in the bag",
                "Grab the Judo 'Dojo-Pack' on your way out the door",
            ),
            checklist = ChecklistType.MORNING, module = judo,
            phase = "Final Gear Check — Before Exit", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🌌  EVENING — Core
        // ─────────────────────────────────────────

        add(
            listOf(
                "Clean the kitchen sink & wipe down workspace",
                "Check calendar — write down Top 3 tasks for tomorrow",
                "Run Gym / Swim — Post-Workout Unload (clear out dirty gear)",
                "Run Gym / Swim — Pre-Packing (pack tomorrow's sports bags)",
                "Lay out tomorrow's casual / work clothes",
            ),
            checklist = ChecklistType.EVENING, module = core,
            phase = "Flight Deck Debrief", phaseIndex = 0
        )

        add(
            listOf(
                "Turn off bright overhead lights; switch to warm lamps / candles",
                "Set phone to Night Shift / Do Not Disturb (stop scrolling)",
                "Open bedroom window slightly (target: 18–19 °C)",
            ),
            checklist = ChecklistType.EVENING, module = core,
            phase = "Dimming the Lights — T minus 60 min", phaseIndex = 1
        )

        add(
            listOf(
                "Take a warm shower or bath (lowers core body temp)",
                "Floss and brush teeth",
                "10 minutes: fiction reading, journaling, or breathing exercises",
                "Verify morning alarm is set accurately",
                "Put on eye mask / insert earplugs if needed",
                "Kill all lights → Complete System Shutdown",
            ),
            checklist = ChecklistType.EVENING, module = core,
            phase = "Engine Shutdown — T minus 30 min", phaseIndex = 2
        )

        // ─────────────────────────────────────────
        // 🏋️  EVENING — Gym
        // ─────────────────────────────────────────

        add(
            listOf(
                "Apparel: Pack 1 training t-shirt and 1 pair of athletic shorts/pants",
                "Undergarments: Pack 1 pair of clean gym socks and fresh underwear",
                "Footwear: Pack clean indoor training sneakers (check soles for dirt)",
                "Hygiene Barrier: Pack 1 small microfiber towel (to layer over gym benches)",
                "Electronics: Pack wireless headphones (verify battery is charged) and fitness tracker",
                "Access: Place Gym Membership Card / Key Fob into the bag's secure pocket",
                "Nutrition/Hydration: Prep water bottle or shaker with cold water",
            ),
            checklist = ChecklistType.EVENING, module = gym,
            phase = "Collect & Pack — Evening Before", phaseIndex = 0
        )

        add(
            listOf(
                "Supplements: Take post-workout recovery supplement (creatine, protein, magnesium)",
                "Laundry Strip: Extract sweaty t-shirt, shorts, and socks → drop directly into the laundry bin",
                "Towel Extraction: Remove the used bench towel → place in laundry",
                "Footwear Airing: Take out training sneakers → place on an open rack to air out",
                "Shaker Sanitize: Empty and wash the shaker bottle with soap immediately to prevent odors",
                "Electronics Dock: Connect headphones to the charging cable",
            ),
            checklist = ChecklistType.EVENING, module = gym,
            phase = "Post-Workout Unload & Reset — Evening Return", phaseIndex = 1
        )

        // ─────────────────────────────────────────
        // 🏊  EVENING — Swim
        // ─────────────────────────────────────────

        add(
            listOf(
                "Swimwear: Pack swimming trunks / jammers",
                "Head/Eye Gear: Pack swim cap and swim goggles (verify anti-fog lenses are clear)",
                "Deck Footwear: Pack rubber pool slides / flip-flops (for shower and deck hygiene)",
                "Shower Kit: Pack body wash/soap, shampoo, and a loofah",
                "Drying Layer: Pack 1 large, highly absorbent body towel",
                "Moisture Barrier: Pack 1 heavy-duty waterproof dry-bag (to isolate wet items later)",
                "Access: Verify Pool Pass / Access Card is inside the bag",
            ),
            checklist = ChecklistType.EVENING, module = swim,
            phase = "Collect & Pack — Evening Before", phaseIndex = 0
        )

        add(
            listOf(
                "Wet Extraction: Pull wet swimming trunks out of the dry-bag → rinse with fresh water and hang up to dry immediately",
                "Towel Drying: Remove the wet body towel → hang on a drying rack or toss into the laundry",
                "Optics Care: Rinse goggles and swim cap with clean, fresh water (no soap) → lay flat to air dry",
                "Footwear Sanitize: Take out pool slides → wipe down and leave to dry",
                "Dry-Bag Airing: Turn the empty waterproof dry-bag inside out or leave it completely open to prevent mold and mildew growth",
            ),
            checklist = ChecklistType.EVENING, module = swim,
            phase = "Post-Workout Unload & Reset — Evening Return", phaseIndex = 1
        )

        // ─────────────────────────────────────────
        // 🥋  EVENING — Judo
        // ─────────────────────────────────────────

        add(
            listOf(
                "Gi: Pack a clean, dry gi — jacket and trousers",
                "Belt: Pack your belt (rolled, not knotted)",
                "Off-Mat Footwear: Pack flip-flops / zori for walking off the mat",
                "Hygiene: Pack a small towel, body wash, and shampoo for the post-session shower",
                "Body Care: Pack finger/toe tape and any joint supports (knee, ankle)",
                "Hydration: Prep water bottle with cold water",
                "Access: Place club membership / dojo access card into the bag's secure pocket",
            ),
            checklist = ChecklistType.EVENING, module = judo,
            phase = "Collect & Pack — Evening Before", phaseIndex = 0
        )

        add(
            listOf(
                "Gi Strip: Remove the sweaty gi → hang to air out or drop into the laundry",
                "Belt Care: Air out the belt → hang it up (traditionally never washed)",
                "Footwear Airing: Take out flip-flops → wipe down and leave to dry",
                "Towel Extraction: Remove the used towel → place in laundry",
                "Hydration Reset: Empty and rinse the water bottle to prevent odors",
            ),
            checklist = ChecklistType.EVENING, module = judo,
            phase = "Post-Workout Unload & Reset — Evening Return", phaseIndex = 1
        )

        // ─────────────────────────────────────────
        // 🚴  MORNING — Cycling
        // ─────────────────────────────────────────

        add(
            listOf(
                "Status Check: Confirm the Cycling 'Velo-Pack' was packed last night",
                "Safety: Confirm helmet is in or strapped to the bag",
                "Visibility: Confirm front and rear lights are charged and clipped on",
                "Security: Confirm bike lock and key / combo are in the bag",
                "Hydration: Fill water bottles and slot them in the bike cages",
                "Fuel: Add an energy gel or bar for rides over 45 min",
                "Tyres: Quick squeeze-check both tyres for correct pressure",
                "Grab the Cycling 'Velo-Pack' on your way out the door",
            ),
            checklist = ChecklistType.MORNING, module = cycling,
            phase = "Final Gear Check — Before Exit", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🏃  MORNING — Running
        // ─────────────────────────────────────────

        add(
            listOf(
                "Status Check: Confirm the Running 'Stride-Pack' was packed last night",
                "Footwear: Confirm running shoes are in the bag",
                "Visibility: Confirm reflective vest or LED clip-on blinker are packed (if low light)",
                "Electronics: Confirm GPS watch and headphones are charged",
                "Hydration: Fill handheld bottle or hydration vest reservoir for runs over 60 min",
                "Fuel: Add an energy gel for runs over 45 min",
                "Grab the Running 'Stride-Pack' on your way out the door",
            ),
            checklist = ChecklistType.MORNING, module = running,
            phase = "Final Gear Check — Before Exit", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🚴  EVENING — Cycling
        // ─────────────────────────────────────────

        add(
            listOf(
                "Apparel: Pack cycling jersey and bib shorts / tights",
                "Base layer: Pack moisture-wicking base layer if cold weather expected",
                "Socks: Pack 1 pair of clean cycling socks",
                "Footwear: Pack cycling shoes; check cleat bolts are tight",
                "Gloves: Pack cycling gloves",
                "Eye protection: Pack sunglasses or clear lenses for low-light riding",
                "Electronics: Charge bike computer / GPS, front light, and rear light overnight",
                "Security: Confirm bike lock and key / combo are in the bag",
            ),
            checklist = ChecklistType.EVENING, module = cycling,
            phase = "Collect & Pack — Evening Before", phaseIndex = 0
        )

        add(
            listOf(
                "Apparel: Remove sweaty jersey, bib shorts, and socks → drop into laundry bin",
                "Footwear: Wipe down cycling shoes; air out on a rack",
                "Bike wipe-down: Remove road grime from frame and drivetrain with a damp cloth",
                "Lights: Detach front and rear lights → connect to chargers",
                "Water bottles: Empty, rinse, and leave open to dry",
                "Helmet: Wipe inside padding; place on shelf (never hang by straps)",
            ),
            checklist = ChecklistType.EVENING, module = cycling,
            phase = "Post-Ride Unload & Reset — Evening Return", phaseIndex = 1
        )

        // ─────────────────────────────────────────
        // 🏃  EVENING — Running
        // ─────────────────────────────────────────

        add(
            listOf(
                "Apparel: Pack moisture-wicking running top and shorts or tights",
                "Socks: Pack 1 pair of technical running socks",
                "Footwear: Pack clean running shoes (check laces and sole wear)",
                "Outerwear: Pack a lightweight wind jacket if tomorrow's forecast is cold or wet",
                "Visibility: Pack reflective vest or LED clip-on blinker",
                "Electronics: Charge GPS watch and wireless headphones overnight",
                "Hydration: Prep handheld bottle or fill hydration vest if a long run is planned",
            ),
            checklist = ChecklistType.EVENING, module = running,
            phase = "Collect & Pack — Evening Before", phaseIndex = 0
        )

        add(
            listOf(
                "Apparel: Remove sweaty top, shorts, and socks → drop into laundry bin",
                "Footwear: Pull out insoles; leave shoes and insoles open on a rack to dry",
                "GPS Watch: Sync the run to your training app → place watch on charger",
                "Headphones: Connect headphones to charging cable",
                "Hydration: Empty and rinse water bottle / vest reservoir; leave open to air dry",
            ),
            checklist = ChecklistType.EVENING, module = running,
            phase = "Post-Run Unload & Reset — Evening Return", phaseIndex = 1
        )

        // ─────────────────────────────────────────
        // 🧘  MORNING — Yoga
        // ─────────────────────────────────────────

        add(
            listOf(
                "Status Check: Confirm the Yoga 'Flow-Pack' was packed last night",
                "Mat: Confirm yoga mat is rolled and in the bag",
                "Props: Confirm yoga block and strap are packed",
                "Apparel: Confirm comfortable, breathable clothes are in the bag",
                "Hydration: Fill water bottle with cool water and stow it",
                "Grab the Yoga 'Flow-Pack' on your way out the door",
            ),
            checklist = ChecklistType.MORNING, module = yoga,
            phase = "Final Gear Check — Before Exit", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🧘  EVENING — Yoga
        // ─────────────────────────────────────────

        add(
            listOf(
                "Mat: Roll up yoga mat and place in bag",
                "Props: Pack yoga block and strap",
                "Apparel: Pack comfortable, moisture-wicking top and leggings / shorts",
                "Hydration: Prep water bottle with cool water",
            ),
            checklist = ChecklistType.EVENING, module = yoga,
            phase = "Collect & Pack — Evening Before", phaseIndex = 0
        )

        add(
            listOf(
                "Mat: Wipe down yoga mat with a damp cloth; unroll and leave to air dry",
                "Props: Wipe block and strap; store on shelf",
                "Apparel: Remove sweaty clothes → drop into laundry bin",
                "Hydration: Empty and rinse water bottle",
            ),
            checklist = ChecklistType.EVENING, module = yoga,
            phase = "Post-Session Unload & Reset — Evening Return", phaseIndex = 1
        )

        // ─────────────────────────────────────────
        // 🚶  MORNING — Walking
        // ─────────────────────────────────────────

        add(
            listOf(
                "Lace up walking shoes",
                "Plan your walking route (aim for 30–60 min)",
                "Grab water bottle",
            ),
            checklist = ChecklistType.MORNING, module = walking,
            phase = "Active Recovery Check", phaseIndex = 0
        )

        // ─────────────────────────────────────────
        // 🚶  EVENING — Walking
        // ─────────────────────────────────────────

        add(
            listOf(
                "Track distance walked (Health app or watch)",
                "Stretch calves and hamstrings (5 min)",
            ),
            checklist = ChecklistType.EVENING, module = walking,
            phase = "Walk Debrief", phaseIndex = 1
        )

        return items
    }
}
