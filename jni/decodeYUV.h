#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <stdio.h>

#define EXPORT __attribute__((visibility("default")))
#define LOG_TAG ("Facedetect")
#ifdef DEBUG
#define LOGD(... ) ((void)__android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__))
#define LOGE(... ) ((void)__android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__))
#else
#define LOGD(... )
#define LOGE(... )
#endif
