//
//  Haptics.swift
//  Clearance
//
//  Thin, main-actor-isolated wrapper around UIKit's feedback generators so the
//  rest of the app can request tactile feedback without touching UIKit directly.
//
//  All generators must be used on the main thread, hence `@MainActor`.
//

import UIKit

@MainActor
final class HapticsManager {

    /// Master switch (e.g. for a future settings toggle). Defaults on.
    var isEnabled: Bool = true

    private let lightImpact = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpact = UIImpactFeedbackGenerator(style: .medium)
    private let selection = UISelectionFeedbackGenerator()
    private let notification = UINotificationFeedbackGenerator()

    init() {}

    /// Warms up the most frequently used generators to minimise first-fire latency.
    func prepare() {
        guard isEnabled else { return }
        lightImpact.prepare()
        selection.prepare()
    }

    /// Subtle cue when a task is checked/unchecked.
    func taskToggled(completed: Bool) {
        guard isEnabled else { return }
        if completed {
            lightImpact.impactOccurred(intensity: 0.7)
        } else {
            selection.selectionChanged()
        }
    }

    /// Cue when an item is skipped or restored for the day.
    func skipped() {
        guard isEnabled else { return }
        mediumImpact.impactOccurred(intensity: 0.5)
    }

    /// Cue when an optional module is toggled on/off.
    func moduleToggled() {
        guard isEnabled else { return }
        selection.selectionChanged()
    }

    /// Celebratory cue when a whole sequence reaches 100%.
    func checklistCompleted() {
        guard isEnabled else { return }
        notification.notificationOccurred(.success)
    }

    /// Cue when the user clears the sequences for the next day.
    func reset() {
        guard isEnabled else { return }
        notification.notificationOccurred(.warning)
    }
}
