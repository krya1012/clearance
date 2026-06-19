# ✈️ Clearance

**A tactical daily-routine checklist for iOS.** Run a focused **🌅 Takeoff** (morning) and
**🌌 Landing** (evening) sequence, with optional **🏋️ Gym** and **🏊 Swim** modules you can
inject on demand. Built to be glanceable, high-contrast, and fast.

> Native SwiftUI · Swift Concurrency · SwiftData · Observation · iOS 17+ · Swift 6

---

## Features

- **Two sequences** — 🌅 *Takeoff* (morning) and 🌌 *Landing* (evening), switched via a
  segmented control.
- **Optional modules** — flip 🏋️ *Gym* / 🏊 *Swim* on and their tasks are injected into (or
  removed from) the active list instantly, with a spring animation. *Core* is always on.
- **Glanceable progress** — a minimalist, animated progress bar plus a live `completed / total`
  fraction and percentage. Skipped tasks are excluded from the denominator.
- **Tactile rows** — large checkbox targets; completing a task dims it, strikes it through, and
  fires a subtle haptic. Hitting 100% plays a success haptic.
- **Add / edit / delete** — tap **+** to create a task, swipe right to edit, swipe left to skip
  or delete. New and edited tasks slot into the right module and phase.
- **Swipe to skip** — swipe a row left to skip it for the day (and swipe again to restore it).
- **Reset for tomorrow** — one bottom-anchored control clears completion + skip state across
  *both* sequences.
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
> release. If your iPhone is on a newer iOS than your Xcode knows about, update Xcode (the Mac
> App Store has the current release) — otherwise the build succeeds but the app will not install.

## Quick start

```bash
git clone https://github.com/krya1012/flight-checklist.git ~/Clearance
cd ~/Clearance
open Clearance.xcodeproj      # Xcode 16+
```

Because the target uses a synchronized file group, **every file under `Clearance/` is included
automatically** — just drop new files into the folder and Xcode picks them up.

### Run in the Simulator (zero setup)

1. In the toolbar, pick an iOS 17+ simulator (e.g. **iPhone 16**).
2. Press **⌘R**. The first launch seeds the default Takeoff/Landing content.

If no simulator runtime is listed, add one in **Xcode ▸ Settings ▸ Platforms**.

### Run on your iPhone

A successful **build** is not the same as a successful **install + launch**. On a physical
device you also need valid signing and a phone that is set up for development:

1. **Set a signing team.** Select the **Clearance** target ▸ **Signing & Capabilities** ▸ enable
   **Automatically manage signing** ▸ pick your **Team**. A free Apple ID ("Personal Team") works.
   If the bundle identifier is taken, change it to something unique (e.g.
   `com.<your-name>.clearance.app`).
2. **Select your iPhone** as the run destination (not *"Any iOS Device (arm64)"*, which is
   build-only — Run stays greyed out on it).
3. **Enable Developer Mode on the iPhone** (required on iOS 16+):
   **Settings ▸ Privacy & Security ▸ Developer Mode ▸ On**, then **restart** the phone and
   confirm. Until this is on, Xcode cannot launch the app.
4. Press **⌘R**.
5. **Trust the developer certificate** (first install only): after the app appears on the home
   screen, go to **Settings ▸ General ▸ VPN & Device Management**, tap your developer profile,
   and tap **Trust**. (This entry only appears *after* the app has installed.)

> **Free-account note:** apps signed with a free Personal Team stop launching after **7 days**.
> Just re-run from Xcode to refresh the provisioning.

### App icon

The bundled `AppIcon` is an empty placeholder, so the app shows a **blank icon** on the home
screen. Drop a 1024×1024 PNG into `Clearance/Assets.xcassets/AppIcon.appiconset` to give it one.

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
├─ ClearanceApp.swift        @main entry — builds the ModelContainer, owns the ViewModel
├─ Models/
│  ├─ ChecklistType.swift    enum .morning/.evening + Takeoff/Landing display metadata
│  ├─ ModuleType.swift       enum .core/.gym/.swim + optional/sort-order helpers
│  ├─ ChecklistItem.swift    @Model — the single persisted entity
│  └─ SeedData.swift         versioned first-launch default content
├─ ViewModels/
│  └─ ChecklistViewModel.swift   @MainActor @Observable — single source of truth
├─ Views/
│  ├─ DashboardView.swift    the screen: switch · progress · toggles · list · reset
│  ├─ ChecklistView.swift    grouped List + phase sub-headers + swipe actions
│  ├─ ChecklistRowView.swift the tactile task row
│  ├─ ProgressHeaderView.swift   animated progress bar
│  ├─ ModuleToggleView.swift Gym/Swim quick-toggle chips
│  └─ ItemEditorView.swift   add / edit task sheet
├─ Helpers/
│  ├─ Haptics.swift          @MainActor wrapper over UIKit feedback generators
│  ├─ Theme.swift            color palettes (true-black evening) + layout/motion tokens
│  └─ Extensions.swift       Color(hex:)
└─ Assets.xcassets/          AccentColor (teal), AppIcon placeholder
```

### Data model (SwiftData)

`ChecklistItem` is the only persisted entity:

| Property | Type | Notes |
|---|---|---|
| `id` | `UUID` | `@Attribute(.unique)` |
| `title` | `String` | |
| `orderIndex` | `Int` | position *within* its (checklist, module, phase) group |
| `isCompleted` | `Bool` | |
| `isSkipped` | `Bool` | swipe-to-skip; cleared on reset |
| `phase` / `phaseIndex` | `String` / `Int` | named sub-section within a module |
| `associatedModule` | `ModuleType` | stored as a `String`-backed `Codable` enum |
| `associatedChecklist` | `ChecklistType` | stored as a `String`-backed `Codable` enum |

`SeedData.seedIfNeeded(in:)` is **versioned** — bumping `currentVersion` deletes the old items
and inserts the fresh canonical Morning/Evening **Core** content plus the **Gym** / **Swim**
module items on the next launch.

`ClearanceApp.init` builds the persistent `ModelContainer`; if the on-device store ever fails to
open (e.g. an incompatible store from an earlier install), it **falls back to an in-memory store**
so the app still launches with clean content instead of crashing on the loading screen.

### State management

`ChecklistViewModel` is `@MainActor @Observable` and is the single source of truth:

- It fetches `allItems` once and re-fetches after every mutation.
- `sections`, `progress`, and the counts are **computed** from `allItems` + `selectedChecklist`
  + `enabledModules`. Because those inputs are Observation-tracked, switching the sequence or
  toggling a module **recomputes the visible list automatically** — no manual `onChange` wiring.
- Mutations (`addItem`, `updateItem`, `deleteItem`, `toggle`, `skip`/`restore`, `setModule`,
  `resetAll`) save the context and fire the appropriate haptic.
- Enabled modules persist in `UserDefaults` (Core is always forced on). Items are filtered
  in-memory (the dataset is tiny), which sidesteps `#Predicate`-on-enum edge cases.

### Concurrency (Swift 6)

Everything UI-facing is main-actor isolated. `ModelContainer` (which is `Sendable`) is built in
`ClearanceApp.init` — `App` is a `@MainActor` protocol, so constructing the `@MainActor`
`ChecklistViewModel` and using `container.mainContext` there is safe under strict concurrency. The
non-`Sendable` `ModelContext`/`HapticsManager` never cross actor boundaries.

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

- [ ] Seed data appears on first launch (Morning/Evening Core).
- [ ] Takeoff ↔ Landing switch updates the list, header, and palette (true-black evening).
- [ ] Toggling Gym/Swim injects/removes their sections with a spring animation.
- [ ] Tapping a row checks it (dim + strikethrough) with a haptic; reaching 100% shows
      "SEQUENCE COMPLETE ✓" and plays a success haptic.
- [ ] The **+** button adds a task; swipe-right edits; swipe-left skips/deletes.
- [ ] The progress bar / percentage / fraction update live.
- [ ] "Reset for tomorrow" clears completion + skip across both sequences (after confirmation).
- [ ] Build is **clean with zero warnings** under Swift 6 language mode.
- [ ] Enabling Reduce Motion removes the animations.

> The ViewModel logic (`progress`, `resetAll`, module filtering) is pure and side-effect-light,
> so it's straightforward to cover with an XCTest target if you want automated tests.

---

## License

Created as a demonstration project. Use freely.
</content>
</invoke>
