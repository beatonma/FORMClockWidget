package com.beatonma.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 04/01/2016.
 */
public class CustomColorPreviewView extends View {
	private int mColor = -1;
	private Bitmap mBackground = null;

	public CustomColorPreviewView(Context context) {
		super(context);
		postInit();
	}

	public CustomColorPreviewView(Context context, AttributeSet attrs) {
		super(context, attrs);
		postInit();
	}

	public CustomColorPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		postInit();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CustomColorPreviewView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		postInit();
	}

	private void postInit() {
		post(new Runnable() {
			@Override
			public void run() {
				init();
			}
		});
	}

	private void init() {
		int cellSize = getResources().getDimensionPixelSize(R.dimen.color_transparent_tile_size);
		mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);

		Canvas canvas = new Canvas(mBackground);
		Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		darkPaint.setColor(Color.parseColor("#dddddd"));
		lightPaint.setColor(Color.WHITE);

		int width = getWidth() / cellSize;
		int height = getHeight() / cellSize;

		for (int i = 0; i < width + 1; i++) {
			for (int j = 0; j < height + 1; j++) {
				int left = i * cellSize;
				int top = j * cellSize;

				if (i % 2 == 0) {
					canvas.drawRect(left, top, left + cellSize, top + cellSize, j % 2 == 0 ? darkPaint : lightPaint);
				}
				else {
					canvas.drawRect(left, top, left + cellSize, top + cellSize, j % 2 == 0 ? lightPaint : darkPaint);
				}
			}
		}

		invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mBackground != null && !mBackground.isRecycled()) {
			canvas.drawBitmap(mBackground, 0, 0, null);
		}

		canvas.drawColor(mColor);
	}

	public void setColor(int color) {
		mColor = color;
		invalidate();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mBackground.recycle();
	}
}
