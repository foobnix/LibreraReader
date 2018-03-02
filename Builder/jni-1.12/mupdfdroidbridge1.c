#include <jni.h>


#include <android/log.h>

#include <android/bitmap.h>
#include "mupdf/fitz.h"
#include "mupdf/ucdn.h"
#include "mupdf/pdf.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

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

#define FUN(A) Java_com_artifex_mupdf_fitz_ ## A
#define PKG "com/artifex/mupdf/fitz/"

/* Do our best to avoid type casting warnings. */

#define CAST(type, var) (type)pointer_cast(var)

static inline void *pointer_cast(jlong l)
{
	return (void *)(intptr_t)l;
}

static inline jlong jlong_cast(const void *p)
{
	return (jlong)(intptr_t)p;
}


#define LOG_TAG "libmupdf"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGT(...) __android_log_print(ANDROID_LOG_INFO,"alert",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


/* All the cached classes/mids/fids we need. */

static jclass cls_Annotation;
static jclass cls_Buffer;
static jclass cls_ColorSpace;
static jclass cls_Cookie;
static jclass cls_Device;
static jclass cls_DisplayList;
static jclass cls_Document;
static jclass cls_DocumentWriter;
static jclass cls_FloatArray;
static jclass cls_Font;
static jclass cls_IllegalArgumentException;
static jclass cls_Image;
static jclass cls_IndexOutOfBoundsException;
static jclass cls_IntegerArray;
static jclass cls_IOException;
static jclass cls_Link;
static jclass cls_Matrix;
static jclass cls_NativeDevice;
static jclass cls_NullPointerException;
static jclass cls_Object;
static jclass cls_Outline;
static jclass cls_OutOfMemoryError;
static jclass cls_Page;
static jclass cls_Path;
static jclass cls_PathWalker;
static jclass cls_PDFAnnotation;
static jclass cls_PDFDocument;
static jclass cls_PDFPage;
static jclass cls_PDFGraftMap;
static jclass cls_PDFObject;
static jclass cls_Pixmap;
static jclass cls_Point;
static jclass cls_Rect;
static jclass cls_RuntimeException;
static jclass cls_Shade;
static jclass cls_StrokeState;
static jclass cls_StructuredText;
static jclass cls_Text;
static jclass cls_TextBlock;
static jclass cls_TextChar;
static jclass cls_TextLine;
static jclass cls_TextWalker;
static jclass cls_TryLaterException;

static jfieldID fid_Annotation_pointer;
static jfieldID fid_Buffer_pointer;
static jfieldID fid_ColorSpace_pointer;
static jfieldID fid_Cookie_pointer;
static jfieldID fid_Device_pointer;
static jfieldID fid_DisplayList_pointer;
static jfieldID fid_Document_pointer;
static jfieldID fid_DocumentWriter_pointer;
static jfieldID fid_Font_pointer;
static jfieldID fid_Image_pointer;
static jfieldID fid_Link_bounds;
static jfieldID fid_Link_page;
static jfieldID fid_Link_uri;
static jfieldID fid_Matrix_a;
static jfieldID fid_Matrix_b;
static jfieldID fid_Matrix_c;
static jfieldID fid_Matrix_d;
static jfieldID fid_Matrix_e;
static jfieldID fid_Matrix_f;
static jfieldID fid_NativeDevice_nativeInfo;
static jfieldID fid_NativeDevice_nativeResource;
static jfieldID fid_Page_pointer;
static jfieldID fid_Path_pointer;
static jfieldID fid_PDFAnnotation_pointer;
static jfieldID fid_PDFDocument_pointer;
static jfieldID fid_PDFPage_pointer;
static jfieldID fid_PDFGraftMap_pointer;
static jfieldID fid_PDFObject_pointer;
static jfieldID fid_PDFObject_Null;
static jfieldID fid_Pixmap_pointer;
static jfieldID fid_Point_x;
static jfieldID fid_Point_y;
static jfieldID fid_Rect_x0;
static jfieldID fid_Rect_x1;
static jfieldID fid_Rect_y0;
static jfieldID fid_Rect_y1;
static jfieldID fid_Shade_pointer;
static jfieldID fid_StrokeState_pointer;
static jfieldID fid_StructuredText_pointer;
static jfieldID fid_TextBlock_bbox;
static jfieldID fid_TextBlock_lines;
static jfieldID fid_TextChar_bbox;
static jfieldID fid_TextChar_c;
static jfieldID fid_TextLine_bbox;
static jfieldID fid_TextLine_chars;
static jfieldID fid_Text_pointer;

static jmethodID mid_Annotation_init;
static jmethodID mid_ColorSpace_fromPointer;
static jmethodID mid_ColorSpace_init;
static jmethodID mid_Device_beginGroup;
static jmethodID mid_Device_beginMask;
static jmethodID mid_Device_beginTile;
static jmethodID mid_Device_clipImageMask;
static jmethodID mid_Device_clipPath;
static jmethodID mid_Device_clipStrokePath;
static jmethodID mid_Device_clipStrokeText;
static jmethodID mid_Device_clipText;
static jmethodID mid_Device_endGroup;
static jmethodID mid_Device_endMask;
static jmethodID mid_Device_endTile;
static jmethodID mid_Device_fillImage;
static jmethodID mid_Device_fillImageMask;
static jmethodID mid_Device_fillPath;
static jmethodID mid_Device_fillShade;
static jmethodID mid_Device_fillText;
static jmethodID mid_Device_ignoreText;
static jmethodID mid_Device_init;
static jmethodID mid_Device_popClip;
static jmethodID mid_Device_strokePath;
static jmethodID mid_Device_strokeText;
static jmethodID mid_DisplayList_init;
static jmethodID mid_Document_init;
static jmethodID mid_Font_init;
static jmethodID mid_Image_init;
static jmethodID mid_Link_init;
static jmethodID mid_Matrix_init;
static jmethodID mid_Object_toString;
static jmethodID mid_Outline_init;
static jmethodID mid_Page_init;
static jmethodID mid_Path_init;
static jmethodID mid_PathWalker_closePath;
static jmethodID mid_PathWalker_curveTo;
static jmethodID mid_PathWalker_lineTo;
static jmethodID mid_PathWalker_moveTo;
static jmethodID mid_PDFAnnotation_init;
static jmethodID mid_PDFDocument_init;
static jmethodID mid_PDFPage_init;
static jmethodID mid_PDFGraftMap_init;
static jmethodID mid_PDFObject_init;
static jmethodID mid_Pixmap_init;
static jmethodID mid_Point_init;
static jmethodID mid_Rect_init;
static jmethodID mid_Shade_init;
static jmethodID mid_StrokeState_init;
static jmethodID mid_StructuredText_init;
static jmethodID mid_TextBlock_init;
static jmethodID mid_TextChar_init;
static jmethodID mid_Text_init;
static jmethodID mid_TextLine_init;
static jmethodID mid_TextWalker_showGlyph;

static const char *current_class_name = NULL;
static jclass current_class = NULL;

static jclass get_class(int *failed, JNIEnv *env, const char *name)
{
	jclass local;

	if (*failed) return NULL;

	current_class_name = name;
	local = (*env)->FindClass(env, name);
	if (!local)
	{
		LOGI("Failed to find class %s", name);
		*failed = 1;
		return NULL;
	}

	current_class = (*env)->NewGlobalRef(env, local);
	if (!current_class)
	{
		LOGI("Failed to make global ref for %s", name);
		*failed = 1;
		return NULL;
	}

	(*env)->DeleteLocalRef(env, local);

	return current_class;
}

static jfieldID get_field(int *failed, JNIEnv *env, const char *field, const char *sig)
{
	jfieldID fid;

	if (*failed || !current_class) return NULL;

	fid = (*env)->GetFieldID(env, current_class, field, sig);
	if (fid == 0)
	{
		LOGI("Failed to get field for %s %s %s", current_class_name, field, sig);
		*failed = 1;
	}

	return fid;
}

static jfieldID get_static_field(int *failed, JNIEnv *env, const char *field, const char *sig)
{
	jfieldID fid;

	if (*failed || !current_class) return NULL;

	fid = (*env)->GetStaticFieldID(env, current_class, field, sig);
	if (fid == 0)
	{
		LOGI("Failed to get static field for %s %s %s", current_class_name, field, sig);
		*failed = 1;
	}

	return fid;
}

static jmethodID get_method(int *failed, JNIEnv *env, const char *method, const char *sig)
{
	jmethodID mid;

	if (*failed || !current_class) return NULL;

	mid = (*env)->GetMethodID(env, current_class, method, sig);
	if (mid == 0)
	{
		LOGI("Failed to get method for %s %s %s", current_class_name, method, sig);
		*failed = 1;
	}

	return mid;
}

static jmethodID get_static_method(int *failed, JNIEnv *env, const char *method, const char *sig)
{
	jmethodID mid;

	if (*failed || !current_class) return NULL;

	mid = (*env)->GetStaticMethodID(env, current_class, method, sig);
	if (mid == 0)
	{
		LOGI("Failed to get static method for %s %s %s", current_class_name, method, sig);
		*failed = 1;
	}

	return mid;
}


static int find_fids(JNIEnv *env)
{
	int err = 0;

	cls_Annotation = get_class(&err, env, PKG"Annotation");
	fid_Annotation_pointer = get_field(&err, env, "pointer", "J");
	mid_Annotation_init = get_method(&err, env, "<init>", "(J)V");

	cls_Buffer = get_class(&err, env, PKG"Buffer");
	fid_Buffer_pointer = get_field(&err, env, "pointer", "J");


	cls_Cookie = get_class(&err, env, PKG"Cookie");
	fid_Cookie_pointer = get_field(&err, env, "pointer", "J");

	cls_Device = get_class(&err, env, PKG"Device");
	fid_Device_pointer = get_field(&err, env, "pointer", "J");
	mid_Device_init = get_method(&err, env, "<init>", "(J)V");
	mid_Device_fillPath = get_method(&err, env, "fillPath", "(L"PKG"Path;ZL"PKG"Matrix;L"PKG"ColorSpace;[FF)V");
	mid_Device_strokePath = get_method(&err, env, "strokePath", "(L"PKG"Path;L"PKG"StrokeState;L"PKG"Matrix;L"PKG"ColorSpace;[FF)V");
	mid_Device_clipPath = get_method(&err, env, "clipPath", "(L"PKG"Path;ZL"PKG"Matrix;)V");
	mid_Device_clipStrokePath = get_method(&err, env, "clipStrokePath", "(L"PKG"Path;L"PKG"StrokeState;L"PKG"Matrix;)V");
	mid_Device_fillText = get_method(&err, env, "fillText", "(L"PKG"Text;L"PKG"Matrix;L"PKG"ColorSpace;[FF)V");
	mid_Device_strokeText = get_method(&err, env, "strokeText", "(L"PKG"Text;L"PKG"StrokeState;L"PKG"Matrix;L"PKG"ColorSpace;[FF)V");
	mid_Device_clipText = get_method(&err, env, "clipText", "(L"PKG"Text;L"PKG"Matrix;)V");
	mid_Device_clipStrokeText = get_method(&err, env, "clipStrokeText", "(L"PKG"Text;L"PKG"StrokeState;L"PKG"Matrix;)V");
	mid_Device_ignoreText = get_method(&err, env, "ignoreText", "(L"PKG"Text;L"PKG"Matrix;)V");
	mid_Device_fillShade = get_method(&err, env, "fillShade", "(L"PKG"Shade;L"PKG"Matrix;F)V");
	mid_Device_fillImage = get_method(&err, env, "fillImage", "(L"PKG"Image;L"PKG"Matrix;F)V");
	mid_Device_fillImageMask = get_method(&err, env, "fillImageMask", "(L"PKG"Image;L"PKG"Matrix;L"PKG"ColorSpace;[FF)V");
	mid_Device_clipImageMask = get_method(&err, env, "clipImageMask", "(L"PKG"Image;L"PKG"Matrix;)V");
	mid_Device_popClip = get_method(&err, env, "popClip", "()V");
	mid_Device_beginMask = get_method(&err, env, "beginMask", "(L"PKG"Rect;ZL"PKG"ColorSpace;[F)V");
	mid_Device_endMask = get_method(&err, env, "endMask", "()V");
	mid_Device_beginGroup = get_method(&err, env, "beginGroup", "(L"PKG"Rect;L"PKG"ColorSpace;ZZIF)V");
	mid_Device_endGroup = get_method(&err, env, "endGroup", "()V");
	mid_Device_beginTile = get_method(&err, env, "beginTile", "(L"PKG"Rect;L"PKG"Rect;FFL"PKG"Matrix;I)I");
	mid_Device_endTile = get_method(&err, env, "endTile", "()V");

	cls_DisplayList = get_class(&err, env, PKG"DisplayList");
	fid_DisplayList_pointer = get_field(&err, env, "pointer", "J");
	mid_DisplayList_init = get_method(&err, env, "<init>", "(J)V");

	cls_Document = get_class(&err, env, PKG"Document");
	fid_Document_pointer = get_field(&err, env, "pointer", "J");
	mid_Document_init = get_method(&err, env, "<init>", "(J)V");

	cls_DocumentWriter = get_class(&err, env, PKG"DocumentWriter");
	fid_DocumentWriter_pointer = get_field(&err, env, "pointer", "J");

	cls_Font = get_class(&err, env, PKG"Font");
	fid_Font_pointer = get_field(&err, env, "pointer", "J");
	mid_Font_init = get_method(&err, env, "<init>", "(J)V");

	cls_Image = get_class(&err, env, PKG"Image");
	fid_Image_pointer = get_field(&err, env, "pointer", "J");
	mid_Image_init = get_method(&err, env, "<init>", "(J)V");

	cls_Link = get_class(&err, env, PKG"Link");
	fid_Link_bounds = get_field(&err, env, "bounds", "L"PKG"Rect;");
	fid_Link_page = get_field(&err, env, "page", "I");
	fid_Link_uri = get_field(&err, env, "uri", "Ljava/lang/String;");
	mid_Link_init = get_method(&err, env, "<init>", "(L"PKG"Rect;ILjava/lang/String;)V");

	cls_Matrix = get_class(&err, env, PKG"Matrix");
	fid_Matrix_a = get_field(&err, env, "a", "F");
	fid_Matrix_b = get_field(&err, env, "b", "F");
	fid_Matrix_c = get_field(&err, env, "c", "F");
	fid_Matrix_d = get_field(&err, env, "d", "F");
	fid_Matrix_e = get_field(&err, env, "e", "F");
	fid_Matrix_f = get_field(&err, env, "f", "F");
	mid_Matrix_init = get_method(&err, env, "<init>", "(FFFFFF)V");

	cls_NativeDevice = get_class(&err, env, PKG"NativeDevice");
	fid_NativeDevice_nativeResource = get_field(&err, env, "nativeResource", "Ljava/lang/Object;");
	fid_NativeDevice_nativeInfo = get_field(&err, env, "nativeInfo", "J");

	cls_Outline = get_class(&err, env, PKG"Outline");
	mid_Outline_init = get_method(&err, env, "<init>", "(Ljava/lang/String;ILjava/lang/String;FF[L"PKG"Outline;)V");

	cls_Page = get_class(&err, env, PKG"Page");
	fid_Page_pointer = get_field(&err, env, "pointer", "J");
	mid_Page_init = get_method(&err, env, "<init>", "(J)V");

	cls_Path = get_class(&err, env, PKG"Path");
	fid_Path_pointer = get_field(&err, env, "pointer", "J");
	mid_Path_init = get_method(&err, env, "<init>", "(J)V");

	cls_PathWalker = get_class(&err, env, PKG"PathWalker");
	mid_PathWalker_moveTo = get_method(&err, env, "moveTo", "(FF)V");
	mid_PathWalker_lineTo = get_method(&err, env, "lineTo", "(FF)V");
	mid_PathWalker_curveTo = get_method(&err, env, "curveTo", "(FFFFFF)V");
	mid_PathWalker_closePath = get_method(&err, env, "closePath", "()V");

	cls_PDFAnnotation = get_class(&err, env, PKG"PDFAnnotation");
	fid_PDFAnnotation_pointer = get_field(&err, env, "pointer", "J");
	mid_PDFAnnotation_init = get_method(&err, env, "<init>", "(J)V");

	cls_PDFDocument = get_class(&err, env, PKG"PDFDocument");
	fid_PDFDocument_pointer = get_field(&err, env, "pointer", "J");
	mid_PDFDocument_init = get_method(&err, env, "<init>", "(J)V");

	cls_PDFGraftMap = get_class(&err, env, PKG"PDFGraftMap");
	fid_PDFGraftMap_pointer = get_field(&err, env, "pointer", "J");
	mid_PDFGraftMap_init = get_method(&err, env, "<init>", "(J)V");


	cls_PDFPage = get_class(&err, env, PKG"PDFPage");
	fid_PDFPage_pointer = get_field(&err, env, "pointer", "J");
	mid_PDFPage_init = get_method(&err, env, "<init>", "(J)V");

	cls_Pixmap = get_class(&err, env, PKG"Pixmap");
	fid_Pixmap_pointer = get_field(&err, env, "pointer", "J");
	mid_Pixmap_init = get_method(&err, env, "<init>", "(J)V");

	cls_Point = get_class(&err, env, PKG"Point");
	mid_Point_init = get_method(&err, env, "<init>", "(FF)V");
	fid_Point_x = get_field(&err, env, "x", "F");
	fid_Point_y = get_field(&err, env, "y", "F");

	cls_Rect = get_class(&err, env, PKG"Rect");
	fid_Rect_x0 = get_field(&err, env, "x0", "F");
	fid_Rect_x1 = get_field(&err, env, "x1", "F");
	fid_Rect_y0 = get_field(&err, env, "y0", "F");
	fid_Rect_y1 = get_field(&err, env, "y1", "F");
	mid_Rect_init = get_method(&err, env, "<init>", "(FFFF)V");

	cls_Shade = get_class(&err, env, PKG"Shade");
	fid_Shade_pointer = get_field(&err, env, "pointer", "J");
	mid_Shade_init = get_method(&err, env, "<init>", "(J)V");

	cls_StrokeState = get_class(&err, env, PKG"StrokeState");
	fid_StrokeState_pointer = get_field(&err, env, "pointer", "J");
	mid_StrokeState_init = get_method(&err, env, "<init>", "(J)V");

	cls_StructuredText = get_class(&err, env, PKG"StructuredText");
	fid_StructuredText_pointer = get_field(&err, env, "pointer", "J");
	mid_StructuredText_init = get_method(&err, env, "<init>", "(J)V");

	cls_Text = get_class(&err, env, PKG"Text");
	fid_Text_pointer = get_field(&err, env, "pointer", "J");
	mid_Text_init = get_method(&err, env, "<init>", "(J)V");

	cls_TextBlock = get_class(&err, env, "com/artifex/mupdf/fitz/StructuredText$TextBlock");
	mid_TextBlock_init = get_method(&err, env, "<init>", "(Lcom/artifex/mupdf/fitz/StructuredText;)V");
	fid_TextBlock_bbox = get_field(&err, env, "bbox", "Lcom/artifex/mupdf/fitz/Rect;");
	fid_TextBlock_lines = get_field(&err, env, "lines", "[Lcom/artifex/mupdf/fitz/StructuredText$TextLine;");

	cls_TextChar = get_class(&err, env, "com/artifex/mupdf/fitz/StructuredText$TextChar");
	mid_TextChar_init = get_method(&err, env, "<init>", "(Lcom/artifex/mupdf/fitz/StructuredText;)V");
	fid_TextChar_bbox = get_field(&err, env, "bbox", "Lcom/artifex/mupdf/fitz/Rect;");
	fid_TextChar_c = get_field(&err, env, "c", "I");

	cls_TextLine = get_class(&err, env, "com/artifex/mupdf/fitz/StructuredText$TextLine");
	mid_TextLine_init = get_method(&err, env, "<init>", "(Lcom/artifex/mupdf/fitz/StructuredText;)V");
	fid_TextLine_bbox = get_field(&err, env, "bbox", "Lcom/artifex/mupdf/fitz/Rect;");
	fid_TextLine_chars = get_field(&err, env, "chars", "[Lcom/artifex/mupdf/fitz/StructuredText$TextChar;");

	cls_TextWalker = get_class(&err, env, PKG"TextWalker");
	mid_TextWalker_showGlyph = get_method(&err, env, "showGlyph", "(L"PKG"Font;L"PKG"Matrix;IIZ)V");

	cls_TryLaterException = get_class(&err, env, PKG"TryLaterException");

	/* Standard Java classes */

	cls_FloatArray = get_class(&err, env, "[F");
	cls_IntegerArray = get_class(&err, env, "[I");

	cls_Object = get_class(&err, env, "java/lang/Object");
	mid_Object_toString = get_method(&err, env, "toString", "()Ljava/lang/String;");

	cls_IndexOutOfBoundsException = get_class(&err, env, "java/lang/IndexOutOfBoundsException");
	cls_IllegalArgumentException = get_class(&err, env, "java/lang/IllegalArgumentException");
	cls_IOException = get_class(&err, env, "java/io/IOException");
	cls_NullPointerException = get_class(&err, env, "java/lang/NullPointerException");
	cls_RuntimeException = get_class(&err, env, "java/lang/RuntimeException");

	cls_OutOfMemoryError = get_class(&err, env, "java/lang/OutOfMemoryError");

	return err;
}

static inline jobject to_Rect_safe(fz_context *ctx, JNIEnv *env, const fz_rect *rect)
{
	if (!ctx || !rect) return NULL;

	return (*env)->NewObject(env, cls_Rect, mid_Rect_init, rect->x0, rect->y0, rect->x1, rect->y1);
}


JNIEXPORT jint JNICALL
FUN(StructuredText_initNative)(JNIEnv *env, jobject self)
{
	return find_fids(env);
}
JNIEXPORT jobject JNICALL
FUN(StructuredText_getBlocks)(JNIEnv *env, jobject self, jlong dochandle, jlong pagehandle)
{

	//find_fids(env);

	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	fz_context* ctx = doc->ctx;

	renderpage_t *page_t = (renderpage_t*) (long) pagehandle;

	fz_stext_page *page = NULL;

	fz_try(ctx)
		{

			page = fz_new_stext_page_from_page(ctx, page_t->page, NULL);
		}
	fz_catch(ctx)
		{
			//jni_rethrow(env, ctx);
			return NULL;
		}


	jobject barr = NULL;
	jobject larr = NULL;
	jobject carr = NULL;
	jobject jrect = NULL;

	int len;
	int b;
	int l;
	int c;

	fz_stext_block *block = NULL;
	fz_stext_line *line = NULL;
	fz_stext_char *ch = NULL;

	jobject jblock = NULL;
	jobject jline = NULL;
	jobject jchar = NULL;

	if (!ctx || !page) return NULL;

	len = 0;
	for (block = page->first_block; block; block = block->next)
		if (block->type == FZ_STEXT_BLOCK_TEXT)
			++len;

	//  create block array
	barr = (*env)->NewObjectArray(env, len, cls_TextBlock, NULL);
	if (!barr) return NULL;

	for (b=0, block = page->first_block; block; ++b, block = block->next)
	{
		//  only do text blocks
		if (block->type != FZ_STEXT_BLOCK_TEXT)
			continue;

		//  make a block
		jblock = (*env)->NewObject(env, cls_TextBlock, mid_TextBlock_init, NULL);
		if (!jblock) return NULL;

		//  set block's bbox
		jrect = to_Rect_safe(ctx, env, &(block->bbox));
		if (!jrect) return NULL;

		(*env)->SetObjectField(env, jblock, fid_TextBlock_bbox, jrect);
		(*env)->DeleteLocalRef(env, jrect);

		//  create block's line array
		len = 0;
		for (line = block->u.t.first_line; line; line = line->next)
			++len;

		larr = (*env)->NewObjectArray(env, len, cls_TextLine, NULL);
		if (!larr) return NULL;

		for (l=0, line = block->u.t.first_line; line; ++l, line = line->next)
		{
			//  make a line
			jline = (*env)->NewObject(env, cls_TextLine, mid_TextLine_init, NULL);
			if (!jline) return NULL;

			//  set line's bbox
			jrect = to_Rect_safe(ctx, env, &(line->bbox));
			if (!jrect) return NULL;

			(*env)->SetObjectField(env, jline, fid_TextLine_bbox, jrect);
			(*env)->DeleteLocalRef(env, jrect);

			//  count the chars
			len = 0;
			for (ch = line->first_char; ch; ch = ch->next)
				len++;

			//  make a char array
			carr = (*env)->NewObjectArray(env, len, cls_TextChar, NULL);
			if (!carr) return NULL;

			for (c=0, ch = line->first_char; ch; ++c, ch = ch->next)
			{
				//  create a char
				jchar = (*env)->NewObject(env, cls_TextChar, mid_TextChar_init, NULL);
				if (!jchar) return NULL;

				//  set the char's bbox
				jrect = to_Rect_safe(ctx, env, &(ch->bbox));
				if (!jrect) return NULL;

				(*env)->SetObjectField(env, jchar, fid_TextChar_bbox, jrect);
				(*env)->DeleteLocalRef(env, jrect);

				//  set the char's value
				(*env)->SetIntField(env, jchar, fid_TextChar_c, ch->c);

				//  add it to the char array
				(*env)->SetObjectArrayElement(env, carr, c, jchar);
				if ((*env)->ExceptionCheck(env)) return NULL;

				(*env)->DeleteLocalRef(env, jchar);
			}

			//  set the line's char array
			(*env)->SetObjectField(env, jline, fid_TextLine_chars, carr);
			(*env)->DeleteLocalRef(env, carr);//1

			//  add to the line array
			(*env)->SetObjectArrayElement(env, larr, l, jline);
			if ((*env)->ExceptionCheck(env)) return NULL;

			(*env)->DeleteLocalRef(env, jline);
		}

		//  set the block's line array
		(*env)->SetObjectField(env, jblock, fid_TextBlock_lines, larr);
		(*env)->DeleteLocalRef(env, larr);

		//  add to the block array
		(*env)->SetObjectArrayElement(env, barr, b, jblock);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, jblock);
	}

	return barr;
}


JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getCharCount(JNIEnv * env, jobject thiz,  jlong dochandle, jlong pagehandle)
{

		renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
		fz_context* ctx = doc->ctx;

		renderpage_t *page_t = (renderpage_t*) (long) pagehandle;

		fz_stext_page *page = NULL;

		fz_try(ctx)
			{

				page = fz_new_stext_page_from_page(ctx, page_t->page, NULL);
			}
		fz_catch(ctx)
			{

				return 0;
			}


			fz_stext_block *block;
			fz_stext_line *line;
			fz_stext_char *ch;
			int len = 0;

			for (block = page->first_block; block; block = block->next)
			{
				if (block->type != FZ_STEXT_BLOCK_TEXT)
					continue;
				for (line = block->u.t.first_line; line; line = line->next)
				{
					for (ch = line->first_char; ch; ch = ch->next)
						if(ch->c != ' ')
							++len;
				}
			}

			//fz_drop_stext_page(page_t->page, page);


		return len;

}


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
	return 112;
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
	char info[256];
	
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

	//char linkbuf[1024];
	//snprintf(linkbuf, 1023, "%s", link->dest.ld.uri.uri);

	//return (*env)->NewStringUTF(env, linkbuf);
	char linkbuf[1024];
	snprintf(linkbuf, 1023, "%s", link->uri);

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

	//if (!link || link->dest.kind != FZ_LINK_GOTO) {
	//	return (jint) 0;
	//}

	//char linkbuf[1024];
	//snprintf(linkbuf, 1023, "%s", link->uri);


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
JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageCount(JNIEnv *env,
		jclass clazz, jlong handle, jint width, jint height, jint size) {
	renderdocument_t *doc = (renderdocument_t*) (long) handle;
	fz_try(doc->ctx)
	{
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
		mupdf_throw_exception(env, "Context cloning failed");
		return (jlong) (long) NULL;
	}

	page = fz_malloc_no_throw(ctx, sizeof(renderpage_t));
	DEBUG("MuPdfPage_open(%p, %d): page=%p", doc, pageno, page);

	if (!page) {
		mupdf_throw_exception(env, "Out of Memory");
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
	fz_separations *seps = NULL;
	fz_var(seps);

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
		//seps = fz_page_separations(ctx, page);
		seps = fz_new_separations(ctx, 0);
		pixmap = fz_new_pixmap_with_data(ctx, colorspace,
				viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0,NULL,1,stride,
				(unsigned char*) buffer);

		//fz_invert_pixmap(ctx,pixmap);
		fz_clear_pixmap_with_value(ctx, pixmap, 0xFF);
		if(r!=-1 && g!=-1 && b!=-1){
			fz_tint_pixmap(ctx,pixmap,r,g,b);
		}
		//fz_clear_pixmap(ctx, pixmap);

		dev = fz_new_draw_device(ctx, NULL, pixmap);

		fz_run_display_list(ctx, page->pageList, dev, &ctm, &viewbox, NULL);

//		fz_annot *annot;
//		page->annot_list = fz_new_display_list(ctx);
//		dev = fz_new_list_device(ctx, page->annot_list);
//		for (annot = fz_first_annot(doc->document, page->page); annot; annot = fz_next_annot(doc->document, annot))
//			fz_run_annot(doc->document, page->page, annot, dev, &fz_identity, NULL);


		fz_drop_pixmap(ctx, pixmap);
	}
	fz_always(ctx)
	{
		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
		fz_drop_separations(ctx, seps);
	}
	fz_catch(ctx)
	{
		DEBUG("Render failed");
	}

	(*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);
}

/*JNI BITMAP API*/
JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_renderPageBitmap(JNIEnv *env,
		jobject this, jlong dochandle, jlong pagehandle, jintArray viewboxarray,
		jfloatArray matrixarray, jobject bitmap) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	renderpage_t *page = (renderpage_t*) (long) pagehandle;

	DEBUG("MuPdfPage_renderPageBitmap(%p, %p): start", doc, page);

	fz_matrix ctm;
	fz_rect viewbox;
	fz_pixmap *pixmap;
	jfloat *matrix;
	jint *viewboxarr;
	jint *dimen;
	int length, val;
	fz_device *dev = NULL;
	fz_separations *seps = NULL;

	fz_var(seps);

	AndroidBitmapInfo info;
	void *pixels;

	int ret;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		ERROR("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return JNI_FALSE;
	}

// DEBUG("Checking format\n");
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		ERROR("Bitmap format is not RGBA_8888 !");
		return JNI_FALSE;
	}

// DEBUG("locking pixels\n");
	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
		ERROR("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return JNI_FALSE;
	}

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

	fz_context* ctx = page->ctx;
	if (!ctx) {
		ERROR("No page context");
		return JNI_FALSE;
	}
	fz_try(ctx)
	{
		fz_colorspace *colorspace = fz_device_bgr(ctx);
		int stride = (fz_colorspace_n(ctx, colorspace) + 1) * (viewbox.x1 - viewbox.x0);
		//seps = fz_page_separations(ctx, page);
		seps = fz_new_separations(ctx, 0);
		pixmap = fz_new_pixmap_with_data(ctx,colorspace,
				viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0,NULL,1,stride,pixels);

		fz_clear_pixmap_with_value(ctx, pixmap,0xff);

		dev = fz_new_draw_device(ctx, NULL, pixmap);

		fz_run_display_list(ctx, page->pageList, dev, &ctm, &viewbox, NULL);
	}

//	fz_annot *annot;
//	page->annot_list = fz_new_display_list(ctx);
//	dev = fz_new_list_device(ctx, page->annot_list);
//	for (annot = fz_first_annot(doc->document, page->page); annot; annot = fz_next_annot(doc->document, annot))
//		fz_run_annot(doc->document, page->page, annot, dev, &fz_identity, NULL);


	fz_always(ctx)
	{
		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
		fz_drop_pixmap(ctx, pixmap);
		fz_drop_separations(ctx, seps);
	}
	fz_catch(ctx)
	{
		DEBUG("Render failed");
	}

	AndroidBitmap_unlockPixels(env, bitmap);

	DEBUG("MuPdfPage_renderPageBitmap(%p, %p): finish", doc, page);

	return JNI_TRUE;
}

//Outline
JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_open(JNIEnv *env,
		jclass clazz, jlong dochandle) {
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	if (!doc->outline){
		fz_context *ctx = doc->ctx;
		//doc->outline = fz_load_outline(ctx, doc->document);

		fz_try(ctx)
			doc->outline = fz_load_outline(ctx, doc->document);
		fz_catch(ctx)
			doc->outline = NULL;

	}
//    DEBUG("PdfOutline.open(): return handle = %p", doc->outline);
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
		jclass clazz, jlong outlinehandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("PdfOutline_getTitle(%p)",outline);
	if (outline)
		return (*env)->NewStringUTF(env, outline->title);
	return NULL;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getLink(JNIEnv *env,
		jclass clazz, jlong outlinehandle, jlong dochandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

// DEBUG("PdfOutline_getLink(%p)",outline);
	if (!outline)
		return NULL;

	char linkbuf[128];
	int pageNo = outline->page;

	snprintf(linkbuf, 127, "#%d", pageNo + 1);

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
	return (jlong) (long) (outline ? outline->next : -1);
	//jlong res = -1;
	//return res;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getChild(JNIEnv *env,
		jclass clazz, jlong outlinehandle) {
	fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("MuPdfOutline_getChild(%p)",outline);
	return (jlong) (long) (outline ? outline->down : -1);
	//jlong res = -1;
	//return res;
}

///////////////
//SEARCH
//////////////
static int
charat(fz_context *ctx, fz_stext_page *page, int idx)
{
	
	return fz_stext_char_at(ctx, page, idx)->c;
}

static const fz_rect
bboxcharat(fz_context *ctx, fz_stext_page *page, int idx)
{
	
	return fz_stext_char_at(ctx, page, idx)->bbox;
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




JNIEXPORT jobject JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_to_StructuredText(JNIEnv *env, jobject self, jlong dochandle, jlong pagehandle, jstring joptions)
{
	


	renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
	fz_context* ctx = doc->ctx;
    renderpage_t *page_t = (renderpage_t*) (long) pagehandle;
	
	fz_page *page = page_t->page;
	fz_stext_page *text = NULL;
	const char *options= NULL;
	fz_stext_options opts;

	if (!ctx || !page) return NULL;

	if (joptions)
	{
		options = (*env)->GetStringUTFChars(env, joptions, NULL);
		if (!options) return NULL;
	}

	fz_try(ctx)
	{
		fz_parse_stext_options(ctx, &opts, options);
		text = fz_new_stext_page_from_page(ctx, page, &opts);
	}
	fz_always(ctx)
	{
		if (options)
			(*env)->ReleaseStringUTFChars(env, joptions, options);
	}
	fz_catch(ctx)
	{
		//jni_rethrow(env, ctx);
		return NULL;
	}

	return NULL;
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
				pagetext = fz_new_stext_page(doc->ctx, fz_bound_page(ctx, page->page, &mediabox));
				dev = fz_new_stext_device(doc->ctx, pagetext, NULL);
				fz_run_page(ctx, page->page, dev, &fz_identity, NULL);

				// DEBUG("MuPdfPage(%p).search(%p, %p): free text device", thiz, doc, page);

				fz_close_device(ctx, dev);
				fz_drop_device(ctx, dev);
				dev = NULL;

				//len = fz_stext_char_count(ctx, pagetext);
				//len = pagetext->len;

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
		//const char *content = pdf_annot_contents(ctx, (pdf_annot *)annot);
		jstring text  = (*env)->NewStringUTF(env, NULL);

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
fz_print_stext_page_as_text_my1(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	fz_stext_block *block;
	fz_stext_line *line;
	fz_stext_char *ch;
	char utf[10];
	int i, n;

	for (block = page->first_block; block; block = block->next)
	{

		if (block->type == FZ_STEXT_BLOCK_IMAGE){
	fz_write_printf(ctx, out, "<image-begin>");
	fz_write_image_as_data_uri(ctx, out, block->u.i.image);
	fz_write_string(ctx, out, "<image-end>");
	fz_write_printf(ctx, out, "<br/>");
		}else if (block->type == FZ_STEXT_BLOCK_TEXT)
		{
			fz_write_printf(ctx, out, "<p>");

					fz_font *font1 = block->u.t.first_line->first_char->font;
					fz_font *font2 = block->u.t.first_line->last_char->font;
					int is_bold = fz_font_is_bold(ctx, font1) && fz_font_is_bold(ctx, font2);
					int is_italic = fz_font_is_italic(ctx, font1) && fz_font_is_italic(ctx, font2);
					int is_mono = fz_font_is_monospaced(ctx, font1) && fz_font_is_monospaced(ctx, font2);

					if (is_bold) fz_write_printf(ctx,out,"<b>");
					if (is_italic) fz_write_printf(ctx,out,"<i>");
					if (is_mono) fz_write_printf(ctx,out,"<tt>");

			
			for (line = block->u.t.first_line; line; line = line->next)
			{
				    
				for (ch = line->first_char; ch; ch = ch->next)
				{
					
					int is_bold_ch = !is_bold && fz_font_is_bold(ctx, ch->font);
					int is_italic_ch = !is_italic && fz_font_is_italic(ctx, ch->font);
					int is_mono_ch = !is_mono && fz_font_is_monospaced(ctx, ch->font);


					if (is_bold_ch) fz_write_printf(ctx,out,"<b>");
					if (is_italic_ch) fz_write_printf(ctx,out,"<i>");
					if (is_mono_ch) fz_write_printf(ctx,out,"<tt>");

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
					if (is_mono_ch) fz_write_printf(ctx,out,"</tt>");

				}
				fz_write_string(ctx, out, "<end-line>");
			}
			
			if (is_bold) fz_write_printf(ctx,out,"</b>");
			if (is_italic) fz_write_printf(ctx,out,"</i>");
			if (is_mono) fz_write_printf(ctx,out,"</tt>");
			
			fz_write_printf(ctx,out,"</p>");
			//fz_write_string(ctx, out, "\n");
			//fz_write_printf(ctx, out, "<br/>");
			
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

	fz_stext_page *text = NULL;
	fz_device *dev = NULL;
	fz_matrix ctm;

	jbyteArray bArray = NULL;
	fz_buffer *buf = NULL;
	fz_output *out = NULL;

	size_t len;

	
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
		
		text = fz_new_stext_page(ctx, fz_bound_page(ctx, page->page, &mediabox));
		int j = (int)opts;
		if(j == -1){
			dev = fz_new_stext_device(ctx, text, NULL);
		}else{
			fz_stext_options stext_options;
			stext_options.flags = j;
			dev = fz_new_stext_device(ctx, text, &stext_options);

		}

		//dev = fz_new_stext_device(ctx, text, NULL);
		fz_run_page(ctx, page->page, dev, &ctm, NULL);

		fz_close_device(ctx, dev);
		fz_drop_device(ctx, dev);
		dev = NULL;


		buf = fz_new_buffer(ctx, 256);
		out = fz_new_output_with_buffer(ctx, buf);

		fz_print_stext_page_as_text_my1(ctx, out, text);

		fz_drop_output(ctx, out);
		out = NULL;

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
		fz_close_device(ctx, dev);
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





