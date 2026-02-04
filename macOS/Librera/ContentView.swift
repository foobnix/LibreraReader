//
//  ContentView.swift
//  Books3
//
//  Created by Ivan Ivanenko on 04.02.2026.
//

import SwiftUI

struct ContentView: View {
    @State private var bookManager = BookManager()
    @State private var isExtracting: Bool = false
    @State private var extractionError: String?
    @State private var selectedCategory: NavigationCategory? = .library
    @State private var searchText = ""
    @State private var sortOption: SortOption = .date
    @State private var sortOrder: SortOrder = .descending
    @Environment(\.openWindow) private var openWindow
    
    enum NavigationCategory: String, CaseIterable, Identifiable {
        case library = "All Books"
        case recent = "Recent Books"
        case about = "About"
        var id: String { rawValue }
        var icon: String {
            switch self {
            case .library: return "books.vertical"
            case .recent: return "clock"
            case .about: return "info.circle"
            }
        }
    }
    
    enum SortOption: String, CaseIterable, Identifiable {
        case title = "Title"
        case date = "Date"
        var id: String { rawValue }
    }
    
    enum SortOrder {
        case ascending, descending
        var icon: String {
            self == .ascending ? "arrow.up" : "arrow.down"
        }
    }
    
    let columns = [
        GridItem(.adaptive(minimum: 160), spacing: 20)
    ]
    
    var body: some View {
        NavigationSplitView {
            List(NavigationCategory.allCases.filter { $0 != .about }, selection: $selectedCategory) { category in
                NavigationLink(value: category) {
                    HStack {
                        Label(category.rawValue, systemImage: category.icon)
                        Spacer()
                        let count = category == .library ? bookManager.books.count : bookManager.recentBooks.count
                        Text("\(count)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.secondary.opacity(0.1))
                            .clipShape(Capsule())
                    }
                }
            }
            
            Spacer()
            
            List(selection: $selectedCategory) {
                NavigationLink(value: NavigationCategory.about) {
                    Label(NavigationCategory.about.rawValue, systemImage: NavigationCategory.about.icon)
                }
            }
            .frame(height: 50)
            
            .navigationTitle("Librera Mac")
        } detail: {
            Group {
                if let category = selectedCategory {
                    switch category {
                    case .library:
                        libraryView
                    case .recent:
                        recentView
                    case .about:
                        aboutView
                    }
                } else {
                    Text("Select a category")
                }
            }
            .searchable(text: $searchText, placement: .automatic, prompt: "Search Title")
        }
        .frame(minWidth: 800, minHeight: 500)
// ... intermediate part ...
        .overlay {
            if isExtracting {
                ZStack {
                    Color.black.opacity(0.4)
                    ProgressView("Opening Book...")
                        .controlSize(.large)
                        .padding()
                        .background(Material.regular)
                        .cornerRadius(12)
                }
            }
        }
        .alert("Error Opening Book", isPresented: Binding(get: { extractionError != nil }, set: { if !$0 { extractionError = nil } })) {
            Button("OK", role: .cancel) { }
        } message: {
            if let error = extractionError {
                Text(error)
            }
        }
    }
    
    private var libraryView: some View {
        bookListView(books: bookManager.books, title: "All Books")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: {
                        bookManager.openFolder()
                    }) {
                        Label("Open Folder", systemImage: "folder.badge.plus")
                    }
                }
                
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Picker("Sort By", selection: $sortOption) {
                            ForEach(SortOption.allCases) { option in
                                Text(option.rawValue).tag(option)
                            }
                        }
                        
                        Divider()
                        
                        Button(action: {
                            sortOrder = sortOrder == .ascending ? .descending : .ascending
                        }) {
                            Label(sortOrder == .ascending ? "Ascending" : "Descending", systemImage: sortOrder.icon)
                        }
                    } label: {
                        Label("Sort", systemImage: "arrow.up.arrow.down")
                    }
                }
                
                if let path = bookManager.currentFolderURL?.path {
                    ToolbarItem(placement: .principal) {
                        Text(URL(fileURLWithPath: path).lastPathComponent)
                            .font(.headline)
                    }
                }
            }
    }
    
    private var recentView: some View {
        bookListView(books: bookManager.recentBooks, title: "Recent Books")
    }
    
    private var aboutView: some View {
        VStack(spacing: 20) {
            Image(nsImage: NSApp.applicationIconImage)
                .resizable()
                .frame(width: 128, height: 128)
                .shadow(radius: 10)
            
            VStack(spacing: 8) {
                Text("Librera Mac")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.1"
                Text("Version \(version)")
                    .font(.headline)
                    .foregroundColor(.secondary)
                
                Text("Author: Ivan Ivanenko")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                Text("Librera Book reader for macOS supports PDF, EPUB, FB2, CBZ, CBR book formats")
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
                    .frame(maxWidth: 400)
                
                Link("librera.mobi", destination: URL(string: "https://librera.mobi/")!)
                    .font(.headline)
                    .padding(.top, 4)
            }
            
            Text("© 2026 Librera Team")
                .font(.caption)
                .foregroundColor(.secondary.opacity(0.7))
                .padding(.top, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(NSColor.windowBackgroundColor))
    }
    
    private func bookListView(books: [Book], title: String) -> some View {
        let sortedBooks = books.sorted { a, b in
            let result: Bool
            switch sortOption {
            case .title:
                result = a.title.localizedCaseInsensitiveCompare(b.title) == .orderedAscending
            case .date:
                result = a.date < b.date
            }
            return sortOrder == .ascending ? result : !result
        }
        
        let filteredBooks = sortedBooks.filter {
            searchText.isEmpty || $0.title.localizedCaseInsensitiveContains(searchText)
        }
        
        return ScrollView {
            if bookManager.isLoading {
                ProgressView("Scanning Library...")
                    .controlSize(.large)
                    .padding(.top, 50)
            } else if filteredBooks.isEmpty {
                VStack(spacing: 20) {
                    Image(systemName: searchText.isEmpty ? "books.vertical" : "magnifyingglass")
                        .font(.system(size: 60))
                        .foregroundColor(.secondary)
                    
                    Text(searchText.isEmpty ? (title == "All Books" ? "No Books Found" : "No Recent Books") : "No Results for \"\(searchText)\"")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    if title == "All Books" && searchText.isEmpty {
                        Text("Open a folder with book files to get started.")
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                        
                        Button("Open Folder") {
                            bookManager.openFolder()
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()
            } else {
                LazyVGrid(columns: columns, spacing: 24) {
                    ForEach(filteredBooks) { book in
                        BookGridItem(book: book)
                            .onTapGesture {
                                openBook(book)
                            }
                            .contextMenu {
                                Button("Open") {
                                    openBook(book)
                                }
                                Button("Show in Finder") {
                                    NSWorkspace.shared.activateFileViewerSelecting([book.url])
                                }
                            }
                    }
                }
                .padding()
            }
        }
        .navigationTitle(title)
    }
    
    private func openBook(_ book: Book) {
        // Track recent
        bookManager.addToRecents(book)
        
        if book.type == .pdf {
            let data = ReaderWindowData(
                url: book.url,
                rootURL: book.url.deletingLastPathComponent(),
                title: book.title,
                bookPath: book.url.path
            )
            openWindow(value: data)
        } else if book.type == .epub || book.type == .fb2 || book.type == .cbz || book.type == .cbr {
            isExtracting = true
            Task {
                do {
                    let (readerURL, rootURL): (URL, URL)
                    if book.type == .epub {
                        (readerURL, rootURL) = try await EpubExtractor.extractEpub(sourceURL: book.url)
                    } else if book.type == .fb2 {
                        (readerURL, rootURL) = try await Fb2Converter.convertFb2(sourceURL: book.url)
                    } else if book.type == .cbz {
                        (readerURL, rootURL) = try await CbzConverter.convertCbz(sourceURL: book.url)
                    } else {
                        (readerURL, rootURL) = try await CbrConverter.convertCbr(sourceURL: book.url)
                    }
                    
                    await MainActor.run {
                        let data = ReaderWindowData(
                            url: readerURL,
                            rootURL: rootURL,
                            title: book.title,
                            bookPath: book.url.path
                        )
                        openWindow(value: data)
                        self.isExtracting = false
                    }
                } catch {
                    await MainActor.run {
                        let typeStr: String
                        switch book.type {
                        case .epub: typeStr = "EPUB"
                        case .fb2: typeStr = "FB2"
                        case .cbz: typeStr = "CBZ"
                        case .cbr: typeStr = "CBR"
                        default: typeStr = "Book"
                        }
                        self.extractionError = "Failed to open \(typeStr): \(error.localizedDescription)"
                        self.isExtracting = false
                    }
                }
            }
        }
    }
}


extension URL: Identifiable {
    public var id: String { self.absoluteString }
}

#Preview {
    ContentView()
}
