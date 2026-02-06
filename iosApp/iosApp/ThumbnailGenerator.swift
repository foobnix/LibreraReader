import SwiftUI
import PDFKit
import UniformTypeIdentifiers
import QuickLookThumbnailing
import CryptoKit
import Unrar
import ZIPFoundation
import libmobi

actor ThumbnailGenerator {
    static let shared = ThumbnailGenerator()
    
    nonisolated let cacheDirectory: URL
    
    private init() {
        let paths = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)
        let cacheBase = paths[0].appendingPathComponent("com.librera.app/Thumbnails")
        self.cacheDirectory = cacheBase
        
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }
    
    enum ThumbnailError: Error {
        case fileNotFound
        case unsupportedFormat
        case noCoverFound
    }
    
    func generateThumbnail(for url: URL, size: CGSize = CGSize(width: 300, height: 450)) async -> PlatformImage? {
        // 1. Check Cache first (Non-isolated for performance)
        let key = await getCacheKey(for: url)
        let cacheURL = cacheDirectory.appendingPathComponent("\(key).png")
        
        if let cachedImage = PlatformImage(contentsOf: cacheURL) {
            print("INFO: Cache HIT for \(url.lastPathComponent)")
            return cachedImage
        }
        
        // 2. Generate if not cached (Isolated to the actor to run serially)
        print("INFO: Cache MISS for \(url.lastPathComponent), generating...")
        return await generateAndCache(url: url, size: size, cacheURL: cacheURL)
    }
    
    private func generateAndCache(url: URL, size: CGSize, cacheURL: URL) async -> PlatformImage? {
        let ext = url.pathExtension.lowercased()
        var generated: PlatformImage?
        
        switch ext {
        case "pdf":
            generated = generatePDFThumbnail(url: url, size: size)
        case "epub":
            generated = await generateEPUBThumbnail(url: url, size: size)
        case "fb2":
            generated = await generateFB2Thumbnail(url: url)
        case "mobi", "azw", "azw3":
            generated = await generateMobiThumbnail(url: url)
        case "cbz":
            generated = await extractFirstImageFromZip(url: url)
        case "cbr":
            generated = await extractFirstImageFromRar(url: url)
        default:
            return nil
        }
        
        // 3. Save to cache
        if let image = generated {
            saveToCache(image: image, url: cacheURL)
        }
        
        return generated
    }
    
    nonisolated func getCacheKey(for url: URL) async -> String {
        let path = url.path
        
        // Use security scope if needed for metadata
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        let resourceValues = try? url.resourceValues(forKeys: [.contentModificationDateKey])
        let modDate = resourceValues?.contentModificationDate ?? Date()
        
        let rawKey = "\(path)_\(modDate.timeIntervalSince1970)"
        let inputData = Data(rawKey.utf8)
        let hashed = SHA256.hash(data: inputData)
        return hashed.compactMap { String(format: "%02x", $0) }.joined()
    }
    
    private func saveToCache(image: PlatformImage, url: URL) {
        guard let pngData = image.pngData() else {
            return
        }
        
        try? pngData.write(to: url)
    }
    
    private func generatePDFThumbnail(url: URL, size: CGSize) -> PlatformImage? {
        guard let document = PDFDocument(url: url),
              let page = document.page(at: 0) else { return nil }
        
        #if canImport(AppKit)
        return page.thumbnail(of: size, for: .mediaBox)
        #else
        // PDFKit on iOS returns UIImage
        return page.thumbnail(of: size, for: .mediaBox)
        #endif
    }
    
    private func generateEPUBThumbnail(url: URL, size: CGSize) async -> PlatformImage? {
        let result = await withCheckedContinuation { (continuation: CheckedContinuation<PlatformImage?, Never>) in
            #if canImport(UIKit)
            let scale = UIScreen.main.scale
            #else
            let scale = NSScreen.main?.backingScaleFactor ?? 2.0
            #endif
            
            let request = QLThumbnailGenerator.Request(fileAt: url, size: size, scale: scale, representationTypes: .thumbnail)
            
            QLThumbnailGenerator.shared.generateBestRepresentation(for: request) { thumbnail, error in
                if let thumbnail = thumbnail {
                    #if canImport(UIKit)
                    continuation.resume(returning: thumbnail.uiImage)
                    #else
                    continuation.resume(returning: thumbnail.nsImage)
                    #endif
                } else {
                    continuation.resume(returning: nil)
                }
            }
        }
        
        // Fallback: If QL fails, try to find the first image in the zip
        if result == nil {
            print("DEBUG: QL failed for EPUB \(url.lastPathComponent), trying fallback cover extraction...")
            return await extractFirstImageFromZip(url: url)
        }
        
        return result
    }
    
    private func extractFirstImageFromZip(url: URL) async -> PlatformImage? {
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        do {
            guard let archive = Archive(url: url, accessMode: .read) else { return nil }
            
            let imageExtensions = Set(["jpg", "jpeg", "png", "webp"])
            
            for entry in archive {
                let ext = (entry.path as NSString).pathExtension.lowercased()
                if imageExtensions.contains(ext) {
                    var data = Data()
                    _ = try archive.extract(entry, consumer: { data.append($0) })
                    
                    if !data.isEmpty {
                        return PlatformImage(data: data)
                    }
                }
            }
        } catch {
            print("DEBUG: ZIPFoundation extraction failed: \(error.localizedDescription)")
        }
        
        return nil
    }
    
    private func extractFirstImageFromRar(url: URL) async -> PlatformImage? {
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        do {
            let archive = try Archive(path: url.path)
            let entries = try archive.entries()
            
            let imageExtensions = Set(["jpg", "jpeg", "png", "webp"])
            
            // Find first image entry
            for entry in entries {
               
                    let ext = (entry.fileName as NSString).pathExtension.lowercased()
                    if imageExtensions.contains(ext) {
                        // Extract directly to memory
                        if let data = try? archive.extract(entry) {
                            return PlatformImage(data: data)
                        }
                    }
                
            }
        } catch {
            print("DEBUG: CBR cover extraction failed: \(error.localizedDescription)")
        }
        return nil
    }
    
    private func generateFB2Thumbnail(url: URL) async -> PlatformImage? {
        // Security scope for reading FB2 content
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        guard let data = try? Data(contentsOf: url) else { return nil }
        let parser = Fb2CoverParser(xmlData: data)
        if let coverData = parser.parseCover() {
            return PlatformImage(data: coverData)
        }
        return nil
    }
    
    private class Fb2CoverParser: NSObject, XMLParserDelegate {
        private let parser: XMLParser
        private var coverId: String?
        private var currentBinaryId: String?
        private var currentBinaryData = ""
        private var isInsideBinary = false
        private var coverData: Data?
        
        init(xmlData: Data) {
            self.parser = XMLParser(data: xmlData)
            super.init()
            self.parser.delegate = self
        }
        
        func parseCover() -> Data? {
            parser.parse()
            return coverData
        }
        
        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
            if elementName == "coverpage" {
                // In FB2, cover is often inside <coverpage><image l:href="#id"/></coverpage>
            } else if elementName == "image" {
                if let href = attributeDict["l:href"] ?? attributeDict["xlink:href"] {
                    if coverId == nil { // Take the first image reference in the doc usually is cover if it's metadata
                         coverId = href.hasPrefix("#") ? String(href.dropFirst()) : href
                    }
                }
            } else if elementName == "binary" {
                let id = attributeDict["id"]
                if id == coverId {
                    isInsideBinary = true
                    currentBinaryId = id
                    currentBinaryData = ""
                }
            }
        }
        
        func parser(_ parser: XMLParser, foundCharacters string: String) {
            if isInsideBinary {
                currentBinaryData += string
            }
        }
        
        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            if elementName == "binary", isInsideBinary {
                isInsideBinary = false
                let cleaned = currentBinaryData.replacingOccurrences(of: "\n", with: "").replacingOccurrences(of: "\r", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
                coverData = Data(base64Encoded: cleaned)
                if coverData != nil {
                    parser.abortParsing() // Found what we need
                }
            }
        }
    }
    
    private func generateMobiThumbnail(url: URL) async -> PlatformImage? {
        // Try QL fallback first (safer and sometimes has covers on macOS)
        #if os(macOS)
        if let qlThumb = await generateEPUBThumbnail(url: url, size: CGSize(width: 300, height: 450)) {
            print("DEBUG: Using QL thumbnail for MOBI/AZW: \(url.lastPathComponent)")
            return qlThumb
        }
        #endif

        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        print("DEBUG: Starting libmobi extraction for \(url.lastPathComponent)...")
        
        // Use a detached task to avoid blocking the cooperative pool with heavy C calls
        return await Task.detached(priority: .background) {
            do {
                let mobi = try Mobi(url: url)
                if let coverData = try mobi.getCover() {
                    print("DEBUG: libmobi successfully extracted cover for \(url.lastPathComponent)")
                    return PlatformImage(data: coverData)
                }
            } catch {
                print("ERROR: libmobi failed for \(url.lastPathComponent): \(error.localizedDescription)")
            }
            return nil
        }.value
    }
}
