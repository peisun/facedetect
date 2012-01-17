package jp.peisun.android.facedetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class OverlayView extends SurfaceView implements SurfaceHolder.Callback {
	private final String TAG = "OverlayView: ";

	private SurfaceHolder mHolder = null;
	
	private Bitmap face_bitmap = null;
	private Rect face_src = null;
	private Paint mPaint = null;

	public Handler postHandler = null;

	public OverlayView(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		setDrawingCacheEnabled(true);            
		// リソースから「笑い男」のビットマップを作る
		face_bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.c00);
		// 
        face_src = new Rect(); 
        face_src.left = 0 ;
        face_src.top = 0 ;
        face_src.right = face_bitmap.getWidth() ;
        face_src.bottom = face_bitmap.getHeight() ;
        
        mPaint = new Paint();
        mPaint.setColor(Color.argb(255, 255, 0, 0)); 
        mPaint.setStyle(Style.STROKE);
        setFocusable(true);
        
    	postHandler = new Handler(){
    		public void handleMessage(Message msg) {
                //メッセージの表示
    			FaceDetector.Face[] faces = (FaceDetector.Face[]) msg.obj;
                faceDraw(faces);
            };
    	};
	}

	private void faceDraw(FaceDetector.Face[] faces) {		
		Canvas canvas = mHolder.lockCanvas();
		if(canvas != null){
			RectF face_dst = new RectF();  /* 顔の表示先領域 */
			for (int i =0 ; i<faces.length && faces[i] != null ;i++) { 
				//Face face = faces[i];
				PointF midPoint = new PointF(0, 0);
				faces[i].getMidPoint(midPoint); 
				float eyesDistance = faces[i].eyesDistance();

				/* 縦横それぞれ1/2の画像で認識させているので、大きさを2倍にしてあげる */
				face_dst.left   = (midPoint.x - (eyesDistance*2))*2 ;
				face_dst.top    = (midPoint.y - (eyesDistance*2))*2 ;
				face_dst.right  = (midPoint.x + (eyesDistance*2))*2 ;
				face_dst.bottom = (midPoint.y + (eyesDistance*2))*2 ;

				canvas.drawBitmap(face_bitmap, face_src, face_dst, mPaint);
				
			}
			mHolder.unlockCanvasAndPost(canvas);
		}		
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "Changed");
		Log.d(TAG, "width:" + width + "height:" + height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "Created");
		holder.setFormat(PixelFormat.TRANSLUCENT);
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "Destroyed");
	}	
}
