LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := standalone

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include

LOCAL_SRC_FILES := \
	src/StBridge.cpp \
	src/StProtocol.cpp \
	src/StQueue.cpp \
	src/StRequestQueue.cpp \
	src/StResponseQueue.cpp \
	src/StStringNaturalCompare.cpp \
	src/StSocket.cpp

LOCAL_ARM_MODE := $(APP_ARM_MODE)


include $(BUILD_STATIC_LIBRARY)
