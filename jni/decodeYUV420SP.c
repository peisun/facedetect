/*
 * YUV420をRGBに変更する
 */
#include "decodeYUV.h"

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

	decodeYUVtoRGB8888(jyuv,rgb,width,height);
	LOGD("decodeYUV end");

	/* YUVのデータの参照を解放 */
	(*env)->ReleaseByteArrayElements(env,data,(jbyte*)jyuv,0);
	/* RGBのメモリの参照を解放 */
	(*env)->ReleaseIntArrayElements(env,pIntArray,rgb,0);
//	(*env)->DeleteLocalRef(env,pIntArray);
	return pIntArray;
}
#if 0
static uint16_t make565(int red,int green,int blue)
{
    return (uint16_t)( ((red   << 8) & 0xf800) |
                       ((green << 2) & 0x03e0) |
                       ((blue  >> 3) & 0x001f) );
}
static int makeYUVtoRGB888(jbyte *yuv420sp)
{
}
static uint16_t makeYUVtoRGB565(jint *yuv)
{
/*
	int red   = (*yuv & 0x00f80000) >> 19;
	int green = (*yuv & 0x0000fc00) >>10;
	int blue  = (*yuv & 0x000000f8) >> 3;
*/
	return (uint16_t) (*yuv & 0x00f80000) >> 19 | (*yuv & 0x0000fc00) >>10 | (*yuv & 0x000000f8) >> 3;
}
#endif
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
		for (i = 0; i < info->width; i++, yp++) {   
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

			*(rgb565+yp) = ((r << 8) & 0xf800 | (g << 2) & 0x3e00 | (b >> 3) & 0x001f);


		}
	}
}
JNIEXPORT void JNICALL Java_jp_peisun_android_facedetect_CameraSurfaceView_createBitmapYUVtoRGB565(JNIEnv *env,jobject thiz,jbyteArray data,jobject bitmap)
{
	AndroidBitmapInfo info;
	jbyte *jyuv;
	int ret;
	void * pixels;

	/* bitmapの情報を取得 */
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGD("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
	/* とりあえずサポートするのはRGB565 */
    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        LOGD("Bitmap format is not RGB_565 !");
        return;
    }
	/* bitmapをlock */
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGD("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
	/* YUVのデータをアクセスできるように */
	jyuv = (*env)->GetByteArrayElements(env,data,NULL);
	LOGD("GetByteArrayElements %x",jyuv);

	/* デコード */
	decodeYUVtoRGB565(&info,pixels,jyuv);

	/* bitmap をunlock */
    AndroidBitmap_unlockPixels(env, bitmap);
	/* YUVのデータの参照を解放 */
	(*env)->ReleaseByteArrayElements(env,data,(jbyte*)jyuv,0);

	return ;
}
