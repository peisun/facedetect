/*
 * YUV420をRGBに変更する
 */
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <stdio.h>


//#define EXPORT __attribute__((visibility("default")))
#define LOG_TAG ("decodeYUV")
//#ifdef DEBUG
#define LOGD(... ) ((void)__android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__))
#define LOGE(... ) ((void)__android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__))
//#else
//#define LOGD(... )
//#define LOGE(... )
//#endif
#define SCALE_NORMAL 0
#define SCALE_DOWN 1
/*
 * ARGB8888に変更する
 */
void decodeYUVtoRGB8888(jbyte *yuv420sp,jint *rgb,jint width,jint height)
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
void scaledown_decodeYUVtoRGB8888(jbyte *yuv420sp,jint *rgb,jint width,jint height)
{
	int j,i;
	int yp;
	int r,g,b;
	int y1192;
	int y,u,v;
	int uvp;
	int frameSize = (width * height); 
	int rgbp = 0;
	for ( j = 0, yp = 0; j < height; j+=2) {   
		uvp = frameSize + (j >> 1) * width;
		u = 0;
		v = 0;   
		for (i = 0; i < width; i+=2, yp+=2) {   
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

			*(rgb+rgbp) = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &  0xff00) | ((b >> 10) & 0xff);   
			rgbp++;


		}
		yp += width;

	}
}
static uint16_t make565(int red,int green,int blue)
{
    return (uint16_t)( ((red   << 8) & 0xf800) |
                       ((green << 2) & 0x03e0) |
                       ((blue  >> 3) & 0x001f) );
}
static uint16_t makeYUVtoRGB565(int red,int green,int blue)
{
/*
	int red   = (*yuv & 0x00f80000) >> 19;
	int green = (*yuv & 0x0000fc00) >>10;
	int blue  = (*yuv & 0x000000f8) >> 3;
*/
	return (uint16_t) ((red >> 2) & 0x0f800 | (green >> 7) & 0x07e0  | (blue >> 13) & 0x001f);
}
/*
 * RGB565に変更する
 */
void decodeYUVtoRGB565(AndroidBitmapInfo *info,void *pixels,jbyte *yuv420sp)
{
	int j,i;
	int yp;
	int r,g,b;
	int y1192;
	int y,u,v;
	int uvp;
	uint16_t *rgb565 = (uint16_t*)pixels;
	int frameSize = info->width * info->height; 
	for ( j = 0, yp = 0; j < info->height; j++) {   
		uvp = frameSize + (j >> 1) * info->width;
		u = 0;
		v = 0;   
		for (i = 0; i < info->width; i++) {   
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

//			*(rgb565+yp) = (uint16_t)((r << 8) & 0xf800 | (g << 2) & 0x3e00 | (b >> 3) & 0x001f);
			*(rgb565+yp) = makeYUVtoRGB565(r,g,b);
			yp++;
			
			

		}
	}
}
void scaledown_decodeYUVtoRGB565(AndroidBitmapInfo *info,void *pixels,jbyte *yuv420sp,int width,int height)
{
	int j,i;
	int yp;
	int r,g,b;
	int y1192;
	int y,u,v;
	int uvp;
	uint16_t *rgb565 = (uint16_t*)pixels;
	int rgbp = 0;

	int frameSize = width * height; 
	for ( j = 0, yp = 0; j < height; j+=2) {   
		uvp = frameSize + (j >> 1) * width;
		u = 0;
		v = 0;   
		for (i = 0; i < width; i+=2,yp+=2) {   
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

//			*(rgb565+yp) = (uint16_t)((r << 8) & 0xf800 | (g << 2) & 0x3e00 | (b >> 3) & 0x001f);
			*(rgb565+rgbp) = makeYUVtoRGB565(r,g,b);
			rgbp++;
			
			

		}
		yp += width;
	}
}
jintArray Java_jp_peisun_android_facedetect_DecodeYUV_decodeYUV420SP(JNIEnv *env,jobject thiz,jbyteArray data,jint width,jint height,jint scale)
{
	jint *rgb;
	jintArray pIntArray;
	jboolean isCopy = JNI_TRUE;
	jbyte *jyuv;
	jint framesize;

	/* YUVのデータをアクセスできるように */
	jyuv = (*env)->GetByteArrayElements(env,data,NULL);
	LOGD("GetByteArrayElements %x",jyuv);

	if(scale == SCALE_NORMAL){
		framesize = width*height;
		/* RGBのメモリを確保 */
		pIntArray = (*env)->NewIntArray(env,framesize);
		LOGD("NewIntArray %x",pIntArray);
		rgb = (*env)->GetIntArrayElements(env,pIntArray,&isCopy);
		LOGD("GetIntArrayElements %x",rgb);
		decodeYUVtoRGB8888(jyuv,rgb,width,height);
	}
	else if(scale == SCALE_DOWN){
		LOGD("scale down");
		framesize = width/2 * height/2;
		/* RGBのメモリを確保 */
		pIntArray = (*env)->NewIntArray(env,framesize);
		LOGD("NewIntArray %x",pIntArray);
		rgb = (*env)->GetIntArrayElements(env,pIntArray,&isCopy);
		LOGD("GetIntArrayElements %x",rgb);
		scaledown_decodeYUVtoRGB8888(jyuv,rgb,width,height);
	}
	else {
		LOGE("unknown scale");
	}
	LOGD("decodeYUV end");

	/* YUVのデータの参照を解放 */
	(*env)->ReleaseByteArrayElements(env,data,(jbyte*)jyuv,0);
	/* RGBのメモリの参照を解放 */
	(*env)->ReleaseIntArrayElements(env,pIntArray,rgb,0);
//	(*env)->DeleteLocalRef(env,pIntArray);
	return pIntArray;
}
void Java_jp_peisun_android_facedetect_DecodeYUV_createBitmapYUVtoRGB565(JNIEnv *env,jobject thiz,jbyteArray data,jint width,jint height,jobject bitmap,jint scale)
{
	AndroidBitmapInfo info;
	jbyte *jyuv;
	int ret;
	void * pixels;

//	LOGD("createBitmapYUVtoRGB565 jni");

	/* bitmapの情報を取得 */
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
	/* とりあえずサポートするのはRGB565 */
    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        LOGE("Bitmap format is not RGB_565 !");
        return;
    }
	/* bitmapをlock */
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
	/* YUVのデータをアクセスできるように */
	jyuv = (*env)->GetByteArrayElements(env,data,NULL);
//	LOGD("GetByteArrayElements %x",jyuv);

	/* デコード */
	if(scale == SCALE_NORMAL){
		decodeYUVtoRGB565(&info,pixels,jyuv);
		
	}
	else if(scale == SCALE_DOWN){
		if(info.width == width/2 && info.height == height/2){
			LOGE("scaledown_decode info.width = %d info.height = %d",info.width,info.height);
			scaledown_decodeYUVtoRGB565(&info,pixels,jyuv,width,height);
		}
		else {
			LOGE("Bitmap size error ");
		}
	}
	else {
		LOGE("unknown scale");
	}

	/* bitmap をunlock */
    AndroidBitmap_unlockPixels(env, bitmap);
	/* YUVのデータの参照を解放 */
	(*env)->ReleaseByteArrayElements(env,data,(jbyte*)jyuv,0);

	return ;
}
