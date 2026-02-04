import SwiftUI

struct ReaderContainerView: View {
    let url: URL
    let rootURL: URL
    let title: String
    let bookPath: String  // Original book path for preferences key
    
    @Environment(\.dismiss) private var dismiss
    
    @State private var settings = ReaderSettings()
    @State private var scrollProgress: Double = 0.0
    @State private var initialScrollProgress: Double = 0.0
    @State private var isLoaded = false
    
    private var isPDF: Bool {
        url.pathExtension.lowercased() == "pdf"
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Header / Navigation
            HStack {
                Text(title)
                    .font(.headline)
                    .lineLimit(1)
                
                Spacer()
                
                // Progress indicator
                Text("\(Int(scrollProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Button("Close") {
                    savePreferences()
                    dismiss()
                }
                .keyboardShortcut(.cancelAction)
            }
            .padding()
            .background(Color(NSColor.windowBackgroundColor))
            
            Divider()
            
            if isLoaded {
                ReaderView(url: url, rootURL: rootURL, settings: settings, initialScrollProgress: initialScrollProgress, scrollProgress: $scrollProgress)
            }
        }
        .frame(minWidth: 800, idealWidth: 1000, maxWidth: .infinity, minHeight: 600, idealHeight: 800, maxHeight: .infinity)
        .toolbar {
            if !isPDF {
                ToolbarItem(placement: .automatic) {
                    ControlGroup {
                        Button(action: { 
                            settings.fontSize = max(12, settings.fontSize - 2)
                            savePreferences()
                        }) {
                            Image(systemName: "textformat.size.smaller")
                        }
                        Button(action: { 
                            settings.fontSize = min(36, settings.fontSize + 2)
                            savePreferences()
                        }) {
                            Image(systemName: "textformat.size.larger")
                        }
                    }
                }
                
                ToolbarItem(placement: .automatic) {
                    Picker("Font", selection: $settings.fontFamily) {
                        ForEach(ReaderSettings.availableFonts, id: \.self) { font in
                            Text(font).tag(font)
                        }
                    }
                    .pickerStyle(.menu)
                    .frame(width: 120)
                    .onChange(of: settings.fontFamily) { _, _ in savePreferences() }
                }
                
                ToolbarItem(placement: .automatic) {
                    Picker("Align", selection: $settings.textAlignment) {
                        ForEach(ReaderSettings.TextAlignment.allCases) { alignment in
                            Image(systemName: alignment.iconName).tag(alignment)
                        }
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 120)
                    .onChange(of: settings.textAlignment) { _, _ in savePreferences() }
                }
                
                ToolbarItem(placement: .automatic) {
                    HStack(spacing: 8) {
                        ForEach(ReaderSettings.ReaderTheme.allCases) { theme in
                            Button(action: {
                                settings.theme = theme
                                savePreferences()
                            }) {
                                ZStack {
                                    Circle()
                                        .fill(theme.color)
                                        .frame(width: 20, height: 20)
                                        .overlay(
                                            Circle()
                                                .stroke(Color.primary.opacity(0.3), lineWidth: 1)
                                        )
                                    
                                    if settings.theme == theme {
                                        Circle()
                                            .stroke(Color.primary, lineWidth: 2)
                                            .frame(width: 24, height: 24)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            .help(theme.displayName)
                        }
                    }
                    .padding(.horizontal, 8)
                }
            }
        }
        .onAppear {
            loadPreferences()
        }
        .onDisappear {
            savePreferences()
        }
        .onExitCommand {
            savePreferences()
            dismiss()
        }
    }
    
    private func loadPreferences() {
        let pref = BookPreferencesManager.shared.load(for: bookPath)
        settings = pref.toReaderSettings()
        scrollProgress = pref.scrollProgress
        initialScrollProgress = pref.scrollProgress
        isLoaded = true
    }
    
    private func savePreferences() {
        var pref = BookPreference()
        pref.update(from: settings)
        pref.scrollProgress = scrollProgress
        BookPreferencesManager.shared.save(pref, for: bookPath)
    }
}
