package com.beatonma.formclockwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.beatonma.colorpicker.ColorContainer;
import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.formclock.FormClockRenderer;
import com.beatonma.formclockwidget.utility.DateUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.TimeUtils;
import com.beatonma.formclockwidget.utility.Utils;
import com.beatonma.formclockwidget.widget.OnTouchActivity;
import com.beatonma.formclockwidget.widget.WidgetUpdateService;

import java.util.Calendar;

/**
 * Created by Michael on 25/10/2015.
 */
public class WidgetProvider extends AppWidgetProvider {
	private final static String TAG = "WidgetProvider";

	public final static String UPDATE = "com.beatonma.formclockwidget.UPDATE";
	public final static String ANIMATE = "com.beatonma.formclockwidget.ANIMATE";

	private int mMaxWidgetWidth;
	private int mMaxWidgetHeight;
	private int mMinWidgetWidth;
	private int mMinWidgetHeight;
	private int mWidth;
	private int mHeight;

	private long mAnimProgress = -1;

	// User preferences
	private boolean mShowShadow = false;
	private boolean mShowDate = false;
	private boolean mShowAlarm = false;
	private int mColor1 = Color.BLACK;
	private int mColor2 = Color.GRAY;
	private int mColor3 = Color.WHITE;
	private int mColorShadow = Color.BLACK;
	private int mColorComplication = Color.WHITE;
	private int mOrientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
	private boolean mZeroPadding = false;
	private String mDateFormat = DateUtils.LONG;
	private boolean mTimeFormat;
	private boolean mDateUppercase = true;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		updateWidgets(context);
		startUpdateService(context);
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
		int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
		int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

		DisplayMetrics dm = context.getResources().getDisplayMetrics();

		maxWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxWidth, dm);
		maxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxHeight, dm);
		minWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidth, dm);
		minHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minHeight, dm);

		// Store dimensions of this specific widget instance
		getSharedPreferences(context).edit()
				.putInt(PrefUtils.MAX_WIDGET_WIDTH + appWidgetId, maxWidth)
				.putInt(PrefUtils.MAX_WIDGET_HEIGHT + appWidgetId, maxHeight)
				.putInt(PrefUtils.MIN_WIDGET_WIDTH + appWidgetId, minWidth)
				.putInt(PrefUtils.MIN_WIDGET_HEIGHT + appWidgetId, minHeight)
				.commit();

		updateWidgets(context);
		startUpdateService(context);
	}

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent) {
		switch (intent.getAction()) {
			case UPDATE:
				updateWidgets(context);
				break;
			case ANIMATE:
				mAnimProgress = intent.getLongExtra("animation_progress", -1);
				updateWidgets(context);
				break;
			default:
				super.onReceive(context, intent);
		}
	}

	@Override
	public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
		super.onRestored(context, oldWidgetIds, newWidgetIds);
		updateWidgets(context);
		startUpdateService(context);
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		updateWidgets(context);
		startUpdateService(context);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		stopUpdateService(context);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		for (int appWidgetId : appWidgetIds) {
			editor
					.remove(PrefUtils.MAX_WIDGET_WIDTH + appWidgetId)
					.remove(PrefUtils.MAX_WIDGET_HEIGHT + appWidgetId)
					.remove(PrefUtils.MIN_WIDGET_WIDTH + appWidgetId)
					.remove(PrefUtils.MIN_WIDGET_HEIGHT + appWidgetId);
		}
		editor.apply();
	}

	private void startUpdateService(Context context) {
		context.startService(new Intent(context, WidgetUpdateService.class));
	}

	private void stopUpdateService(Context context) {
		Intent intent = new Intent(context, WidgetUpdateService.class);
		context.stopService(intent);
	}

	private void updateWidgets(Context context) {
		ComponentName component = new ComponentName(context.getPackageName(), getClass().getName());
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int appWidgetIds[] = appWidgetManager.getAppWidgetIds(component);

		Bitmap bitmap = null;

		for (int id : appWidgetIds) {
			loadPreferences(context, getSharedPreferences(context), id);
			populateWidgetDimensions(context);

			bitmap = generateBitmap(context);
			updateWidget(context, appWidgetManager, id, bitmap);
		}

		if (bitmap != null) {
			bitmap.recycle();
		}
	}

	@SuppressWarnings("NewApi")
	private Bitmap generateBitmap(Context context) {
		FormClockRenderer renderer = new FormClockRenderer(buildOptions(context), buildPaints(context));
		renderer.setTextSize(getTextSize(renderer));

		if (mAnimProgress != -1) {
			Calendar cal = Calendar.getInstance();

			String currentTime = formatTime(cal, renderer.getOptions().is24Hour);
			cal.add(Calendar.MINUTE, -1);
			String previousTime = formatTime(cal, renderer.getOptions().is24Hour);

			renderer.setTime(previousTime, currentTime);
			renderer.setAnimationTime(mAnimProgress);
		}
		else {
			renderer.setTime(getTimeString(renderer.getOptions().is24Hour));
		}

		if (mShowAlarm) {
			if (Utils.isLollipop()) {
				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				if (am.getNextAlarmClock() != null) {
					renderer.setAlarmTime(am.getNextAlarmClock().getTriggerTime());
				}
			}
			else {
				renderer.setAlarmTime(Settings.System.getString(
						context.getContentResolver(),
						Settings.System.NEXT_ALARM_FORMATTED));
			}
		}

		PointF maxSize = renderer.getMaxSize();
		PointF renderSize = renderer.synchronousMeasure();

		Bitmap bitmap = Bitmap.createBitmap((int) maxSize.x, (int) maxSize.y, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		if (PrefUtils.DEBUG) {
			canvas.drawColor(context.getResources().getColor(R.color.Highlight));
		}

		renderer.draw(canvas,
				(maxSize.x - renderSize.x) / 2,
				(maxSize.y - renderSize.y) / 2);

		return bitmap;
	}

	private void populateWidgetDimensions(Context context) {
		int orientation = context.getResources().getConfiguration().orientation;

		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mHeight = mMinWidgetHeight;
			mWidth = mMaxWidgetWidth;
		}
		else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			mWidth = mMinWidgetWidth;
			mHeight = mMaxWidgetHeight;
		}
		else {
			mWidth = mMaxWidgetWidth;
			mHeight = mMaxWidgetHeight;
		}
	}

	private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bitmap bitmap) {
		RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		view.setImageViewBitmap(R.id.clock_view, bitmap);
		view.setOnClickPendingIntent(R.id.clock_view, getOnClickIntent(context, appWidgetId));
		appWidgetManager.updateAppWidget(appWidgetId, view);
	}

	private String getTimeString(boolean is24Hour) {
		Calendar cal = Calendar.getInstance();
		return formatTime(cal, is24Hour);
	}

	private String formatTime(Calendar cal, boolean is24Hour) {
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

	private int getTextSize(FormClockRenderer renderer) {
		int textSize = 1;
		PointF maxDimensions = new PointF(mWidth, mHeight);
		PointF measuredDimensions = new PointF(1, 1);

		while (textFits(measuredDimensions, maxDimensions)) {
			renderer.setTextSize(textSize++);
			measuredDimensions = renderer.getMaxSize();
		}

		textSize -= 1;
		return textSize;
	}

	private boolean textFits(PointF measuredTextSize, PointF maxDimensions) {
		if (measuredTextSize.x > maxDimensions.x) {
			return false;
		}
		if (measuredTextSize.y > maxDimensions.y) {
			return false;
		}
		return true;
	}

	private FormClockRenderer.Options buildOptions(Context context) {
		FormClockRenderer.Options options = new FormClockRenderer.Options();
		options.charSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, context.getResources().getDisplayMetrics());
		options.maxComplicationSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 36, context.getResources().getDisplayMetrics());

		options.is24Hour = mTimeFormat;
		options.glyphAnimDuration = 2000;
		options.glyphAnimAverageDelay = options.glyphAnimDuration / 4;
		options.showShadow = mShowShadow;
		options.showDate = mShowDate;
		options.showAlarm = mShowAlarm;
		options.dateFormat = mDateFormat;
		options.uppercase = mDateUppercase;
		options.zeroPadding = mZeroPadding;

		if (mOrientation == FormClockRenderer.ORIENTATION_FIT_SPACE) {
			if (mWidth > mHeight) {
				options.orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
			}
			else {
				options.orientation = FormClockRenderer.ORIENTATION_VERTICAL;
			}
		}
		else if (mOrientation == FormClockRenderer.ORIENTATION_FIT_DEVICE_ORIENTATION) {
			int deviceOrientation = context.getResources().getConfiguration().orientation;
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

	private FormClockRenderer.ClockPaints buildPaints(Context context) {
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
		paint.setTypeface(Typeface.createFromAsset(context.getResources().getAssets(), "Roboto-Regular.ttf"));
		paints.complications = paint;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(mColorShadow);
		paint.setStyle(Paint.Style.STROKE);
		paint.setTypeface(Typeface.createFromAsset(context.getResources().getAssets(), "Roboto-Regular.ttf"));
		paints.shadow = paint;

		return paints;
	}

	private PendingIntent getUpdateIntent(Context context, int appWidgetId) {
		Intent intent = new Intent(UPDATE);
		return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent getOnClickIntent(Context context, int appWidgetId) {
		Intent intent = new Intent(context, OnTouchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}


	private SharedPreferences getSharedPreferences(Context context) {
		return PrefUtils.get(context);
	}

	private void loadPreferences(Context context, SharedPreferences preferences, int appWidgetId) {
		boolean firstRun = preferences.getBoolean(PrefUtils.PREF_FIRST_RUN, true);
		if (firstRun) {
			preferences = PrefUtils.initiate(context, preferences);
		}

		mMaxWidgetWidth = preferences.getInt(PrefUtils.MAX_WIDGET_WIDTH + appWidgetId, 1000);
		mMaxWidgetHeight = preferences.getInt(PrefUtils.MAX_WIDGET_HEIGHT + appWidgetId, 500);
		mMinWidgetWidth = preferences.getInt(PrefUtils.MIN_WIDGET_WIDTH + appWidgetId, 1000);
		mMinWidgetHeight = preferences.getInt(PrefUtils.MIN_WIDGET_HEIGHT + appWidgetId, 500);

		try {
			mOrientation = Integer.valueOf(preferences.getString(PrefUtils.PREF_THEME_ORIENTATION, "" + FormClockRenderer.ORIENTATION_HORIZONTAL));
		}
		catch (Exception e) {
			mOrientation = preferences.getInt(PrefUtils.PREF_THEME_ORIENTATION, FormClockRenderer.ORIENTATION_HORIZONTAL);
		}

		mShowDate = preferences.getBoolean(PrefUtils.PREF_SHOW_DATE, false);
		mShowAlarm = preferences.getBoolean(PrefUtils.PREF_SHOW_ALARM, false);
		mShowShadow = preferences.getBoolean(PrefUtils.PREF_THEME_SHADOW, false);

		mDateFormat = DateUtils.getFormat(preferences);
		mDateUppercase = preferences.getBoolean(PrefUtils.PREF_DATE_UPPERCASE, true);

		mTimeFormat = TimeUtils.getFormat(context, preferences);
		mZeroPadding = preferences.getBoolean(PrefUtils.PREF_FORMAT_ZERO_PADDING, false);

		mColor1 = getColor(preferences, PrefUtils.PREF_COLOR1);
		mColor2 = getColor(preferences, PrefUtils.PREF_COLOR2);
		mColor3 = getColor(preferences, PrefUtils.PREF_COLOR3);

		mColorShadow = getColor(preferences, PrefUtils.PREF_COLOR_SHADOW);
		mColorComplication = getColor(preferences, PrefUtils.PREF_COLOR_COMPLICATIONS);
	}

	private int getColor(SharedPreferences preferences, String key) {
		ColorContainer container = ColorUtils.getColorContainerFromPreference(preferences, key);
		return container.getColor();
	}
}
