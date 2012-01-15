package jp.peisun.android.facedetect;

import android.content.Context;
import android.content.res.Configuration;
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
import android.graphics.PorterDuff;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class OverlayView extends SurfaceView implements SurfaceHolder.Callback,Runnable {
	private final String TAG = "OverlayView";

	private Context mContext = null;
	private SurfaceHolder mHolder = null;
	
	private Bitmap c00 = null;
	private Rect c00_src = null;
	private Paint mPaint = null;
	private int mPreviewWidth = 0;
	private int mPreviewHeight = 0;
	final int MAXFACES = 3;
	int findFace = 0;
	
	RectF rect = new RectF();  /* マークの表示先座標 */
	//private FaceDetector.Face[] faces = new FaceDetector.Face[MAXFACES];
	private FaceDetector mFaceDetector = null;
	private Bitmap bmp;
	private boolean mOrient = false;
	
	private volatile long findFaceTime = 500;
	protected final Object lock = new Object();
	public OverlayView(Context context) {
		super(context);
		// TODO 自動生成されたコンストラクター・スタブ
		mContext = context;
		mHolder = getHolder();
		mHolder.addCallback(this);
		setDrawingCacheEnabled(true);            
		// リソースから「笑い男」のビットマップを作る
		c00 = BitmapFactory.decodeResource(this.getResources(), R.drawable.c00);
		// 
        c00_src = new Rect(); 
        c00_src.left = 0 ;
        c00_src.top = 0 ;
        c00_src.right = c00.getWidth() ;
        c00_src.bottom = c00.getHeight() ;
        
        mPaint = new Paint();
        mPaint.setColor(Color.argb(255, 255, 0, 0)); 
        mPaint.setStyle(Style.STROKE);
        setFocusable(true);
        
        
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO 自動生成されたメソッド・スタブ
		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO 自動生成されたメソッド・スタブ
		//mHolder = holder;
		mHolder.setFormat(PixelFormat.TRANSLUCENT);
		//mHolder.addCallback(this);
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO 自動生成されたメソッド・スタブ
		
	}
	

	protected int getPortrait() {

		boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		if (portrait) {
			return 90;
		}
		else{
			return 0;
		}
	}
	public void doDraw(FaceDetector.Face[] faces) {
		// TODO 自動生成されたメソッド・スタブ
		
		Canvas canvas = mHolder.lockCanvas();
		if(canvas != null){
		//canvas.drawColor(Color.TRANSPARENT);
		//
		canvas.drawColor(0,PorterDuff.Mode.CLEAR ); 
		//canvas.rotate(90);
		//canvas.drawBitmap(c00, 0, 0, null);
		
		// 縮小したBitmapの表示
//		synchronized(bmp){
//		canvas.drawBitmap(bmp, 0, 0, mPaint);
//		}
		for (int i =0 ; i<faces.length && faces[i] != null ;i++) { 
			//Face face = faces[i];
			PointF midPoint = new PointF(0, 0);
			faces[i].getMidPoint(midPoint); 
			float eyesDistance = faces[i].eyesDistance();


			/* 縦横それぞれ1/2の画像で認識させているので、大きさを2倍にしてあげる */
			rect.left   = (midPoint.x - (eyesDistance*2))*2 ;
			rect.top    = (midPoint.y - (eyesDistance*2))*2 ;
			rect.right  = (midPoint.x + (eyesDistance*2))*2 ;
			rect.bottom = (midPoint.y + (eyesDistance*2))*2 ;


			canvas.drawBitmap(c00, c00_src, rect, mPaint);
			
		}
		
		mHolder.unlockCanvasAndPost(canvas);
		}
		
	}
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
            //メッセージの表示
			FaceDetector.Face[] faces = (FaceDetector.Face[]) msg.obj;
            doDraw(faces);
            
        };
	};

	public long getFindFaceTime(){
		return findFaceTime;
	}
	public void surfaceChanged(int width,int height,boolean orient){
		mPreviewWidth = width;
		mPreviewHeight = height;
		/* mFaceDetectorがnull、つまり最初の仕事 */
		if(mFaceDetector == null){
			mFaceDetector = new FaceDetector(width/2,height/2,MAXFACES);
		}
		/* 回転してたら作り直し */
		if(orient != mOrient){
			mOrient = orient;
			mFaceDetector = null;
			mFaceDetector = new FaceDetector(width/2,height/2,MAXFACES);
			if(bmp != null){
			synchronized(bmp){
				bmp = null; /* Bitmapも作り直し */
			}
			}
		}
		
	}
	public void createBitmapDraw(Bitmap bmp){
		if(bmp != null){
		Canvas canvas = mHolder.lockCanvas();
		//canvas.drawColor(Color.TRANSPARENT);
		
		//canvas.drawBitmap(c00, 0, 0, null);
		canvas.drawBitmap(bmp, 0, 0, mPaint);
		mHolder.unlockCanvasAndPost(canvas);
		}

	}
	public void startFindFace(byte[] data,int width,int height,boolean orient){
		int rgb[] = null;
		long pre0,pre1 = 0;
		if(data.length == 0) return ;

		if(bmp == null){
			pre0 = System.currentTimeMillis();
			if(orient == true){
				rgb = DecodeYUV.decodeYUV420SP(data, width, height,DecodeYUV.SCALE_DOWN_ROTATE);
				pre1 = System.currentTimeMillis();
				bmp = Bitmap.createBitmap(rgb,  height/2,width/2,Bitmap.Config.RGB_565);
			}
			else {
				rgb = DecodeYUV.decodeYUV420SP(data, width, height,DecodeYUV.SCALE_DOWN);
				pre1 = System.currentTimeMillis();
				bmp = Bitmap.createBitmap(rgb, width/2, height/2,Bitmap.Config.RGB_565);
			}
			long pre2 = System.currentTimeMillis();
			Log.d(TAG,"time " + (pre1-pre0) + ":"+ (pre2-pre1));
//			createBitmapDraw(bmp);
			new Thread(this).start();
			return ;
		}
		synchronized(bmp){
		
			long pre3 = System.currentTimeMillis();
			
			if(orient == true){
				DecodeYUV.createBitmapYUVtoRGB565(data,width,height,bmp,DecodeYUV.SCALE_DOWN_ROTATE);
			}
			else {
			//createBitmapYUVtoRGB565(data,bmp);
				DecodeYUV.createBitmapYUVtoRGB565(data,width,height,bmp,DecodeYUV.SCALE_DOWN);
			}
			long pre4 = System.currentTimeMillis();
			Log.d(TAG,"createBitmap time " + (pre4-pre3));
//			createBitmapDraw(bmp);
		}
		new Thread(this).start();
		return ;
	}
	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ
		Log.d(TAG,"FindFaceDetector start");
		if(bmp != null){
			synchronized(bmp){
				if(mFaceDetector == null) return;
				FaceDetector.Face[] faces = new FaceDetector.Face[MAXFACES];
				long pre5 = System.currentTimeMillis();
				int findFace = mFaceDetector.findFaces(bmp,faces);
				long pre6 = System.currentTimeMillis();
				Log.d(TAG,"findFaces time " + (pre6-pre5));
				findFaceTime = (findFaceTime + (pre6-pre5))/2;
				
					Message msg = Message.obtain();
					msg.obj = faces;
					mHandler.sendMessage(msg);
				
				Log.d(TAG,"FindFaceDetector end");
				//		invalidate();
			}
		}
		return;
		
	}
	
	
	
}
