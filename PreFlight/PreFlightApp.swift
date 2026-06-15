//
//  PreFlightApp.swift
//  PreFlight
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
struct PreFlightApp: App {

    /// The SwiftData stack. `ModelContainer` is `Sendable`, so storing it as a
    /// plain `let` is concurrency-safe.
    private let modelContainer: ModelContainer

    /// The single source of truth for checklist state. Held in `@State` so the
    /// instance survives view-tree re-evaluations.
    @State private var viewModel: ChecklistViewModel

    init() {
        do {
            // A single configuration backed by the default on-disk store.
            let container = try ModelContainer(
                for: ChecklistItem.self,
                configurations: ModelConfiguration(isStoredInMemoryOnly: false)
            )
            self.modelContainer = container
            _viewModel = State(initialValue: ChecklistViewModel(modelContext: container.mainContext))
        } catch {
            // A failure here means the persistent store is unusable — there is no
            // sensible way to continue, so surface it loudly during development.
            fatalError("Failed to initialise the PreFlight ModelContainer: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            DashboardView(viewModel: viewModel)
        }
        .modelContainer(modelContainer)
    }
}
