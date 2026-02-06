import Foundation

struct Fb2Converter {
    enum ConversionError: Error, LocalizedError {
        case fileReadFailed(URL, String)
        case parsingFailed(String)
        case templateWriteFailed(String)
        
        var errorDescription: String? {
            switch self {
            case .fileReadFailed(let url, let reason):
                return "Failed to read FB2 file at \(url.path): \(reason)"
            case .parsingFailed(let reason):
                return "Failed to parse FB2 XML: \(reason)"
            case .templateWriteFailed(let reason):
                return "Failed to write reader template: \(reason)"
            }
        }
    }
    
    /// Converts FB2 file to a directory containing index.html and its images.
    /// Returns (indexHTMLURL, extractionRootURL).
    static func convertFb2(sourceURL: URL) async throws -> (URL, URL) {
        let fileManager = FileManager.default
        let tempDir = fileManager.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try fileManager.createDirectory(at: tempDir, withIntermediateDirectories: true)
        
        print("DEBUG: Converting FB2 \(sourceURL.lastPathComponent) to HTML...")
        
        let data: Data
        do {
            data = try Data(contentsOf: sourceURL)
        } catch {
            print("ERROR: Could not read FB2 file: \(error)")
            throw ConversionError.fileReadFailed(sourceURL, error.localizedDescription)
        }
        
        let (xmlContent, detectedEncoding) = sanitizeXmlData(data)
        
        guard let cleanData = xmlContent.data(using: .utf8) else {
            print("ERROR: Could not convert sanitized XML to UTF-8 data")
            throw ConversionError.parsingFailed("Data conversion to UTF-8 failed.")
        }
        
        let parser = Fb2Parser(xmlData: cleanData, tempDir: tempDir)
        let htmlBody = parser.parse()
        
        if let parseError = parser.getError() {
            print("ERROR: XML Parsing failed (Encoding: \(detectedEncoding ?? "unknown")): \(parseError)")
            throw ConversionError.parsingFailed(parseError.localizedDescription)
        }
        
        if htmlBody.isEmpty {
            print("ERROR: Generated HTML body is empty. Parsing might have failed.")
            throw ConversionError.parsingFailed("No content found in XML structure.")
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
            .title { font-size: 1.5em; font-weight: bold; margin: 1em 0; text-align: center; }
            .section { margin-bottom: 2em; }
            p { margin: 0.5em 0; }
            cite { font-style: italic; margin-left: 2em; display: block; }
            poem { margin: 1em 0; white-space: pre-wrap; }
            stanza { display: block; margin-bottom: 1em; }
            v { display: block; margin-left: 1em; }
        </style>
        """
        
        let indexHTML = """
        <!DOCTYPE html>
        <html>
        <head>
        \(headContent)
        </head>
        <body>
        \(htmlBody)
        </body>
        </html>
        """
        
        let indexURL = tempDir.appendingPathComponent("index.html")
        do {
            try indexHTML.write(to: indexURL, atomically: true, encoding: .utf8)
        } catch {
            print("ERROR: Could not write index.html: \(error)")
            throw ConversionError.templateWriteFailed(error.localizedDescription)
        }
        
        return (indexURL, tempDir)
    }
    
    private class Fb2Parser: NSObject, XMLParserDelegate {
        private let parser: XMLParser
        private let tempDir: URL
        private var parseError: Error?
        
        private var html = ""
        private var currentElement = ""
        private var currentBinaryId = ""
        private var currentBinaryData = ""
        
        private var isInsideBody = false
        private var isInsideBinary = false
        
        init(xmlData: Data, tempDir: URL) {
            self.parser = XMLParser(data: xmlData)
            self.tempDir = tempDir
            super.init()
            self.parser.delegate = self
        }
        
        func parse() -> String {
            parser.parse()
            return html
        }
        
        func getError() -> Error? {
            return parseError ?? parser.parserError
        }
        
        func parser(_ parser: XMLParser, parseErrorOccurred parseError: Error) {
            self.parseError = parseError
            print("ERROR: Fb2Parser error: \(parseError.localizedDescription)")
        }
        
        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
            currentElement = elementName
            
            switch elementName {
            case "body":
                isInsideBody = true
            case "section":
                html += "<div class=\"section\">"
            case "title":
                html += "<div class=\"title\">"
            case "p":
                html += "<p>"
            case "cite":
                html += "<blockquote><cite>"
            case "poem":
                html += "<div class=\"poem\">"
            case "stanza":
                html += "<div class=\"stanza\">"
            case "v":
                html += "<div class=\"v\">"
            case "image":
                if let href = attributeDict["l:href"] ?? attributeDict["xlink:href"] {
                    // Remove # prefix if exists
                    let id = href.hasPrefix("#") ? String(href.dropFirst()) : href
                    html += "<img src=\"\(id)\">"
                }
            case "binary":
                isInsideBinary = true
                currentBinaryId = attributeDict["id"] ?? ""
                currentBinaryData = ""
            default:
                break
            }
        }
        
        func parser(_ parser: XMLParser, foundCharacters string: String) {
            if isInsideBinary {
                currentBinaryData += string
            } else if isInsideBody {
                // Escape simple HTML characters just in case, but usually XML handles this
                html += string
            }
        }
        
        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            switch elementName {
            case "body":
                isInsideBody = false
            case "section":
                html += "</div>"
            case "title":
                html += "</div>"
            case "p":
                html += "</p>"
            case "cite":
                html += "</cite></blockquote>"
            case "poem":
                html += "</div>"
            case "stanza":
                html += "</div>"
            case "v":
                html += "</div>"
            case "binary":
                isInsideBinary = false
                saveBinary(id: currentBinaryId, base64: currentBinaryData)
            default:
                break
            }
        }
        
        private func saveBinary(id: String, base64: String) {
            guard !id.isEmpty, !base64.isEmpty else { return }
            // Clean base64 string (remove whitespace/newlines)
            let cleaned = base64.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "\n", with: "").replacingOccurrences(of: "\r", with: "")
            if let data = Data(base64Encoded: cleaned) {
                let fileURL = tempDir.appendingPathComponent(id)
                try? data.write(to: fileURL)
            }
        }
    }
    
    private static func sanitizeXmlData(_ data: Data) -> (String, String?) {
        // 1. Try to detect encoding from XML header
        var encodingName: String?
        if let headerString = String(data: data.prefix(200), encoding: .ascii) {
            let pattern = "encoding=[\"']([^\"']+)[\"']"
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
               let match = regex.firstMatch(in: headerString, range: NSRange(headerString.startIndex..., in: headerString)) {
                if let range = Range(match.range(at: 1), in: headerString) {
                    encodingName = String(headerString[range])
                }
            }
        }
        
        // 2. Convert to string using detected encoding or fallbacks
        var xmlString: String?
        var usedEncoding: String?
        
        if let name = encodingName {
            let cfEncoding = CFStringConvertIANACharSetNameToEncoding(name as CFString)
            if cfEncoding != kCFStringEncodingInvalidId {
                let nsEncoding = CFStringConvertEncodingToNSStringEncoding(cfEncoding)
                xmlString = String(data: data, encoding: String.Encoding(rawValue: nsEncoding))
                usedEncoding = name
            }
        }
        
        if xmlString == nil {
            // Try Windows-1251 (CP1251) which is extremely common for FB2
            xmlString = String(data: data, encoding: .windowsCP1251)
            usedEncoding = "windows-1251"
        }
        
        if xmlString == nil {
            // Fallback to UTF-8
            xmlString = String(data: data, encoding: .utf8)
            usedEncoding = "utf-8"
        }
        
        guard var content = xmlString else {
            return ("", nil)
        }
        
        // 3. Replace common HTML entities that are not standard XML
        let entities = [
            "&nbsp;": "&#160;",
            "&mdash;": "&#8212;",
            "&ndash;": "&#8211;",
            "&laquo;": "&#171;",
            "&raquo;": "&#187;",
            "&rdquo;": "&#8221;",
            "&ldquo;": "&#8220;",
            "&lsquo;": "&#8216;",
            "&rsquo;": "&#8217;",
            "&copy;": "&#169;",
            "&reg;": "&#174;",
            "&trade;": "&#8482;",
            "&hellip;": "&#8230;"
        ]
        
        for (entity, replacement) in entities {
            content = content.replacingOccurrences(of: entity, with: replacement)
        }
        
        return (content, usedEncoding)
    }
}
