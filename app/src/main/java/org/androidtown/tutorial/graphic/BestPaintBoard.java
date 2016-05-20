package org.androidtown.tutorial.graphic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.OutputStream;
import java.util.Stack;

/**
 * 페인트보드에 기능 추가
 * Path를 사용하는 방식으로 수정
 *
 * @author Mike
 *
 */
public class BestPaintBoard extends SurfaceView implements SurfaceHolder.Callback {
	// SurfaceHolder.Callback 인터페이스를 implements 받도록 변경했습니다.
	/**
	 * Undo data
	 */
	Stack undos = new Stack();

	/**
	 * Maximum Undos
	 */
	public static int maxUndos = 10;

	/**
	 * Changed flag
	 */
	public boolean changed = false;

	/**
	 * Canvas instance
	 */
	Canvas mCanvas;

	/**
	 * Bitmap for double buffering
	 */
	Bitmap mBitmap;

	/**
	 * Paint instance
	 */
	final Paint mPaint;

	/**
	 * X coordinate
	 */
	float lastX;

	/**
	 * Y coordinate
	 */
	float lastY;
	private final Path mPath = new Path();
	private float mCurveEndX;
	private float mCurveEndY;
	private int mInvalidateExtraBorder = 10;
	static final float TOUCH_TOLERANCE = 8;
	private static final boolean RENDERING_ANTIALIAS = true;
	private static final boolean DITHER_FLAG = true;
	private int mCertainColor = 0xFF000000;
	private float mStrokeWidth = 2.0f;

	/**
	 * Initialize paint object and coordinates
	 *
	 * @param c
	 */
	public BestPaintBoard(Context context) {
		super(context);
		// create a new paint object
		mPaint = new Paint();
		mPaint.setAntiAlias(RENDERING_ANTIALIAS);
		mPaint.setColor(mCertainColor);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(mStrokeWidth);
		mPaint.setDither(DITHER_FLAG);
		lastX = -1;
		lastY = -1;
		Log.i("GoodPaintBoard", "initialized.");
	}

	/**
	 * Clear undo
	 */
	public void clearUndo() {
		while(true) {
			Bitmap prev = (Bitmap)undos.pop();
			if (prev == null) return;
			prev.recycle();
		}
	}

	/**
	 * Save undo
	 */
	public void saveUndo() {
		if (mBitmap == null) return;
		while (undos.size() >= maxUndos){
			Bitmap i = (Bitmap)undos.get(undos.size()-1);
			i.recycle();
			undos.remove(i);
		}
		Bitmap img = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas();
		canvas.setBitmap(img);
		canvas.drawBitmap(mBitmap, 0, 0, mPaint);
		undos.push(img);
		Log.i("GoodPaintBoard", "saveUndo() called.");
	}

	/**
	 * Undo
	 */
	public void undo() {
		Bitmap prev = null;
		try {
			prev = (Bitmap)undos.pop();
		} catch(Exception ex) {
			Log.e("GoodPaintBoard", "Exception : " + ex.getMessage());
		}

		if (prev != null){
			drawBackground(mCanvas);
			mCanvas.drawBitmap(prev, 0, 0, mPaint);
			// invalidate();
			Canvas canvas = holder.lockCanvas();
			if(canvas != null){
				myDraw(canvas);
				holder.unlockCanvasAndPost(canvas);
			}

			prev.recycle();
		}

		Log.i("GoodPaintBoard", "undo() called.");
	}

	/**
	 * Paint background
	 *
	 * @param g
	 * @param w
	 * @param h
	 */
	public void drawBackground(Canvas canvas) {
		if (canvas != null) {
			canvas.drawColor(Color.WHITE);
		}
	}

	/**
	 * Update paint properties
	 *
	 * @param canvas
	 */
	public void updatePaintProperty(int color, int size) {
		mPaint.setColor(color);
		mPaint.setStrokeWidth(size);
	}

	/**
	 * Create a new image
	 */
	public void newImage(int width, int height) {
		Bitmap img = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas();
		canvas.setBitmap(img);
		mBitmap = img;
		mCanvas = canvas;
		drawBackground(mCanvas);
		changed = false;
		// invalidate();
		// Canvas canvas = holder.lockCanvas();
		// if(canvas != null) {
		//   myDraw(canvas);
		//   holder.unlockCanvasAndPost(canvas);
		// }

		// Invalidate() 대신에 주어진 코드로 화면을 갱신하였지만,
		// onWindowFocusChanged 이전의 myDraw가 호출되므로 지움 <
	}

	/**
	 * Set image
	 *
	 * @param newImage
	 */
	public void setImage(Bitmap newImage) {
		changed = false;
		setImageSize(newImage.getWidth(),newImage.getHeight(),newImage);
		// invalidate();
		Canvas canvas = holder.lockCanvas();
		if(canvas != null){
			myDraw(canvas);
			holder.unlockCanvasAndPost(canvas);
		}
		// invalidate() 대신 주어진 코드를 호출
	}

	/**
	 * Set image size
	 *
	 * @param width
	 * @param height
	 * @param newImage
	 */
	public void setImageSize(int width, int height, Bitmap newImage) {
		if (mBitmap != null){
			if (width < mBitmap.getWidth()) width = mBitmap.getWidth();
			if (height < mBitmap.getHeight()) height = mBitmap.getHeight();
		}
		if (width < 1 || height < 1) return;
		Bitmap img = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas();
		drawBackground(canvas);
		if (newImage != null) {
			canvas.setBitmap(newImage);
		}
		if (mBitmap != null) {
			mBitmap.recycle();
			mCanvas.restore();
		}
		mBitmap = img;
		mCanvas = canvas;
		clearUndo();
	}

	/**
	 * onSizeChanged
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w > 0 && h > 0) {
			newImage(w, h);
		}
	}

	/**
	 * Draw the bitmap
	 */
	// protected void onDraw(Canvas canvas) {
	//   super.onDraw(canvas);
	//   if (mBitmap != null) {
	//     canvas.drawBitmap(mBitmap, 0, 0, null);
	//   }
	// }
	// onDraw 메소드를 바로 아래에 있는 myDraw 함수로 대체하였으므로 onDraw()를 주석처리함.
	SurfaceHolder holder;                    // SurfaceHolder 선언
	public void myDraw(Canvas canvas) {     // myDraw() 메소드 선언
		if (mBitmap != null) {
			canvas.drawBitmap(mBitmap, 0, 0, null);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		if (hasWindowFocus) {
			// Window Focus를 가져왔다면,
			holder = getHolder();
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				// myDraw(canvas);
				holder.unlockCanvasAndPost(canvas);
			}
			// Thread 시작
			thread = new DrawThread(true);
			thread.start();
		} else {
			// Window focus를 잃었다면 (화면을 벗어났다면)
			mHandler.removeCallbacks(drawRunnable);	// handler가 다루는 runnable 종료
			thread.setPlaying(false);			// thread의 while 루프 정지
			thread.interrupt();			// thread 정지
			thread = null;
		}
	}
	// onWindowFocusChanged()에서 window focus를 가져오면 스레드 실행 / focus를 잃을 때 스레드 종료

	DrawThread thread;    // custom thread 레퍼런스 변수

	// Thread 클래스를 상속받아 DrawThread를 구현
	public class DrawThread extends Thread {
		private int i = 0;
		private boolean isPlay = true;
		public DrawThread (boolean isPlay){
			this.isPlay = isPlay;
		}
		public void setPlaying(boolean isPlay){
			this.isPlay = isPlay;
		}
		@Override
		public void run() {
			while (isPlay) {
				try { Thread.sleep(33);       // 1초가 1000 이므로 1초에 30회 정도 호출하려면 sleep을 33으로
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Log.d("스레드", "돌고있당");       // Thread 동작을 확인하는 로그
				mHandler.post(drawRunnable);    // 핸들러에게 drawRunnable의 내용을 수행하라고 전달
			}
		}
	}

	Handler mHandler = new Handler();            // 핸들러 객체

	Runnable drawRunnable = new Runnable() {    // Runnable (핸들러에게 주는 명령)
		@Override
		public void run() {
			Log.d("핸들러", "돌고있다");                // 핸들러가 동작하는지 확인하는 로그
			Canvas canvas = holder.lockCanvas();
			if(canvas != null){
				myDraw(canvas);                        // myDraw를 호출한다.
				holder.unlockCanvasAndPost(canvas);
			}
		}
	};

	// implements한 Interface의 Override 함수들
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	/**
	 * Handles touch event, UP, DOWN and MOVE
	 */
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch (action) {
			case MotionEvent.ACTION_UP:
				changed = true;

				Rect rect = touchUp(event, false);
				if (rect != null) {
					// invalidate(rect);
					Canvas canvas = holder.lockCanvas();
					if(canvas != null){
						myDraw(canvas);
						holder.unlockCanvasAndPost(canvas);
					}
				}
				mPath.rewind();
				return true;

			case MotionEvent.ACTION_DOWN:
				saveUndo();
				rect = touchDown(event);
				if (rect != null) {
					// invalidate(rect);
					Canvas canvas = holder.lockCanvas();
					if(canvas != null){
						myDraw(canvas);
						holder.unlockCanvasAndPost(canvas);
					}
				}
				return true;

			case MotionEvent.ACTION_MOVE:
				rect = touchMove(event);
				if (rect != null) {
					// invalidate(rect);
					Canvas canvas = holder.lockCanvas();
					if(canvas != null){
						myDraw(canvas);
						holder.unlockCanvasAndPost(canvas);
					}
				}
				return true;
		}
		return false;
	}


	/**
	 * Process event for touch down
	 *
	 * @param event
	 * @return
	 */
	private Rect touchDown(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		lastX = x;
		lastY = y;
		Rect mInvalidRect = new Rect();
		mPath.moveTo(x, y);
		final int border = mInvalidateExtraBorder;
		mInvalidRect.set((int) x - border, (int) y - border, (int) x + border, (int) y + border);
		mCurveEndX = x;
		mCurveEndY = y;
		mCanvas.drawPath(mPath, mPaint);
		return mInvalidRect;
	}

	/**
	 * Process event for touch move
	 *
	 * @param event
	 * @return
	 */
	private Rect touchMove(MotionEvent event) {
		Rect rect = processMove(event);
		return rect;
	}

	private Rect touchUp(MotionEvent event, boolean cancel) {
		Rect rect = processMove(event);
		return rect;
	}

	/**
	 * Process Move Coordinates
	 *
	 * @param x
	 * @param y
	 * @param dx
	 * @param dy
	 * @return
	 */
	private Rect processMove(MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();
		final float dx = Math.abs(x - lastX);
		final float dy = Math.abs(y - lastY);
		Rect mInvalidRect = new Rect();
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			final int border = mInvalidateExtraBorder;
			mInvalidRect.set((int) mCurveEndX - border, (int) mCurveEndY - border,
					(int) mCurveEndX + border, (int) mCurveEndY + border);
			float cX = mCurveEndX = (x + lastX) / 2;
			float cY = mCurveEndY = (y + lastY) / 2;
			mPath.quadTo(lastX, lastY, cX, cY);
			// union with the control point of the new curve
			mInvalidRect.union((int) lastX - border, (int) lastY - border, (int) lastX + border, (int) lastY + border);
			// union with the end point of the new curve
			mInvalidRect.union((int) cX - border, (int) cY - border, (int) cX + border, (int) cY + border);
			lastX = x;
			lastY = y;
			mCanvas.drawPath(mPath, mPaint);
		}
		return mInvalidRect;
	}

	/**
	 * Save this contents into a Jpeg image
	 *
	 * @param outstream
	 * @return
	 */
	public boolean Save(OutputStream outstream) {
		try {
			mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
			// invalidate();
			Canvas canvas = holder.lockCanvas();
			if(canvas != null){
				// myDraw(canvas);
				holder.unlockCanvasAndPost(canvas);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
