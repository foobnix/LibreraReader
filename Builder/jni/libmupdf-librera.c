#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#include "../ebookdroid.h"
#include "../javahelpers.h"
#include "mupdf/fitz.h"
#include "mupdf/pdf.h"
#include "androidfonts.h"

/* Debugging helper */

#define DEBUG(args...) __android_log_print(ANDROID_LOG_DEBUG, "MuPDF", args)

#define ERROR(args...) __android_log_print(ANDROID_LOG_ERROR, "MuPDF", args)

#define INFO(args...) __android_log_print(ANDROID_LOG_INFO, "MuPDF", args)

#define PACKAGENAME "org/ebookdroid/droids/mupdf/codec"

typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s {
  fz_context *ctx;
  fz_document *document;
  fz_outline *outline;
  char *accel;
  unsigned char format; // save current document format.
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s {
  fz_context *ctx;
  fz_page *page;
  int number;
  fz_display_list *pageList;
  // fz_display_list* annot_list;
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"
#define PASSWORD_REQUIRED_EXCEPTION                                            \
  "org/ebookdroid/droids/mupdf/codec/exceptions/"                              \
  "MuPdfPasswordRequiredException"
#define WRONG_PASSWORD_EXCEPTION                                               \
  "org/ebookdroid/droids/mupdf/codec/exceptions/"                              \
  "MuPdfWrongPasswordEnteredException"

extern fz_locks_context *jni_new_locks();

extern void jni_free_locks(fz_locks_context *locks);

///////////////////
static struct sigaction old_sa[32]; // Using fixed size instead of NSIG

void signal_handler(int sig, siginfo_t *info, void *context) {
  // Log the crash
  __android_log_print(ANDROID_LOG_ERROR, "NDK",
                      "Signal %d caught, preventing crash propagation", sig);

  // Restore original handler before exit to avoid recursion
  sigaction(sig, &old_sa[sig], NULL);

  // Exit gracefully
  exit(0);
}

void setup_signal_handlers() {
  struct sigaction sa;

  // Clear the structure
  memset(&sa, 0, sizeof(struct sigaction));

  // Setup signal handler
  sa.sa_sigaction = signal_handler;
  sa.sa_flags = SA_SIGINFO |
                SA_RESETHAND; // SA_RESETHAND resets to default after first call

  // Initialize the mask (block no signals during handler)
  sigemptyset(&sa.sa_mask);

  // Set up backup array for original handlers
  memset(old_sa, 0, sizeof(old_sa));

  // Catch critical signals
  sigaction(SIGSEGV, &sa, &old_sa[SIGSEGV]); // Segmentation fault
  sigaction(SIGABRT, &sa, &old_sa[SIGABRT]); // Abort
  sigaction(SIGILL, &sa, &old_sa[SIGILL]);   // Illegal instruction
  sigaction(SIGFPE, &sa, &old_sa[SIGFPE]);   // Floating point exception
  sigaction(SIGBUS, &sa, &old_sa[SIGBUS]);   // Bus error
  sigaction(SIGTRAP, &sa, &old_sa[SIGTRAP]); // Trace/breakpoint trap
}

jstring safeNewStringUTF(JNIEnv *env, const char *str) {
  if (!str) {
    return (*env)->NewStringUTF(env, "");
  }

  // First pass: calculate the size needed for the filtered string
  size_t len = 0;
  const unsigned char *p = (const unsigned char *)str;

  while (*p) {
    unsigned char c = *p;

    if (c < 0x80) {
      // 1-byte sequence (0xxxxxxx)
      len++;
      p++;
    } else if ((c & 0xE0) == 0xC0) {
      // 2-byte sequence (110xxxxx 10xxxxxx)
      if (p[1] && (p[1] & 0xC0) == 0x80) {
        len += 2;
        p += 2;
      } else {
        p++; // Skip invalid byte
      }
    } else if ((c & 0xF0) == 0xE0) {
      // 3-byte sequence (1110xxxx 10xxxxxx 10xxxxxx)
      if (p[1] && p[2] && (p[1] & 0xC0) == 0x80 && (p[2] & 0xC0) == 0x80) {
        len += 3;
        p += 3;
      } else {
        p++; // Skip invalid byte
      }
    } else if ((c & 0xF8) == 0xF0) {
      // 4-byte sequence (11110xxx 10xxxxxx 10xxxxxx 10xxxxxx)
      if (p[1] && p[2] && p[3] && (p[1] & 0xC0) == 0x80 &&
          (p[2] & 0xC0) == 0x80 && (p[3] & 0xC0) == 0x80) {
        len += 4;
        p += 4;
      } else {
        p++; // Skip invalid byte
      }
    } else {
      // Invalid start byte
      p++;
    }
  }

  // Allocate buffer for filtered string
  char *filtered = (char *)malloc(len + 1);
  if (!filtered) {
    return (*env)->NewStringUTF(env, "");
  }

  char *dest = filtered;
  p = (const unsigned char *)str;

  // Second pass: copy valid UTF-8 sequences
  while (*p) {
    unsigned char c = *p;

    if (c < 0x80) {
      *dest++ = *p++;
    } else if ((c & 0xE0) == 0xC0) {
      if (p[1] && (p[1] & 0xC0) == 0x80) {
        *dest++ = *p++;
        *dest++ = *p++;
      } else {
        p++;
      }
    } else if ((c & 0xF0) == 0xE0) {
      if (p[1] && p[2] && (p[1] & 0xC0) == 0x80 && (p[2] & 0xC0) == 0x80) {
        *dest++ = *p++;
        *dest++ = *p++;
        *dest++ = *p++;
      } else {
        p++;
      }
    } else if ((c & 0xF8) == 0xF0) {
      if (p[1] && p[2] && p[3] && (p[1] & 0xC0) == 0x80 &&
          (p[2] & 0xC0) == 0x80 && (p[3] & 0xC0) == 0x80) {
        *dest++ = *p++;
        *dest++ = *p++;
        *dest++ = *p++;
        *dest++ = *p++;
      } else {
        p++;
      }
    } else {
      p++;
    }
  }

  *dest = '\0';

  jstring result = (*env)->NewStringUTF(env, filtered);
  free(filtered);

  return result;
}

//////////////

void mupdf_throw_exception_ex(JNIEnv *env, const char *exception,
                              char *message) {
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

static void mupdf_free_document(renderdocument_t *doc) {
  if (!doc) {
    return;
  }

  if (doc->ctx) {
    if (doc->document) {
      fz_drop_document(doc->ctx, doc->document);
      doc->document = NULL;
    }

    fz_drop_context(doc->ctx);
    doc->ctx = NULL;
  }

  doc->accel = NULL;
  free(doc);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getFzVersion(
    JNIEnv *env, jclass clazz) {
  return safeNewStringUTF(env, FZ_VERSION);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_open(
    JNIEnv *env, jclass clazz, jint storememory, jint format, jstring fname,
    jstring pwd, jstring jcss, jint isDocCSS, jfloat imageScale, jint antialias,
    jstring accelerate, jint is_image_scale) {

  // try this
  // setup_signal_handlers();
  ///

  renderdocument_t *doc;
  jboolean iscopy;
  jclass cls;
  jfieldID fid;
  char *filename;
  char *accel;
  char *password;
  char *css;

  filename = (char *)(*env)->GetStringUTFChars(env, fname, &iscopy);
  accel = (char *)(*env)->GetStringUTFChars(env, accelerate, &iscopy);
  password = (char *)(*env)->GetStringUTFChars(env, pwd, &iscopy);
  css = (char *)(*env)->GetStringUTFChars(env, jcss, NULL);

  doc = malloc(sizeof(renderdocument_t));
  if (!doc) {
    mupdf_throw_exception(env, "Out of Memory");
    goto cleanup;
  }
  DEBUG("MuPdfDocument.nativeOpen(): storememory = %d", storememory);

  doc->ctx = fz_new_context(NULL, NULL, FZ_STORE_DEFAULT);
  // doc->ctx = fz_new_context(NULL, NULL, FZ_STORE_UNLIMITED);

  if (!doc->ctx) {
    free(doc);
    mupdf_throw_exception(env, "Out of Memory");
    goto cleanup;
  }
  doc->ctx->image_scale = imageScale;
  doc->ctx->is_image_scale = is_image_scale;

  fz_register_document_handlers(doc->ctx);

  fz_install_load_system_font_funcs(
      doc->ctx, load_droid_font, load_droid_cjk_font, load_droid_fallback_font);

  fz_set_user_css(doc->ctx, css);
  fz_set_use_document_css(doc->ctx, isDocCSS);

  doc->document = NULL;
  doc->outline = NULL;

  fz_set_aa_level(doc->ctx, antialias);
  doc->format = format;
  fz_try(doc->ctx) {
    printf("Open start %s \n", filename);
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Open");

    doc->accel = accel;
    int atime = fz_stat_mtime(accel);
    if (atime == 0) {
      doc->document =
          (fz_document *)fz_open_accelerated_document(doc->ctx, filename, NULL);
    } else {
      doc->document = (fz_document *)fz_open_accelerated_document(
          doc->ctx, filename, accel);
    }

    // fz_drop_context(doc->ctx);
    // fz_set_user_css(doc->ctx,css);

    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Open succes");
    printf("Open  end %s \n", filename);

    // char info[64];
    // fz_lookup_metadata(doc->ctx, doc->document, FZ_META_FORMAT, info,
    // sizeof(info));
  }
  fz_always(doc->ctx) { printf("fz_always %s \n", filename); }

  fz_catch(doc->ctx) {
    //	   mupdf_throw_exception(env, "Open Document  Exception");
    printf("%s \n", filename);
    mupdf_throw_exception(env, "PDF file not found or corrupted");
    mupdf_free_document(doc);
    doc = NULL;
    goto cleanup;
  }

  /*
   * Handle encrypted PDF files
   */
  fz_try(doc->ctx) {
    if (fz_needs_password(doc->ctx, doc->document)) {
      if (strlen(password)) {
        int ok = fz_authenticate_password(doc->ctx, doc->document, password);
        if (!ok) {
          mupdf_free_document(doc);
          mupdf_throw_exception_ex(env, WRONG_PASSWORD_EXCEPTION,
                                   "Wrong password given");
          doc = NULL;
          goto cleanup;
        }
      } else {
        mupdf_free_document(doc);
        mupdf_throw_exception_ex(env, PASSWORD_REQUIRED_EXCEPTION,
                                 "Document needs a password!");
        doc = NULL;
        goto cleanup;
      }
    }
  }
  fz_catch(doc->ctx) {
    //	   mupdf_throw_exception(env, "Open Document  Exception");
    printf("%s \n", filename);
    mupdf_throw_exception(env, "PDF file not found or corrupted");
    mupdf_free_document(doc);
    doc = NULL;
    goto cleanup;
  }

cleanup:

  (*env)->ReleaseStringUTFChars(env, fname, filename);
  (*env)->ReleaseStringUTFChars(env, pwd, password);
  //(*env)->ReleaseStringUTFChars(env, accelerate, accel);
  (*env)->ReleaseStringUTFChars(env, jcss, css);

  return (jlong)(long)doc;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_free(JNIEnv *env,
                                                          jclass clazz,
                                                          jlong handle) {
  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  mupdf_free_document(doc);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getMeta(JNIEnv *env,
                                                             jclass cls,
                                                             jlong handle,
                                                             jstring joptions) {
  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  char info[2048];

  const char *options = (*env)->GetStringUTFChars(env, joptions, NULL);
  if (!options) {
    return safeNewStringUTF(env, "");
  }

  if (!doc || !doc->ctx || !doc->document) {
    (*env)->ReleaseStringUTFChars(env, joptions, options);
    return safeNewStringUTF(env, "");
  }

  fz_try(doc->ctx) {
    fz_lookup_metadata(doc->ctx, doc->document, options, info, sizeof(info));
  }
  fz_catch(doc->ctx) { info[0] = '\0'; }

  (*env)->ReleaseStringUTFChars(env, joptions, options);
  return safeNewStringUTF(env, info);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageInfo(
    JNIEnv *env, jclass cls, jlong handle, jint pageNumber, jobject cpi) {

  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  // TODO: Review this. Possible broken

  fz_page *page = NULL;
  fz_rect bounds;

  jclass clazz;
  jfieldID fid;

  fz_try(doc->ctx) {
    page = fz_load_page(doc->ctx, doc->document, pageNumber - 1);
    bounds = fz_bound_page(doc->ctx, page);
  }
  fz_catch(doc->ctx) { return -1; }

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
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getFirstPageLink(
    JNIEnv *env, jclass clazz, jlong handle, jlong pagehandle) {
  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;

  if (!doc || !doc->ctx || !page || !page->page) {
    return 0; // Return 0 (NULL) instead of -1 for pointer types
  }

  fz_link *links = NULL;
  fz_try(doc->ctx) { links = fz_load_links(doc->ctx, page->page); }
  fz_catch(doc->ctx) { links = NULL; }

  return (jlong)(long)links;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getNextPageLink(
    JNIEnv *env, jclass clazz, jlong linkhandle) {
  fz_link *link = (fz_link *)(long)linkhandle;
  return (jlong)(long)(link ? link->next : NULL);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkType(
    JNIEnv *env, jclass clazz, jlong handle, jlong linkhandle) {
  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  fz_link *link = (fz_link *)(long)linkhandle;

  if (!doc || !doc->ctx || !link || !link->uri) {
    return -1;
  }

  int result = -1;
  fz_try(doc->ctx) { result = (jint)fz_is_external_link(doc->ctx, link->uri); }
  fz_catch(doc->ctx) { result = -1; }

  return result;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkUrl(
    JNIEnv *env, jclass clazz, jlong linkhandle) {
  fz_link *link = (fz_link *)(long)linkhandle;

  if (!link || !link->uri) {
    return NULL;
  }

  char linkbuf[4048];
  snprintf(linkbuf, sizeof(linkbuf), "%s", link->uri);

  return safeNewStringUTF(env, linkbuf);
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkSourceRect(
    JNIEnv *env, jclass clazz, jlong linkhandle, jfloatArray boundsArray) {
  fz_link *link = (fz_link *)(long)linkhandle;

  // if (!link || link->dest.kind != FZ_LINK_GOTO)
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
    JNIEnv *env, jclass clazz, jlong handle, jlong linkhandle) {
  fz_link *link = (fz_link *)(long)linkhandle;
  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  if (!doc || !doc->ctx || !doc->document || !link || !link->uri) {
    return -1;
  }
  int pageNum = -1;
  fz_try(doc->ctx) {
    pageNum = fz_page_number_from_location(
        doc->ctx, doc->document,
        fz_resolve_link(doc->ctx, doc->document, link->uri, NULL, NULL));
  }
  fz_catch(doc->ctx) { pageNum = -1; }

  return pageNum;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getLinkPage(JNIEnv *env,
                                                              jclass clazz,
                                                              jlong handle,
                                                              jstring id) {

  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  if (!doc || !doc->ctx || !doc->document) {
    return -1;
  }
  const char *str = (*env)->GetStringUTFChars(env, id, NULL);
  if (!str) {
    return -1;
  }

  int pageNum = -1;
  fz_try(doc->ctx) {
    pageNum = fz_page_number_from_location(
        doc->ctx, doc->document,
        fz_resolve_link(doc->ctx, doc->document, str, NULL, NULL));
  }
  fz_catch(doc->ctx) { pageNum = -1; }

  (*env)->ReleaseStringUTFChars(env, id, str);
  return pageNum;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkTargetPoint(
    JNIEnv *env, jclass clazz, jlong linkhandle, jfloatArray pointArray) {
  fz_link *link = (fz_link *)(long)linkhandle;

  // if (!link || link->dest.kind != FZ_LINK_GOTO) {
  // 	return 0;
  // }

  jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
  if (!point) {
    return 0;
  }

  // DEBUG("MuPdfLinks_fillPageLinkTargetPoint(): %d %x (%f, %f) - (%f, %f)",
  // 		link->dest.ld.gotor.page, link->dest.ld.gotor.flags,
  // 		link->dest.ld.gotor.lt.x, link->dest.ld.gotor.lt.y,
  // 		link->dest.ld.gotor.rb.x, link->dest.ld.gotor.rb.y);

  point[0] = link->rect.x1;
  point[1] = link->rect.y1;

  (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

  // return link->dest.ld.gotor.flags;
  jint res = 1;
  return res;
}

static int fontSize = 0;
JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageCount(
    JNIEnv *env, jclass clazz, jlong handle, jint width, jint height,
    jint size) {
  renderdocument_t *doc = (renderdocument_t *)(long)handle;
  if (!doc || !doc->ctx || !doc->document) {
    return -1;
  }
  fz_try(doc->ctx) {
    fontSize = size;
    DEBUG("fontSize set %d", fontSize);

    fz_layout_document(doc->ctx, doc->document, width, height, size);

    int count = fz_count_pages(doc->ctx, doc->document);
    fz_save_accelerator(doc->ctx, doc->document, doc->accel);
    return count;
  }
  fz_catch(doc->ctx) {
    // mupdf_free_document(doc);
    mupdf_throw_exception(env, "page count 0");
    return 0;
  }
  return 0;
}

JNIEXPORT jlong JNICALL Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_open(
    JNIEnv *env, jclass clazz, jlong dochandle, jint pageno) {
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;
  renderpage_t *page = NULL;
  fz_device *dev = NULL;

  DEBUG("MuPdfPage_open(%p, %d): start", doc, pageno);

  // fz_context* ctx = fz_clone_context(doc->ctx);
  fz_context *ctx = doc->ctx;
  if (!ctx || doc->ctx == NULL) {
    mupdf_throw_exception(env, "Context cloning failed");
    return (jlong)(long)NULL;
  }

  page = fz_malloc_no_throw(ctx, sizeof(renderpage_t));
  DEBUG("MuPdfPage_open(%p, %d): page=%p", doc, pageno, page);

  if (!page) {
    mupdf_throw_exception(env, "Out of Memory");
    return (jlong)(long)NULL;
  }

  page->ctx = ctx;
  page->page = NULL;
  page->pageList = NULL;
  // page->annot_list = NULL;

  // fz_rect mediabox = fz_empty_rect;
  fz_try(ctx) {

    page->page = fz_load_page(ctx, doc->document, pageno - 1);
    fz_rect mediabox = fz_bound_page(ctx, page->page);

    page->pageList = fz_new_display_list(ctx, mediabox);

    dev = fz_new_list_device(ctx, page->pageList);

    fz_run_page(ctx, page->page, dev, fz_identity, NULL);
  }
  fz_always(ctx) {
    fz_close_device(ctx, dev);
    fz_drop_device(ctx, dev);

    dev = NULL;
  }
  fz_catch(ctx) {

    // fz_drop_display_list(ctx, page->pageList);
    // fz_drop_page(ctx, page->page);
    // fz_free(ctx, page);
    //  fz_free_device(dev);
    //  fz_free_display_list(ctx, page->pageList);
    //  fz_free_page(doc->document, page->page);

    // fz_free(ctx, page);
    // fz_free_context(ctx);

    // page = NULL;
    // ctx = NULL;
    // mupdf_throw_exception(env, "error loading page");
  }

  DEBUG("MuPdfPage_open(%p, %d): finish: %p", doc, pageno, page);

  return (jlong)(long)page;
}

JNIEXPORT void JNICALL Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_free(
    JNIEnv *env, jclass clazz, jlong dochandle, jlong handle) {

  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;
  renderpage_t *page = (renderpage_t *)(long)handle;

  if (!doc || !doc->ctx || !page || doc->ctx == NULL) {
    DEBUG("No page to free");
    return;
  }

  fz_try(doc->ctx) { fz_drop_display_list(doc->ctx, page->pageList); }
  fz_always(doc->ctx) { page->pageList = NULL; }
  fz_catch(doc->ctx) { DEBUG("MuPdfPage_free fz_catch fz_drop_display_list "); }

  fz_try(doc->ctx) { fz_drop_page(doc->ctx, page->page); }
  fz_always(doc->ctx) { page->page = NULL; }
  fz_catch(doc->ctx) { DEBUG("MuPdfPage_free fz_catch fz_drop_page "); }

  page->ctx = NULL;
  page = NULL;
  DEBUG("MuPdfPage_free success");
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getBounds(JNIEnv *env,
                                                           jclass clazz,
                                                           jlong dochandle,
                                                           jlong handle,
                                                           jfloatArray bounds) {
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;
  renderpage_t *page = (renderpage_t *)(long)handle;
  if (!doc || !page || !page->ctx || !page->page)
    return;
  jfloat *bbox = (*env)->GetPrimitiveArrayCritical(env, bounds, 0);
  if (!bbox)
    return;
  fz_rect page_bounds;
  fz_try(page->ctx) { page_bounds = fz_bound_page(page->ctx, page->page); }
  fz_catch(page->ctx) { page_bounds = fz_empty_rect; }
  // DEBUG("Bounds: %f %f %f %f", page_bounds.x0, page_bounds.y0,
  // page_bounds.x1, page_bounds.y1);
  bbox[0] = page_bounds.x0;
  bbox[1] = page_bounds.y0;
  bbox[2] = page_bounds.x1;
  bbox[3] = page_bounds.y1;
  (*env)->ReleasePrimitiveArrayCritical(env, bounds, bbox, 0);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_renderPage(
    JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
    jintArray viewboxarray, jfloatArray matrixarray, jintArray bufferarray,
    jint r, jint g, jint b) {
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;
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

  fz_context *ctx = page->ctx;
  if (!ctx || !page) {
    (*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);
    return;
  }

  fz_colorspace *colorspace = fz_device_bgr(ctx);

  fz_try(ctx) {

    int stride =
        (fz_colorspace_n(ctx, colorspace) + 1) * (viewbox.x1 - viewbox.x0);
    pixmap = fz_new_pixmap_with_data(ctx, colorspace, viewbox.x1 - viewbox.x0,
                                     (viewbox.y1 - viewbox.y0), NULL, 1, stride,
                                     (unsigned char *)buffer);

    // fz_invert_pixmap(ctx,pixmap);
    fz_clear_pixmap_with_value(ctx, pixmap, 0xFF);
    if (r != -1 && g != -1 && b != -1) {
      int value = (r * 65536) + (g * 256) + b;
      fz_tint_pixmap(ctx, pixmap, 0, value);
    }

    dev = fz_new_draw_device(ctx, fz_identity, pixmap);

    fz_run_display_list(ctx, page->pageList, dev, ctm, viewbox, NULL);

    fz_drop_pixmap(ctx, pixmap);
  }
  fz_always(ctx) {
    fz_close_device(ctx, dev);
    fz_drop_device(ctx, dev);
  }
  fz_catch(ctx) { DEBUG("Render failed"); }

  (*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);
}

// Outline
JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_open(JNIEnv *env,
                                                         jclass clazz,
                                                         jlong dochandle) {
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;

  if (doc && doc->ctx && !doc->outline) {
    fz_context *ctx = doc->ctx;
    // doc->outline = fz_load_outline(ctx, doc->document);
    // fz_count_chapters(ctx, doc->document);
    fz_try(ctx) { doc->outline = fz_load_outline(ctx, doc->document); }
    fz_catch(ctx) { doc->outline = NULL; }
  }
  // DEBUG("PdfOutline.open(): return handle = %p", doc->outline);
  return (jlong)(long)doc->outline;
}

JNIEXPORT void JNICALL Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_free(
    JNIEnv *env, jclass clazz, jlong dochandle) {
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;

  if (!doc) {
    DEBUG("MuPdfOutline_free: NULL document");
    return;
  }
  if (!doc->ctx) {
    DEBUG("MuPdfOutline_free: NULL context");
    return;
  }

  if (doc->outline) {

    fz_try(doc->ctx) { fz_drop_outline(doc->ctx, doc->outline); }
    fz_catch(doc->ctx) { DEBUG("MuPdfOutline_free: error dropping outline"); }
  }
  DEBUG("MuPdfOutline_free");

  doc->outline = NULL;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getTitle(
    JNIEnv *env, jclass clazz, jlong dochandle, jlong outlinehandle) {
  fz_outline *outline = (fz_outline *)(long)outlinehandle;
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;

  if (!doc || !doc->ctx || !outline) {
    return NULL;
  }

  if (!outline->title || outline->title[0] == '\0') {
    return NULL;
  }

  jstring result = NULL;

  fz_try(doc->ctx) { result = safeNewStringUTF(env, outline->title); }
  fz_catch(doc->ctx) { result = NULL; }

  return result;
}
JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getLink(JNIEnv *env,
                                                            jclass clazz,
                                                            jlong outlinehandle,
                                                            jlong dochandle) {
  fz_outline *outline = (fz_outline *)(long)outlinehandle;
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;
  if (!doc || !outline) {
    return NULL;
  }

  char linkbuf[4048];
  int pageNo = -1; // outline->page;

  snprintf(linkbuf, sizeof(linkbuf), "#%d", pageNo + 1);

  return safeNewStringUTF(env, linkbuf);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getLinkUri(
    JNIEnv *env, jclass clazz, jlong outlinehandle, jlong dochandle) {
  fz_outline *outline = (fz_outline *)(long)outlinehandle;
  renderdocument_t *doc = (renderdocument_t *)(long)dochandle;
  if (!doc || !outline) {
    return NULL;
  }

  // DEBUG("PdfOutline_getLink(%p)",outline);
  if (outline && outline->uri) {
    return safeNewStringUTF(env, outline->uri);
  } else {
    return NULL;
  }
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_fillLinkTargetPoint(
    JNIEnv *env, jclass clazz, jlong outlinehandle, jfloatArray pointArray) {
  fz_outline *outline = (fz_outline *)(long)outlinehandle;
  int page = -1; // outline->page;

  return page;
}

static int fillInOutlineItems(JNIEnv *env, jclass olClass, jmethodID ctor,
                              jobjectArray arr, int pos, fz_outline *outline,
                              int level) {
  while (outline) {
    int page = -1; // outline->page
    if (page >= 0 && outline->title) {
      jobject ol;
      jstring title = safeNewStringUTF(env, outline->title);
      if (title == NULL)
        return -1;
      ol = (*env)->NewObject(env, olClass, ctor, level, title, page);
      if (ol == NULL)
        return -1;
      (*env)->SetObjectArrayElement(env, arr, pos, ol);
      (*env)->DeleteLocalRef(env, ol);
      (*env)->DeleteLocalRef(env, title);
      pos++;
    }
    pos = fillInOutlineItems(env, olClass, ctor, arr, pos, outline->down,
                             level + 1);
    if (pos < 0)
      return -1;
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
//			outline->dest.ld.gotor.page,
// outline->dest.ld.gotor.flags,
// outline->dest.ld.gotor.lt.x, outline->dest.ld.gotor.lt.y,
// outline->dest.ld.gotor.rb.x, outline->dest.ld.gotor.rb.y);
//
//	point[0] = outline->dest.ld.gotor.lt.x;
//	point[1] = outline->dest.ld.gotor.lt.y;
//
//	(*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);
//
//	return outline->dest.ld.gotor.flags;
//}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getNext(
    JNIEnv *env, jclass clazz, jlong outlinehandle) {
  fz_outline *outline = (fz_outline *)(long)outlinehandle;
  //	DEBUG("MuPdfOutline_getNext(%p)",outline);
  // return (jlong) (long) (outline ? outline->next : 0);
  if (outline)
    return (jlong)(long)outline->next;
  else
    return 0;
  // jlong res = -1;
  // return res;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getChild(
    JNIEnv *env, jclass clazz, jlong outlinehandle) {
  fz_outline *outline = (fz_outline *)(long)outlinehandle;
  //	DEBUG("MuPdfOutline_getChild(%p)",outline);

  if (outline)
    return (jlong)(long)outline->down;
  else
    return 0;
  // return (jlong) (long) (outline ? outline->down : 0);
  // jlong res = -1;
  // return res;
}

///////////////
// SEARCH
//////////////

JNIEXPORT jobject JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_text116(JNIEnv *env,
                                                         jobject thiz,
                                                         jlong handle,
                                                         jlong pagehandle) {

  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;

  jclass textCharClass;
  jclass textSpanClass;
  jclass textLineClass;
  jclass textBlockClass;
  jmethodID ctor;

  fz_stext_page *stext = NULL;
  fz_device *dev = NULL;
  fz_matrix ctm;

  fz_context *ctx = doc_t->ctx;
  fz_document *doc = doc_t->document;

  if (!ctx || !doc || !page) {
    return NULL;
  }

  textCharClass = (*env)->FindClass(env, PACKAGENAME "/TextChar");
  if (textCharClass == NULL)
    return NULL;
  textSpanClass = (*env)->FindClass(env, "[L" PACKAGENAME "/TextChar;");
  if (textSpanClass == NULL)
    return NULL;
  textLineClass = (*env)->FindClass(env, "[[L" PACKAGENAME "/TextChar;");
  if (textLineClass == NULL)
    return NULL;
  textBlockClass = (*env)->FindClass(env, "[[[L" PACKAGENAME "/TextChar;");
  if (textBlockClass == NULL)
    return NULL;
  ctor = (*env)->GetMethodID(env, textCharClass, "<init>", "(FFFFI)V");
  if (ctor == NULL)
    return NULL;

  fz_var(stext);
  fz_var(dev);

  ArrayListHelper alh;
  ArrayListHelper_init(&alh, env);

  jobject arrayList = ArrayListHelper_create(&alh);

  fz_try(ctx) {
    fz_stext_options opts;
    stext = fz_new_stext_page_from_page(ctx, page->page, NULL);

    for (fz_stext_block *block = stext->first_block; block;
         block = block->next) {
      if (block->type == FZ_STEXT_BLOCK_TEXT) {
        for (fz_stext_line *line = block->u.t.first_line; line;
             line = line->next) {
          for (fz_stext_char *ch = line->first_char; ch; ch = ch->next) {
            fz_rect bbox = fz_rect_from_quad(ch->quad);
            jobject cobj = (*env)->NewObject(env, textCharClass, ctor, bbox.x0,
                                             bbox.y0, bbox.x1, bbox.y1, ch->c);
            if (cobj == NULL)
              fz_throw(ctx, FZ_ERROR_GENERIC, "NewObjectfailed");

            ArrayListHelper_add(&alh, arrayList, cobj);
            (*env)->DeleteLocalRef(env, cobj);
          }

          fz_rect bbox = fz_empty_rect;
          jobject cobj = (*env)->NewObject(env, textCharClass, ctor, bbox.x0,
                                           bbox.y0, bbox.x1, bbox.y1, ' ');
          ArrayListHelper_add(&alh, arrayList, cobj);
          (*env)->DeleteLocalRef(env, cobj);
        }
      }
    }
  }
  fz_always(ctx) {
    fz_drop_stext_page(ctx, stext);
    // fz_close_device(ctx, dev);
    // fz_drop_device(ctx, dev);
  }
  fz_catch(ctx) { return NULL; }

  return arrayList;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_hasChangesInternal(
    JNIEnv *env, jclass clazz, jlong handle) {

  if (handle == 0)
    return JNI_FALSE;

  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;

  if (!doc_t || !doc_t->ctx || !doc_t->document)
    return JNI_FALSE;

  fz_context *ctx = doc_t->ctx;

  pdf_document *idoc = NULL;

  fz_try(ctx) { idoc = pdf_specifics(ctx, doc_t->document); }
  fz_catch(ctx) { return JNI_FALSE; }
  if (!idoc)
    return JNI_FALSE;

  return (idoc && pdf_has_unsaved_changes(ctx, idoc)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_saveInternal(
    JNIEnv *env, jclass clazz, jlong handle, jstring fname) {
  DEBUG("save to file 1");
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  if (!doc_t || !doc_t->ctx || !doc_t->document) {
    return;
  }
  fz_context *ctx = doc_t->ctx;
  const char *path = (*env)->GetStringUTFChars(env, fname, NULL);
  if (!path) {
    return;
  }
  DEBUG("save to file %s", path);
  DEBUG("save to file 2");

  pdf_document *idoc = NULL;
  fz_try(ctx) { idoc = pdf_specifics(ctx, doc_t->document); }
  fz_catch(ctx) {
    (*env)->ReleaseStringUTFChars(env, fname, path);
    return;
  }

  if (!idoc) {
    (*env)->ReleaseStringUTFChars(env, fname, path);
    return;
  }

  pdf_write_options opts = {0};
  opts.do_incremental = pdf_can_be_saved_incrementally(ctx, idoc);

  DEBUG("save to file 3");

  fz_try(ctx) {
    // fz_write_document(ctx, doc_t->document, path, &opts);
    pdf_save_document(ctx, idoc, path, &opts);
  }
  fz_catch(ctx) { ERROR("save to file not success"); }
  (*env)->ReleaseStringUTFChars(env, fname, path);
  DEBUG("save to file 4");
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_deletePage(JNIEnv *env,
                                                                jclass clazz,
                                                                jlong handle,
                                                                jint page) {
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  if (!doc_t || !doc_t->ctx || !doc_t->document) {
    return;
  }
  fz_context *ctx = doc_t->ctx;
  pdf_document *idoc = NULL;
  fz_try(ctx) {
    idoc = pdf_specifics(ctx, doc_t->document);
    if (idoc) {
      pdf_delete_page(ctx, idoc, page);
    }
  }
  fz_catch(ctx) { ERROR("Failed to delete page"); }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_setMetaData(
    JNIEnv *env, jclass clazz, jlong handle, jstring jkey, jstring jvalue) {
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  fz_context *ctx = doc_t->ctx;
  fz_document *doc = doc_t->document;
  const char *key = NULL;
  const char *value = NULL;

  if (!ctx || !doc)
    return;
  if (!jkey)
    return;
  if (!jvalue)
    return;

  key = (*env)->GetStringUTFChars(env, jkey, NULL);
  value = (*env)->GetStringUTFChars(env, jvalue, NULL);
  if (!key || !value) {
    if (key)
      (*env)->ReleaseStringUTFChars(env, jkey, key);
    return;
  }

  fz_try(ctx) { fz_set_metadata(ctx, doc, key, value); }
  fz_always(ctx) {
    (*env)->ReleaseStringUTFChars(env, jkey, key);
    (*env)->ReleaseStringUTFChars(env, jvalue, value);
  }
  fz_catch(ctx) { ERROR("can't set metadata"); }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_addInkAnnotationInternal(
    JNIEnv *env, jobject thiz, jlong handle, jlong pagehandle,
    jfloatArray jcolors, jobjectArray arcs, jint width, jfloat alpha) {
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;

  if (!doc_t || !doc_t->ctx || !doc_t->document) {
    return;
  }
  fz_context *ctx = doc_t->ctx;
  fz_document *doc = doc_t->document;
  pdf_document *idoc = NULL;
  fz_try(ctx) { idoc = pdf_specifics(ctx, doc); }
  fz_catch(ctx) { return; }
  jclass pt_cls;
  jfieldID x_fid, y_fid;
  int i, j, k, n;
  fz_point *pts = NULL;
  int *counts = NULL;
  int total = 0;
  float color[4];

  jfloat *co = (*env)->GetPrimitiveArrayCritical(env, jcolors, 0);
  color[0] = co[0];
  color[1] = co[1];
  color[2] = co[2];
  color[3] = alpha;

  (*env)->ReleasePrimitiveArrayCritical(env, jcolors, co, 0);

  if (!doc || !idoc || !page)
    return;

  fz_var(pts);
  fz_var(counts);

  pdf_annot *annot;
  fz_matrix ctm;

  // float zoom = 72/160;
  // zoom = 1.0 / zoom;
  // fz_scale(&ctm, zoom,zoom);

  DEBUG("addInkAnnotationInternal 1");
  pt_cls = (*env)->FindClass(env, "android/graphics/PointF");
  DEBUG("addInkAnnotationInternal 2");
  if (pt_cls == NULL)
    fz_throw(ctx, FZ_ERROR_GENERIC, "FindClass");
  DEBUG("addInkAnnotationInternal 3");
  x_fid = (*env)->GetFieldID(env, pt_cls, "x", "F");
  DEBUG("addInkAnnotationInternal 4");
  if (x_fid == NULL)
    fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(x)");
  DEBUG("addInkAnnotationInternal 5");
  y_fid = (*env)->GetFieldID(env, pt_cls, "y", "F");
  DEBUG("addInkAnnotationInternal 6");
  if (y_fid == NULL)
    fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(y)");
  DEBUG("addInkAnnotationInternal 7");
  n = (*env)->GetArrayLength(env, arcs);
  DEBUG("addInkAnnotationInternal 8");
  counts = fz_malloc_array(ctx, n, int);
  DEBUG("addInkAnnotationInternal 9");
  for (i = 0; i < n; i++) {
    jobjectArray arc =
        (jobjectArray)(*env)->GetObjectArrayElement(env, arcs, i);
    int count = (*env)->GetArrayLength(env, arc);
    (*env)->DeleteLocalRef(env, arc);

    counts[i] = count;
    total += count;
  }
  DEBUG("addInkAnnotationInternal 10");
  pts = fz_malloc_array(ctx, total, fz_point);

  k = 0;
  for (i = 0; i < n; i++) {
    jobjectArray arc =
        (jobjectArray)(*env)->GetObjectArrayElement(env, arcs, i);
    int count = counts[i];

    for (j = 0; j < count; j++) {
      jobject jpt = (*env)->GetObjectArrayElement(env, arc, j);
      pts[k].x = jpt ? (*env)->GetFloatField(env, jpt, x_fid) : 0.0f;
      pts[k].y = jpt ? (*env)->GetFloatField(env, jpt, y_fid) : 0.0f;
      (*env)->DeleteLocalRef(env, jpt);
      // fz_transform_point(&pts[k], &ctm);
      k++;
    }
    (*env)->DeleteLocalRef(env, arc);
  }
  DEBUG("addInkAnnotationInternal 11");
  fz_try(ctx) {
    annot = pdf_create_annot(ctx, (pdf_page *)page->page, PDF_ANNOT_INK);

    pdf_set_annot_border(ctx, annot, width);
    pdf_set_annot_color(ctx, annot, 3, color);
    pdf_set_annot_ink_list(ctx, annot, n, counts, pts);

    pdf_update_page(ctx, (pdf_page *)page->page);

    // page->pageList = fz_new_display_list(ctx);

    // dev = fz_new_list_device(ctx, page->pageList);
    // fz_drop_display_list(ctx, page->annot_list);
    // page->annot_list= NULL;
  }
  fz_always(ctx) {
    fz_free(ctx, pts);
    fz_free(ctx, counts);
  }
  fz_catch(ctx) {
    // LOGE("addInkAnnotation: %s failed", ctx->error->message);
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls != NULL)
      (*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_searchPage");
    (*env)->DeleteLocalRef(env, cls);
  }
}

JNIEXPORT jobjectArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getAnnotationsInternal(
    JNIEnv *env, jobject thiz, jlong handle, jlong pagehandle,
    jobjectArray arcs) {
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;
  fz_context *ctx = doc_t->ctx;
  pdf_document *idoc = pdf_specifics(ctx, doc_t->document);

  if (!ctx || !doc_t->document || !idoc || !page) {
    return NULL;
  }

  jclass annotClass;
  jmethodID ctor;
  jobjectArray arr;
  jobject jannot;
  pdf_annot *annot;
  fz_matrix ctm;
  int count;

  annotClass = (*env)->FindClass(env, "org/ebookdroid/core/codec/Annotation");
  if (annotClass == NULL)
    return NULL;
  ctor = (*env)->GetMethodID(env, annotClass, "<init>", "(FFFFI[B)V");
  if (ctor == NULL)
    return NULL;

  // fz_scale(&ctm, 1, 1);

  count = 0;
  fz_try(ctx) {
    for (annot = pdf_first_annot(ctx, (pdf_page *)page->page); annot != NULL;
         annot = pdf_next_annot(ctx, annot))
      count++;
  }
  fz_catch(ctx) { return NULL; }

  arr = (*env)->NewObjectArray(env, count, annotClass, NULL);
  if (arr == NULL)
    return NULL;

  count = 0;
  fz_try(ctx) {
    for (annot = pdf_first_annot(ctx, (pdf_page *)page->page); annot != NULL;
         annot = pdf_next_annot(ctx, annot)) {
      fz_rect rect;
      enum pdf_annot_type type = pdf_annot_type(ctx, (pdf_annot *)annot);
      rect = pdf_bound_annot(ctx, annot);
      const char *content = pdf_annot_contents(ctx, (pdf_annot *)annot);
      // jstring text = (*env)->NewStringUTF(env, content);
      size_t content_len = content ? strlen(content) : 0;
      jbyteArray textArray = (*env)->NewByteArray(env, content_len);
      if (textArray && content_len > 0) {
        (*env)->SetByteArrayRegion(env, textArray, 0, content_len,
                                   (const jbyte *)content);
      }

      jannot = (*env)->NewObject(env, annotClass, ctor, (float)rect.x0,
                                 (float)rect.y0, (float)rect.x1, (float)rect.y1,
                                 type, textArray);
      if (jannot == NULL)
        fz_throw(ctx, FZ_ERROR_GENERIC, "Failed to create annotation object");
      (*env)->SetObjectArrayElement(env, arr, count, jannot);
      (*env)->DeleteLocalRef(env, jannot);
      if (textArray) {
        (*env)->DeleteLocalRef(env, textArray);
      }

      count++;
    }
  }
  fz_catch(ctx) { return NULL; }

  return arr;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_deleteAnnotationInternal(
    JNIEnv *env, jobject thiz, jlong handle, jlong pagehandle,
    int annot_index) {
  // LOGE("deleteAnnotationInternal 1");
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;
  if (!doc_t || !doc_t->ctx || !doc_t->document) {
    return;
  }
  fz_context *ctx = doc_t->ctx;
  pdf_annot *annot;
  pdf_document *idoc = NULL;
  fz_try(ctx) { idoc = pdf_specifics(ctx, doc_t->document); }
  fz_catch(ctx) { return; }

  if (!page) {
    return;
  }

  // LOGE("deleteAnnotationInternal 2");

  fz_try(ctx) {
    DEBUG("deleteAnnotationInternal 3");
    annot = pdf_first_annot(ctx, (pdf_page *)page->page);
    DEBUG("deleteAnnotationInternal 31");
    int i;
    for (i = 0; i < annot_index && annot; i++) {
      DEBUG("deleteAnnotationInternal 32");
      annot = pdf_next_annot(ctx, annot);
      DEBUG("deleteAnnotationInternal 33");
    }

    if (annot) {
      DEBUG("deleteAnnotationInternal 4");
      // pdf_delete_annot(ctx, idoc, (pdf_page *) page->page, (pdf_annot
      // *)annot);
      pdf_delete_annot(ctx, (pdf_page *)page->page, annot);
      pdf_update_page(ctx, (pdf_page *)page->page);

      // fz_drop_display_list(ctx, page->pageList);
    }
  }
  fz_catch(ctx) {
    // LOGE("deleteAnnotationInternal: %s", ctx->error->message);
  }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_addMarkupAnnotationInternal(
    JNIEnv *env, jobject thiz, jlong handle, jlong pagehandle,
    jobjectArray points, enum pdf_annot_type type, jobjectArray jcolors) {
  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;
  if (!page) {
    return;
  }

  fz_context *ctx = doc_t->ctx;
  fz_document *doc = doc_t->document;
  pdf_document *idoc = pdf_specifics(ctx, doc);
  jclass pt_cls;
  jfieldID x_fid, y_fid;
  int i, n;
  fz_quad *pts = NULL;
  float color[3];
  float alpha = 1.0;

  if (idoc == NULL)
    return;

  switch (type) {
  case PDF_ANNOT_HIGHLIGHT:
    alpha = 0.4;
  case PDF_ANNOT_UNDERLINE:
  case PDF_ANNOT_STRIKE_OUT:
    break;
  default:
    return;
  }
  // LOGE("addMarkupAnnotationInternal 1");

  jfloat *co = (*env)->GetPrimitiveArrayCritical(env, jcolors, 0);
  color[0] = co[0];
  color[1] = co[1];
  color[2] = co[2];

  (*env)->ReleasePrimitiveArrayCritical(env, jcolors, co, 0);

  fz_var(pts);
  fz_try(ctx) {
    pdf_annot *annot;
    fz_matrix ctm;

    // LOGE("addMarkupAnnotationInternal 2");

    pt_cls = (*env)->FindClass(env, "android/graphics/PointF");
    if (pt_cls == NULL)
      fz_throw(ctx, FZ_ERROR_GENERIC, "FindClass");
    x_fid = (*env)->GetFieldID(env, pt_cls, "x", "F");
    if (x_fid == NULL)
      fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(x)");
    y_fid = (*env)->GetFieldID(env, pt_cls, "y", "F");
    if (y_fid == NULL)
      fz_throw(ctx, FZ_ERROR_GENERIC, "GetFieldID(y)");

    n = (*env)->GetArrayLength(env, points);

    // LOGE("addMarkupAnnotationInternal 3");
    pts = fz_malloc_array(ctx, n / 4, fz_quad);
    // LOGE("addMarkupAnnotationInternal 4");
    for (i = 0; i < n; i++) {
      fz_point pt;
      jobject opt = (*env)->GetObjectArrayElement(env, points, i);
      pt.x = opt ? (*env)->GetFloatField(env, opt, x_fid) : 0.0f;
      pt.y = opt ? (*env)->GetFloatField(env, opt, y_fid) : 0.0f;
      // pt = fz_transform_point(pt, ctm);

      // Refer to underlineText method in VerticalModeController.java for the
      // order of points in quadPoints
      switch (i % 4) {
      case 0:
        pts[i / 4].ll = pt;
        break;
      case 1:
        pts[i / 4].lr = pt;
        break;
      case 2:
        pts[i / 4].ur = pt;
        break;
      case 3:
        pts[i / 4].ul = pt;
        break;
      }

      (*env)->DeleteLocalRef(env, opt);
    }

    // LOGE("addMarkupAnnotationInternal 5");
    annot =
        (pdf_annot *)pdf_create_annot_raw(ctx, (pdf_page *)page->page, type);
    pdf_set_annot_quad_points(ctx, (pdf_annot *)annot, n / 4, pts);
    pdf_set_annot_color(ctx, annot, 3, color);
    pdf_set_annot_opacity(ctx, annot, alpha);
    pdf_update_annot(ctx, annot);
    pdf_update_page(ctx, (pdf_page *)page->page);
    // dump_annotation_display_lists(glo);
  }
  fz_always(ctx) { fz_free(ctx, pts); }
  fz_catch(ctx) {
    // LOGE("addStrikeOutAnnotation: %s failed", ctx->error->message);
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls != NULL)
      (*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_searchPage");
    (*env)->DeleteLocalRef(env, cls);
  }
}

static void fz_print_stext_image_as_html_my(fz_context *ctx, fz_output *out,
                                            fz_stext_block *block) {

  fz_write_printf(ctx, out, "<image-begin>");

  fz_write_image_as_data_uri(ctx, out, block->u.i.image);

  fz_write_string(ctx, out, "<image-end>");
  fz_write_printf(ctx, out, "<br/>");
}

void fz_print_stext_block_as_html_my(fz_context *ctx, fz_output *out,
                                     fz_stext_block *block) {
  fz_stext_line *line;
  fz_stext_char *ch;
  int x, y;

  fz_font *font = NULL;
  float size = 0;
  int sup = 0;
  int color = 0;

  float fs = block->u.t.first_line->first_char->size;

  if (fs > fontSize) {
    fz_write_printf(ctx, out, "<pause>");
  }

  fz_write_printf(ctx, out, "<p>");

  fz_font *block1 = block->u.t.first_line->first_char->font;
  fz_font *block2 = block->u.t.last_line->last_char->font;

  int is_block_bold =
      fz_font_is_bold(ctx, block1) && fz_font_is_bold(ctx, block2);
  int is_block_italic =
      fz_font_is_italic(ctx, block1) && fz_font_is_italic(ctx, block2);

  if (is_block_bold)
    fz_write_printf(ctx, out, "<b>");
  if (is_block_italic)
    fz_write_printf(ctx, out, "<i>");

  for (line = block->u.t.first_line; line; line = line->next) {
    font = NULL;
    int i, n;
    char utf[10];
    for (ch = line->first_char; ch; ch = ch->next) {
      int is_bold_ch = !is_block_bold && fz_font_is_bold(ctx, ch->font);
      int is_italic_ch = !is_block_italic && fz_font_is_italic(ctx, ch->font);

      if (is_bold_ch)
        fz_write_printf(ctx, out, "<b>");
      if (is_italic_ch)
        fz_write_printf(ctx, out, "<i>");

      switch (ch->c) {
      case '<':
        fz_write_string(ctx, out, "&lt;");
        break;
      case '>':
        fz_write_string(ctx, out, "&gt;");
        break;
      case '&':
        fz_write_string(ctx, out, "&amp;");
        break;
      case '"':
        fz_write_string(ctx, out, "&quot;");
        break;
        // case '\'': fz_write_string(ctx, out, "&apos;"); break;
      default:
        n = fz_runetochar(utf, ch->c);
        for (i = 0; i < n; i++)
          fz_write_byte(ctx, out, utf[i]);
        break;
      }
      if (is_bold_ch && is_italic_ch)
        fz_write_printf(ctx, out, "</i></b>");
      else if (is_bold_ch)
        fz_write_printf(ctx, out, "</b>");
      else if (is_italic_ch)
        fz_write_printf(ctx, out, "</i>");
    }
    fz_write_string(ctx, out, " ");
  }
  if (is_block_bold && is_block_italic)
    fz_write_printf(ctx, out, "</i></b>");
  else if (is_block_bold)
    fz_write_printf(ctx, out, "</b>");
  else if (is_block_italic)
    fz_write_printf(ctx, out, "</i>");

  fz_write_string(ctx, out, "</p>\n");
  if (fs > fontSize) {
    fz_write_string(ctx, out, "<pause>\n");
  }
}

void fz_print_stext_page_as_html_my(fz_context *ctx, fz_output *out,
                                    fz_stext_page *page, int id) {
  fz_stext_block *block;

  for (block = page->first_block; block; block = block->next) {
    if (block->type == FZ_STEXT_BLOCK_IMAGE)
      fz_print_stext_image_as_html_my(ctx, out, block);
    else if (block->type == FZ_STEXT_BLOCK_TEXT)
      fz_print_stext_block_as_html_my(ctx, out, block);
  }
}

JNIEXPORT jbyteArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getPageAsHtml(
    JNIEnv *env, jobject thiz, jlong handle, jlong pagehandle, jint jopts) {

  renderdocument_t *doc_t = (renderdocument_t *)(long)handle;
  renderpage_t *page = (renderpage_t *)(long)pagehandle;

  fz_context *ctx = doc_t->ctx;
  fz_document *doc = doc_t->document;
  pdf_document *idoc = pdf_specifics(ctx, doc);

  if (!ctx || !doc || !page) {
    return NULL;
  }

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

  fz_try(ctx) {
    int b, l, s, c;
    // fz_rect mediabox;

    ctm = fz_identity;

    fz_stext_options opts = {0};

    if (jopts == 4) {
      opts.flags = FZ_STEXT_PRESERVE_IMAGES;
    }

    text = fz_new_stext_page(ctx, fz_bound_page(ctx, page->page));

    dev = fz_new_stext_device(ctx, text, &opts);

    fz_run_page(ctx, page->page, dev, ctm, NULL);
    fz_close_device(ctx, dev);
    // fz_drop_device(ctx, dev);
    // dev = NULL;

    // fz_analyze_text(ctx, sheet, text);

    buf = fz_new_buffer(ctx, 256);
    out = fz_new_output_with_buffer(ctx, buf);

    fz_print_stext_page_as_html_my(ctx, out, text, page->number);
    fz_close_output(ctx, out);

    // fz_drop_output(ctx, out);
    // out = NULL;

    len = fz_buffer_storage(ctx, buf, &data);

    // bArray = (*env)->NewByteArray(env, buf->len);
    bArray = (*env)->NewByteArray(env, len);
    if (bArray == NULL)
      fz_throw(ctx, FZ_ERROR_GENERIC, "Failed to make byteArray");
    (*env)->SetByteArrayRegion(env, bArray, 0, len, (const jbyte *)data);
  }
  fz_always(ctx) {
    fz_drop_stext_page(ctx, text);
    // fz_close_device(ctx, dev);
    fz_drop_device(ctx, dev);
    fz_drop_output(ctx, out);
    fz_drop_buffer(ctx, buf);
  }
  fz_catch(ctx) {
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls != NULL)
      (*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_textAsHtml");
    (*env)->DeleteLocalRef(env, cls);

    return NULL;
  }

  return bArray;
}
