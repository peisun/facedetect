package jp.peisun.android.facedetect;

import android.graphics.Bitmap;

public class DecodeYUV {
	public static final int SCALE_NORMAL = 0;
	public static final int SCALE_DOWN = 1;
	public static final int SCALE_DOWN_ROTATE = 2;
	public native static int[] decodeYUV420SP(byte[] data,int width,int height,int scale);
	public native static void createBitmapYUVtoRGB565(byte[] data,int width,int height,Bitmap bitmap,int scale);
	
	static {
		System.loadLibrary("decodeYUV_jni");
	}
}
