import SwiftUI
import Combine

@Observable
class BookManager {
    static let shared = BookManager() // Singleton for app-level URL handling and shared state
    
    var books: [Book] = []
    var currentFolderURL: URL?
    var isLoading: Bool = false
    var recentBookPaths: [String] = []
    var requestToOpenURL: URL?
    
    private let bookmarkKey = "LastOpenedFolderBookmark"
    private let recentBooksKey = "RecentBookPaths"
    
    init() {
        loadRecentBookPaths()
    }
    
    private func loadRecentBookPaths() {
        recentBookPaths = UserDefaults.standard.stringArray(forKey: recentBooksKey) ?? []
    }
    
    func addToRecents(_ book: Book) {
        let path = book.url.path
        if let index = recentBookPaths.firstIndex(of: path) {
            recentBookPaths.remove(at: index)
        }
        recentBookPaths.insert(path, at: 0)
        
        // Limit to 20 recent books
        if recentBookPaths.count > 20 {
            recentBookPaths.removeLast()
        }
        
        UserDefaults.standard.set(recentBookPaths, forKey: recentBooksKey)
    }
    
    var recentBooks: [Book] {
        // Find existing books that match the recent paths
        // We might need to handle books not currently in 'books' if they were from another folder
        // For now, let's just filter from loaded books for simplicity, 
        // but ideally we should be able to load them directly.
        recentBookPaths.compactMap { path in
            let url = URL(fileURLWithPath: path)
            // If the book is already in the scanned list, return it (to reuse thumbnail etc)
            if let existing = books.firstIndex(where: { $0.url.path == path }) {
                return books[existing]
            }
            // Otherwise create a temporary one (it will eventually get a thumbnail if we re-scan)
            return Book(url: url)
        }
    }
    
    func openFolder() {
        #if os(macOS)
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = false
        panel.prompt = "Select Book Library"
        
        panel.begin { response in
            if response == .OK, let url = panel.url {
                self.loadFolder(at: url)
            }
        }
        #endif
    }
    
    func loadFolder(at url: URL) {
        // Stop accessing previous resource if any
        if let previous = currentFolderURL {
            previous.stopAccessingSecurityScopedResource()
        }
        
        // Start accessing new resource
        let accessing = url.startAccessingSecurityScopedResource()
        if !accessing {
            print("Failed to access security scoped resource: \(url)")
            // Proceed anyway as it might be a normal folder selection not requiring scope yet or already has access
        }
        
        self.currentFolderURL = url
        self.saveBookmark(for: url)
        self.scanFiles(in: url)
    }
    
    private func scanFiles(in url: URL) {
        isLoading = true
        books = []
        
        #if os(macOS)
        let activity = ProcessInfo.processInfo.beginActivity(options: [.background, .suddenTerminationDisabled, .automaticTerminationDisabled], reason: "Scanning and generating thumbnails for books")
        #endif
        
        Task {
            #if os(macOS)
            defer { ProcessInfo.processInfo.endActivity(activity) }
            #endif
            
            defer {
                Task { @MainActor in
                    self.isLoading = false
                    print("INFO: Finished scanning folder: \(url.path). Found \(self.books.count) books.")
                }
            }
            
            let fileManager = FileManager.default
            let keys: [URLResourceKey] = [.isRegularFileKey, .contentModificationDateKey]
            let options: FileManager.DirectoryEnumerationOptions = [.skipsHiddenFiles, .skipsPackageDescendants]
            
            guard let enumerator = fileManager.enumerator(at: url, includingPropertiesForKeys: keys, options: options, errorHandler: { (url, error) -> Bool in
                print("Error scanning \(url.path): \(error.localizedDescription)")
                return true
            }) else {
                return
            }
            
            var batch: [Book] = []
            let batchSize = 20
            
            for case let fileURL as URL in enumerator {
                let ext = fileURL.pathExtension.lowercased()
                let supported = ["pdf", "epub", "fb2", "mobi", "azw", "azw3", "cbz", "cbr"]
                
                if supported.contains(ext) {
                    let resourceValues = try? fileURL.resourceValues(forKeys: Set(keys))
                    let modDate = resourceValues?.contentModificationDate ?? Date()
                    let newBook = Book(url: fileURL, date: modDate)
                    batch.append(newBook)
                    
                    if batch.count >= batchSize {
                        let currentBatch = batch
                        batch = []
                        await MainActor.run {
                            self.books.append(contentsOf: currentBatch)
                        }
                    }
                }
            }
            
            // Final batch
            if !batch.isEmpty {
                let currentBatch = batch
                await MainActor.run {
                    self.books.append(contentsOf: currentBatch)
                }
            }
            
            // Generate thumbnails lazily for all books found
            let allBooks = await MainActor.run { self.books }
            for book in allBooks {
                // Skip if already has thumbnail (though it shouldn't here as we just scanned)
                if book.thumbnail != nil { continue }
                
                Task {
                    if let thumb = await ThumbnailGenerator.shared.generateThumbnail(for: book.url) {
                        await MainActor.run {
                            if let index = self.books.firstIndex(where: { $0.id == book.id }) {
                                self.books[index].thumbnail = thumb
                            }
                        }
                    }
                }
            }
        }
    }
    
    private func saveBookmark(for url: URL) {
        do {
            #if os(macOS)
            let options: URL.BookmarkCreationOptions = .withSecurityScope
            #else
            let options: URL.BookmarkCreationOptions = .minimalBookmark
            #endif
            
            let bookmarkData = try url.bookmarkData(options: options, includingResourceValuesForKeys: nil, relativeTo: nil)
            UserDefaults.standard.set(bookmarkData, forKey: bookmarkKey)
        } catch {
            print("Failed to save bookmark: \(error)")
        }
    }
    
    func restoreLastOpenedFolder() {
        guard let bookmarkData = UserDefaults.standard.data(forKey: bookmarkKey) else { return }
        
        var isStale = false
        do {
            #if os(macOS)
            let options: URL.BookmarkResolutionOptions = .withSecurityScope
            #else
            let options: URL.BookmarkResolutionOptions = []
            #endif
            
            let url = try URL(resolvingBookmarkData: bookmarkData, options: options, relativeTo: nil, bookmarkDataIsStale: &isStale)
            
            if isStale {
                print("Bookmark is stale")
                // In a perfect world we re-save it, but for now just load
                saveBookmark(for: url)
            }
            
            let accessing = url.startAccessingSecurityScopedResource()
            if accessing {
                self.currentFolderURL = url
                self.scanFiles(in: url)
            } else {
                print("Failed to access restored bookmark resource")
            }
        } catch {
            print("Failed to resolve bookmark: \(error)")
        }
    }
}
