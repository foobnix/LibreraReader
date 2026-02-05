import Foundation

struct BookPreference: Codable {
    var fontSize: Double = 22.0
    var fontFamily: String = "Serif"
    var themeName: String = "sepia"
    var textAlignmentName: String = "justify"
    var hyphenationLanguageCode: String = "auto"
    var scrollProgress: Double = 0.0
    
    var theme: ReaderSettings.ReaderTheme {
        get { ReaderSettings.ReaderTheme(rawValue: themeName) ?? .white }
        set { themeName = newValue.rawValue }
    }
    
    var textAlignment: ReaderSettings.TextAlignment {
        get { ReaderSettings.TextAlignment(rawValue: textAlignmentName) ?? .left }
        set { textAlignmentName = newValue.rawValue }
    }
    
    var hyphenationLanguage: ReaderSettings.HyphenationLanguage {
        get { ReaderSettings.HyphenationLanguage(rawValue: hyphenationLanguageCode) ?? .auto }
        set { hyphenationLanguageCode = newValue.rawValue }
    }
    
    func toReaderSettings() -> ReaderSettings {
        var settings = ReaderSettings()
        settings.fontSize = fontSize
        settings.fontFamily = fontFamily
        settings.theme = theme
        settings.textAlignment = textAlignment
        settings.hyphenationLanguage = hyphenationLanguage
        return settings
    }
    
    mutating func update(from settings: ReaderSettings) {
        fontSize = settings.fontSize
        fontFamily = settings.fontFamily
        themeName = settings.theme.rawValue
        textAlignmentName = settings.textAlignment.rawValue
        hyphenationLanguageCode = settings.hyphenationLanguage.rawValue
    }
}

class BookPreferencesManager {
    static let shared = BookPreferencesManager()
    
    private let userDefaults = UserDefaults.standard
    private let preferencesKey = "BookPreferences"
    
    private init() {}
    
    private func key(for bookPath: String) -> String {
        // Use a stable hash of the path. Swift's .hashValue is not stable across restarts.
        let stableHash = bookPath.utf8.reduce(5381) {
            ($0 << 5) &+ $0 &+ Int($1)
        }
        return "\(preferencesKey)_\(stableHash)"
    }
    
    func load(for bookPath: String) -> BookPreference {
        let key = key(for: bookPath)
        guard let data = userDefaults.data(forKey: key),
              let preference = try? JSONDecoder().decode(BookPreference.self, from: data) else {
            return BookPreference()
        }
        return preference
    }
    
    func save(_ preference: BookPreference, for bookPath: String) {
        let key = key(for: bookPath)
        if let data = try? JSONEncoder().encode(preference) {
            userDefaults.set(data, forKey: key)
            // Notify that progress might have changed
            NotificationCenter.default.post(name: .bookProgressChanged, object: bookPath)
        }
    }
}

extension Notification.Name {
    static let bookProgressChanged = Notification.Name("bookProgressChanged")
}
