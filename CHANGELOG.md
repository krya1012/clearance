# Changelog

All notable changes to Clearance are recorded here.

---

## [Unreleased]

### Added
- **Dynamic sport modules** — Gym, Swim, Judo, Cycling, Running are now SwiftData
  `ActivityModule` objects instead of a compile-time enum. Users can add, rename, change
  emoji, or delete any optional module via Schedule → Manage modules.
- **Cycling (🚴) and Running (🏃) modules** with full morning gear-check and evening
  pack/unload task content (Velo-Pack and Stride-Pack checklists).
- **Weekly schedule grid** — Schedule sheet shows a compact day × module matrix instead
  of per-day chip rows; tap any cell to toggle a module on/off for that weekday.
- **Activity selector inside the task list** — today's / tomorrow's activity toggle rows
  now live at the top of the scrollable task list so they scroll off-screen rather than
  occupying fixed space above it.
- **Module reorder in Schedule** — drag handles are always visible in Schedule →
  Active modules; drag any row to change the module display order without a separate
  "Reorder" mode button.
- **Judo (🥋) module** with morning gear-check (Dojo-Pack) and evening pack/unload tasks.
- **App icon** — 1024 × 1024 neon-green checkmark + airplane mark.

### Changed
- **Dashboard top bar** simplified: pencil/edit button removed. Bar now shows only the
  Takeoff / Landing picker, the calendar shortcut, and the + add-task button.
- **Active modules row layout** in Schedule uses a circle/checkmark indicator on the
  left, leaving the right side free for the drag handle.
- **Manage modules** sheet — "Reorder" toolbar button removed; reordering is done
  directly from Schedule → Active modules.
- Seed data version bumped to **7**; first launch after update re-seeds all modules and
  tasks with fresh UUIDs and clears stale UserDefaults module keys.

### Fixed
- Task drag-to-reorder was silently broken: phase sub-header rows were rendered as
  standalone List rows interleaved with `ForEach`+`.onMove` blocks, causing SwiftUI's
  drag-index tracker to mis-map drop positions. Fixed by wrapping each sub-header in a
  single-item `ForEach` with `.moveDisabled(true)`.
- Stale module UUIDs in UserDefaults after a re-seed caused all optional modules to
  appear disabled on first launch. Fixed by calling `ScheduleStore.clearModuleKeys()`
  before re-seeding and by reconciling saved IDs against live module UUIDs on VM init.
- License name corrected from "krya1012" to "Yaraslau Krautsou".
