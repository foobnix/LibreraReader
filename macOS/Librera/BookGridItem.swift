import SwiftUI

struct BookGridItem: View {
    let book: Book
    @State private var readingProgress: Double = 0
    
    #if os(macOS)
    private let itemWidth: CGFloat = 140
    private let itemHeight: CGFloat = 210
    #else
    private let itemWidth: CGFloat = 80
    private let itemHeight: CGFloat = 120
    #endif
    
    private func updateProgress() {
        let pref = BookPreferencesManager.shared.load(for: book.url.path)
        readingProgress = pref.scrollProgress
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .bottom) {
                ZStack(alignment: .bottomTrailing) {
                    // Cover Image
                    Group {
                        if let image = book.thumbnail {
                            Image(platformImage: image)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } else {
                            Rectangle()
                                .fill(Color.gray.opacity(0.2))
                                .overlay(
                                    Image(systemName: "text.book.closed")
                                        .font(.system(size: 40))
                                        .foregroundColor(.gray)
                                )
                        }
                    }
                    .frame(width: itemWidth, height: itemHeight)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .shadow(radius: 4, y: 2)
                    
                    // Format Badge
                    Text(book.type.rawValue.uppercased())
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(book.type == .pdf ? Color.red.opacity(0.8) : Color.blue.opacity(0.8))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                        .padding(6)
                }
                
                // Progress Bar
                if readingProgress > 0 {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Rectangle()
                                .fill(Color.black.opacity(0.3))
                            Rectangle()
                                .fill(Color.green)
                                .frame(width: geo.size.width * readingProgress)
                        }
                    }
                    .frame(height: 4)
                    .clipShape(RoundedRectangle(cornerRadius: 2))
                    .padding(.horizontal, 4)
                    .padding(.bottom, 4)
                }
            }
            .frame(width: itemWidth, height: itemHeight)
            
            // Title
            Text(book.title)
                .font(.subheadline)
                .fontWeight(.medium)
                .lineLimit(2)
                .truncationMode(.tail)
                .frame(width: itemWidth, alignment: .leading)
                #if os(macOS)
                .help(book.title)
                #endif
        }
        .padding(8)
        .background(Color(PlatformColor.controlBackgroundColor).opacity(0.5))
        .cornerRadius(12)
        .onAppear {
            updateProgress()
        }
        .onReceive(NotificationCenter.default.publisher(for: .bookProgressChanged)) { notification in
            if let path = notification.object as? String, path == book.url.path {
                updateProgress()
            }
        }
    }
}
