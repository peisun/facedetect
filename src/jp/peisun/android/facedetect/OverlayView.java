package jp.peisun.android.facedetect;

import java.util.Timer;
import java.util.TimerTask;

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
	private Paint mPaintRed = null;
	private Paint mPaintBlack = null;
	
	private int mWidth;
	private int mHeight;
	
	private Timer mTimer = new Timer(true);

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
        
        mPaintRed = new Paint();
        mPaintRed.setColor(Color.RED);
        mPaintRed.setStyle(Style.STROKE);
        mPaintRed.setStrokeWidth(1.5f);

        mPaintBlack = new Paint();
        mPaintBlack.setColor(Color.BLACK);
        mPaintBlack.setStyle(Style.FILL);
        
        setFocusable(true);        
	}

	public static final int MODE_FACE = 0;
	public static final int MODE_BAR = 1;
	public static final int MODE_RECT = 2;
	
	private int mFaceMode = MODE_RECT;
	
	public void setMode(int mode) {
		mFaceMode = mode;
	}
	
	private int mResultWidth;
	private int mResultHeight;
	private int mResultRotate;
	private FaceDetector.Face[] mResultFaces;
	
	public void setDetectResult(FaceDetector.Face[] faces, DetectResult result) {
		mResultFaces = faces.clone();
		mResultWidth = result.getWidth();
		mResultHeight = result.getHeight();
		mResultRotate = result.getRotate();
	}

	private void faceDraw() {
		Log.i(TAG, "Drawing Faces");
		Log.d(TAG, "detectWidth:" + mResultWidth + "/detectHeight:" + mResultHeight);
		Canvas canvas = mHolder.lockCanvas();
		if(canvas != null){
			RectF face_dst = new RectF();  /* 顔の表示先領域 */

			canvas.drawColor(0, PorterDuff.Mode.CLEAR); /* 透明色の塗り潰し */
			canvas.rotate(mResultRotate); /* 本体の向きに合わせてCanvasを回転 */
			
			/* 検出座標とCanvas座標の変換用倍率設定 */
			float widthX = (float)mWidth / (float)mResultWidth;
			float heightX = (float)mHeight / (float)mResultHeight;
			/* 回転させたCanvasの原点位置を変更する（画面左上に原点を移動） */
			/* Canvasの回転によって変換用倍率設定を変更 */
			switch (mResultRotate) {
			case -90:
				canvas.translate(-mHeight, 0); 
				widthX = (float)mHeight / (float)mResultWidth;
				heightX = (float)mWidth / (float)mResultHeight;
				break;
			case 90:
				canvas.translate(0, -mWidth);
				widthX = (float)mHeight / (float)mResultWidth;
				heightX = (float)mWidth / (float)mResultHeight;
				break;
			case 180:
			case -180:
				canvas.translate(-mWidth, -mHeight);
				break;
			default:
				break;
			}

			for (int i =0 ; i < mResultFaces.length && mResultFaces[i] != null ;i++) { 
				PointF point = new PointF();
				mResultFaces[i].getMidPoint(point);
				float eyesDistance = mResultFaces[i].eyesDistance() * 1.5f;//顔中心から輪郭までの幅は目の間の距離の1.5倍程度
				float centerX = mResultWidth - point.x;//何故か？左右が逆（認識結果のX軸原点と描画領域のX軸原点）
				float centerY = point.y;
				Log.d(TAG, "centerX:" + centerX + " centerY:" + centerY + " eyesDistance:" + eyesDistance);
				Log.d(TAG, "widthX:" + widthX + " heightX" + heightX);

				if (mFaceMode == MODE_FACE) {
					face_dst.left   = (centerX - eyesDistance) * widthX;
					face_dst.top    = (centerY - eyesDistance) * heightX;
					face_dst.right  = (centerX + eyesDistance) * widthX;
					face_dst.bottom = (centerY + eyesDistance) * heightX;

					canvas.drawBitmap(face_bitmap, face_src, face_dst, null);
				} else if (mFaceMode == MODE_BAR) {
					face_dst.left   = (centerX - eyesDistance) * widthX;
					face_dst.top    = centerY * heightX - eyesDistance / 2.0f;
					face_dst.right  = (centerX + eyesDistance) * widthX;
					face_dst.bottom = centerY * heightX + eyesDistance / 2.0f;

					canvas.drawRect(face_dst, mPaintBlack);				
				} else if (mFaceMode == MODE_RECT) {
					face_dst.left   = (centerX - eyesDistance) * widthX;
					face_dst.top    = (centerY - eyesDistance) * heightX;
					face_dst.right  = (centerX + eyesDistance) * widthX;
					face_dst.bottom = (centerY + eyesDistance) * heightX;

					canvas.drawRect(face_dst, mPaintRed);						
				}
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
		TimerTask DrawPeriod = new TimerTask() {			
			@Override
			public void run() {
				faceDraw();
			}
		};
		mTimer.scheduleAtFixedRate(DrawPeriod, 1000, 15);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "Created");
		holder.setFormat(PixelFormat.TRANSLUCENT);
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mTimer != null) {
			mTimer.cancel();
		}
		Log.i(TAG, "Destroyed");
	}	
}
