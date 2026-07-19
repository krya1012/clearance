//
//  ClearanceApp.swift
//  Clearance
//
//  The application entry point.
//
//  Responsibilities:
//  • Builds the SwiftData `ModelContainer` for the persistent store.
//  • Seeds default checklist content on first launch (via the ViewModel).
//  • Owns the single, app-wide `ChecklistViewModel` instance.
//
//  Concurrency note: SwiftUI's `App` protocol is `@MainActor`-isolated, so
//  constructing the `@MainActor` view model and touching `container.mainContext`
//  inside `init()` is safe under Swift 6 strict concurrency.
//

import SwiftData
import SwiftUI

@main
struct ClearanceApp: App {

    /// The SwiftData stack. `ModelContainer` is `Sendable`, so storing it as a
    /// plain `let` is concurrency-safe.
    private let modelContainer: ModelContainer

    /// The single source of truth for checklist state. Held in `@State` so the
    /// instance survives view-tree re-evaluations.
    @State private var viewModel: ChecklistViewModel

    init() {
        let container: ModelContainer
        do {
            container = try ModelContainer(
                for: ChecklistItem.self, ActivityModule.self,
                configurations: ModelConfiguration(isStoredInMemoryOnly: false)
            )
        } catch {
            // The persistent store failed to open — most often an incompatible
            // store left behind by a previous install whose schema differs.
            // Rather than crash on the launch screen (which looks exactly like
            // "the app won't run"), fall back to an in-memory store so the UI
            // still appears with clean, freshly-seeded content.
            container = try! ModelContainer(
                for: ChecklistItem.self, ActivityModule.self,
                configurations: ModelConfiguration(isStoredInMemoryOnly: true)
            )
        }
        self.modelContainer = container
        _viewModel = State(initialValue: ChecklistViewModel(modelContext: container.mainContext))
    }

    var body: some Scene {
        WindowGroup {
            DashboardView(viewModel: viewModel)
        }
        .modelContainer(modelContainer)
    }
}
