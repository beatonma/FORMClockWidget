package com.beatonma.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 09/11/2015.
 */
public class ColorSeekbarView extends RelativeLayout {
	public final static int CHANNEL_RED = 0;
	public final static int CHANNEL_GREEN = 1;
	public final static int CHANNEL_BLUE = 2;
	public final static int CHANNEL_ALPHA = 3;

	private int mChannel = 0;

	private TextView mText;
	private AppCompatSeekBar mSlider;
	private TextView mValue;

	private OnValueChangedListener mListener;

	public ColorSeekbarView(Context context) {
		super(context);
		init(context, null, 0, 0);
	}

	public ColorSeekbarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0, 0);
	}

	public ColorSeekbarView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ColorSeekbarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs, defStyleAttr, defStyleRes);
	}

	@SuppressWarnings("NewApi")
	private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		View v = inflate(context, R.layout.view_color_slider, this);

		mText = (TextView) v.findViewById(R.id.text);
		mSlider = (AppCompatSeekBar) v.findViewById(R.id.slider);
		mValue = (TextView) v.findViewById(R.id.value);

		TypedArray a = context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.ColorSeekbarView, defStyleAttr, defStyleRes);

		try {
			int ref = a.getResourceId(R.styleable.ColorSeekbarView_text, 0);
			if (ref != 0) {
				mText.setText(getResources().getString(ref));
			}
			else {
				mText.setText(a.getString(R.styleable.ColorSeekbarView_text));
			}

			ref = a.getResourceId(R.styleable.ColorSeekbarView_textcolor, 0);
			if (ref != 0) {
				int color = getResources().getInteger(ref);
				mText.setTextColor(color);
				mValue.setTextColor(color);
				if (Utils.isLollipop()) {
					mSlider.setThumbTintList(ColorStateList.valueOf(color));
					mSlider.setProgressTintList(ColorStateList.valueOf(color));
				}
				else {
					Drawable d = mSlider.getThumb();
					d.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
					mSlider.setThumb(d);

					d = mSlider.getProgressDrawable();
					d.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
					mSlider.setProgressDrawable(d);
				}
			}

			mChannel = a.getInteger(R.styleable.ColorSeekbarView_channel, 0);
		}
		finally {
			a.recycle();
		}

		mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mValue.setText(String.valueOf(progress));
				notifyListener(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
	}

	public void setValue(int value) {
		mSlider.setProgress(value);
	}

	public void setListener(OnValueChangedListener listener) {
		mListener = listener;
	}

	public void notifyListener(int value) {
		if (mListener != null) {
			mListener.onValueChanged(mChannel, value);
		}
	}

	public interface OnValueChangedListener {
		void onValueChanged(int channel, int value);
	}
}
