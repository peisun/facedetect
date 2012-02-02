package jp.peisun.android.facedetect;

import android.media.FaceDetector;

public class DetectResult {

		private FaceDetector.Face [] faces;
		private int detectWidth;
		private int detectHeight;
		private int rotate;
		
		public DetectResult(FaceDetector.Face[] faces, int width, int height) {
			this.faces = faces;
			this.detectWidth = width;
			this.detectHeight = height;
			this.rotate = 0;
		}
		
		public FaceDetector.Face[] getFaces() {
			return faces;
		}
		
		public int getWidth() {
			return detectWidth;
		}
		
		public int getHeight() {
			return detectHeight;
		}
		
		public void setRotate(int rotate) {
			this.rotate = rotate;
		}
		
		public int getRotate() {
			return this.rotate;
		}
}
