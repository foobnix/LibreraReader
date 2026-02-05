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
    @State private var showUI = true
    
    private var isPDF: Bool {
        url.pathExtension.lowercased() == "pdf"
    }
    
    var body: some View {
        ZStack(alignment: .top) {
            Color(PlatformColor.windowBackgroundColor)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                #if os(iOS)
                if showUI {
                    VStack(spacing: 0) {
                        // Header / Navigation - Line 1 (iOS)
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
                        .padding([.horizontal, .top])
                        .padding(.bottom, 8)
                        
                        if !isPDF {
                            // Header / Settings - Line 2 (iOS)
                            HStack(spacing: 0) {
                                // Font Size
                                HStack(spacing: 0) {
                                    Button(action: { 
                                        settings.fontSize = max(12, settings.fontSize - 2)
                                        savePreferences()
                                    }) {
                                        Image(systemName: "textformat.size.smaller")
                                            .padding(8)
                                    }
                                    Divider().frame(height: 20)
                                    Button(action: { 
                                        settings.fontSize = min(36, settings.fontSize + 2)
                                        savePreferences()
                                    }) {
                                        Image(systemName: "textformat.size.larger")
                                            .padding(8)
                                    }
                                }
                                .background(Color.primary.opacity(0.1))
                                .cornerRadius(8)
                                
                                Spacer()
                                
                                // Alignment
                                Picker("Align", selection: $settings.textAlignment) {
                                    ForEach(ReaderSettings.TextAlignment.allCases) { alignment in
                                        Image(systemName: alignment.iconName).tag(alignment)
                                    }
                                }
                                .pickerStyle(.segmented)
                                .frame(width: 100)
                                .onChange(of: settings.textAlignment) { _, _ in savePreferences() }
                                
                                Spacer()
                                
                                // Settings Menu (Font & Hyphenation)
                                Menu {
                                    Picker("Font Family", selection: $settings.fontFamily) {
                                        ForEach(ReaderSettings.availableFonts, id: \.self) { font in
                                            Text(font).tag(font)
                                        }
                                    }
                                    
                                    Picker("Hyphenation", selection: $settings.hyphenationLanguage) {
                                        ForEach(ReaderSettings.HyphenationLanguage.allCases) { lang in
                                            Text(lang.displayName).tag(lang)
                                        }
                                    }
                                } label: {
                                    Image(systemName: "gearshape")
                                        .font(.title3)
                                        .padding(8)
                                        .contentShape(Rectangle())
                                }
                                
                                Spacer()
                                
                                // Themes
                                HStack(spacing: 12) {
                                    ForEach(ReaderSettings.ReaderTheme.allCases) { theme in
                                        Button(action: {
                                            settings.theme = theme
                                            savePreferences()
                                        }) {
                                            ZStack {
                                                Circle()
                                                    .fill(theme.color)
                                                    .frame(width: 22, height: 22)
                                                    .overlay(
                                                        Circle()
                                                            .stroke(Color.primary.opacity(0.3), lineWidth: 1)
                                                    )
                                                
                                                if settings.theme == theme {
                                                    Circle()
                                                        .stroke(Color.primary, lineWidth: 2)
                                                        .frame(width: 28, height: 28)
                                                }
                                            }
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                            }
                            .padding([.horizontal, .bottom])
                            .padding(.top, 4)
                        }
                    }
                    .background(Color(PlatformColor.windowBackgroundColor))
                    .transition(.move(edge: .top).combined(with: .opacity))
                    
                    Divider()
                }
                #else
                // Original macOS Header
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
                .background(Color(PlatformColor.windowBackgroundColor))
                
                Divider()
                #endif
                
                if isLoaded {
                    ReaderView(url: url, rootURL: rootURL, settings: settings, initialScrollProgress: initialScrollProgress, scrollProgress: $scrollProgress)
                        #if os(iOS)
                        .onTapGesture(count: 1) {
                            withAnimation(.easeInOut) {
                                showUI.toggle()
                            }
                        }
                        #endif
                }
            }
        }
        #if os(iOS)
        .statusBar(hidden: !showUI)
        .persistentSystemOverlays(showUI ? .automatic : .hidden)
        .toolbar(showUI ? .automatic : .hidden, for: .navigationBar)
        #else
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
                    Menu {
                        Picker("Font Family", selection: $settings.fontFamily) {
                            ForEach(ReaderSettings.availableFonts, id: \.self) { font in
                                Text(font).tag(font)
                            }
                        }
                        
                        Picker("Hyphenation", selection: $settings.hyphenationLanguage) {
                            ForEach(ReaderSettings.HyphenationLanguage.allCases) { lang in
                                Text(lang.displayName).tag(lang)
                            }
                        }
                    } label: {
                        Label("Settings", systemImage: "gearshape")
                    }
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
        #endif
        
        .onAppear {
            loadPreferences()
        }
        .onDisappear {
            savePreferences()
        }
        #if os(macOS)
        .onExitCommand {
            savePreferences()
            dismiss()
        }
        #endif
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
