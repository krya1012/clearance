# ✈️ PreFlight

**A tactical daily-routine checklist for iOS.** Run a focused **🌅 Takeoff** (morning) and
**🌌 Landing** (evening) sequence, with optional **🏋️ Gym** and **🏊 Swim** modules you can
inject on demand. Built to be glanceable, high-contrast, and fast.

> Native SwiftUI · Swift Concurrency · SwiftData · iOS 17+

---

## Status

This project was authored in a **Linux** environment with no Swift/Xcode toolchain, so it has
**not been compiled here**. All source targets iOS 17+ and is written to build clean under
**Swift 6 strict concurrency**. Do the final build/run on a Mac with **Xcode 16 or newer**
(see [Building](#building)).

---

## Building

```bash
open PreFlight.xcodeproj      # Xcode 16+
# Select an iOS 17+ simulator (e.g. iPhone 15) and press ⌘R
```

The project uses an Xcode 16 *synchronized file group*, so every file under `PreFlight/` is
included in the target automatically — just add files to the folder and Xcode picks them up.

<details>
<summary>Fallback if the project won't open (older Xcode)</summary>

1. File ▸ New ▸ Project ▸ iOS App. Name it **PreFlight**, Interface **SwiftUI**, Storage **SwiftData**.
2. Delete the generated `ContentView.swift` / `Item.swift`.
3. Drag the contents of the `PreFlight/` folder (Models, Views, ViewModels, Helpers, `PreFlightApp.swift`,
   `Assets.xcassets`) into the project, choosing *Copy items if needed* and your app target.
4. Set the Deployment Target to **iOS 17.0** and Swift Language Version to **6**.
</details>

---

## Architecture

A small, centralized **MVVM** design using the modern **Observation** framework.

```
PreFlight/
├─ PreFlightApp.swift        # @main entry; builds ModelContainer + owns the ViewModel
├─ Models/                   # SwiftData model + enums + seed data
├─ ViewModels/               # ChecklistViewModel (single source of truth)
├─ Views/                    # SwiftUI screens & components
├─ Helpers/                  # Haptics, Theme, small extensions
└─ Assets.xcassets/          # AccentColor, AppIcon
```

Design notes and per-file responsibilities are filled in as the implementation lands
(see commit history — one commit per build phase).

---

## License

Created as a demonstration project. Use freely.
