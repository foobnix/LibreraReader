//
//  Books3App.swift
//  Books3
//
//  Created by Ivan Ivanenko on 04.02.2026.
//

import SwiftUI

@main
struct LibreraMacApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        
        WindowGroup(for: ReaderWindowData.self) { $data in
            if let data = data {
                ReaderContainerView(
                    url: data.url,
                    rootURL: data.rootURL,
                    title: data.title,
                    bookPath: data.bookPath
                )
            }
        }
        .windowResizability(.contentSize)
        .defaultSize(width: 900, height: 700)
    }
}

struct ReaderWindowData: Codable, Hashable {
    let url: URL
    let rootURL: URL
    let title: String
    let bookPath: String
}
