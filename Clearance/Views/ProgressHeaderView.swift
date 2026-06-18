//
//  ProgressHeaderView.swift
//  PreFlight
//
//  A minimalist, glanceable progress header: the active sequence's name on the
//  left, a percentage + fraction on the right, and an animated progress bar.
//

import SwiftUI

struct ProgressHeaderView: View {
    let checklist: ChecklistType
    let progress: Double
    let completed: Int
    let total: Int
    let palette: ChecklistPalette

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var percentText: String {
        "\(Int((progress * 100).rounded()))%"
    }

    private var isComplete: Bool { total > 0 && completed == total }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(checklist.title)
                        .font(.largeTitle.weight(.bold))
                        .foregroundStyle(palette.primaryText)
                    Text(isComplete ? "SEQUENCE COMPLETE ✓" : checklist.subtitle.uppercased())
                        .font(.caption.weight(.semibold))
                        .tracking(1.5)
                        .foregroundStyle(isComplete ? palette.tint : palette.secondaryText)
                        .contentTransition(.opacity)
                }

                Spacer(minLength: 12)

                VStack(alignment: .trailing, spacing: 2) {
                    Text(percentText)
                        .font(.title2.weight(.bold).monospacedDigit())
                        .foregroundStyle(palette.tint)
                        .contentTransition(.numericText(value: progress))
                    Text("\(completed)/\(total)")
                        .font(.subheadline.weight(.medium).monospacedDigit())
                        .foregroundStyle(palette.secondaryText)
                }
                .animation(reduceMotion ? nil : Theme.Motion.snappy, value: progress)
            }

            progressBar
                .frame(height: 10)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(checklist.title) progress")
        .accessibilityValue("\(completed) of \(total) complete, \(percentText)")
    }

    private var progressBar: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(palette.secondaryText.opacity(0.18))
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [palette.tint.opacity(0.85), palette.tint],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: max(0, geo.size.width * progress))
            }
        }
        .animation(reduceMotion ? nil : Theme.Motion.spring, value: progress)
    }
}

#Preview("Progress Header") {
    VStack(spacing: 32) {
        ProgressHeaderView(
            checklist: .morning,
            progress: 0.42,
            completed: 3,
            total: 7,
            palette: Theme.palette(for: .morning, scheme: .light)
        )
        ProgressHeaderView(
            checklist: .evening,
            progress: 1.0,
            completed: 6,
            total: 6,
            palette: Theme.palette(for: .evening, scheme: .dark)
        )
    }
    .padding()
    .background(Color(hex: 0x101014))
}
