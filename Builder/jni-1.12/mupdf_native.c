#include <jni.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <pthread.h>
#endif

#include "mupdf/fitz.h"
#include "mupdf/ucdn.h"
#include "mupdf/pdf.h"

#include "mupdf_native.h" /* javah generated prototypes */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef HAVE_ANDROID
#include <android/log.h>
#include <android/bitmap.h>
#define LOG_TAG "libmupdf"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGT(...) __android_log_print(ANDROID_LOG_INFO,"alert",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#undef LOGI
#undef LOGE
#define LOGI(...) do{printf(__VA_ARGS__);putchar('\n');}while(0)
#define LOGE(...) do{printf(__VA_ARGS__);putchar('\n');}while(0)
#endif

#define MY_JNI_VERSION JNI_VERSION_1_6

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

#ifdef _WIN32
static DWORD context_key;
#else
static pthread_key_t context_key;
#endif
static fz_context *base_context;

static int check_enums()
{
	int valid = 1;

	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_TEXT == PDF_ANNOT_TEXT;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_LINK == PDF_ANNOT_LINK;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_FREE_TEXT == PDF_ANNOT_FREE_TEXT;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_LINE == PDF_ANNOT_LINE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_SQUARE == PDF_ANNOT_SQUARE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_CIRCLE == PDF_ANNOT_CIRCLE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_POLYGON == PDF_ANNOT_POLYGON;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_POLY_LINE == PDF_ANNOT_POLY_LINE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_HIGHLIGHT == PDF_ANNOT_HIGHLIGHT;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_UNDERLINE == PDF_ANNOT_UNDERLINE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_SQUIGGLY == PDF_ANNOT_SQUIGGLY;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_STRIKE_OUT == PDF_ANNOT_STRIKE_OUT;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_STAMP == PDF_ANNOT_STAMP;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_CARET == PDF_ANNOT_CARET;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_INK == PDF_ANNOT_INK;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_POPUP == PDF_ANNOT_POPUP;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_FILE_ATTACHMENT == PDF_ANNOT_FILE_ATTACHMENT;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_SOUND == PDF_ANNOT_SOUND;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_MOVIE == PDF_ANNOT_MOVIE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_WIDGET == PDF_ANNOT_WIDGET;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_SCREEN == PDF_ANNOT_SCREEN;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_PRINTER_MARK == PDF_ANNOT_PRINTER_MARK;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_TRAP_NET == PDF_ANNOT_TRAP_NET;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_WATERMARK == PDF_ANNOT_WATERMARK;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_3D == PDF_ANNOT_3D;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_TYPE_UNKNOWN == PDF_ANNOT_UNKNOWN;

	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_NONE == PDF_ANNOT_LINE_ENDING_NONE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_SQUARE == PDF_ANNOT_LINE_ENDING_SQUARE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_CIRCLE == PDF_ANNOT_LINE_ENDING_CIRCLE;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_DIAMOND == PDF_ANNOT_LINE_ENDING_DIAMOND;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_OPENARROW == PDF_ANNOT_LINE_ENDING_OPENARROW;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_CLOSEDARROW == PDF_ANNOT_LINE_ENDING_CLOSEDARROW;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_BUTT == PDF_ANNOT_LINE_ENDING_BUTT;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_ROPENARR == PDF_ANNOT_LINE_ENDING_ROPENARROW;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_RCLOSEDARROW == PDF_ANNOT_LINE_ENDING_RCLOSEDARROW;
	valid &= com_artifex_mupdf_fitz_PDFAnnotation_LINE_ENDING_SLASH == PDF_ANNOT_LINE_ENDING_SLASH;

	return valid ? 1 : 0;
}

/* Helper functions to set the java exception flag. */

static void jni_throw(JNIEnv *env, int type, const char *mess)
{
	if (type == FZ_ERROR_TRYLATER)
		(*env)->ThrowNew(env, cls_TryLaterException, mess);
	else
		(*env)->ThrowNew(env, cls_RuntimeException, mess);
}

static void jni_rethrow(JNIEnv *env, fz_context *ctx)
{
	jni_throw(env, fz_caught(ctx), fz_caught_message(ctx));
}

static void jni_throw_oom(JNIEnv *env, const char *info)
{
	(*env)->ThrowNew(env, cls_OutOfMemoryError, info);
}

static void jni_throw_oob(JNIEnv *env, const char *info)
{
	(*env)->ThrowNew(env, cls_IndexOutOfBoundsException, info);
}

static void jni_throw_arg(JNIEnv *env, const char *info)
{
	(*env)->ThrowNew(env, cls_IllegalArgumentException, info);
}

static void jni_throw_io(JNIEnv *env, const char *info)
{
	(*env)->ThrowNew(env, cls_IOException, info);
}

static void jni_throw_null(JNIEnv *env, const char *info)
{
	(*env)->ThrowNew(env, cls_NullPointerException, info);
}

/* Convert a java exception and throw into fitz. */

static void fz_throw_java(fz_context *ctx, JNIEnv *env)
{
	jthrowable ex = (*env)->ExceptionOccurred(env);
	if (ex)
	{
		jobject msg;
		(*env)->ExceptionClear(env);
		msg = (*env)->CallObjectMethod(env, ex, mid_Object_toString);
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionClear(env);
		else if (msg)
		{
			const char *p = (*env)->GetStringUTFChars(env, msg, NULL);
			if (p)
			{
				char buf[256];
				fz_strlcpy(buf, p, sizeof buf);
				(*env)->ReleaseStringUTFChars(env, msg, p);
				fz_throw(ctx, FZ_ERROR_GENERIC, "%s", buf);
			}
		}
	}
	fz_throw(ctx, FZ_ERROR_GENERIC, "unknown java error");
}

/* Load classes, field and method IDs. */

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

	cls_ColorSpace = get_class(&err, env, PKG"ColorSpace");
	fid_ColorSpace_pointer = get_field(&err, env, "pointer", "J");
	mid_ColorSpace_init = get_method(&err, env, "<init>", "(J)V");
	mid_ColorSpace_fromPointer = get_static_method(&err, env, "fromPointer", "(J)L"PKG"ColorSpace;");

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

	cls_PDFObject = get_class(&err, env, PKG"PDFObject");
	fid_PDFObject_pointer = get_field(&err, env, "pointer", "J");
	fid_PDFObject_Null = get_static_field(&err, env, "Null", "L"PKG"PDFObject;");
	mid_PDFObject_init = get_method(&err, env, "<init>", "(J)V");

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


	cls_TextBlock = get_class(&err, env, PKG"StructuredText$TextBlock");
	mid_TextBlock_init = get_method(&err, env, "<init>", "(Lcom/artifex/mupdf/fitz/StructuredText;)V");
	fid_TextBlock_bbox = get_field(&err, env, "bbox", "L"PKG"Rect;");
	fid_TextBlock_lines = get_field(&err, env, "lines", "[L"PKG"StructuredText$TextLine;");

	cls_TextChar = get_class(&err, env, PKG"StructuredText$TextChar");
	mid_TextChar_init = get_method(&err, env, "<init>", "(Lcom/artifex/mupdf/fitz/StructuredText;)V");
	fid_TextChar_bbox = get_field(&err, env, "bbox", "L"PKG"Rect;");
	fid_TextChar_c = get_field(&err, env, "c", "I");

	cls_TextLine = get_class(&err, env, PKG"StructuredText$TextLine");
	mid_TextLine_init = get_method(&err, env, "<init>", "(Lcom/artifex/mupdf/fitz/StructuredText;)V");
	fid_TextLine_bbox = get_field(&err, env, "bbox", "L"PKG"Rect;");
	fid_TextLine_chars = get_field(&err, env, "chars", "[L"PKG"StructuredText$TextChar;");

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

static void lose_fids(JNIEnv *env)
{
	(*env)->DeleteGlobalRef(env, cls_Annotation);
	(*env)->DeleteGlobalRef(env, cls_Buffer);
	(*env)->DeleteGlobalRef(env, cls_ColorSpace);
	(*env)->DeleteGlobalRef(env, cls_Cookie);
	(*env)->DeleteGlobalRef(env, cls_Device);
	(*env)->DeleteGlobalRef(env, cls_DisplayList);
	(*env)->DeleteGlobalRef(env, cls_Document);
	(*env)->DeleteGlobalRef(env, cls_DocumentWriter);
	(*env)->DeleteGlobalRef(env, cls_FloatArray);
	(*env)->DeleteGlobalRef(env, cls_Font);
	(*env)->DeleteGlobalRef(env, cls_IllegalArgumentException);
	(*env)->DeleteGlobalRef(env, cls_Image);
	(*env)->DeleteGlobalRef(env, cls_IndexOutOfBoundsException);
	(*env)->DeleteGlobalRef(env, cls_IntegerArray);
	(*env)->DeleteGlobalRef(env, cls_IOException);
	(*env)->DeleteGlobalRef(env, cls_Link);
	(*env)->DeleteGlobalRef(env, cls_Matrix);
	(*env)->DeleteGlobalRef(env, cls_NativeDevice);
	(*env)->DeleteGlobalRef(env, cls_NullPointerException);
	(*env)->DeleteGlobalRef(env, cls_Object);
	(*env)->DeleteGlobalRef(env, cls_Outline);
	(*env)->DeleteGlobalRef(env, cls_OutOfMemoryError);
	(*env)->DeleteGlobalRef(env, cls_Page);
	(*env)->DeleteGlobalRef(env, cls_Path);
	(*env)->DeleteGlobalRef(env, cls_PathWalker);
	(*env)->DeleteGlobalRef(env, cls_PDFAnnotation);
	(*env)->DeleteGlobalRef(env, cls_PDFDocument);
	(*env)->DeleteGlobalRef(env, cls_PDFPage);
	(*env)->DeleteGlobalRef(env, cls_PDFGraftMap);
	(*env)->DeleteGlobalRef(env, cls_PDFObject);
	(*env)->DeleteGlobalRef(env, cls_Pixmap);
	(*env)->DeleteGlobalRef(env, cls_Point);
	(*env)->DeleteGlobalRef(env, cls_Rect);
	(*env)->DeleteGlobalRef(env, cls_RuntimeException);
	(*env)->DeleteGlobalRef(env, cls_Shade);
	(*env)->DeleteGlobalRef(env, cls_StrokeState);
	(*env)->DeleteGlobalRef(env, cls_StructuredText);
	(*env)->DeleteGlobalRef(env, cls_Text);
	(*env)->DeleteGlobalRef(env, cls_TextBlock);
	(*env)->DeleteGlobalRef(env, cls_TextChar);
	(*env)->DeleteGlobalRef(env, cls_TextLine);
	(*env)->DeleteGlobalRef(env, cls_TextWalker);
	(*env)->DeleteGlobalRef(env, cls_TryLaterException);
}

#ifdef HAVE_ANDROID

static fz_font *load_noto(fz_context *ctx, const char *filename, int idx)
{
	fz_font *font;
	fz_try(ctx)
		font = fz_new_font_from_file(ctx, NULL, filename, idx, 0);
	fz_catch(ctx)
		return NULL;
	return font;
}

static fz_font *load_noto_cjk(fz_context *ctx, int lang)
{
	fz_font *font = load_noto(ctx, "/system/fonts/NotoSansCJK-Regular.ttc", lang);
	if (!font)
		font = load_noto(ctx, "/system/fonts/DroidSansFallback.ttf", 0);
	return font;
}

static fz_font *load_noto3(fz_context *ctx, const char *a, const char *b, const char *c)
{
	fz_font *font = load_noto(ctx, a, 0);
	if (!font && b) font = load_noto(ctx, b, 0);
	if (!font && c) font = load_noto(ctx, c, 0);
	return font;
}

enum { JP, KR, SC, TC };

#define NOTO3(NAME1, NAME2, NAME3) load_noto3(ctx, "/system/fonts/" NAME1 "-Regular.ttf", "/system/fonts/" NAME2 "-Regular.ttf", "/system/fonts/" NAME3 "-Regular.ttf")
#define NOTO2(NAME1, NAME2) load_noto3(ctx, "/system/fonts/" NAME1 "-Regular.ttf", "/system/fonts/" NAME2 "-Regular.ttf", NULL)
#define NOTO(NAME) load_noto3(ctx, "/system/fonts/" NAME "-Regular.ttf", NULL, NULL)

fz_font *load_droid_fallback_font(fz_context *ctx, int script, int language, int serif, int bold, int italic)
{
	switch (script)
	{
	default:
	case UCDN_SCRIPT_COMMON:
	case UCDN_SCRIPT_INHERITED:
	case UCDN_SCRIPT_UNKNOWN:
		return NULL;

	case UCDN_SCRIPT_HANGUL: return load_noto_cjk(ctx, KR);
	case UCDN_SCRIPT_HIRAGANA: return load_noto_cjk(ctx, JP);
	case UCDN_SCRIPT_KATAKANA: return load_noto_cjk(ctx, JP);
	case UCDN_SCRIPT_BOPOMOFO: return load_noto_cjk(ctx, SC);
	case UCDN_SCRIPT_HAN:
		switch (language) {
		case FZ_LANG_ja: return load_noto_cjk(ctx, JP);
		case FZ_LANG_ko: return load_noto_cjk(ctx, KR);
		case FZ_LANG_zh_Hant: return load_noto_cjk(ctx, TC);
		default:
		case FZ_LANG_zh_Hans: return load_noto_cjk(ctx, SC);
		}

	case UCDN_SCRIPT_LATIN: return NOTO2("NotoSans", "DroidSans");
	case UCDN_SCRIPT_GREEK: return NOTO2("NotoSans", "DroidSans");
	case UCDN_SCRIPT_CYRILLIC: return NOTO2("NotoSans", "DroidSans");
	case UCDN_SCRIPT_ARABIC: return NOTO3("NotoNaskh", "NotoNaskhArabic", "DroidNaskh");

	case UCDN_SCRIPT_ARMENIAN: return NOTO2("NotoSansArmenian", "DroidSansArmenian");
	case UCDN_SCRIPT_BALINESE: return NOTO("NotoSansBalinese");
	case UCDN_SCRIPT_BAMUM: return NOTO("NotoSansBamum");
	case UCDN_SCRIPT_BATAK: return NOTO("NotoSansBatak");
	case UCDN_SCRIPT_BENGALI: return NOTO("NotoSansBengali");
	case UCDN_SCRIPT_CANADIAN_ABORIGINAL: return NOTO("NotoSansCanadianAboriginal");
	case UCDN_SCRIPT_CHAM: return NOTO("NotoSansCham");
	case UCDN_SCRIPT_CHEROKEE: return NOTO("NotoSansCherokee");
	case UCDN_SCRIPT_DEVANAGARI: return NOTO2("NotoSansDevanagari", "DroidSansDevanagari");
	case UCDN_SCRIPT_ETHIOPIC: return NOTO2("NotoSansEthiopic", "DroidSansEthiopic");
	case UCDN_SCRIPT_GEORGIAN: return NOTO2("NotoSansGeorgian", "DroidSansGeorgian");
	case UCDN_SCRIPT_GUJARATI: return NOTO("NotoSansGujarati");
	case UCDN_SCRIPT_GURMUKHI: return NOTO("NotoSansGurmukhi");
	case UCDN_SCRIPT_HEBREW: return NOTO2("NotoSansHebrew", "DroidSansHebrew");
	case UCDN_SCRIPT_JAVANESE: return NOTO("NotoSansJavanese");
	case UCDN_SCRIPT_KANNADA: return NOTO("NotoSansKannada");
	case UCDN_SCRIPT_KAYAH_LI: return NOTO("NotoSansKayahLi");
	case UCDN_SCRIPT_KHMER: return NOTO("NotoSansKhmer");
	case UCDN_SCRIPT_LAO: return NOTO("NotoSansLao");
	case UCDN_SCRIPT_LEPCHA: return NOTO("NotoSansLepcha");
	case UCDN_SCRIPT_LIMBU: return NOTO("NotoSansLimbu");
	case UCDN_SCRIPT_LISU: return NOTO("NotoSansLisu");
	case UCDN_SCRIPT_MALAYALAM: return NOTO("NotoSansMalayalam");
	case UCDN_SCRIPT_MANDAIC: return NOTO("NotoSansMandaic");
	case UCDN_SCRIPT_MEETEI_MAYEK: return NOTO("NotoSansMeeteiMayek");
	case UCDN_SCRIPT_MONGOLIAN: return NOTO("NotoSansMongolian");
	case UCDN_SCRIPT_MYANMAR: return NOTO("NotoSansMyanmar");
	case UCDN_SCRIPT_NEW_TAI_LUE: return NOTO("NotoSansNewTaiLue");
	case UCDN_SCRIPT_NKO: return NOTO("NotoSansNKo");
	case UCDN_SCRIPT_OL_CHIKI: return NOTO("NotoSansOlChiki");
	case UCDN_SCRIPT_ORIYA: return NOTO("NotoSansOriya");
	case UCDN_SCRIPT_SAURASHTRA: return NOTO("NotoSansSaurashtra");
	case UCDN_SCRIPT_SINHALA: return NOTO("NotoSansSinhala");
	case UCDN_SCRIPT_SUNDANESE: return NOTO("NotoSansSundanese");
	case UCDN_SCRIPT_SYLOTI_NAGRI: return NOTO("NotoSansSylotiNagri");
	case UCDN_SCRIPT_SYRIAC: return NOTO("NotoSansSyriacEastern");
	case UCDN_SCRIPT_TAI_LE: return NOTO("NotoSansTaiLe");
	case UCDN_SCRIPT_TAI_THAM: return NOTO("NotoSansTaiTham");
	case UCDN_SCRIPT_TAI_VIET: return NOTO("NotoSansTaiViet");
	case UCDN_SCRIPT_TAMIL: return NOTO2("NotoSansTamil", "DroidSansTamil");
	case UCDN_SCRIPT_TELUGU: return NOTO("NotoSansTelugu");
	case UCDN_SCRIPT_THAANA: return NOTO("NotoSansThaana");
	case UCDN_SCRIPT_THAI: return NOTO2("NotoSansThai", "DroidSansThai");
	case UCDN_SCRIPT_TIBETAN: return NOTO("NotoSansTibetan");
	case UCDN_SCRIPT_TIFINAGH: return NOTO("NotoSansTifinagh");
	case UCDN_SCRIPT_VAI: return NOTO("NotoSansVai");
	case UCDN_SCRIPT_YI: return NOTO("NotoSansYi");

	/* Historic */
	case UCDN_SCRIPT_AVESTAN: return NOTO("NotoSansAvestan");
	case UCDN_SCRIPT_BRAHMI: return NOTO("NotoSansBrahmi");
	case UCDN_SCRIPT_BUGINESE: return NOTO("NotoSansBuginese");
	case UCDN_SCRIPT_BUHID: return NOTO("NotoSansBuhid");
	case UCDN_SCRIPT_CARIAN: return NOTO("NotoSansCarian");
	case UCDN_SCRIPT_COPTIC: return NOTO("NotoSansCoptic");
	case UCDN_SCRIPT_CUNEIFORM: return NOTO("NotoSansCuneiform");
	case UCDN_SCRIPT_CYPRIOT: return NOTO("NotoSansCypriot");
	case UCDN_SCRIPT_DESERET: return NOTO("NotoSansDeseret");
	case UCDN_SCRIPT_EGYPTIAN_HIEROGLYPHS: return NOTO("NotoSansEgyptianHieroglyphs");
	case UCDN_SCRIPT_GLAGOLITIC: return NOTO("NotoSansGlagolitic");
	case UCDN_SCRIPT_GOTHIC: return NOTO("NotoSansGothic");
	case UCDN_SCRIPT_HANUNOO: return NOTO("NotoSansHanunoo");
	case UCDN_SCRIPT_IMPERIAL_ARAMAIC: return NOTO("NotoSansImperialAramaic");
	case UCDN_SCRIPT_INSCRIPTIONAL_PAHLAVI: return NOTO("NotoSansInscriptionalPahlavi");
	case UCDN_SCRIPT_INSCRIPTIONAL_PARTHIAN: return NOTO("NotoSansInscriptionalParthian");
	case UCDN_SCRIPT_KAITHI: return NOTO("NotoSansKaithi");
	case UCDN_SCRIPT_KHAROSHTHI: return NOTO("NotoSansKharoshthi");
	case UCDN_SCRIPT_LINEAR_B: return NOTO("NotoSansLinearB");
	case UCDN_SCRIPT_LYCIAN: return NOTO("NotoSansLycian");
	case UCDN_SCRIPT_LYDIAN: return NOTO("NotoSansLydian");
	case UCDN_SCRIPT_OGHAM: return NOTO("NotoSansOgham");
	case UCDN_SCRIPT_OLD_ITALIC: return NOTO("NotoSansOldItalic");
	case UCDN_SCRIPT_OLD_PERSIAN: return NOTO("NotoSansOldPersian");
	case UCDN_SCRIPT_OLD_SOUTH_ARABIAN: return NOTO("NotoSansOldSouthArabian");
	case UCDN_SCRIPT_OLD_TURKIC: return NOTO("NotoSansOldTurkic");
	case UCDN_SCRIPT_OSMANYA: return NOTO("NotoSansOsmanya");
	case UCDN_SCRIPT_PHAGS_PA: return NOTO("NotoSansPhagsPa");
	case UCDN_SCRIPT_PHOENICIAN: return NOTO("NotoSansPhoenician");
	case UCDN_SCRIPT_REJANG: return NOTO("NotoSansRejang");
	case UCDN_SCRIPT_RUNIC: return NOTO("NotoSansRunic");
	case UCDN_SCRIPT_SAMARITAN: return NOTO("NotoSansSamaritan");
	case UCDN_SCRIPT_SHAVIAN: return NOTO("NotoSansShavian");
	case UCDN_SCRIPT_TAGALOG: return NOTO("NotoSansTagalog");
	case UCDN_SCRIPT_TAGBANWA: return NOTO("NotoSansTagbanwa");
	case UCDN_SCRIPT_UGARITIC: return NOTO("NotoSansUgaritic");
	}
	return NULL;
}

fz_font *load_droid_cjk_font(fz_context *ctx, const char *name, int ros, int serif)
{
	switch (ros) {
	case FZ_ADOBE_CNS_1: return load_noto_cjk(ctx, TC);
	case FZ_ADOBE_GB_1: return load_noto_cjk(ctx, SC);
	case FZ_ADOBE_JAPAN_1: return load_noto_cjk(ctx, JP);
	case FZ_ADOBE_KOREA_1: return load_noto_cjk(ctx, KR);
	}
	return NULL;
}

fz_font *load_droid_font(fz_context *ctx, const char *name, int bold, int italic, int needs_exact_metrics)
{
	return NULL;
}

#endif

/* Put the fz_context in thread-local storage */

#ifdef _WIN32
static CRITICAL_SECTION mutexes[FZ_LOCK_MAX];
#else
static pthread_mutex_t mutexes[FZ_LOCK_MAX];
#endif

static void lock(void *user, int lock)
{
#ifdef _WIN32
	EnterCriticalSection(&mutexes[lock]);
#else
	(void)pthread_mutex_lock(&mutexes[lock]);
#endif
}

static void unlock(void *user, int lock)
{
#ifdef _WIN32
	LeaveCriticalSection(&mutexes[lock]);
#else
	(void)pthread_mutex_unlock(&mutexes[lock]);
#endif
}

static const fz_locks_context locks =
{
	NULL, /* user */
	lock,
	unlock
};

static void fin_base_context(JNIEnv *env)
{
	int i;

	for (i = 0; i < FZ_LOCK_MAX; i++)
#ifdef _WIN32
		DeleteCriticalSection(&mutexes[i]);
#else
		(void)pthread_mutex_destroy(&mutexes[i]);
#endif

	fz_drop_context(base_context);
	base_context = NULL;
}

#ifndef _WIN32
static void drop_tls_context(void *arg)
{
	fz_context *ctx = (fz_context *)arg;

	fz_drop_context(ctx);
}
#endif

static int init_base_context(JNIEnv *env)
{
	int i;

#ifdef _WIN32
	/* No destructor on windows. We will leak contexts.
	 * There is no easy way around this, but this page:
	 * http://stackoverflow.com/questions/3241732/is-there-anyway-to-dynamically-free-thread-local-storage-in-the-win32-apis/3245082#3245082
	 * suggests a workaround that we can implement if we
	 * need to. */
	context_key = TlsAlloc();
	if (context_key == TLS_OUT_OF_INDEXES)
		return -1;
#else
	pthread_key_create(&context_key, drop_tls_context);
#endif

	for (i = 0; i < FZ_LOCK_MAX; i++)
#ifdef _WIN32
		InitializeCriticalSection(&mutexes[i]);
#else
		(void)pthread_mutex_init(&mutexes[i], NULL);
#endif

	base_context = fz_new_context(NULL, &locks, FZ_STORE_DEFAULT);
	if (!base_context)
		return -1;

	fz_register_document_handlers(base_context);

#ifdef HAVE_ANDROID
	fz_install_load_system_font_funcs(base_context,
		load_droid_font,
		load_droid_cjk_font,
		load_droid_fallback_font);
#endif

	return 0;
}

static fz_context *get_context(JNIEnv *env)
{
	fz_context *ctx = (fz_context *)
#ifdef _WIN32
		TlsGetValue(context_key);
#else
		pthread_getspecific(context_key);
#endif

	if (ctx)
		return ctx;

	ctx = fz_clone_context(base_context);
	if (!ctx) { jni_throw_oom(env, "failed to clone fz_context"); return NULL; }

#ifdef _WIN32
	TlsSetValue(context_key, ctx);
#else
	pthread_setspecific(context_key, ctx);
#endif
	return ctx;
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env;

	if ((*vm)->GetEnv(vm, (void **)&env, MY_JNI_VERSION) != JNI_OK)
		return -1;

	return MY_JNI_VERSION;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
	JNIEnv *env;

	if ((*vm)->GetEnv(vm, (void **)&env, MY_JNI_VERSION) != JNI_OK)
		return; /* If this fails, we're really in trouble! */

	fz_drop_context(base_context);
	base_context = NULL;
	lose_fids(env);
}

JNIEXPORT jint JNICALL
FUN(Context_initNative)(JNIEnv *env, jclass cls)
{
	if (!check_enums())
		return -1;

	/* Must init the context before find_finds, because the act of
	 * finding the fids can cause classes to load. This causes
	 * statics to be setup, which can in turn call JNI code, which
	 * requires the context. (For example see ColorSpace) */
	if (init_base_context(env) < 0)
		return -1;

	if (find_fids(env) != 0)
	{
		fin_base_context(env);
		return -1;
	}

	return 0;
}

JNIEXPORT jint JNICALL
FUN(Context_gprfSupportedNative)(JNIEnv * env, jclass class)
{
#ifdef FZ_ENABLE_GPRF
	return JNI_TRUE;
#else
	return JNI_FALSE;
#endif
}

/* Conversion functions: C to Java. These all throw fitz exceptions. */

static inline jobject to_ColorSpace(fz_context *ctx, JNIEnv *env, fz_colorspace *cs)
{
	jobject jcs;

	if (!ctx || !cs) return NULL;

	fz_keep_colorspace(ctx, cs);
	jcs = (*env)->CallStaticObjectMethod(env, cls_ColorSpace, mid_ColorSpace_fromPointer, jlong_cast(cs));
	if (!jcs)
		fz_drop_colorspace(ctx, cs);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jcs;
}

static inline jobject to_Image(fz_context *ctx, JNIEnv *env, fz_image *img)
{
	jobject jimg;

	if (!ctx || !img) return NULL;

	fz_keep_image(ctx, img);
	jimg = (*env)->NewObject(env, cls_Image, mid_Image_init, jlong_cast(img));
	if (!jimg)
		fz_drop_image(ctx, img);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jimg;
}

static inline jobject to_Matrix(fz_context *ctx, JNIEnv *env, const fz_matrix *mat)
{
	jobject jctm;

	if (!ctx) return NULL;

	jctm = (*env)->NewObject(env, cls_Matrix, mid_Matrix_init, mat->a, mat->b, mat->c, mat->d, mat->e, mat->f);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jctm;
}

static inline jobject to_Path(fz_context *ctx, JNIEnv *env, const fz_path *path)
{
	jobject jpath;

	if (!ctx || !path) return NULL;

	fz_keep_path(ctx, path);
	jpath = (*env)->NewObject(env, cls_Path, mid_Path_init, jlong_cast(path));
	if (!jpath)
		fz_drop_path(ctx, path);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jpath;
}

static inline jobject to_Rect(fz_context *ctx, JNIEnv *env, const fz_rect *rect)
{
	jobject jrect;

	if (!ctx) return NULL;

	jrect = (*env)->NewObject(env, cls_Rect, mid_Rect_init, rect->x0, rect->y0, rect->x1, rect->y1);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jrect;
}

static inline jobject to_Shade(fz_context *ctx, JNIEnv *env, fz_shade *shd)
{
	jobject jshd;

	if (!ctx || !shd) return NULL;

	fz_keep_shade(ctx, shd);
	jshd = (*env)->NewObject(env, cls_Shade, mid_Shade_init, jlong_cast(shd));
	if (!jshd)
		fz_drop_shade(ctx, shd);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jshd;
}

static inline jobject to_StrokeState(fz_context *ctx, JNIEnv *env, const fz_stroke_state *state)
{
	jobject jstate;

	if (!ctx || !state) return NULL;

	fz_keep_stroke_state(ctx, state);
	jstate = (*env)->NewObject(env, cls_StrokeState, mid_StrokeState_init, jlong_cast(state));
	if (!jstate)
		fz_drop_stroke_state(ctx, state);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jstate;
}

static inline jobject to_Text(fz_context *ctx, JNIEnv *env, const fz_text *text)
{
	jobject jtext;

	if (!ctx) return NULL;

	fz_keep_text(ctx, text);
	jtext = (*env)->NewObject(env, cls_Text, mid_Text_init, jlong_cast(text));
	if (!jtext)
		fz_drop_text(ctx, text);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return jtext;
}

static inline jfloatArray to_jfloatArray(fz_context *ctx, JNIEnv *env, const float *color, jint n)
{
	jfloatArray arr;

	if (!ctx) return NULL;

	arr = (*env)->NewFloatArray(env, n);
	if (!arr)
		fz_throw_java(ctx, env);

	(*env)->SetFloatArrayRegion(env, arr, 0, n, color);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return arr;
}

/* Conversion functions: C to Java. None of these throw fitz exceptions. */

static inline jobject to_Annotation_safe(fz_context *ctx, JNIEnv *env, fz_annot *annot)
{
	jobject jannot;
	pdf_annot *pannot;

	if (!ctx || !annot) return NULL;

	fz_keep_annot(ctx, annot);

	pannot = pdf_annot_from_fz_annot(ctx, annot);
	if (pannot)
		jannot = (*env)->NewObject(env, cls_PDFAnnotation, mid_PDFAnnotation_init, jlong_cast(annot));
	else
		jannot = (*env)->NewObject(env, cls_Annotation, mid_Annotation_init, jlong_cast(annot));
	if (!jannot)
		fz_drop_annot(ctx, annot);

	return jannot;
}

static inline jint to_ColorParams_safe(fz_context *ctx, JNIEnv *env, const fz_color_params *cp)
{
	if (!ctx || !cp)
		return 0;

	return (((int) (!!cp->bp)<<5) | ((int) (!!cp->op)<<6) || ((int) (!!cp->opm)<<7) || (cp->ri & 31));
}

static inline jobject to_ColorSpace_safe(fz_context *ctx, JNIEnv *env, fz_colorspace *cs)
{
	jobject jcs;

	if (!ctx || !cs) return NULL;

	fz_keep_colorspace(ctx, cs);
	jcs = (*env)->CallStaticObjectMethod(env, cls_ColorSpace, mid_ColorSpace_fromPointer, jlong_cast(cs));
	if (!jcs) fz_drop_colorspace(ctx, cs);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return jcs;
}

static inline jobject to_Font_safe(fz_context *ctx, JNIEnv *env, fz_font *font)
{
	jobject jfont;

	if (!ctx || !font) return NULL;

	fz_keep_font(ctx, font);
	jfont = (*env)->NewObject(env, cls_Font, mid_Font_init, jlong_cast(font));
	if (!jfont)
		fz_drop_font(ctx, font);

	return jfont;
}

static inline jobject to_Image_safe(fz_context *ctx, JNIEnv *env, fz_image *img)
{
	jobject jimg;

	if (!ctx || !img) return NULL;

	fz_keep_image(ctx, img);
	jimg = (*env)->NewObject(env, cls_Image, mid_Image_init, jlong_cast(img));
	if (!jimg)
		fz_drop_image(ctx, img);

	return jimg;
}

static inline jobject to_Outline_safe(fz_context *ctx, JNIEnv *env, fz_document *doc, fz_outline *outline)
{
	jobject joutline = NULL;
	jobject jarr = NULL;
	jsize jindex = 0;
	jsize count = 0;
	fz_outline *counter = outline;

	if (!ctx || !outline) return NULL;

	while (counter)
	{
		count++;
		counter = counter->next;
	}

	jarr = (*env)->NewObjectArray(env, count, cls_Outline, NULL);
	if (!jarr) return NULL;

	while (outline)
	{
		jstring jtitle = NULL;
		jint jpage = -1;
		jstring juri = NULL;
		jobject jdown = NULL;
		float x = 0;
		float y = 0;

		if (outline->title)
		{
			jtitle = (*env)->NewStringUTF(env, outline->title);
			if (!jtitle) return NULL;
		}

		if (outline->uri)
		{
			if (fz_is_external_link(ctx, outline->uri))
			{
				juri = (*env)->NewStringUTF(env, outline->uri);
				if (!juri) return NULL;
			}
			else
				jpage = fz_resolve_link(ctx, doc, outline->uri, &x, &y);
		}

		if (outline->down)
		{
			jdown = to_Outline_safe(ctx, env, doc, outline->down);
			if (!jdown) return NULL;
		}

		joutline = (*env)->NewObject(env, cls_Outline, mid_Outline_init, jtitle, jpage, juri, x, y, jdown);
		if (!joutline) return NULL;

		if (jdown)
			(*env)->DeleteLocalRef(env, jdown);
		if (juri)
			(*env)->DeleteLocalRef(env, juri);
		if (jtitle)
			(*env)->DeleteLocalRef(env, jtitle);

		(*env)->SetObjectArrayElement(env, jarr, jindex++, joutline);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, joutline);
		outline = outline->next;
	}

	return jarr;
}

static inline jobject to_PDFObject_safe(fz_context *ctx, JNIEnv *env, jobject pdf, pdf_obj *obj)
{
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	if (obj == NULL)
		return (*env)->GetStaticObjectField(env, cls_PDFObject, fid_PDFObject_Null);

	pdf_keep_obj(ctx, obj);
	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), pdf);
	if (!jobj)
		pdf_drop_obj(ctx, obj);

	return jobj;
}

static inline jobject to_Point_safe(fz_context *ctx, JNIEnv *env, fz_point point)
{
	if (!ctx) return NULL;

	return (*env)->NewObject(env, cls_Point, mid_Point_init, point.x, point.y);
}

static inline jobject to_Rect_safe(fz_context *ctx, JNIEnv *env, const fz_rect *rect)
{
	if (!ctx || !rect) return NULL;

	return (*env)->NewObject(env, cls_Rect, mid_Rect_init, rect->x0, rect->y0, rect->x1, rect->y1);
}

static inline jobjectArray to_jRectArray_safe(fz_context *ctx, JNIEnv *env, const fz_rect *rects, jint n)
{
	jobjectArray arr;
	int i;

	if (!ctx || !rects) return NULL;

	arr = (*env)->NewObjectArray(env, n, cls_Rect, NULL);
	if (!arr) return NULL;

	for (i = 0; i < n; i++)
	{
		jobject jrect = to_Rect_safe(ctx, env, &rects[i]);
		if (!jrect) return NULL;

		(*env)->SetObjectArrayElement(env, arr, i, jrect);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, jrect);
	}

	return arr;
}

/* Conversion functions: C to Java. Take ownership of fitz object. None of these throw fitz exceptions. */

static inline jobject to_Document_safe_own(fz_context *ctx, JNIEnv *env, fz_document *doc)
{
	jobject obj;
	pdf_document *pdf;

	if (!ctx || !doc) return NULL;

	pdf = pdf_document_from_fz_document(ctx, doc);
	if (pdf)
		obj = (*env)->NewObject(env, cls_PDFDocument, mid_PDFDocument_init, jlong_cast(pdf));
	else
		obj = (*env)->NewObject(env, cls_Document, mid_Document_init, jlong_cast(doc));
	if (!obj)
		fz_drop_document(ctx, doc);

	return obj;
}

static inline jobject to_Device_safe_own(fz_context *ctx, JNIEnv *env, fz_device *device)
{
	jobject jdev;

	if (!ctx || !device) return NULL;

	jdev = (*env)->NewObject(env, cls_DisplayList, mid_Device_init, jlong_cast(device));
	if (!jdev)
		fz_drop_device(ctx, device);

	return jdev;
}

static inline jobject to_DisplayList_safe_own(fz_context *ctx, JNIEnv *env, fz_display_list *list)
{
	jobject jlist;

	if (!ctx || !list) return NULL;

	jlist = (*env)->NewObject(env, cls_DisplayList, mid_DisplayList_init, jlong_cast(list));
	if (!jlist)
		fz_drop_display_list(ctx, list);

	return jlist;
}

static inline jobject to_Page_safe_own(fz_context *ctx, JNIEnv *env, fz_page *page)
{
	jobject jobj;
	pdf_page *ppage;

	if (!ctx || !page) return NULL;

	ppage = pdf_page_from_fz_page(ctx, page);
	if (ppage)
		jobj = (*env)->NewObject(env, cls_PDFPage, mid_PDFPage_init, jlong_cast(page));
	else
		jobj = (*env)->NewObject(env, cls_Page, mid_Page_init, jlong_cast(page));
	if (!jobj)
		fz_drop_page(ctx, page);

	return jobj;
}

static inline jobject to_PDFAnnotation_safe_own(fz_context *ctx, JNIEnv *env, jobject pdf, pdf_annot *annot)
{
	jobject jannot;

	if (!ctx || !annot || !pdf) return NULL;

	jannot = (*env)->NewObject(env, cls_PDFAnnotation, mid_PDFAnnotation_init, jlong_cast(annot), pdf);
	if (!jannot)
		pdf_drop_annots(ctx, annot);

	return jannot;
}

static inline jobject to_PDFGraftMap_safe_own(fz_context *ctx, JNIEnv *env, jobject pdf, pdf_graft_map *map)
{
	jobject jmap;

	if (!ctx || !map || !pdf) return NULL;

	jmap = (*env)->NewObject(env, cls_PDFGraftMap, mid_PDFGraftMap_init, jlong_cast(map), pdf);
	if (!jmap)
		pdf_drop_graft_map(ctx, map);

	return jmap;
}

static inline jobject to_PDFObject_safe_own(fz_context *ctx, JNIEnv *env, jobject pdf, pdf_obj *obj)
{
	jobject jobj;

	if (!ctx || !obj || !pdf) return NULL;

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), pdf);
	if (!jobj)
		pdf_drop_obj(ctx, obj);

	return jobj;
}

static inline jobject to_Pixmap_safe_own(fz_context *ctx, JNIEnv *env, fz_pixmap *pixmap)
{
	jobject jobj;

	if (!ctx || !pixmap) return NULL;

	jobj = (*env)->NewObject(env, cls_Pixmap, mid_Pixmap_init, jlong_cast(pixmap));
	if (!jobj)
		fz_drop_pixmap(ctx, pixmap);

	return jobj;
}

static inline jobject to_StructuredText_safe_own(fz_context *ctx, JNIEnv *env, fz_stext_page *text)
{
	jobject jtext;

	if (!ctx || !text) return NULL;

	jtext = (*env)->NewObject(env, cls_StructuredText, mid_StructuredText_init, jlong_cast(text));
	if (!jtext)
		fz_drop_stext_page(ctx, text);

	return jtext;
}

/* Conversion functions: Java to C. These all throw java exceptions. */

static inline fz_annot *from_Annotation(JNIEnv *env, jobject jobj)
{
	fz_annot *annot;
	if (!jobj) return NULL;
	annot = CAST(fz_annot *, (*env)->GetLongField(env, jobj, fid_Annotation_pointer));
	if (!annot) jni_throw_null(env, "cannot use already destroyed Annotation");
	return annot;
}

static inline fz_buffer *from_Buffer(JNIEnv *env, jobject jobj)
{
	fz_buffer *buffer;
	if (!jobj) return NULL;
	buffer = CAST(fz_buffer *, (*env)->GetLongField(env, jobj, fid_Buffer_pointer));
	if (!buffer) jni_throw_null(env, "cannot use already destroyed Buffer");
	return buffer;
}

static inline fz_colorspace *from_ColorSpace(JNIEnv *env, jobject jobj)
{
	fz_colorspace *cs;
	if (!jobj) return NULL;
	cs = CAST(fz_colorspace *, (*env)->GetLongField(env, jobj, fid_ColorSpace_pointer));
	if (!cs) jni_throw_null(env, "cannot use already destroyed ColorSpace");
	return cs;
}

static inline fz_cookie *from_Cookie(JNIEnv *env, jobject jobj)
{
	fz_cookie *cookie;
	if (!jobj) return NULL;
	cookie = CAST(fz_cookie *, (*env)->GetLongField(env, jobj, fid_Cookie_pointer));
	if (!cookie) jni_throw_null(env, "cannot use already destroyed Cookie");
	return cookie;
}

static fz_device *from_Device(JNIEnv *env, jobject jobj)
{
	fz_device *dev;
	if (!jobj) return NULL;
	dev = CAST(fz_device *, (*env)->GetLongField(env, jobj, fid_Device_pointer));
	if (!dev) jni_throw_null(env, "cannot use already destroyed Device");
	return dev;
}

static inline fz_display_list *from_DisplayList(JNIEnv *env, jobject jobj)
{
	fz_display_list *list;
	if (!jobj) return NULL;
	list = CAST(fz_display_list *, (*env)->GetLongField(env, jobj, fid_DisplayList_pointer));
	if (!list) jni_throw_null(env, "cannot use already destroyed DisplayList");
	return list;
}

static inline fz_document *from_Document(JNIEnv *env, jobject jobj)
{
	fz_document *doc;
	if (!jobj) return NULL;
	doc = CAST(fz_document *, (*env)->GetLongField(env, jobj, fid_Document_pointer));
	if (!doc) jni_throw_null(env, "cannot use already destroyed Document");
	return doc;
}

static inline fz_document_writer *from_DocumentWriter(JNIEnv *env, jobject jobj)
{
	fz_document_writer *wri;
	if (!jobj) return NULL;
	wri = CAST(fz_document_writer *, (*env)->GetLongField(env, jobj, fid_DocumentWriter_pointer));
	if (!wri) jni_throw_null(env, "cannot use already destroyed DocumentWriter");
	return wri;
}

static inline fz_font *from_Font(JNIEnv *env, jobject jobj)
{
	fz_font *font;
	if (!jobj) return NULL;
	font = CAST(fz_font *, (*env)->GetLongField(env, jobj, fid_Font_pointer));
	if (!font) jni_throw_null(env, "cannot use already destroyed Font");
	return font;
}

static inline fz_image *from_Image(JNIEnv *env, jobject jobj)
{
	fz_image *image;
	if (!jobj) return NULL;
	image = CAST(fz_image *, (*env)->GetLongField(env, jobj, fid_Image_pointer));
	if (!image) jni_throw_null(env, "cannot use already destroyed Image");
	return image;
}

static inline fz_page *from_Page(JNIEnv *env, jobject jobj)
{
	fz_page *page;
	if (!jobj) return NULL;
	page = CAST(fz_page *, (*env)->GetLongField(env, jobj, fid_Page_pointer));
	if (!page) jni_throw_null(env, "cannot use already destroyed Page");
	return page;
}

static inline fz_path *from_Path(JNIEnv *env, jobject jobj)
{
	fz_path *path;
	if (!jobj) return NULL;
	path = CAST(fz_path *, (*env)->GetLongField(env, jobj, fid_Path_pointer));
	if (!path) jni_throw_null(env, "cannot use already destroyed Path");
	return path;
}

static inline pdf_annot *from_PDFAnnotation(JNIEnv *env, jobject jobj)
{
	pdf_annot *annot;
	if (!jobj) return NULL;
	annot = CAST(pdf_annot *, (*env)->GetLongField(env, jobj, fid_PDFAnnotation_pointer));
	if (!annot) jni_throw_null(env, "cannot use already destroyed PDFAnnotation");
	return annot;
}

static inline pdf_document *from_PDFDocument(JNIEnv *env, jobject jobj)
{
	pdf_document *pdf;
	if (!jobj) return NULL;
	pdf = CAST(pdf_document *, (*env)->GetLongField(env, jobj, fid_PDFDocument_pointer));
	if (!pdf) jni_throw_null(env, "cannot use already destroyed PDFDocument");
	return pdf;
}

static inline pdf_graft_map *from_PDFGraftMap(JNIEnv *env, jobject jobj)
{
	pdf_graft_map *map;
	if (!jobj) return NULL;
	map = CAST(pdf_graft_map *, (*env)->GetLongField(env, jobj, fid_PDFGraftMap_pointer));
	if (!map) jni_throw_null(env, "cannot use already destroyed PDFGraftMap");
	return map;
}

static inline pdf_obj *from_PDFObject(JNIEnv *env, jobject jobj)
{
	pdf_obj *obj;
	if (!jobj) return NULL;
	obj = CAST(pdf_obj *, (*env)->GetLongField(env, jobj, fid_PDFObject_pointer));
	if (!obj) jni_throw_null(env, "cannot use already destroyed PDFObject");
	return obj;
}

static inline pdf_page *from_PDFPage(JNIEnv *env, jobject jobj)
{
	pdf_page *page;
	if (!jobj) return NULL;
	page = CAST(pdf_page *, (*env)->GetLongField(env, jobj, fid_PDFPage_pointer));
	if (!page) jni_throw_null(env, "cannot use already destroyed PDFPage");
	return page;
}

static inline fz_pixmap *from_Pixmap(JNIEnv *env, jobject jobj)
{
	fz_pixmap *pixmap;
	if (!jobj) return NULL;
	pixmap = CAST(fz_pixmap *, (*env)->GetLongField(env, jobj, fid_Pixmap_pointer));
	if (!pixmap) jni_throw_null(env, "cannot use already destroyed Pixmap");
	return pixmap;
}

static inline fz_shade *from_Shade(JNIEnv *env, jobject jobj)
{
	fz_shade *shd;
	if (!jobj) return NULL;
	shd = CAST(fz_shade *, (*env)->GetLongField(env, jobj, fid_Shade_pointer));
	if (!shd) jni_throw_null(env, "cannot use already destroyed Shade");
	return shd;
}

static inline fz_stroke_state *from_StrokeState(JNIEnv *env, jobject jobj)
{
	fz_stroke_state *stroke;
	if (!jobj) return NULL;
	stroke = CAST(fz_stroke_state *, (*env)->GetLongField(env, jobj, fid_StrokeState_pointer));
	if (!stroke) jni_throw_null(env, "cannot use already destroyed StrokeState");
	return stroke;
}

static inline fz_stext_page *from_StructuredText(JNIEnv *env, jobject jobj)
{
	fz_stext_page *stext;
	if (!jobj) return NULL;
	stext = CAST(fz_stext_page *, (*env)->GetLongField(env, jobj, fid_StructuredText_pointer));
	if (!stext) jni_throw_null(env, "cannot use already destroyed StructuredText");
	return stext;
}

static inline fz_text *from_Text(JNIEnv *env, jobject jobj)
{
	fz_text *text;
	if (!jobj) return NULL;
	text = CAST(fz_text *, (*env)->GetLongField(env, jobj, fid_Text_pointer));
	if (!text) jni_throw_null(env, "cannot use already destroyed Text");
	return text;
}

static inline int from_jfloatArray(JNIEnv *env, float *color, jint n, jfloatArray jcolor)
{
	jsize len;

	if (!jcolor)
		len = 0;
	else
	{
		len = (*env)->GetArrayLength(env, jcolor);
		if (len > n)
			len = n;
		(*env)->GetFloatArrayRegion(env, jcolor, 0, len, color);
		if ((*env)->ExceptionCheck(env)) return 0;
	}

	if (len < n)
		memset(color+len, 0, (n - len) * sizeof(float));

	return 1;
}

static inline fz_matrix from_Matrix(JNIEnv *env, jobject jmat)
{
	fz_matrix mat;

	if (!jmat)
		return fz_identity;

	mat.a = (*env)->GetFloatField(env, jmat, fid_Matrix_a);
	mat.b = (*env)->GetFloatField(env, jmat, fid_Matrix_b);
	mat.c = (*env)->GetFloatField(env, jmat, fid_Matrix_c);
	mat.d = (*env)->GetFloatField(env, jmat, fid_Matrix_d);
	mat.e = (*env)->GetFloatField(env, jmat, fid_Matrix_e);
	mat.f = (*env)->GetFloatField(env, jmat, fid_Matrix_f);

	return mat;
}

static inline fz_point from_Point(JNIEnv *env, jobject jpt)
{
	fz_point pt;

	if (!jpt)
	{
		pt.x = pt.y = 0;
		return pt;
	}

	pt.x = (*env)->GetFloatField(env, jpt, fid_Point_x);
	pt.y = (*env)->GetFloatField(env, jpt, fid_Point_y);

	return pt;
}

static inline fz_rect from_Rect(JNIEnv *env, jobject jrect)
{
	fz_rect rect;

	if (!jrect)
		return fz_empty_rect;

	rect.x0 = (*env)->GetFloatField(env, jrect, fid_Rect_x0);
	rect.x1 = (*env)->GetFloatField(env, jrect, fid_Rect_x1);
	rect.y0 = (*env)->GetFloatField(env, jrect, fid_Rect_y0);
	rect.y1 = (*env)->GetFloatField(env, jrect, fid_Rect_y1);

	return rect;
}

/* Conversion functions: Java to C. None of these throw java exceptions. */

static inline fz_annot *from_Annotation_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_annot *, (*env)->GetLongField(env, jobj, fid_Annotation_pointer));
}

static inline fz_buffer *from_Buffer_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_buffer *, (*env)->GetLongField(env, jobj, fid_Buffer_pointer));
}

static inline fz_color_params from_ColorParams_safe(JNIEnv *env, jint params)
{
	fz_color_params p;

	p.bp = (params>>5) & 1;
	p.op = (params>>6) & 1;
	p.opm = (params>>7) & 1;
	p.ri = (params & 31);

	return p;
}

static inline fz_colorspace *from_ColorSpace_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_colorspace *, (*env)->GetLongField(env, jobj, fid_ColorSpace_pointer));
}

static inline fz_cookie *from_Cookie_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_cookie *, (*env)->GetLongField(env, jobj, fid_Cookie_pointer));
}

static fz_device *from_Device_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_device *, (*env)->GetLongField(env, jobj, fid_Device_pointer));
}

static inline fz_display_list *from_DisplayList_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_display_list *, (*env)->GetLongField(env, jobj, fid_DisplayList_pointer));
}

static inline fz_document *from_Document_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_document *, (*env)->GetLongField(env, jobj, fid_Document_pointer));
}

static inline fz_document_writer *from_DocumentWriter_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_document_writer *, (*env)->GetLongField(env, jobj, fid_DocumentWriter_pointer));
}

static inline fz_font *from_Font_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_font *, (*env)->GetLongField(env, jobj, fid_Font_pointer));
}

static inline fz_image *from_Image_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_image *, (*env)->GetLongField(env, jobj, fid_Image_pointer));
}

static inline fz_page *from_Page_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_page *, (*env)->GetLongField(env, jobj, fid_Page_pointer));
}

static inline fz_path *from_Path_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_path *, (*env)->GetLongField(env, jobj, fid_Path_pointer));
}

static inline pdf_document *from_PDFDocument_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(pdf_document *, (*env)->GetLongField(env, jobj, fid_PDFDocument_pointer));
}

static inline pdf_graft_map *from_PDFGraftMap_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(pdf_graft_map *, (*env)->GetLongField(env, jobj, fid_PDFGraftMap_pointer));
}

static inline pdf_obj *from_PDFObject_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(pdf_obj *, (*env)->GetLongField(env, jobj, fid_PDFObject_pointer));
}

static inline fz_pixmap *from_Pixmap_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_pixmap *, (*env)->GetLongField(env, jobj, fid_Pixmap_pointer));
}

static inline fz_shade *from_Shade_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_shade *, (*env)->GetLongField(env, jobj, fid_Shade_pointer));
}

static inline fz_stroke_state *from_StrokeState_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_stroke_state *, (*env)->GetLongField(env, jobj, fid_StrokeState_pointer));
}

static inline fz_stext_page *from_StructuredText_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_stext_page *, (*env)->GetLongField(env, jobj, fid_StructuredText_pointer));
}

static inline fz_text *from_Text_safe(JNIEnv *env, jobject jobj)
{
	if (!jobj) return NULL;
	return CAST(fz_text *, (*env)->GetLongField(env, jobj, fid_Text_pointer));
}

/*
	Devices can either be implemented in C, or in Java.
	We therefore have to think about 4 possible call combinations.

	1) C -> C:
	The standard mupdf case. No special worries here.
	2) C -> Java:
	This can only happen when we call run on a page/annotation/
	displaylist. We need to ensure that the java Device has an
	appropriate fz_java_device generated for it, which is done by the
	Device constructor. The 'run' calls take care to lock/unlock for us.
	3) Java -> C:
	The C device will have a java shim (a subclass of NativeDevice).
	All calls will go through the device methods in NativeDevice,
	which converts the java objects to C ones, and lock/unlock
	any underlying objects as required.
	4) Java -> Java:
	No special worries.
 */

typedef struct
{
	fz_device super;
	JNIEnv *env;
	jobject self;
}
fz_java_device;

static void
fz_java_device_fill_path(fz_context *ctx, fz_device *dev, const fz_path *path, int even_odd, const fz_matrix *ctm, fz_colorspace *cs, const float *color, float alpha, const fz_color_params *cs_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jpath = to_Path(ctx, env, path);
	jobject jcs = to_ColorSpace(ctx, env, cs);
	jobject jctm = to_Matrix(ctx, env, ctm);
	jfloatArray jcolor = to_jfloatArray(ctx, env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS);
	int jcp = to_ColorParams_safe(ctx, env, cs_params);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_fillPath, jpath, (jboolean)even_odd, jctm, jcs, jcolor, alpha, jcp);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_stroke_path(fz_context *ctx, fz_device *dev, const fz_path *path, const fz_stroke_state *state, const fz_matrix *ctm, fz_colorspace *cs, const float *color, float alpha, const fz_color_params *cs_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jpath = to_Path(ctx, env, path);
	jobject jstate = to_StrokeState(ctx, env, state);
	jobject jcs = to_ColorSpace(ctx, env, cs);
	jobject jctm = to_Matrix(ctx, env, ctm);
	jfloatArray jcolor = to_jfloatArray(ctx, env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS);
	int jcp = to_ColorParams_safe(ctx, env, cs_params);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_strokePath, jpath, jstate, jctm, jcs, jcolor, alpha, jcp);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_clip_path(fz_context *ctx, fz_device *dev, const fz_path *path, int even_odd, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jpath = to_Path(ctx, env, path);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_clipPath, jpath, (jboolean)even_odd, jctm);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_clip_stroke_path(fz_context *ctx, fz_device *dev, const fz_path *path, const fz_stroke_state *state, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jpath = to_Path(ctx, env, path);
	jobject jstate = to_StrokeState(ctx, env, state);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_clipStrokePath, jpath, jstate, jctm);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_fill_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_matrix *ctm, fz_colorspace *cs, const float *color, float alpha, const fz_color_params *cs_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jtext = to_Text(ctx, env, text);
	jobject jctm = to_Matrix(ctx, env, ctm);
	jobject jcs = to_ColorSpace(ctx, env, cs);
	jfloatArray jcolor = to_jfloatArray(ctx, env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS);
	int jcp = to_ColorParams_safe(ctx, env, cs_params);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_fillText, jtext, jctm, jcs, jcolor, alpha, jcp);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_stroke_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_stroke_state *state, const fz_matrix *ctm, fz_colorspace *cs, const float *color, float alpha, const fz_color_params *cs_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jtext = to_Text(ctx, env, text);
	jobject jstate = to_StrokeState(ctx, env, state);
	jobject jctm = to_Matrix(ctx, env, ctm);
	jobject jcs = to_ColorSpace(ctx, env, cs);
	jfloatArray jcolor = to_jfloatArray(ctx, env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS);
	int jcp = to_ColorParams_safe(ctx, env, cs_params);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_strokeText, jtext, jstate, jctm, jcs, jcolor, alpha, jcp);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_clip_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jtext = to_Text(ctx, env, text);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_clipText, jtext, jctm);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_clip_stroke_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_stroke_state *state, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jtext = to_Text(ctx, env, text);
	jobject jstate = to_StrokeState(ctx, env, state);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_clipStrokeText, jtext, jstate, jctm);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_ignore_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_matrix *ctm)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jtext = to_Text(ctx, env, text);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_ignoreText, jtext, jctm);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_fill_shade(fz_context *ctx, fz_device *dev, fz_shade *shd, const fz_matrix *ctm, float alpha, const fz_color_params *color_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jshd = to_Shade(ctx, env, shd);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_fillShade, jshd, jctm, alpha);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_fill_image(fz_context *ctx, fz_device *dev, fz_image *img, const fz_matrix *ctm, float alpha, const fz_color_params *color_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jimg = to_Image(ctx, env, img);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_fillImage, jimg, jctm, alpha);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_fill_image_mask(fz_context *ctx, fz_device *dev, fz_image *img, const fz_matrix *ctm, fz_colorspace *cs, const float *color, float alpha, const fz_color_params *cs_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jimg = to_Image(ctx, env, img);
	jobject jctm = to_Matrix(ctx, env, ctm);
	jobject jcs = to_ColorSpace(ctx, env, cs);
	jfloatArray jcolor = to_jfloatArray(ctx, env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS);
	int jcp = to_ColorParams_safe(ctx, env, cs_params);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_fillImageMask, jimg, jctm, jcs, jcolor, alpha, jcp);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_clip_image_mask(fz_context *ctx, fz_device *dev, fz_image *img, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jimg = to_Image(ctx, env, img);
	jobject jctm = to_Matrix(ctx, env, ctm);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_clipImageMask, jimg, jctm);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_pop_clip(fz_context *ctx, fz_device *dev)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_popClip);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_begin_mask(fz_context *ctx, fz_device *dev, const fz_rect *rect, int luminosity, fz_colorspace *cs, const float *bc, const fz_color_params *cs_params)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jrect = to_Rect(ctx, env, rect);
	jobject jcs = to_ColorSpace(ctx, env, cs);
	jfloatArray jbc = to_jfloatArray(ctx, env, bc, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS);
	int jcp = to_ColorParams_safe(ctx, env, cs_params);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_beginMask, jrect, (jint)luminosity, jcs, jbc, jcp);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_end_mask(fz_context *ctx, fz_device *dev)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_endMask);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_begin_group(fz_context *ctx, fz_device *dev, const fz_rect *rect, fz_colorspace *cs, int isolated, int knockout, int blendmode, float alpha)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jrect = to_Rect(ctx, env, rect);
	jobject jcs = to_ColorSpace(ctx, env, cs);

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_beginGroup, jrect, jcs, (jboolean)isolated, (jboolean)knockout, (jint)blendmode, alpha);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_end_group(fz_context *ctx, fz_device *dev)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_endGroup);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static int
fz_java_device_begin_tile(fz_context *ctx, fz_device *dev, const fz_rect *area, const fz_rect *view, float xstep, float ystep, const fz_matrix *ctm, int id)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;
	jobject jarea = to_Rect(ctx, env, area);
	jobject jview = to_Rect(ctx, env, view);
	jobject jctm = to_Matrix(ctx, env, ctm);
	int res;

	res = (*env)->CallIntMethod(env, jdev->self, mid_Device_beginTile, jarea, jview, xstep, ystep, jctm, (jint)id);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);

	return res;
}

static void
fz_java_device_end_tile(fz_context *ctx, fz_device *dev)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;

	(*env)->CallVoidMethod(env, jdev->self, mid_Device_endTile);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
fz_java_device_drop(fz_context *ctx, fz_device *dev)
{
	fz_java_device *jdev = (fz_java_device *)dev;
	JNIEnv *env = jdev->env;

	(*env)->DeleteGlobalRef(env, jdev->self);
}

static fz_device *fz_new_java_device(fz_context *ctx, JNIEnv *env, jobject self)
{
	fz_java_device *dev = NULL;
	jobject jself;

	jself = (*env)->NewGlobalRef(env, self);
	if (!jself) return NULL;

	fz_try(ctx)
	{
		dev = fz_new_derived_device(ctx, fz_java_device);
		dev->env = env;
		dev->self = jself;

		dev->super.drop_device = fz_java_device_drop;

		dev->super.fill_path = fz_java_device_fill_path;
		dev->super.stroke_path = fz_java_device_stroke_path;
		dev->super.clip_path = fz_java_device_clip_path;
		dev->super.clip_stroke_path = fz_java_device_clip_stroke_path;

		dev->super.fill_text = fz_java_device_fill_text;
		dev->super.stroke_text = fz_java_device_stroke_text;
		dev->super.clip_text = fz_java_device_clip_text;
		dev->super.clip_stroke_text = fz_java_device_clip_stroke_text;
		dev->super.ignore_text = fz_java_device_ignore_text;

		dev->super.fill_shade = fz_java_device_fill_shade;
		dev->super.fill_image = fz_java_device_fill_image;
		dev->super.fill_image_mask = fz_java_device_fill_image_mask;
		dev->super.clip_image_mask = fz_java_device_clip_image_mask;

		dev->super.pop_clip = fz_java_device_pop_clip;

		dev->super.begin_mask = fz_java_device_begin_mask;
		dev->super.end_mask = fz_java_device_end_mask;
		dev->super.begin_group = fz_java_device_begin_group;
		dev->super.end_group = fz_java_device_end_group;

		dev->super.begin_tile = fz_java_device_begin_tile;
		dev->super.end_tile = fz_java_device_end_tile;
	}
	fz_catch(ctx)
	{
		fz_drop_device(ctx, &dev->super);
		jni_rethrow(env, ctx);
		return NULL;
	}

	return (fz_device*)dev;
}

JNIEXPORT jlong JNICALL
FUN(Device_newNative)(JNIEnv *env, jclass self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		dev = fz_new_java_device(ctx, env, self);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(dev);
}

JNIEXPORT void JNICALL
FUN(Device_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device_safe(env, self);

	if (!ctx || !dev) return;

	fz_drop_device(ctx, dev);
}

/* Device Interface */

typedef struct NativeDeviceInfo NativeDeviceInfo;

typedef void (NativeDeviceLockFn)(JNIEnv *env, NativeDeviceInfo *info);
typedef void (NativeDeviceUnlockFn)(JNIEnv *env, NativeDeviceInfo *info);

struct NativeDeviceInfo
{
	/* Some devices (like the AndroidDrawDevice, or DrawDevice) need
	 * to lock/unlock the java object around device calls. We have functions
	 * here to do that. Other devices (like the DisplayList device) need
	 * no such locking, so these are NULL. */
	NativeDeviceLockFn *lock; /* Function to lock */
	NativeDeviceUnlockFn *unlock; /* Function to unlock */
	jobject object; /* The java object that needs to be locked. */

	/* Conceptually, we support drawing onto a 'plane' of pixels.
	 * The plane is width/height in size. The page is positioned on this
	 * at xOffset,yOffset. We want to redraw the given patch of this.
	 *
	 * The samples pointer in pixmap is updated on every lock/unlock, to
	 * cope with the object moving in memory.
	 */
	fz_pixmap *pixmap;
	int xOffset;
	int yOffset;
	int width;
};

static NativeDeviceInfo *lockNativeDevice(JNIEnv *env, jobject self)
{
	NativeDeviceInfo *info = NULL;

	if (!(*env)->IsInstanceOf(env, self, cls_NativeDevice))
		return NULL;

	info = CAST(NativeDeviceInfo *, (*env)->GetLongField(env, self, fid_NativeDevice_nativeInfo));
	if (!info)
	{
		/* Some devices (like the Displaylist device) need no locking, so have no info. */
		return NULL;
	}
	info->object = (*env)->GetObjectField(env, self, fid_NativeDevice_nativeResource);

	info->lock(env, info);

	return info;
}

static void unlockNativeDevice(JNIEnv *env, NativeDeviceInfo *info)
{
	if (info)
		info->unlock(env, info);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	NativeDeviceInfo *ninfo;

	if (!ctx) return;

	FUN(Device_finalize)(env, self); /* Call super.finalize() */

	ninfo = CAST(NativeDeviceInfo *, (*env)->GetLongField(env, self, fid_NativeDevice_nativeInfo));
	if (ninfo)
	{
		fz_drop_pixmap(ctx, ninfo->pixmap);
		fz_free(ctx, ninfo);
	}
}

JNIEXPORT void JNICALL
FUN(NativeDevice_close)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_close_device(ctx, dev);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_fillPath)(JNIEnv *env, jobject self, jobject jpath, jboolean even_odd, jobject jctm, jobject jcs, jfloatArray jcolor, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_path *path = from_Path(env, jpath);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	float color[FZ_MAX_COLORS];
	NativeDeviceInfo *info;
	fz_color_params cp = from_ColorParams_safe(env, jcp);

	if (!ctx || !dev) return;
	if (!path) { jni_throw_arg(env, "path must not be null"); return; }
	if (!from_jfloatArray(env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS, jcolor)) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_fill_path(ctx, dev, path, even_odd, &ctm, cs, color, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_strokePath)(JNIEnv *env, jobject self, jobject jpath, jobject jstroke, jobject jctm, jobject jcs, jfloatArray jcolor, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_path *path = from_Path(env, jpath);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	float color[FZ_MAX_COLORS];
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!path) { jni_throw_arg(env, "path must not be null"); return; }
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return; }
	if (!from_jfloatArray(env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS, jcolor)) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_stroke_path(ctx, dev, path, stroke, &ctm, cs, color, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_clipPath)(JNIEnv *env, jobject self, jobject jpath, jboolean even_odd, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_path *path = from_Path(env, jpath);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!path) { jni_throw_arg(env, "path must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_clip_path(ctx, dev, path, even_odd, &ctm, NULL);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_clipStrokePath)(JNIEnv *env, jobject self, jobject jpath, jobject jstroke, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_path *path = from_Path(env, jpath);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!path) { jni_throw_arg(env, "path must not be null"); return; }
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_clip_stroke_path(ctx, dev, path, stroke, &ctm, NULL);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_fillText)(JNIEnv *env, jobject self, jobject jtext, jobject jctm, jobject jcs, jfloatArray jcolor, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_text *text = from_Text(env, jtext);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	float color[FZ_MAX_COLORS];
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!text) { jni_throw_arg(env, "text must not be null"); return; }
	if (!from_jfloatArray(env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS, jcolor)) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_fill_text(ctx, dev, text, &ctm, cs, color, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_strokeText)(JNIEnv *env, jobject self, jobject jtext, jobject jstroke, jobject jctm, jobject jcs, jfloatArray jcolor, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_text *text = from_Text(env, jtext);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	float color[FZ_MAX_COLORS];
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!text) { jni_throw_arg(env, "text must not be null"); return; }
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return; }
	if (!from_jfloatArray(env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS, jcolor)) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_stroke_text(ctx, dev, text, stroke, &ctm, cs, color, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_clipText)(JNIEnv *env, jobject self, jobject jtext, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_text *text = from_Text(env, jtext);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!text) { jni_throw_arg(env, "text must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_clip_text(ctx, dev, text, &ctm, NULL);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_clipStrokeText)(JNIEnv *env, jobject self, jobject jtext, jobject jstroke, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_text *text = from_Text(env, jtext);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!text) { jni_throw_arg(env, "text must not be null"); return; }
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_clip_stroke_text(ctx, dev, text, stroke, &ctm, NULL);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_ignoreText)(JNIEnv *env, jobject self, jobject jtext, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_text *text = from_Text(env, jtext);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!text) { jni_throw_arg(env, "text must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_ignore_text(ctx, dev, text, &ctm);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_fillShade)(JNIEnv *env, jobject self, jobject jshd, jobject jctm, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_shade *shd = from_Shade(env, jshd);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!shd) { jni_throw_arg(env, "shade must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_fill_shade(ctx, dev, shd, &ctm, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_fillImage)(JNIEnv *env, jobject self, jobject jimg, jobject jctm, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_image *img = from_Image(env, jimg);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!img) { jni_throw_arg(env, "image must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_fill_image(ctx, dev, img, &ctm, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_fillImageMask)(JNIEnv *env, jobject self, jobject jimg, jobject jctm, jobject jcs, jfloatArray jcolor, jfloat alpha, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_image *img = from_Image(env, jimg);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	float color[FZ_MAX_COLORS];
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!img) { jni_throw_arg(env, "image must not be null"); return; }
	if (!from_jfloatArray(env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS, jcolor)) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_fill_image_mask(ctx, dev, img, &ctm, cs, color, alpha, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_clipImageMask)(JNIEnv *env, jobject self, jobject jimg, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_image *img = from_Image(env, jimg);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!img) { jni_throw_arg(env, "image must not be null"); return; }

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_clip_image_mask(ctx, dev, img, &ctm, NULL);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_popClip)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_pop_clip(ctx, dev);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_beginMask)(JNIEnv *env, jobject self, jobject jrect, jboolean luminosity, jobject jcs, jfloatArray jcolor, jint jcp)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_rect rect = from_Rect(env, jrect);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_color_params cp = from_ColorParams_safe(env, jcp);
	float color[FZ_MAX_COLORS];
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;
	if (!from_jfloatArray(env, color, cs ? fz_colorspace_n(ctx, cs) : FZ_MAX_COLORS, jcolor)) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_begin_mask(ctx, dev, &rect, luminosity, cs, color, &cp);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_endMask)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_end_mask(ctx, dev);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_beginGroup)(JNIEnv *env, jobject self, jobject jrect, jboolean isolated, jboolean knockout, jint blendmode, jfloat alpha)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_rect rect = from_Rect(env, jrect);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_begin_group(ctx, dev, &rect, NULL, isolated, knockout, blendmode, alpha);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(NativeDevice_endGroup)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_end_group(ctx, dev);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jint JNICALL
FUN(NativeDevice_beginTile)(JNIEnv *env, jobject self, jobject jarea, jobject jview, jfloat xstep, jfloat ystep, jobject jctm, jint id)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	fz_rect area = from_Rect(env, jarea);
	fz_rect view = from_Rect(env, jview);
	fz_matrix ctm = from_Matrix(env, jctm);
	NativeDeviceInfo *info;
	int i = 0;

	if (!ctx || !dev) return 0;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		i = fz_begin_tile_id(ctx, dev, &area, &view, xstep, ystep, &ctm, id);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return i;
}

JNIEXPORT void JNICALL
FUN(NativeDevice_endTile)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_device *dev = from_Device(env, self);
	NativeDeviceInfo *info;

	if (!ctx || !dev) return;

	info = lockNativeDevice(env, self);
	fz_try(ctx)
		fz_end_tile(ctx, dev);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jlong JNICALL
FUN(DrawDevice_newNative)(JNIEnv *env, jclass self, jobject jpixmap)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, jpixmap);
	fz_device *device = NULL;

	if (!ctx) return 0;
	if (!pixmap) { jni_throw_arg(env, "pixmap must not be null"); return 0; }

	fz_try(ctx)
		device = fz_new_draw_device(ctx, NULL, pixmap);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(device);
}

JNIEXPORT jlong JNICALL
FUN(DisplayListDevice_newNative)(JNIEnv *env, jclass self, jobject jlist)
{
	fz_context *ctx = get_context(env);
	fz_display_list *list = from_DisplayList(env, jlist);
	fz_device *device = NULL;

	if (!ctx) return 0;

	fz_var(device);

	fz_try(ctx)
		device = fz_new_list_device(ctx, list);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(device);
}

#ifdef HAVE_ANDROID

static jlong
newNativeAndroidDrawDevice(JNIEnv *env, jobject self, fz_context *ctx, jobject obj, jint width, jint height, NativeDeviceLockFn *lock, NativeDeviceUnlockFn *unlock, jint xOrigin, jint yOrigin, jint patchX0, jint patchY0, jint patchX1, jint patchY1)
{
	fz_device *device = NULL;
	fz_pixmap *pixmap = NULL;
	unsigned char dummy;
	NativeDeviceInfo *ninfo = NULL;
	fz_irect bbox;

	if (!ctx) return 0;

	/* Ensure patch fits inside bitmap. */
	if (patchX0 < 0) patchX0 = 0;
	if (patchY0 < 0) patchY0 = 0;
	if (patchX1 > width) patchX1 = width;
	if (patchY1 > height) patchY1 = height;

	bbox.x0 = xOrigin + patchX0;
	bbox.y0 = yOrigin + patchY0;
	bbox.x1 = xOrigin + patchX1;
	bbox.y1 = yOrigin + patchY1;

	fz_var(pixmap);
	fz_var(ninfo);

	fz_try(ctx)
	{
		pixmap = fz_new_pixmap_with_bbox_and_data(ctx, fz_device_rgb(ctx), &bbox, NULL, 1, &dummy);
		pixmap->stride = width * sizeof(int32_t);
		ninfo = fz_malloc(ctx, sizeof(*ninfo));
		ninfo->pixmap = pixmap;
		ninfo->lock = lock;
		ninfo->unlock = unlock;
		ninfo->xOffset = patchX0;
		ninfo->yOffset = patchY0;
		ninfo->width = width;
		ninfo->object = obj;
		(*env)->SetLongField(env, self, fid_NativeDevice_nativeInfo, jlong_cast(ninfo));
		(*env)->SetObjectField(env, self, fid_NativeDevice_nativeResource, obj);
		lockNativeDevice(env,self);
		fz_clear_pixmap_with_value(ctx, pixmap, 0xff);
		unlockNativeDevice(env,ninfo);
		device = fz_new_draw_device(ctx, NULL, pixmap);
	}
	fz_catch(ctx)
	{
		fz_drop_pixmap(ctx, pixmap);
		fz_free(ctx, ninfo);
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(device);
}

static void androidDrawDevice_lock(JNIEnv *env, NativeDeviceInfo *info)
{
	uint8_t *pixels;

	assert(info);
	assert(info->object);

	if (AndroidBitmap_lockPixels(env, info->object, (void **)&pixels) != ANDROID_BITMAP_RESULT_SUCCESS)
	{
		jni_throw(env, FZ_ERROR_GENERIC, "bitmap lock failed in DrawDevice call");
		return;
	}

	/* Now offset pixels to allow for the page offsets */
	pixels += sizeof(int32_t) * (info->xOffset + info->width * info->yOffset);

	info->pixmap->samples = pixels;
}

static void androidDrawDevice_unlock(JNIEnv *env, NativeDeviceInfo *info)
{
	assert(info);
	assert(info->object);

	if (AndroidBitmap_unlockPixels(env, info->object) != ANDROID_BITMAP_RESULT_SUCCESS)
		jni_throw(env, FZ_ERROR_GENERIC, "bitmap unlock failed in DrawDevice call");
}

JNIEXPORT jlong JNICALL
FUN(android_AndroidDrawDevice_newNative)(JNIEnv *env, jclass self, jobject jbitmap, jint xOrigin, jint yOrigin, jint pX0, jint pY0, jint pX1, jint pY1)
{
	fz_context *ctx = get_context(env);
	AndroidBitmapInfo info;
	jlong device = 0;
	int ret;

	if (!ctx) return 0;
	if (!jbitmap) { jni_throw_arg(env, "bitmap must not be null"); return 0; }

	if ((ret = AndroidBitmap_getInfo(env, jbitmap, &info)) != ANDROID_BITMAP_RESULT_SUCCESS)
		jni_throw(env, FZ_ERROR_GENERIC, "new DrawDevice failed to get bitmap info");
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
		jni_throw(env, FZ_ERROR_GENERIC, "new DrawDevice failed as bitmap format is not RGBA_8888");
	if (info.stride != info.width * 4)
		jni_throw(env, FZ_ERROR_GENERIC, "new DrawDevice failed as bitmap width != stride");

	fz_try(ctx)
		device = newNativeAndroidDrawDevice(env, self, ctx, jbitmap, info.width, info.height, androidDrawDevice_lock, androidDrawDevice_unlock, xOrigin, yOrigin, pX0, pY0, pX1, pY1);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return device;
}

JNIEXPORT jlong JNICALL
FUN(AndroidImage_newImageFromBitmap)(JNIEnv *env, jobject self, jobject jbitmap, jlong jmask)
{
	fz_context *ctx = get_context(env);
	fz_image *mask = CAST(fz_image *, jmask);
	fz_image *image = NULL;
	fz_pixmap *pixmap = NULL;
	AndroidBitmapInfo info;
	void *pixels;
	int ret;

	if (!ctx) return 0;
	if (!jbitmap) { jni_throw_arg(env, "bitmap must not be null"); return 0; }

	if (mask && mask->mask)
		jni_throw(env, FZ_ERROR_GENERIC, "new Image failed as mask cannot be masked");
	if ((ret = AndroidBitmap_getInfo(env, jbitmap, &info)) != ANDROID_BITMAP_RESULT_SUCCESS)
		jni_throw(env, FZ_ERROR_GENERIC, "new Image failed to get bitmap info");
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
		jni_throw(env, FZ_ERROR_GENERIC, "new Image failed as bitmap format is not RGBA_8888");
	if (info.stride != info.width)
		jni_throw(env, FZ_ERROR_GENERIC, "new Image failed as bitmap width != stride");

	fz_var(pixmap);

	fz_try(ctx)
	{
		pixmap = fz_new_pixmap(ctx, fz_device_rgb(ctx), info.width, info.height, NULL, 1);
		if (AndroidBitmap_lockPixels(env, jbitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS)
			fz_throw(ctx, FZ_ERROR_GENERIC, "bitmap lock failed in new Image");
		memcpy(pixmap->samples, pixels, info.width * info.height * 4);
		if (AndroidBitmap_unlockPixels(env, jbitmap) != ANDROID_BITMAP_RESULT_SUCCESS)
			fz_throw(ctx, FZ_ERROR_GENERIC, "Bitmap unlock failed in new Image");
		image = fz_new_image_from_pixmap(ctx, fz_keep_pixmap(ctx, pixmap), fz_keep_image(ctx, mask));
	}
	fz_always(ctx)
		fz_drop_pixmap(ctx, pixmap);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(image);
}
#endif

/* ColorSpace Interface */

JNIEXPORT void JNICALL
FUN(ColorSpace_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_colorspace *cs = from_ColorSpace_safe(env, self);
	if (!ctx || !cs) return;
	fz_drop_colorspace(ctx, cs);
}

JNIEXPORT jint JNICALL
FUN(ColorSpace_getNumberOfComponents)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_colorspace *cs = from_ColorSpace(env, self);
	if (!ctx) return 0;
	return fz_colorspace_n(ctx, cs);
}

JNIEXPORT jlong JNICALL
FUN(ColorSpace_nativeDeviceGray)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	if (!ctx) return 0;
	return jlong_cast(fz_device_gray(ctx));
}

JNIEXPORT jlong JNICALL
FUN(ColorSpace_nativeDeviceRGB)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	if (!ctx) return 0;
	return jlong_cast(fz_device_rgb(ctx));
}

JNIEXPORT jlong JNICALL
FUN(ColorSpace_nativeDeviceBGR)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	if (!ctx) return 0;
	return jlong_cast(fz_device_bgr(ctx));
}

JNIEXPORT jlong JNICALL
FUN(ColorSpace_nativeDeviceCMYK)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	if (!ctx) return 0;
	return jlong_cast(fz_device_cmyk(ctx));
}

/* Font interface */

JNIEXPORT void JNICALL
FUN(Font_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_font *font = from_Font_safe(env, self);

	if (!ctx || !font) return;

	fz_drop_font(ctx, font);
}

JNIEXPORT jlong JNICALL
FUN(Font_newNative)(JNIEnv *env, jobject self, jstring jname, jint index)
{
	fz_context *ctx = get_context(env);
	const char *name = NULL;
	fz_font *font = NULL;

	if (!ctx) return 0;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return 0;
	}

	fz_try(ctx)
	{
		const unsigned char *data;
		int size;

		data = fz_lookup_base14_font(ctx, name, &size);
		if (data)
			font = fz_new_font_from_memory(ctx, name, data, size, index, 0);
		else
			font = fz_new_font_from_file(ctx, name, name, index, 0);
	}
	fz_always(ctx)
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(font);
}

JNIEXPORT jstring JNICALL
FUN(Font_getName)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_font *font = from_Font(env, self);

	if (!ctx || !font) return NULL;

	return (*env)->NewStringUTF(env, fz_font_name(ctx, font));
}

JNIEXPORT jint JNICALL
FUN(Font_encodeCharacter)(JNIEnv *env, jobject self, jint unicode)
{
	fz_context *ctx = get_context(env);
	fz_font *font = from_Font(env, self);
	jint glyph = 0;

	if (!ctx || !font) return 0;

	fz_try(ctx)
		glyph = fz_encode_character(ctx, font, unicode);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return glyph;
}

JNIEXPORT jfloat JNICALL
FUN(Font_advanceGlyph)(JNIEnv *env, jobject self, jint glyph, jboolean wmode)
{
	fz_context *ctx = get_context(env);
	fz_font *font = from_Font(env, self);
	float advance = 0;

	if (!ctx || !font) return 0;

	fz_try(ctx)
		advance = fz_advance_glyph(ctx, font, glyph, wmode);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return advance;
}

/* Pixmap Interface */

JNIEXPORT void JNICALL
FUN(Pixmap_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap_safe(env, self);

	if (!ctx || !pixmap) return;

	fz_drop_pixmap(ctx, pixmap);
}

JNIEXPORT jlong JNICALL
FUN(Pixmap_newNative)(JNIEnv *env, jobject self, jobject jcs, jint x, jint y, jint w, jint h, jboolean alpha)
{
	fz_context *ctx = get_context(env);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_pixmap *pixmap = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
	{
		pixmap = fz_new_pixmap(ctx, cs, w, h, NULL, alpha);
		pixmap->x = x;
		pixmap->y = y;
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(pixmap);
}

JNIEXPORT void JNICALL
FUN(Pixmap_clear)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);

	if (!ctx || !pixmap) return;

	fz_try(ctx)
		fz_clear_pixmap(ctx, pixmap);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Pixmap_clearWithValue)(JNIEnv *env, jobject self, jint value)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);

	if (!ctx || !pixmap) return;

	fz_try(ctx)
		fz_clear_pixmap_with_value(ctx, pixmap, value);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Pixmap_saveAsPNG)(JNIEnv *env, jobject self, jstring jfilename)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);
	const char *filename = "null";

	if (!ctx) return;
	if (!jfilename) { jni_throw_arg(env, "filename must not be null"); return; }

	filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
	if (!filename) return;

	fz_try(ctx)
		fz_save_pixmap_as_png(ctx, pixmap, filename);
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getX)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->x : 0;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getY)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->y : 0;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getWidth)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->w : 0;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getHeight)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->h : 0;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getNumberOfComponents)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->n : 0;
}

JNIEXPORT jboolean JNICALL
FUN(Pixmap_getAlpha)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap && pixmap->alpha ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getStride)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->stride : 0;
}

JNIEXPORT jobject JNICALL
FUN(Pixmap_getColorSpace)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);
	fz_colorspace *cs;

	if (!ctx | !pixmap) return NULL;

	fz_try(ctx)
		cs = fz_pixmap_colorspace(ctx, pixmap);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_ColorSpace_safe(ctx, env, cs);
}

JNIEXPORT jbyteArray JNICALL
FUN(Pixmap_getSamples)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);
	int size = pixmap->h * pixmap->stride;
	jbyteArray arr;

	if (!ctx | !pixmap) return NULL;

	arr = (*env)->NewByteArray(env, size);
	if (!arr) return NULL;

	(*env)->SetByteArrayRegion(env, arr, 0, size, (const jbyte *)pixmap->samples);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return arr;
}

JNIEXPORT jbyte JNICALL
FUN(Pixmap_getSample)(JNIEnv *env, jobject self, jint x, jint y, jint k)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);

	if (!ctx | !pixmap) return 0;

	if (x < 0 || x >= pixmap->w) { jni_throw_oob(env, "x out of range"); return 0; }
	if (y < 0 || y >= pixmap->h) { jni_throw_oob(env, "y out of range"); return 0; }
	if (k < 0 || k >= pixmap->n) { jni_throw_oob(env, "k out of range"); return 0; }

	return pixmap->samples[(x + y * pixmap->w) * pixmap->n + k];
}

JNIEXPORT jintArray JNICALL
FUN(Pixmap_getPixels)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, self);
	int size = pixmap->w * pixmap->h;
	jintArray arr;

	if (!ctx | !pixmap) return NULL;

	if (pixmap->n != 4 || !pixmap->alpha)
	{
		jni_throw(env, FZ_ERROR_GENERIC, "invalid colorspace for getPixels (must be RGB/BGR with alpha)");
		return NULL;
	}

	if (size * 4 != pixmap->h * pixmap->stride)
	{
		jni_throw(env, FZ_ERROR_GENERIC, "invalid stride for getPixels");
		return NULL;
	}

	arr = (*env)->NewIntArray(env, size);
	if (!arr) return NULL;

	(*env)->SetIntArrayRegion(env, arr, 0, size, (const jint *)pixmap->samples);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return arr;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getXResolution)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->xres : 0;
}

JNIEXPORT jint JNICALL
FUN(Pixmap_getYResolution)(JNIEnv *env, jobject self)
{
	fz_pixmap *pixmap = from_Pixmap(env, self);
	return pixmap ? pixmap->yres : 0;
}

/* Path Interface */

JNIEXPORT void JNICALL
FUN(Path_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path_safe(env, self);

	if (!ctx || !path) return;

	fz_drop_path(ctx, path);
}

JNIEXPORT jlong JNICALL
FUN(Path_newNative)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_path *path = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		path = fz_new_path(ctx);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(path);
}

JNIEXPORT jobject JNICALL
FUN(Path_currentPoint)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);
	fz_point point;

	if (!ctx || !path) return NULL;

	fz_try(ctx)
		point = fz_currentpoint(ctx, path);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Point_safe(ctx, env, point);
}

JNIEXPORT void JNICALL
FUN(Path_moveTo)(JNIEnv *env, jobject self, jfloat x, jfloat y)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_moveto(ctx, path, x, y);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_lineTo)(JNIEnv *env, jobject self, jfloat x, jfloat y)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_lineto(ctx, path, x, y);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_curveTo)(JNIEnv *env, jobject self, jfloat cx1, jfloat cy1, jfloat cx2, jfloat cy2, jfloat ex, jfloat ey)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_curveto(ctx, path, cx1, cy1, cx2, cy2, ex, ey);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_curveToV)(JNIEnv *env, jobject self, jfloat cx, jfloat cy, jfloat ex, jfloat ey)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_curvetov(ctx, path, cx, cy, ex, ey);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_curveToY)(JNIEnv *env, jobject self, jfloat cx, jfloat cy, jfloat ex, jfloat ey)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_curvetoy(ctx, path, cx, cy, ex, ey);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_rect)(JNIEnv *env, jobject self, jint x1, jint y1, jint x2, jint y2)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_rectto(ctx, path, x1, y1, x2, y2);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_closePath)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_closepath(ctx, path);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Path_transform)(JNIEnv *env, jobject self, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);
	fz_matrix ctm = from_Matrix(env, jctm);

	if (!ctx || !path) return;

	fz_try(ctx)
		fz_transform_path(ctx, path, &ctm);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jlong JNICALL
FUN(Path_cloneNative)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_path *old_path = from_Path(env, self);
	fz_path *new_path = NULL;

	if (!ctx || !old_path) return 0;

	fz_try(ctx)
		new_path = fz_clone_path(ctx, old_path);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(new_path);
}

JNIEXPORT jobject JNICALL
FUN(Path_getBounds)(JNIEnv *env, jobject self, jobject jstroke, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_rect rect;

	if (!ctx || !path) return NULL;
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return NULL; }

	fz_try(ctx)
		fz_bound_path(ctx, path, stroke, &ctm, &rect);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Rect_safe(ctx, env, &rect);
}

typedef struct
{
	JNIEnv *env;
	jobject obj;
} path_walker_state;

static void
pathWalkMoveTo(fz_context *ctx, void *arg, float x, float y)
{
	path_walker_state *state = (path_walker_state *)arg;
	JNIEnv *env = state->env;
	(*env)->CallVoidMethod(env, state->obj, mid_PathWalker_moveTo, x, y);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
pathWalkLineTo(fz_context *ctx, void *arg, float x, float y)
{
	path_walker_state *state = (path_walker_state *)arg;
	JNIEnv *env = state->env;
	(*env)->CallVoidMethod(env, state->obj, mid_PathWalker_lineTo, x, y);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
pathWalkCurveTo(fz_context *ctx, void *arg, float x1, float y1, float x2, float y2, float x3, float y3)
{
	path_walker_state *state = (path_walker_state *)arg;
	JNIEnv *env = state->env;
	(*env)->CallVoidMethod(env, state->obj, mid_PathWalker_curveTo, x1, y1, x2, y2, x3, y3);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static void
pathWalkClosePath(fz_context *ctx, void *arg)
{
	path_walker_state *state = (path_walker_state *) arg;
	JNIEnv *env = state->env;
	(*env)->CallVoidMethod(env, state->obj, mid_PathWalker_closePath);
	if ((*env)->ExceptionCheck(env))
		fz_throw_java(ctx, env);
}

static const fz_path_walker java_path_walker =
{
	pathWalkMoveTo,
	pathWalkLineTo,
	pathWalkCurveTo,
	pathWalkClosePath,
	NULL,
	NULL,
	NULL,
	NULL
};

JNIEXPORT void JNICALL
FUN(Path_walk)(JNIEnv *env, jobject self, jobject obj)
{
	fz_context *ctx = get_context(env);
	fz_path *path = from_Path(env, self);
	path_walker_state state;

	if (!ctx || !path) return;
	if (!obj) { jni_throw_arg(env, "object must not be null"); return; }

	state.env = env;
	state.obj = obj;

	fz_try(ctx)
		fz_walk_path(ctx, path, &java_path_walker, &state);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

/* Rect interface */

JNIEXPORT void JNICALL
FUN(Rect_adjustForStroke)(JNIEnv *env, jobject self, jobject jstroke, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_rect rect = from_Rect(env, self);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);

	if (!ctx) return;
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return; }

	fz_try(ctx)
		fz_adjust_rect_for_stroke(ctx, &rect, stroke, &ctm);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return;
	}

	(*env)->SetFloatField(env, self, fid_Rect_x0, rect.x0);
	(*env)->SetFloatField(env, self, fid_Rect_x1, rect.x1);
	(*env)->SetFloatField(env, self, fid_Rect_y0, rect.y0);
	(*env)->SetFloatField(env, self, fid_Rect_y1, rect.y1);
}

/* StrokeState interface */

JNIEXPORT void JNICALL
FUN(StrokeState_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_stroke_state *stroke = from_StrokeState_safe(env, self);

	if (!ctx || !stroke) return;

	fz_drop_stroke_state(ctx, stroke);
}

JNIEXPORT jlong JNICALL
FUN(StrokeState_newStrokeState)(JNIEnv *env, jobject self, jint startCap, jint dashCap, jint endCap, jint lineJoin, jfloat lineWidth, jfloat miterLimit, jfloat dashPhase, jfloatArray dash)
{
	fz_context *ctx = get_context(env);
	fz_stroke_state *stroke = NULL;
	jsize len = 0;

	if (!ctx) return 0;
	if (!dash) { jni_throw_arg(env, "dash must not be null"); return 0; }

	len = (*env)->GetArrayLength(env, dash);

	fz_try(ctx)
	{
		stroke = fz_new_stroke_state_with_dash_len(ctx, len);
		stroke->start_cap = startCap;
		stroke->dash_cap = dashCap;
		stroke->end_cap = endCap;
		stroke->linejoin = lineJoin;
		stroke->linewidth = lineWidth;
		stroke->miterlimit = miterLimit;
		stroke->dash_phase = dashPhase;
		stroke->dash_len = len;
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	(*env)->GetFloatArrayRegion(env, dash, 0, len, &stroke->dash_list[0]);
	if ((*env)->ExceptionCheck(env)) return 0;

	return jlong_cast(stroke);
}

JNIEXPORT jint JNICALL
FUN(StrokeState_getStartCap)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->start_cap : 0;
}

JNIEXPORT jint JNICALL
FUN(StrokeState_getDashCap)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->dash_cap : 0;
}

JNIEXPORT jint JNICALL
FUN(StrokeState_getEndCap)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->end_cap : 0;
}

JNIEXPORT jint JNICALL
FUN(StrokeState_getLineJoin)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->linejoin : 0;
}

JNIEXPORT float JNICALL
FUN(StrokeState_getLineWidth)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->linewidth : 0;
}

JNIEXPORT float JNICALL
FUN(StrokeState_getMiterLimit)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->miterlimit : 0;
}

JNIEXPORT float JNICALL
FUN(StrokeState_getDashPhase)(JNIEnv *env, jobject self)
{
	fz_stroke_state *stroke = from_StrokeState(env, self);
	return stroke ? stroke->dash_phase : 0;
}

JNIEXPORT jfloatArray JNICALL
FUN(StrokeState_getDashes)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_stroke_state *stroke = from_StrokeState(env, self);
	jfloatArray arr;

	if (!ctx || !stroke) return NULL;

	if (stroke->dash_len == 0)
		return NULL; /* there are no dashes, so return NULL instead of empty array */

	arr = (*env)->NewFloatArray(env, stroke->dash_len);
	if (!arr) return NULL;

	(*env)->SetFloatArrayRegion(env, arr, 0, stroke->dash_len, &stroke->dash_list[0]);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return arr;
}

/* Text interface */

JNIEXPORT void JNICALL
FUN(Text_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_text *text = from_Text_safe(env, self);

	if (!ctx || !text) return;

	fz_drop_text(ctx, text);
}

JNIEXPORT jlong JNICALL
FUN(Text_clone)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_text *old_text = from_Text(env, self);
	fz_text *new_text = NULL;

	if (!ctx || !old_text) return 0;

	fz_try(ctx)
		new_text = fz_clone_text(ctx, old_text);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(new_text);
}

JNIEXPORT jlong JNICALL
FUN(Text_newNative)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_text *text = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		text = fz_new_text(ctx);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(text);
}

JNIEXPORT jlong JNICALL
FUN(Text_cloneNative)(JNIEnv *env, jobject self, jobject jold)
{
	fz_context *ctx = get_context(env);
	fz_text *old = from_Text(env, jold);
	fz_text *text = NULL;

	if (!ctx) return 0;
	if (!old) { jni_throw_arg(env, "old must not be null"); return 0; }

	fz_try(ctx)
		text = fz_clone_text(ctx, old);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(text);
}

JNIEXPORT jobject JNICALL
FUN(Text_getBounds)(JNIEnv *env, jobject self, jobject jstroke, jobject jctm)
{
	fz_context *ctx = get_context(env);
	fz_text *text = from_Text(env, self);
	fz_stroke_state *stroke = from_StrokeState(env, jstroke);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_rect rect;

	if (!ctx || !text) return NULL;
	if (!stroke) { jni_throw_arg(env, "stroke must not be null"); return NULL; }

	fz_try(ctx)
		fz_bound_text(ctx, text, stroke, &ctm, &rect);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Rect_safe(ctx, env, &rect);
}

JNIEXPORT void JNICALL
FUN(Text_showGlyph)(JNIEnv *env, jobject self, jobject jfont, jobject jtrm, jint glyph, jint unicode, jboolean wmode)
{
	fz_context *ctx = get_context(env);
	fz_text *text = from_Text(env, self);
	fz_font *font = from_Font(env, jfont);
	fz_matrix trm = from_Matrix(env, jtrm);

	if (!ctx || !text) return;
	if (!font) { jni_throw_arg(env, "font must not be null"); return; }

	fz_try(ctx)
		fz_show_glyph(ctx, text, font, &trm, glyph, unicode, wmode, 0, FZ_BIDI_NEUTRAL, FZ_LANG_UNSET);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Text_showString)(JNIEnv *env, jobject self, jobject jfont, jobject jtrm, jstring jstr, jboolean wmode)
{
	fz_context *ctx = get_context(env);
	fz_text *text = from_Text(env, self);
	fz_font *font = from_Font(env, jfont);
	fz_matrix trm = from_Matrix(env, jtrm);
	const char *str = NULL;

	if (!ctx || !text) return;
	if (!jfont) { jni_throw_arg(env, "font must not be null"); return; }
	if (!jstr) { jni_throw_arg(env, "string must not be null"); return; }

	str = (*env)->GetStringUTFChars(env, jstr, NULL);
	if (!str) return;

	fz_try(ctx)
		fz_show_string(ctx, text, font, &trm, str, wmode, 0, FZ_BIDI_NEUTRAL, FZ_LANG_UNSET);
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jstr, str);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return;
	}

	(*env)->SetFloatField(env, jtrm, fid_Matrix_e, trm.e);
	(*env)->SetFloatField(env, jtrm, fid_Matrix_f, trm.f);
}

JNIEXPORT void JNICALL
FUN(Text_walk)(JNIEnv *env, jobject self, jobject walker)
{
	fz_context *ctx = get_context(env);
	fz_text *text = from_Text(env, self);
	fz_text_span *span;
	fz_font *font = NULL;
	jobject jfont = NULL;
	jobject jtrm = NULL;
	int i;

	if (!ctx || !text) return;
	if (!walker) { jni_throw_arg(env, "walker must not be null"); return; }

	if (text->head == NULL)
		return; /* text has no spans to walk */

	for (span = text->head; span; span = span->next)
	{
		if (font != span->font)
		{
			if (jfont)
				(*env)->DeleteLocalRef(env, jfont);
			font = span->font;
			jfont = to_Font_safe(ctx, env, font);
			if (!jfont) return;
		}

		for (i = 0; i < span->len; ++i)
		{
			jtrm = (*env)->NewObject(env, cls_Matrix, mid_Matrix_init,
					span->trm.a, span->trm.b, span->trm.c, span->trm.d,
					span->items[i].x, span->items[i].y);
			if (!jtrm) return;

			(*env)->CallVoidMethod(env, walker, mid_TextWalker_showGlyph,
					jfont, jtrm,
					(jint)span->items[i].gid,
					(jint)span->items[i].ucs,
					(jint)span->wmode);
			if ((*env)->ExceptionCheck(env)) return;

			(*env)->DeleteLocalRef(env, jtrm);
		}
	}
}

/* Image interface */

JNIEXPORT void JNICALL
FUN(Image_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_image *image = from_Image_safe(env, self);

	if (!ctx || !image) return;

	fz_drop_image(ctx, image);
}

JNIEXPORT jlong JNICALL
FUN(Image_newNativeFromPixmap)(JNIEnv *env, jobject self, jobject jpixmap)
{
	fz_context *ctx = get_context(env);
	fz_pixmap *pixmap = from_Pixmap(env, jpixmap);
	fz_image *image = NULL;

	if (!ctx) return 0;
	if (!pixmap) { jni_throw_arg(env, "pixmap must not be null"); return 0; }

	fz_try(ctx)
		image = fz_new_image_from_pixmap(ctx, pixmap, NULL);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(image);
}

JNIEXPORT jlong JNICALL
FUN(Image_newNativeFromFile)(JNIEnv *env, jobject self, jstring jfilename)
{
	fz_context *ctx = get_context(env);
	const char *filename = "null";
	fz_image *image = NULL;

	if (!ctx) return 0;
	if (!jfilename) { jni_throw_arg(env, "filename must not be null"); return 0; }

	filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
	if (!filename) return 0;

	fz_try(ctx)
		image = fz_new_image_from_file(ctx, filename);
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(image);
}

JNIEXPORT jint JNICALL
FUN(Image_getWidth)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image ? image->w : 0;
}

JNIEXPORT jint JNICALL
FUN(Image_getHeight)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image ? image->h : 0;
}

JNIEXPORT jint JNICALL
FUN(Image_getNumberOfComponents)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image ? image->n : 0;
}

JNIEXPORT jobject JNICALL
FUN(Image_getColorSpace)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_image *image = from_Image(env, self);
	if (!ctx || !image) return NULL;
	return to_ColorSpace_safe(ctx, env, image->colorspace);
}

JNIEXPORT jint JNICALL
FUN(Image_getBitsPerComponent)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image ? image->bpc : 0;
}

JNIEXPORT jint JNICALL
FUN(Image_getXResolution)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image ? image->xres : 0;
}

JNIEXPORT jint JNICALL
FUN(Image_getYResolution)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image ? image->yres : 0;
}

JNIEXPORT jboolean JNICALL
FUN(Image_getImageMask)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image && image->imagemask ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(Image_getInterpolate)(JNIEnv *env, jobject self)
{
	fz_image *image = from_Image(env, self);
	return image && image->interpolate ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL
FUN(Image_getMask)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_image *img = from_Image(env, self);

	if (!ctx || !img) return NULL;

	return to_Image_safe(ctx, env, img->mask);
}

JNIEXPORT jobject JNICALL
FUN(Image_toPixmap)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_image *img = from_Image(env, self);
	fz_pixmap *pixmap = NULL;

	if (!ctx || !img) return NULL;

	fz_try(ctx)
		pixmap = fz_get_pixmap_from_image(ctx, img, NULL, NULL, NULL, NULL);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Pixmap_safe_own(ctx, env, pixmap);
}

/* Annotation Interface */

JNIEXPORT void JNICALL
FUN(Annotation_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_annot *annot = from_Annotation_safe(env, self);

	if (!ctx || !annot) return;

	fz_drop_annot(ctx, annot);
}

JNIEXPORT void JNICALL
FUN(Annotation_run)(JNIEnv *env, jobject self, jobject jdev, jobject jctm, jobject jcookie)
{
	fz_context *ctx = get_context(env);
	fz_annot *annot = from_Annotation(env, self);
	fz_device *dev = from_Device(env, jdev);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_cookie *cookie= from_Cookie(env, jcookie);
	NativeDeviceInfo *info;

	if (!ctx || !annot) return;
	if (!dev) { jni_throw_arg(env, "device must not be null"); return; }

	info = lockNativeDevice(env, jdev);
	fz_try(ctx)
		fz_run_annot(ctx, annot, dev, &ctm, cookie);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jlong JNICALL
FUN(Annotation_advance)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_annot *annot = from_Annotation(env, self);

	if (!ctx || !annot) return 0;

	fz_try(ctx)
		annot = fz_next_annot(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(annot);
}

JNIEXPORT jobject JNICALL
FUN(Annotation_toPixmap)(JNIEnv *env, jobject self, jobject jctm, jobject jcs, jboolean alpha)
{
	fz_context *ctx = get_context(env);
	fz_annot *annot = from_Annotation(env, self);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_pixmap *pixmap = NULL;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		pixmap = fz_new_pixmap_from_annot(ctx, annot, &ctm, cs, alpha);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Pixmap_safe_own(ctx, env, pixmap);
}

JNIEXPORT jobject JNICALL
FUN(Annotation_getBounds)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_annot *annot = from_Annotation(env, self);
	fz_rect rect;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		fz_bound_annot(ctx, annot, &rect);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Rect_safe(ctx, env, &rect);
}

JNIEXPORT jobject JNICALL
FUN(Annotation_toDisplayList)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_annot *annot = from_Annotation(env, self);
	fz_display_list *list = NULL;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		list = fz_new_display_list_from_annot(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_DisplayList_safe_own(ctx, env, list);
}

/* Document interface */

static char *make_tmp_gproof_path(const char *path)
{
	FILE *f;
	int i;
	char *buf = malloc(strlen(path) + 20 + 1);
	if (!buf)
		return NULL;

	for (i = 0; i < 10000; i++)
	{
		sprintf(buf, "%s.%d.gproof", path, i);

		LOGE("Trying for %s\n", buf);
		f = fopen(buf, "r");
		if (f != NULL)
		{
			fclose(f);
			continue;
		}

		f = fopen(buf, "w");
		if (f != NULL)
		{
			fclose(f);
			break;
		}
	}
	if (i == 10000)
	{
		LOGE("Failed to find temp gproof name");
		free(buf);
		return NULL;
	}

	LOGE("Rewritten to %s\n", buf);
	return buf;
}

JNIEXPORT jstring JNICALL
FUN(Document_proofNative)(JNIEnv *env, jobject self, jstring jCurrentPath, jstring jPrintProfile, jstring jDisplayProfile, jint inResolution)
{
#ifdef FZ_ENABLE_GPRF
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	char *tmp;
	jstring ret;
	const char *currentPath = NULL;
	const char *printProfile = NULL;
	const char *displayProfile = NULL;

	if (!ctx || !doc) return NULL;
	if (!jCurrentPath) { jni_throw_arg(env, "currentPath must not be null"); return NULL; }
	if (!jPrintProfile) { jni_throw_arg(env, "printProfile must not be null"); return NULL; }
	if (!jDisplayProfile) { jni_throw_arg(env, "displayProfile must not be null"); return NULL; }

	currentPath = (*env)->GetStringUTFChars(env, jCurrentPath, NULL);
	if (!currentPath)
		return NULL;

	printProfile = (*env)->GetStringUTFChars(env, jPrintProfile, NULL);
	if (!printProfile)
	{
		(*env)->ReleaseStringUTFChars(env, jCurrentPath, currentPath);
		return NULL;
	}

	displayProfile = (*env)->GetStringUTFChars(env, jDisplayProfile, NULL);
	if (!displayProfile)
	{
		(*env)->ReleaseStringUTFChars(env, jCurrentPath, currentPath);
		(*env)->ReleaseStringUTFChars(env, jPrintProfile, printProfile);
		return NULL;
	}

	tmp = make_tmp_gproof_path(currentPath);
	if (!tmp)
	{
		(*env)->ReleaseStringUTFChars(env, jCurrentPath, currentPath);
		(*env)->ReleaseStringUTFChars(env, jPrintProfile, printProfile);
		(*env)->ReleaseStringUTFChars(env, jDisplayProfile, displayProfile);
		return NULL;
	}

	fz_try(ctx)
	{
		LOGE("Creating %s\n", tmp);
		fz_save_gproof(ctx, currentPath, doc, tmp, inResolution, printProfile, displayProfile);
		ret = (*env)->NewStringUTF(env, tmp);
	}
	fz_always(ctx)
	{
		free(tmp);
		(*env)->ReleaseStringUTFChars(env, jCurrentPath, currentPath);
		(*env)->ReleaseStringUTFChars(env, jPrintProfile, printProfile);
		(*env)->ReleaseStringUTFChars(env, jDisplayProfile, displayProfile);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}
	return ret;
#else
	return NULL;
#endif
}

JNIEXPORT void JNICALL
FUN(Document_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document_safe(env, self);

	if (!ctx || !doc) return;

	fz_drop_document(ctx, doc);

	/* This is a reasonable place to call Memento. */
	Memento_fin();
}

JNIEXPORT jobject JNICALL
FUN(Document_openNativeWithPath)(JNIEnv *env, jclass cls, jstring jfilename)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = NULL;
	const char *filename = NULL;

	if (!ctx) return 0;
	if (jfilename)
	{
		filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
		if (!filename) return 0;
	}

	fz_try(ctx)
		doc = fz_open_document(ctx, filename);
	fz_always(ctx)
		if (filename)
			(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return to_Document_safe_own(ctx, env, doc);
}

JNIEXPORT jobject JNICALL
FUN(Document_openNativeWithBuffer)(JNIEnv *env, jclass cls, jobject jbuffer, jstring jmagic)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = NULL;
	const char *magic = NULL;
	fz_stream *stream = NULL;
	int n;
	jbyte *buffer = NULL;
	fz_buffer *buf = NULL;

	if (!ctx) return NULL;
	if (!jmagic) { jni_throw_arg(env, "magic must not be null"); return NULL; }

	magic = (*env)->GetStringUTFChars(env, jmagic, NULL);
	if (!magic) return NULL;

	n = (*env)->GetArrayLength(env, jbuffer);

	buffer = (*env)->GetByteArrayElements(env, jbuffer, NULL);
	if (!buffer) {
		if (magic)
			(*env)->ReleaseStringUTFChars(env, jmagic, magic);
		jni_throw_io(env, "cannot get bytes to read");
		return NULL;
	}

	fz_try(ctx)
	{
		buf = fz_new_buffer(ctx, n);
		fz_append_data(ctx, buf, buffer, n);
		stream = fz_open_buffer(ctx, buf);
		doc = fz_open_document_with_stream(ctx, magic, stream);
	}
	fz_always(ctx)
	{
		fz_drop_stream(ctx, stream);
		fz_drop_buffer(ctx, buf);
		(*env)->ReleaseByteArrayElements(env, jbuffer, buffer, 0);
		if (magic)
			(*env)->ReleaseStringUTFChars(env, jmagic, magic);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Document_safe_own(ctx, env, doc);
}

JNIEXPORT jboolean JNICALL
FUN(Document_recognize)(JNIEnv *env, jclass self, jstring jmagic)
{
	fz_context *ctx = get_context(env);
	const char *magic = NULL;
	jboolean recognized = JNI_FALSE;

	if (!ctx) return JNI_FALSE;
	if (jmagic)
	{
		magic = (*env)->GetStringUTFChars(env, jmagic, NULL);
		if (!magic) return JNI_FALSE;
	}

	fz_try(ctx)
		recognized = fz_recognize_document(ctx, magic) != NULL;
	fz_always(ctx)
		if (magic)
			(*env)->ReleaseStringUTFChars(env, jmagic, magic);
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	return recognized;
}

JNIEXPORT jboolean JNICALL
FUN(Document_needsPassword)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	int okay = 0;

	if (!ctx || !doc) return JNI_FALSE;

	fz_try(ctx)
		okay = fz_needs_password(ctx, doc);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return okay ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(Document_authenticatePassword)(JNIEnv *env, jobject self, jstring jpassword)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	const char *password = NULL;
	int okay = 0;

	if (!ctx || !doc) return JNI_FALSE;

	if (jpassword)
	{
		password = (*env)->GetStringUTFChars(env, jpassword, NULL);
		if (!password) return JNI_FALSE;
	}

	fz_try(ctx)
		okay = fz_authenticate_password(ctx, doc, password);
	fz_always(ctx)
		if (password)
			(*env)->ReleaseStringUTFChars(env, jpassword, password);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return okay ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
FUN(Document_countPages)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	int count = 0;

	if (!ctx || !doc) return 0;

	fz_try(ctx)
		count = fz_count_pages(ctx, doc);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return count;
}

JNIEXPORT jboolean JNICALL
FUN(Document_isReflowable)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	int is_reflowable = 0;

	if (!ctx || !doc) return JNI_FALSE;

	fz_try(ctx)
		is_reflowable = fz_is_document_reflowable(ctx, doc);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return is_reflowable ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
FUN(Document_layout)(JNIEnv *env, jobject self, jfloat w, jfloat h, jfloat em)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);

	if (!ctx || !doc) return;

	fz_try(ctx)
		fz_layout_document(ctx, doc, w, h, em);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(Document_loadPage)(JNIEnv *env, jobject self, jint number)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	fz_page *page = NULL;

	if (!ctx || !doc) return NULL;

	fz_try(ctx)
		page = fz_load_page(ctx, doc, number);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Page_safe_own(ctx, env, page);
}

JNIEXPORT jobject JNICALL
FUN(Document_getMetaData)(JNIEnv *env, jobject self, jstring jkey)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	const char *key = NULL;
	char info[256];

	if (!ctx || !doc) return NULL;
	if (!jkey) { jni_throw_arg(env, "key must not be null"); return NULL; }

	key = (*env)->GetStringUTFChars(env, jkey, NULL);
	if (!key) return 0;

	fz_try(ctx)
		fz_lookup_metadata(ctx, doc, key, info, sizeof info);
	fz_always(ctx)
		if (key)
			(*env)->ReleaseStringUTFChars(env, jkey, key);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jboolean JNICALL
FUN(Document_isUnencryptedPDF)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	pdf_document *idoc = pdf_specifics(ctx, doc);
	int cryptVer;

	if (!ctx || !doc) return JNI_FALSE;
	if (!idoc)
		return JNI_FALSE;

	cryptVer = pdf_crypt_version(ctx, idoc);
	return (cryptVer == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL
FUN(Document_loadOutline)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	fz_outline *outline = NULL;
	jobject joutline = NULL;

	if (!ctx || !doc) return NULL;

	fz_var(outline);

	fz_try(ctx)
		outline = fz_load_outline(ctx, doc);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	if (outline)
	{
		joutline = to_Outline_safe(ctx, env, doc, outline);
		if (!joutline)
			jni_throw(env, FZ_ERROR_GENERIC, "loadOutline failed");
		fz_drop_outline(ctx, outline);
	}

	return joutline;
}

JNIEXPORT jlong JNICALL
FUN(Document_makeBookmark)(JNIEnv *env, jobject self, jint page)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	fz_bookmark mark = 0;

	fz_try(ctx)
		mark = fz_make_bookmark(ctx, doc, page);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return mark;
}

JNIEXPORT jint JNICALL
FUN(Document_findBookmark)(JNIEnv *env, jobject self, jlong mark)
{
	fz_context *ctx = get_context(env);
	fz_document *doc = from_Document(env, self);
	int page = -1;

	fz_try(ctx)
		page = fz_lookup_bookmark(ctx, doc, mark);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return -1;
	}

	return page;
}

/* Page interface */

JNIEXPORT void JNICALL
FUN(Page_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page_safe(env, self);

	if (!ctx || !page) return;

	fz_drop_page(ctx, page);
}

JNIEXPORT jobject JNICALL
FUN(Page_toPixmap)(JNIEnv *env, jobject self, jobject jctm, jobject jcs, jboolean alpha)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_pixmap *pixmap = NULL;

	if (!ctx || !page) return NULL;

	fz_try(ctx)
		pixmap = fz_new_pixmap_from_page(ctx, page, &ctm, cs, alpha);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Pixmap_safe_own(ctx, env, pixmap);
}

JNIEXPORT jobject JNICALL
FUN(Page_getBounds)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_rect rect;

	if (!ctx || !page) return NULL;

	fz_try(ctx)
		fz_bound_page(ctx, page, &rect);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Rect_safe(ctx, env, &rect);
}

JNIEXPORT void JNICALL
FUN(Page_run)(JNIEnv *env, jobject self, jobject jdev, jobject jctm, jobject jcookie)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_device *dev = from_Device(env, jdev);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_cookie *cookie = from_Cookie(env, jcookie);
	NativeDeviceInfo *info;

	if (!ctx || !page) return;
	if (!dev) { jni_throw_arg(env, "device must not be null"); return; }

	info = lockNativeDevice(env, jdev);
	fz_try(ctx)
		fz_run_page(ctx, page, dev, &ctm, cookie);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Page_runPageContents)(JNIEnv *env, jobject self, jobject jdev, jobject jctm, jobject jcookie)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_device *dev = from_Device(env, jdev);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_cookie *cookie = from_Cookie(env, jcookie);
	NativeDeviceInfo *info;

	if (!ctx || !page) return;
	if (!dev) { jni_throw_arg(env, "device must not be null"); return; }

	info = lockNativeDevice(env, jdev);
	fz_try(ctx)
		fz_run_page_contents(ctx, page, dev, &ctm, cookie);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(Page_getAnnotations)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_annot *annot = NULL;
	fz_annot *annots = NULL;
	jobject jannots = NULL;
	int annot_count;
	int i;

	if (!ctx || !page) return NULL;

	/* count the annotations */
	fz_try(ctx)
	{
		annots = fz_first_annot(ctx, page);

		annot = annots;
		for (annot_count = 0; annot; annot_count++)
			annot = fz_next_annot(ctx, annot);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	/* no annotations, return NULL instead of empty array */
	if (annot_count == 0)
		return NULL;

	/* now run through actually creating the annotation objects */
	jannots = (*env)->NewObjectArray(env, annot_count, cls_Annotation, NULL);
	if (!jannots) return NULL;

	annot = annots;
	for (i = 0; annot && i < annot_count; i++)
	{
		jobject jannot = to_Annotation_safe(ctx, env, annot);
		if (!jannot) return NULL;

		(*env)->SetObjectArrayElement(env, jannots, i, jannot);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, jannot);

		fz_try(ctx)
			annot = fz_next_annot(ctx, annot);
		fz_catch(ctx)
		{
			jni_rethrow(env, ctx);
			return NULL;
		}
	}

	return jannots;
}

JNIEXPORT jobject JNICALL
FUN(Page_getLinks)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_link *link = NULL;
	fz_link *links = NULL;
	jobject jlinks = NULL;
	int link_count;
	int i;

	if (!ctx || !page) return NULL;

	fz_var(links);

	fz_try(ctx)
		links = fz_load_links(ctx, page);
	fz_catch(ctx)
	{
		fz_drop_link(ctx, links);
		jni_rethrow(env, ctx);
		return NULL;
	}

	/* count the links */
	link = links;
	for (link_count = 0; link; link_count++)
		link = link->next;

	/* no links, return NULL instead of empty array */
	if (link_count == 0)
	{
		fz_drop_link(ctx, links);
		return NULL;
	}

	/* now run through actually creating the link objects */
	jlinks = (*env)->NewObjectArray(env, link_count, cls_Link, NULL);
	if (!jlinks) return NULL;

	link = links;
	for (i = 0; link && i < link_count; i++)
	{
		jobject jbounds = NULL;
		jobject jlink = NULL;
		jobject juri = NULL;
		int page = 0;

		jbounds = to_Rect_safe(ctx, env, &link->rect);
		if (!jbounds) return NULL;

		if (fz_is_external_link(ctx, link->uri))
		{
			juri = (*env)->NewStringUTF(env, link->uri);
			if (!juri) return NULL;
		}
		else
			page = fz_resolve_link(ctx, link->doc, link->uri, NULL, NULL);

		jlink = (*env)->NewObject(env, cls_Link, mid_Link_init, jbounds, page, juri);
		(*env)->DeleteLocalRef(env, jbounds);
		if (!jlink) return NULL;
		if (juri)
			(*env)->DeleteLocalRef(env, juri);

		(*env)->SetObjectArrayElement(env, jlinks, i, jlink);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, jlink);
		link = link->next;
	}

	fz_drop_link(ctx, links);

	return jlinks;
}

JNIEXPORT jobject JNICALL
FUN(Page_search)(JNIEnv *env, jobject self, jstring jneedle)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_rect hits[256];
	const char *needle = NULL;
	int n = 0;

	if (!ctx || !page) return NULL;
	if (!jneedle) { jni_throw_arg(env, "needle must not be null"); return NULL; }

	needle = (*env)->GetStringUTFChars(env, jneedle, NULL);
	if (!needle) return 0;

	fz_try(ctx)
		n = fz_search_page(ctx, page, needle, hits, nelem(hits));
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jneedle, needle);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_jRectArray_safe(ctx, env, hits, n);
}

JNIEXPORT jobject JNICALL
FUN(Page_toDisplayList)(JNIEnv *env, jobject self, jboolean no_annotations)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_display_list *list = NULL;

	if (!ctx || !page) return NULL;

	fz_try(ctx)
		if (no_annotations)
			list = fz_new_display_list_from_page_contents(ctx, page);
		else
			list = fz_new_display_list_from_page(ctx, page);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_DisplayList_safe_own(ctx, env, list);
}

JNIEXPORT jobject JNICALL
FUN(Page_toStructuredText)(JNIEnv *env, jobject self, jstring joptions)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
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
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_StructuredText_safe_own(ctx, env, text);
}


JNIEXPORT jbyteArray JNICALL
FUN(Page_textAsHtml)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_page *page = from_Page(env, self);
	fz_stext_page *text = NULL;
	fz_device *dev = NULL;
	fz_matrix ctm;
	jbyteArray arr = NULL;
	fz_buffer *buf = NULL;
	fz_output *out = NULL;
	unsigned char *data;
	size_t len;

	if (!ctx || !page) return NULL;

	fz_var(text);
	fz_var(dev);
	fz_var(buf);
	fz_var(out);

	fz_try(ctx)
	{
		fz_rect mediabox;

		ctm = fz_identity;
		text = fz_new_stext_page(ctx, fz_bound_page(ctx, page, &mediabox));
		dev = fz_new_stext_device(ctx, text, NULL);
		fz_run_page(ctx, page, dev, &ctm, NULL);
		fz_close_device(ctx, dev);

		buf = fz_new_buffer(ctx, 256);
		out = fz_new_output_with_buffer(ctx, buf);
		fz_print_stext_header_as_html(ctx, out);
		fz_print_stext_page_as_html(ctx, out, text);
		fz_print_stext_trailer_as_html(ctx, out);
		fz_close_output(ctx, out);
	}
	fz_always(ctx)
	{
		fz_drop_output(ctx, out);
		fz_drop_device(ctx, dev);
		fz_drop_stext_page(ctx, text);
	}
	fz_catch(ctx)
	{
		fz_drop_buffer(ctx, buf);
		jni_rethrow(env, ctx);
		return NULL;
	}

	len = fz_buffer_storage(ctx, buf, &data);
	arr = (*env)->NewByteArray(env, (jsize)len);
	if (arr)
	{
		(*env)->SetByteArrayRegion(env, arr, 0, (jsize)len, (jbyte *)data);
	}
	fz_drop_buffer(ctx, buf);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return arr;
}

/* Cookie interface */

JNIEXPORT void JNICALL
FUN(Cookie_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_cookie *cookie = from_Cookie_safe(env, self);

	if (!ctx || !cookie) return;

	fz_free(ctx, cookie);
}

JNIEXPORT jlong JNICALL
FUN(Cookie_newNative)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_cookie *cookie = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		cookie = fz_malloc_struct(ctx, fz_cookie);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(cookie);
}

JNIEXPORT void JNICALL
FUN(Cookie_abort)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_cookie *cookie = from_Cookie(env, self);

	if (!ctx || !cookie) return;

	cookie->abort = 1;
}

/* DisplayList interface */

JNIEXPORT jlong JNICALL
FUN(DisplayList_newNative)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);

	fz_display_list *list = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		list = fz_new_display_list(ctx, NULL);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(list);
}

JNIEXPORT void JNICALL
FUN(DisplayList_run)(JNIEnv *env, jobject self, jobject jdev, jobject jctm, jobject jrect, jobject jcookie)
{
	fz_context *ctx = get_context(env);
	fz_display_list *list = from_DisplayList(env, self);
	fz_device *dev = from_Device(env, jdev);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_cookie *cookie = from_Cookie(env, jcookie);
	NativeDeviceInfo *info;
	fz_rect local_rect;
	fz_rect *rect = NULL;

	if (!ctx || !list) return;
	if (!dev) { jni_throw_arg(env, "device must not be null"); return; }

	/* Use a scissor rectangle if one is supplied */
	if (jrect)
	{
		rect = &local_rect;
		local_rect = from_Rect(env, jrect);
	}

	info = lockNativeDevice(env, jdev);
	fz_try(ctx)
		fz_run_display_list(ctx, list, dev, &ctm, rect, cookie);
	fz_always(ctx)
		unlockNativeDevice(env, info);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(DisplayList_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_display_list *list = from_DisplayList_safe(env, self);

	if (!ctx || !list) return;

	fz_drop_display_list(ctx, list);
}

JNIEXPORT jobject JNICALL
FUN(DisplayList_toPixmap)(JNIEnv *env, jobject self, jobject jctm, jobject jcs, jboolean alpha)
{
	fz_context *ctx = get_context(env);
	fz_display_list *list = from_DisplayList(env, self);
	fz_matrix ctm = from_Matrix(env, jctm);
	fz_colorspace *cs = from_ColorSpace(env, jcs);
	fz_pixmap *pixmap = NULL;

	if (!ctx || !list) return NULL;

	fz_try(ctx)
		pixmap = fz_new_pixmap_from_display_list(ctx, list, &ctm, cs, alpha);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Pixmap_safe_own(ctx, env, pixmap);
}

JNIEXPORT jobject JNICALL
FUN(DisplayList_toStructuredText)(JNIEnv *env, jobject self, jstring joptions)
{
	fz_context *ctx = get_context(env);
	fz_display_list *list = from_DisplayList(env, self);
	fz_stext_page *text = NULL;
	const char *options= NULL;
	fz_stext_options opts;

	if (!ctx || !list) return NULL;

	if (joptions)
	{
		options = (*env)->GetStringUTFChars(env, joptions, NULL);
		if (!options) return NULL;
	}

	fz_try(ctx)
	{
		fz_parse_stext_options(ctx, &opts, options);
		text = fz_new_stext_page_from_display_list(ctx, list, &opts);
	}
	fz_always(ctx)
	{
		if (options)
			(*env)->ReleaseStringUTFChars(env, joptions, options);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_StructuredText_safe_own(ctx, env, text);
}

JNIEXPORT jobject JNICALL
FUN(DisplayList_search)(JNIEnv *env, jobject self, jstring jneedle)
{
	fz_context *ctx = get_context(env);
	fz_display_list *list = from_DisplayList(env, self);
	fz_rect hits[256];
	const char *needle = NULL;
	int n = 0;

	if (!ctx || !list) return NULL;
	if (!jneedle) { jni_throw_arg(env, "needle must not be null"); return NULL; }

	needle = (*env)->GetStringUTFChars(env, jneedle, NULL);
	if (!needle) return NULL;

	fz_try(ctx)
		n = fz_search_display_list(ctx, list, needle, hits, nelem(hits));
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jneedle, needle);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_jRectArray_safe(ctx, env, hits, n);
}

/* Buffer interface */

JNIEXPORT void JNICALL
FUN(Buffer_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer_safe(env, self);

	if (!ctx || !buf) return;

	fz_drop_buffer(ctx, buf);
}

JNIEXPORT jlong JNICALL
FUN(Buffer_newNativeBuffer)(JNIEnv *env, jobject self, jint n)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		buf = fz_new_buffer(ctx, n);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(buf);
}

JNIEXPORT jint JNICALL
FUN(Buffer_getLength)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);

	if (!ctx || !buf) return -1;

	return (jint)fz_buffer_storage(ctx, buf, NULL);
}

JNIEXPORT jint JNICALL
FUN(Buffer_readByte)(JNIEnv *env, jobject self, jint jat)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	size_t at = (size_t) jat;
	size_t len;
	unsigned char *data;

	if (!ctx || !buf) return -1;
	if (jat < 0) { jni_throw_oob(env, "at is negative"); return -1; }

	len = fz_buffer_storage(ctx, buf, &data);
	if (at >= len)
		return -1;

	return data[at];
}

JNIEXPORT jint JNICALL
FUN(Buffer_readBytes)(JNIEnv *env, jobject self, jint jat, jobject jbs)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	size_t at = (size_t) jat;
	jbyte *bs = NULL;
	size_t len;
	size_t remaining_input = 0;
	size_t remaining_output = 0;
	unsigned char *data;

	if (!ctx || !buf) return -1;
	if (jat < 0) { jni_throw_oob(env, "at is negative"); return -1; }
	if (!jbs) { jni_throw_arg(env, "buffer must not be null"); return -1; }

	len = fz_buffer_storage(ctx, buf, &data);
	if (at >= len)
		return -1;

	remaining_input = len - at;
	remaining_output = (*env)->GetArrayLength(env, jbs);
	len = fz_minz(0, remaining_output);
	len = fz_minz(len, remaining_input);

	bs = (*env)->GetByteArrayElements(env, jbs, NULL);
	if (!bs) { jni_throw_io(env, "cannot get bytes to read"); return -1; }

	memcpy(bs, &data[at], len);
	(*env)->ReleaseByteArrayElements(env, jbs, bs, 0);

	return (jint)len;
}

JNIEXPORT jint JNICALL
FUN(Buffer_readBytesInto)(JNIEnv *env, jobject self, jint jat, jobject jbs, jint joff, jint jlen)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	size_t at = (size_t) jat;
	jbyte *bs = NULL;
	size_t off = (size_t) joff;
	size_t len = (size_t) jlen;
	size_t bslen = 0;
	size_t blen;
	unsigned char *data;

	if (!ctx || !buf) return -1;
	if (jat < 0) { jni_throw_oob(env, "at is negative"); return -1; }
	if (!jbs) { jni_throw_arg(env, "buffer must not be null"); return -1; }
	if (joff < 0) { jni_throw_oob(env, "offset is negative"); return -1; }
	if (jlen < 0) { jni_throw_oob(env, "length is negative"); return -1; }

	bslen = (*env)->GetArrayLength(env, jbs);
	if (len > bslen - off) { jni_throw_oob(env, "offset + length is outside of buffer"); return -1; }

	blen = fz_buffer_storage(ctx, buf, &data);
	if (at >= blen)
		return -1;

	len = fz_minz(len, blen - at);

	bs = (*env)->GetByteArrayElements(env, jbs, NULL);
	if (!bs) { jni_throw_io(env, "cannot get bytes to read"); return -1; }

	memcpy(&bs[off], &data[at], len);
	(*env)->ReleaseByteArrayElements(env, jbs, bs, 0);

	return (jint)len;
}

JNIEXPORT void JNICALL
FUN(Buffer_writeByte)(JNIEnv *env, jobject self, jbyte b)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);

	if (!ctx || !buf) return;

	fz_try(ctx)
		fz_append_byte(ctx, buf, b);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Buffer_writeBytes)(JNIEnv *env, jobject self, jobject jbs)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	jsize len = 0;
	jbyte *bs = NULL;

	if (!ctx || !buf) return;
	if (!jbs) { jni_throw_arg(env, "buffer must not be null"); return; }

	len = (*env)->GetArrayLength(env, jbs);
	bs = (*env)->GetByteArrayElements(env, jbs, NULL);
	if (!bs) { jni_throw_io(env, "cannot get bytes to write"); return; }

	fz_try(ctx)
		fz_append_data(ctx, buf, bs, len);
	fz_always(ctx)
		(*env)->ReleaseByteArrayElements(env, jbs, bs, JNI_ABORT);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Buffer_writeBytesFrom)(JNIEnv *env, jobject self, jobject jbs, jint joff, jint jlen)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	jbyte *bs = NULL;
	jsize off = (jsize) joff;
	jsize len = (jsize) jlen;
	jsize bslen = 0;

	if (!ctx || !buf) return;
	if (!jbs) { jni_throw_arg(env, "buffer must not be null"); return; }

	bslen = (*env)->GetArrayLength(env, jbs);
	if (joff < 0)
	{
		jni_throw_oob(env, "offset is negative");
		return;
	}
	if (jlen < 0)
	{
		jni_throw_oob(env, "length is negative");
		return;
	}
	if (off + len >= bslen)
	{
		jni_throw_oob(env, "offset + length is outside of buffer");
		return;
	}

	bs = (*env)->GetByteArrayElements(env, jbs, NULL);
	if (!bs) { jni_throw_io(env, "cannot get bytes to write"); return; }

	fz_try(ctx)
		fz_append_data(ctx, buf, &bs[off], len);
	fz_always(ctx)
		(*env)->ReleaseByteArrayElements(env, jbs, bs, JNI_ABORT);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Buffer_writeBuffer)(JNIEnv *env, jobject self, jobject jbuf)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	fz_buffer *cat = from_Buffer(env, jbuf);

	if (!ctx || !buf) return;
	if (!cat) { jni_throw_arg(env, "buffer must not be null"); return; }

	fz_try(ctx)
		fz_append_buffer(ctx, buf, cat);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Buffer_writeRune)(JNIEnv *env, jobject self, jint rune)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);

	if (!ctx || !buf) return;

	fz_try(ctx)
		fz_append_rune(ctx, buf, rune);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Buffer_writeLine)(JNIEnv *env, jobject self, jstring jline)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	const char *line = NULL;

	if (!ctx || !buf) return;
	if (!jline) { jni_throw_arg(env, "line must not be null"); return; }

	line = (*env)->GetStringUTFChars(env, jline, NULL);
	if (!line) return;

	fz_try(ctx)
	{
		fz_append_string(ctx, buf, line);
		fz_append_byte(ctx, buf, '\n');
	}
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jline, line);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(Buffer_writeLines)(JNIEnv *env, jobject self, jobject jlines)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	int i = 0;
	jsize len = 0;

	if (!ctx || !buf) return;
	if (!jlines) { jni_throw_arg(env, "lines must not be null"); return; }

	len = (*env)->GetArrayLength(env, jlines);

	for (i = 0; i < len; ++i)
	{
		const char *line;
		jobject jline;

		jline = (*env)->GetObjectArrayElement(env, jlines, i);
		if ((*env)->ExceptionCheck(env)) return;

		if (!jline)
			continue;
		line = (*env)->GetStringUTFChars(env, jline, NULL);
		if (!line) return;

		fz_try(ctx)
		{
			fz_append_string(ctx, buf, line);
			fz_append_byte(ctx, buf, '\n');
		}
		fz_always(ctx)
			(*env)->ReleaseStringUTFChars(env, jline, line);
		fz_catch(ctx)
		{
			jni_rethrow(env, ctx);
			return;
		}
	}
}

JNIEXPORT void JNICALL
FUN(Buffer_save)(JNIEnv *env, jobject self, jstring jfilename)
{
	fz_context *ctx = get_context(env);
	fz_buffer *buf = from_Buffer(env, self);
	const char *filename = NULL;

	if (!ctx || !buf) return;
	if (jfilename)
	{
		filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
		if (!filename) return;
	}

	fz_try(ctx)
		fz_save_buffer(ctx, buf, filename);
	fz_always(ctx)
		if (filename)
			(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

/* DocumentWriter interface */

JNIEXPORT void JNICALL
FUN(DocumentWriter_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document_writer *wri = from_DocumentWriter_safe(env, self);

	if (!ctx || !wri) return;

	fz_drop_document_writer(ctx, wri);
}

JNIEXPORT jlong JNICALL
FUN(DocumentWriter_newNativeDocumentWriter)(JNIEnv *env, jobject self, jstring jfilename, jstring jformat, jstring joptions)
{
	fz_context *ctx = get_context(env);
	fz_document_writer *wri = from_DocumentWriter(env, self);
	const char *filename = NULL;
	const char *format = NULL;
	const char *options = NULL;

	if (!ctx || !wri) return 0;
	if (!jfilename) { jni_throw_arg(env, "filename must not be null"); return 0; }

	filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
	if (!filename) return 0;

	if (jformat)
	{
		format = (*env)->GetStringUTFChars(env, jformat, NULL);
		if (!format)
		{
			(*env)->ReleaseStringUTFChars(env, jfilename, filename);
			return 0;
		}
	}
	if (joptions)
	{
		options = (*env)->GetStringUTFChars(env, joptions, NULL);
		if (!options)
		{
			if (format)
				(*env)->ReleaseStringUTFChars(env, jformat, format);
			(*env)->ReleaseStringUTFChars(env, jfilename, filename);
			return 0;
		}
	}

	fz_try(ctx)
		wri = fz_new_document_writer(ctx, filename, format, options);
	fz_always(ctx)
	{
		if (options)
			(*env)->ReleaseStringUTFChars(env, joptions, options);
		if (format)
			(*env)->ReleaseStringUTFChars(env, jformat, format);
		(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(wri);
}

JNIEXPORT jobject JNICALL
FUN(DocumentWriter_beginPage)(JNIEnv *env, jobject self, jobject jmediabox)
{
	fz_context *ctx = get_context(env);
	fz_document_writer *wri = from_DocumentWriter(env, self);
	fz_rect mediabox = from_Rect(env, jmediabox);
	fz_device *device = NULL;

	if (!ctx || !wri) return NULL;

	fz_try(ctx)
		device = fz_begin_page(ctx, wri, &mediabox);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Device_safe_own(ctx, env, device);
}

JNIEXPORT void JNICALL
FUN(DocumentWriter_endPage)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document_writer *wri = from_DocumentWriter(env, self);

	if (!ctx || !wri) return;

	fz_try(ctx)
		fz_end_page(ctx, wri);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(DocumentWriter_close)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_document_writer *wri = from_DocumentWriter(env, self);

	if (!ctx || !wri) return;

	fz_try(ctx)
		fz_close_document_writer(ctx, wri);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

/* StructuredText interface */

JNIEXPORT void JNICALL
FUN(StructuredText_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_stext_page *text = from_StructuredText_safe(env, self);

	if (!ctx || !text) return;

	fz_drop_stext_page(ctx, text);
}

JNIEXPORT jobject JNICALL
FUN(StructuredText_search)(JNIEnv *env, jobject self, jstring jneedle)
{
	fz_context *ctx = get_context(env);
	fz_stext_page *text = from_StructuredText(env, self);
	fz_rect hits[256];
	const char *needle = NULL;
	int n = 0;

	if (!ctx || !text) return NULL;
	if (!jneedle) { jni_throw_arg(env, "needle must not be null"); return NULL; }

	needle = (*env)->GetStringUTFChars(env, jneedle, NULL);
	if (!needle) return NULL;

	fz_try(ctx)
		n = fz_search_stext_page(ctx, text, needle, hits, nelem(hits));
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jneedle, needle);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_jRectArray_safe(ctx, env, hits, n);
}

JNIEXPORT jobject JNICALL
FUN(StructuredText_highlight)(JNIEnv *env, jobject self, jobject jpt1, jobject jpt2)
{
	fz_context *ctx = get_context(env);
	fz_stext_page *text = from_StructuredText(env, self);
	fz_point pt1 = from_Point(env, jpt1);
	fz_point pt2 = from_Point(env, jpt2);
	fz_rect hits[1000];
	int n = 0;

	if (!ctx || !text) return NULL;

	fz_try(ctx)
		n = fz_highlight_selection(ctx, text, pt1, pt2, hits, nelem(hits));
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_jRectArray_safe(ctx, env, hits, n);
}

JNIEXPORT jobject JNICALL
FUN(StructuredText_copy)(JNIEnv *env, jobject self, jobject jpt1, jobject jpt2)
{
	fz_context *ctx = get_context(env);
	fz_stext_page *text = from_StructuredText(env, self);
	fz_point pt1 = from_Point(env, jpt1);
	fz_point pt2 = from_Point(env, jpt2);
	jobject jstring = NULL;
	char *s = NULL;

	if (!ctx || !text) return NULL;

	fz_var(s);

	fz_try(ctx)
		s = fz_copy_selection(ctx, text, pt1, pt2, 0);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jstring = (*env)->NewStringUTF(env, s);
	fz_free(ctx, s);

	return jstring;
}

JNIEXPORT jobject JNICALL
FUN(StructuredText_getBlocks)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_stext_page *page = from_StructuredText(env, self);

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
		jblock = (*env)->NewObject(env, cls_TextBlock, mid_TextBlock_init, self);
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
			jline = (*env)->NewObject(env, cls_TextLine, mid_TextLine_init, self);
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
				jchar = (*env)->NewObject(env, cls_TextChar, mid_TextChar_init, self);
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

/* PDFDocument interface */

JNIEXPORT jlong JNICALL
FUN(PDFDocument_newNative)(JNIEnv *env, jclass cls)
{
	fz_context *ctx = get_context(env);
	pdf_document *doc = NULL;

	if (!ctx) return 0;

	fz_try(ctx)
		doc = pdf_create_document(ctx);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return jlong_cast(doc);
}

JNIEXPORT void JNICALL
FUN(PDFDocument_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument_safe(env, self);

	if (!ctx || !pdf) return;

	fz_drop_document(ctx, &pdf->super);
}

JNIEXPORT jint JNICALL
FUN(PDFDocument_countObjects)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	int count = 0;

	if (!ctx || !pdf) return 0;

	fz_try(ctx)
		count = pdf_xref_len(ctx, pdf);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return count;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newNull)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_null(ctx, pdf);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newBoolean)(JNIEnv *env, jobject self, jboolean b)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_bool(ctx, pdf, b);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newInteger)(JNIEnv *env, jobject self, jint i)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_int(ctx, pdf, i);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newReal)(JNIEnv *env, jobject self, jfloat f)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_real(ctx, pdf, f);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newString)(JNIEnv *env, jobject self, jstring jstring)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	const char *s = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;
	if (!jstring) { jni_throw_arg(env, "string must not be null"); return NULL; }

	s = (*env)->GetStringUTFChars(env, jstring, NULL);
	if (!s) return NULL;

	fz_try(ctx)
		obj = pdf_new_text_string(ctx, pdf, s);
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jstring, s);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newByteString)(JNIEnv *env, jobject self, jobject jbs)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jbyte *bs;
	size_t bslen;
	jobject jobj;

	if (!ctx || !pdf) return NULL;
	if (!jbs) { jni_throw_arg(env, "bs must not be null"); return NULL; }

	bslen = (*env)->GetArrayLength(env, jbs);

	fz_try(ctx)
		bs = fz_malloc(ctx, bslen);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	(*env)->GetByteArrayRegion(env, jbs, 0, bslen, bs);
	if ((*env)->ExceptionCheck(env)) {
		fz_free(ctx, bs);
		return NULL;
	}

	fz_try(ctx)
		obj = pdf_new_string(ctx, pdf, (char *) bs, bslen);
	fz_always(ctx)
		fz_free(ctx, bs);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newName)(JNIEnv *env, jobject self, jstring jname)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	const char *name = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;
	if (!jname) { jni_throw_arg(env, "name must not be null"); return NULL; }

	name = (*env)->GetStringUTFChars(env, jname, NULL);
	if (!name) return NULL;

	fz_try(ctx)
		obj = pdf_new_name(ctx, pdf, name);
	fz_always(ctx)
		(*env)->ReleaseStringUTFChars(env, jname, name);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newIndirect)(JNIEnv *env, jobject self, jint num, jint gen)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_indirect(ctx, pdf, num, gen);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newArray)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_array(ctx, pdf, 0);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newDictionary)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;
	jobject jobj;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_new_dict(ctx, pdf, 0);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, obj);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_findPage)(JNIEnv *env, jobject self, jint jat)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;

	if (!ctx || !pdf) return NULL;
	if (jat < 0 || jat >= pdf_count_pages(ctx, pdf)) { jni_throw_oob(env, "at is not a valid page"); return NULL; }

	fz_try(ctx)
		obj = pdf_lookup_page_obj(ctx, pdf, jat);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe(ctx, env, self, obj);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_getTrailer)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = NULL;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		obj = pdf_trailer(ctx, pdf);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe(ctx, env, self, obj);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addObject)(JNIEnv *env, jobject self, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = from_PDFObject(env, jobj);

	if (!ctx || !pdf) return NULL;
	if (!jobj) { jni_throw_arg(env, "object must not be null"); return NULL; }

	fz_try(ctx)
		obj = pdf_add_object_drop(ctx, pdf, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_createObject)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		ind = pdf_new_indirect(ctx, pdf, pdf_create_object(ctx, pdf), 0);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT void JNICALL
FUN(PDFDocument_deleteObject)(JNIEnv *env, jobject self, jint num)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);

	if (!ctx || !pdf) return;

	fz_try(ctx)
		pdf_delete_object(ctx, pdf, num);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_newPDFGraftMap)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_graft_map *map = NULL;

	if (!ctx || !pdf) return NULL;

	fz_try(ctx)
		map = pdf_new_graft_map(ctx, pdf);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFGraftMap_safe_own(ctx, env, self, map);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_graftObject)(JNIEnv *env, jobject self, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, jobj);
	pdf_document *dst = from_PDFDocument(env, self);

	if (!ctx || !dst) return NULL;
	if (!dst) { jni_throw_arg(env, "dst must not be null"); return NULL; }

	fz_try(ctx)
		obj = pdf_graft_object(ctx, dst, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, obj);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addStreamBuffer)(JNIEnv *env, jobject self, jobject jbuf, jobject jobj, jboolean compressed)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = from_PDFObject(env, jobj);
	fz_buffer *buf = from_Buffer(env, jbuf);
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!jbuf) { jni_throw_arg(env, "buffer must not be null"); return NULL; }

	fz_try(ctx)
		ind = pdf_add_stream(ctx, pdf, buf, obj, compressed);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addStreamString)(JNIEnv *env, jobject self, jstring jbuf, jobject jobj, jboolean compressed)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	pdf_obj *obj = from_PDFObject(env, jobj);
	fz_buffer *buf = NULL;
	const char *sbuf = NULL;
	unsigned char *data = NULL;
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!jbuf) { jni_throw_arg(env, "buffer must not be null"); return NULL; }

	sbuf = (*env)->GetStringUTFChars(env, jbuf, NULL);
	if (!sbuf) return NULL;

	fz_var(buf);

	fz_try(ctx)
	{
		size_t len = strlen(sbuf);
		data = fz_malloc(ctx, len);
		memcpy(data, sbuf, len);
		buf = fz_new_buffer_from_data(ctx, data, len);
		ind = pdf_add_stream(ctx, pdf, buf, obj, compressed);
	}
	fz_always(ctx)
	{
		fz_drop_buffer(ctx, buf);
		(*env)->ReleaseStringUTFChars(env, jbuf, sbuf);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addPageBuffer)(JNIEnv *env, jobject self, jobject jmediabox, jint rotate, jobject jresources, jobject jcontents)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	fz_rect mediabox = from_Rect(env, jmediabox);
	pdf_obj *resources = from_PDFObject(env, jresources);
	fz_buffer *contents = from_Buffer(env, jcontents);
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!resources) { jni_throw_arg(env, "resources must not be null"); return NULL; }
	if (!contents) { jni_throw_arg(env, "contents must not be null"); return NULL; }

	fz_try(ctx)
		ind = pdf_add_page(ctx, pdf, &mediabox, rotate, resources, contents);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addPageString)(JNIEnv *env, jobject self, jobject jmediabox, jint rotate, jobject jresources, jstring jcontents)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	fz_rect mediabox = from_Rect(env, jmediabox);
	pdf_obj *resources = from_PDFObject(env, jresources);
	const char *scontents = NULL;
	fz_buffer *contents = NULL;
	unsigned char *data = NULL;
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!resources) { jni_throw_arg(env, "resources must not be null"); return NULL; }
	if (!contents) { jni_throw_arg(env, "contents must not be null"); return NULL; }

	scontents = (*env)->GetStringUTFChars(env, jcontents, NULL);
	if (!scontents) return NULL;

	fz_var(contents);

	fz_try(ctx)
	{
		size_t len = strlen(scontents);
		data = fz_malloc(ctx, len);
		contents = fz_new_buffer_from_data(ctx, data, len);
		ind = pdf_add_page(ctx, pdf, &mediabox, rotate, resources, contents);
	}
	fz_always(ctx)
	{
		fz_drop_buffer(ctx, contents);
		(*env)->ReleaseStringUTFChars(env, jcontents, scontents);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT void JNICALL
FUN(PDFDocument_insertPage)(JNIEnv *env, jobject self, jint jat, jobject jpage)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	int at = jat;
	pdf_obj *page = from_PDFObject(env, jpage);

	if (!ctx || !pdf) return;
	if (jat < 0 || jat >= pdf_count_pages(ctx, pdf)) { jni_throw_oob(env, "at is not a valid page"); return; }
	if (!page) { jni_throw_arg(env, "page must not be null"); return; }

	fz_try(ctx)
		pdf_insert_page(ctx, pdf, at, page);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFDocument_deletePage)(JNIEnv *env, jobject self, jint jat)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	int at = jat;

	if (!ctx || !pdf) return;
	if (jat < 0 || jat >= pdf_count_pages(ctx, pdf)) { jni_throw_oob(env, "at is not a valid page"); return; }

	fz_try(ctx)
		pdf_delete_page(ctx, pdf, at);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addImage)(JNIEnv *env, jobject self, jobject jimage)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	fz_image *image = from_Image(env, jimage);
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!image) { jni_throw_arg(env, "image must not be null"); return NULL; }

	fz_try(ctx)
		ind = pdf_add_image(ctx, pdf, image, 0);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addFont)(JNIEnv *env, jobject self, jobject jfont)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	fz_font *font = from_Font(env, jfont);
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!font) { jni_throw_arg(env, "font must not be null"); return NULL; }

	fz_try(ctx)
		ind = pdf_add_cid_font(ctx, pdf, font);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT jobject JNICALL
FUN(PDFDocument_addSimpleFont)(JNIEnv *env, jobject self, jobject jfont)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	fz_font *font = from_Font(env, jfont);
	pdf_obj *ind = NULL;

	if (!ctx || !pdf) return NULL;
	if (!font) { jni_throw_arg(env, "font must not be null"); return NULL; }

	fz_try(ctx)
		ind = pdf_add_simple_font(ctx, pdf, font);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, ind);
}

JNIEXPORT jboolean JNICALL
FUN(PDFDocument_hasUnsavedChanges)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	if (!ctx || !pdf) return JNI_FALSE;
	return pdf_has_unsaved_changes(ctx, pdf) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFDocument_canBeSavedIncrementally)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	if (!ctx || !pdf) return JNI_FALSE;
	return pdf_can_be_saved_incrementally(ctx, pdf) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
FUN(PDFDocument_save)(JNIEnv *env, jobject self, jstring jfilename, jstring joptions)
{
	fz_context *ctx = get_context(env);
	pdf_document *pdf = from_PDFDocument(env, self);
	const char *filename = NULL;
	const char *options = NULL;
	pdf_write_options pwo;
	int errors = 0;

	if (!ctx || !pdf) return 0;
	if (!jfilename) { jni_throw_arg(env, "filename must not be null"); return 0; }

	filename = (*env)->GetStringUTFChars(env, jfilename, NULL);
	if (!filename) return 0;

	if (joptions)
	{
		options = (*env)->GetStringUTFChars(env, joptions, NULL);
		if (!options)
		{
			(*env)->ReleaseStringUTFChars(env, jfilename, filename);
			return 0;
		}
	}

	fz_try(ctx)
	{
		pdf_parse_write_options(ctx, &pwo, options);
		pwo.errors = &errors;
		pdf_save_document(ctx, pdf, filename, &pwo);
	}
	fz_always(ctx)
	{
		if (options)
			(*env)->ReleaseStringUTFChars(env, joptions, options);
		(*env)->ReleaseStringUTFChars(env, jfilename, filename);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	return errors;
}

/* PDFObject interface */

JNIEXPORT void JNICALL
FUN(PDFObject_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject_safe(env, self);

	if (!ctx || !obj) return;

	pdf_drop_obj(ctx, obj);
}

JNIEXPORT jlong JNICALL
FUN(PDFObject_newNull)(JNIEnv *env, jclass cls)
{
	fz_context *ctx = get_context(env);

	if (!ctx) return 0;

	/* Not nice to pass doc as NULL, but it is unused */
	return jlong_cast(pdf_new_null(ctx, NULL));
}

JNIEXPORT jint JNICALL
FUN(PDFObject_toIndirect)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int num = 0;

	if (!ctx || !obj) return 0;

	fz_try(ctx)
		num = pdf_to_num(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return num;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isIndirect)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_indirect(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isNull)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_null(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isBoolean)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_bool(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isInteger)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_int(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isReal)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_real(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isNumber)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_number(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isString)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_string(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isName)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_name(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isArray)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_array(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isDictionary)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_dict(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_isStream)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_is_stream(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
FUN(PDFObject_readStream)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	fz_buffer *buf = NULL;
	jbyteArray arr = NULL;

	if (!ctx || !obj) return NULL;

	fz_var(buf);

	fz_try(ctx)
	{
		size_t len;
		unsigned char *data;

		buf = pdf_load_stream(ctx, obj);
		len = fz_buffer_storage(ctx, buf, &data);
		arr = (*env)->NewByteArray(env, (jsize)len);
		if (arr)
		{
			(*env)->SetByteArrayRegion(env, arr, 0, (jsize)len, (signed char *) &data[0]);
			if ((*env)->ExceptionCheck(env))
				arr = NULL;
		}
	}
	fz_always(ctx)
		fz_drop_buffer(ctx, buf);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return arr;
}

JNIEXPORT jbyteArray JNICALL
FUN(PDFObject_readRawStream)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	fz_buffer *buf = NULL;
	jbyteArray arr = NULL;

	if (!ctx || !obj) return NULL;

	fz_var(buf);

	fz_try(ctx)
	{
		unsigned char *data;
		size_t len;

		buf = pdf_load_raw_stream(ctx, obj);
		len = fz_buffer_storage(ctx, buf, &data);
		arr = (*env)->NewByteArray(env, (jsize)len);
		if (arr)
		{
			(*env)->SetByteArrayRegion(env, arr, 0, (jsize)len, (signed char *) &data[0]);
			if ((*env)->ExceptionCheck(env))
				arr = NULL;
		}
	}
	fz_always(ctx)
		fz_drop_buffer(ctx, buf);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return arr;
}

JNIEXPORT void JNICALL
FUN(PDFObject_writeObject)(JNIEnv *env, jobject self, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_obj *ref = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, ref);
	pdf_obj *obj = from_PDFObject(env, jobj);

	if (!ctx || !obj) return;
	if (!pdf) { jni_throw_arg(env, "object not bound to document"); return; }
	if (!obj) { jni_throw_arg(env, "object must not be null"); return; }

	fz_try(ctx)
		pdf_update_object(ctx, pdf, pdf_to_num(ctx, ref), obj);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_writeStreamBuffer)(JNIEnv *env, jobject self, jobject jbuf)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, obj);
	fz_buffer *buf = from_Buffer(env, jbuf);

	if (!ctx || !obj) return;
	if (!pdf) { jni_throw_arg(env, "object not bound to document"); return; }
	if (!buf) { jni_throw_arg(env, "buffer must not be null"); return; }

	fz_try(ctx)
		pdf_update_stream(ctx, pdf, obj, buf, 0);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_writeStreamString)(JNIEnv *env, jobject self, jstring jstr)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, obj);
	const char *str = NULL;
	unsigned char *data = NULL;
	fz_buffer *buf = NULL;

	if (!ctx || !obj) return;
	if (!pdf) { jni_throw_arg(env, "object not bound to document"); return; }
	if (!jstr) { jni_throw_arg(env, "string must not be null"); return; }

	str = (*env)->GetStringUTFChars(env, jstr, NULL);
	if (!str) return;

	fz_var(buf);

	fz_try(ctx)
	{
		size_t len = strlen(str);
		data = fz_malloc(ctx, len);
		memcpy(data, str, len);
		buf = fz_new_buffer_from_data(ctx, data, len);
		pdf_update_stream(ctx, pdf, obj, buf, 0);
	}
	fz_always(ctx)
	{
		fz_drop_buffer(ctx, buf);
		(*env)->ReleaseStringUTFChars(env, jstr, str);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_writeRawStreamBuffer)(JNIEnv *env, jobject self, jobject jbuf)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, obj);
	fz_buffer *buf = from_Buffer(env, jbuf);

	if (!ctx || !obj) return;
	if (!pdf) { jni_throw_arg(env, "object not bound to document"); return; }
	if (!buf) { jni_throw_arg(env, "buffer must not be null"); return; }

	fz_try(ctx)
		pdf_update_stream(ctx, pdf, obj, buf, 1);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_writeRawStreamString)(JNIEnv *env, jobject self, jstring jstr)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, obj);
	const char *str = NULL;
	unsigned char *data = NULL;
	fz_buffer *buf = NULL;

	if (!ctx || !obj) return;
	if (!pdf) { jni_throw_arg(env, "object not bound to document"); return; }
	if (!jstr) { jni_throw_arg(env, "string must not be null"); return; }

	str = (*env)->GetStringUTFChars(env, jstr, NULL);
	if (!str) return;

	fz_var(buf);

	fz_try(ctx)
	{
		size_t len = strlen(str);
		data = fz_malloc(ctx, len);
		memcpy(data, str, len);
		buf = fz_new_buffer_from_data(ctx, data, len);
		pdf_update_stream(ctx, pdf, obj, buf, 1);
	}
	fz_always(ctx)
	{
		fz_drop_buffer(ctx, buf);
		(*env)->ReleaseStringUTFChars(env, jstr, str);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFObject_resolve)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	pdf_obj *ind = NULL;
	jobject jobj;

	if (!ctx || !obj) return NULL;

	fz_try(ctx)
		ind = pdf_resolve_indirect(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	pdf_keep_obj(ctx, ind);
	jobj = (*env)->NewObject(env, cls_PDFObject, mid_PDFObject_init, jlong_cast(obj), self);
	if (!jobj)
		pdf_drop_obj(ctx, ind);
	return jobj;
}

JNIEXPORT jobject JNICALL
FUN(PDFObject_getArray)(JNIEnv *env, jobject self, jint index)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *val = NULL;

	if (!ctx || !arr) return NULL;

	fz_try(ctx)
		val = pdf_array_get(ctx, arr, index);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe(ctx, env, self, val);
}

JNIEXPORT jobject JNICALL
FUN(PDFObject_getDictionary)(JNIEnv *env, jobject self, jstring jname)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	const char *name = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return NULL;

	if (jname)
		name = (*env)->GetStringUTFChars(env, jname, NULL);

	if (name)
	{
		fz_try(ctx)
			val = pdf_dict_gets(ctx, dict, name);
		fz_always(ctx)
			(*env)->ReleaseStringUTFChars(env, jname, name);
		fz_catch(ctx)
		{
			jni_rethrow(env, ctx);
			return NULL;
		}
	}

	return to_PDFObject_safe(ctx, env, self, val);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putArrayBoolean)(JNIEnv *env, jobject self, jint index, jboolean b)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *val = NULL;

	if (!ctx || !arr) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		val = pdf_new_bool(ctx, pdf, b);
		pdf_array_put(ctx, arr, index, val);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, val);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putArrayInteger)(JNIEnv *env, jobject self, jint index, jint i)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *val = NULL;

	if (!ctx || !arr) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		val = pdf_new_int(ctx, pdf, i);
		pdf_array_put(ctx, arr, index, val);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, val);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putArrayFloat)(JNIEnv *env, jobject self, jint index, jfloat f)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *val = NULL;

	if (!ctx || !arr) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		val = pdf_new_real(ctx, pdf, f);
		pdf_array_put(ctx, arr, index, val);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, val);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putArrayString)(JNIEnv *env, jobject self, jint index, jstring jstr)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	const char *str = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !arr) return;
	if (jstr)
	{
		str = (*env)->GetStringUTFChars(env, jstr, NULL);
		if (!str) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		val = str ? pdf_new_string(ctx, pdf, str, strlen(str)) : NULL;
		pdf_array_put(ctx, arr, index, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		if (str)
			(*env)->ReleaseStringUTFChars(env, jstr, str);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putArrayPDFObject)(JNIEnv *env, jobject self, jint index, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *obj = from_PDFObject(env, jobj);

	if (!ctx || !arr) return;

	fz_try(ctx)
		pdf_array_put(ctx, arr, index, obj);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryStringBoolean)(JNIEnv *env, jobject self, jstring jname, jboolean b)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	const char *name = NULL;
	pdf_obj *key = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		key = name ? pdf_new_name(ctx, pdf, name) : NULL;
		val = pdf_new_bool(ctx, pdf, b);
		pdf_dict_put(ctx, dict, key, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		pdf_drop_obj(ctx, key);
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryStringInteger)(JNIEnv *env, jobject self, jstring jname, jint i)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	const char *name = NULL;
	pdf_obj *key = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		key = name ? pdf_new_name(ctx, pdf, name) : NULL;
		val = pdf_new_int(ctx, pdf, i);
		pdf_dict_put(ctx, dict, key, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		pdf_drop_obj(ctx, key);
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryStringFloat)(JNIEnv *env, jobject self, jstring jname, jfloat f)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	const char *name = NULL;
	pdf_obj *key = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		key = name ? pdf_new_name(ctx, pdf, name) : NULL;
		val = pdf_new_real(ctx, pdf, f);
		pdf_dict_put(ctx, dict, key, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		pdf_drop_obj(ctx, key);
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryStringString)(JNIEnv *env, jobject self, jstring jname, jstring jstr)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, dict);
	const char *name = NULL;
	const char *str = NULL;
	pdf_obj *key = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return;
	}
	if (jstr)
	{
		str = (*env)->GetStringUTFChars(env, jstr, NULL);
		if (!str)
		{
			(*env)->ReleaseStringUTFChars(env, jname, str);
			return;
		}
	}

	fz_try(ctx)
	{
		key = name ? pdf_new_name(ctx, pdf, name) : NULL;
		val = val ? pdf_new_string(ctx, pdf, str, strlen(str)) : NULL;
		pdf_dict_put(ctx, dict, key, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		pdf_drop_obj(ctx, key);
		if (str)
			(*env)->ReleaseStringUTFChars(env, jstr, str);
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryStringPDFObject)(JNIEnv *env, jobject self, jstring jname, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_document *pdf = pdf_get_bound_document(ctx, dict);
	pdf_obj *val = from_PDFObject(env, jobj);
	const char *name = NULL;
	pdf_obj *key = NULL;

	if (!ctx || !dict) return;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return;
	}

	fz_try(ctx)
	{
		key = name ? pdf_new_name(ctx, pdf, name) : NULL;
		pdf_dict_put(ctx, dict, key, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, key);
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryPDFObjectBoolean)(JNIEnv *env, jobject self, jobject jname, jboolean b)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_obj *name = from_PDFObject(env, jname);
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		val = pdf_new_bool(ctx, pdf, b);
		pdf_dict_put(ctx, dict, name, val);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, val);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryPDFObjectInteger)(JNIEnv *env, jobject self, jobject jname, jint i)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_obj *name = from_PDFObject(env, jname);
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		val = pdf_new_int(ctx, pdf, i);
		pdf_dict_put(ctx, dict, name, val);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, val);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryPDFObjectFloat)(JNIEnv *env, jobject self, jobject jname, jfloat f)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_obj *name = from_PDFObject(env, jname);
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		val = pdf_new_real(ctx, pdf, f);
		pdf_dict_put(ctx, dict, name, val);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, val);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryPDFObjectString)(JNIEnv *env, jobject self, jobject jname, jstring jstr)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_obj *name = from_PDFObject(env, jname);
	const char *str = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;
	if (jstr)
	{
		str = (*env)->GetStringUTFChars(env, jstr, NULL);
		if (!str) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		val = str ? pdf_new_string(ctx, pdf, str, strlen(str)) : NULL;
		pdf_dict_put(ctx, dict, name, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		if (str)
			(*env)->ReleaseStringUTFChars(env, jstr, str);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_putDictionaryPDFObjectPDFObject)(JNIEnv *env, jobject self, jobject jname, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_obj *name = from_PDFObject(env, jname);
	pdf_obj *obj = from_PDFObject(env, jobj);

	if (!ctx || !dict) return;

	fz_try(ctx)
		pdf_dict_put(ctx, dict, name, obj);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_deleteArray)(JNIEnv *env, jobject self, jint index)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);

	if (!ctx || !arr) return;

	fz_try(ctx)
		pdf_array_delete(ctx, arr, index);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_deleteDictionaryString)(JNIEnv *env, jobject self, jstring jname)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	const char *name = NULL;
	pdf_obj *val = NULL;

	if (!ctx || !dict) return;
	if (jname)
	{
		name = (*env)->GetStringUTFChars(env, jname, NULL);
		if (!name) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, dict);
		val = name ? pdf_new_name(ctx, pdf, name) : NULL;
		pdf_dict_del(ctx, dict, val);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, val);
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_deleteDictionaryPDFObject)(JNIEnv *env, jobject self, jobject jname)
{
	fz_context *ctx = get_context(env);
	pdf_obj *dict = from_PDFObject(env, self);
	pdf_obj *name = from_PDFObject(env, jname);

	if (!ctx || !dict) return;

	fz_try(ctx)
		pdf_dict_del(ctx, dict, name);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jboolean JNICALL
FUN(PDFObject_asBoolean)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int b = 0;

	if (!ctx || !obj) return JNI_FALSE;

	fz_try(ctx)
		b = pdf_to_bool(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return JNI_FALSE;
	}

	return b ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
FUN(PDFObject_asInteger)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int i = 0;

	if (!ctx || !obj) return 0;

	fz_try(ctx)
		i = pdf_to_int(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return i;
}

JNIEXPORT jfloat JNICALL
FUN(PDFObject_asFloat)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	float f = 0;

	if (!ctx || !obj) return 0;

	fz_try(ctx)
		f = pdf_to_real(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return f;
}

JNIEXPORT jint JNICALL
FUN(PDFObject_asIndirect)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	int ind = 0;

	if (!ctx || !obj) return 0;

	fz_try(ctx)
		ind = pdf_to_num(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return ind;
}

JNIEXPORT jstring JNICALL
FUN(PDFObject_asString)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	char *str = NULL;
	jstring jstr;

	if (!ctx || !obj) return NULL;

	fz_try(ctx)
		str = pdf_to_utf8(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jstr = (*env)->NewStringUTF(env, str);
	fz_free(ctx, str);
	return jstr;
}

JNIEXPORT jobject JNICALL
FUN(PDFObject_asByteString)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	const char *str = NULL;
	jobject jbs = NULL;
	jbyte *bs = NULL;
	int len;

	if (!ctx || !obj) return NULL;

	fz_try(ctx)
	{
		str = pdf_to_str_buf(ctx, obj);
		len = pdf_to_str_len(ctx, obj);
	}
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jbs = (*env)->NewByteArray(env, len);
	if (!jbs) return NULL;
	bs = (*env)->GetByteArrayElements(env, jbs, NULL);
	if (!bs) return NULL;

	memcpy(bs, str, len);

	(*env)->ReleaseByteArrayElements(env, jbs, bs, 0);

	return jbs;
}

JNIEXPORT jstring JNICALL
FUN(PDFObject_asName)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, self);
	const char *str = NULL;

	if (!ctx || !obj) return NULL;

	fz_try(ctx)
		str = pdf_to_name(ctx, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return (*env)->NewStringUTF(env, str);
}

JNIEXPORT jint JNICALL
FUN(PDFObject_size)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	int len;

	if (!ctx || !arr) return 0;

	fz_try(ctx)
		len = pdf_array_len(ctx, arr);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return len;
}

JNIEXPORT void JNICALL
FUN(PDFObject_pushBoolean)(JNIEnv *env, jobject self, jboolean b)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *item = NULL;

	if (!ctx || !arr) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		item = pdf_new_bool(ctx, pdf, b);
		pdf_array_push(ctx, arr, item);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, item);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_pushInteger)(JNIEnv *env, jobject self, jint i)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *item = NULL;

	if (!ctx || !arr) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		item = pdf_new_int(ctx, pdf, i);
		pdf_array_push(ctx, arr, item);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, item);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_pushFloat)(JNIEnv *env, jobject self, jfloat f)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *item = NULL;

	if (!ctx || !arr) return;

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		item = pdf_new_real(ctx, pdf, f);
		pdf_array_push(ctx, arr, item);
	}
	fz_always(ctx)
		pdf_drop_obj(ctx, item);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_pushString)(JNIEnv *env, jobject self, jstring jstr)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	const char *str = NULL;
	pdf_obj *item = NULL;

	if (!ctx || !arr) return;
	if (jstr)
	{
		str = (*env)->GetStringUTFChars(env, jstr, NULL);
		if (!str) return;
	}

	fz_try(ctx)
	{
		pdf_document *pdf = pdf_get_bound_document(ctx, arr);
		item = str ? pdf_new_string(ctx, pdf, str, strlen(str)) : NULL;
		pdf_array_push(ctx, arr, item);
	}
	fz_always(ctx)
	{
		pdf_drop_obj(ctx, item);
		if (str)
			(*env)->ReleaseStringUTFChars(env, jstr, str);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFObject_pushPDFObject)(JNIEnv *env, jobject self, jobject jitem)
{
	fz_context *ctx = get_context(env);
	pdf_obj *arr = from_PDFObject(env, self);
	pdf_obj *item = from_PDFObject(env, jitem);

	if (!ctx || !arr) return;

	fz_try(ctx)
		pdf_array_push(ctx, arr, item);
	fz_always(ctx)
		pdf_drop_obj(ctx, item);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jstring JNICALL
FUN(PDFObject_toString)(JNIEnv *env, jobject self, jboolean tight)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject_safe(env, self);
	jstring string = NULL;
	char *s = NULL;
	int n = 0;

	if (!ctx || !obj) return NULL;

	fz_var(s);

	fz_try(ctx)
	{
		n = pdf_sprint_obj(ctx, NULL, 0, obj, tight);
		s = fz_malloc(ctx, n + 1);
		pdf_sprint_obj(ctx, s, n + 1, obj, tight);
		string = (*env)->NewStringUTF(env, s);
	}
	fz_always(ctx)
		fz_free(ctx, s);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return string;
}

/* Shade interface */

JNIEXPORT void JNICALL
FUN(Shade_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	fz_shade *shd = from_Shade_safe(env, self);

	if (!ctx || !shd) return;

	fz_drop_shade(ctx, shd);
}

/* PDFGraftMap interface */

JNIEXPORT void JNICALL
FUN(PDFGraftMap_finalize)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_graft_map *map = from_PDFGraftMap_safe(env, self);

	if (!ctx || !map) return;

	pdf_drop_graft_map(ctx, map);
}

JNIEXPORT jobject JNICALL
FUN(PDFGraftMap_graftObject)(JNIEnv *env, jobject self, jobject jobj)
{
	fz_context *ctx = get_context(env);
	pdf_obj *obj = from_PDFObject(env, jobj);
	pdf_graft_map *map = from_PDFGraftMap(env, self);

	if (!ctx) return NULL;
	if (!map) { jni_throw_arg(env, "map must not be null"); return NULL; }

	fz_try(ctx)
		obj = pdf_graft_mapped_object(ctx, map, obj);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFObject_safe_own(ctx, env, self, obj);
}

/* PDFPage interface */

JNIEXPORT jobject JNICALL
FUN(PDFPage_createAnnotation)(JNIEnv *env, jobject self, jint subtype)
{
	fz_context *ctx = get_context(env);
	pdf_page *page = from_PDFPage(env, self);
	pdf_annot *annot = NULL;

	if (!ctx || !page) return NULL;

	fz_try(ctx)
		annot = pdf_create_annot(ctx, page, subtype);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_PDFAnnotation_safe_own(ctx, env, self, annot);
}

JNIEXPORT void JNICALL
FUN(PDFPage_deleteAnnotation)(JNIEnv *env, jobject self, jobject jannot)
{
	fz_context *ctx = get_context(env);
	pdf_page *page = from_PDFPage(env, self);
	pdf_annot *annot = from_PDFAnnotation(env, jannot);

	if (!ctx || !page) return;

	fz_try(ctx)
		pdf_delete_annot(ctx, page, annot);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

/* PDFAnnotation interface */

JNIEXPORT jint JNICALL
FUN(PDFAnnotation_getType)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	jint subtype = 0;

	if (!ctx || !annot) return 0;

	fz_try(ctx)
		subtype = pdf_annot_type(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return subtype;
}

JNIEXPORT jint JNICALL
FUN(PDFAnnotation_getFlags)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	jint flags = 0;

	if (!ctx || !annot) return 0;

	fz_try(ctx)
		flags = pdf_annot_flags(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return flags;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setFlags)(JNIEnv *env, jobject self, jint flags)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);

	if (!ctx || !annot) return;

	fz_try(ctx)
		pdf_set_annot_flags(ctx, annot, flags);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jstring JNICALL
FUN(PDFAnnotation_getContents)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	char *contents = NULL;
	jstring result;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		contents = pdf_copy_annot_contents(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	result = (*env)->NewStringUTF(env, contents);
	fz_free(ctx, contents);
	return result;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setContents)(JNIEnv *env, jobject self, jstring jcontents)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	const char *contents = NULL;

	if (!ctx || !annot) return;
	if (jcontents)
	{
		contents = (*env)->GetStringUTFChars(env, jcontents, NULL);
		if (!contents) return;
	}

	fz_try(ctx)
		pdf_set_annot_contents(ctx, annot, contents);
	fz_always(ctx)
		if (contents)
			(*env)->ReleaseStringUTFChars(env, jcontents, contents);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jstring JNICALL
FUN(PDFAnnotation_getAuthor)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	char *author = NULL;
	jstring result;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		author = pdf_copy_annot_author(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	result = (*env)->NewStringUTF(env, author);
	fz_free(ctx, author);
	return result;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setAuthor)(JNIEnv *env, jobject self, jstring jauthor)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	const char *author = NULL;

	if (!ctx || !annot) return;
	if (jauthor)
	{
		author = (*env)->GetStringUTFChars(env, jauthor, NULL);
		if (!author) return;
	}

	fz_try(ctx)
		pdf_set_annot_author(ctx, annot, author);
	fz_always(ctx)
		if (author)
			(*env)->ReleaseStringUTFChars(env, jauthor, author);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jlong JNICALL
FUN(PDFAnnotation_getModificationDateNative)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	jlong t;

	if (!ctx || !annot) return -1;

	fz_try(ctx)
		t = pdf_annot_modification_date(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return -1;
	}

	return t * 1000;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setModificationDate)(JNIEnv *env, jobject self, jlong time)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);

	fz_try(ctx)
		pdf_set_annot_modification_date(ctx, annot, time / 1000);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return;
	}
}

JNIEXPORT jobject JNICALL
FUN(PDFAnnotation_getRect)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	fz_rect rect;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		pdf_annot_rect(ctx, annot, &rect);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	return to_Rect_safe(ctx, env, &rect);
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setRect)(JNIEnv *env, jobject self, jobject jrect)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	fz_rect rect = from_Rect(env, jrect);

	if (!ctx || !annot) return;

	fz_try(ctx)
		pdf_set_annot_rect(ctx, annot, &rect);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jfloat JNICALL
FUN(PDFAnnotation_getBorder)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	jfloat border;

	if (!ctx || !annot) return 0;

	fz_try(ctx)
		border = pdf_annot_border(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return 0;
	}

	return border;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setBorder)(JNIEnv *env, jobject self, jfloat border)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);

	if (!ctx || !annot) return;

	fz_try(ctx)
		pdf_set_annot_border(ctx, annot, border);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFAnnotation_getColor)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int n;
	float color[4];
	jfloatArray arr;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		pdf_annot_color(ctx, annot, &n, color);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	arr = (*env)->NewFloatArray(env, n);
	if (!arr) return NULL;

	(*env)->SetFloatArrayRegion(env, arr, 0, n, &color[0]);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return arr;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setColor)(JNIEnv *env, jobject self, jobject jcolor)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	float color[4];
	int n;

	if (!ctx || !annot) return;
	if (!from_jfloatArray(env, color, nelem(color), jcolor)) return;
	n = (*env)->GetArrayLength(env, jcolor);

	fz_try(ctx)
		pdf_set_annot_color(ctx, annot, n, color);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFAnnotation_getInteriorColor)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int n;
	float color[4];
	jfloatArray arr;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		pdf_annot_interior_color(ctx, annot, &n, color);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	arr = (*env)->NewFloatArray(env, n);
	if (!arr) return NULL;

	(*env)->SetFloatArrayRegion(env, arr, 0, n, &color[0]);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return arr;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setInteriorColor)(JNIEnv *env, jobject self, jobject jcolor)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	float color[4];
	int n;

	if (!ctx || !annot) return;
	if (!from_jfloatArray(env, color, nelem(color), jcolor)) return;
	n = (*env)->GetArrayLength(env, jcolor);

	fz_try(ctx)
		pdf_set_annot_interior_color(ctx, annot, n, color);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFAnnotation_getQuadPoints)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int i, n;
	float qp[8];
	jobject jqps;
	jfloatArray jqp;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		n = pdf_annot_quad_point_count(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jqps = (*env)->NewObjectArray(env, n, cls_FloatArray, NULL);
	if (!jqps) return NULL;

	for (i = 0; i < n; i++)
	{
		fz_try(ctx)
			pdf_annot_quad_point(ctx, annot, i, qp);
		fz_catch(ctx)
		{
			jni_rethrow(env, ctx);
			return NULL;
		}

		jqp = (*env)->NewFloatArray(env, 8);
		if (!jqp) return NULL;

		(*env)->SetFloatArrayRegion(env, jqp, 0, 8, &qp[0]);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->SetObjectArrayElement(env, jqps, i, jqp);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, jqp);
	}

	return jqps;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setQuadPoints)(JNIEnv *env, jobject self, jobject jqps)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int i, n, m;
	float *qp;
	jfloatArray jqp;

	if (!ctx || !annot) return;

	n = (*env)->GetArrayLength(env, jqps);

	fz_try(ctx)
		qp = fz_malloc(ctx, n * 8 * sizeof *qp);
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	for (i = 0; i < n; i++)
	{
		jqp = (*env)->GetObjectArrayElement(env, jqps, i);
		if ((*env)->ExceptionCheck(env))
		{
			fz_free(ctx, qp);
			return;
		}

		if (!jqp)
			continue;

		m = (*env)->GetArrayLength(env, jqp);
		if (m > 8)
			m = 8;

		(*env)->GetFloatArrayRegion(env, jqp, 0, m, &qp[i * 8]);
		if ((*env)->ExceptionCheck(env))
		{
			fz_free(ctx, qp);
			return;
		}

		if (m < 8)
			memset(&qp[i * 8 + m], 0, (8 - m) * sizeof (float));

		(*env)->DeleteLocalRef(env, jqp);
	}

	fz_try(ctx)
		pdf_set_annot_quad_points(ctx, annot, n, qp);
	fz_always(ctx)
		fz_free(ctx, qp);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_updateAppearance)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);

	if (!ctx || !annot) return;

	fz_try(ctx)
		pdf_update_appearance(ctx, annot->page->doc, annot);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFAnnotation_getInkList)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int i, k, n, m;
	float v[2];
	jobject jinklist;
	jfloatArray jpath;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		n = pdf_annot_ink_list_count(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jinklist = (*env)->NewObjectArray(env, n, cls_FloatArray, NULL);
	if (!jinklist) return NULL;

	for (i = 0; i < n; i++)
	{
		fz_try(ctx)
			m = pdf_annot_ink_list_stroke_count(ctx, annot, i);
		fz_catch(ctx)
		{
			jni_rethrow(env, ctx);
			return NULL;
		}

		jpath = (*env)->NewFloatArray(env, m * 2);
		if (!jpath) return NULL;

		for (k = 0; k < m; k++)
		{
			fz_try(ctx)
				pdf_annot_ink_list_stroke_vertex(ctx, annot, i, k, v);
			fz_catch(ctx)
			{
				jni_rethrow(env, ctx);
				return NULL;
			}

			(*env)->SetFloatArrayRegion(env, jpath, k * 2, 2, &v[0]);
			if ((*env)->ExceptionCheck(env)) return NULL;
		}

		(*env)->SetObjectArrayElement(env, jinklist, i, jpath);
		if ((*env)->ExceptionCheck(env)) return NULL;

		(*env)->DeleteLocalRef(env, jpath);
	}

	return jinklist;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setInkList)(JNIEnv *env, jobject self, jobject jinklist)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int i, k, n, m;
	jfloatArray jpath;
	float *points = NULL;
	int *counts = NULL;

	if (!ctx || !annot) return;

	n = (*env)->GetArrayLength(env, jinklist);

	for (i = m = 0; i < n; i++)
	{
		jpath = (*env)->GetObjectArrayElement(env, jinklist, i);
		if ((*env)->ExceptionCheck(env)) return;

		if (!jpath)
			continue;

		m += (*env)->GetArrayLength(env, jpath) / 2;

		(*env)->DeleteLocalRef(env, jpath);
	}

	fz_try(ctx)
	{
		counts = fz_malloc(ctx, n * sizeof(int));
		points = fz_malloc(ctx, m * 2 * sizeof(float));
	}
	fz_catch(ctx)
	{
		fz_free(ctx, counts);
		fz_free(ctx, points);
		jni_rethrow(env, ctx);
	}

	for (i = k = 0; i < n; k += counts[i++])
	{
		jpath = (*env)->GetObjectArrayElement(env, jinklist, i);
		if ((*env)->ExceptionCheck(env))
		{
			fz_free(ctx, counts);
			fz_free(ctx, points);
			return;
		}

		if (!jpath)
			continue;

		counts[i] = (*env)->GetArrayLength(env, jpath);
		(*env)->GetFloatArrayRegion(env, jpath, 0, counts[i], &points[k]);
		if ((*env)->ExceptionCheck(env))
		{
			fz_free(ctx, counts);
			fz_free(ctx, points);
			return;
		}
		counts[i] /= 2;

		(*env)->DeleteLocalRef(env, jpath);
	}

	fz_try(ctx)
		pdf_set_annot_ink_list(ctx, annot, n, counts, points);
	fz_always(ctx)
	{
		fz_free(ctx, counts);
		fz_free(ctx, points);
	}
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jboolean JNICALL
FUN(PDFAnnotation_isOpen)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int open = 0;

	if (!ctx || !annot) return 0;

	fz_try(ctx)
		open = pdf_annot_is_open(ctx, annot);
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	return open;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setIsOpen)(JNIEnv *env, jobject self, jboolean open)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);

	if (!ctx || !annot) return;

	fz_try(ctx)
		pdf_set_annot_is_open(ctx, annot, open);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jobject JNICALL
FUN(PDFAnnotation_getVertices)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int i, n;
	float vertex[2];
	jobject jvertices;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		n = pdf_annot_vertex_count(ctx, annot);
	fz_catch(ctx)
	{
		jni_rethrow(env, ctx);
		return NULL;
	}

	jvertices = (*env)->NewObjectArray(env, 2 * n, cls_FloatArray, NULL);
	if (!jvertices) return NULL;

	for (i = 0; i < n; i++)
	{
		fz_try(ctx)
			pdf_annot_vertex(ctx, annot, i, vertex);
		fz_catch(ctx)
		{
			jni_rethrow(env, ctx);
			return NULL;
		}

		(*env)->SetFloatArrayRegion(env, jvertices, i * 2, 2, &vertex[0]);
		if ((*env)->ExceptionCheck(env)) return NULL;
	}

	return jvertices;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setVertices)(JNIEnv *env, jobject self, jobject jvertices)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	float *vertices = NULL;
	int n;

	if (!ctx || !annot) return;

	n = (*env)->GetArrayLength(env, jvertices);

	fz_try(ctx)
		vertices = fz_malloc(ctx, n * sizeof *vertices);
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	(*env)->GetFloatArrayRegion(env, jvertices, 0, n, vertices);
	if ((*env)->ExceptionCheck(env))
	{
		fz_free(ctx, vertices);
		return;
	}

	fz_try(ctx)
		pdf_set_annot_vertices(ctx, annot, n, vertices);
	fz_always(ctx)
		fz_free(ctx, vertices);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jstring JNICALL
FUN(PDFAnnotation_getIcon)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	const char *name = NULL;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		name = pdf_annot_icon_name(ctx, annot);
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	return (*env)->NewStringUTF(env, name);
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setIcon)(JNIEnv *env, jobject self, jstring jname)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	const char *name = NULL;

	if (!ctx || !annot) return;
	if (!jname) { jni_throw_arg(env, "icon name must not be null"); return; }

	name = (*env)->GetStringUTFChars(env, jname, NULL);
	if (!name) return;

	fz_try(ctx)
		pdf_set_annot_icon_name(ctx, annot, name);
	fz_always(ctx)
		if (name)
			(*env)->ReleaseStringUTFChars(env, jname, name);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}

JNIEXPORT jintArray JNICALL
FUN(PDFAnnotation_getLineEndingStyles)(JNIEnv *env, jobject self)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);
	int line_endings[2];
	jintArray jline_endings = NULL;

	if (!ctx || !annot) return NULL;

	fz_try(ctx)
		pdf_annot_line_ending_styles(ctx, annot, &line_endings[0], &line_endings[1]);
	fz_catch(ctx)
		jni_rethrow(env, ctx);

	jline_endings = (*env)->NewIntArray(env, 2);
	(*env)->SetIntArrayRegion(env, jline_endings, 0, 2, &line_endings[0]);
	if ((*env)->ExceptionCheck(env)) return NULL;

	return jline_endings;
}

JNIEXPORT void JNICALL
FUN(PDFAnnotation_setLineEndingStyles)(JNIEnv *env, jobject self, jint start_style, jint end_style)
{
	fz_context *ctx = get_context(env);
	pdf_annot *annot = from_PDFAnnotation(env, self);

	if (!ctx || !annot) return;

	fz_try(ctx)
		pdf_set_annot_line_ending_styles(ctx, annot, start_style, end_style);
	fz_catch(ctx)
		jni_rethrow(env, ctx);
}
