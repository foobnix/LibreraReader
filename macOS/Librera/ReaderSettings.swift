import SwiftUI

struct ReaderSettings {
    var fontSize: Double = 22.0
    var fontFamily: String = "Serif"
    var theme: ReaderTheme = .white
    var textAlignment: TextAlignment = .justify
    
    enum TextAlignment: String, CaseIterable, Identifiable {
        case left
        case justify
        case right
        
        var id: String { rawValue }
        
        var displayName: String { rawValue.capitalized }
        
        var cssValue: String { rawValue }
        
        var iconName: String {
            switch self {
            case .left: return "text.alignleft"
            case .right: return "text.alignright"
            case .justify: return "text.justify"
            }
        }
    }
    
    enum ReaderTheme: String, CaseIterable, Identifiable {
        case white
        case sepia
      
        case black
        
        var id: String { rawValue }
        
        var backgroundColor: String {
            switch self {
            case .white: return "#ffffff"
            case .sepia: return "#f4ecd8"
           
            case .black: return "#000000"
            }
        }
        
        var textColor: String {
            switch self {
            case .white, .sepia: return "#000000"
            case  .black: return "#e5e5e7"
            }
        }
        
        var color: Color {
            Color(hex: backgroundColor)
        }
        
        var displayName: String { rawValue.capitalized }
    }
    
    static let availableFonts = ["System", "Serif", "Sans-Serif", "Monospace"]
    
    var cssFontFamily: String {
        switch fontFamily {
        case "Serif": return "Georgia, serif"
        case "Sans-Serif": return "Helvetica, Arial, sans-serif"
        case "Monospace": return "Menlo, monospace"
        default: return "-apple-system, system-ui, sans-serif"
        }
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
