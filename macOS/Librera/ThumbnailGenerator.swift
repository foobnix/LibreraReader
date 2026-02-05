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
        let cacheBase = paths[0].appendingPathComponent("com.librera.mac/Thumbnails")
        self.cacheDirectory = cacheBase
        
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }
    
    enum ThumbnailError: Error {
        case fileNotFound
        case unsupportedFormat
        case noCoverFound
    }
    
    func generateThumbnail(for url: URL, size: CGSize = CGSize(width: 300, height: 450)) async -> NSImage? {
        // 1. Check Cache first (Non-isolated for performance)
        let key = await getCacheKey(for: url)
        let cacheURL = cacheDirectory.appendingPathComponent("\(key).png")
        
        if let cachedImage = NSImage(contentsOf: cacheURL) {
            print("INFO: Cache HIT for \(url.lastPathComponent)")
            return cachedImage
        }
        
        // 2. Generate if not cached (Isolated to the actor to run serially)
        print("INFO: Cache MISS for \(url.lastPathComponent), generating...")
        return await generateAndCache(url: url, size: size, cacheURL: cacheURL)
    }
    
    private func generateAndCache(url: URL, size: CGSize, cacheURL: URL) async -> NSImage? {
        let ext = url.pathExtension.lowercased()
        var generated: NSImage?
        
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
    
    private func saveToCache(image: NSImage, url: URL) {
        guard let tiffData = image.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiffData),
              let pngData = bitmap.representation(using: .png, properties: [:]) else {
            return
        }
        
        try? pngData.write(to: url)
    }
    
    private func generatePDFThumbnail(url: URL, size: CGSize) -> NSImage? {
        guard let document = PDFDocument(url: url),
              let page = document.page(at: 0) else { return nil }
        
        return page.thumbnail(of: size, for: .mediaBox)
    }
    
    private func generateEPUBThumbnail(url: URL, size: CGSize) async -> NSImage? {
        let result = await withCheckedContinuation { (continuation: CheckedContinuation<NSImage?, Never>) in
            let request = QLThumbnailGenerator.Request(fileAt: url, size: size, scale: NSScreen.main?.backingScaleFactor ?? 2.0, representationTypes: .thumbnail)
            
            QLThumbnailGenerator.shared.generateBestRepresentation(for: request) { thumbnail, error in
                if let thumbnail = thumbnail {
                    continuation.resume(returning: thumbnail.nsImage)
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
    
    private func extractFirstImageFromZip(url: URL) async -> NSImage? {
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
                        return NSImage(data: data)
                    }
                }
            }
        } catch {
            print("DEBUG: ZIPFoundation extraction failed: \(error.localizedDescription)")
        }
        
        return nil
    }
    
    private func extractFirstImageFromRar(url: URL) async -> NSImage? {
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
                            return NSImage(data: data)
                        }
                    }
                
            }
        } catch {
            print("DEBUG: CBR cover extraction failed: \(error.localizedDescription)")
        }
        return nil
    }
    
    private func generateFB2Thumbnail(url: URL) async -> NSImage? {
        // Security scope for reading FB2 content
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        guard let data = try? Data(contentsOf: url) else { return nil }
        let parser = Fb2CoverParser(xmlData: data)
        if let coverData = parser.parseCover() {
            return NSImage(data: coverData)
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
    
    private func generateMobiThumbnail(url: URL) async -> NSImage? {
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }
        
        do {
            let mobi = try Mobi(url: url)
            if let coverData = try mobi.getCover() {
                return NSImage(data: coverData)
            }
        } catch {
            print("ERROR: Could not generate MOBI thumbnail: \(error)")
        }
        return nil
    }
}
