import SwiftUI
import WebKit
import PDFKit

struct ReaderView: NSViewRepresentable {
    let url: URL
    let rootURL: URL
    let settings: ReaderSettings
    let initialScrollProgress: Double
    @Binding var scrollProgress: Double
    
    private var isPDF: Bool {
        url.pathExtension.lowercased() == "pdf"
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: ReaderView
        var hasLoadedContent = false
        var savedInitialProgress: Double = 0.0
        var pdfView: PDFView?
        var scrollObserver: Any?
        
        init(_ parent: ReaderView) {
            self.parent = parent
            self.savedInitialProgress = parent.initialScrollProgress
        }
        
        deinit {
            if let observer = scrollObserver {
                NotificationCenter.default.removeObserver(observer)
            }
        }
        
        // MARK: - WKWebView Delegate
        
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            print("WebView Finished Loading")
            hasLoadedContent = true
            parent.applySettings(to: webView, preserveScroll: false)
            
            if savedInitialProgress > 0 {
                let js = """
                (function() {
                    var docHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                    var viewHeight = window.innerHeight;
                    var scrollTop = \(savedInitialProgress) * (docHeight - viewHeight);
                    window.scrollTo(0, Math.max(0, scrollTop));
                })();
                """
                webView.evaluateJavaScript(js, completionHandler: nil)
            }
            
            setupWebViewScrollTracking(webView)
        }
        
        func setupWebViewScrollTracking(_ webView: WKWebView) {
            let js = """
            (function() {
                var lastUpdate = 0;
                var throttleMs = 200;
                window.addEventListener('scroll', function() {
                    var now = Date.now();
                    if (now - lastUpdate < throttleMs) return;
                    lastUpdate = now;
                    
                    var scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0;
                    var docHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                    var viewHeight = window.innerHeight;
                    var progress = docHeight > viewHeight ? scrollTop / (docHeight - viewHeight) : 0;
                    window.webkit.messageHandlers.scrollHandler.postMessage(progress);
                });
            })();
            """
            webView.evaluateJavaScript(js, completionHandler: nil)
        }
        
        // MARK: - PDFView Tracking
        
        func setupPDFScrollTracking(_ pdfView: PDFView) {
            self.pdfView = pdfView
            
            // Listen for scroll notifications from the inner scroll view
            if let scrollView = pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView {
                scrollObserver = NotificationCenter.default.addObserver(
                    forName: NSView.boundsDidChangeNotification,
                    object: scrollView.contentView,
                    queue: .main
                ) { [weak self] _ in
                    self?.updatePDFProgress()
                }
            }
        }
        
        private func updatePDFProgress() {
            guard let pdfView = pdfView, let scrollView = pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView else { return }
            
            let visibleRect = scrollView.contentView.documentVisibleRect
            let docHeight = scrollView.documentView?.frame.height ?? 0
            let viewHeight = visibleRect.height
            
            // On macOS, origin.y at top is (docHeight - viewHeight) and 0 at bottom (unless flipped)
            // So we invert the progress calculation
            let progress = docHeight > viewHeight ? 1.0 - (visibleRect.origin.y / (docHeight - viewHeight)) : 0
            
            DispatchQueue.main.async {
                self.parent.scrollProgress = min(1.0, max(0.0, progress))
            }
        }
        
        func restorePDFPosition(_ pdfView: PDFView) {
            guard savedInitialProgress > 0 else { return }
            
            // Wait slightly for PDF to layout
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                if let scrollView = pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView {
                    let docHeight = scrollView.documentView?.frame.height ?? 0
                    let viewHeight = scrollView.contentView.documentVisibleRect.height
                    
                    // Invert for restoration as well
                    let scrollTop = (1.0 - self.savedInitialProgress) * (docHeight - viewHeight)
                    
                    scrollView.contentView.scroll(to: NSPoint(x: 0, y: scrollTop))
                }
            }
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeNSView(context: Context) -> NSView {
        let container = NSView()
        
        if isPDF {
            let pdfView = PDFView()
            pdfView.document = PDFDocument(url: url)
            pdfView.autoScales = true
            pdfView.displayMode = .singlePageContinuous
            pdfView.displayDirection = .vertical
            
            pdfView.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(pdfView)
            
            NSLayoutConstraint.activate([
                pdfView.topAnchor.constraint(equalTo: container.topAnchor),
                pdfView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                pdfView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                pdfView.trailingAnchor.constraint(equalTo: container.trailingAnchor)
            ])
            
            context.coordinator.setupPDFScrollTracking(pdfView)
            context.coordinator.restorePDFPosition(pdfView)
            
        } else {
            let config = WKWebViewConfiguration()
            config.preferences.setValue(true, forKey: "developerExtrasEnabled")
            
            let scrollHandler = ScrollMessageHandler { progress in
                DispatchQueue.main.async {
                    context.coordinator.parent.scrollProgress = progress
                }
            }
            config.userContentController.add(scrollHandler, name: "scrollHandler")
            
            let webView = WKWebView(frame: .zero, configuration: config)
            webView.navigationDelegate = context.coordinator
            
            webView.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(webView)
            
            NSLayoutConstraint.activate([
                webView.topAnchor.constraint(equalTo: container.topAnchor),
                webView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                webView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                webView.trailingAnchor.constraint(equalTo: container.trailingAnchor)
            ])
            
            webView.loadFileURL(url, allowingReadAccessTo: rootURL)
        }
        
        return container
    }
    
    func updateNSView(_ nsView: NSView, context: Context) {
        context.coordinator.parent = self
        
        if !isPDF, let webView = nsView.subviews.first(where: { $0 is WKWebView }) as? WKWebView {
            applySettings(to: webView, preserveScroll: true)
        }
    }
    
    func applySettings(to webView: WKWebView, preserveScroll: Bool) {
        // Only called for EPUB (WKWebView)
        let js: String
        if preserveScroll {
            js = """
            (function() {
                var scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0;
                var docHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                var viewHeight = window.innerHeight;
                var scrollPercent = docHeight > viewHeight ? scrollTop / (docHeight - viewHeight) : 0;
                
                document.documentElement.style.setProperty('--font-size', '\(settings.fontSize)px');
                document.documentElement.style.setProperty('--font-family', '\(settings.cssFontFamily)');
                document.documentElement.style.setProperty('--bg-color', '\(settings.theme.backgroundColor)');
                document.documentElement.style.setProperty('--text-color', '\(settings.theme.textColor)');
                document.body.style.textAlign = '\(settings.textAlignment.cssValue)';
                
                requestAnimationFrame(function() {
                    var newDocHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                    var newScrollTop = scrollPercent * (newDocHeight - viewHeight);
                    window.scrollTo(0, Math.max(0, newScrollTop));
                });
            })();
            """
        } else {
            js = """
            document.documentElement.style.setProperty('--font-size', '\(settings.fontSize)px');
            document.documentElement.style.setProperty('--font-family', '\(settings.cssFontFamily)');
            document.documentElement.style.setProperty('--bg-color', '\(settings.theme.backgroundColor)');
            document.documentElement.style.setProperty('--text-color', '\(settings.theme.textColor)');
            document.body.style.textAlign = '\(settings.textAlignment.cssValue)';
            """
        }
        webView.evaluateJavaScript(js, completionHandler: nil)
    }
}

class ScrollMessageHandler: NSObject, WKScriptMessageHandler {
    let onScroll: (Double) -> Void
    init(onScroll: @escaping (Double) -> Void) { self.onScroll = onScroll }
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if let progress = message.body as? Double { onScroll(min(1.0, max(0.0, progress))) }
    }
}
