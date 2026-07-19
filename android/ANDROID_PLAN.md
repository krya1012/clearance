# Clearance for Android — updated implementation plan (mirrors iOS v12)

## Status

**Milestones 1–2 (scaffold + data layer) are done** — see `android/app/src/main/java/com/krya1012/clearance/data/` and `domain/`. The data model, `SeedData`, `ScheduleStore`, `PeriodicTaskStore`, and `TemplateCatalog` described below already exist in code and are the source of truth if this doc and the code ever disagree. The `ViewModel`/`UI detail` sections below still describe **not-yet-built** work (milestones 3+).

## Context

The original Android plan targeted a much simpler iOS app: compile-time `ModuleType` enum
with only Gym/Swim, no weekly schedule, seed version 4. The iOS app has since grown
substantially. This plan reflects the current iOS feature set that the Android port must
match.

**What changed since the old plan:**
- `ModuleType` enum → dynamic `ActivityModule` (Room `@Entity`); users add/rename/emoji/delete modules
- 8 default modules: Core, Gym, Swim, Judo, Cycling, Running, Yoga, Walking (seed v12)
- Weekly schedule: per-weekday recurring plan (`Map<Int, Set<UUID>>`)
- Per-day overrides: one-day exceptions for today/tomorrow
- Activity gating: morning = gear-check for today's activities; evening phase 0 = pack for
  tomorrow, phase >0 = unload today
- "Restore default tasks" per module
- Activity selector rows scroll inside the main list (not fixed above it)
- Schedule editor: day × module grid, active-modules list with drag reorder, auto-reset time
- Periodic tasks (weekly/monthly recurring, e.g. Laundry/Shopping/Budget/Post) + a template
  library for quickly re-adding a deleted sport module
- Seed version: **12**

---

## Stack decisions (unchanged from old plan)

- **Location:** `android/` directory in the repo root; push on `main`
- **Language / UI:** Kotlin, Jetpack Compose, Material 3
- **Persistence:** Room (tasks + modules), DataStore Preferences (schedule, overrides,
  enabled modules, seed version, reset config)
- **Architecture:** `ViewModel` + `StateFlow`; no DI framework
- **applicationId:** `com.krya1012.clearance`
- **SDK:** `minSdk 26`, `compileSdk`/`targetSdk 35`, Kotlin 2.x + Compose compiler plugin
- **Extra dependency (new):** `sh.calvin.reorderable:reorderable` (Compose drag-to-reorder
  for the active-modules list); it is small and focused — no DI or codegen required

---

## Project structure (under `android/`)

```
android/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradle/wrapper/gradle-wrapper.properties
├─ gradlew, gradlew.bat
└─ app/
   ├─ build.gradle.kts
   ├─ proguard-rules.pro
   └─ src/main/
      ├─ AndroidManifest.xml
      └─ java/com/krya1012/clearance/
         ├─ ClearanceApplication.kt          # Room DB + DataStore singletons (app-scoped)
         ├─ MainActivity.kt                  # setContent { ClearanceTheme { DashboardScreen(vm) } }
         │
         ├─ data/
         │  ├─ ActivityModule.kt             # @Entity id:String name emoji sortOrder isCore
         │  ├─ ChecklistItem.kt              # @Entity id title orderIndex isCompleted isSkipped
         │  │                               #         phase phaseIndex associatedModule associatedChecklist
         │  ├─ ChecklistType.kt             # enum MORNING/EVENING + label/emoji
         │  ├─ Weekday.kt                   # enum (SUN=1…SAT=7, matches Calendar.DAY_OF_WEEK)
         │  ├─ ActivityModuleDao.kt         # Flow<List<ActivityModule>> + CRUD
         │  ├─ ChecklistItemDao.kt          # Flow<List<ChecklistItem>> + CRUD
         │  ├─ ClearanceDatabase.kt         # @Database(entities=[ActivityModule,ChecklistItem])
         │  ├─ ScheduleStore.kt             # DataStore: weeklySchedule, activityOverrides,
         │  │                               #            enabledModules, resetHour, lastAutoReset
         │  ├─ PeriodicTask.kt              # weekly/monthly recurring task, Recurrence enum
         │  ├─ PeriodicTaskStore.kt         # DataStore: periodic tasks JSON + seed version
         │  ├─ TemplateCatalog.kt           # pre-built module templates (Manage modules → add)
         │  └─ SeedData.kt                  # v12 seed: 8 modules + full task content
         │
         ├─ domain/
         │  ├─ ActivityGating.kt            # pure gating rules (role-of/isVisible/activitiesFor)
         │  └─ AutoReset.kt                 # pure reset-threshold computation
         │
         ├─ vm/
         │  └─ ChecklistViewModel.kt        # all reactive state + business logic (see below)
         │
         ├─ ui/
         │  ├─ DashboardScreen.kt           # top bar + progress header + LazyColumn
         │  ├─ ActivitySelectorSection.kt   # TODAY / DONE TODAY / PACKING FOR TOMORROW rows
         │  ├─ ChecklistSectionContent.kt   # module header → phase subheader → task rows
         │  ├─ ChecklistRow.kt              # checkbox, title, dim/strikethrough, SKIPPED badge
         │  ├─ ProgressHeaderView.kt        # animated LinearProgressIndicator + label
         │  ├─ ScheduleEditorSheet.kt       # ModalBottomSheet: active modules + weekly grid +
         │  │                               #                  auto-reset + reset-now
         │  ├─ WeeklyPlanGrid.kt            # day × module matrix (tap cell to toggle)
         │  ├─ ModuleManagerSheet.kt        # add / rename / delete / reorder modules
         │  ├─ ItemEditorSheet.kt           # add / edit task (title, checklist, module, phase)
         │  └─ theme/
         │     ├─ Color.kt
         │     ├─ Theme.kt                  # ClearanceTheme; follows system light/dark (evening true-black in dark mode)
         │     └─ Type.kt
         │
         └─ util/
            └─ Haptics.kt                   # wrapper over View.performHapticFeedback / VibrationEffect
```

---

## Data layer detail

### `ActivityModule` (Room @Entity)
```kotlin
@Entity(tableName = "modules")
data class ActivityModule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val sortOrder: Int,
    val isCore: Boolean = false,
    val isLocked: Boolean = false,
    val activityTypeRaw: String = ActivityType.SPORT.name,
)
val ActivityModule.activityType get() = ActivityType.fromRawValue(activityTypeRaw)
val ActivityModule.isOptional get() = !isCore
val ActivityModule.label get() = "$emoji $name"
```

### `ChecklistItem` (Room @Entity)
```kotlin
@Entity(tableName = "items")
data class ChecklistItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val orderIndex: Int,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val phase: String,
    val phaseIndex: Int,
    val associatedModule: String,   // ActivityModule.id (UUID string)
    val associatedChecklist: String // ChecklistType.name
)
```

### `ScheduleStore` (DataStore Preferences)

Stores all transient schedule state — the Android analog of iOS `ScheduleStore.swift`:

| Key | Type | Content |
|-----|------|---------|
| `clearance.seedVersion.v1` | `Int` | seed version (current 12, default 0) |
| `clearance.weeklySchedule.v2` | `String` (JSON) | `Map<Int, List<String>>` weekday → module UUIDs |
| `clearance.activityOverrides.v2` | `String` (JSON) | `Map<String, List<String>>` dateKey → module UUIDs |
| `clearance.enabledModules.v2` | `String` (JSON) | `List<String>` of enabled optional module UUIDs |
| `clearance.autoResetHour.v1` | `Int` | hour 0–6 (default 3) |
| `clearance.lastAutoReset.v1` | `Long` | epoch ms of last auto-reset |

`dateKey` format: `"yyyy-MM-dd"` in the device's local timezone — matching iOS.

### `SeedData` (v12)

Same 8 modules and full task content as `ios/Clearance/Models/SeedData.swift`.
`seedIfNeeded(context, moduleDao, itemDao, scheduleStore)` is called from
`ClearanceApplication.onCreate` (on a coroutine). Migration is **additive** — matches seed
modules by name against existing records:
1. Read `seedVersion` from DataStore
2. If `< 12`: for each seed module, find existing by name (keep id) or insert new; insert
   seed items only for modules with zero items; add new optional module IDs to
   `enabledModules` without clearing other schedule prefs; save version 12
3. One-time migrations (deprecated-module cleanup; `Rest`→`Walking` rename) are gated to the
   specific version each was introduced at (10 and 11), not to the general version check —
   otherwise they'd re-run on every future bump against a user's own later module reusing one
   of those names

**Not yet ported from iOS:** `Clearance.swift`'s recovery path — if the store ever ends up
with zero modules (e.g. after a corrupted/unopenable database), reseed regardless of the
version key and clear `ScheduleStore`'s module-keyed prefs first. `ClearanceDatabase.kt`
currently has no corruption-recovery path at all (an unhandled `Room.databaseBuilder(...)`
failure crashes rather than degrading), so this doesn't yet have anywhere to hook in on
Android — tracked as a separate gap from the seed-migration logic itself.

---

## ViewModel detail

```kotlin
class ChecklistViewModel(
    private val moduleDao: ActivityModuleDao,
    private val itemDao: ChecklistItemDao,
    private val scheduleStore: ScheduleStore
) : ViewModel() {

    val selectedChecklist = MutableStateFlow(ChecklistType.MORNING)
    val allModules: StateFlow<List<ActivityModule>>   // moduleDao.observeAll()
    val allItems: StateFlow<List<ChecklistItem>>      // itemDao.observeAll()
    val enabledModuleIDs: StateFlow<Set<String>>      // from ScheduleStore
    val weeklySchedule: StateFlow<Map<Int, Set<String>>>
    val activityOverrides: MutableStateFlow<Map<String, Set<String>>>
    val resetHour: StateFlow<Int>

    // Derived (combine)
    val todayActivityIDs: StateFlow<Set<String>>
    val tomorrowActivityIDs: StateFlow<Set<String>>
    val sections: StateFlow<List<ChecklistSection>>  // module→phase→items, gated + sorted
    val progress: StateFlow<Double>
    val completedCount: StateFlow<Int>
    val totalActiveCount: StateFlow<Int>
}
```

**Activity gating** mirrors iOS exactly:
- `todayActivityIDs` = `overrides[todayKey] ?: weeklySchedule[Calendar.DAY_OF_WEEK]`, intersected with `enabledModuleIDs`
- `tomorrowActivityIDs` = same for tomorrow's date
- Morning items: shown when `todayActivityIDs.contains(item.associatedModule)`
- Evening items: `phaseIndex == 0` gated by `tomorrowActivityIDs`; `phaseIndex > 0` gated by `todayActivityIDs`

**Stale-UUID reconciliation on init** (same logic as iOS):
```kotlin
val saved = scheduleStore.loadEnabledModuleIDs()
val optionalIDs = allModules.filter { it.isOptional }.map { it.id }.toSet()
val validIDs = saved.intersect(optionalIDs)
enabledModuleIDs = if (validIDs.isEmpty() && optionalIDs.isNotEmpty()) optionalIDs else validIDs
```

**Auto-reset** — called on every `onResume` equivalent (via `Lifecycle.Event.ON_RESUME`):
compare `lastAutoReset` epoch vs today's `resetHour` threshold; if passed, call `resetAll(silent=true)`.

**Restore default tasks**:
```kotlin
fun restoreDefaultTasks(module: ActivityModule) {
    viewModelScope.launch {
        itemDao.deleteByModule(module.id)
        SeedData.defaultItemsFor(module, allModules.value).forEach { itemDao.insert(it) }
    }
}
fun hasDefaultTasks(module: ActivityModule): Boolean =
    SeedData.defaultItemsFor(module, allModules.value).isNotEmpty()
```

---

## UI detail

### `DashboardScreen`

Top bar: `SegmentedButton` (Takeoff / Landing) | calendar icon (opens schedule sheet) |
`+` icon (opens add-task sheet). No reorder/edit button.

Body: single `LazyColumn`:
1. **Activity selector items** (scroll away with content): one `LazyListScope.items` block
   per selector section (TODAY / DONE TODAY / PACKING FOR TOMORROW) — tapping a row calls
   `toggleTodayActivity` / `toggleTomorrowActivity`.
2. **Task sections**: for each `ChecklistSection`, a `stickyHeader` with module emoji+name,
   then for each `ChecklistPhase` an optional phase sub-header row (non-interactive) followed
   by task rows.
3. Bottom spacer (88 dp) for the FAB.

### `ScheduleEditorSheet` (ModalBottomSheet)

Sections:
1. **Active modules** — `LazyColumn` with `ReorderableItem` (from `sh.calvin.reorderable`)
   so drag handles are always visible; tap a row toggles `enabledModuleIDs`. "Manage modules"
   button opens `ModuleManagerSheet`.
2. **Auto-reset** — `Slider` or `DropdownMenu` for hour 0–6; "Reset now" button.
3. **Weekly plan** — `WeeklyPlanGrid`: 7 rows (Mon–Sun) × N columns (one per enabled module);
   each cell is an `IconButton` toggling `weeklySchedule[day][moduleId]`.

### `ModuleManagerSheet`

List of all optional modules with:
- Tap → open rename/emoji editor (AlertDialog or separate sheet)
- Swipe right (leading) → "Restore defaults" (if `hasDefaultTasks`) with confirmation dialog
- Swipe left (trailing) → Delete with confirmation dialog
- Drag handle (reorderable list) → `moveModule` updates sortOrder

Core module shown in a separate non-editable row at the top; swipe left on it shows "Restore defaults".

### Theming

Both sequences follow the system's light/dark appearance setting (`isSystemInDarkTheme()`), same as iOS's `@Environment(\.colorScheme)` — no custom day/night logic. Evening (`ChecklistType.EVENING`) uses true-black (`Color(0xFF000000)`) specifically in dark mode, not regardless of system setting.
Morning uses a warm amber accent (`#F59E0B`/`#FBBF24`); evening uses teal (`#2DD4BF`/`#0D9488`).

---

## Behavior mapping: iOS → Android

| iOS | Android |
|-----|---------|
| `@Observable` VM + computed `sections` | `ViewModel` + `combine(allModules, allItems, ...)` → `StateFlow<List<ChecklistSection>>` |
| `UserDefaults` / `ScheduleStore` | DataStore Preferences + JSON for maps |
| SwiftUI `List` `.onMove` | `ReorderableLazyColumn` (sh.calvin.reorderable) |
| SwiftUI swipe leading/trailing | Compose `SwipeToDismissBox` for delete; leading swipe for Restore |
| `HapticsManager` | `View.performHapticFeedback(HapticFeedbackConstants.*)` |
| `@Environment(\.colorScheme)` (both sequences follow system light/dark) | `isSystemInDarkTheme()` (both sequences follow system light/dark) |
| `SeedData.currentVersion = 12` | `SeedData.CURRENT_VERSION = 12` |
| `ActivityModule.id.uuidString` | `UUID.randomUUID().toString()` |
| Weekday rawValue 1(Sun)…7(Sat) | `Calendar.DAY_OF_WEEK` values (identical mapping) |

---

## Execution phases

1. **Scaffold** — Gradle files, manifest, Application, MainActivity, empty `DashboardScreen`,
   theme (Color/Type/Theme). Extend `.gitignore` for Android artifacts. Build compiles. Commit.

2. **Data layer** — `ActivityModule`, `ChecklistItem`, DAOs, `ClearanceDatabase`,
   `ScheduleStore`, `PeriodicTaskStore`, `TemplateCatalog`, `SeedData` (v12 full content),
   `domain/` pure functions + unit tests. Unit-testable with no UI. **Done.**

3. **ViewModel** — all `StateFlow`s, activity gating, CRUD, toggle/skip/reset, module
   management, restore-defaults, auto-reset logic, stale-UUID reconciliation. Commit.

4. **Core UI** — `DashboardScreen` with `LazyColumn` (activity selector + task sections +
   progress header + top bar), `ChecklistRow`, `ProgressHeaderView`. Takeoff/Landing switch
   works; tasks toggle; progress updates. Commit.

5. **Schedule & module management** — `ScheduleEditorSheet`, `WeeklyPlanGrid`,
   `ModuleManagerSheet`, `ItemEditorSheet`. Full CRUD on modules and tasks. Drag reorder.
   Swipe Restore + Delete. Confirm dialogs. Commit.

6. **Polish** — dark-mode true-black evening polish, haptics, Compose animations
   (`AnimatedVisibility`, `animateItemPlacement`), content descriptions (accessibility),
   adaptive launcher icon (neon-green checkmark + airplane), `android/README.md`. Commit.

Push to `main`.

---

## Verification (on user's machine)

1. Open `android/` in **Android Studio Hedgehog+**; let Gradle sync.
2. Run on Pixel emulator API 34 or a physical Android device with USB debugging.
3. Check parity with iOS:
   - All 8 seed modules appear; tasks grouped by module → phase
   - Takeoff/Landing switch updates list and palette; toggling system Light/Dark Mode updates both sequences' palettes, with Landing true-black only in dark mode
   - Activity selector (TODAY / DONE TODAY / PACKING FOR TOMORROW) scrolls with the list
   - Tapping a module row in the selector toggles it; correct tasks appear / disappear
   - Weekly plan grid in Schedule toggles per-weekday; next-day open reflects it
   - Add/edit/delete tasks; add/rename/emoji-change/delete modules
   - Restore default tasks (swipe right) resets a module's tasks
   - Auto-reset at configured hour; "Reset now" clears both sequences
   - Progress bar animates to 100% and shows completion message
   - Haptics fire on toggle, skip, module-toggle, completion, reset

---

## Notes / caveats

- The Gradle **wrapper jar** (`gradle-wrapper.jar`) can't be authored as text; Android Studio
  regenerates it on first sync, or run `gradle wrapper` once with a local Gradle install.
- `reorderable` library (`sh.calvin.reorderable`) is the only added dependency beyond the
  standard Jetpack stack; it is MIT-licensed and actively maintained.
- DataStore JSON serialization for the schedule maps uses `kotlinx.serialization` (already
  pulled in as a transitive dependency of many Jetpack libs; add explicit dep if not present).
- One intentional UX divergence: edit/skip live in a **⋮ overflow menu** on each task row
  rather than bidirectional swipe, because Compose's `SwipeToDismissBox` cleanly supports
  only one dismiss direction.
