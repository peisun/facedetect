package jp.peisun.android.facedetect;

import android.media.FaceDetector;

public class DetectResult {

		private FaceDetector.Face [] faces;
		private int detectWidth;
		private int detectHeight;
		
		public DetectResult(FaceDetector.Face[] f, int w, int h) {
			faces = f;
			detectWidth = w;
			detectHeight = h;
		}
		
		FaceDetector.Face[] getFaces() {
			return faces;
		}
		
		int getWidth() {
			return detectWidth;
		}
		
		int getHeight() {
			return detectHeight;
		}
}
