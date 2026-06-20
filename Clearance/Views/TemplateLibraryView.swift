//
//  TemplateLibraryView.swift
//  Clearance
//
//  Browse sport activity templates. Tap + to install; checkmark when already installed.
//

import SwiftUI

struct TemplateLibraryView: View {
    let viewModel: ChecklistViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(TemplateCatalog.all) { entry in
                    catalogRow(entry)
                }
            }
            .navigationTitle("Template Library")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func catalogRow(_ entry: TemplateEntry) -> some View {
        HStack(spacing: 12) {
            Text(entry.emoji)
                .font(.title3)
                .frame(width: 32, alignment: .center)

            VStack(alignment: .leading, spacing: 2) {
                Text(entry.name)
                    .font(.body.weight(.medium))
                Text(entry.tagline)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 0)

            if viewModel.isTemplateInstalled(entry) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.title3)
                    .foregroundStyle(Color.secondary.opacity(0.5))
            } else {
                Button {
                    viewModel.installTemplate(entry)
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(Color.accentColor)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Install \(entry.name)")
            }
        }
        .padding(.vertical, 4)
    }
}
