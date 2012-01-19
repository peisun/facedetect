package jp.peisun.android.facedetect;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FacedetectActivity extends Activity {

	/* カメラSurface関連 */
	private CameraSurfaceView mCameraView = null;
	
	/* レイアウト */
	private FrameLayout preview= null;
	

	/* OverlayView */
	OverlayView mOverlayView = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* フルスクリーンモードにする */
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		/* タイトルを消す */
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);
		/* カメラのSurfaceを作る */
		mCameraView = new CameraSurfaceView(this);
		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mCameraView);

		mOverlayView = new OverlayView(this);
		preview.addView(mOverlayView, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mOverlayView.setZOrderOnTop(true);
		mCameraView.setOverlayView(mOverlayView);

	}

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