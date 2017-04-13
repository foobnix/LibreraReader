#include <jni.h>
#include <android/log.h>

#include "libs/fb2toepub/hdr.h"
#include "libs/fb2toepub/streamzip.h"
#include "libs/fb2toepub/streamconv.h"
#include "libs/fb2toepub/scanner.h"
#include "libs/fb2toepub/fb2toepubconv.h"


#define LG_TAG "EbookConv"
#define LOGD(format, args...)  __android_log_print(ANDROID_LOG_DEBUG, LG_TAG, format, ##args);
#define LOGE(format, args...)  __android_log_print(ANDROID_LOG_ERROR, LG_TAG, format, ##args);

static std::string fromJstring(JNIEnv *env, jstring js) {
	const char *_js = env->GetStringUTFChars(js, NULL);
	std::string outStr(_js, env->GetStringUTFLength(js));
	env->ReleaseStringUTFChars(js, _js);
	return outStr;
}

using namespace Fb2ToEpub;

extern "C"
JNIEXPORT int JNICALL Java_com_foobnix_libfb2_Fb2Converter_fb2ToEpubNative(
	JNIEnv *env, jclass cls,
	jstring fb2FileName, jstring epubFileName, jstring cssDir, jstring fontsDir)
{
	std::string fnameFb2 = fromJstring(env, fb2FileName);
	std::string fnameEpub = fromJstring(env, epubFileName);
	std::string dirCss = fromJstring(env, cssDir);
	std::string dirFonts = fromJstring(env, fontsDir);

	int ret;
	try
	{
		// create input stream
		Ptr<InStm> pin = CreateInUnicodeStm(CreateUnpackStm(fnameFb2.c_str()));

		// create output stream
		Ptr<OutPackStm> pout = CreatePackStm(fnameEpub.c_str());
		bool fOutputFileCreated = true;

		// create translite converter
		Ptr<XlitConv> xlitConv; // empty transliterator, we don't need it.

		strvector css, fonts, mfonts;
		if (dirCss > "")
			css.push_back(dirCss);
		if (dirFonts > "")
			fonts.push_back(dirFonts);

		ret = Convert(pin, css, fonts, mfonts, xlitConv, pout);
	}
	catch (InternalException& ei)
	{
		LOGE("InternalException in Convert(): %s, file: %s, line: %d", ei.what(), ei.File(), ei.Line());
		ret = -3;
	}
	catch (ParserException& ep)
	{
		LOGE("ParserException in Convert(): %s, file: %s, 1st line %d, last line %d", ep.what(), ep.File(), ep.Location().fstLn_, ep.Location().lstLn_);
		ret = -4;
	}
	catch (FontException& ef)
	{
		LOGE("FontException in Convert(): %s, file: %s", ef.what(), ef.File());
		ret = -5;
	}
	catch (IOException& e)
	{
		LOGE("IOException in Convert(): %s, file: %s", e.what(), e.File());
		ret = -1;
	}
	catch (ExternalException& ee)
	{
		LOGE("Exception in Convert(): %s", ee.what());
		ret = -2;
	}
	catch (...)
	{
		LOGE("Unknown exception in Convert.");
		ret = -9;
	}

	return ret;
}
