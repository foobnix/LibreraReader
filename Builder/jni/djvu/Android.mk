LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := djvu

LOCAL_CFLAGS    := $(APP_CFLAGS) -fexceptions -DHAVE_CONFIG_H
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_C_INCLUDES := \
    $(MUPDF_ROOT)/thirdparty/libjpeg \
    $(MUPDF_ROOT)/scripts/libjpeg \
	$(LOCAL_PATH)/libdjvu \

LOCAL_SRC_FILES := \
	libdjvu/Arrays.cpp \
	libdjvu/BSByteStream.cpp \
	libdjvu/BSEncodeByteStream.cpp \
	libdjvu/ByteStream.cpp \
	libdjvu/DataPool.cpp \
	libdjvu/DjVmDir.cpp \
	libdjvu/DjVmDir0.cpp \
	libdjvu/DjVmDoc.cpp \
	libdjvu/DjVmNav.cpp \
	libdjvu/DjVuAnno.cpp \
	libdjvu/DjVuDocument.cpp \
	libdjvu/DjVuDumpHelper.cpp \
	libdjvu/DjVuErrorList.cpp \
	libdjvu/DjVuFile.cpp \
	libdjvu/DjVuFileCache.cpp \
	libdjvu/DjVuGlobal.cpp \
	libdjvu/DjVuGlobalMemory.cpp \
	libdjvu/DjVuImage.cpp \
	libdjvu/DjVuInfo.cpp \
	libdjvu/DjVuMessage.cpp \
	libdjvu/DjVuMessageLite.cpp \
	libdjvu/DjVuNavDir.cpp \
	libdjvu/DjVuPalette.cpp \
	libdjvu/DjVuPort.cpp \
	libdjvu/DjVuText.cpp \
	libdjvu/DjVuToPS.cpp \
	libdjvu/GBitmap.cpp \
	libdjvu/GContainer.cpp \
	libdjvu/GException.cpp \
	libdjvu/GIFFManager.cpp \
	libdjvu/GMapAreas.cpp \
	libdjvu/GOS.cpp \
	libdjvu/GPixmap.cpp \
	libdjvu/GRect.cpp \
	libdjvu/GScaler.cpp \
	libdjvu/GSmartPointer.cpp \
	libdjvu/GString.cpp \
	libdjvu/GThreads.cpp \
	libdjvu/GURL.cpp \
	libdjvu/GUnicode.cpp \
	libdjvu/IFFByteStream.cpp \
	libdjvu/IW44Image.cpp \
	libdjvu/IW44EncodeCodec.cpp \
	libdjvu/JB2Image.cpp \
	libdjvu/JPEGDecoder.cpp \
	libdjvu/MMRDecoder.cpp \
	libdjvu/MMX.cpp \
	libdjvu/UnicodeByteStream.cpp \
	libdjvu/XMLParser.cpp \
	libdjvu/XMLTags.cpp \
	libdjvu/ZPCodec.cpp \
	libdjvu/atomic.cpp \
	libdjvu/debug.cpp \
	libdjvu/ddjvuapi.cpp \
	libdjvu/miniexp.cpp

include $(BUILD_STATIC_LIBRARY)
