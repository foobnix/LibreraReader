#include "mupdf/fitz.h"
#include "html-imp.h"

#include <string.h>
#include <math.h>

enum { T, R, B, L };

typedef struct epub_document_s epub_document;
typedef struct epub_chapter_s epub_chapter;
typedef struct epub_page_s epub_page;

struct epub_document_s
{
	fz_document super;
	fz_archive *zip;
	fz_html_font_set *set;
	int count;
	epub_chapter *spine;
	fz_outline *outline;
	char *dc_title, *dc_creator;
};

struct epub_chapter_s
{
	char *path;
	int start;
	fz_html *html;
	epub_chapter *next;
};

struct epub_page_s
{
	fz_page super;
	epub_document *doc;
	int number;
};

static int count_chapter_pages(epub_chapter *ch)
{
	if (ch->html->root->b > 0)
		return ceilf(ch->html->root->b / ch->html->page_h);
	return 1;
}

static int
epub_resolve_link(fz_context *ctx, fz_document *doc_, const char *dest, float *xp, float *yp)
{
	epub_document *doc = (epub_document*)doc_;
	epub_chapter *ch;

	const char *s = strchr(dest, '#');
	size_t n = s ? s - dest : strlen(dest);
	if (s && s[1] == 0)
		s = NULL;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		if (!strncmp(ch->path, dest, n) && ch->path[n] == 0)
		{
			if (s)
			{
				/* Search for a matching fragment */
				float y = fz_find_html_target(ctx, ch->html, s+1);
				if (y >= 0)
				{
					int page = y / ch->html->page_h;
					if (yp) *yp = y - page * ch->html->page_h;
					return ch->start + page;
				}
				return -1;
			}
			return ch->start;
		}
	}

	return -1;
}

static void
epub_update_outline(fz_context *ctx, fz_document *doc, fz_outline *node)
{
	while (node)
	{
		node->page = -1;//epub_resolve_link(ctx, doc, node->uri, &node->x, &node->y);
		epub_update_outline(ctx, doc, node->down);
		node = node->next;
	}
}

static void
epub_layout(fz_context *ctx, fz_document *doc_, float w, float h, float em)
{
	epub_document *doc = (epub_document*)doc_;
	epub_chapter *ch;
	int count = 0;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		ch->start = count;
		fz_layout_html(ctx, ch->html, w, h, em);
		count += count_chapter_pages(ch);
	}

	epub_update_outline(ctx, doc_, doc->outline);
}

static int
epub_count_pages(fz_context *ctx, fz_document *doc_)
{
	epub_document *doc = (epub_document*)doc_;
	epub_chapter *ch;
	int count = 0;
	for (ch = doc->spine; ch; ch = ch->next)
		count += count_chapter_pages(ch);
	return count;
}

static void
epub_drop_page(fz_context *ctx, fz_page *page_)
{
}

static fz_rect
epub_bound_page(fz_context *ctx, fz_page *page_)
{
	epub_page *page = (epub_page*)page_;
	epub_document *doc = page->doc;
	epub_chapter *ch;
	int n = page->number;
	int count = 0;
	fz_rect bbox;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		int cn = count_chapter_pages(ch);
		if (n < count + cn)
		{
			bbox.x0 = 0;
			bbox.y0 = 0;
			bbox.x1 = ch->html->page_w + ch->html->page_margin[L] + ch->html->page_margin[R];
			bbox.y1 = ch->html->page_h + ch->html->page_margin[T] + ch->html->page_margin[B];
			return bbox;
		}
		count += cn;
	}

	return fz_unit_rect;
}

static void
epub_run_page(fz_context *ctx, fz_page *page_, fz_device *dev, fz_matrix ctm, fz_cookie *cookie)
{
	epub_page *page = (epub_page*)page_;
	epub_document *doc = page->doc;
	epub_chapter *ch;
	int n = page->number;
	int count = 0;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		int cn = count_chapter_pages(ch);
		if (n < count + cn)
		{
			fz_draw_html(ctx, dev, ctm, ch->html, n-count);
			break;
		}
		count += cn;
	}
}

static fz_link *
epub_load_links(fz_context *ctx, fz_page *page_)
{
	epub_page *page = (epub_page*)page_;
	epub_document *doc = page->doc;
	epub_chapter *ch;
	int n = page->number;
	int count = 0;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		int cn = count_chapter_pages(ch);
		if (n < count + cn)
			return fz_load_html_links(ctx, ch->html, n - count, ch->path, doc);
		count += cn;
	}

	return NULL;
}

static fz_bookmark
epub_make_bookmark(fz_context *ctx, fz_document *doc_, int n)
{
	epub_document *doc = (epub_document*)doc_;
	epub_chapter *ch;
	int count = 0;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		int cn = count_chapter_pages(ch);
		if (n < count + cn)
			return fz_make_html_bookmark(ctx, ch->html, n - count);
		count += cn;
	}

	return 0;
}

static int
epub_lookup_bookmark(fz_context *ctx, fz_document *doc_, fz_bookmark mark)
{
	epub_document *doc = (epub_document*)doc_;
	epub_chapter *ch;

	for (ch = doc->spine; ch; ch = ch->next)
	{
		int p = fz_lookup_html_bookmark(ctx, ch->html, mark);
		if (p != -1)
			return ch->start + p;
	}
	return -1;
}

static fz_page *
epub_load_page(fz_context *ctx, fz_document *doc_, int number)
{
	epub_document *doc = (epub_document*)doc_;
	epub_page *page = fz_new_derived_page(ctx, epub_page);
	page->super.bound_page = epub_bound_page;
	page->super.run_page_contents = epub_run_page;
	page->super.load_links = epub_load_links;
	page->super.drop_page = epub_drop_page;
	page->doc = doc;
	page->number = number;
	return (fz_page*)page;
}

static void
epub_drop_document(fz_context *ctx, fz_document *doc_)
{
	epub_document *doc = (epub_document*)doc_;
	epub_chapter *ch, *next;
	ch = doc->spine;
	while (ch)
	{
		next = ch->next;
		fz_drop_html(ctx, ch->html);
		fz_free(ctx, ch->path);
		fz_free(ctx, ch);
		ch = next;
	}
	fz_drop_archive(ctx, doc->zip);
	fz_drop_html_font_set(ctx, doc->set);
	fz_drop_outline(ctx, doc->outline);
	fz_free(ctx, doc->dc_title);
	fz_free(ctx, doc->dc_creator);
}

static const char *
rel_path_from_idref(fz_xml *manifest, const char *idref)
{
	fz_xml *item;
	if (!idref)
		return NULL;
	item = fz_xml_find_down(manifest, "item");
	while (item)
	{
		const char *id = fz_xml_att(item, "id");
		if (id && !strcmp(id, idref))
			return fz_xml_att(item, "href");

		const char *id2 = fz_xml_att(item, "properties");
				if (id2 && !strcmp(id2, idref))
					return fz_xml_att(item, "href");


		item = fz_xml_find_next(item, "item");
	}
	return NULL;
}

static const char *
path_from_idref(char *path, fz_xml *manifest, const char *base_uri, const char *idref, int n)
{
	const char *rel_path = rel_path_from_idref(manifest, idref);
	if (!rel_path)
	{
		path[0] = 0;
		return NULL;
	}
	fz_strlcpy(path, base_uri, n);
	fz_strlcat(path, "/", n);
	fz_strlcat(path, rel_path, n);
	return fz_cleanname(fz_urldecode(path));
}

static epub_chapter *
epub_parse_chapter(fz_context *ctx, epub_document *doc, const char *path)
{
	fz_archive *zip = doc->zip;
	fz_buffer *buf = NULL;
	epub_chapter *ch;
	char base_uri[2048];

	fz_dirname(base_uri, path, sizeof base_uri);

	ch = fz_malloc_struct(ctx, epub_chapter);
	ch->path = NULL;
	ch->html = NULL;
	ch->next = NULL;

	fz_var(buf);

	fz_try(ctx)
	{
		buf = fz_read_archive_entry(ctx, zip, path);
		ch->path = fz_strdup(ctx, path);
		ch->html = fz_parse_html(ctx, doc->set, zip, base_uri, buf, fz_user_css(ctx));
	}
	fz_always(ctx)
		fz_drop_buffer(ctx, buf);
	fz_catch(ctx)
	{
		fz_drop_html(ctx, ch->html);
		fz_free(ctx, ch->path);
		fz_free(ctx, ch);
		fz_rethrow(ctx);
	}

	return ch;
}

static fz_outline *
epub_parse_ncx_imp(fz_context *ctx, epub_document *doc, fz_xml *node, char *base_uri)
{
	char path[2048];
	fz_outline *outline, *head, **tailp;

	head = NULL;
	tailp = &head;

	node = fz_xml_find_down(node, "navPoint");
	while (node)
	{
		char *text = fz_xml_text(fz_xml_down(fz_xml_find_down(fz_xml_find_down(node, "navLabel"), "text")));
		char *content = fz_xml_att(fz_xml_find_down(node, "content"), "src");
		if (text && content)
		{
			fz_strlcpy(path, base_uri, sizeof path);
			fz_strlcat(path, "/", sizeof path);
			fz_strlcat(path, content, sizeof path);
			fz_urldecode(path);
			fz_cleanname(path);

			fz_try(ctx)
			{
				*tailp = outline = fz_new_outline(ctx);
				tailp = &(*tailp)->next;
				outline->title = fz_strdup(ctx, text);
				outline->uri = fz_strdup(ctx, path);
				outline->page = -1;
				outline->down = epub_parse_ncx_imp(ctx, doc, node, base_uri);
				outline->is_open = 1;
			}
			fz_catch(ctx)
			{
				fz_drop_outline(ctx, head);
				fz_rethrow(ctx);
			}
		}
		node = fz_xml_find_next(node, "navPoint");
	}

	return head;
}

static void
epub_parse_ncx(fz_context *ctx, epub_document *doc, const char *path)
{
	fz_archive *zip = doc->zip;
	fz_buffer *buf = NULL;
	fz_xml_doc *ncx = NULL;
	char base_uri[2048];

	fz_var(buf);
	fz_var(ncx);

	fz_try(ctx)
	{
		fz_dirname(base_uri, path, sizeof base_uri);
		buf = fz_read_archive_entry(ctx, zip, path);
		ncx = fz_parse_xml(ctx, buf, 0);
		doc->outline = epub_parse_ncx_imp(ctx, doc, fz_xml_find_down(fz_xml_root(ncx), "navMap"), base_uri);
	}
	fz_always(ctx)
	{
		fz_drop_buffer(ctx, buf);
		fz_drop_xml(ctx, ncx);
	}
	fz_catch(ctx)
		fz_rethrow(ctx);
}

static fz_outline *
epub_parse_nav_imp(fz_context *ctx, epub_document *doc, fz_xml *node, char *base_uri)
{
	char path[2048];
	fz_outline *outline, *head, **tailp;

	head = NULL;
	tailp = &head;


	node =  fz_xml_find_down(fz_xml_find_down(node, "ol"),"li");


	while (node)
	{
		fz_xml *tag = fz_xml_find_down(node, "a");
		char *text = fz_xml_text(fz_xml_down(tag));
		if(!text){
			text = fz_xml_text(fz_xml_down(fz_xml_down(tag)));
		}
		char *content = fz_xml_att(tag, "href");
		if (text && content)
		{
			fz_strlcpy(path, base_uri, sizeof path);
			fz_strlcat(path, "/", sizeof path);
			fz_strlcat(path, content, sizeof path);
			fz_urldecode(path);
			fz_cleanname(path);

            fz_try(ctx)
                        {


			*tailp = outline = fz_new_outline(ctx);
			tailp = &(*tailp)->next;
			outline->title = fz_strdup(ctx, text);
			outline->uri = fz_strdup(ctx, path);
			outline->page = -1;
			outline->down = epub_parse_nav_imp(ctx, doc, node, base_uri);
			}
            fz_catch(ctx)
            {
                fz_drop_outline(ctx, head);
                fz_rethrow(ctx);
            }
		}
		node = fz_xml_find_next(node, "li");
	}

	return head;
}

static void
epub_parse_nav(fz_context *ctx, epub_document *doc, const char *path)
{
	fz_archive *zip = doc->zip;
	fz_buffer *buf = NULL;
	fz_xml_doc *ncx = NULL;
	char base_uri[2048];

    fz_var(buf);
	fz_var(ncx);

    fz_try(ctx)
	{

        fz_dirname(base_uri, path, sizeof base_uri);

        buf = fz_read_archive_entry(ctx, zip, path);
        ncx = fz_parse_xml(ctx, buf, 0);

        fz_xml *body = fz_xml_find_down(fz_xml_root(ncx), "body");
        fz_xml *section = fz_xml_find_down(body, "section");
        if(section){
            body = section;
        }
        fz_xml *nav = fz_xml_find_down(body, "nav");

        while (nav){
            char *id = fz_xml_att(nav, "epub:type");
            if(!strcmp(id, "toc")){
                doc->outline = epub_parse_nav_imp(ctx, doc, nav, base_uri);
                break;
            }
            nav = fz_xml_find_next(body,"nav");
        }
	}
	fz_always(ctx)
    {
        fz_drop_buffer(ctx, buf);
        fz_drop_xml(ctx, ncx);
    }
    fz_catch(ctx)
        fz_rethrow(ctx);
}


static char *
find_metadata(fz_context *ctx, fz_xml *metadata, char *key)
{
	char *text = fz_xml_text(fz_xml_down(fz_xml_find_down(metadata, key)));
	if (text)
		return fz_strdup(ctx, text);
	return NULL;
}

static void
epub_parse_header(fz_context *ctx, epub_document *doc)
{
	fz_archive *zip = doc->zip;
	fz_buffer *buf = NULL;
	fz_xml_doc *container_xml = NULL;
	fz_xml_doc *content_opf = NULL;
	fz_xml *container, *rootfiles, *rootfile;
	fz_xml *package, *manifest, *spine, *itemref, *metadata;
	char base_uri[2048];
	const char *full_path;
	const char *version;
	char ncx[2048], s[2048];
	epub_chapter **tailp;

//	if (fz_has_archive_entry(ctx, zip, "META-INF/rights.xml"))
//		fz_throw(ctx, FZ_ERROR_GENERIC, "EPUB is locked by DRM");
//	if (fz_has_archive_entry(ctx, zip, "META-INF/encryption.xml"))
//		fz_throw(ctx, FZ_ERROR_GENERIC, "EPUB is locked by DRM");

	fz_var(buf);
	fz_var(container_xml);
	fz_var(content_opf);

	fz_try(ctx)
	{
		/* parse META-INF/container.xml to find OPF */

		buf = fz_read_archive_entry(ctx, zip, "META-INF/container.xml");
		container_xml = fz_parse_xml(ctx, buf, 0);
		fz_drop_buffer(ctx, buf);
		buf = NULL;

		container = fz_xml_find(fz_xml_root(container_xml), "container");
		rootfiles = fz_xml_find_down(container, "rootfiles");
		rootfile = fz_xml_find_down(rootfiles, "rootfile");
		full_path = fz_xml_att(rootfile, "full-path");
		if (!full_path)
			fz_throw(ctx, FZ_ERROR_GENERIC, "cannot find root file in EPUB");

		fz_dirname(base_uri, full_path, sizeof base_uri);

		/* parse OPF to find NCX and spine */

		buf = fz_read_archive_entry(ctx, zip, full_path);
		content_opf = fz_parse_xml(ctx, buf, 0);
		fz_drop_buffer(ctx, buf);
		buf = NULL;

		package = fz_xml_find(fz_xml_root(content_opf), "package");
		version = fz_xml_att(package, "version");
		if (!version || strcmp(version, "2.0"))
			fz_warn(ctx, "unknown epub version: %s", version ? version : "<none>");

		metadata = fz_xml_find_down(package, "metadata");
		if (metadata)
		{
			doc->dc_title = find_metadata(ctx, metadata, "title");
			doc->dc_creator = find_metadata(ctx, metadata, "creator");
		}

		manifest = fz_xml_find_down(package, "manifest");
		spine = fz_xml_find_down(package, "spine");

		if (path_from_idref(ncx, manifest, base_uri, fz_xml_att(spine, "toc"), sizeof ncx))
        	{
        		epub_parse_ncx(ctx, doc, ncx);
        	}else if (path_from_idref(ncx, manifest, base_uri, "nav", sizeof ncx))
        	{
        		epub_parse_nav(ctx, doc, ncx);
        	}

		doc->spine = NULL;
		tailp = &doc->spine;
		itemref = fz_xml_find_down(spine, "itemref");
		while (itemref)
		{
			if (path_from_idref(s, manifest, base_uri, fz_xml_att(itemref, "idref"), sizeof s))
			{
				fz_try(ctx)
				{
					*tailp = epub_parse_chapter(ctx, doc, s);
					tailp = &(*tailp)->next;
				}
				fz_catch(ctx)
				{
					fz_rethrow_if(ctx, FZ_ERROR_TRYLATER);
					fz_warn(ctx, "ignoring chapter %s", s);
				}
			}
			itemref = fz_xml_find_next(itemref, "itemref");
		}
	}
	fz_always(ctx)
	{
		fz_drop_xml(ctx, content_opf);
		fz_drop_xml(ctx, container_xml);
		fz_drop_buffer(ctx, buf);
	}
	fz_catch(ctx)
		fz_rethrow(ctx);
}

static fz_outline *
epub_load_outline(fz_context *ctx, fz_document *doc_)
{
	epub_document *doc = (epub_document*)doc_;
	return fz_keep_outline(ctx, doc->outline);
}

static int
epub_lookup_metadata(fz_context *ctx, fz_document *doc_, const char *key, char *buf, int size)
{
	epub_document *doc = (epub_document*)doc_;
	if (!strcmp(key, FZ_META_FORMAT))
		return (int)fz_strlcpy(buf, "EPUB", size);
	if (!strcmp(key, FZ_META_INFO_TITLE) && doc->dc_title)
		return (int)fz_strlcpy(buf, doc->dc_title, size);
	if (!strcmp(key, FZ_META_INFO_AUTHOR) && doc->dc_creator)
		return (int)fz_strlcpy(buf, doc->dc_creator, size);
	return -1;
}

static fz_document *
epub_init(fz_context *ctx, fz_archive *zip)
{
	epub_document *doc;

	doc = fz_new_derived_document(ctx, epub_document);
	doc->zip = zip;
	doc->set = fz_new_html_font_set(ctx);

	doc->super.drop_document = epub_drop_document;
	doc->super.layout = epub_layout;
	doc->super.load_outline = epub_load_outline;
	doc->super.resolve_link = epub_resolve_link;
	doc->super.make_bookmark = epub_make_bookmark;
	doc->super.lookup_bookmark = epub_lookup_bookmark;
	doc->super.count_pages = epub_count_pages;
	doc->super.load_page = epub_load_page;
	doc->super.lookup_metadata = epub_lookup_metadata;
	doc->super.is_reflowable = 1;

	fz_try(ctx)
	{
		epub_parse_header(ctx, doc);
	}
	fz_catch(ctx)
	{
		fz_drop_document(ctx, &doc->super);
		fz_rethrow(ctx);
	}

	return (fz_document*)doc;
}

static fz_document *
epub_open_document_with_stream(fz_context *ctx, fz_stream *file)
{
	return epub_init(ctx, fz_open_zip_archive_with_stream(ctx, file));
}

static fz_document *
epub_open_document(fz_context *ctx, const char *filename)
{
	if (strstr(filename, "META-INF/container.xml") || strstr(filename, "META-INF\\container.xml"))
	{
		char dirname[2048], *p;
		fz_strlcpy(dirname, filename, sizeof dirname);
		p = strstr(dirname, "META-INF");
		*p = 0;
		if (!dirname[0])
			fz_strlcpy(dirname, ".", sizeof dirname);
		return epub_init(ctx, fz_open_directory(ctx, dirname));
	}

	return epub_init(ctx, fz_open_zip_archive(ctx, filename));
}

static int
epub_recognize(fz_context *doc, const char *magic)
{
	if (strstr(magic, "META-INF/container.xml") || strstr(magic, "META-INF\\container.xml"))
		return 200;
	return 0;
}

static const char *epub_extensions[] =
{
	"epub",
	NULL
};

static const char *epub_mimetypes[] =
{
	"application/epub+zip",
	NULL
};

fz_document_handler epub_document_handler =
{
	epub_recognize,
	epub_open_document,
	epub_open_document_with_stream,
	epub_extensions,
	epub_mimetypes
};
