import Foundation
import libmobi

struct MobiConverter {
    enum ConversionError: Error, LocalizedError {
        case fileReadFailed(URL, String)
        case templateWriteFailed(String)
        
        var errorDescription: String? {
            switch self {
            case .fileReadFailed(let url, let reason):
                return "Failed to read MOBI file at \(url.path): \(reason)"
            case .templateWriteFailed(let reason):
                return "Failed to write reader template: \(reason)"
            }
        }
    }
    
    /// Converts MOBI file to a directory containing index.html and its resources.
    /// Returns (indexHTMLURL, extractionRootURL).
    static func convertMobi(sourceURL: URL) async throws -> (URL, URL) {
        let fileManager = FileManager.default
        let tempDir = fileManager.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try fileManager.createDirectory(at: tempDir, withIntermediateDirectories: true)
        
        print("DEBUG: Converting MOBI \(sourceURL.lastPathComponent) (Single HTML) to HTML...")
        
        do {
            let mobi = try Mobi(url: sourceURL)
            
            // 1. Dump all parts (images, flow, etc.)
            // libmobi usually creates a folder named "<path>_markup"
            try mobi.dumpSource(dstFolder: tempDir)
            let markupDir = tempDir.deletingLastPathComponent().appendingPathComponent(tempDir.lastPathComponent + "_markup")
            
            // 2. Get the raw HTML (Single file containing all pages)
            var rawml = try mobi.getRawml()
            
            // 3. Flatten and fix image references
            var resourceMap: [String: String] = [:]
            
            // Collect all files from the markup directory (including subdirs like OEBPS)
            let enumerator = fileManager.enumerator(at: markupDir, includingPropertiesForKeys: [.isRegularFileKey], options: [.skipsHiddenFiles])
            
            while let fileURL = enumerator?.nextObject() as? URL {
                let filename = fileURL.lastPathComponent
                if filename.hasPrefix("resource") {
                    let pattern = "resource(\\d+)\\.(\\w+)"
                    let regex = try? NSRegularExpression(pattern: pattern, options: [])
                    let range = NSRange(filename.startIndex..<filename.endIndex, in: filename)
                    
                    if let match = regex?.firstMatch(in: filename, options: [], range: range) {
                        let idRange = Range(match.range(at: 1), in: filename)!
                        let extRange = Range(match.range(at: 2), in: filename)!
                        let id = String(filename[idRange])
                        let ext = String(filename[extRange])
                        
                        let newFilename = "\(id).\(ext)"
                        let newURL = tempDir.appendingPathComponent(newFilename)
                        
                        // Move to flat tempDir
                        try? fileManager.moveItem(at: fileURL, to: newURL)
                        resourceMap[id] = newFilename
                        
                        let normalizedId = id.trimmingCharacters(in: CharacterSet(charactersIn: "0"))
                        let finalNormalizedId = normalizedId.isEmpty ? "0" : normalizedId
                        if resourceMap[finalNormalizedId] == nil {
                            resourceMap[finalNormalizedId] = newFilename
                        }
                    }
                }
            }
            
            // 4. Regex to fix image references
            // Type A: recindex="(\d+)" (Common in old MOBI)
            let recindexPattern = "recindex=\"(\\d+)\""
            let recindexRegex = try NSRegularExpression(pattern: recindexPattern, options: [])
            
            // Type B: src="kindle:embed:(\d+)\?mime=image/(\w+)" (Common in AZW/KF8)
            let kindlePattern = "src=\"kindle:embed:(\\d+)\\?mime=image/(\\w+)\""
            let kindleRegex = try NSRegularExpression(pattern: kindlePattern, options: [])
            
            let range = NSRange(rawml.startIndex..<rawml.endIndex, in: rawml)
            
            // Process Type A: recindex
            let recindexMatches = recindexRegex.matches(in: rawml, options: [], range: range).reversed()
            for match in recindexMatches {
                if let indexRange = Range(match.range(at: 1), in: rawml) {
                    let indexStr = String(rawml[indexRange])
                    if let filename = resourceMap[indexStr] ?? resourceMap[indexStr.trimmingCharacters(in: CharacterSet(charactersIn: "0")).isEmpty ? "0" : indexStr.trimmingCharacters(in: CharacterSet(charactersIn: "0"))] {
                        let replacement = "src=\"\(filename)\""
                        if let fullMatchRange = Range(match.range, in: rawml) {
                            rawml.replaceSubrange(fullMatchRange, with: replacement)
                        }
                    }
                }
            }
            
            // Process Type B: kindle:embed
            let newRange = NSRange(rawml.startIndex..<rawml.endIndex, in: rawml)
            let kindleMatches = kindleRegex.matches(in: rawml, options: [], range: newRange).reversed()
            for match in kindleMatches {
                if let indexRange = Range(match.range(at: 1), in: rawml) {
                    let indexStr = String(rawml[indexRange])
                    if let filename = resourceMap[indexStr] ?? resourceMap[indexStr.trimmingCharacters(in: CharacterSet(charactersIn: "0")).isEmpty ? "0" : indexStr.trimmingCharacters(in: CharacterSet(charactersIn: "0"))] {
                        let replacement = "src=\"\(filename)\""
                        if let fullMatchRange = Range(match.range, in: rawml) {
                            rawml.replaceSubrange(fullMatchRange, with: replacement)
                        }
                    }
                }
            }
            
            let headContent = """
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                :root {
                    --font-size: 18px;
                    --font-family: -apple-system, system-ui, sans-serif;
                    --bg-color: #ffffff;
                    --text-color: #000000;
                    --text-alignment: justify;
                }
                body { 
                    font-family: var(--font-family); 
                    font-size: var(--font-size); 
                    background-color: var(--bg-color); 
                    color: var(--text-color);
                    text-align: var(--text-alignment);
                    line-height: 1.6; 
                    padding: 20px 40px;
                    margin: 0;
                }
                img { max-width: 100%; height: auto; display: block; margin: 1em auto; }
                h1, h2, h3 { text-align: center; }
                p { margin: 0.5em 0; }
            </style>
            """
            
            let indexHTML = """
            <!DOCTYPE html>
            <html>
            <head>
            \(headContent)
            </head>
            <body>
            \(rawml)
            </body>
            </html>
            """
            
            let indexURL = tempDir.appendingPathComponent("index.html")
            try indexHTML.write(to: indexURL, atomically: true, encoding: .utf8)
            
            // Clean up the markup dir since we moved images out
            try? fileManager.removeItem(at: markupDir)
            
            print("DEBUG: MOBI converted (Single HTML) placed at: \(indexURL.path)")
            
            return (indexURL, tempDir)
        } catch {
            print("ERROR: Could not process MOBI file: \(error)")
            throw ConversionError.fileReadFailed(sourceURL, error.localizedDescription)
        }
    }
}
