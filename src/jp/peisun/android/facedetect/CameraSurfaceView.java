package jp.peisun.android.facedetect;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

	private SensorManager mSensorManager;
	private Sensor mGSensor;
	private int mRotate;

	/* FaceDetectorの定数 */
	private final int MAXDETECTOR = 10;
	private final int MAXFACES = 3;
	/* 顔認識のリソース配列 */
	private int DetectorNo = 0;
	private Thread[] detectThread = new Thread[MAXDETECTOR];
	private FaceDetector[] mFaceDetector = new FaceDetector[MAXDETECTOR];
	private FaceDetector[] mFaceDetector_portrait = new FaceDetector[MAXDETECTOR];
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
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mSensorListener, mGSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
		
	private final SensorEventListener mSensorListener = new SensorEventListener() {
		private float[] gSensor = new float[3];
		private final int X = 0;
		private final int Y = 1;
		private final int Z = 2;
		private double degree;
		
		@Override
		public void onSensorChanged(SensorEvent event) {			
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				gSensor = event.values.clone();
				float x = gSensor[X];
				float y = gSensor[Y];
				float z = gSensor[Z];
				
				if (-9.0f < z && z < 9.0f) {
					degree = Math.toDegrees(Math.atan2(y, x));
				} else {
					degree = 0.0f;
				}
				
				if (-45.0f < degree && degree < 45.0f) {
					mRotate = 0;
				} else if (45.0f < degree && degree < 135.0f) {
					mRotate = -90;
				} else if ( 135.0f < degree && degree < 180.0f) {
					mRotate = -180;
				} else if ( -135.0f < degree && degree < -45.0f) {
					mRotate = 90;
				} else if (-180.0f < degree && degree < -135.0f) {
					mRotate = 180;
				}
			}
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO 自動生成されたメソッド・スタブ
			
		}
	};

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
			mFaceDetector_portrait[i] = new FaceDetector(h, w, MAXFACES);
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
			final Bitmap bitmap = bmp[DetectorNo];

			Log.d(TAG, "bitmap Width:" + w + "/Height:" + h);
			Log.d(TAG, "Start YUVtoRGB convert");
			decodeYUV.createBitmap(yuvdata, w, h, bitmap, DecodeYUV.SCALE_DOWN);
			Log.d(TAG, "Finished YUVtoRGB convert");
			
			Log.d(TAG, "Thread No" + DetectorNo);
			FaceDetector facedetector;
			if (Math.abs(mRotate) == 90)
				facedetector = mFaceDetector_portrait[DetectorNo];
			else
				facedetector = mFaceDetector[DetectorNo];
				
			final DetectResult detectresult = detectResult[DetectorNo];
			detectresult.setRotate(mRotate);
			dthread = new faceDetectThread(DetectorNo, facedetector, bitmap, detectresult);
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
		private int mId;
		private FaceDetector mFacedetector;
		private Bitmap mBitmap;
		private DetectResult mResult;
		
		public faceDetectThread(int id, FaceDetector facedetector, Bitmap bitmap, DetectResult result) {
			mId = id;
			mFacedetector = facedetector;
			mBitmap = bitmap;
			mResult = result;
		}

		@Override
		public void run() {
			if (mFacedetectEnable) {
				Log.i(TAG, "Start FaceDetect:" + mId);
				Matrix matrix = new Matrix();
				matrix.setRotate(mResult.getRotate());
				Bitmap bitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
				mResult.setWidth(bitmap.getWidth());
				mResult.setHeight(bitmap.getHeight());
				FaceDetector.Face [] faces = mResult.getFaces();
				int faceCount = mFacedetector.findFaces(bitmap, faces);
				bitmap.recycle();
				Log.i(TAG, "Finished FaceDetect:" + mId + " :FaceCount:" + faceCount);
				if (faceCount > 0) {
					for (int i = faceCount; i < MAXFACES; i++) {
						faces[i] = null;
					}
				}
				mOverlayView.setDetectResult(faces, mResult);
			}
			return;
		}
	}
}
