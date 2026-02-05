import Foundation
import SwiftUI

enum BookType: String, Codable {
    case pdf
    case epub
    case fb2
    case mobi
    case azw
    case azw3
    case cbz
    case cbr
    case unknown
}

struct Book: Identifiable, Hashable {
    let id: UUID
    let url: URL
    let title: String
    let type: BookType
    let date: Date
    var thumbnail: PlatformImage?
    
    init(url: URL, date: Date = Date()) {
        self.id = UUID()
        self.url = url
        self.date = date
        self.title = url.deletingPathExtension().lastPathComponent
        
        let ext = url.pathExtension.lowercased()
        if ext == "pdf" {
            self.type = .pdf
        } else if ext == "epub" {
            self.type = .epub
        } else if ext == "fb2" {
            self.type = .fb2
        } else if ext == "mobi" {
            self.type = .mobi
        } else if ext == "azw" {
            self.type = .azw
        } else if ext == "azw3" {
            self.type = .azw3
        } else if ext == "cbz" {
            self.type = .cbz
        } else if ext == "cbr" {
            self.type = .cbr
        } else {
            self.type = .unknown
        }
    }
}
