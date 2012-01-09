/*
 * YUV420をRGB888に変更する
 */

#include <jni.h>
#include <android/log.h>
#include <stdio.h>

#define EXPORT __attribute__((visibility("default")))
#define LOG_TAG ("decodeYUV420SP")
#ifdef DEBUG
#define LOGD(... ) ((void)__android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__))
#else
#define LOGD(... )
#endif

void decodeYUVtoRGB565(jbyte *yuv420sp,jint *jrgb,jint width,jint height)
{
	int j,i;
	int yp;
	int r,g,b;
	int y1192;
	int y,u,v;
	int uvp;
	short *rgb = (short*)jrgb;
	int frameSize = width * height; 
	for ( j = 0, yp = 0; j < height; j++) {   
		uvp = frameSize + (j >> 1) * width;
		u = 0;
		v = 0;   
		for (i = 0; i < width; i++, yp++) {   
			y = (0xff & ((int) *(yuv420sp+yp))) - 16;   
			if (y < 0) y = 0;   
			if ((i & 1) == 0) {   
				v = (0xff & *(yuv420sp+uvp)) - 128;   
				uvp++;
				u = (0xff & *(yuv420sp+uvp)) - 128;
				uvp++;
			}   

			y1192 = 1192 * y;   
			r = (y1192 + 1634 * v);   
			g = (y1192 - 833 * v - 400 * u);   
			b = (y1192 + 2066 * u);   

			if (r < 0) r = 0; else if (r > 262143) r = 262143;   
			if (g < 0) g = 0; else if (g > 262143) g = 262143;   
			if (b < 0) b = 0; else if (b > 262143) b = 262143;   

			*(rgb+yp) = (short)((r & 0x03e000) >> 2) | ((g & 0x03f000) >> 7 ) | ((b & 0x03e000 >> 13));   
		}
	}

}
void decodeYUV(jbyte *yuv420sp,jint *rgb,jint width,jint height)
{
	int j,i;
	int yp;
	int r,g,b;
	int y1192;
	int y,u,v;
	int uvp;
	int frameSize = width * height; 
	for ( j = 0, yp = 0; j < height; j++) {   
		uvp = frameSize + (j >> 1) * width;
		u = 0;
		v = 0;   
		for (i = 0; i < width; i++, yp++) {   
			y = (0xff & ((int) *(yuv420sp+yp))) - 16;   
			if (y < 0) y = 0;   
			if ((i & 1) == 0) {   
				v = (0xff & *(yuv420sp+uvp)) - 128;   
				uvp++;
				u = (0xff & *(yuv420sp+uvp)) - 128;
				uvp++;
			}   

			y1192 = 1192 * y;   
			r = (y1192 + 1634 * v);   
			g = (y1192 - 833 * v - 400 * u);   
			b = (y1192 + 2066 * u);   

			if (r < 0) r = 0; else if (r > 262143) r = 262143;   
			if (g < 0) g = 0; else if (g > 262143) g = 262143;   
			if (b < 0) b = 0; else if (b > 262143) b = 262143;   

			*(rgb+yp) = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &  0xff00) | ((b >> 10) & 0xff);   


		}
	}
}
jintArray Java_jp_peisun_android_facedetect_CameraSurfaceView_decodeYUV420SP(JNIEnv *env,jobject thiz,jbyteArray data,jint width,jint height)
{
	jint *rgb;
	jintArray pIntArray;
	jboolean isCopy = JNI_TRUE;
	jbyte *jyuv;
	/* YUVのデータをアクセスできるように */
	jyuv = (*env)->GetByteArrayElements(env,data,NULL);
	LOGD("GetByteArrayElements %x",jyuv);
	/* RGBのメモリを確保 */
	pIntArray = (*env)->NewIntArray(env,width*height);
	LOGD("NewIntArray %x",pIntArray);
	rgb = (*env)->GetIntArrayElements(env,pIntArray,&isCopy);
	LOGD("GetIntArrayElements %x",rgb);

	decodeYUV(jyuv,rgb,width,height);
	LOGD("decodeYUV end");

	/* YUVのデータの参照を解放 */
	(*env)->ReleaseByteArrayElements(env,data,(jbyte*)jyuv,0);
	/* RGBのメモリの参照を解放 */
	(*env)->ReleaseIntArrayElements(env,pIntArray,rgb,0);
//	(*env)->DeleteLocalRef(env,pIntArray);
	return pIntArray;
}
jintArray Java_jp_peisun_android_facedetect_CameraSurfaceView_decodeYUV420TORGB565(JNIEnv *env,jobject thiz,jbyteArray data,jint width,jint height)
{
	jint *rgb;
	jintArray pIntArray;
	jboolean isCopy = JNI_TRUE;
	jbyte *jyuv;
	/* YUVのデータをアクセスできるように */
	jyuv = (*env)->GetByteArrayElements(env,data,NULL);
	LOGD("GetByteArrayElements %x",jyuv);
	/* RGBのメモリを確保 */
	pIntArray = (*env)->NewIntArray(env,width*height/2);
	LOGD("NewIntArray %x",pIntArray);
	rgb = (*env)->GetIntArrayElements(env,pIntArray,&isCopy);
	LOGD("GetIntArrayElements %x",rgb);

	decodeYUVtoRGB565(jyuv,rgb,width,height);
	LOGD("decodeYUV end");

	/* YUVのデータの参照を解放 */
	(*env)->ReleaseByteArrayElements(env,data,(jbyte*)jyuv,0);
	/* RGBのメモリの参照を解放 */
	(*env)->ReleaseIntArrayElements(env,pIntArray,rgb,0);
//	(*env)->DeleteLocalRef(env,pIntArray);
	return pIntArray;
}
