package com.beatonma.formclockwidget.formclock;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.app.ConfigActivity;
import com.beatonma.formclockwidget.utility.DateUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.TimeUtils;
import com.beatonma.formclockwidget.utility.Utils;

import java.util.Calendar;

/**
 * Created by Michael on 06/11/2015.
 */
public class FormClockView extends View {
	private final static String TAG = "FormClockView";

	private final static String DEFAULT_TIME = " 0:00";

	private FormClockRenderer mRenderer;
	private PointF mMaxDimensions;	// Allowed size of the clock within the View. This is the upper limit for mMaxSize
	private PointF mMaxSize;		// Size of largest time + complications with current Options

	// User options
	private int mColor1 = Color.BLACK;
	private int mColor2 = Color.GRAY;
	private int mColor3 = Color.WHITE;
	private int mColorComplication = Color.WHITE;
	private int mColorShadow = Color.BLACK;

	private int mOrientation = FormClockRenderer.ORIENTATION_HORIZONTAL;

	private float mWidth;
	private float mHeight;

	private boolean mLayoutComplete = false;

	private long mAnimProgress = -1;
	private String mPreviousMinute = DEFAULT_TIME;
	private String mNowMinute = DEFAULT_TIME;

//	private int mEggRotation = 0;

	public FormClockView(Context context) {
		super(context);
		postInit(context, null, 0, 0);
	}

	public FormClockView(Context context, AttributeSet attrs) {
		super(context, attrs);
		postInit(context, attrs, 0, 0);
	}

	public FormClockView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		postInit(context, attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public FormClockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		postInit(context, attrs, defStyleAttr, defStyleRes);
	}

	private void postInit(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
		post(new Runnable() {
			@Override
			public void run() {
				mLayoutComplete = true;
				init(context, attrs, defStyleAttr, defStyleRes);
			}
		});
	}

	@SuppressWarnings("NewApi")
	private void init(Context context, AttributeSet attrs, int defStylrAttr, int defStyleRes) {
		mWidth = getMeasuredWidth();
		mHeight = getMeasuredHeight();

		mMaxDimensions = new PointF(mWidth, mHeight);

		SharedPreferences preferences = context.getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);

		mRenderer = new FormClockRenderer(buildOptions(preferences), buildPaints(preferences));

		// Find the max text size that will fit within the limits
		mRenderer.setTextSize(mRenderer.getMaxTextSize(mMaxDimensions));

		// Find the largest dimensions of the chosen text size
		mMaxSize = mRenderer.getMaxSize();

		if (mRenderer.getOptions().showAlarm) {
			if (Utils.isLollipop()) {
				AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
				if (am.getNextAlarmClock() != null) {
					mRenderer.setAlarmTime(am.getNextAlarmClock().getTriggerTime());
				}
			} else {
				mRenderer.setAlarmTime(Settings.System.getString(
						getContext().getContentResolver(),
						Settings.System.NEXT_ALARM_FORMATTED));
			}
		}

		ViewGroup.LayoutParams lp = getLayoutParams();
		lp.width = (int) mMaxSize.x;
		lp.height = (int) mMaxSize.y;
		setLayoutParams(lp);

		mPreviousMinute = format(Calendar.getInstance(), mRenderer.getOptions().is24Hour);
		mNowMinute = mPreviousMinute;

		invalidate();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		super.draw(canvas);

		if (mRenderer == null) {
			return;
		}

		mRenderer.setTime(mPreviousMinute, mNowMinute);
		mRenderer.setAnimationTime(mAnimProgress);

		PointF renderSize = mRenderer.synchronousMeasure();

//		canvas.drawColor(getResources().getColor(R.color.Highlight));
		mRenderer.draw(canvas,
				(mMaxSize.x - renderSize.x) / 2,
				(mMaxSize.y - renderSize.y) / 2);
	}

	public void setAnimationProgress(long progress) {
		mAnimProgress = progress;
		invalidate();
	}

	public long setTimeAndProgress(String previousMinute, String nowMinute, long progress) {
		mPreviousMinute = previousMinute;
		mNowMinute = nowMinute;
		mAnimProgress = progress;

		mRenderer.setTime(mPreviousMinute, mNowMinute);
		return mRenderer.getAnimDuration();
	}

	public long getAnimDuration(Calendar calendar) {
		boolean is24Hour = mRenderer.getOptions().is24Hour;

		mNowMinute = format(calendar, is24Hour);
		calendar.add(Calendar.MINUTE, -1);
		mPreviousMinute = format(calendar, is24Hour);
		calendar.add(Calendar.MINUTE, 1);

		mRenderer.setTime(mPreviousMinute, mNowMinute);
		return mRenderer.getAnimDuration();
	}

	private FormClockRenderer.Options buildOptions(SharedPreferences preferences) {
		FormClockRenderer.Options options = new FormClockRenderer.Options();
		options.charSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, getResources().getDisplayMetrics());
		options.maxComplicationSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics());
		options.glyphAnimDuration = 2000;
		options.glyphAnimAverageDelay = options.glyphAnimDuration / 4;

		if (preferences != null) {
			options.showShadow = preferences.getBoolean(PrefUtils.PREF_THEME_SHADOW, false);
			options.showDate = preferences.getBoolean(PrefUtils.PREF_SHOW_DATE, false);
			options.showAlarm = preferences.getBoolean(PrefUtils.PREF_SHOW_ALARM, false);
			options.dateFormat = DateUtils.getFormat(preferences);
			options.is24Hour = TimeUtils.getFormat(getContext(), preferences);
			options.uppercase = preferences.getBoolean(PrefUtils.PREF_DATE_UPPERCASE, true);
			options.zeroPadding = preferences.getBoolean(PrefUtils.PREF_FORMAT_ZERO_PADDING, false);

			mOrientation = preferences.getInt(PrefUtils.PREF_THEME_ORIENTATION, FormClockRenderer.ORIENTATION_HORIZONTAL);
		}

		if (mOrientation == FormClockRenderer.ORIENTATION_FIT_SPACE) {
			if (mWidth > mHeight) {
				options.orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
			}
			else {
				options.orientation = FormClockRenderer.ORIENTATION_VERTICAL;
			}
		}
		else if (mOrientation == FormClockRenderer.ORIENTATION_FIT_DEVICE_ORIENTATION) {
			int deviceOrientation = getResources().getConfiguration().orientation;
			if (deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
				options.orientation = FormClockRenderer.ORIENTATION_VERTICAL;
			}
			else if (deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				options.orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
			}
			else {
				options.orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
			}
		}
		else {
			options.orientation = mOrientation;
		}

		return options;
	}

	private FormClockRenderer.ClockPaints buildPaints(SharedPreferences preferences) {
		if (preferences != null) {
			mColor1 = ColorUtils.getColorContainerFromPreference(preferences, PrefUtils.PREF_COLOR1).getColor();
			mColor2 = ColorUtils.getColorContainerFromPreference(preferences, PrefUtils.PREF_COLOR2).getColor();
			mColor3 = ColorUtils.getColorContainerFromPreference(preferences, PrefUtils.PREF_COLOR3).getColor();
			mColorShadow = ColorUtils.getColorContainerFromPreference(preferences, PrefUtils.PREF_COLOR_SHADOW).getColor();
			mColorComplication = ColorUtils.getColorContainerFromPreference(preferences, PrefUtils.PREF_COLOR_COMPLICATIONS).getColor();
		}

		FormClockRenderer.ClockPaints paints = new FormClockRenderer.ClockPaints();
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(mColor1);
		paints.fills[0] = paint;
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(mColor2);
		paints.fills[1] = paint;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(mColor3);
		paints.fills[2] = paint;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(mColorComplication);
		paint.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Roboto-Regular.ttf"));
		paints.complications = paint;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(mColorShadow);
		paint.setStyle(Paint.Style.STROKE);
		paint.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Roboto-Regular.ttf"));
		paints.shadow = paint;

		return paints;
	}

	public void setOrientation(int orientation) {
		mOrientation = orientation;
	}

	@SuppressWarnings("NewApi")
	public void regen(SharedPreferences preferences) {
		if (!mLayoutComplete) {
			return;
		}

		mRenderer = new FormClockRenderer(buildOptions(preferences), buildPaints(preferences));

		mRenderer.setTextSize(mRenderer.getMaxTextSize(mMaxDimensions));
		mMaxSize = mRenderer.getMaxSize();

		if (mRenderer.getOptions().showAlarm) {
			if (Utils.isLollipop()) {
				AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
				if (am.getNextAlarmClock() != null) {
					mRenderer.setAlarmTime(am.getNextAlarmClock().getTriggerTime());
				}
			} else {
				mRenderer.setAlarmTime(Settings.System.getString(
						getContext().getContentResolver(),
						Settings.System.NEXT_ALARM_FORMATTED));
			}
		}

		ViewGroup.LayoutParams lp = getLayoutParams();
		lp.width = (int) mMaxSize.x;
		lp.height = (int) mMaxSize.y;
		setLayoutParams(lp);

		mPreviousMinute = format(Calendar.getInstance(), mRenderer.getOptions().is24Hour);
		mNowMinute = mPreviousMinute;

		invalidate();
		Log.d(TAG, "Regenerated");
	}

	private String format(Calendar cal, boolean is24Hour) {
		int hour = cal.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
		int min = cal.get(Calendar.MINUTE);

		if (!is24Hour && hour == 0) {
			hour = 12;
		}

		return (hour < 10 ? " " : "")
				+ hour
				+ ":" + (min < 10 ? "0" : "")
				+ min;
	}

	public boolean isSameSize(FormClockView other) {
		return this.mRenderer.getOptions()
				.isSameSize(other.mRenderer.getOptions());
	}


	// Eggs?!
	private FormClockRenderer.Options buildEggOptions() {
		FormClockRenderer.Options options = new FormClockRenderer.Options();
		options.charSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, getResources().getDisplayMetrics());
		options.maxComplicationSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics());
		options.glyphAnimDuration = 3000;
		options.glyphAnimAverageDelay = options.glyphAnimDuration / 4;
		options.orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
		options.showDate = false;
		options.showAlarm = false;
		options.showShadow = true;

		return options;
	}

	private FormClockRenderer.ClockPaints buildEggPaints() {
		return buildPaints(null);
	}

	public void regenEgg() {
		if (!mLayoutComplete) {
			return;
		}

		mRenderer = new FormClockRenderer(buildEggOptions(), buildEggPaints());

		mRenderer.setTextSize(mRenderer.getMaxEggTextSize(mMaxDimensions));
		mMaxSize = new PointF(mWidth, mHeight);

		ViewGroup.LayoutParams lp = getLayoutParams();
		lp.width = (int) mMaxSize.x;
		lp.height = (int) mMaxSize.y;
		setLayoutParams(lp);
	}

	private int getAccent() {
		if (getContext() instanceof ConfigActivity) {
			return ((ConfigActivity) getContext()).getAccentColor();
		}
		else {
			return getResources().getColor(R.color.Accent);
		}
	}
}
