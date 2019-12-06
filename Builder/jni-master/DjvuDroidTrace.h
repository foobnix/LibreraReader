/*
 * DjvuDroidTrace.h
 *
 *  Created on: 21.01.2010
 *      Author: Cool
 */

#ifndef DJVUDROIDTRACE_H_
#define DJVUDROIDTRACE_H_
#include <android/log.h>

#define DJVU_DROID "DjvuDroidNativeCodec"

#define DEBUG_PRINT(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, DJVU_DROID, fmt, args)
#define DEBUG_WRITE(str) __android_log_write(ANDROID_LOG_DEBUG, DJVU_DROID, str)

#endif /* DJVUDROIDTRACE_H_ */
