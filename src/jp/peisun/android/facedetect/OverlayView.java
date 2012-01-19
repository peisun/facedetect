package jp.peisun.android.facedetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class OverlayView extends SurfaceView implements SurfaceHolder.Callback {
	private final String TAG = "OverlayView:";

	private SurfaceHolder mHolder = null;
	
	private Bitmap face_bitmap = null;
	private Rect face_src = null;
	private Paint mPaint = null;
	
	private int mWidth;
	private int mHeight;

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
	}

	public void faceDraw(FaceDetector.Face[] faces, int width, int height) {
		Log.i(TAG, "Drawing Faces");
		Log.d(TAG, "detectWidth:" + width + "/detectHeight:" + height);
		Canvas canvas = mHolder.lockCanvas();
		if(canvas != null){
			RectF face_dst = new RectF();  /* 顔の表示先領域 */
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
			float widthX = (float)mWidth / (float)width;
			float heightX = (float)mHeight / (float)height;
			for (int i =0 ; i < faces.length && faces[i] != null ;i++) { 
				PointF point = new PointF();
				faces[i].getMidPoint(point);
				float eyesDistance = faces[i].eyesDistance() * 1.5f;//顔全体の幅は目の間の距離の1.5倍程度
				float centerX = width - point.x;
				float centerY = point.y;
				Log.d(TAG, "centerX:" + centerX + " centerY:" + centerY + " eyesDistance:" + eyesDistance);
				Log.d(TAG, "widthX:" + widthX + " heightX" + heightX);

				face_dst.left   = (centerX - eyesDistance) * widthX;
				face_dst.top    = (centerY - eyesDistance) * heightX;
				face_dst.right  = (centerX + eyesDistance) * widthX;
				face_dst.bottom = (centerY + eyesDistance) * heightX;

				canvas.drawBitmap(face_bitmap, face_src, face_dst, mPaint);
				
			}
			mHolder.unlockCanvasAndPost(canvas);
		}		
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "Changed");
		Log.d(TAG, "SurfaceSize Width:" + width + "/Height:" + height);
		mWidth = width;
		mHeight = height;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "Created");
		holder.setFormat(PixelFormat.TRANSLUCENT);
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "Destroyed");
	}	
}
