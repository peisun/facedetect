package jp.peisun.android.facedetect;

import java.util.List;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FacedetectActivity extends Activity {

	private CameraSurfaceView mCameraView = null;
	private FrameLayout preview= null;
	private final String TAG = "facedetect:activity";
    @Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO 自動生成されたメソッド・スタブ
		super.onWindowFocusChanged(hasFocus);
		//mSize.width = findViewById(R.id.linearLayout1).getWidth();
		//mSize.height = findViewById(R.id.linearLayout1).getHeight();
		
		
		
	}
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* フルスクリーンに　*/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /* タイトルバーを消す */
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        
        setContentView(R.layout.main);
        mCameraView = new CameraSurfaceView(this);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraView);

    }

	@Override
	protected void onPause() {
		// TODO 自動生成されたメソッド・スタブ
		super.onPause();
		
	}
	
}