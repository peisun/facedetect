package jp.peisun.android.facedetect;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback ,Camera.PreviewCallback {
	private final String TAG = "CameraSurface:";

	private SurfaceHolder mHolder = null;
	/* カメラ関連 */
	private Camera mCamera = null;
	private int mDefaultCameraId = 0;

	private Size mCaptureSize = null;

	/* FaceDetectorの定数 */
	private final int MAXDETECTOR = 1;
	private final int MAXFACES = 3;
	/* 顔認識のリソース配列 */
	private int DetectorNo = 0;
	private Thread[] detectThread = new Thread[MAXDETECTOR];
	private FaceDetector[] mFaceDetector = new FaceDetector[MAXDETECTOR];
	private Bitmap[] bmp = new Bitmap[MAXDETECTOR];
	private DetectResult [] detectResult = new DetectResult[MAXDETECTOR];

	private volatile boolean mFacedetectEnable = false;
	
	private DecodeYUV decodeYUV = new DecodeYUV();
	
	private OverlayView mOverlayView;
	public void setOverlayView(OverlayView view) {
		mOverlayView = view;
	}
	
	public CameraSurfaceView(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mOverlayView = null;
	}

	// SurfaceViewが生成されたらカメラをオープンする
	@Override
    public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "Created");
		/* フロントカメラを探す */
		mDefaultCameraId = getCameraFacing();
    	mCamera = Camera.open(mDefaultCameraId);
        try {
        	mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
        	mCamera.release();
            mCamera = null;
            e.printStackTrace();
        }
    }
    // SurfaceViewが破棄されるタイミングでカメラを開放する
	@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	mFacedetectEnable = false;
		mCamera.setOneShotPreviewCallback(null);
    	mCamera.stopPreview();
    	mCamera.release();
    	mCamera= null;
    	for (int i = 0; i < MAXDETECTOR; i++) {
    		Thread thread = detectThread[i];
    		if (thread.isAlive()) {
	    		try {
					thread.join();
				} catch (InterruptedException e) {
					Log.e(TAG, "DetectThread[" + i + "] Join failed");
					e.printStackTrace();
				}    				
    		}
    	}
		Log.i(TAG, "Destroyed");
    }
	
	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "Changed");
		Log.d(TAG, "SurfaceSize Width:" + width + "/Height:" + height);

		Camera.Parameters parameters = mCamera.getParameters();

        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        mCaptureSize = getOptimalPreviewSize(supportedSizes, width, height);
        parameters.setPreviewSize(mCaptureSize.width, mCaptureSize.height);
        mCamera.setParameters(parameters);
        
        mCamera.setDisplayOrientation(0); /* 横向き */
        
		final int w = mCaptureSize.width / 2;
		final int h = mCaptureSize.height / 2;
		
		for(int i = 0; i < MAXDETECTOR; i++) {
			mFaceDetector[i] = new FaceDetector(w, h, MAXFACES);
			bmp[i] = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
			detectResult[i] = new DetectResult(new FaceDetector.Face[MAXFACES], w, h);
		}
        mCamera.setOneShotPreviewCallback(this);
    	mCamera.startPreview();
    	mFacedetectEnable = true;
    	Log.d(TAG, "Preview Started Width:" + w + "/Height:" + h);
    }

	private int getCameraFacing(){
    	int cameraId = 0;
    	// カメラの個数を取得
		int numberOfCameras = Camera.getNumberOfCameras();
		// フロントカメラがあれば、それをデフォルトとする
		CameraInfo cameraInfo = new CameraInfo();	
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
			}
		}
		return cameraId;
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
	public void onPreviewFrame(byte[] yuvdata, Camera camera) {
		if (mFacedetectEnable == false) return; //if Enable is false, then Do nothing;
		if (mOverlayView != null) {
			Thread dthread = detectThread[DetectorNo];
			if (dthread != null) {
				if (dthread.isAlive()) {
					try {
						dthread.join();
					} catch (InterruptedException e) {
						Log.e(TAG, "DetectThread[" + DetectorNo + "] Join failed");
						e.printStackTrace();
					}
				}
			}
			
			final int w = mCaptureSize.width;
			final int h = mCaptureSize.height;
			final FaceDetector facedetector = mFaceDetector[DetectorNo];
			final Bitmap bitmap = bmp[DetectorNo];
			final DetectResult detectresult = detectResult[DetectorNo];

			Log.d(TAG, "bitmap Width:" + w + "/Height:" + h);
			Log.d(TAG, "Start YUVtoRGB convert");
			decodeYUV.createBitmap(yuvdata, w, h, bitmap, DecodeYUV.SCALE_DOWN);
			Log.d(TAG, "Finished YUVtoRGB convert");
			Log.d(TAG, "Thread No" + DetectorNo);
			dthread = new faceDetectThread(facedetector, bitmap, detectresult);
			detectThread[DetectorNo++] = dthread;
			dthread.start();
			
			if (DetectorNo >= MAXDETECTOR) {
				DetectorNo = 0;
			}
		}
		mCamera.setOneShotPreviewCallback(this);
	}

	class faceDetectThread extends Thread implements Runnable {
		private final String TAG = "Facedetect:";
		private FaceDetector mFacedetector;
		private Bitmap mBitmap;
		private DetectResult mResult;
		
		public faceDetectThread(FaceDetector facedetector, Bitmap bitmap, DetectResult result) {
			mFacedetector = facedetector;
			mBitmap = bitmap;
			mResult = result;
		}

		@Override
		public void run() {
			if (mFacedetectEnable) {
				Log.i(TAG, "Start FaceDetect");
				FaceDetector.Face [] faces = mResult.getFaces();
				int faceCount = mFacedetector.findFaces(mBitmap, faces);
				Log.i(TAG, "Finished FaceDetect");
				Log.d(TAG, "FaceCount:" + faceCount);
				if (faceCount > 0) {
					for (int i = faceCount; i < MAXFACES; i++) {
						faces[i] = null;
					}
				}
				mOverlayView.faceDraw(faces, mResult.getWidth(), mResult.getHeight());
			}
			return;
		}
	}
}
