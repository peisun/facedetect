package jp.peisun.android.facedetect;


import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;



public class CameraSurfaceView extends SurfaceView 
implements SurfaceHolder.Callback ,Camera.PreviewCallback {
	/* カメラ関連 */
	private Camera mCamera = null;
	private int mDefaultCameraId = 0;
	private volatile boolean mPortrait = false;

	/* OverlayView */
	OverlayView mOverlayView = null;

	/* 自分自身 */
	Context mContext = null;
	SurfaceHolder mHolder = null;
	public CameraSurfaceView(Context context) {
		super(context);

		mContext = context;
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}
	public void setOverlayView(OverlayView view){
		mOverlayView = view;
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

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO 自動生成されたメソッド・スタブ
		Size s = camera.getParameters().getPreviewSize();

		mOverlayView.startFindFace(data,s.width,s.height,mPortrait);
		
		mCamera.setOneShotPreviewCallback(this);
	}



	// SurfaceViewが生成されたらカメラをオープンする
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
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
		mCamera.setOneShotPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera= null;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		Camera.Parameters parameters = mCamera.getParameters();

		List<Size> sizes = parameters.getSupportedPreviewSizes();
		Size optimalSize = getOptimalPreviewSize(sizes, width, height);
		parameters.setPreviewSize(optimalSize.width, optimalSize.height);

		mCamera.setParameters(parameters);

		mPortrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		if (mPortrait) {
			mCamera.setDisplayOrientation(90);  /* 縦向き */
		}
		else{
			mCamera.setDisplayOrientation(0); /* 横向き */
		}


		mOverlayView.createFaceDetector(width,height,mPortrait);
		mCamera.setOneShotPreviewCallback(this);
		mCamera.startPreview();
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



}
