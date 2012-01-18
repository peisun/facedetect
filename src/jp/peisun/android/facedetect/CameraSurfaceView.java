package jp.peisun.android.facedetect;

import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback ,Camera.PreviewCallback {
	private final String TAG = "CameraSurface: ";

	private SurfaceHolder mHolder = null;
	/* カメラ関連 */
	private Camera mCamera = null;
	private int mDefaultCameraId = 0;

	private Size mPreviewSize = null;
	private boolean isPortrait = false;
	
	private final int MAXDETECTOR = 1;
	private final int MAXFACES = 3;
	private FaceDetector[] mFaceDetector = new FaceDetector[MAXDETECTOR];
	
	private Thread[] detectThread = new Thread[MAXDETECTOR];
	private int DetectorNo = 0;
	private int[][] rgb = new int[MAXDETECTOR][];
	private Bitmap[] bmp = new Bitmap[MAXDETECTOR];
	private FaceDetector.Face[][] faces = new FaceDetector.Face[MAXDETECTOR][];

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
		Log.d(TAG, "Created");
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
		Log.d(TAG, "Destroyed");
		mCamera.setOneShotPreviewCallback(null);
    	mCamera.stopPreview();
    	mCamera.release();
    	mCamera= null;
    }
	
	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "Changed");
		Log.d(TAG, "width:" + width + " height:" + height);

		Camera.Parameters parameters = mCamera.getParameters();

        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        mPreviewSize = getOptimalPreviewSize(supportedSizes, width, height);
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        
        mCamera.setParameters(parameters);
        
        isPortrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		if (isPortrait) {
			mCamera.setDisplayOrientation(90);  /* 縦向き */
		}
		else{
			mCamera.setDisplayOrientation(0); /* 横向き */
		}
		for(int i = 0; i < MAXDETECTOR; i++) {
			mFaceDetector[i] = new FaceDetector(mPreviewSize.width, mPreviewSize.height, MAXFACES);
			detectThread[i] = null;
			rgb[i] = new int[mPreviewSize.width * mPreviewSize.height];
			bmp[i] = Bitmap.createBitmap(mPreviewSize.width, mPreviewSize.height, Bitmap.Config.RGB_565);
			faces[i] = new FaceDetector.Face[MAXFACES];
		}
        mCamera.setOneShotPreviewCallback(this);
    	mCamera.startPreview();
    	Log.d(TAG, "Preview Started Width:" + mPreviewSize.width + "Height:" + mPreviewSize.height);
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
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mOverlayView != null) {
			Thread dthread = detectThread[DetectorNo];
			if (dthread != null) {
				if (dthread.isAlive()) {
					try {
						dthread.join();
					} catch (InterruptedException e) {
						Log.d(TAG, "DetectThread[" + DetectorNo + "] Join failed");
						e.printStackTrace();
					}
				}
			}
			if (isPortrait) {
				rgb[DetectorNo] = DecodeYUV.decodeYUV420SP(data, mPreviewSize.width, mPreviewSize.height, DecodeYUV.SCALE_DOWN_ROTATE);
				DecodeYUV.createBitmapYUVtoRGB565(data, mPreviewSize.width, mPreviewSize.height, bmp[DetectorNo], DecodeYUV.SCALE_DOWN_ROTATE);
			} else {
				rgb[DetectorNo] = DecodeYUV.decodeYUV420SP(data, mPreviewSize.width, mPreviewSize.height, DecodeYUV.SCALE_DOWN);
				DecodeYUV.createBitmapYUVtoRGB565(data, mPreviewSize.width, mPreviewSize.height, bmp[DetectorNo], DecodeYUV.SCALE_DOWN);
			}
			
			dthread = new faceDetectThread(mFaceDetector[DetectorNo], faces[DetectorNo], bmp[DetectorNo]);
			dthread.start();
			detectThread[DetectorNo++] = dthread;
			
			if (DetectorNo >= MAXDETECTOR) {
				DetectorNo = 0;
			}
		}
		mCamera.setOneShotPreviewCallback(this);
	}

	class faceDetectThread extends Thread implements Runnable {
		private final String TAG = "Facedetect: ";
		private Bitmap mBitmap;
		private FaceDetector mFacedetector;
		private FaceDetector.Face[] mFaces;
		
		public faceDetectThread(FaceDetector facedetector, FaceDetector.Face[] faces, Bitmap bitmap) {
			mFacedetector = facedetector;
			mFaces = faces;
			mBitmap = bitmap;
		}

		@Override
		public void run() {
			Log.d(TAG, "Start FaceDetect");
			int faceCount = mFacedetector.findFaces(mBitmap, mFaces);
			Log.d(TAG, "Finished FaceDetect");
			Log.d(TAG, "FaceCount:" + faceCount);
			if (faceCount > 0) {
				Message msg = Message.obtain();
				msg.obj = mFaces;
				mOverlayView.postHandler.sendMessage(msg);
			}
			return;
		}
	}
}
