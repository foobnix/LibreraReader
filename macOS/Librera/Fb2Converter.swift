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
        
        let parser = Fb2Parser(xmlData: data, tempDir: tempDir)
        let htmlBody = parser.parse()
        
        if let parseError = parser.getError() {
            print("ERROR: XML Parsing failed: \(parseError)")
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
}
