package jp.peisun.android.facedetect;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class FacedetectActivity extends Activity {
	private final String TAG = "MainActivity:";

	private OverlayView overlayview;
	
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
        overlayview = new OverlayView(getApplicationContext());
        addContentView(overlayview, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        overlayview.setZOrderOnTop(true);
        cameraSurfaceView.setOverlayView(overlayview);
        Log.i(TAG, "Activity Created");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, 0, 0, "The Laughing Man");
    	menu.add(Menu.NONE, 1, 1, "Blind Bar");
    	menu.add(Menu.NONE, 2, 2, "Rectangle");
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case 0:
    		overlayview.setMode(OverlayView.MODE_FACE);
    		break;
    	case 1:
    		overlayview.setMode(OverlayView.MODE_BAR);
    		break;
    	case 2:
    		overlayview.setMode(OverlayView.MODE_RECT);
    		break;
    	default:
    		return super.onOptionsItemSelected(item);	
    	}
    	return true;
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