import Foundation
import Unrar

struct CbrConverter {
    enum ConversionError: Error, LocalizedError {
        case fileReadFailed(URL, String)
        case unrarFailed(String)
        case templateWriteFailed(String)
        
        var errorDescription: String? {
            switch self {
            case .fileReadFailed(let url, let reason):
                return "Failed to read CBR file at \(url.path): \(reason)"
            case .unrarFailed(let reason):
                return "Failed to unrar CBR: \(reason)"
            case .templateWriteFailed(let reason):
                return "Failed to write reader template: \(reason)"
            }
        }
    }
    
    /// Converts CBR file to a directory containing index.html and its images.
    /// Returns (indexHTMLURL, extractionRootURL).
    static func convertCbr(sourceURL: URL) async throws -> (URL, URL) {
        let fileManager = FileManager.default
        let tempDir = fileManager.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try fileManager.createDirectory(at: tempDir, withIntermediateDirectories: true)
        
        print("DEBUG: Converting CBR \(sourceURL.lastPathComponent) to HTML...")
        
        do {
            let archive = try Archive(path: sourceURL.path)
            let entries = try archive.entries()
            
            var imageFiles: [String] = []
            let imageExtensions = Set(["jpg", "jpeg", "png", "gif", "webp"])
            
            for entry in entries {
              
                    let fileName = entry.fileName
                    let ext = (fileName as NSString).pathExtension.lowercased()
                    if imageExtensions.contains(ext) {
                        imageFiles.append(fileName)
                        
                        // Extract file
                        let destinationURL = tempDir.appendingPathComponent(fileName)
                        let destDir = destinationURL.deletingLastPathComponent()
                        if !fileManager.fileExists(atPath: destDir.path) {
                            try fileManager.createDirectory(at: destDir, withIntermediateDirectories: true)
                        }
                        
                        let data = try archive.extract(entry)
                        try data.write(to: destinationURL)
                    }
                
            }
            
            // Natural sort
            imageFiles.sort { $0.localizedStandardCompare($1) == .orderedAscending }
            
            if imageFiles.isEmpty {
                throw ConversionError.unrarFailed("No images found in CBR archive.")
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
            try indexHTML.write(to: indexURL, atomically: true, encoding: .utf8)
            
            return (indexURL, tempDir)
            
        } catch {
            print("ERROR: CBR conversion failed: \(error)")
            throw ConversionError.unrarFailed(error.localizedDescription)
        }
    }
}
