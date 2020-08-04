#include <jni.h>

#include <android/log.h>

#include <android/bitmap.h>
#include "mupdf/fitz.h"
#include "mupdf/pdf.h"
#include "ctype.h"


#include <stdio.h>
#include <stdlib.h>

#include "../ebookdroid.h"
#include "../javahelpers.h"

/* Debugging helper */

#define DEBUG(args...) \
    __android_log_print(ANDROID_LOG_DEBUG, "MuPDF", args)

#define ERROR(args...) \
    __android_log_print(ANDROID_LOG_ERROR, "MuPDF", args)

#define INFO(args...) \
    __android_log_print(ANDROID_LOG_INFO, "MuPDF", args)

#define PACKAGENAME "org/ebookdroid/droids/mupdf/codec"


#define MAX_SEARCH_HITS (500)
#define NUM_CACHE (3)
#define STRIKE_HEIGHT (0.375f)
#define UNDERLINE_HEIGHT (0.075f)
#define LINE_THICKNESS (0.07f)
#define INK_THICKNESS (4.0f)
#define SMALL_FLOAT (0.00001)

typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s {
	fz_context *ctx;
	fz_document *document;
	fz_outline *outline;
	unsigned char format; // save current document format.
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s {
	fz_context *ctx;
	fz_page *page;
	int number;
	fz_display_list* pageList;
	//fz_display_list* annot_list;
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"
#define PASSWORD_REQUIRED_EXCEPTION "org/ebookdroid/droids/mupdf/codec/exceptions/MuPdfPasswordRequiredException"
#define WRONG_PASSWORD_EXCEPTION "org/ebookdroid/droids/mupdf/codec/exceptions/MuPdfWrongPasswordEnteredException"



extern fz_locks_context * jni_new_locks();
extern void jni_free_locks(fz_locks_context *locks);

void mupdf_throw_exception_ex(JNIEnv *env, const char* exception, char *message) {
	jthrowable new_exception = (*env)->FindClass(env, exception);
	if (new_exception == NULL) {
		DEBUG("Exception class not found: '%s'", exception);
		return;
	}
	DEBUG("Exception '%s', Message: '%s'", exception, message);
	(*env)->ThrowNew(env, new_exception, message);
}

void mupdf_throw_exception(JNIEnv *env, char *message) {
	mupdf_throw_exception_ex(env, RUNTIME_EXCEPTION, message);
}

static void mupdf_free_document(renderdocument_t* doc) {
	////LOGE("mupdf_free_document 1");
	if (!doc) {
		return;
	}
	////LOGE("mupdf_free_document 2");
	//fz_locks_context *locks = doc->ctx->locks;

	if (doc->outline) {
		fz_drop_outline(doc->ctx, doc->outline);
	}

	////LOGE("mupdf_free_document 3");

	doc->outline = NULL;

	if (doc->document) {
		fz_drop_document(doc->ctx, doc->document);
	}
//	//LOGE("mupdf_free_document 4");

	doc->document = NULL;

	//fz_flush_warnings(doc->ctx);

//	//LOGE("mupdf_free_document 5");

	fz_drop_context(doc->ctx);
	doc->ctx = NULL;
//	//LOGE("mupdf_free_document 6");
	//jni_free_locks(locks);

	free(doc);
	doc = NULL;

//	//LOGE("mupdf_free_document 7");
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getMupdfVersion(JNIEnv *env,
		jclass clazz) {
	return 111;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_open(JNIEnv *env,
		jclass clazz, jint storememory, jint format, jstring fname, jstring pwd, jstring jcss, jint isDocCSS) {
	renderdocument_t *doc;
	jboolean iscopy;
	jclass cls;
	jfieldID fid;
	char *filename;
	char *password;
	char *css;

	filename = (char*) (*env)->GetStringUTFChars(env, fname, &iscopy);
	password = (char*) (*env)->GetStringUTFChars(env, pwd, &iscopy);
	css = (char*) (*env)->GetStringUTFChars(env, jcss, NULL);

	doc = malloc(sizeof(renderdocument_t));
	if (!doc) {
		mupdf_throw_exception(env, "Out of Memory");
		goto cleanup;
	}
	DEBUG("MuPdfDocument.nativeOpen(): storememory = %d", storememory);


	doc->ctx = fz_new_context(NULL, NULL, storememory);

	if (!doc->ctx) {
		free(doc);
		mupdf_throw_exception(env, "Out of Memory");
		goto cleanup;
	}

	fz_register_document_handlers(doc->ctx);

	fz_set_user_css(doc->ctx, css);
	fz_set_use_document_css(doc->ctx, isDocCSS);

	doc->document = NULL;
	doc->outline = NULL;

//    fz_set_aa_level(fz_catch(ctx), alphabits);
	doc->format = format;
	fz_try(doc->ctx)
	{
		printf("Open start %s \n", filename);
		__android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Open");

		doc->document = (fz_document*) fz_open_document(doc->ctx, filename);

		//fz_drop_context(doc->ctx);
		//fz_set_user_css(doc->ctx,css);


		__android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Open succes");
		printf("Open  end %s \n", filename);

		//char info[64];
		//fz_lookup_metadata(doc->ctx, doc->document, FZ_META_FORMAT, info, sizeof(info));
	}
	fz_always(doc->ctx)
	{
		printf("fz_always %s \n", filename);
	}

	fz_catch(doc->ctx)
	{
		//       mupdf_throw_exception(env, "Open Document  Exception");
		printf("%s \n", filename);
		mupdf_throw_exception(env, "PDF file not found or corrupted");
		mupdf_free_document(doc);
		goto cleanup;
	}

	/*
	 * Handle encrypted PDF files
	 */fz_try(doc->ctx)
	{
		if (fz_needs_password(doc->ctx,doc->document)) {
			if (strlen(password)) {
				int ok = fz_authenticate_password(doc->ctx, doc->document, password);
				if (!ok) {
					mupdf_free_document(doc);
					mupdf_throw_exception_ex(env,
					WRONG_PASSWORD_EXCEPTION, "Wrong password given");
					goto cleanup;
				}
			} else {
				mupdf_free_document(doc);
				mupdf_throw_exception_ex(env,
				PASSWORD_REQUIRED_EXCEPTION, "Document needs a password!");
				goto cleanup;
			}
		}
	}
	fz_catch(doc->ctx)
	{
		//       mupdf_throw_exception(env, "Open Document  Exception");
		printf("%s \n", filename);
		mupdf_throw_exception(env, "PDF file not found or corrupted");
		mupdf_free_document(doc);
	}

	cleanup:

	(*env)->ReleaseStringUTFChars(env, fname, filename);
	(*env)->ReleaseStringUTFChars(env, pwd, password);

	// DEBUG("MuPdfDocument.nativeOpen(): return handle = %p", doc);
	return (jlong) (long) doc;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_free(JNIEnv *env,
		jclass clazz, jlong handle) {
	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	mupdf_free_document(doc);
}


JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getMeta(JNIEnv *env,
		jclass cls, jlong handle, jstring joptions) {
	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	char info[2048];

	const char *options = (*env)->GetStringUTFChars(env, joptions, NULL);

	fz_lookup_metadata(doc->ctx, doc->document, options, info, sizeof(info));

	return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageInfo(JNIEnv *env,
		jclass cls, jlong handle, jint pageNumber, jobject cpi) {

	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	//TODO: Review this. Possible broken

	fz_page *page = NULL;
	fz_rect bounds;

	jclass clazz;
	jfieldID fid;

	fz_try(doc->ctx)
	{
		page = fz_load_page(doc->ctx, doc->document, pageNumber - 1);
		fz_bound_page(doc->ctx, page, &bounds);
	}
	fz_catch(doc->ctx)
	{
		return -1;
	}

	if (page) {
		clazz = (*env)->GetObjectClass(env, cpi);
		if (0 == clazz) {
			return (-1);
		}

		fid = (*env)->GetFieldID(env, clazz, "width", "I");
		(*env)->SetIntField(env, cpi, fid, bounds.x1 - bounds.x0);

		fid = (*env)->GetFieldID(env, clazz, "height", "I");
		(*env)->SetIntField(env, cpi, fid, bounds.y1 - bounds.y0);

		fid = (*env)->GetFieldID(env, clazz, "dpi", "I");
		(*env)->SetIntField(env, cpi, fid, 0);

		fid = (*env)->GetFieldID(env, clazz, "rotation", "I");
		(*env)->SetIntField(env, cpi, fid, 0);

		fid = (*env)->GetFieldID(env, clazz, "version", "I");
		(*env)->SetIntField(env, cpi, fid, 0);

		fz_drop_page(doc->ctx, page);
		return 0;
	}
	return -1;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getFirstPageLink(JNIEnv *env,
		jclass clazz, jlong handle, jlong pagehandle) {
	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;
	return (jlong) (long) (
			(page && doc) ? fz_load_links(doc->ctx, page->page) : NULL);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getNextPageLink(JNIEnv *env,
		jclass clazz, jlong linkhandle) {
	fz_link *link = (fz_link*) (long) linkhandle;
	return (jlong) (long) (link ? link->next : NULL);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkType(JNIEnv *env,
		jclass clazz, jlong handle, jlong linkhandle) {
		
	fz_link *link = (fz_link*) (long) linkhandle;
	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	return (jint) fz_is_external_link(doc->ctx, link->uri);
	//return 0;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkUrl(JNIEnv *env,
		jclass clazz, jlong linkhandle) {
	fz_link *link = (fz_link*) (long) linkhandle;

	//if (!link || link->dest.kind != FZ_LINK_URI) {
//		return NULL;
//	}

	//char linkbuf[2048];
	//snprintf(linkbuf, 1023, "%s", link->dest.ld.uri.uri);

	//return (*env)->NewStringUTF(env, linkbuf);
	char linkbuf[2048];
	snprintf(linkbuf, 2047, "%s", link->uri);

	return (*env)->NewStringUTF(env, linkbuf);
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkSourceRect(
		JNIEnv *env, jclass clazz, jlong linkhandle, jfloatArray boundsArray) {
	fz_link *link = (fz_link*) (long) linkhandle;

	//if (!link || link->dest.kind != FZ_LINK_GOTO)
	if (!link) {
		return JNI_FALSE;
	}

	jfloat *bounds = (*env)->GetPrimitiveArrayCritical(env, boundsArray, 0);
	if (!bounds) {
		return JNI_FALSE;
	}

	bounds[0] = link->rect.x0;
	bounds[1] = link->rect.y0;
	bounds[2] = link->rect.x1;
	bounds[3] = link->rect.y1;

	(*env)->ReleasePrimitiveArrayCritical(env, boundsArray, bounds, 0);

	return JNI_TRUE;
}
JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkTargetPage(
		JNIEnv *env, jclass clazz, jlong handle,  jlong linkhandle) {
	fz_link *link = (fz_link*) (long) linkhandle;
	renderdocument_t *doc = (renderdocument_t *)(long)handle;

	int pageNum =  fz_resolve_link(doc->ctx, link->doc, link->uri, NULL, NULL);
    return pageNum;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getLinkPage(
		JNIEnv *env, jclass clazz, jlong handle,  jstring id) {

	renderdocument_t *doc = (renderdocument_t *)(long)handle;
	const char *str = (*env)->GetStringUTFChars(env, id, NULL);

	int pageNum =  fz_resolve_link(doc->ctx, doc->document, str, NULL, NULL);
    return pageNum;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkTargetPoint(
		JNIEnv *env, jclass clazz, jlong linkhandle, jfloatArray pointArray) {
	fz_link *link = (fz_link*) (long) linkhandle;

	//if (!link || link->dest.kind != FZ_LINK_GOTO) {
	//	return 0;
	//}

	jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
	if (!point) {
		return 0;
	}
//
//	DEBUG("MuPdfLinks_fillPageLinkTargetPoint(): %d %x (%f, %f) - (%f, %f)",
//			link->dest.ld.gotor.page, link->dest.ld.gotor.flags,
//			link->dest.ld.gotor.lt.x, link->dest.ld.gotor.lt.y,
//			link->dest.ld.gotor.rb.x, link->dest.ld.gotor.rb.y);
//
	point[0] = link->rect.x1;
	point[1] = link->rect.y1;
//
	(*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);
//
//	return link->dest.ld.gotor.flags;
	jint res = 1;
	return res;
}
static int fontSize = 0;
JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageCount(JNIEnv *env,
		jclass clazz, jlong handle, jint width, jint height, jint size) {
	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	fz_try(doc->ctx)
	{
	    fontSize = size;
	    DEBUG("fontSize set %d", fontSize);

		fz_layout_document(doc->ctx, doc->document, width, height, size);
		return (fz_count_pages(doc->ctx, doc->document));
	}
	fz_catch(doc->ctx)
	{
		mupdf_free_document(doc);
		mupdf_throw_exception(env, "page count 0");
		return 0;
	}
	return 0;
}




JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_open(JNIEnv *env, jclass clazz,
		jlong dochandle, jint pageno) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	renderpage_t *page = NULL;
	fz_device *dev = NULL;

	DEBUG("MuPdfPage_open(%p, %d): start", doc, pageno);

	//fz_context* ctx = fz_clone_context(doc->ctx);
	fz_context* ctx = doc->ctx;
	if (!ctx || doc->ctx == NULL) {
		//mupdf_throw_exception(env, "Context cloning failed");
		return (jlong) (long) NULL;
	}

	page = fz_malloc_no_throw(ctx, sizeof(renderpage_t));
	DEBUG("MuPdfPage_open(%p, %d): page=%p", doc, pageno, page);

	if (!page) {
		//mupdf_throw_exception(env, "Out of Memory");
		return (jlong) (long) NULL;
	}

	page->ctx = ctx;
	page->page = NULL;
	page->pageList = NULL;
	//page->annot_list = NULL;

	fz_try(ctx)
	{
		page->pageList = fz_new_display_list(ctx, NULL);
		dev = fz_new_list_device(ctx, page->pageList);
		page->page = fz_load_page(ctx ,doc->document, pageno - 1);
		fz_run_page(ctx, page->page, dev, &fz_identity, NULL);
	}
	fz_always(ctx)
	{
		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);


		dev = NULL;
	}
	fz_catch(ctx)
	{
//		fz_free_device(dev);
//		//fz_free_display_list(ctx, page->pageList);
//		fz_free_page(doc->document, page->page);
//
//		fz_free(ctx, page);
//		fz_free_context(ctx);
//
//		page = NULL;
//		ctx = NULL;
		//mupdf_throw_exception(env, "error loading page");
	}

	DEBUG("MuPdfPage_open(%p, %d): finish: %p", doc, pageno, page);

	return (jlong) (long) page;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_free(JNIEnv *env, jclass clazz,
		jlong dochandle, jlong handle) {

	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	renderpage_t *page = (renderpage_t*) (long) handle;

	if (!doc || !doc->ctx || !page || doc->ctx == NULL) {
		DEBUG("No page to free");
		return;
	}

	fz_try(doc->ctx)
	{
		DEBUG("MuPdfPage_free(%p): start", page);
			if (page->pageList) {
				//fz_free_display_list(ctx, page->pageList);
				fz_drop_display_list(doc->ctx, page->pageList);
				page->pageList = NULL;
			}

			if (page->page) {
				fz_drop_page(doc->ctx, page->page);
				page->page = NULL;
			}

			//fz_free(doc->ctx, page);
			//fz_drop_context(ctx);

			page->ctx = NULL;
			page = NULL;

	}
	fz_catch(doc->ctx)
	{
		DEBUG("MuPdfPage_free(%p): error", page);
	}

	DEBUG("MuPdfPage_free(%p): finish", page);
}




JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getBounds(JNIEnv *env,
		jclass clazz, jlong dochandle, jlong handle, jfloatArray bounds) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	renderpage_t *page = (renderpage_t*) (long) handle;
	jfloat *bbox = (*env)->GetPrimitiveArrayCritical(env, bounds, 0);
	if (!bbox)
		return;
	fz_rect page_bounds;
	fz_bound_page(page->ctx, page->page, &page_bounds);
// DEBUG("Bounds: %f %f %f %f", page_bounds.x0, page_bounds.y0, page_bounds.x1, page_bounds.y1);
	bbox[0] = page_bounds.x0;
	bbox[1] = page_bounds.y0;
	bbox[2] = page_bounds.x1;
	bbox[3] = page_bounds.y1;
	(*env)->ReleasePrimitiveArrayCritical(env, bounds, bbox, 0);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_renderPage(JNIEnv *env,
		jobject this, jlong dochandle, jlong pagehandle, jintArray viewboxarray,
		jfloatArray matrixarray, jintArray bufferarray, jint r, jint g, jint b) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;
// DEBUG("PdfView(%p).renderPage(%p, %p)", this, doc, page);
	fz_matrix ctm;
	fz_rect viewbox;
	fz_pixmap *pixmap;
	jfloat *matrix;
	jint *viewboxarr;
	jint *dimen;
	jint *buffer;
	int length, val;
	fz_device *dev = NULL;

	/* initialize parameter arrays for MuPDF */

	matrix = (*env)->GetPrimitiveArrayCritical(env, matrixarray, 0);
	ctm = fz_identity;
	ctm.a = matrix[0];
	ctm.b = matrix[1];
	ctm.c = matrix[2];
	ctm.d = matrix[3];
	ctm.e = matrix[4];
	ctm.f = matrix[5];
	(*env)->ReleasePrimitiveArrayCritical(env, matrixarray, matrix, 0);

	viewboxarr = (*env)->GetPrimitiveArrayCritical(env, viewboxarray, 0);
	viewbox.x0 = viewboxarr[0];
	viewbox.y0 = viewboxarr[1];
	viewbox.x1 = viewboxarr[2];
	viewbox.y1 = viewboxarr[3];
	(*env)->ReleasePrimitiveArrayCritical(env, viewboxarray, viewboxarr, 0);

	buffer = (*env)->GetPrimitiveArrayCritical(env, bufferarray, 0);

	fz_context* ctx = page->ctx;

	fz_try(ctx)
	{

		 fz_colorspace *colorspace = fz_device_bgr(ctx);
		//fz_colorspace *colorspace = fz_device_rgb(ctx);
		int stride = (fz_colorspace_n(ctx, colorspace) + 1) * (viewbox.x1 - viewbox.x0);
		pixmap = fz_new_pixmap_with_data(ctx, colorspace,
				viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0,1,stride,
				(unsigned char*) buffer);

		//fz_invert_pixmap(ctx,pixmap);
		fz_clear_pixmap_with_value(ctx, pixmap, 0xFF);
		if(r!=-1 && g!=-1 && b!=-1){
			fz_tint_pixmap(ctx,pixmap,r,g,b);
		}

		dev = fz_new_draw_device(ctx, NULL, pixmap);

		fz_run_display_list(ctx, page->pageList, dev, &ctm, &viewbox, NULL);


		fz_drop_pixmap(ctx, pixmap);
	}
	fz_always(ctx)
	{
		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
	}
	fz_catch(ctx)
	{
		DEBUG("Render failed");
	}

	(*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);
}



//Outline
JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_open(JNIEnv *env,
		jclass clazz, jlong dochandle) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

	fz_context *ctx = doc->ctx;

	if(!doc || !ctx){
	    return -1;
	}

	if (!doc->outline){
		fz_try(ctx)
			doc->outline = fz_load_outline(ctx, doc->document);
		fz_catch(ctx)
			doc->outline = NULL;
	}

	return (jlong) (long) doc->outline;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_free(JNIEnv *env,
		jclass clazz, jlong dochandle) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
//    DEBUG("PdfOutline_free(%p)", doc);
	if (doc) {
		if (doc->outline)
			fz_drop_outline(doc->ctx, doc->outline);
		doc->outline = NULL;
	}
}


JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getTitle(JNIEnv *env,
		jclass clazz, jlong dochandle, jlong outlinehandle) {

    	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
        fz_outline *outline = (fz_outline*) (long) outlinehandle;
        if(!doc || !outline || doc->ctx == NULL){
                        return NULL;
         }
        if (outline){
            char st[4048];
            fz_try(doc->ctx)
                snprintf(st, 4047, "%s", outline->title);
            fz_catch(doc->ctx)
                return NULL;
            return (*env)->NewStringUTF(env, st);
        }


    	return NULL;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getLink(JNIEnv *env,
		jclass clazz, jlong outlinehandle, jlong dochandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

// DEBUG("PdfOutline_getLink(%p)",outline);
	if(!doc || !outline || doc->ctx == NULL){
                   return NULL;
     }

	char linkbuf[4048];
	int pageNo = outline->page;

	snprintf(linkbuf, 4047, "#%d", pageNo + 1);

	return (*env)->NewStringUTF(env, linkbuf);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getLinkUri(JNIEnv *env,
		jclass clazz, jlong outlinehandle, jlong dochandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

// DEBUG("PdfOutline_getLink(%p)",outline);
	if (!outline)
		return NULL;

	return (*env)->NewStringUTF(env, outline->uri);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_fillLinkTargetPoint(
		JNIEnv *env, jclass clazz, jlong outlinehandle, jfloatArray pointArray) {
		fz_outline *outline = (fz_outline*) (long) outlinehandle;
		int page = outline->page;


		return page;
}

static int
fillInOutlineItems(JNIEnv * env, jclass olClass, jmethodID ctor, jobjectArray arr, int pos, fz_outline *outline, int level)
{
	while (outline)
	{
		int page = outline->page;
		if (page >= 0 && outline->title)
		{
			jobject ol;
			jstring title = (*env)->NewStringUTF(env, outline->title);
			if (title == NULL) return -1;
			ol = (*env)->NewObject(env, olClass, ctor, level, title, page);
			if (ol == NULL) return -1;
			(*env)->SetObjectArrayElement(env, arr, pos, ol);
			(*env)->DeleteLocalRef(env, ol);
			(*env)->DeleteLocalRef(env, title);
			pos++;
		}
		pos = fillInOutlineItems(env, olClass, ctor, arr, pos, outline->down, level+1);
		if (pos < 0) return -1;
		outline = outline->next;
	}

	return pos;
}

//{
//	fz_outline *outline = (fz_outline*) (long) outlinehandle;
//
//	if (!outline || outline->dest.kind != FZ_LINK_GOTO) {
//		return 0;
//	}
//
//	jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
//	if (!point) {
//		return 0;
//	}
//
//	DEBUG("MuPdfOutline_fillLinkTargetPoint(): %d %x (%f, %f) - (%f, %f)",
//			outline->dest.ld.gotor.page, outline->dest.ld.gotor.flags,
//			outline->dest.ld.gotor.lt.x, outline->dest.ld.gotor.lt.y,
//			outline->dest.ld.gotor.rb.x, outline->dest.ld.gotor.rb.y);
//
//	point[0] = outline->dest.ld.gotor.lt.x;
//	point[1] = outline->dest.ld.gotor.lt.y;
//
//	(*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);
//
//	return outline->dest.ld.gotor.flags;
//}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getNext(JNIEnv *env,
		jclass clazz, jlong outlinehandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("MuPdfOutline_getNext(%p)",outline);

        if(outline)
            return (jlong) outline->next;
        else return -1;

	//return (jlong) (long) (outline ? outline->next : -1);
	//jlong res = -1;
	//return res;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getChild(JNIEnv *env,
		jclass clazz, jlong outlinehandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("MuPdfOutline_getChild(%p)",outline);
    if(outline){
        return (jlong)outline->down;
    }else{
        return -1;
    }
	//return outline ? outline->down : -1;
	//jlong res = -1;
	//return res;
}

///////////////
//SEARCH
//////////////
static int
charat(fz_context *ctx, fz_stext_page *page, int idx)
{
	fz_char_and_box cab;
	return fz_stext_char_at(ctx, &cab, page, idx)->c;
}

static const fz_rect
bboxcharat(fz_context *ctx, fz_stext_page *page, int idx)
{
	fz_char_and_box cab;
	return fz_stext_char_at(ctx, &cab, page, idx)->bbox;
}

static int
textlen(fz_stext_page *page)
{
	int len = 0;
	int block_num;

	for (block_num = 0; block_num < page->len; block_num++)
	{
		fz_stext_block *block;
		fz_stext_line *line;

		if (page->blocks[block_num].type != FZ_PAGE_BLOCK_TEXT)
			continue;
		block = page->blocks[block_num].u.text;
		for (line = block->lines; line < block->lines + block->len; line++)
		{
			fz_stext_span *span;

			for (span = line->first_span; span; span = span->next)
			{
				len += span->len;
			}
			len++; /* pseudo-newline */
		}
	}
	return len;
}


static int match(fz_context *ctx, fz_stext_page *page, const char *s, int n) {
	int orig = n;
	int c;
	while (*s) {
		s += fz_chartorune(&c, (char *) s);
		if (c == ' ' && charat(ctx,page, n) == ' ') {
			while (charat(ctx,page, n) == ' ') {
				n++;
			}
		} else {
			if (tolower(c) != tolower(charat(ctx,page, n))) {
				return 0;
			}
			n++;
		}
	}
	return n - orig;
}

JNIEXPORT jobjectArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_search(JNIEnv * env,
		jobject thiz, jlong dochandle, jlong pagehandle, jstring text) {

	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	fz_context* ctx = doc->ctx;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;
// DEBUG("MuPdfPage(%p).search(%p, %p)", thiz, doc, page);

	if (!doc || !page) {
		return NULL;
	}

	const char *str = (*env)->GetStringUTFChars(env, text, NULL);
	if (str == NULL) {
		return NULL;
	}

	ArrayListHelper alh;
	PageTextBoxHelper ptbh;
	if (!ArrayListHelper_init(&alh, env)
			|| !PageTextBoxHelper_init(&ptbh, env)) {
		//DEBUG("search(): JNI helper initialization failed", pagehandle);
		return NULL;
	}
	jobject arrayList = ArrayListHelper_create(&alh);
// DEBUG("MuPdfPage(%p).search(%p, %p): array: %p", thiz, doc, page, arrayList);
	if (!arrayList) {
		return NULL;
	}

	fz_rect *hit_bbox = NULL;

	fz_stext_sheet *sheet = NULL;
	fz_stext_page *pagetext = NULL;
	fz_device *dev = NULL;
	int pos;
	int len;
	int i, n;
	int hit_count = 0;

	fz_try(doc->ctx)
			{
				fz_rect rect;
				fz_rect mediabox;

				// DEBUG("MuPdfPage(%p).search(%p, %p): load page text", thiz, doc, page);

				fz_bound_page(ctx, page->page,  &rect);
				sheet = fz_new_stext_sheet(doc->ctx);
				pagetext = fz_new_stext_page(doc->ctx, fz_bound_page(ctx, page->page, &mediabox));
				dev = fz_new_stext_device(doc->ctx, sheet, pagetext, NULL);
				fz_run_page(ctx, page->page, dev, &fz_identity, NULL);

				// DEBUG("MuPdfPage(%p).search(%p, %p): free text device", thiz, doc, page);

				fz_close_device(ctx, dev);
				fz_drop_device(ctx, dev);
				dev = NULL;

				len = textlen(pagetext);

				// DEBUG("MuPdfPage(%p).search(%p, %p): text length: %d", thiz, doc, page, len);

				for (pos = 0; pos < len; pos++) {
					fz_rect rr = fz_empty_rect;
					// DEBUG("MuPdfPage(%p).search(%p, %p): match %d", thiz, doc, page, pos);

					n = match(ctx, pagetext, str, pos);
					if (n > 0) {
						DEBUG(
								"MuPdfPage(%p).search(%p, %p): match found: %d, %d",
								thiz, doc, page, pos, n);
						for (i = 0; i < n; i++) {
							fz_rect r2 =  bboxcharat(ctx, pagetext, pos + i);
							fz_union_rect(&rr, &r2);
						}

						if (!fz_is_empty_rect(&rr)) {
							int coords[4];
							coords[0] = (rr.x0);
							coords[1] = (rr.y0);
							coords[2] = (rr.x1);
							coords[3] = (rr.y1);
							DEBUG(
									"MuPdfPage(%p).search(%p, %p): found rectangle (%d, %d - %d, %d)",
									thiz, doc, page, coords[0], coords[1],
									coords[2], coords[3]);
							jobject ptb = PageTextBoxHelper_create(&ptbh);
							if (ptb) {
								// DEBUG("MuPdfPage(%p).search(%p, %p): rect %p", thiz, doc, page, ptb);
								PageTextBoxHelper_setRect(&ptbh, ptb, coords);
								// PageTextBoxHelper_setText(&ptbh, ptb, txt);
								// DEBUG("MuPdfPage(%p).search(%p, %p): add rect %p to array %p", thiz, doc, page, ptb, arrayList);
								ArrayListHelper_add(&alh, arrayList, ptb);
							}
						}
					}
				}
			}fz_always(doc->ctx)
			{
				// DEBUG("MuPdfPage(%p).search(%p, %p): free resources", thiz, doc, page);
				if (pagetext) {
					fz_drop_stext_page(doc->ctx, pagetext);
				}
				if (sheet) {
					fz_drop_stext_sheet(doc->ctx, sheet);
				}
				if (dev) {
					fz_close_device(doc->ctx, dev);
					fz_drop_device(doc->ctx, dev);
				}
			}fz_catch(doc->ctx) {
		jclass cls;
		(*env)->ReleaseStringUTFChars(env, text, str);
		cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
		if (cls != NULL) {
			(*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_searchPage");
		}
		(*env)->DeleteLocalRef(env, cls);
		return NULL;
	}

	(*env)->ReleaseStringUTFChars(env, text, str);

	return arrayList;
}


JNIEXPORT jobjectArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_text(JNIEnv * env,
		jobject thiz, jlong handle, jlong pagehandle) {

	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;

	jclass textCharClass;
	jclass textSpanClass;
	jclass textLineClass;
	jclass textBlockClass;
	jmethodID ctor;
	jobjectArray barr = NULL;
	fz_stext_sheet *sheet = NULL;
	fz_stext_page *text = NULL;
	fz_device *dev = NULL;
	fz_matrix ctm;

	fz_context *ctx = doc_t->ctx;
	fz_document *doc = doc_t->document;


	textCharClass = (*env)->FindClass(env, PACKAGENAME "/TextChar");
	if (textCharClass == NULL) return NULL;
	textSpanClass = (*env)->FindClass(env, "[L" PACKAGENAME "/TextChar;");
	if (textSpanClass == NULL) return NULL;
	textLineClass = (*env)->FindClass(env, "[[L" PACKAGENAME "/TextChar;");
	if (textLineClass == NULL) return NULL;
	textBlockClass = (*env)->FindClass(env, "[[[L" PACKAGENAME "/TextChar;");
	if (textBlockClass == NULL) return NULL;
	ctor = (*env)->GetMethodID(env, textCharClass, "<init>", "(FFFFC)V");
	if (ctor == NULL) return NULL;

	fz_var(sheet);
	fz_var(text);
	fz_var(dev);

	fz_try(ctx)
	{
		int b, l, s, c;
		fz_rect rect;
		fz_bound_page(ctx ,page->page,  &rect);
		fz_rect mediabox;

		sheet = fz_new_stext_sheet(ctx);
		text = fz_new_stext_page(ctx, fz_bound_page(ctx, page->page, &mediabox));
		dev = fz_new_stext_device(ctx, sheet, text, NULL);
		fz_run_page(ctx, page->page, dev, &fz_identity, NULL);


		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
		dev = NULL;

		barr = (*env)->NewObjectArray(env, text->len, textBlockClass, NULL);
		if (barr == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "NewObjectArray failed");

		for (b = 0; b < text->len; b++)
		{
			fz_stext_block *block;
			jobjectArray *larr;

			if (text->blocks[b].type != FZ_PAGE_BLOCK_TEXT)
				continue;
			block = text->blocks[b].u.text;
			larr = (*env)->NewObjectArray(env, block->len, textLineClass, NULL);
			if (larr == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "NewObjectArray failed");

			for (l = 0; l < block->len; l++)
			{
				fz_stext_line *line = &block->lines[l];
				jobjectArray *sarr;
				fz_stext_span *span;
				int len = 0;

				for (span = line->first_span; span; span = span->next)
					len++;

				sarr = (*env)->NewObjectArray(env, len, textSpanClass, NULL);
				if (sarr == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "NewObjectArray failed");

				for (s=0, span = line->first_span; span; s++, span = span->next)
				{
					jobjectArray *carr = (*env)->NewObjectArray(env, span->len, textCharClass, NULL);
					if (carr == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "NewObjectArray failed");

					for (c = 0; c < span->len; c++)
					{
						fz_stext_char *ch = &span->text[c];

						fz_rect bbox;
						fz_stext_char_bbox(ctx, &bbox, span, c);

						jobject cobj = (*env)->NewObject(env, textCharClass, ctor, bbox.x0, bbox.y0, bbox.x1, bbox.y1, ch->c);
						if (cobj == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "NewObjectfailed");

						(*env)->SetObjectArrayElement(env, carr, c, cobj);
						(*env)->DeleteLocalRef(env, cobj);
					}

					(*env)->SetObjectArrayElement(env, sarr, s, carr);
					(*env)->DeleteLocalRef(env, carr);
				}

				(*env)->SetObjectArrayElement(env, larr, l, sarr);
				(*env)->DeleteLocalRef(env, sarr);
			}

			(*env)->SetObjectArrayElement(env, barr, b, larr);
			(*env)->DeleteLocalRef(env, larr);
		}
	}
	fz_always(ctx)
	{
		fz_drop_stext_page(ctx, text);
		fz_drop_stext_sheet(ctx, sheet);
		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
	}
	fz_catch(ctx)
	{
		//jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
		//if (cls != NULL)
	//		(*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_text");
	//	(*env)->DeleteLocalRef(env, cls);

		return NULL;
	}

	return barr;
}


JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_hasChangesInternal(JNIEnv *env,
		jclass clazz, jlong handle) {
	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	fz_context *ctx = doc_t->ctx;

	pdf_document *idoc = pdf_specifics(ctx, doc_t->document);

	return (idoc && pdf_has_unsaved_changes(ctx, idoc)) ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_saveInternal(JNIEnv *env,
		jclass clazz, jlong handle, jstring fname) {
	DEBUG("save to file 1");
	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	fz_context *ctx = doc_t->ctx;
	const char *path = (*env)->GetStringUTFChars(env, fname, NULL);
	DEBUG("save to file %s", path);
	DEBUG("save to file 2");

	pdf_document *idoc = pdf_specifics(ctx, doc_t->document);

	pdf_write_options opts = { 0 };
	opts.do_incremental = pdf_can_be_saved_incrementally(ctx, idoc);

	DEBUG("save to file 3");


	fz_try(ctx)
	{
		//fz_write_document(ctx, doc_t->document, path, &opts);
		pdf_save_document(ctx, idoc, path, &opts);
	}
	fz_catch(ctx)
	{
		ERROR("save to file not success");
	}
	DEBUG("save to file 4");
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_deletePage(JNIEnv *env,
		jclass clazz, jlong handle, jint page) {
	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	fz_context *ctx = doc_t->ctx;
	pdf_document *idoc = pdf_specifics(ctx, doc_t->document);
	pdf_delete_page(ctx, idoc, page);
}


JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_addInkAnnotationInternal(JNIEnv *env,
		jobject thiz, jlong handle, jlong pagehandle, jfloatArray jcolors, jobjectArray arcs, jint width, jfloat alpha) {

	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;




	fz_context *ctx = doc_t->ctx;
	fz_document *doc = doc_t->document;
	pdf_document *idoc = pdf_specifics(ctx, doc);
	jclass pt_cls;
	jfieldID x_fid, y_fid;
	int i, j, k, n;
	float *pts = NULL;
	int *counts = NULL;
	int total = 0;
	float color[4];


	jfloat *co = (*env)->GetPrimitiveArrayCritical(env, jcolors, 0);
	color[0] = co[0];
	color[1] = co[1];
	color[2] = co[2];
	color[3] = alpha;

	(*env)->ReleasePrimitiveArrayCritical(env, jcolors, co, 0);

	if (idoc == NULL)
		return;


	fz_var(pts);
	fz_var(counts);

		pdf_annot *annot;
		fz_matrix ctm;

		float zoom = 72/160;
		//zoom = 1.0 / zoom;
		//fz_scale(&ctm, zoom,zoom);

		DEBUG("addInkAnnotationInternal 1");
		pt_cls = (*env)->FindClass(env, "android/graphics/PointF");
		DEBUG("addInkAnnotationInternal 2");
		if (pt_cls == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "FindClass");
		DEBUG("addInkAnnotationInternal 3");
		x_fid = (*env)->GetFieldID(env, pt_cls, "x", "F");
		DEBUG("addInkAnnotationInternal 4");
		if (x_fid == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(x)");
		DEBUG("addInkAnnotationInternal 5");
		y_fid = (*env)->GetFieldID(env, pt_cls, "y", "F");
		DEBUG("addInkAnnotationInternal 6");
		if (y_fid == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(y)");
		DEBUG("addInkAnnotationInternal 7");
		n = (*env)->GetArrayLength(env, arcs);
		DEBUG("addInkAnnotationInternal 8");
		counts = fz_malloc_array(ctx, n, sizeof(int));
		DEBUG("addInkAnnotationInternal 9");
		for (i = 0; i < n; i++)
		{
			jobjectArray arc = (jobjectArray)(*env)->GetObjectArrayElement(env, arcs, i);
			int count = (*env)->GetArrayLength(env, arc);
			(*env)->DeleteLocalRef(env,arc);

			counts[i] = count;
			total += count;
		}
		DEBUG("addInkAnnotationInternal 10");
		pts = fz_malloc_array(ctx, total * 2, sizeof(float));

		k = 0;
		for (i = 0; i < n; i++)
		{
			jobjectArray arc = (jobjectArray)(*env)->GetObjectArrayElement(env, arcs, i);
			int count = counts[i];

			for (j = 0; j < count; j++)
			{
				jobject jpt = (*env)->GetObjectArrayElement(env, arc, j);
				fz_point pt;
				pt.x = jpt ? (*env)->GetFloatField(env, jpt, x_fid) : 0.0f;
				pt.y = jpt ? (*env)->GetFloatField(env, jpt, y_fid) : 0.0f;
				(*env)->DeleteLocalRef(env, jpt);
				//fz_transform_point(&pts[k], &ctm);
				//k++;
				pts[k++] = pt.x;
				pts[k++] = pt.y;
			}
			(*env)->DeleteLocalRef(env, arc);
		}
		DEBUG("addInkAnnotationInternal 11");
		fz_try(ctx)
			{
		//annot = (fz_annot *)pdf_create_annot(ctx, idoc, (pdf_page *)page->page, FZ_ANNOT_INK);
		//pdf_set_ink_annot_list(ctx, idoc, (pdf_annot *)annot, pts, counts, n, color, width);

			annot = pdf_create_annot(ctx, (pdf_page *)page->page, PDF_ANNOT_INK);

			pdf_set_annot_border(ctx, annot, width);
			pdf_set_annot_color(ctx, annot, 3, color);
			pdf_set_annot_ink_list(ctx, annot, n, counts, pts);

		//page->pageList = fz_new_display_list(ctx);

		//dev = fz_new_list_device(ctx, page->pageList);
		//fz_drop_display_list(ctx, page->annot_list);
		//page->annot_list= NULL;
	}
	fz_always(ctx)
	{
		fz_free(ctx, pts);
		fz_free(ctx, counts);
	}
	fz_catch(ctx)
	{
		//LOGE("addInkAnnotation: %s failed", ctx->error->message);
		jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
		if (cls != NULL)
			(*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_searchPage");
		(*env)->DeleteLocalRef(env, cls);
	}
}

JNIEXPORT jobjectArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getAnnotationsInternal(JNIEnv * env,
		jobject thiz, jlong handle, jlong pagehandle, jobjectArray arcs) {

	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;
	fz_context *ctx = doc_t->ctx;
	pdf_document *idoc = pdf_specifics(ctx, doc_t->document);

	jclass annotClass;
	jmethodID ctor;
	jobjectArray arr;
	jobject jannot;
	fz_annot *annot;
	fz_matrix ctm;
	int count;



	annotClass = (*env)->FindClass(env, "org/ebookdroid/core/codec/Annotation");
	if (annotClass == NULL) return NULL;
	ctor = (*env)->GetMethodID(env, annotClass, "<init>", "(FFFFILjava/lang/String;)V");
	if (ctor == NULL) return NULL;



	fz_scale(&ctm, 1, 1);

	count = 0;
	for (annot = fz_first_annot(ctx, page->page); annot; annot = fz_next_annot(ctx, annot))
		count ++;

	arr = (*env)->NewObjectArray(env, count, annotClass, NULL);
	if (arr == NULL) return NULL;

	count = 0;
	for (annot = fz_first_annot(ctx, page->page); annot; annot = fz_next_annot(ctx,  annot))
	{
		fz_rect rect;
		fz_annot_type type = pdf_annot_type(ctx, (pdf_annot *)annot);
		fz_bound_annot(ctx,  annot, &rect);
		const char *content = pdf_annot_contents(ctx, (pdf_annot *)annot);
		jstring text  = (*env)->NewStringUTF(env, content);

		jannot = (*env)->NewObject(env, annotClass, ctor,
				(float)rect.x0, (float)rect.y0, (float)rect.x1, (float)rect.y1, type,text);
		if (jannot == NULL) return NULL;
		(*env)->SetObjectArrayElement(env, arr, count, jannot);
		(*env)->DeleteLocalRef(env, jannot);

		count ++;
	}

	return arr;
}
JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_deleteAnnotationInternal(JNIEnv * env, jobject thiz, jlong handle, jlong pagehandle,int annot_index)
{
	//LOGE("deleteAnnotationInternal 1");
	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;
	fz_context *ctx = doc_t->ctx;
	fz_annot *annot;
	pdf_document *idoc = pdf_specifics(ctx, doc_t->document);

	//LOGE("deleteAnnotationInternal 2");

	fz_try(ctx)
	{
		//LOGE("deleteAnnotationInternal 3");
		annot = fz_first_annot(ctx, page->page);
		//LOGE("deleteAnnotationInternal 31");
		int i;
		for (i = 0; i < annot_index && annot; i++){
			//LOGE("deleteAnnotationInternal 32");
			annot = fz_next_annot(ctx,  annot);
			//LOGE("deleteAnnotationInternal 33");
		}

		if (annot)
		{
			//LOGE("deleteAnnotationInternal 4");
			//pdf_delete_annot(ctx, idoc, (pdf_page *) page->page, (pdf_annot *)annot);
			pdf_delete_annot(ctx, (pdf_page *)page->page, (pdf_annot *)annot);

			//fz_drop_display_list(ctx, page->pageList);
		}
	}
	fz_catch(ctx)
	{
		//LOGE("deleteAnnotationInternal: %s", ctx->error->message);
	}
}


JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_addMarkupAnnotationInternal(JNIEnv * env,
		jobject thiz, jlong handle, jlong pagehandle, jobjectArray points, fz_annot_type type, jobjectArray jcolors) {

	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;

	fz_context *ctx = doc_t->ctx;
	fz_document *doc = doc_t->document;
	pdf_document *idoc = pdf_specifics(ctx, doc);
	jclass pt_cls;
	jfieldID x_fid, y_fid;
	int i, n;
	float *pts = NULL;
	float color[3];
	float alpha;
	float line_height;
	float line_thickness;

	if (idoc == NULL)
		return;

	switch (type)
	{
		case PDF_ANNOT_HIGHLIGHT:
			alpha = 0.4;
			line_thickness = 1.0;
			line_height = 0.45;
			break;
		case PDF_ANNOT_UNDERLINE:
			alpha = 1.0;
			line_thickness = LINE_THICKNESS;
			line_height = UNDERLINE_HEIGHT;
			break;
		case PDF_ANNOT_STRIKE_OUT:
			alpha = 1.0;
			line_thickness = LINE_THICKNESS;
			line_height = STRIKE_HEIGHT;
			break;
		default:
			return;
	}
	//LOGE("addMarkupAnnotationInternal 1");

	jfloat *co = (*env)->GetPrimitiveArrayCritical(env, jcolors, 0);
	color[0] = co[0];
	color[1] = co[1];
	color[2] = co[2];

	(*env)->ReleasePrimitiveArrayCritical(env, jcolors, co, 0);

	fz_var(pts);
	fz_try(ctx)
	{
		fz_annot *annot;
		fz_matrix ctm;

		//LOGE("addMarkupAnnotationInternal 2");

		pt_cls = (*env)->FindClass(env, "android/graphics/PointF");
		if (pt_cls == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "FindClass");
		x_fid = (*env)->GetFieldID(env, pt_cls, "x", "F");
		if (x_fid == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(x)");
		y_fid = (*env)->GetFieldID(env, pt_cls, "y", "F");
		if (y_fid == NULL) fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(y)");

		n = (*env)->GetArrayLength(env, points);

		//LOGE("addMarkupAnnotationInternal 3");
		pts = fz_malloc_array(ctx, n * 2, sizeof(float));
		//LOGE("addMarkupAnnotationInternal 4");
		for (i = 0; i < n; i++)
		{
			fz_point pt;
			jobject opt = (*env)->GetObjectArrayElement(env, points, i);
			pt.x = opt ? (*env)->GetFloatField(env, opt, x_fid) : 0.0f;
			pt.y = opt ? (*env)->GetFloatField(env, opt, y_fid) : 0.0f;
			//fz_transform_point(&pts[i], &ctm);

			(*env)->DeleteLocalRef(env,opt);
			pts[i*2+0] = pt.x;
			pts[i*2+1] = pt.y;
		}

		//LOGE("addMarkupAnnotationInternal 5");
		//annot = (fz_annot *)pdf_create_annot(ctx, idoc, (pdf_page *)page->page, type);
		//pdf_set_markup_annot_quadpoints(ctx,idoc, (pdf_annot *)annot, pts, n);
		//pdf_set_markup_appearance(ctx,idoc, (pdf_annot *)annot, color, alpha, line_thickness, line_height);


		annot = (fz_annot *)pdf_create_annot(ctx, (pdf_page *)page->page, type);
		pdf_set_annot_quad_points(ctx, (pdf_annot *)annot, n / 4, pts);
		pdf_set_markup_appearance(ctx, idoc, (pdf_annot *)annot, color, alpha, line_thickness, line_height);


		//dump_annotation_display_lists(glo);
	}
	fz_always(ctx)
	{
		fz_free(ctx, pts);
	}
	fz_catch(ctx)
	{
		//LOGE("addStrikeOutAnnotation: %s failed", ctx->error->message);
		jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
		if (cls != NULL)
			(*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_searchPage");
		(*env)->DeleteLocalRef(env, cls);
	}
}


void
fz_print_stext_page_as_text_my1(fz_context *ctx, fz_output *out, fz_stext_page *page, int opts)
{
	int block_n;

	for (block_n = 0; block_n < page->len; block_n++)
	{
		if (opts == 4 && page->blocks[block_n].type == FZ_PAGE_BLOCK_IMAGE)
		{
			fz_write_printf(ctx, out, "<image-begin>");
			//fz_write_image_as_data_uri(ctx, out, page->blocks[block_n].u.image);

			//image begin


			fz_image_block *image = page->blocks[block_n].u.image;
			fz_compressed_buffer *buffer = fz_compressed_image_buffer(ctx, image->image);
			//fz_write_printf(ctx, out, "<img width=%d height=%d src=\"data:", image->image->w, image->image->h);
			switch (buffer == NULL ? FZ_IMAGE_JPX : buffer->params.type)
			{
			case FZ_IMAGE_JPEG:
				fz_write_printf(ctx, out, "data:image/jpeg;base64,");
				send_data_base64_stext(ctx, out, buffer->buffer);
				break;
			case FZ_IMAGE_PNG:
				fz_write_printf(ctx, out, "data:image/png;base64,");
				send_data_base64_stext(ctx, out, buffer->buffer);
				break;
			default:
				{
					fz_buffer *buf = fz_new_buffer_from_image_as_png(ctx, image->image);
					fz_write_printf(ctx, out, "data:image/png;base64,");
					send_data_base64_stext(ctx, out, buf);
					fz_drop_buffer(ctx, buf);
					break;
				}
			}
			//fz_write_printf(ctx, out, "\">\n");


			//image end


			fz_write_string(ctx, out, "<image-end>");
			fz_write_printf(ctx, out, "<br/>");
		}else if(page->blocks[block_n].type == FZ_PAGE_BLOCK_TEXT){
			fz_stext_block *block = page->blocks[block_n].u.text;
			fz_stext_line *line;
			fz_stext_char *ch;
			char utf[10];
			int i, n;

			fz_write_printf(ctx, out, "<p>");

			fz_font *font = block->lines->first_span->text->style->font;
			fz_font *font2 = block->lines->last_span->text->style->font;


			int is_bold = fz_font_is_bold(ctx,font) && fz_font_is_bold(ctx,font2);
			int is_italic = fz_font_is_italic(ctx, font) && fz_font_is_italic(ctx, font2);
			int is_mono = fz_font_is_monospaced(ctx, font) && fz_font_is_monospaced(ctx, font2);



	        int fs = block->lines->first_span->text->style->size;
	        DEBUG("fontSize get %d", fs);

            if(fs > fontSize){
			    fz_write_printf(ctx,out,"<pause>");
			}

			if (is_bold) fz_write_printf(ctx,out,"<b>");
			if (is_italic) fz_write_printf(ctx,out,"<i>");
			//if (is_mono) fz_write_printf(ctx,out,"<tt>");


			for (line = block->lines; line < block->lines + block->len; line++)
			{

				fz_font *l_font = line->first_span->text->style->font;
				fz_font *l_font2 = line->last_span->text->style->font;

				int a = fz_font_is_bold(ctx,l_font) && fz_font_is_bold(ctx,l_font2);
				int b = fz_font_is_italic(ctx,l_font) && fz_font_is_italic(ctx,l_font2);
				int is_pause_line = !is_bold && !is_italic && (a || b);

				fz_stext_span *span;
				for (span = line->first_span; span; span = span->next)
				{

					fz_font *s_font = span->text->style->font;




					int is_pause_span = !is_pause_line &&( fz_font_is_bold(ctx,s_font) || fz_font_is_italic(ctx,s_font));

					for (ch = span->text; ch < span->text + span->len; ch++)
					{

						int is_bold_ch = !is_bold && fz_font_is_bold(ctx, ch->style->font);
						int is_italic_ch = !is_italic && fz_font_is_italic(ctx, ch->style->font);
						int is_mono_ch = !is_mono && fz_font_is_monospaced(ctx, ch->style->font);


						if (is_bold_ch) fz_write_printf(ctx,out,"<b>");
						if (is_italic_ch) fz_write_printf(ctx,out,"<i>");
						//if (is_mono_ch) fz_write_printf(ctx,out,"<code>");

						switch (ch->c)
							{
							case '<': fz_write_string(ctx, out, "&lt;"); break;
							case '>': fz_write_string(ctx, out, "&gt;"); break;
							case '&': fz_write_string(ctx, out, "&amp;"); break;
							case '"': fz_write_string(ctx, out, "&quot;"); break;
							//case '\'': fz_write_string(ctx, out, "&apos;"); break;
							default:
								   n = fz_runetochar(utf, ch->c);
									for (i = 0; i < n; i++)
										fz_write_byte(ctx, out, utf[i]);
								   break;
							}

						if (is_bold_ch) fz_write_printf(ctx,out,"</b>");
						if (is_italic_ch) fz_write_printf(ctx,out,"</i>");
						//if (is_mono_ch) fz_write_printf(ctx,out,"</code>");
					}

					if(is_pause_span){
						//fz_write_printf(ctx,out,"<pause-span>");
					}



				fz_write_string(ctx, out, "<end-line>");
				}
				if(is_pause_line){
					//fz_write_printf(ctx,out,"<pause-line>");
				}

			}
			if(block->lines->first_span->text->style->size > fontSize){
			    fz_write_printf(ctx,out,"<pause>");
			}


			if (is_bold) {fz_write_printf(ctx,out,"</b>"); fz_write_printf(ctx,out,"<pause>");}
			if (is_italic) {fz_write_printf(ctx,out,"</i>"); fz_write_printf(ctx,out,"<pause>");}
			//if (is_mono) fz_write_printf(ctx,out,"</tt>");
			fz_write_printf(ctx,out,"</p>");
			if(fs > fontSize){
                fz_write_printf(ctx,out,"<pause>");
            }
			//fz_printf(ctx, out, "<br/>");

			if(block_n < page->len-1){
            		    fz_write_printf(ctx,out,"<end-block>");
            		}
		}

	}

}




JNIEXPORT jbyteArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getPageAsHtml(JNIEnv * env, jobject thiz,  jlong handle, jlong pagehandle, jint opts)
{

	renderdocument_t *doc_t = (renderdocument_t*) (long) handle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;

	fz_context *ctx = doc_t->ctx;
	fz_document *doc = doc_t->document;
	pdf_document *idoc = pdf_specifics(ctx, doc);

	fz_stext_sheet *sheet = NULL;
	fz_stext_page *text = NULL;
	fz_device *dev = NULL;
	fz_matrix ctm;

	jbyteArray bArray = NULL;
	fz_buffer *buf = NULL;
	fz_output *out = NULL;

	size_t len;

	fz_var(sheet);
	fz_var(text);


	fz_var(dev);
	fz_var(buf);
	fz_var(out);

	unsigned char *data;

	fz_try(ctx)
	{
		int b, l, s, c;
		fz_rect mediabox;

		ctm = fz_identity;
		sheet = fz_new_stext_sheet(ctx);


		//fz_stext_options opts;
		//fz_parse_stext_options(ctx, &opts, options);

		text = fz_new_stext_page(ctx, fz_bound_page(ctx, page->page, &mediabox));


		dev = fz_new_stext_device(ctx,sheet,text, NULL);
		int j = (int)opts;
		if(j == 4){
			fz_disable_device_hints(ctx, dev, FZ_IGNORE_IMAGE);
		}


		fz_run_page(ctx, page->page, dev, &ctm, NULL);


		fz_close_device(ctx, dev);
		//fz_drop_device(ctx, dev);
		//dev = NULL;

		//fz_analyze_text(ctx, sheet, text);

		buf = fz_new_buffer(ctx, 256);
		out = fz_new_output_with_buffer(ctx, buf);

		fz_print_stext_page_as_text_my1(ctx, out, text, j);

		//fz_drop_output(ctx, out);
		//out = NULL;

		len = fz_buffer_storage(ctx, buf, &data);

		//bArray = (*env)->NewByteArray(env, buf->len);
		bArray = (*env)->NewByteArray(env, len);
		if (bArray == NULL)
			fz_throw(ctx, FZ_ERROR_GENERIC, "Failed to make byteArray");
		(*env)->SetByteArrayRegion(env, bArray, 0, len, (const jbyte *)data);

	}
	fz_always(ctx)
	{
		fz_drop_stext_page(ctx, text);
		fz_drop_stext_sheet(ctx, sheet);
		//fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
		fz_drop_output(ctx, out);
		fz_drop_buffer(ctx, buf);
	}
	fz_catch(ctx)
	{
		jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
		if (cls != NULL)
			(*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_textAsHtml");
		(*env)->DeleteLocalRef(env, cls);

		return NULL;
	}

	return bArray;
}




