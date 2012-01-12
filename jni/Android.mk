LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := decodeYUV_jni
LOCAL_SRC_FILES := decodeYUV420SP.c

LOCAL_LDLIBS := -lm -llog -ljnigraphics -DDEBUG
include $(BUILD_SHARED_LIBRARY)
