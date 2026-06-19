# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

This is a pure SwiftUI / SwiftData iOS app with no package dependencies and no external build tools.

- **Open:** `open Clearance.xcodeproj` (requires Xcode 16+)
- **Build/run:** `⌘R` in Xcode — pick an iOS 17+ simulator or a physical iPhone
- **Build check (no simulator required):**
  ```
  xcodebuild -project Clearance.xcodeproj -scheme Clearance -destination 'platform=iOS Simulator,name=iPhone 17' build
  ```
- **Target Swift version:** Swift 6 strict concurrency (`SWIFT_VERSION = 6.0`). The build must be **zero-warning** under strict mode.
- No test target exists yet; the README notes the ViewModel logic is side-effect-light and straightforward to cover with XCTest if one is added.

## Architecture

Single-screen MVVM app. One `@MainActor @Observable` view model owns all state; views are purely reactive.

**Data flow:**
1. `ClearanceApp.init` builds the `ModelContainer` (with in-memory fallback on store open failure) and instantiates `ChecklistViewModel`.
2. `ChecklistViewModel` holds `allItems: [ChecklistItem]` fetched from SwiftData. All derived state (`sections`, `progress`, counts) is computed from `allItems` + `selectedChecklist` + today/tomorrow activity sets — no manual `onChange` wiring needed.
3. Every mutation saves the context, fires a haptic, then calls `reloadItems()` to re-fetch.

**Activity gating (the key non-obvious behavior):**
- `ScheduleStore` persists a recurring weekly plan (`[Weekday: Set<ModuleType>]`) and per-date overrides (`[String: Set<ModuleType>]`) in `UserDefaults`.
- `ChecklistViewModel` computes `todayActivities` and `tomorrowActivities` from those on init (and on `refresh()`). Activities are intersected with `enabledModules` so disabled modules can never bleed in via saved overrides.
- Morning items for optional modules are shown when `todayActivities` contains that module (gear-check role).
- Evening items for optional modules split by `phaseIndex`: `0` = pack-for-tomorrow (gated by `tomorrowActivities`), `>0` = post-session unload (gated by `todayActivities`). This lets one evening both unload today's sport and pack tomorrow's different sport.

**Global module enable/disable:**
- `enabledModules: Set<ModuleType>` on the VM (persisted via `ScheduleStore`) controls which optional modules (Gym/Swim/Judo) are visible at all.
- Disabled modules are filtered out of `sections`, `todayActivities`, `tomorrowActivities`, the activity-selector chips, and the schedule editor's weekday grid.
- Toggled via `toggleModuleEnabled(_:)` from the "Active modules" section in `ScheduleEditorView`.
- Default is all modules enabled (backwards compatible with existing installs).

**Auto-reset:**
- `ChecklistViewModel.refresh()` (called on every foreground wake) calls `checkAutoReset()`, which silently clears all tasks if the configured `resetHour` has passed since the last auto-reset.
- `resetHour: Int` (0–6, default 3 = 3 AM) is persisted in UserDefaults and editable from the "Auto-reset" section in `ScheduleEditorView`.
- Manual reset ("Reset now") lives in that same section; it calls `resetAll()` with a haptic and dismisses the sheet.
- `resetAll(silent:)` — pass `silent: true` to skip the haptic (used by auto-reset).

**Seed data versioning:**
- `SeedData.currentVersion` (an `Int` in `SeedData.swift`) gates seeding via `UserDefaults`. Bump it whenever canonical checklist content changes — the old items are deleted and the fresh set is re-inserted on next launch.

**Persistence keys (UserDefaults):**
- `Clearance.seedVersion.v1` — tracks seed version
- `Clearance.weeklySchedule.v1` — JSON-encoded weekly schedule
- `Clearance.activityOverrides.v1` — JSON-encoded per-date overrides (pruned to ±1 day)
- `Clearance.enabledModules.v1` — JSON-encoded set of active optional module rawValues
- `Clearance.autoResetHour.v1` — Int, hour of day for nightly auto-reset (default 3)
- `Clearance.lastAutoReset.v1` — Date, timestamp of last auto-reset (default `.distantPast`)

## Design constraints

- **Swift 6 strict concurrency:** all UI-facing code is `@MainActor`. `ModelContext` and `HapticsManager` must never cross actor boundaries.
- **Synchronized file group:** every file dropped into `Clearance/` is auto-included; no manual project.pbxproj edits needed for new source files.
- **Theming:** `Theme.swift` owns all layout metrics, spring curves, and per-sequence color palettes. Landing (evening) is intentionally true-black regardless of system appearance (`.preferredColorScheme(.dark)` applied at the dashboard level).
- **Accessibility:** VoiceOver labels/values/traits on all interactive elements; Dynamic Type via system text styles; all explicit animations suppressed under Reduce Motion.
