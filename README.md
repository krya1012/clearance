# ✈️ PreFlight

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
- **Swipe to skip** — swipe a row left to skip it for the day (and swipe again to restore it).
- **Reset for tomorrow** — one bottom-anchored control clears completion + skip state across
  *both* sequences.
- **Built for night** — the *Landing* sequence uses a **true-black**, high-contrast palette
  regardless of system appearance.
- **Accessible** — VoiceOver labels/values/traits throughout, Dynamic Type via system text
  styles, and full **Reduce Motion** support.

---

## Status & environment note

This project was authored in a **Linux** environment with **no Swift/Xcode toolchain**, so it
has **not been compiled here**. SwiftUI/SwiftData/UIKit only build on Apple platforms. All code
targets iOS 17+ and is written to compile clean under **Swift 6 language mode** (strict
concurrency). Perform the final build/run on a Mac (see below).

---

## Requirements

| | |
|---|---|
| Xcode | **16.0+** (the project uses an Xcode 16 *synchronized file group*) |
| iOS deployment target | **17.0** |
| Swift language version | **6.0** |

## Building & running

```bash
open PreFlight.xcodeproj      # Xcode 16+
# Choose an iOS 17+ simulator (e.g. iPhone 15) and press ⌘R
```

Because the target uses a synchronized file group, **every file under `PreFlight/` is included
automatically** — just drop new files into the folder and Xcode picks them up.

<details>
<summary>Fallback if the project won't open (e.g. older Xcode)</summary>

1. File ▸ New ▸ Project ▸ iOS App → name **PreFlight**, Interface **SwiftUI**, Storage **SwiftData**.
2. Delete the generated `ContentView.swift` / `Item.swift`.
3. Drag the contents of `PreFlight/` (Models, Views, ViewModels, Helpers, `PreFlightApp.swift`,
   `Assets.xcassets`) into the project — *Copy items if needed*, add to the app target.
4. Set Deployment Target **iOS 17.0** and Swift Language Version **6**.
5. Set `DEVELOPMENT_ASSET_PATHS` to `"PreFlight/Preview Content"` (so previews build).
</details>

---

## Architecture

A small, centralized **MVVM** design on the modern **Observation** framework.

```
PreFlight/
├─ PreFlightApp.swift        @main entry — builds the ModelContainer, owns the ViewModel
├─ Models/
│  ├─ ChecklistType.swift    enum .morning/.evening + Takeoff/Landing display metadata
│  ├─ ModuleType.swift       enum .core/.gym/.swim + optional/sort-order helpers
│  ├─ ChecklistItem.swift    @Model — the single persisted entity
│  └─ SeedData.swift         idempotent first-launch default content
├─ ViewModels/
│  └─ ChecklistViewModel.swift   @MainActor @Observable — single source of truth
├─ Views/
│  ├─ DashboardView.swift    the screen: switch · progress · toggles · list · reset
│  ├─ ChecklistView.swift    grouped List + swipe-to-skip
│  ├─ ChecklistRowView.swift the tactile task row
│  ├─ ProgressHeaderView.swift   animated progress bar
│  └─ ModuleToggleView.swift Gym/Swim quick-toggle chips
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
| `orderIndex` | `Int` | position *within* its (checklist, module) group |
| `isCompleted` | `Bool` | |
| `isSkipped` | `Bool` | swipe-to-skip; cleared on reset |
| `associatedModule` | `ModuleType` | stored as a `String`-backed `Codable` enum |
| `associatedChecklist` | `ChecklistType` | stored as a `String`-backed `Codable` enum |

`SeedData.seedIfNeeded(in:)` runs once on first launch (when the store is empty) and inserts the
default Morning/Evening **Core** items plus **Gym** and **Swim** module items.

### State management

`ChecklistViewModel` is `@MainActor @Observable` and is the single source of truth:

- It fetches `allItems` once and re-fetches after every mutation.
- `sections`, `progress`, and the counts are **computed** from `allItems` + `selectedChecklist`
  + `enabledModules`. Because those inputs are Observation-tracked, switching the sequence or
  toggling a module **recomputes the visible list automatically** — no manual `onChange` wiring.
- Mutations (`toggle`, `skip`/`restore`, `setModule`, `resetAll`) save the context and fire the
  appropriate haptic.
- Enabled modules persist in `UserDefaults` (Core is always forced on). Items are filtered
  in-memory (the dataset is tiny), which sidesteps `#Predicate`-on-enum edge cases.

### Concurrency (Swift 6)

Everything UI-facing is main-actor isolated. `ModelContainer` (which is `Sendable`) is built in
`PreFlightApp.init` — `App` is a `@MainActor` protocol, so constructing the `@MainActor`
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

Open in Xcode 16+, run on an iOS 17 simulator, and confirm:

- [ ] Seed data appears on first launch (Morning/Evening Core).
- [ ] Takeoff ↔ Landing switch updates the list, header, and palette (true-black evening).
- [ ] Toggling Gym/Swim injects/removes their sections with a spring animation.
- [ ] Tapping a row checks it (dim + strikethrough) with a haptic; reaching 100% shows
      "SEQUENCE COMPLETE ✓" and plays a success haptic.
- [ ] The progress bar / percentage / fraction update live.
- [ ] Swipe-left skips a row (badge + dim, excluded from progress); swipe again restores it.
- [ ] "Reset for tomorrow" clears completion + skip across both sequences (after confirmation).
- [ ] Build is **clean with zero warnings** under Swift 6 language mode.
- [ ] Enabling Reduce Motion removes the animations.

> The ViewModel logic (`progress`, `resetAll`, module filtering) is pure and side-effect-light,
> so it's straightforward to cover with an XCTest target if you want automated tests.

---

## License

Created as a demonstration project. Use freely.
