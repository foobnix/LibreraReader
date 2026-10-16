/*
 * The bridge between Librera and foliate-js.
 *
 * Librera calls window.libreraCall(id, method, argsJson) from FoliateRenderer and gets the answer
 * back through the injected `librera` interface - librera.reply(id, json) or librera.fail(id, msg).
 * Everything here runs in the WebView; nothing blocks, because the Java side is what waits.
 *
 * Geometry: every rectangle handed back is normalized to the page, 0..1 with the origin at the top
 * left, which is the shape Librera's TextWord and PageLink already use. It is worked out from the
 * section's own iframe: the paginator keeps the section in an iframe inside a scrolled container,
 * so that iframe's bounding rect in this document already has the scroll offset and the page
 * padding folded into it, and a rect inside the section only has to be shifted by it.
 */

import './foliate/view.js'

let view = null
let config = null
/** The visible Range of the page the book is on, from the paginator's own relocate event. */
let visible = null

const reply = (id, value) => window.librera.reply(id, JSON.stringify(value))
const fail = (id, error) => window.librera.fail(id, String((error && error.stack) || error))
const log = message => {
    try {
        window.librera.log(String(message))
    } catch (e) {
        // the interface is gone; nothing to do about it here
    }
}

window.addEventListener('error', event => {
    log('UNCAUGHT ' + (event.message || '') + ' at ' + (event.filename || '') + ':' + (event.lineno || 0)
        + ' ' + ((event.error && event.error.stack) || ''))
})
window.addEventListener('unhandledrejection', event => {
    log('UNHANDLED REJECTION ' + ((event.reason && event.reason.stack) || event.reason))
})

window.libreraCall = (id, method, argsJson) => {
    (async () => {
        try {
            const handler = handlers[method]
            if (!handler) throw new Error('no such method: ' + method)
            reply(id, (await handler(argsJson ? JSON.parse(argsJson) : {})) || {})
        } catch (e) {
            fail(id, e)
        }
    })()
}

/**
 * Resolves once the browser has laid out and painted, so a capture gets the page that was asked
 * for.
 *
 * Two frames is the reliable signal, but this WebView is never attached to a window, and Chromium
 * throttles - sometimes stops - requestAnimationFrame for a view it believes nobody can see. A
 * timer is therefore raced against the frames: whichever arrives first wins, so a throttled view
 * still makes progress instead of hanging every call until it times out.
 */
const painted = () => {
    // Reading a layout property forces the layout to be up to date there and then, which is the
    // part a capture actually depends on - a page drawn half way through being laid out is a
    // blank page. Both documents are asked, because the text is in the section's iframe.
    try {
        void document.documentElement.offsetHeight
        const current = contents()
        if (current && current.doc && current.doc.documentElement) {
            void current.doc.documentElement.offsetHeight
        }
    } catch (e) {
        // measuring is best effort; the wait below still happens
    }
    return new Promise(resolve => {
        let settled = false
        const finish = () => {
            if (settled) return
            settled = true
            resolve()
        }
        requestAnimationFrame(() => requestAnimationFrame(finish))
        // The WebView is parked out of sight, and a view nobody can see has its frames throttled
        // - sometimes stopped. So a timer backs the frames up rather than the call hanging. It is
        // long enough to cover a section that was slow to lay out, which 64ms was not.
        setTimeout(finish, 400)
    })
}

/** The section document currently loaded, or null. */
const contents = () => {
    const all = view && view.renderer ? view.renderer.getContents() : null
    return all && all.length ? all[0] : null
}

/** The size of this page in CSS pixels: the whole shell, which is what gets captured. */
const pageSize = () => ({
    width: document.documentElement.clientWidth,
    height: document.documentElement.clientHeight,
})

/**
 * Where a section's iframe sits in this document. A rect from inside the section is shifted by
 * this to land in the captured page.
 */
const frameOffset = doc => {
    const frame = doc && doc.defaultView ? doc.defaultView.frameElement : null
    if (!frame) return null
    const rect = frame.getBoundingClientRect()
    return { left: rect.left, top: rect.top }
}

const rectWidth = rect => rect.right - rect.left
const rectHeight = rect => rect.bottom - rect.top

/** A section rect as a fraction of the page, or null when it is not on this page. */
const normalize = (rect, offset, size) => {
    if (!rect) return null
    // Measured from the edges, not read off the rectangle: the boxes of a word broken across
    // lines are built here out of several of the browser's, and those carry edges only.
    if (!rectWidth(rect) && !rectHeight(rect)) return null
    const left = (offset.left + rect.left) / size.width
    const right = (offset.left + rect.right) / size.width
    const top = (offset.top + rect.top) / size.height
    const bottom = (offset.top + rect.bottom) / size.height
    // Anything off this page belongs to a neighbouring column. A sliver of slack either side
    // keeps a glyph that straddles the edge rather than dropping it.
    if (right <= 0.001 || left >= 0.999) return null
    return {
        l: Math.max(0, Math.min(1, left)),
        t: Math.max(0, Math.min(1, top)),
        r: Math.max(0, Math.min(1, right)),
        b: Math.max(0, Math.min(1, bottom)),
    }
}

/** The text nodes worth looking at: those the visible range covers, else the whole section. */
const textNodes = doc => {
    const root = visible && visible.commonAncestorContainer
        && doc.contains(visible.commonAncestorContainer)
        ? (visible.commonAncestorContainer.nodeType === Node.TEXT_NODE
            ? visible.commonAncestorContainer.parentNode
            : visible.commonAncestorContainer)
        : doc.body
    if (!root) return []
    const walker = doc.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
        acceptNode(node) {
            if (!node.nodeValue || !node.nodeValue.trim()) return NodeFilter.FILTER_REJECT
            const name = node.parentNode && node.parentNode.nodeName
            // Neither of these is on the page even though it is in the document
            if (name === 'SCRIPT' || name === 'STYLE' || name === 'HEAD') return NodeFilter.FILTER_REJECT
            return NodeFilter.FILTER_ACCEPT
        },
    })
    const nodes = []
    let node
    while ((node = walker.nextNode())) nodes.push(node)
    return nodes
}

/** The nearest block ancestor, so the page's text can be grouped into paragraphs. */
const blockOf = node => {
    let el = node.parentNode
    while (el && el.nodeType === Node.ELEMENT_NODE) {
        const display = el.ownerDocument.defaultView.getComputedStyle(el).display
        if (display && display !== 'inline' && display !== 'inline-block') return el
        el = el.parentNode
    }
    return node.parentNode
}

/**
 * Text as the rest of Librera should see it.
 *
 * The conversion puts a soft hyphen inside every long word so the engine can break it (see
 * HypenUtils), and a browser keeps those characters in the DOM whether or not it drew one. They
 * are invisible, so they must not reach a selection the reader copies, the text TTS speaks, or the
 * bookmark anchor - MuPDF's text layer does not carry them either, and the two engines have to
 * agree for a reading position to survive a switch between them.
 */
const readable = text => text.replace(/\u00AD/g, '')

const escapeHtml = text => text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

/**
 * Every word of the page with its rectangle, in reading order, spaces included as words of their
 * own - the shape MuPdfPage builds for Librera, so selection, search and TTS cannot tell them
 * apart. Each word is measured with a Range rather than by wrapping it in an element, so the
 * book's own layout is not disturbed by being measured.
 */
const collectWords = () => {
    const current = contents()
    if (!current) return { words: [] }
    const doc = current.doc
    const offset = frameOffset(doc)
    if (!offset) return { words: [] }
    const size = pageSize()
    const words = []
    const range = doc.createRange()

    /** Adds one word for a range of a text node that is known to sit on a single line. */
    const add = (node, from, to, text, rect) => {
        // The whitespace a line wraps on sits at the end of that line, and the browser gives it
        // the whole rest of the line's width. Highlighted, that draws a band down the right
        // margin beside every line of a selection. A space is never much wider than it is tall,
        // whatever the justification stretches it to, so that is what decides.
        if (/^\s*$/.test(text) && rectWidth(rect) > 3 * rectHeight(rect)) return
        const box = normalize(rect, offset, size)
        if (!box) return
        box.w = readable(text)
        words.push(box)
    }

    /**
     * A run of the text node as one or more words - one per line it is laid out on.
     *
     * A range that crosses a line break has a bounding box that is the union of both lines: as
     * wide as the column and as tall as two lines, which draws a selection as a block over the
     * text rather than a highlight on it. It happens constantly in a real book - the whitespace
     * between two words is usually a newline and the next line's indentation in the source, and
     * that run is exactly what a line wraps on, while a long word carries a soft hyphen it can be
     * broken at.
     *
     * So the boxes are the ones the browser reports for the run, each of which is by definition a
     * single line, and every character is handed to the box that contains it. Taking the boxes as
     * given, rather than looking for the character the line changes at, is what makes this hold
     * for a word broken at a soft hyphen - where the hyphen is drawn on the line before the break
     * and the characters either side of it are not where counting would put them.
     */
    const addRun = (node, from, to, isSpace) => {
        range.setStart(node, from)
        range.setEnd(node, to)
        const rects = Array.from(range.getClientRects())
        const text = node.nodeValue

        if (rects.length <= 1) {
            add(node, from, to, isSpace ? ' ' : text.slice(from, to),
                rects.length ? rects[0] : range.getBoundingClientRect())
            return
        }
        if (isSpace) {
            // Whitespace collapses to a single space wherever it is drawn; the rest of the run
            // renders nowhere, so the first box is the whole of it.
            add(node, from, to, ' ', rects[0])
            return
        }

        // The boxes grouped into lines. A word broken at a soft hyphen has three boxes on two
        // lines - the text before the break, the hyphen the browser drew for it, and the rest -
        // and the first two belong to one line and one highlight.
        const lines = []
        for (const rect of rects) {
            const last = lines[lines.length - 1]
            if (last && Math.abs(last.top - rect.top) <= 1) {
                last.left = Math.min(last.left, rect.left)
                last.right = Math.max(last.right, rect.right)
                last.bottom = Math.max(last.bottom, rect.bottom)
            } else {
                lines.push({ left: rect.left, top: rect.top, right: rect.right, bottom: rect.bottom })
            }
        }
        if (lines.length <= 1) {
            add(node, from, to, text.slice(from, to), range.getBoundingClientRect())
            return
        }

        // Where each line's text ends, found by growing the range from the start of the run: the
        // box of a range only reaches a line once the range does. Measuring whole ranges rather
        // than single characters is what makes this hold - a character can have no box of its own
        // at all, which is how the soft hyphen at a break behaves.
        let start = from
        for (let k = 0; k < lines.length; k++) {
            let end = start
            if (k === lines.length - 1) {
                end = to
            } else {
                const limit = lines[k].bottom + 1
                while (end < to) {
                    range.setStart(node, from)
                    range.setEnd(node, end + 1)
                    if (range.getBoundingClientRect().bottom > limit) break
                    end++
                }
            }
            if (end > start) {
                add(node, start, end, text.slice(start, end), lines[k])
            }
            start = end
        }
    }

    for (const node of textNodes(doc)) {
        const text = node.nodeValue
        let i = 0
        while (i < text.length) {
            // A run of whitespace is one word, a run of non-whitespace is one word
            const isSpace = /\s/.test(text[i])
            let end = i + 1
            while (end < text.length && /\s/.test(text[end]) === isSpace) end++
            try {
                addRun(node, i, end, isSpace)
            } catch (e) {
                // a node that moved under us; the rest of the page is still worth having
            }
            i = end
        }
    }
    range.detach && range.detach()
    return { words }
}

/**
 * The page as the pseudo-HTML Librera's TTS and its text export read: a <p> per paragraph with
 * the lines joined, and a <pause> around a paragraph set larger than the body text. This is what
 * MuPDF's own fz_print_stext_page_as_html_my writes, so TxtUtils.replaceHTMLforTTS and
 * replacePDFEndLine need no special case for this engine.
 */
const collectText = () => {
    const current = contents()
    if (!current) return { html: '', text: '' }
    const doc = current.doc
    const offset = frameOffset(doc)
    if (!offset) return { html: '', text: '' }
    const size = pageSize()
    const bodyFont = config ? config.fontSizePx / (window.devicePixelRatio || 1) : 0

    // The paragraphs of this page, in order, each with the text that is actually on it
    const blocks = []
    const seen = new Map()
    const range = doc.createRange()
    for (const node of textNodes(doc)) {
        try {
            range.selectNodeContents(node)
        } catch (e) {
            continue
        }
        if (!normalize(range.getBoundingClientRect(), offset, size)) {
            // The whole node is off this page; its words cannot be on it either
            continue
        }
        const block = blockOf(node)
        let entry = seen.get(block)
        if (!entry) {
            entry = { text: '', size: 0 }
            seen.set(block, entry)
            blocks.push(entry)
            try {
                const style = doc.defaultView.getComputedStyle(block)
                entry.size = parseFloat(style.fontSize) || 0
            } catch (e) {
                entry.size = 0
            }
        }
        // Only the part of the node that is on this page
        const text = node.nodeValue
        let i = 0
        let piece = ''
        while (i < text.length) {
            const isSpace = /\s/.test(text[i])
            let end = i + 1
            while (end < text.length && /\s/.test(text[end]) === isSpace) end++
            try {
                range.setStart(node, i)
                range.setEnd(node, end)
                if (normalize(range.getBoundingClientRect(), offset, size)) {
                    piece += isSpace ? ' ' : text.slice(i, end)
                }
            } catch (e) {
                // skip this run
            }
            i = end
        }
        entry.text += piece
    }
    range.detach && range.detach()

    let html = ''
    let plain = ''
    for (const block of blocks) {
        const text = readable(block.text).replace(/\s+/g, ' ').trim()
        if (!text) continue
        const heading = bodyFont > 0 && block.size > bodyFont
        if (heading) html += '<pause>'
        html += '<p>' + escapeHtml(text) + ' </p>\n'
        if (heading) html += '<pause>\n'
        plain += text + ' '
    }
    return { html, text: plain.trim() }
}

/** The links on this page: a target section and fraction for an internal one, a url otherwise. */
const collectLinks = async () => {
    const current = contents()
    if (!current) return { links: [] }
    const doc = current.doc
    const offset = frameOffset(doc)
    if (!offset) return { links: [] }
    const size = pageSize()
    const links = []
    const book = view.book
    const section = book.sections[current.index]
    for (const anchor of doc.querySelectorAll('a[href]')) {
        const rect = normalize(anchor.getBoundingClientRect(), offset, size)
        if (!rect) continue
        const raw = anchor.getAttribute('href')
        if (!raw) continue
        // A href in a section is relative to that section, so it has to be made book-absolute
        // before the book can resolve it - the same two steps foliate's own click handler takes.
        const href = (section && section.resolveHref) ? section.resolveHref(raw) : raw
        const external = book.isExternal ? book.isExternal(href) : /^(https?|mailto|tel):/i.test(href)
        if (external) {
            links.push(Object.assign(rect, { url: raw }))
            continue
        }
        try {
            const target = view.resolveNavigation(href)
            if (target && typeof target.index === 'number') {
                links.push(Object.assign(rect, {
                    index: target.index,
                    fraction: typeof target.anchor === 'number' ? target.anchor : 0,
                }))
            }
        } catch (e) {
            // a href this book resolves to nothing; leave it out rather than guess
        }
    }
    return { links }
}

/** The styles, with the root font size the engine was configured with. */
const applyStyles = () => {
    if (!view || !view.renderer || !config) return
    const dpr = window.devicePixelRatio || 1
    // The page is captured at device pixels while CSS lays out in CSS pixels, so the size MuPDF
    // would have been laid out with has to be divided by the ratio between them.
    const root = ':root{font-size:' + (config.fontSizePx / dpr) + 'px;}'
    view.renderer.setStyles([root + (config.before || ''), config.after || ''])
}

const handlers = {
    /** Creates the view, opens the book, and sets the page up as one paginated column. */
    async open(cfg) {
        config = cfg
        document.documentElement.style.background = cfg.background
        document.body.style.background = cfg.background

        view = document.createElement('foliate-view')
        document.body.append(view)
        view.addEventListener('relocate', event => {
            visible = event.detail ? event.detail.range : null
        })
        view.addEventListener('load', () => {
            // Each section gets the styles as it loads; the paginator re-applies them itself on a
            // section change, but the root font size is ours to keep setting.
            applyStyles()
        })

        const response = await fetch(cfg.book)
        if (!response.ok) throw new Error('could not read the book: ' + response.status)
        const blob = await response.blob()
        // The name decides the format foliate picks; it is an epub by the time it reaches here
        await view.open(new File([blob], 'book.epub', { type: 'application/epub+zip' }))

        const renderer = view.renderer
        const dpr = window.devicePixelRatio || 1
        renderer.setAttribute('flow', 'paginated')
        // Every one of the paginator's own defaults is replaced here. Left alone they would add a
        // second set of margins on top of the reader's: a 720px cap on the text column, a 48px
        // band above and below, a 7% gap, and two columns on a wide page.
        renderer.setAttribute('max-column-count', '1')
        renderer.setAttribute('max-inline-size', Math.round(cfg.width / dpr) + 'px')
        renderer.setAttribute('max-block-size', Math.round(cfg.height / dpr) + 'px')
        // The gap, halved. The paginator's grid puts `minmax(half-gap, 1fr)` and then another
        // `half-gap` on each side of the column, and with the column allowed the full width of
        // the page the `1fr` collapses to its own minimum - so each side ends up carrying two
        // half-gaps, and the whole gap is laid out twice over.
        const sides = Math.max(0, Math.min(30, (cfg.sidesPx || 0) / 2 / cfg.width * 100))
        renderer.setAttribute('gap', sides.toFixed(2) + '%')
        renderer.setAttribute('margin', Math.round((cfg.bandPx || 0) / dpr) + 'px')
        applyStyles()

        await renderer.goTo({ index: 0, anchor: 0 })
        await painted()
        log('open: ' + view.book.sections.length + ' sections, page '
            + document.documentElement.clientWidth + 'x' + document.documentElement.clientHeight
            + ' at dpr ' + dpr)
        return { sections: view.book.sections.length }
    },

    /**
     * Lays every section out in turn and counts its pages.
     *
     * This is the only way to a real page count: the paginator knows how many pages a section
     * holds only once it has columnised it. `pages` counts two blank pages either side of the
     * text, which is why they come off here.
     */
    async index() {
        const renderer = view.renderer
        const count = view.book.sections.length
        const pages = []
        for (let i = 0; i < count; i++) {
            try {
                await renderer.goTo({ index: i, anchor: 0 })
                await painted()
                pages.push(Math.max(1, (renderer.pages || 3) - 2))
            } catch (e) {
                // A section that will not lay out still has to occupy a page, or every page after
                // it would be numbered wrongly.
                log('section ' + i + ' did not lay out: ' + e)
                pages.push(1)
            }
        }
        return { pages }
    },

    /**
     * Puts the book on one page of one section, and says how much text that page holds.
     *
     * The count is what lets Librera tell a page that was drawn before it was ready from a page
     * that is simply blank - the end of a chapter - and redraw only the first.
     */
    async goTo({ index, fraction }) {
        await view.renderer.goTo({ index, anchor: fraction })
        await painted()
        return { chars: visible ? String(visible).length : 0 }
    },

    /** Waits for a re-columnisation to finish, after the page size changed. */
    async settle() {
        await painted()
        await painted()
        return {}
    },

    async words() {
        return collectWords()
    },

    async text() {
        return collectText()
    },

    async links() {
        return collectLinks()
    },

    /**
     * The table of contents, each entry resolved to the section and fraction it points at so
     * Librera can turn it into a page number through the index it already has.
     */
    async toc() {
        const toc = []
        const walk = async (items, level) => {
            if (!items || level > 16) return
            for (const item of items) {
                if (item.label) {
                    const entry = { title: String(item.label).trim(), level, href: item.href || '' }
                    try {
                        const target = item.href ? view.resolveNavigation(item.href) : null
                        if (target && typeof target.index === 'number') {
                            entry.index = target.index
                            entry.fraction = typeof target.anchor === 'number' ? target.anchor : 0
                        }
                    } catch (e) {
                        // an entry whose target is broken still belongs in the contents
                    }
                    toc.push(entry)
                }
                await walk(item.subitems, level + 1)
            }
        }
        await walk(view.book.toc, 0)
        return { toc }
    },
}

// Librera waits for this before it asks for anything
window.librera.ready()
