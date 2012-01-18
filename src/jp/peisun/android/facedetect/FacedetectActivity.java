package jp.peisun.android.facedetect;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

public class FacedetectActivity extends Activity {
	private final String TAG = "MainActivity:";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        /* フルスクリーンモードにする */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /* タイトルを消す */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        CameraSurfaceView cameraSurfaceView = new CameraSurfaceView(getApplicationContext());
        setContentView(cameraSurfaceView);
        OverlayView overlayview = new OverlayView(getApplicationContext());
        addContentView(overlayview, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        overlayview.setZOrderOnTop(true);
        cameraSurfaceView.setOverlayView(overlayview);
        Log.i(TAG, "Activity Created");        
    }

	@Override
	protected void onPause() {
		super.onPause();
        Log.i(TAG, "Activity onPause");		
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "Activity onResume");		
	}
	
}