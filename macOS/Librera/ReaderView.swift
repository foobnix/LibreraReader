import SwiftUI
import WebKit
import PDFKit

#if os(macOS)
typealias NativeView = NSView
protocol PlatformRepresentable: NSViewRepresentable {
    func makeNSView(context: Context) -> NSView
    func updateNSView(_ nsView: NSView, context: Context)
}
#else
typealias NativeView = UIView
protocol PlatformRepresentable: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView
    func updateUIView(_ uiView: UIView, context: Context)
}
#endif

struct ReaderView: PlatformRepresentable {
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
        var observers: [Any] = []
        #if os(macOS)
        var keyMonitor: Any?
        #endif
        
        init(_ parent: ReaderView) {
            self.parent = parent
            self.savedInitialProgress = parent.initialScrollProgress
        }
        
        deinit {
            for observer in observers {
                if let noteObserver = observer as? NSObjectProtocol {
                    NotificationCenter.default.removeObserver(noteObserver)
                }
            }
            #if os(macOS)
            if let monitor = keyMonitor {
                NSEvent.removeMonitor(monitor)
            }
            #endif
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
            // Inject SmoothScroller and scroll tracking
            let js = """
            (function() {
                // Prevent multiple injections
                if (window.SmoothScroller) return;

                class SmoothScroller {
                    constructor() {
                        this.targetY = window.scrollY;
                        this.currentY = window.scrollY;
                        this.velocity = 0;
                        this.isAnimating = false;
                        this.rafId = null;
                        this.lastScrollTime = 0;
                        
                        // Configuration
                        this.friction = 0.15; // Higher = more friction, stops faster
                        this.wheelMultiplier = 2.0; // Velocity multiplier
                        this.arrowStep = 200; // Pixels per arrow key press
                        
                        // Bind events
                        window.addEventListener('wheel', this.onWheel.bind(this), { passive: false });
                        
                        // Handle internal links and programmatic scrolls by updating target
                        window.addEventListener('scroll', this.onNativeScroll.bind(this));
                        
                        // Report progress to Swift
                        this.progressInterval = setInterval(this.reportProgress.bind(this), 200);
                        
                        // Expose to window for external control
                        window.smoothScrollBy = this.scrollBy.bind(this);
                        window.smoothScrollPageDown = this.scrollPageDown.bind(this);
                        window.smoothScrollPageUp = this.scrollPageUp.bind(this);
                        
                        // Also listen for key events directly in webview
                         window.addEventListener('keydown', (e) => {
                            switch(e.key) {
                                case 'ArrowUp':
                                    this.scrollBy(-this.arrowStep);
                                    e.preventDefault();
                                    break;
                                case 'ArrowDown':
                                    this.scrollBy(this.arrowStep);
                                    e.preventDefault();
                                    break;
                                case 'ArrowLeft':
                                    this.scrollPageUp();
                                    e.preventDefault();
                                    break;
                                case 'ArrowRight':
                                    this.scrollPageDown();
                                    e.preventDefault();
                                    break;
                            }
                        });
                    }
                    
                    scrollPageDown() {
                        let step = window.innerHeight * 0.9;
                        this.scrollBy(step);
                    }
                    
                    scrollPageUp() {
                        let step = window.innerHeight * 0.9;
                        this.scrollBy(-step);
                    }

                    onNativeScroll() {
                        if (!this.isAnimating) {
                            this.targetY = window.scrollY;
                            this.currentY = window.scrollY;
                        }
                    }

                    reportProgress() {
                        var scrollTop = window.pageYOffset || document.documentElement.scrollTop || 0;
                        var docHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                        var viewHeight = window.innerHeight;
                        var progress = docHeight > viewHeight ? scrollTop / (docHeight - viewHeight) : 0;
                        try {
                            window.webkit.messageHandlers.scrollHandler.postMessage(progress);
                        } catch(e) {}
                    }

                    onWheel(e) {
                         // Only intercept vertical scrolling
                        if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) return;
                        
                        e.preventDefault();
                        
                        // Add velocity
                        this.targetY += e.deltaY * this.wheelMultiplier;
                        this.clampTarget();
                        this.startAnimation();
                    }
                    
                    scrollBy(amount) {
                        this.targetY += amount;
                        this.clampTarget();
                        this.startAnimation();
                    }
                    
                    clampTarget() {
                        var docHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                        var maxScroll = docHeight - window.innerHeight;
                        this.targetY = Math.max(0, Math.min(this.targetY, maxScroll));
                    }
                    
                    startAnimation() {
                        if (!this.isAnimating) {
                            this.isAnimating = true;
                            this.rafId = requestAnimationFrame(this.animate.bind(this));
                        }
                    }
                    
                    animate() {
                        // Init currentY if needed (e.g. after resize/native scroll)
                        if (Math.abs(window.scrollY - this.currentY) > 10) {
                             this.currentY = window.scrollY;
                        }
                        
                        // Lerp
                        var diff = this.targetY - this.currentY;
                        var delta = diff * this.friction;
                        
                        if (Math.abs(diff) < 0.5) {
                            this.currentY = this.targetY;
                            window.scrollTo(0, this.currentY);
                            this.isAnimating = false;
                            this.reportProgress();
                            return;
                        }
                        
                        this.currentY += delta;
                        window.scrollTo(0, this.currentY);
                        
                        this.rafId = requestAnimationFrame(this.animate.bind(this));
                    }
                }
                
                window.SmoothScroller = new SmoothScroller();
            })();
            """
            webView.evaluateJavaScript(js, completionHandler: nil)
            
            #if os(macOS)
            // Add global key monitor for this webview
            if keyMonitor == nil {
                keyMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self, weak webView] event in
                    guard let self = self, let webView = webView, webView.window?.isKeyWindow == true else { return event }
                    
                    // Check if focus is inside the window but not necessarily on the webview
                    // If focusing some text input, ignore
                    // But general window focus should trigger scroll
                    
                    switch event.keyCode {
                    case 126: // Up
                        webView.evaluateJavaScript("window.smoothScrollBy && window.smoothScrollBy(-200)", completionHandler: nil)
                        return nil
                    case 125: // Down
                        webView.evaluateJavaScript("window.smoothScrollBy && window.smoothScrollBy(200)", completionHandler: nil)
                        return nil
                    case 123: // Left
                        webView.evaluateJavaScript("window.smoothScrollPageUp && window.smoothScrollPageUp()", completionHandler: nil)
                        return nil
                    case 124: // Right
                        webView.evaluateJavaScript("window.smoothScrollPageDown && window.smoothScrollPageDown()", completionHandler: nil)
                        return nil
                    default:
                        return event
                    }
                }
            }
            #endif
        }
        
        // MARK: - PDFView Tracking
        
        func setupPDFScrollTracking(_ pdfView: PDFView) {
            self.pdfView = pdfView
            
            #if os(macOS)
            // Listen for scroll notifications from the inner scroll view
            if let scrollView = pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView {
                let observer = NotificationCenter.default.addObserver(
                    forName: NSView.boundsDidChangeNotification,
                    object: scrollView.contentView,
                    queue: .main
                ) { [weak self] _ in
                    self?.updatePDFProgress()
                }
                observers.append(observer)
            }
            
            // Handle arrow keys for PDF
            keyMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self] event in
                guard let self = self, let pdfView = self.pdfView, pdfView.window?.isKeyWindow == true else { return event }
                
                var isInside = false
                var responder = pdfView.window?.firstResponder
                while responder != nil {
                    if responder === pdfView { isInside = true; break }
                    responder = (responder as? NSView)?.superview
                }
                if !isInside { return event }

                let jumpH = pdfView.visibleRect.width / 2
                let jumpV = pdfView.visibleRect.height / 2
                
                let isFlipped = (pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView)?.subviews.first(where: { $0 is NSClipView })?.isFlipped ?? false
                let vDir: CGFloat = isFlipped ? 1.0 : -1.0
                
                switch event.keyCode {
                case 126: // Up
                    self.scrollPDF(by: CGPoint(x: 0, y: -vDir * jumpV))
                    return nil
                case 125: // Down
                    self.scrollPDF(by: CGPoint(x: 0, y: vDir * jumpV))
                    return nil
                case 123: // Left
                    self.scrollPDF(by: CGPoint(x: -jumpH, y: 0))
                    return nil
                case 124: // Right
                    self.scrollPDF(by: CGPoint(x: jumpH, y: 0))
                    return nil
                default:
                    return event
                }
            }
            #else
            // iOS PDF Scroll Tracking
            let pageObserver = NotificationCenter.default.addObserver(
                forName: .PDFViewPageChanged,
                object: pdfView,
                queue: .main
            ) { [weak self] _ in
                self?.updatePDFProgress()
            }
            observers.append(pageObserver)
            
            // For continuous scrolling on iOS, we use KVO on contentOffset
            if let scrollView = pdfView.subviews.first(where: { $0 is UIScrollView }) as? UIScrollView {
                let kvo = scrollView.observe(\.contentOffset, options: .new) { [weak self] _, _ in
                    self?.updatePDFProgress()
                }
                observers.append(kvo)
            }
            #endif
        }
        
        #if os(macOS)
        private func scrollPDF(by offset: CGPoint) {
            guard let scrollView = pdfView?.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView else { return }
            let contentView = scrollView.contentView
            let currentOrigin = contentView.bounds.origin
            
            let docFrame = scrollView.documentView?.frame ?? .zero
            let viewSize = contentView.bounds.size
            
            var newOrigin = CGPoint(x: currentOrigin.x + offset.x, y: currentOrigin.y + offset.y)
            
            newOrigin.x = max(0, min(newOrigin.x, docFrame.width - viewSize.width))
            let minY = docFrame.minY
            let maxY = docFrame.maxY - viewSize.height
            newOrigin.y = max(minY, min(newOrigin.y, maxY))
            
            NSAnimationContext.beginGrouping()
            NSAnimationContext.current.duration = 0.25
            contentView.animator().scroll(to: newOrigin)
            NSAnimationContext.endGrouping()
            
            scrollView.reflectScrolledClipView(contentView)
        }
        #endif
        
        private func updatePDFProgress() {
            guard let pdfView = pdfView else { return }
            
            #if os(macOS)
            guard let scrollView = pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView else { return }
            let visibleRect = scrollView.contentView.documentVisibleRect
            let docHeight = scrollView.documentView?.frame.height ?? 0
            let viewHeight = visibleRect.height
            let progress = docHeight > viewHeight ? 1.0 - (visibleRect.origin.y / (docHeight - viewHeight)) : 0
            #else
            // iOS/iPadOS
            guard let scrollView = pdfView.subviews.first(where: { $0 is UIScrollView }) as? UIScrollView else {
                // Fallback to page-based progress
                if let document = pdfView.document {
                    let currentPage = pdfView.currentPage
                    let pageIndex = document.index(for: currentPage ?? document.page(at: 0)!)
                    let progress = Double(pageIndex) / Double(max(1, document.pageCount - 1))
                    DispatchQueue.main.async { self.parent.scrollProgress = progress }
                }
                return
            }
            let contentOffset = scrollView.contentOffset.y
            let contentHeight = scrollView.contentSize.height
            let viewHeight = scrollView.bounds.height
            let progress = contentHeight > viewHeight ? contentOffset / (contentHeight - viewHeight) : 0
            #endif
            
            DispatchQueue.main.async {
                self.parent.scrollProgress = min(1.0, max(0.0, progress))
            }
        }
        
        func restorePDFPosition(_ pdfView: PDFView) {
            guard savedInitialProgress > 0 else { return }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                #if os(macOS)
                if let scrollView = pdfView.subviews.first(where: { $0 is NSScrollView }) as? NSScrollView {
                    let docHeight = scrollView.documentView?.frame.height ?? 0
                    let viewHeight = scrollView.contentView.documentVisibleRect.height
                    let scrollTop = (1.0 - self.savedInitialProgress) * (docHeight - viewHeight)
                    scrollView.contentView.scroll(to: CGPoint(x: 0, y: scrollTop))
                }
                #else
                if let scrollView = pdfView.subviews.first(where: { $0 is UIScrollView }) as? UIScrollView {
                    let contentHeight = scrollView.contentSize.height
                    let viewHeight = scrollView.bounds.height
                    let scrollTop = self.savedInitialProgress * (contentHeight - viewHeight)
                    scrollView.setContentOffset(CGPoint(x: 0, y: scrollTop), animated: false)
                }
                #endif
            }
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    // Abstract creation to shared method
    private func createView(context: Context) -> NativeView {
        let container = NativeView()
        
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
            #if os(macOS)
            config.preferences.setValue(true, forKey: "developerExtrasEnabled")
            #endif
            
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

    #if os(macOS)
    func makeNSView(context: Context) -> NSView { createView(context: context) }
    func updateNSView(_ nsView: NSView, context: Context) {
        context.coordinator.parent = self
        if !isPDF, let webView = nsView.subviews.first(where: { $0 is WKWebView }) as? WKWebView {
            applySettings(to: webView, preserveScroll: true)
        }
    }
    #else
    func makeUIView(context: Context) -> UIView { createView(context: context) }
    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.parent = self
        if !isPDF, let webView = uiView.subviews.first(where: { $0 is WKWebView }) as? WKWebView {
            applySettings(to: webView, preserveScroll: true)
        }
    }
    #endif
    
    func applySettings(to webView: WKWebView, preserveScroll: Bool) {
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
