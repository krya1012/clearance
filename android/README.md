# Clearance for Android

A native Kotlin / Jetpack Compose port of the iOS Clearance app — see the repository root
`CLAUDE.md` for what the app is (a pre-flight-style morning/evening checklist, "Takeoff" and
"Landing"). This directory has **no code shared with `ios/`** — same product spec, independent
implementation.

## Build & run

- **Open:** `android/` in Android Studio (Hedgehog or newer), let Gradle sync.
- **Build/run:** run the `app` configuration on an API 26+ emulator or device.
- **Command line:** `cd android && ./gradlew :app:assembleDebug` (or `:app:testDebugUnitTest`
  for the JUnit suite).
- **Target:** `minSdk 26`, `compileSdk`/`targetSdk 35`, Kotlin 2.x, zero-warning build
  (`allWarningsAsErrors = true`).

> Every milestone of this port was built in a sandboxed session whose network egress policy
> blocks `dl.google.com` (where AGP/Room/Compose artifacts are hosted), so a real Gradle build
> could never be verified there — all of it went through careful manual review against the
> actual repo APIs instead. Run the build/test commands above for real once you have normal
> network access, and report anything that doesn't compile.

## Architecture

`vm/ChecklistViewModel.kt` is the single source of truth: Room (`data/ActivityModuleDao`,
`ChecklistItemDao`) exposes reactive `Flow`s directly, while `ScheduleStore`/`PeriodicTaskStore`
are one-shot `suspend` DataStore reads, so the ViewModel owns `MutableStateFlow`s for
schedule-derived state and persists on every mutation. All gating/grouping logic funnels
through one atomic `combine()` (see `computeGated`) so a schedule change can never be observed
by `sections` a tick before `todayActivityIDs`/`tomorrowActivityIDs` catch up.

`ui/` is one Compose screen (`DashboardScreen`) plus `ModalBottomSheet`s for schedule editing,
module management, template browsing, and task editing — see `ANDROID_PLAN.md` for the full
file-by-file breakdown and the milestone history that built it up.

## Known platform-parity gaps

- **Reduce Motion:** iOS suppresses its explicit animations under
  `UIAccessibility.isReduceMotionEnabled`. Jetpack Compose has no equivalent already
  established in this codebase, so the animations added in the polish pass (`AnimatedVisibility`,
  `animateItem()`, the Morning/Evening color cross-fade) are kept short (~200–250ms) rather than
  gated behind a reduced-motion check. Revisit if a clean way to read the platform's
  "Remove animations" accessibility setting is added later.
- **Calendar system:** `domain/AutoReset.kt`'s `Calendar.getInstance(zone)` only takes a
  `TimeZone`, not a `Locale`, so it doesn't honor a non-Gregorian calendar system the way iOS's
  `Calendar.current` does. Accepted as a known, low-impact gap (see the comment in
  `AutoReset.kt`) rather than pulling in ICU4J for it.

## Status

All six milestones in `ANDROID_PLAN.md` are complete: scaffold, data layer, ViewModel, core
dashboard UI, schedule/module-management UI, and this polish pass (haptics, animations,
accessibility, adaptive launcher icon, this file).
