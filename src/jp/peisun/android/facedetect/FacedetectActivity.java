package jp.peisun.android.facedetect;

import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FacedetectActivity extends Activity {

	/* カメラSurface関連 */
	private SurfaceView mCameraView = null;
	private FrameLayout preview= null;
	private SurfaceHolder mCameraSurfaceHolder = null;
	
	/* カメラ関連 */
	Camera mCamera = null;
	int mDefaultCameraId = 0;
	
	/* OverlayView */
	OverlayView mOverlayView = null;
	private final String TAG = "facedetect:activity";
    @Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO �����������ꂽ���\�b�h�E�X�^�u
		super.onWindowFocusChanged(hasFocus);
		//mSize.width = findViewById(R.id.linearLayout1).getWidth();
		//mSize.height = findViewById(R.id.linearLayout1).getHeight();
		
		
		
	}
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* フルスクリーンモードにする */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /* タイトルを消す */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
      /* カメラのSurfaceを作る */
        //mCameraView = new CameraSurfaceView(this);
        mCameraView = new SurfaceView(this);
        mCameraSurfaceHolder = mCameraView.getHolder();
        // SurfaceViewにリスナーを登録
        mCameraSurfaceHolder.addCallback(surfaceListener);
        mCameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //mCameraSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        setContentView(mCameraView);
        
        mOverlayView = new OverlayView(this);
        addContentView(mOverlayView, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        mOverlayView.setZOrderOnTop(true);
        
        
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
    private Camera.PreviewCallback previewListener = new Camera.PreviewCallback(){

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			// TODO 自動生成されたメソッド・スタブ
			Size s = camera.getParameters().getPreviewSize();
			boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    		
			mOverlayView.startFindFace(data,s.width,s.height,portrait);
			long sleepTtime = mOverlayView.getFindFaceTime();
			try{
			Thread.sleep(sleepTtime);
			}
			catch(Exception e){
				
			}
			mCamera.setOneShotPreviewCallback(previewListener);
		}
    	
    };
    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
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
            
            boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    		if (portrait) {
    			mCamera.setDisplayOrientation(90);  /* 縦向き */
    		}
    		else{
    			mCamera.setDisplayOrientation(0); /* 横向き */
    		}
             
            
            //mOverlayView.setPreviewSize(optimalSize.width,optimalSize.height);
            mOverlayView.surfaceChanged(width,height,portrait);
            mCamera.setOneShotPreviewCallback(previewListener);
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
    };
	@Override
	protected void onPause() {
		super.onPause();
		
	}

	@Override
	protected void onResume() {
		// TODO 自動生成されたメソッド・スタブ
		super.onResume();
		
		
	}
	
}