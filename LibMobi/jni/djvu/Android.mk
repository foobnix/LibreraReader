LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := djvu

LOCAL_CFLAGS    := -fexceptions -D__APPLE__ -DHAVE_CONFIG_H

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/my \
	$(LOCAL_PATH)/../jpeg-turbo/include

LOCAL_SRC_FILES :=  \
	my/DjvuDroidBridge.cpp \
	src/Arrays.cpp \
	src/BSByteStream.cpp \
	src/BSEncodeByteStream.cpp \
	src/ByteStream.cpp \
	src/DataPool.cpp \
	src/DjVmDir.cpp \
	src/DjVmDir0.cpp \
	src/DjVmDoc.cpp \
	src/DjVmNav.cpp \
	src/DjVuAnno.cpp \
	src/DjVuDocument.cpp \
	src/DjVuDumpHelper.cpp \
	src/DjVuErrorList.cpp \
	src/DjVuFile.cpp \
	src/DjVuFileCache.cpp \
	src/DjVuGlobal.cpp \
	src/DjVuGlobalMemory.cpp \
	src/DjVuImage.cpp \
	src/DjVuInfo.cpp \
	src/DjVuMessage.cpp \
	src/DjVuMessageLite.cpp \
	src/DjVuNavDir.cpp \
	src/DjVuPalette.cpp \
	src/DjVuPort.cpp \
	src/DjVuText.cpp \
	src/GBitmap.cpp \
	src/GContainer.cpp \
	src/GException.cpp \
	src/GIFFManager.cpp \
	src/GMapAreas.cpp \
	src/GOS.cpp \
	src/GPixmap.cpp \
	src/GRect.cpp \
	src/GScaler.cpp \
	src/GSmartPointer.cpp \
	src/GString.cpp \
	src/GThreads.cpp \
	src/GURL.cpp \
	src/GUnicode.cpp \
	src/IFFByteStream.cpp \
	src/IW44Image.cpp \
	src/IW44EncodeCodec.cpp \
	src/JB2Image.cpp \
	src/JPEGDecoder.cpp \
	src/MMRDecoder.cpp \
	src/MMX.cpp \
	src/UnicodeByteStream.cpp \
	src/XMLParser.cpp \
	src/XMLTags.cpp \
	src/ZPCodec.cpp \
	src/atomic.cpp \
	src/debug.cpp \
	src/ddjvuapi.cpp \
	src/miniexp.cpp

LOCAL_STATIC_LIBRARIES := jpeg-turbo #standalone 
LOCAL_LDLIBS := -lm -llog -ljnigraphics

LOCAL_ARM_MODE := $(APP_ARM_MODE)

include $(BUILD_STATIC_LIBRARY)

