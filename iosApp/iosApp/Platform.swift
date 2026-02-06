import SwiftUI

#if canImport(UIKit)
import UIKit
public typealias PlatformImage = UIImage
public typealias PlatformColor = UIColor

extension Image {
    init(platformImage: PlatformImage) {
        self.init(uiImage: platformImage)
    }
}

extension PlatformColor {
    static var controlBackgroundColor: PlatformColor {
        return .secondarySystemBackground
    }
    static var windowBackgroundColor: PlatformColor {
        return .systemBackground
    }
}

extension PlatformImage {
    static var appIcon: PlatformImage? {
        return nil // On iOS, icons are not typically accessed this way
    }
    
    convenience init?(contentsOf url: URL) {
        self.init(contentsOfFile: url.path)
    }
}

#elseif canImport(AppKit)
import AppKit
public typealias PlatformImage = NSImage
public typealias PlatformColor = NSColor

extension Image {
    init(platformImage: PlatformImage) {
        self.init(nsImage: platformImage)
    }
}

extension PlatformImage {
    static var appIcon: PlatformImage? {
        return NSApp.applicationIconImage
    }
    
    func pngData() -> Data? {
        guard let tiffRepresentation = tiffRepresentation,
              let bitmapImage = NSBitmapImageRep(data: tiffRepresentation) else { return nil }
        return bitmapImage.representation(using: .png, properties: [:])
    }
}
#endif
