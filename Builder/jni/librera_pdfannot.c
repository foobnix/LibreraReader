/*
 * Annotations through PDFium itself: listing them, drawing ink and marking text, moving,
 * restyling and erasing them - for the PDFium engine, with or without MuPDF in the build
 * (org.ebookdroid.droids.pdfium.codec.PdfiumAnnots). pdfiumandroid saves the document.
 *
 * PDFium is the library pdfiumandroid has already loaded; its functions are looked up at run
 * time rather than linked, so this library has nothing to build against and works with whichever
 * libpdfium.so the app carries. The page is the one pdfiumandroid opened, passed in as its native
 * pointer, which is PDFium's page; the caller holds the lock every PDFium call in the app is made
 * under.
 *
 * Coordinates are PDF user space (origin bottom left, y up); Java maps them to and from the page.
 */
#include <jni.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "pdfannot"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef void* FPDF_PAGE;
typedef void* FPDF_ANNOTATION;
typedef void* FPDF_PAGEOBJECT;
typedef int FPDF_BOOL;
typedef unsigned short FPDF_WCHAR;

typedef struct { float x; float y; } FS_POINTF;
typedef struct { float left; float top; float right; float bottom; } FS_RECTF;
typedef struct { float x1, y1, x2, y2, x3, y3, x4, y4; } FS_QUADPOINTSF;

#define FPDF_ANNOT_INK 15
#define FPDF_ANNOT_APPEARANCEMODE_NORMAL 0
#define FPDFANNOT_COLORTYPE_Color 0
#define FPDF_ANNOT_FLAG_PRINT 4
#define FPDF_LINECAP_ROUND 1
#define FPDF_LINEJOIN_ROUND 1
#define FPDF_FILLMODE_NONE 0

static int (*pGetAnnotCount)(FPDF_PAGE);
static FPDF_ANNOTATION (*pGetAnnot)(FPDF_PAGE, int);
static void (*pCloseAnnot)(FPDF_ANNOTATION);
static FPDF_ANNOTATION (*pCreateAnnot)(FPDF_PAGE, int);
static FPDF_BOOL (*pRemoveAnnot)(FPDF_PAGE, int);
static int (*pGetSubtype)(FPDF_ANNOTATION);
static FPDF_BOOL (*pGetRect)(FPDF_ANNOTATION, FS_RECTF*);
static FPDF_BOOL (*pSetRect)(FPDF_ANNOTATION, const FS_RECTF*);
static unsigned long (*pGetStringValue)(FPDF_ANNOTATION, const char*, FPDF_WCHAR*, unsigned long);
static int (*pAddInkStroke)(FPDF_ANNOTATION, const FS_POINTF*, size_t);
static FPDF_BOOL (*pRemoveInkList)(FPDF_ANNOTATION);
static unsigned long (*pGetInkListCount)(FPDF_ANNOTATION);
static unsigned long (*pGetInkListPath)(FPDF_ANNOTATION, unsigned long, FS_POINTF*, unsigned long);
static FPDF_BOOL (*pSetColor)(FPDF_ANNOTATION, int, unsigned int, unsigned int, unsigned int, unsigned int);
static FPDF_BOOL (*pGetColor)(FPDF_ANNOTATION, int, unsigned int*, unsigned int*, unsigned int*, unsigned int*);
static FPDF_BOOL (*pSetBorder)(FPDF_ANNOTATION, float, float, float);
static FPDF_BOOL (*pGetBorder)(FPDF_ANNOTATION, float*, float*, float*);
static int (*pGetObjectCount)(FPDF_ANNOTATION);
static FPDF_BOOL (*pRemoveObject)(FPDF_ANNOTATION, int);
static FPDF_BOOL (*pAppendObject)(FPDF_ANNOTATION, FPDF_PAGEOBJECT);
static FPDF_BOOL (*pSetFlags)(FPDF_ANNOTATION, int);
static FPDF_BOOL (*pAppendAttachmentPoints)(FPDF_ANNOTATION, const FS_QUADPOINTSF*);
static FPDF_BOOL (*pSetAP)(FPDF_ANNOTATION, int, const FPDF_WCHAR*);
static FPDF_PAGEOBJECT (*pCreateNewPath)(float, float);
static FPDF_BOOL (*pPathLineTo)(FPDF_PAGEOBJECT, float, float);
static FPDF_BOOL (*pPathSetDrawMode)(FPDF_PAGEOBJECT, int, FPDF_BOOL);
static FPDF_BOOL (*pSetStrokeColor)(FPDF_PAGEOBJECT, unsigned int, unsigned int, unsigned int, unsigned int);
static FPDF_BOOL (*pSetStrokeWidth)(FPDF_PAGEOBJECT, float);
static FPDF_BOOL (*pSetLineJoin)(FPDF_PAGEOBJECT, int);
static FPDF_BOOL (*pSetLineCap)(FPDF_PAGEOBJECT, int);
static void (*pDestroyObject)(FPDF_PAGEOBJECT);

static void* find(void* lib, const char* name, int* missing) {
    void* f = dlsym(lib, name);
    if (!f) {
        LOGE("PDFium has no %s", name);
        (*missing)++;
    }
    return f;
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_init(JNIEnv* env, jclass clazz)
{
    void* lib = dlopen("libpdfium.so", RTLD_NOW);
    if (!lib) {
        LOGE("libpdfium.so is not loaded: %s", dlerror());
        return JNI_FALSE;
    }
    int missing = 0;
    pGetAnnotCount = find(lib, "FPDFPage_GetAnnotCount", &missing);
    pGetAnnot = find(lib, "FPDFPage_GetAnnot", &missing);
    pCloseAnnot = find(lib, "FPDFPage_CloseAnnot", &missing);
    pCreateAnnot = find(lib, "FPDFPage_CreateAnnot", &missing);
    pRemoveAnnot = find(lib, "FPDFPage_RemoveAnnot", &missing);
    pGetSubtype = find(lib, "FPDFAnnot_GetSubtype", &missing);
    pGetRect = find(lib, "FPDFAnnot_GetRect", &missing);
    pSetRect = find(lib, "FPDFAnnot_SetRect", &missing);
    pGetStringValue = find(lib, "FPDFAnnot_GetStringValue", &missing);
    pAddInkStroke = find(lib, "FPDFAnnot_AddInkStroke", &missing);
    pRemoveInkList = find(lib, "FPDFAnnot_RemoveInkList", &missing);
    pGetInkListCount = find(lib, "FPDFAnnot_GetInkListCount", &missing);
    pGetInkListPath = find(lib, "FPDFAnnot_GetInkListPath", &missing);
    pSetColor = find(lib, "FPDFAnnot_SetColor", &missing);
    pGetColor = find(lib, "FPDFAnnot_GetColor", &missing);
    pSetBorder = find(lib, "FPDFAnnot_SetBorder", &missing);
    pGetBorder = find(lib, "FPDFAnnot_GetBorder", &missing);
    pGetObjectCount = find(lib, "FPDFAnnot_GetObjectCount", &missing);
    pRemoveObject = find(lib, "FPDFAnnot_RemoveObject", &missing);
    pAppendObject = find(lib, "FPDFAnnot_AppendObject", &missing);
    pSetFlags = find(lib, "FPDFAnnot_SetFlags", &missing);
    pAppendAttachmentPoints = find(lib, "FPDFAnnot_AppendAttachmentPoints", &missing);
    pSetAP = find(lib, "FPDFAnnot_SetAP", &missing);
    pCreateNewPath = find(lib, "FPDFPageObj_CreateNewPath", &missing);
    pPathLineTo = find(lib, "FPDFPath_LineTo", &missing);
    pPathSetDrawMode = find(lib, "FPDFPath_SetDrawMode", &missing);
    pSetStrokeColor = find(lib, "FPDFPageObj_SetStrokeColor", &missing);
    pSetStrokeWidth = find(lib, "FPDFPageObj_SetStrokeWidth", &missing);
    pSetLineJoin = find(lib, "FPDFPageObj_SetLineJoin", &missing);
    pSetLineCap = find(lib, "FPDFPageObj_SetLineCap", &missing);
    pDestroyObject = find(lib, "FPDFPageObj_Destroy", &missing);
    return missing == 0 ? JNI_TRUE : JNI_FALSE;
}

/* Every annotation of the page: subtype, then its rectangle left, top, right, bottom. */
JNIEXPORT jfloatArray JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_list(JNIEnv* env, jclass clazz, jlong page)
{
    if (!page) {
        return NULL;
    }
    int count = pGetAnnotCount((FPDF_PAGE)page);
    if (count < 0) {
        count = 0;
    }
    jfloat* values = (jfloat*)calloc(count * 5 + 1, sizeof(jfloat));
    int i;
    for (i = 0; i < count; i++) {
        FPDF_ANNOTATION annot = pGetAnnot((FPDF_PAGE)page, i);
        FS_RECTF rect = {0, 0, 0, 0};
        int subtype = -1;
        if (annot) {
            subtype = pGetSubtype(annot);
            pGetRect(annot, &rect);
            pCloseAnnot(annot);
        }
        values[i * 5] = (jfloat)subtype;
        values[i * 5 + 1] = rect.left;
        values[i * 5 + 2] = rect.top;
        values[i * 5 + 3] = rect.right;
        values[i * 5 + 4] = rect.bottom;
    }
    jfloatArray result = (*env)->NewFloatArray(env, count * 5);
    if (result) {
        (*env)->SetFloatArrayRegion(env, result, 0, count * 5, values);
    }
    free(values);
    return result;
}

/* The text an annotation carries, its /Contents, or null. */
JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_contents(JNIEnv* env, jclass clazz, jlong page, jint index)
{
    FPDF_ANNOTATION annot = page ? pGetAnnot((FPDF_PAGE)page, index) : NULL;
    if (!annot) {
        return NULL;
    }
    jstring result = NULL;
    unsigned long bytes = pGetStringValue(annot, "Contents", NULL, 0);
    if (bytes > 2) {
        FPDF_WCHAR* buffer = (FPDF_WCHAR*)malloc(bytes);
        pGetStringValue(annot, "Contents", buffer, bytes);
        /* UTF-16LE with a terminating 0, which Java's strings are on every Android ABI */
        result = (*env)->NewString(env, (const jchar*)buffer, (jsize)(bytes / 2 - 1));
        free(buffer);
    }
    pCloseAnnot(annot);
    return result;
}

static unsigned int channel(float value) {
    int c = (int)(value * 255 + 0.5f);
    return c < 0 ? 0 : c > 255 ? 255 : (unsigned int)c;
}

/*
 * Draws an ink annotation's strokes into its appearance, in its colour and width, and fits its
 * rectangle round them - so the ink shows the same in PDFium, MuPDF and any other viewer, which
 * would otherwise each make up an appearance of their own, or none.
 */
static void ink_appearance(FPDF_ANNOTATION annot) {
    unsigned int r = 0, g = 0, b = 0, a = 255;
    float hr = 0, vr = 0, width = 1;
    /* PDFium reads no colour off an annotation that has an appearance: the old one goes first */
    pSetAP(annot, FPDF_ANNOT_APPEARANCEMODE_NORMAL, NULL);
    pGetColor(annot, FPDFANNOT_COLORTYPE_Color, &r, &g, &b, &a);
    if (!pGetBorder(annot, &hr, &vr, &width) || width <= 0) {
        width = 1;
    }
    int n = pGetObjectCount(annot);
    while (n-- > 0) {
        pRemoveObject(annot, n);
    }

    unsigned long strokes = pGetInkListCount(annot);
    unsigned long s;
    FS_RECTF bounds = {0, 0, 0, 0};
    int first = 1;
    for (s = 0; s < strokes; s++) {
        unsigned long count = pGetInkListPath(annot, s, NULL, 0);
        if (count == 0) {
            continue;
        }
        FS_POINTF* points = (FS_POINTF*)malloc(count * sizeof(FS_POINTF));
        pGetInkListPath(annot, s, points, count);
        unsigned long k;
        for (k = 0; k < count; k++) {
            if (first) {
                bounds.left = bounds.right = points[k].x;
                bounds.top = bounds.bottom = points[k].y;
                first = 0;
            }
            if (points[k].x < bounds.left) bounds.left = points[k].x;
            if (points[k].x > bounds.right) bounds.right = points[k].x;
            if (points[k].y > bounds.top) bounds.top = points[k].y;
            if (points[k].y < bounds.bottom) bounds.bottom = points[k].y;
        }
        free(points);
    }
    /* The rectangle first: the appearance is cut to it */
    bounds.left -= width;
    bounds.right += width;
    bounds.top += width;
    bounds.bottom -= width;
    pSetRect(annot, &bounds);

    for (s = 0; s < strokes; s++) {
        unsigned long count = pGetInkListPath(annot, s, NULL, 0);
        if (count == 0) {
            continue;
        }
        FS_POINTF* points = (FS_POINTF*)malloc(count * sizeof(FS_POINTF));
        pGetInkListPath(annot, s, points, count);
        FPDF_PAGEOBJECT path = pCreateNewPath(points[0].x, points[0].y);
        unsigned long k;
        /* A tap is a dot: a stroke that goes nowhere still needs a length to be drawn */
        if (count == 1) {
            pPathLineTo(path, points[0].x + 0.01f, points[0].y);
        }
        for (k = 1; k < count; k++) {
            pPathLineTo(path, points[k].x, points[k].y);
        }
        free(points);
        pPathSetDrawMode(path, FPDF_FILLMODE_NONE, 1);
        pSetStrokeColor(path, r, g, b, a);
        pSetStrokeWidth(path, width);
        pSetLineJoin(path, FPDF_LINEJOIN_ROUND);
        pSetLineCap(path, FPDF_LINECAP_ROUND);
        if (!pAppendObject(annot, path)) {
            pDestroyObject(path);
        }
    }
}

/* A new ink annotation: strokes as x,y pairs, counts the points of each. */
JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_addInk(JNIEnv* env, jclass clazz, jlong page,
                                                            jfloatArray jxy, jintArray jcounts,
                                                            jfloat r, jfloat g, jfloat b, jfloat width)
{
    if (!page) {
        return JNI_FALSE;
    }
    FPDF_ANNOTATION annot = pCreateAnnot((FPDF_PAGE)page, FPDF_ANNOT_INK);
    if (!annot) {
        return JNI_FALSE;
    }
    jfloat* xy = (*env)->GetFloatArrayElements(env, jxy, NULL);
    jint* counts = (*env)->GetIntArrayElements(env, jcounts, NULL);
    jsize strokes = (*env)->GetArrayLength(env, jcounts);
    jsize i;
    int at = 0;
    for (i = 0; i < strokes; i++) {
        pAddInkStroke(annot, (const FS_POINTF*)(xy + at * 2), (size_t)counts[i]);
        at += counts[i];
    }
    (*env)->ReleaseIntArrayElements(env, jcounts, counts, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, jxy, xy, JNI_ABORT);

    pSetFlags(annot, FPDF_ANNOT_FLAG_PRINT);
    pSetColor(annot, FPDFANNOT_COLORTYPE_Color, channel(r), channel(g), channel(b), 255);
    pSetBorder(annot, 0, 0, width);
    ink_appearance(annot);
    pCloseAnnot(annot);
    return JNI_TRUE;
}

/* A text mark - highlight, underline or strike-out - over the given quads, 8 floats each. */
JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_addMarkup(JNIEnv* env, jclass clazz, jlong page, jint subtype,
                                                               jfloatArray jquads, jfloat r, jfloat g, jfloat b)
{
    jsize length = (*env)->GetArrayLength(env, jquads);
    if (!page || length < 8) {
        return JNI_FALSE;
    }
    FPDF_ANNOTATION annot = pCreateAnnot((FPDF_PAGE)page, subtype);
    if (!annot) {
        return JNI_FALSE;
    }
    jfloat* q = (*env)->GetFloatArrayElements(env, jquads, NULL);
    FS_RECTF bounds = {q[0], q[1], q[0], q[1]};
    jsize i;
    for (i = 0; i + 8 <= length; i += 8) {
        FS_QUADPOINTSF quad = {q[i], q[i + 1], q[i + 2], q[i + 3], q[i + 4], q[i + 5], q[i + 6], q[i + 7]};
        pAppendAttachmentPoints(annot, &quad);
        int k;
        for (k = 0; k < 8; k += 2) {
            if (q[i + k] < bounds.left) bounds.left = q[i + k];
            if (q[i + k] > bounds.right) bounds.right = q[i + k];
            if (q[i + k + 1] > bounds.top) bounds.top = q[i + k + 1];
            if (q[i + k + 1] < bounds.bottom) bounds.bottom = q[i + k + 1];
        }
    }
    (*env)->ReleaseFloatArrayElements(env, jquads, q, JNI_ABORT);
    pSetRect(annot, &bounds);
    pSetFlags(annot, FPDF_ANNOT_FLAG_PRINT);
    pSetColor(annot, FPDFANNOT_COLORTYPE_Color, channel(r), channel(g), channel(b), 255);
    pCloseAnnot(annot);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_remove(JNIEnv* env, jclass clazz, jlong page, jint index)
{
    return page && pRemoveAnnot((FPDF_PAGE)page, index) ? JNI_TRUE : JNI_FALSE;
}

/* Moves an ink annotation by dx, dy: its strokes are read, shifted and written back. */
JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_move(JNIEnv* env, jclass clazz, jlong page, jint index,
                                                          jfloat dx, jfloat dy)
{
    FPDF_ANNOTATION annot = page ? pGetAnnot((FPDF_PAGE)page, index) : NULL;
    if (!annot) {
        return JNI_FALSE;
    }
    if (pGetSubtype(annot) != FPDF_ANNOT_INK) {
        pCloseAnnot(annot);
        return JNI_FALSE;
    }
    unsigned long strokes = pGetInkListCount(annot);
    FS_POINTF** paths = (FS_POINTF**)calloc(strokes + 1, sizeof(FS_POINTF*));
    unsigned long* counts = (unsigned long*)calloc(strokes + 1, sizeof(unsigned long));
    unsigned long s, k;
    for (s = 0; s < strokes; s++) {
        counts[s] = pGetInkListPath(annot, s, NULL, 0);
        paths[s] = (FS_POINTF*)malloc((counts[s] + 1) * sizeof(FS_POINTF));
        pGetInkListPath(annot, s, paths[s], counts[s]);
        for (k = 0; k < counts[s]; k++) {
            paths[s][k].x += dx;
            paths[s][k].y += dy;
        }
    }
    pRemoveInkList(annot);
    for (s = 0; s < strokes; s++) {
        if (counts[s] > 0) {
            pAddInkStroke(annot, paths[s], counts[s]);
        }
        free(paths[s]);
    }
    free(paths);
    free(counts);
    ink_appearance(annot);
    pCloseAnnot(annot);
    return JNI_TRUE;
}

/* A new colour (unless rgb is null) and line width (when above 0) for an ink annotation. */
JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdfium_codec_PdfiumAnnots_style(JNIEnv* env, jclass clazz, jlong page, jint index,
                                                           jfloatArray jrgb, jfloat width)
{
    FPDF_ANNOTATION annot = page ? pGetAnnot((FPDF_PAGE)page, index) : NULL;
    if (!annot) {
        return JNI_FALSE;
    }
    if (pGetSubtype(annot) != FPDF_ANNOT_INK) {
        pCloseAnnot(annot);
        return JNI_FALSE;
    }
    /* Nor does it take a colour while the appearance is there; ink_appearance draws a new one */
    pSetAP(annot, FPDF_ANNOT_APPEARANCEMODE_NORMAL, NULL);
    if (jrgb != NULL && (*env)->GetArrayLength(env, jrgb) >= 3) {
        jfloat* c = (*env)->GetFloatArrayElements(env, jrgb, NULL);
        pSetColor(annot, FPDFANNOT_COLORTYPE_Color, channel(c[0]), channel(c[1]), channel(c[2]), 255);
        (*env)->ReleaseFloatArrayElements(env, jrgb, c, JNI_ABORT);
    }
    if (width > 0) {
        pSetBorder(annot, 0, 0, width);
    }
    ink_appearance(annot);
    pCloseAnnot(annot);
    return JNI_TRUE;
}

