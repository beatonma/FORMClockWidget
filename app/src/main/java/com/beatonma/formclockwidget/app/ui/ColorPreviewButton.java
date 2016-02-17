package com.beatonma.formclockwidget.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;

import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 16/11/2015.
 */
public class ColorPreviewButton extends Button {
	private final static String TAG = "ColorPreviewButton";

	private Paint mPaint;
	private int mMargin;

	private int mActiveColor = -1;
	private int mInactiveColor = Color.parseColor("#dddddd");

	private Bitmap mBackground = null;
	private Drawable mFromWallpaperIcon = null;

	private String mKey = "";

	public ColorPreviewButton(Context context) {
		super(context);
		postInit();
	}

	public ColorPreviewButton(Context context, AttributeSet attrs) {
		super(context, attrs);
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

	// Set up background transparency grid and shadow provider
	@SuppressWarnings("NewApi")
	private void init() {
		mMargin = (int) getResources().getDimension(R.dimen.button_margin);

		if (Utils.isLollipop()) {
			setOutlineProvider(new ViewOutlineProvider() {
				@Override
				public void getOutline(View view, Outline outline) {
					outline.setRect(mMargin, mMargin, getWidth() - mMargin, getHeight() - mMargin);
				}
			});
		}

		int cellSize = getResources().getDimensionPixelSize(R.dimen.color_transparent_tile_size);
		mBackground = Bitmap.createBitmap(getWidth() - (2 * mMargin), getHeight() - (2 * mMargin), Bitmap.Config.RGB_565);

		int size = (getWidth() / 2);
		int margin = size / 2;
		mFromWallpaperIcon = getResources().getDrawable(R.drawable.ic_wallpaper_black_24dp);
		mFromWallpaperIcon.setBounds(margin, margin, getWidth() - margin, getHeight() - margin);
//		mFromWallpaperIcon.mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

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

	private void initPaint() {
		if (mPaint == null) {
			mPaint = new Paint();
			mPaint.setStyle(Paint.Style.FILL);
		}
	}

	@SuppressWarnings("NewApi")
	public void setColor(int color) {
		initPaint();

		mActiveColor = color;

		if (isClickable()) {
			mPaint.setColor(mActiveColor);
		}

		invalidate();
	}

	public void setKey(String key) {
		mKey = key;
	}

	public void setActive(boolean b) {
		setClickable(b);

		initPaint();
		mPaint.setColor(b ? mActiveColor : mInactiveColor);
		invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mBackground != null && !mBackground.isRecycled()) {
			canvas.drawBitmap(mBackground, mMargin, mMargin, null);
		}

		if (mPaint != null) {
			canvas.drawRect(mMargin, mMargin, getWidth() - mMargin, getHeight() - mMargin, mPaint);
		}

		if (!isClickable() && mFromWallpaperIcon != null) {
			int color = ColorUtils.getColorContainerFromPreference(PrefUtils.get(getContext()), mKey, Color.WHITE).getFromWallpaper();
			mFromWallpaperIcon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
			mFromWallpaperIcon.draw(canvas);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mBackground.recycle();
	}
}
