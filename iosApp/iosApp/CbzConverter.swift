import Foundation
import ZIPFoundation

struct CbzConverter {
    enum ConversionError: Error, LocalizedError {
        case fileReadFailed(URL, String)
        case unzipFailed(String)
        case templateWriteFailed(String)
        
        var errorDescription: String? {
            switch self {
            case .fileReadFailed(let url, let reason):
                return "Failed to read CBZ file at \(url.path): \(reason)"
            case .unzipFailed(let reason):
                return "Failed to unzip CBZ: \(reason)"
            case .templateWriteFailed(let reason):
                return "Failed to write reader template: \(reason)"
            }
        }
    }
    
    /// Converts CBZ file to a directory containing index.html and its images.
    /// Returns (indexHTMLURL, extractionRootURL).
    static func convertCbz(sourceURL: URL) async throws -> (URL, URL) {
        let fileManager = FileManager.default
        let tempDir = fileManager.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try fileManager.createDirectory(at: tempDir, withIntermediateDirectories: true)
        
        print("DEBUG: Converting CBZ \(sourceURL.lastPathComponent) to HTML...")
        
        // 1. Unzip using ZIPFoundation
        do {
            try fileManager.unzipItem(at: sourceURL, to: tempDir)
        } catch {
            throw ConversionError.unzipFailed(error.localizedDescription)
        }
        
        // 2. Find and sort images
        let resourceKeys: [URLResourceKey] = [.nameKey, .isDirectoryKey]
        let enumerator = fileManager.enumerator(at: tempDir, includingPropertiesForKeys: resourceKeys, options: [.skipsHiddenFiles])
        
        var imageFiles: [String] = []
        let imageExtensions = Set(["jpg", "jpeg", "png", "gif", "webp"])
        
        while let fileURL = enumerator?.nextObject() as? URL {
            let ext = fileURL.pathExtension.lowercased()
            if imageExtensions.contains(ext) {
                // Get path relative to tempDir
                let relativePath = fileURL.path.replacingOccurrences(of: tempDir.path + "/", with: "")
                imageFiles.append(relativePath)
            }
        }
        
        // Natural sort
        imageFiles.sort { $0.localizedStandardCompare($1) == .orderedAscending }
        
        if imageFiles.isEmpty {
            throw ConversionError.unzipFailed("No images found in CBZ archive.")
        }
        
        // 3. Generate index.html
        var imagesHTML = ""
        for imagePath in imageFiles {
            imagesHTML += "<img src=\"\(imagePath)\" class=\"comic-page\">\n"
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
            }
            body { 
                font-family: var(--font-family); 
                font-size: var(--font-size); 
                background-color: var(--bg-color); 
                color: var(--text-color);
                margin: 0;
                padding: 0;
                display: flex;
                flex-direction: column;
                align-items: center;
            }
            .comic-page { 
                max-width: 100%; 
                height: auto; 
                display: block; 
                margin: 0;
                padding: 0;
            }
        </style>
        """
        
        let indexHTML = """
        <!DOCTYPE html>
        <html>
        <head>
        \(headContent)
        </head>
        <body>
        \(imagesHTML)
        </body>
        </html>
        """
        
        let indexURL = tempDir.appendingPathComponent("index.html")
        do {
            try indexHTML.write(to: indexURL, atomically: true, encoding: .utf8)
        } catch {
            throw ConversionError.templateWriteFailed(error.localizedDescription)
        }
        
        return (indexURL, tempDir)
    }
}
