//
//  ChecklistView.swift
//  PreFlight
//
//  The scrollable task stream, grouped by module. Each row is a full-width
//  tappable Button (large target) and supports swipe-left to skip / restore.
//

import SwiftData
import SwiftUI

struct ChecklistView: View {
    let viewModel: ChecklistViewModel
    let palette: ChecklistPalette

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Group {
            if viewModel.sections.isEmpty {
                emptyState
            } else {
                list
            }
        }
        .animation(reduceMotion ? nil : Theme.Motion.spring, value: viewModel.sections.map(\.id))
    }

    private var list: some View {
        List {
            ForEach(viewModel.sections) { section in
                Section {
                    ForEach(section.items, id: \.id) { item in
                        row(for: item)
                    }
                } header: {
                    sectionHeader(section.module)
                }
            }

            // Bottom inset so the floating reset control never covers the last row.
            Color.clear
                .frame(height: 88)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .listSectionSpacing(Theme.Layout.sectionSpacing)
        .scrollContentBackground(.hidden)
        .scrollIndicators(.hidden)
    }

    private var emptyState: some View {
        VStack(spacing: 10) {
            Image(systemName: "checklist")
                .font(.system(size: 40, weight: .semibold))
            Text("Nothing scheduled")
                .font(.headline)
            Text("Enable a module above to add tasks.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(palette.secondaryText)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.horizontal, Theme.Layout.screenPadding)
    }

    private func row(for item: ChecklistItem) -> some View {
        Button {
            // Skipped rows aren't toggleable; restore them via swipe instead.
            guard !item.isSkipped else { return }
            viewModel.toggle(item)
        } label: {
            ChecklistRowView(item: item, palette: palette)
        }
        .buttonStyle(.plain)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(
            top: 5, leading: Theme.Layout.screenPadding,
            bottom: 5, trailing: Theme.Layout.screenPadding
        ))
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            if item.isSkipped {
                Button {
                    viewModel.restore(item)
                } label: {
                    Label("Restore", systemImage: "arrow.uturn.backward")
                }
                .tint(.blue)
            } else {
                Button {
                    viewModel.skip(item)
                } label: {
                    Label("Skip", systemImage: "moon.zzz.fill")
                }
                .tint(.orange)
            }
        }
    }

    private func sectionHeader(_ module: ModuleType) -> some View {
        HStack(spacing: 8) {
            Text(module.emoji)
            Text(module.title.uppercased())
                .font(.footnote.weight(.bold))
                .tracking(1.5)
            Spacer()
        }
        .foregroundStyle(palette.secondaryText)
        .padding(.horizontal, Theme.Layout.screenPadding)
        .padding(.vertical, 4)
    }
}

#Preview("Checklist") {
    let container = try! ModelContainer(
        for: ChecklistItem.self,
        configurations: ModelConfiguration(isStoredInMemoryOnly: true)
    )
    let vm = ChecklistViewModel(modelContext: container.mainContext)
    vm.setModule(.gym, enabled: true)
    return ChecklistView(viewModel: vm, palette: Theme.palette(for: .morning, scheme: .light))
        .background(Color(hex: 0xF4F6F8))
}
