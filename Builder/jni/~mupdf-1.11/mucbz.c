#include "mupdf/fitz.h"

#define DPI 72.0f

typedef struct cbz_document_s cbz_document;
typedef struct cbz_page_s cbz_page;

static const char *cbz_ext_list[] = {
	".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tif", ".tiff", ".jpx", ".jp2", ".j2k", ".wdp", ".hdp", ".jxr", ".pbm", ".pgm", ".ppm", ".pam", ".pnm",
	NULL
};

struct cbz_page_s
{
	fz_page super;
	fz_image *image;
};

struct cbz_document_s
{
	fz_document super;
	fz_archive *arch;
	int page_count;
	const char **page;
};

static inline int cbz_isdigit(int c)
{
	return c >= '0' && c <= '9';
}

static inline int cbz_toupper(int c)
{
	if (c >= 'a' && c <= 'z')
		return c - 'a' + 'A';
	return c;
}

static inline int
cbz_strnatcmp(const char *a, const char *b)
{
	int x, y;

	while (*a || *b)
	{
		if (cbz_isdigit(*a) && cbz_isdigit(*b))
		{
			x = *a++ - '0';
			while (cbz_isdigit(*a))
				x = x * 10 + *a++ - '0';
			y = *b++ - '0';
			while (cbz_isdigit(*b))
				y = y * 10 + *b++ - '0';
		}
		else
		{
			x = cbz_toupper(*a++);
			y = cbz_toupper(*b++);
		}
		if (x < y)
			return -1;
		if (x > y)
			return 1;
	}

	return 0;
}

static int
cbz_compare_page_names(const void *a, const void *b)
{
	return cbz_strnatcmp(*(const char **)a, *(const char **)b);
}

static void
cbz_create_page_list_dir(fz_context *ctx, cbz_document *doc)
{
	fz_archive *arch = doc->arch;
	int i, k, count;

	fz_buffer *buf;
	fz_xml *container_xml;

	buf = fz_read_archive_entry(ctx, arch, "book.ldir");
	container_xml = fz_parse_xml(ctx, buf, 0);
	fz_drop_buffer(ctx, buf);

	fz_xml *container;
	container = fz_xml_find(container_xml, "container");

	const char *version;
	version = fz_xml_att(container, "count");

	count = fz_atoi(version);

	fz_xml *itemref;
	itemref = fz_xml_find_down(container_xml, "item");

	
	doc->page_count = 0;
	doc->page = fz_malloc_array(ctx, count, sizeof *doc->page);
	
	while (itemref)
	{
		const char *path;
		path = fz_xml_att(itemref, "path");
	
		doc->page[doc->page_count++] = path;
		
		itemref = fz_xml_find_next(itemref, "item");
		
	}
	
}

static void
cbz_create_page_list(fz_context *ctx, cbz_document *doc)
{
	fz_archive *arch = doc->arch;
	int i, k, count;

	count = fz_count_archive_entries(ctx, arch);

	doc->page_count = 0;
	doc->page = fz_malloc_array(ctx, count, sizeof *doc->page);

	for (i = 0; i < count; i++)
	{
		for (k = 0; cbz_ext_list[k]; k++)
		{
			const char *name = fz_list_archive_entry(ctx, arch, i);
			const char *ext = name ? strrchr(name, '.') : NULL;
			if (ext && !fz_strcasecmp(ext, cbz_ext_list[k]))
			{
				doc->page[doc->page_count++] = name;
				break;
			}
		}
	}

	qsort((char **)doc->page, doc->page_count, sizeof *doc->page, cbz_compare_page_names);
}

static void
cbz_drop_document(fz_context *ctx, cbz_document *doc)
{
	fz_drop_archive(ctx, doc->arch);
	fz_free(ctx, (char **)doc->page);
}

static int
cbz_count_pages(fz_context *ctx, cbz_document *doc)
{
	return doc->page_count;
}

static fz_rect *
cbz_bound_page(fz_context *ctx, cbz_page *page, fz_rect *bbox)
{
	fz_image *image = page->image;
	int xres, yres;

	fz_image_resolution(image, &xres, &yres);
	bbox->x0 = bbox->y0 = 0;
	bbox->x1 = image->w * DPI / xres;
	bbox->y1 = image->h * DPI / yres;
	return bbox;
}

static void
cbz_run_page(fz_context *ctx, cbz_page *page, fz_device *dev, const fz_matrix *ctm, fz_cookie *cookie)
{
	fz_matrix local_ctm = *ctm;
	fz_image *image = page->image;
	int xres, yres;
	float w, h;

	fz_image_resolution(image, &xres, &yres);
	w = image->w * DPI / xres;
	h = image->h * DPI / yres;
	fz_pre_scale(&local_ctm, w, h);
	fz_fill_image(ctx, dev, image, &local_ctm, 1);
}

static void
cbz_drop_page(fz_context *ctx, cbz_page *page)
{
	fz_drop_image(ctx, page->image);
}

static cbz_page *
cbz_load_page(fz_context *ctx, cbz_document *doc, int number)
{
	cbz_page *page = NULL;
	fz_buffer *buf = NULL;

	if (number < 0 || number >= doc->page_count)
		return NULL;

	fz_var(page);

	if (doc->arch)
		//buf = fz_read_archive_entry(ctx, doc->arch, doc->page[number]);
		if (fz_has_archive_entry(ctx, doc->arch, doc->page[number]))
			buf = fz_read_archive_entry(ctx, doc->arch, doc->page[number]);
		else
			buf = fz_read_file(ctx, doc->page[number]);

	if (!buf)
		fz_throw(ctx, FZ_ERROR_GENERIC, "cannot load cbz page");

	fz_try(ctx)
	{
		page = fz_new_derived_page(ctx, cbz_page);
		page->super.bound_page = (fz_page_bound_page_fn *)cbz_bound_page;
		page->super.run_page_contents = (fz_page_run_page_contents_fn *)cbz_run_page;
		page->super.drop_page = (fz_page_drop_page_fn *)cbz_drop_page;
		page->image = fz_new_image_from_buffer(ctx, buf);
	}
	fz_always(ctx)
	{
		fz_drop_buffer(ctx, buf);
	}
	fz_catch(ctx)
	{
		fz_drop_page(ctx, &page->super);
		fz_rethrow(ctx);
	}

	return page;
}

static int
cbz_lookup_metadata(fz_context *ctx, cbz_document *doc, const char *key, char *buf, int size)
{
	if (!strcmp(key, "format"))
		return (int) fz_strlcpy(buf, fz_archive_format(ctx, doc->arch), size);
	return -1;
}

static fz_document *
cbz_init(fz_context *ctx, fz_archive *zip, int type)
{
	cbz_document *doc;

	doc = fz_new_derived_document(ctx, cbz_document);

	doc->super.drop_document = (fz_document_drop_fn *)cbz_drop_document;
	doc->super.count_pages = (fz_document_count_pages_fn *)cbz_count_pages;
	doc->super.load_page = (fz_document_load_page_fn *)cbz_load_page;
	

	fz_try(ctx)
	{

		doc->arch = zip;
		if(type==1){
			cbz_create_page_list_dir(ctx, doc);
		}else{
			cbz_create_page_list(ctx, doc);
		}
	}
	fz_catch(ctx)
	{
		fz_drop_document(ctx, &doc->super);
		fz_rethrow(ctx);
	}
	return &doc->super;
}


static fz_document *
cbz_open_document(fz_context *ctx, const char *filename)
{
	
	if (strstr(filename, "book.ldir"))
	{
		printf("cbz_open_document 1");	
		char dirname[2048], *p;
		fz_strlcpy(dirname, filename, sizeof dirname);
		p = strstr(dirname, "book.ldir");
		*p = 0;
		printf("cbz_open_document 2");
		if (!dirname[0])
			fz_strlcpy(dirname, ".", sizeof dirname);
		printf("cbz_init dirname %s",dirname);	
		return cbz_init(ctx, fz_open_directory(ctx, dirname),1);
	}

	return cbz_init(ctx, fz_open_zip_archive(ctx, filename),2);
}

static int
cbz_recognize(fz_context *ctx, const char *magic)
{
	char *ext = strrchr(magic, '.');
	if ((ext && !fz_strcasecmp(ext, ".cbz")) || !strcmp(magic, "cbz") ||
			!strcmp(magic, "application/x-cbz"))
		return 100;
	if ((ext && !fz_strcasecmp(ext, ".zip")) || !strcmp(magic, "zip") ||
			!strcmp(magic, "application/zip"))
		return 100;
	if (strstr(magic, "book.ldir"))
		return 200;
	if ((ext && !fz_strcasecmp(ext, ".tar")) || !strcmp(magic, "tar") ||
			!strcmp(magic, "application/x-tar"))
		return 100;
	if ((ext && !fz_strcasecmp(ext, ".cbt")) || !strcmp(magic, "cbt") ||
			!strcmp(magic, "application/x-cbt"))
		return 100;
	return 0;
}

fz_document_handler cbz_document_handler =
{
	cbz_recognize,
	cbz_open_document,
	NULL
};
