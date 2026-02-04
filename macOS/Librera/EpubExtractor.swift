import Foundation

struct EpubExtractor {
    enum ExtractionError: Error, LocalizedError {
        case unzipFailed
        case containerNotFound
        case rootfileNotFound(String)
        case spineNotFound(String)
        case contentReadFailed(String)
        
        var errorDescription: String? {
            switch self {
            case .unzipFailed: return "Failed to unzip EPUB file. It might be corrupted."
            case .containerNotFound: return "EPUB structure error: META-INF/container.xml not found."
            case .rootfileNotFound(let path): return "EPUB root file (.opf) not found at: \(path)"
            case .spineNotFound(let details): return "Failed to parse book content (spine): \(details)"
            case .contentReadFailed(let details): return "Failed to read chapter content: \(details)"
            }
        }
    }
    
    // MARK: - Helper Methods
    
    static func rewritePaths(in html: String, chapterDir: URL, opfDir: URL, spineMap: [String: Int]) -> String {
        var newHTML = html
        // Pattern matches src="value" or href="value" or xlink:href="value"
        // Groups: 1=attr name (ignoring), 2=quote, 3=path, 4=quote
        let pattern = "(src|href|xlink:href)=([\"'])([^\"']+)([\"'])"
        
        do {
            let regex = try NSRegularExpression(pattern: pattern, options: .caseInsensitive)
            let matches = regex.matches(in: html, range: NSRange(html.startIndex..., in: html))
            
            // Iterate backwards to replace without invalidating ranges
            for match in matches.reversed() {
                if let range = Range(match.range(at: 3), in: html),
                   let attrRange = Range(match.range(at: 1), in: html) {
                    let path = String(html[range])
                    let attr = String(html[attrRange]).lowercased()
                    
                    // Skip anchors (internal to page), absolute URLs, or data URIs
                    if path.hasPrefix("#") || path.contains("://") || path.hasPrefix("data:") {
                        continue
                    }
                    
                    // Resolve absolute path of the target
                    // Note: path might include query or fragments, we should strip them for file lookup
                    let components = URLComponents(string: path)
                    let cleanPath = components?.path ?? path
                    let fragment = components?.fragment
                    
                    let itemURL = chapterDir.appendingPathComponent(cleanPath)
                    
                    // Check if this is a link to another chapter
                    // We need the path relative to OPF dir to check against spineMap
                    // Using existing getRelativePath logic (but standardized)
                    let relPathToOpf = getRelativePath(from: opfDir, to: itemURL)
                    
                    if attr == "href" || attr == "xlink:href" {
                        // Check spine map
                        if let index = spineMap[relPathToOpf] {
                            // It's a link to a chapter!
                            // Rewrite to #chapter_N OR #fragment if present
                            
                            if let frag = fragment, !frag.isEmpty {
                                // e.g. chapter2.html#section1 -> #section1
                                newHTML.replaceSubrange(range, with: "#\(frag)")
                            } else {
                                // e.g. chapter2.html -> #chapter_1
                                newHTML.replaceSubrange(range, with: "#chapter_\(index)")
                            }
                            continue
                        }
                    }
                    
                    // If not a spine link, treat as resource or external file
                    // 2. Relative path from OPF dir (for images/css)
                    let newPath = getRelativePath(from: opfDir, to: itemURL)
                    
                    newHTML.replaceSubrange(range, with: newPath)
                }
            }
        } catch {
            print("Regex error: \(error)")
        }
        
        return newHTML
    }
    
    static func getRelativePath(from base: URL, to dest: URL) -> String {
        // Standardize paths
        let destPath = dest.standardized.path
        let basePath = base.standardized.path
        
        // Simple case: dest is inside base
        if destPath.hasPrefix(basePath) {
            var relative = destPath.dropFirst(basePath.count)
            if relative.hasPrefix("/") { relative = relative.dropFirst() }
            return String(relative)
        }
        
        // If not, we might need ..
        // For this app, everything is usually inside the unzipped folder.
        // If the path drifted out, we just return the path (it might fail but better than crashing).
        return destPath // Fallback
    }
    
    static func sanitizeMedia(html: String) -> String {
        // Remove 'autoplay' attribute from <audio> and <video> tags
        // This prevents music from starting automatically when opening an EPUB
        let pattern = "(<(audio|video)[^>]*)\\s+autoplay([^>]*>)"
        let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive)
        let range = NSRange(html.startIndex..., in: html)
        return regex?.stringByReplacingMatches(in: html, options: [], range: range, withTemplate: "$1$3") ?? html
    }
    
    // MARK: - XML Parser
    
    class OpfParser: NSObject, XMLParserDelegate {
        private let parser: XMLParser
        private var spineIdRefs: [String] = []
        private var manifest: [String: String] = [:] // id -> href
        private var inSpine = false
        private var inManifest = false
        
        init(xmlData: Data) {
            self.parser = XMLParser(data: xmlData)
            super.init()
            self.parser.delegate = self
        }
        
        func parseSpine() -> [String] {
            parser.parse()
            
            if let error = parser.parserError {
                print("ERROR: OPF XML Parsing error: \(error.localizedDescription)")
            }
            
            print("DEBUG: Manifest contains \(manifest.count) items.")
            print("DEBUG: Spine idrefs count: \(spineIdRefs.count)")
            
            // Map spine IDs to Hrefs
            let hrefs = spineIdRefs.compactMap { idref -> String? in
                if let href = manifest[idref] {
                    return href
                } else {
                    print("DEBUG: Warning: idref '\(idref)' not found in manifest.")
                    return nil
                }
            }
            return hrefs
        }
        
        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
            // Support both element name and qualified name for reliability
            let tag = qName ?? elementName 
            
            if tag.contains("manifest") {
                inManifest = true
            } else if tag.contains("spine") {
                inSpine = true
            }
            
            if tag.contains("item") && !tag.contains("itemref"), inManifest {
                if let id = attributeDict["id"], let href = attributeDict["href"] {
                    manifest[id] = href
                }
            }
            
            if tag.contains("itemref"), inSpine {
                if let idref = attributeDict["idref"] {
                    spineIdRefs.append(idref)
                }
            }
        }
        
        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            let tag = qName ?? elementName
            if tag.contains("manifest") {
                inManifest = false
            } else if tag.contains("spine") {
                inSpine = false
            }
        }
    }
    
    // MARK: - Main Extraction Logic
    
    /// Extracts the EPUB at sourceURL to a temporary directory, combining all chapters into a single index.html.
    /// Returns (indexHTMLURL, extractionRootURL).
    static func extractEpub(sourceURL: URL) async throws -> (URL, URL) {
        let fileManager = FileManager.default
        let tempDir = fileManager.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        
        print("DEBUG: Extracting EPUB to: \(tempDir.path)")
        
        try fileManager.createDirectory(at: tempDir, withIntermediateDirectories: true)
        
        // 1. Unzip
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/ditto")
        process.arguments = ["-x", "-k", sourceURL.path, tempDir.path]
        
        try process.run()
        process.waitUntilExit()
        
        if process.terminationStatus != 0 {
            throw ExtractionError.unzipFailed
        }
        
        // 2. Find OPF path from container.xml
        let containerURL = tempDir.appendingPathComponent("META-INF/container.xml")
        guard let containerData = try? Data(contentsOf: containerURL),
              let containerString = String(data: containerData, encoding: .utf8) else {
            print("ERROR: Could not find or read container.xml at \(containerURL.path)")
            throw ExtractionError.containerNotFound
        }
        
        guard let range = containerString.range(of: "full-path=\""),
              let endRange = containerString[range.upperBound...].range(of: "\"") else {
            print("ERROR: Could not find 'full-path' attribute in container.xml")
            throw ExtractionError.rootfileNotFound("full-path attribute missing in container.xml")
        }
        
        let opfPath = String(containerString[range.upperBound..<endRange.lowerBound])
        let opfURL = tempDir.appendingPathComponent(opfPath)
        let opfDir = opfURL.deletingLastPathComponent()
        
        // 3. Parse OPF to find all spine items
        guard let opfData = try? Data(contentsOf: opfURL) else {
            print("ERROR: Could not find or read OPF file at \(opfURL.path)")
            throw ExtractionError.rootfileNotFound(opfPath)
        }
        
        let parser = OpfParser(xmlData: opfData)
        let spineHrefs = parser.parseSpine()
        
        if spineHrefs.isEmpty {
            print("ERROR: Spine collection is empty for book at \(sourceURL.lastPathComponent)")
            throw ExtractionError.spineNotFound("No readable chapters found in book structure.")
        }
        
        // 4. Pre-calculate Spine Map for Link Rewriting
        var spineMap: [String: Int] = [:]
        for (index, href) in spineHrefs.enumerated() {
             let chapterURL = opfDir.appendingPathComponent(href)
             // Standardize relative to OPF dir
             spineMap[chapterURL.standardizedFileURL.path] = index // Store full path or relative?
             // Actually rewritePaths calculates `relPathToOpf` from `chapterDir + href`. 
             // That returns a standardized path if inside the dir.
             // Let's store the relative path string as the key, as returned by getRelativePath.
             
             // Wait, getRelativePath logic:
             let itemURL = opfDir.appendingPathComponent(href)
             let rel = getRelativePath(from: opfDir, to: itemURL)
             spineMap[rel] = index
        }
        
        // 5. Stitch content
        var combinedBody = ""
        var stylesheets: Set<String> = []
        
        print("DEBUG: Found \(spineHrefs.count) chapters.")
        
        for (index, href) in spineHrefs.enumerated() {
            let chapterURL = opfDir.appendingPathComponent(href)
            let standardPath = chapterURL.standardizedFileURL.path
            
            if !fileManager.fileExists(atPath: standardPath) {
                print("DEBUG: Skipping missing chapter: \(href)")
                continue
            }
            
            guard let chapterData = try? Data(contentsOf: URL(fileURLWithPath: standardPath)),
                  let chapterString = String(data: chapterData, encoding: .utf8) else {
                continue
            }
            
            // Extract body content
            if let bodyRange = chapterString.range(of: "<body", options: .caseInsensitive),
               let bodyEndRange = chapterString.range(of: "</body>", options: .caseInsensitive) {
                
                if let bodyTagEnd = chapterString[bodyRange.upperBound...].range(of: ">") {
                    let contentStart = bodyTagEnd.upperBound
                    let contentEnd = bodyEndRange.lowerBound
                    
                    if contentStart < contentEnd {
                        var bodyContent = String(chapterString[contentStart..<contentEnd])
                        
                        let chapterDir = URL(fileURLWithPath: standardPath).deletingLastPathComponent()
                        bodyContent = rewritePaths(in: bodyContent, chapterDir: chapterDir, opfDir: opfDir, spineMap: spineMap)
                        
                        // Prevent media autoplay
                        bodyContent = sanitizeMedia(html: bodyContent)
                        
                        combinedBody += "\n<div id=\"chapter_\(index)\" class=\"chapter\">\n"
                        combinedBody += bodyContent
                        combinedBody += "\n</div>\n"
                    }
                }
            }
            
            // Extract stylesheets (Simple Regex)
            let linkPattern = "<link[^>]+rel=[\"']stylesheet[\"'][^>]*>"
            if let regex = try? NSRegularExpression(pattern: linkPattern, options: .caseInsensitive) {
                let matches = regex.matches(in: chapterString, range: NSRange(chapterString.startIndex..., in: chapterString))
                for match in matches {
                    let linkTag = String(chapterString[Range(match.range, in: chapterString)!])
                    if let hrefRange = linkTag.range(of: "href=[\"']([^\"']+)[\"']", options: .regularExpression),
                       let urlStart = linkTag[hrefRange].firstIndex(of: "\"") ?? linkTag[hrefRange].firstIndex(of: "'") {
                        let urlPart = linkTag[hrefRange].dropFirst(linkTag.distance(from: linkTag.startIndex, to: urlStart) + 1).dropLast()
                        // Resolve valid CSS
                        stylesheets.insert(String(linkTag[hrefRange]))
                    }
                }
            }
        }
        
        // 6. Generate index.html
        var headContent = "<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
        
        headContent += """
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
                line-height: 1.6; 
                padding: 20px 40px;
                margin: 0;
            }
            img { max-width: 100%; height: auto; }
            .chapter { margin-bottom: 50px; border-bottom: 1px solid #eee; padding-bottom: 20px; }
        </style>
        """
        
        if combinedBody.isEmpty {
            print("WARNING: Combined body content is empty! Parsing failed or chapters were empty.")
            combinedBody = "<h1 style='color:red;'>Error: No content found in EPUB.</h1><p>Check console logs for details.</p>"
        }
        
        let indexHTML = """
        <!DOCTYPE html>
        <html>
        <head>
        \(headContent)
        </head>
        <body>
        \(combinedBody)
        </body>
        </html>
        """
        
        let indexURL = opfDir.appendingPathComponent("index.html")
        try indexHTML.write(to: indexURL, atomically: true, encoding: .utf8)
        
        print("DEBUG: Created index.html at: \(indexURL.path)")
        
        return (indexURL, tempDir)
    }
}