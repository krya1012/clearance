# ✈️ Clearance

**A tactical daily-routine checklist for iOS.** Run a focused **🌅 Takeoff** (morning) and
**🌌 Landing** (evening) sequence, with sport modules you can enable, reorder, or create yourself.
Built to be glanceable, high-contrast, and fast.

> Native SwiftUI · Swift Concurrency · SwiftData · Observation · iOS 17+ · Swift 6

---

## Features

- **Two sequences** — 🌅 *Takeoff* (morning) and 🌌 *Landing* (evening), switched via a
  segmented control.
- **Dynamic activity modules** — ships with 🎯 *Core* (always on), 🏋️ *Gym*, 🏊 *Swim*,
  🥋 *Judo*, 🚴 *Cycling*, and 🏃 *Running*. Modules are **fully user-manageable**: add a new
  sport, rename it, change its emoji, or delete it entirely from the *Manage modules* sheet.
- **Weekly schedule + per-day override** — set which days you train in the schedule editor; tap
  the activity chips on the dashboard to override just today or tomorrow.
- **Unpack today / pack tomorrow** — the evening shows the *unload* tasks for what you did
  **today** and the *pack* tasks for what you're doing **tomorrow**, so you can (e.g.) unpack
  Swim and pack Gym on the same night. The morning then shows that day's gear-check.
- **Glanceable progress** — a minimalist animated progress bar plus a live `completed / total`
  fraction and percentage. Skipped tasks are excluded from the denominator.
- **Tactile rows** — large checkbox targets; completing a task dims it, strikes it through, and
  fires a subtle haptic. Hitting 100% plays a success haptic.
- **Add / edit / delete** — tap **+** to create a task, swipe right to edit, swipe left to skip
  or delete. New tasks slot into the right module and phase automatically.
- **Swipe to skip** — swipe a row left to skip it for the day (swipe again to restore).
- **Auto-reset** — tasks are silently cleared at a configurable hour (default 3 AM) on first
  open after that time. A manual "Reset now" button is in the schedule editor.
- **Built for night** — the *Landing* sequence uses a **true-black**, high-contrast palette
  regardless of system appearance.
- **Accessible** — VoiceOver labels/values/traits throughout, Dynamic Type via system text
  styles, and full **Reduce Motion** support.

---

## Requirements

| | |
|---|---|
| Xcode | **16.0+** (the project uses an Xcode 16 *synchronized file group*) |
| iOS deployment target | **17.0** |
| Swift language version | **6.0** |

> Deploying to a **physical iPhone** requires an Xcode version that supports your phone's iOS
> release. If your iPhone is on a newer iOS than your Xcode knows about, update Xcode — otherwise
> the build succeeds but the app will not install.

## Quick start

```bash
git clone https://github.com/krya1012/clearance.git ~/Clearance
cd ~/Clearance
open Clearance.xcodeproj      # Xcode 16+
```

Because the target uses a synchronized file group, **every file under `Clearance/` is included
automatically** — just drop new files into the folder and Xcode picks them up.

### Run in the Simulator (zero setup)

1. In the toolbar, pick an iOS 17+ simulator (e.g. **iPhone 16**).
2. Press **⌘R**. The first launch seeds the default Takeoff/Landing content.

If no simulator runtime is listed, add one in **Xcode ▸ Settings ▸ Platforms**.

### Build & run from the terminal

**Build only** (verify zero warnings under Swift 6):
```bash
xcodebuild -project Clearance.xcodeproj -scheme Clearance \
  -destination 'generic/platform=iOS' \
  build
```

**Build, install, and launch** on a simulator:
```bash
# 1. Build into a local output folder
xcodebuild -project Clearance.xcodeproj -scheme Clearance \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  -derivedDataPath .build \
  build

# 2. Boot the simulator
open -a Simulator
xcrun simctl boot "iPhone 16" 2>/dev/null; true

# 3. Install and launch
xcrun simctl install booted \
  .build/Build/Products/Debug-iphonesimulator/Clearance.app
xcrun simctl launch booted com.krya1012.clearance.app
```

**Clean build** (useful after schema changes or seed-version bumps):
```bash
xcodebuild -project Clearance.xcodeproj -scheme Clearance \
  -destination 'generic/platform=iOS' \
  clean build
```

To list all available simulator names: `xcrun simctl list devices available`.

### Run on your iPhone

A successful **build** is not the same as a successful **install + launch**. On a physical
device you also need valid signing and a phone set up for development:

1. **Set a signing team.** Select the **Clearance** target ▸ **Signing & Capabilities** ▸ enable
   **Automatically manage signing** ▸ pick your **Team**. A free Apple ID ("Personal Team") works.
2. **Select your iPhone** as the run destination (not *"Any iOS Device (arm64)"*, which is
   build-only).
3. **Enable Developer Mode on the iPhone** (required on iOS 16+):
   **Settings ▸ Privacy & Security ▸ Developer Mode ▸ On**, then restart the phone and confirm.
4. Press **⌘R**.
5. **Trust the developer certificate** (first install only): **Settings ▸ General ▸ VPN & Device
   Management**, tap your developer profile, tap **Trust**.

> **Free-account note:** apps signed with a free Personal Team stop launching after **7 days**.
> Just re-run from Xcode to refresh the provisioning.

### App icon

The bundled `AppIcon` is an empty placeholder. Drop a 1024×1024 PNG into
`Clearance/Assets.xcassets/AppIcon.appiconset` to give it one.

<details>
<summary>Fallback if the project won't open (e.g. older Xcode)</summary>

1. File ▸ New ▸ Project ▸ iOS App → name **Clearance**, Interface **SwiftUI**, Storage **SwiftData**.
2. Delete the generated `ContentView.swift` / `Item.swift`.
3. Drag the contents of `Clearance/` (Models, Views, ViewModels, Helpers, `ClearanceApp.swift`,
   `Assets.xcassets`) into the project — *Copy items if needed*, add to the app target.
4. Set Deployment Target **iOS 17.0** and Swift Language Version **6**.
5. Set `DEVELOPMENT_ASSET_PATHS` to `"Clearance/Preview Content"` (so previews build).
</details>

---

## Architecture

A small, centralized **MVVM** design on the modern **Observation** framework.

```
Clearance/
├─ ClearanceApp.swift        @main entry — builds ModelContainer, owns the ViewModel
├─ Models/
│  ├─ ChecklistType.swift    enum .morning/.evening + Takeoff/Landing display metadata
│  ├─ ActivityModule.swift   @Model — user-managed sport module (name, emoji, sortOrder, isCore)
│  ├─ Weekday.swift          enum Sun…Sat aligned with Calendar; drives the schedule
│  ├─ ChecklistItem.swift    @Model — the single persisted task entity
│  └─ SeedData.swift         versioned first-launch default modules + content (version 7)
├─ ViewModels/
│  └─ ChecklistViewModel.swift   @MainActor @Observable — single source of truth
├─ Views/
│  ├─ DashboardView.swift        the screen: switch · progress · activity pickers · list
│  ├─ ChecklistView.swift        grouped List + phase sub-headers + swipe actions
│  ├─ ChecklistRowView.swift     the tactile task row
│  ├─ ProgressHeaderView.swift   animated progress bar
│  ├─ ActivitySelectorView.swift today / done-today / tomorrow activity chips
│  ├─ ScheduleEditorView.swift   weekly plan + active modules + auto-reset (sheet)
│  ├─ ModuleManagerView.swift    add / rename / delete sport modules (sheet)
│  └─ ItemEditorView.swift       add / edit task sheet
├─ Helpers/
│  ├─ Haptics.swift          @MainActor wrapper over UIKit feedback generators
│  ├─ Theme.swift            color palettes (true-black evening) + layout/motion tokens
│  ├─ ScheduleStore.swift    UserDefaults-backed weekly schedule, per-day overrides, enabled IDs
│  └─ Extensions.swift       Color(hex:)
└─ Assets.xcassets/          AccentColor (teal), AppIcon
```

### Data model (SwiftData)

Two persisted entities:

**`ActivityModule`**

| Property | Type | Notes |
|---|---|---|
| `id` | `UUID` | `@Attribute(.unique)` |
| `name` | `String` | user-editable |
| `emoji` | `String` | user-editable |
| `sortOrder` | `Int` | display order |
| `isCore` | `Bool` | `true` for the one permanent Core module |

**`ChecklistItem`**

| Property | Type | Notes |
|---|---|---|
| `id` | `UUID` | `@Attribute(.unique)` |
| `title` | `String` | |
| `orderIndex` | `Int` | position within its (checklist, module, phase) group |
| `isCompleted` | `Bool` | |
| `isSkipped` | `Bool` | swipe-to-skip; cleared on reset |
| `phase` / `phaseIndex` | `String` / `Int` | named sub-section within a module |
| `associatedModule` | `String` | `ActivityModule.id.uuidString` |
| `associatedChecklist` | `ChecklistType` | stored as a `String`-backed `Codable` enum |

`SeedData.seedIfNeeded(in:)` is **versioned** — bumping `currentVersion` deletes all existing
modules and items, then re-seeds the canonical set on next launch.

`ClearanceApp.init` builds the persistent `ModelContainer`; if the on-device store ever fails to
open (e.g. an incompatible store from an earlier install), it **falls back to an in-memory store**
so the app still launches with clean content instead of crashing.

### State management

`ChecklistViewModel` is `@MainActor @Observable` and is the single source of truth:

- It fetches `allModules` and `allItems` once and re-fetches after every mutation.
- `sections`, `progress`, and the counts are **computed** from `allItems` + `selectedChecklist`
  + `todayActivityIDs` / `tomorrowActivityIDs` + `enabledModuleIDs`. Switching the sequence,
  toggling an activity, or enabling/disabling a module **recomputes the visible list
  automatically** — no manual `onChange` wiring.
- Activity gating by `phaseIndex`: evening `phaseIndex == 0` = pack for tomorrow (gated by
  `tomorrowActivityIDs`); `phaseIndex > 0` = post-session unload (gated by `todayActivityIDs`).
- `enabledModuleIDs: Set<UUID>` and the weekly schedule / per-day overrides are persisted via
  `ScheduleStore` (UserDefaults, v2 keys using UUID strings).
- Module CRUD (`addModule`, `updateModule`, `deleteModule`) lives on the VM; deleting a module
  also deletes all its tasks.

### Concurrency (Swift 6)

Everything UI-facing is main-actor isolated. `ModelContainer` (which is `Sendable`) is built in
`ClearanceApp.init` — `App` is a `@MainActor` protocol, so constructing the `@MainActor`
`ChecklistViewModel` and using `container.mainContext` there is safe under strict concurrency. The
non-`Sendable` `ModelContext` / `HapticsManager` never cross actor boundaries.

### Design system

`Theme` centralizes layout metrics, spring/motion curves, and a per-sequence `ChecklistPalette`.
*Takeoff* follows the system appearance; *Landing* is intentionally **true black** for nighttime
use, and the dashboard applies `.preferredColorScheme(.dark)` while it's active.

### Accessibility

- VoiceOver: rows, chips, and the progress header expose labels, values, traits, and hints.
- Dynamic Type: all text uses system text styles.
- Reduce Motion: every explicit animation is suppressed when the user enables it.

---

## Verification checklist (on a Mac)

Open in Xcode 16+, run on an iOS 17+ simulator or your iPhone, and confirm:

- [ ] Seed data appears on first launch (Morning/Evening Core + 5 sport modules).
- [ ] Takeoff ↔ Landing switch updates the list, header, and palette (true-black evening).
- [ ] Tapping activity chips injects/removes sport sections with a spring animation.
- [ ] Weekly schedule (calendar icon) correctly gates activities by day.
- [ ] Morning shows the gear-check for today's activities; evening shows unload (today) and pack (tomorrow).
- [ ] Tapping a row checks it (dim + strikethrough) with a haptic; reaching 100% plays a success haptic.
- [ ] The **+** button adds a task; swipe-right edits; swipe-left skips/deletes.
- [ ] Progress bar / percentage / fraction update live.
- [ ] Schedule editor ▸ "Manage modules" — add a new module, rename one, delete one; changes appear immediately everywhere.
- [ ] Auto-reset clears tasks on first open after the configured hour.
- [ ] "Reset now" in the schedule editor clears both sequences.
- [ ] Build is **clean with zero warnings** under Swift 6 language mode.
- [ ] Enabling Reduce Motion removes the animations.

---

## License

Copyright © 2026 Yaraslau Krautsou. Licensed under
**[CC BY-NC 4.0](LICENSE.md)** (Creative Commons Attribution-NonCommercial 4.0 International).

You are free to use, copy, adapt, and redistribute Clearance for any **noncommercial** purpose,
provided you give appropriate **credit** and indicate if changes were made.
**Commercial use is not permitted.** See [`LICENSE.md`](LICENSE.md) for details.
