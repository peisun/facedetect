package jp.peisun.android.facedetect;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import 	android.graphics.PorterDuff.Mode;


public class CameraSurfaceView extends SurfaceView 
implements SurfaceHolder.Callback ,Camera.PreviewCallback {
	private Context mContext = null;
	private SurfaceHolder mHolder = null;
	private Camera mCamera = null;
	private Size mSize = null;
	private int mOrientation = 0;
	private int defaultCameraId = 0;
	private final String TAG = "Facedetect:CameraSurface";
	private List<Camera.Size> mPreviewSize = null;
	private Camera.Parameters mCameraParameters =null;
	private FaceDetector mFaceDetector = null;
	private Bitmap c00 = null;
	private Rect c00_src = null;
	
	private int mDispWidth = 0;
	private int mDispHeight = 0;
	public CameraSurfaceView(Context context) {
		super(context);
		// TODO 自動生成されたコンストラクター・スタブ
		mContext = context;
		mHolder = getHolder();
		mHolder.addCallback(this);
		//mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		
		// 笑い男のbitmap
		c00 = BitmapFactory.decodeResource(this.getResources(), R.drawable.c00);
		// 描写元の設定
        Rect c00_src = new Rect(); 
        c00_src.left = 0 ;
        c00_src.top = 0 ;
        c00_src.right = c00.getWidth() ;
        c00_src.bottom = c00.getHeight() ;
        
        /* ディスプレイサイズを取得しておく */
        getDisplaySize();
        
		// 利用可能なカメラの個数を取得
		int numberOfCameras = Camera.getNumberOfCameras();
		// CameraInfoからバックフロントカメラのidを取得
		CameraInfo cameraInfo = new CameraInfo();	
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				defaultCameraId = i;
			}
		}



	}
	public void getDisplaySize(){
		WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		mDispWidth = disp.getWidth();
		mDispHeight = disp.getHeight();
		
	}
	public int getMinPreviewFps(){
		List<int[]> fps = mCameraParameters.getSupportedPreviewFpsRange();
		int[] range = fps.get(0);
		return range[0];
	}
	public List<Camera.Size> getPreviewSizeList(){
		Camera.Size setSize = null;
		List<Camera.Size> mPreviewSize = (List<Camera.Size>) mCameraParameters.getSupportedPreviewSizes();


		return mPreviewSize;
	}
	protected int getPortrait() {

		boolean portrait = (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		if (portrait) {
			return 90;
		}
		else{
			return 0;
		}
	}
	private void cameraPreviewStart(){
		// Set orientation
		mOrientation = getPortrait();

		try {
			mCamera.setDisplayOrientation(mOrientation);
			//mCamera.setPreviewDisplay();
			mCamera.setOneShotPreviewCallback(this);
			mCamera.startPreview();

		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		} 
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO 自動生成されたメソッド・スタブ
		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}
		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}
		//Camera.Size setSize = getOptimalPreviewSize(getPreviewSizeList(),width,height);
		Camera.Size setSize = getResizePreviewSize(getPreviewSizeList(),width,height);
		// start preview with new settings

		if(setSize != null){
			try {
				mCameraParameters.setPreviewSize(setSize.width,setSize.height);
				//mCameraParameters.setPreviewFormat(ImageFormat.RGB_565);
				//int range = getMinPreviewFps();
				//mCameraParameters.setPreviewFpsRange(range, range);
				mCamera.setParameters(mCameraParameters);
				Log.d(TAG,"setPreviewSize " +setSize.width+" "+ setSize.height );

			}
			catch(Exception e){
				Log.d(TAG,"Error camera parameter " + e.getMessage());
				e.printStackTrace();
			}
		}	

		cameraPreviewStart();

	}
	private Size getResizePreviewSize(List<Size> sizes,int w, int h){
		Size reSize = null;
		for(Size size: sizes){
			if(size.width < w && size.height < h){
				reSize = size;
				break;
			}
		}
		return reSize;
	}
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w / h;
		if (sizes == null) return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO 自動生成されたメソッド・スタブ
		try {
			mCamera = Camera.open(defaultCameraId);
			mCameraParameters = mCamera.getParameters();
			cameraPreviewStart();
		}
		catch(Exception e){
			Log.d(TAG,"Camera not open");
		}
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO 自動生成されたメソッド・スタブ

		mCamera.setOneShotPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
	}
	/* (非 Javadoc)
	 * @see android.hardware.Camera.PreviewCallback#onPreviewFrame(byte[], android.hardware.Camera)
	 */
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO 自動生成されたメソッド・スタブ
		try {
		final int MAXFACES = 3;
		int findFace = 0;
		FaceDetector.Face[] faces = new FaceDetector.Face[MAXFACES];
		Size s = camera.getParameters().getPreviewSize();
		mFaceDetector = new FaceDetector(s.width,s.height,MAXFACES);
		
//		Log.d(TAG, "onPreviewFrame size " + s.width +" " + s.height); 
		
		final int[] rgb = decodeYUV420SP(data, s.width, s.height);
		
		
		Bitmap bmp = Bitmap.createBitmap(rgb, s.width, s.height,Bitmap.Config.RGB_565);
		
		long pre = System.currentTimeMillis();
		
		findFace = mFaceDetector.findFaces(bmp,faces);
		
		
		long pre2 = System.currentTimeMillis();
		Log.d(TAG,"findFaces " + (pre2 - pre) + "find " + findFace);
		//Log.d(TAG,"find face " + findFace);

		Canvas canvas = mHolder.lockCanvas(); 
		canvas.drawColor(0x00000000);
		canvas.drawBitmap(rgb, 0, s.width, 0, 0, s.width, s.height, false, null);
		//canvas.drawBitmap(bmp, 0, 0, null);
		if(findFace > 0){
			Paint paint = new Paint();
		    paint.setColor(Color.argb(255, 255, 0, 0)); // 赤
		    paint.setStyle(Style.STROKE); // 塗りつぶしなしの線
			for (int i = 0; i < findFace; i++) { // 認識した数だけ処理
		        //Face face = faces[i];
		        PointF midPoint = new PointF(0, 0);
		        faces[i].getMidPoint(midPoint); // 顔認識結果を取得
		        float eyesDistance = faces[i].eyesDistance();
		        
		        // 描写先の設定
		        RectF rect = new RectF(); 
		        rect.left = midPoint.x - (eyesDistance*2) ;
		        rect.top = midPoint.y - (eyesDistance*2) ;
		        rect.right = midPoint.x + (eyesDistance*2) ;
		        rect.bottom = midPoint.y + (eyesDistance*2) ;

		        
		        canvas.drawBitmap(c00, c00_src, rect, paint); // 笑い男に
			 }
			// ここでは、結果をわかりやすくするために、元のビットマップを複製し、
			// 赤い四角を描画しています

//			Paint paint = new Paint();
//			paint.setColor(Color.argb(255, 255, 0, 0)); // 赤
//			paint.setStyle(Style.STROKE); // 塗りつぶしなしの線
//			for (int i = 0; i < findFace; i++) { // 認識した数だけ処理
//				FaceDetector.Face face = faces[i];
//				PointF midPoint = new PointF(0, 0);
//				face.getMidPoint(midPoint); // 顔認識結果を取得
//				float eyesDistance = face.eyesDistance(); // 顔認識結果を取得
//				RectF rect = new RectF(); // 正方形
//				rect.left = midPoint.x - eyesDistance / 2;
//				rect.top = midPoint.y - eyesDistance / 2;
//				rect.right = midPoint.x + eyesDistance / 2;
//				rect.bottom = midPoint.y + eyesDistance / 2;
//				canvas.drawRect(rect, paint); // 正方形を描画
//			}

		}
		mHolder.unlockCanvasAndPost(canvas);
		camera.setOneShotPreviewCallback(this);
		}
		catch(Exception e){
			Log.d(TAG,"onPreviewFrame " + e.getMessage());
			e.printStackTrace();
		}
	}
	// YUV420 to BMP  
	/*
	public int[] decodeYUV420SP( byte[] yuv420sp, int width, int height) {   

	    final int frameSize = width * height;   

	    int rgb[]=new int[width*height];   
	    for (int j = 0, yp = 0; j < height; j++) {   
	        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;   
	        for (int i = 0; i < width; i++, yp++) {   
	            int y = (0xff & ((int) yuv420sp[yp])) - 16;   
	            if (y < 0) y = 0;   
	            if ((i & 1) == 0) {   
	                v = (0xff & yuv420sp[uvp++]) - 128;   
	                u = (0xff & yuv420sp[uvp++]) - 128;   
	            }   

	            int y1192 = 1192 * y;   
	            int r = (y1192 + 1634 * v);   
	            int g = (y1192 - 833 * v - 400 * u);   
	            int b = (y1192 + 2066 * u);   

	            if (r < 0) r = 0; else if (r > 262143) r = 262143;   
	            if (g < 0) g = 0; else if (g > 262143) g = 262143;   
	            if (b < 0) b = 0; else if (b > 262143) b = 262143;   

	            rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &  0xff00) | ((b >> 10) & 0xff);   


	        }   
	    }   
	    return rgb;   
	    } 
	 */
	public native int[] decodeYUV420SP(byte[] data,int width,int height);
	public native int[] decodeYUV420TORGB565(byte[] data,int width,int height);
	static {
		System.loadLibrary("decodeYUV420SP_jni");
	}

}
