LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ARM_MODE := $(MY_ARM_MODE)
LOCAL_MODULE    := djvu
LOCAL_CFLAGS    += -I$(MUPDF_ROOT)/thirdparty/libjpeg -fexceptions -DHAVE_CONFIG_H  -DTHREADMODEL=POSIXTHREADS

LOCAL_CFLAGS += $(MY_O)

LOCAL_SRC_FILES := Arrays.cpp BSByteStream.cpp BSEncodeByteStream.cpp ByteStream.cpp DataPool.cpp DjVmDir.cpp DjVmDir0.cpp DjVmDoc.cpp DjVmNav.cpp DjVuAnno.cpp DjVuDocument.cpp DjVuDumpHelper.cpp DjVuErrorList.cpp DjVuFile.cpp DjVuFileCache.cpp DjVuGlobal.cpp DjVuGlobalMemory.cpp DjVuImage.cpp DjVuInfo.cpp DjVuMessage.cpp DjVuMessageLite.cpp DjVuNavDir.cpp DjVuPalette.cpp DjVuPort.cpp DjVuText.cpp GBitmap.cpp GContainer.cpp GException.cpp GIFFManager.cpp GMapAreas.cpp GOS.cpp GPixmap.cpp GRect.cpp GScaler.cpp GSmartPointer.cpp GString.cpp GThreads.cpp GURL.cpp GUnicode.cpp IFFByteStream.cpp IW44Image.cpp IW44EncodeCodec.cpp JB2Image.cpp JPEGDecoder.cpp MMRDecoder.cpp MMX.cpp UnicodeByteStream.cpp XMLParser.cpp XMLTags.cpp ZPCodec.cpp atomic.cpp debug.cpp ddjvuapi.cpp miniexp.cpp

include $(BUILD_STATIC_LIBRARY)
