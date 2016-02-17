package com.beatonma.formclockwidget.widget;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;

import com.beatonma.formclockwidget.WidgetProvider;
import com.beatonma.formclockwidget.formclock.FormClockRenderer;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.UpdateUtils;
import com.beatonma.formclockwidget.utility.WallpaperUtils;

import java.util.Calendar;

/**
 * Created by Michael on 30/10/2015.
 */
public class WidgetUpdateService extends Service {
	private final static String TAG = "UpdateService";
	private final static int FRAME_RATE = 60;
	private final static IntentFilter INTENT_FILTER;
	private final static String MUZEI_ARTWORK_CHANGED = "com.google.android.apps.muzei.ACTION_ARTWORK_CHANGED";
	public final static String UPDATE_GENERAL = "com.beatonma.formclockwidget.UPDATE_GENERAL";
	public final static String UPDATE_COLORS = "com.beatonma.formclockwidget.UPDATE_COLORS";

	// Action launcher update intent - I don't think we can actually do anything with this...
	private final static String ACTION_PUBLISH_STATE = "com.actionlauncher.api.action.PUBLISH_UPDATE";

	static {
		INTENT_FILTER = new IntentFilter();
		INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
		INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
		INTENT_FILTER.addAction(MUZEI_ARTWORK_CHANGED);
		INTENT_FILTER.addAction(UPDATE_GENERAL);
		INTENT_FILTER.addAction(UPDATE_COLORS);
	}

	private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(UPDATE_COLORS)) {
				Log.d(TAG, "Forcing wallpaper color sync");
				getWallpaperColors();
			}
			else if (intent.getAction().equals(MUZEI_ARTWORK_CHANGED)) {
				Log.d(TAG, "Muzei wallpaper changed - updating colors");
				getWallpaperColors();
			}
			else if (shouldUpdateColors()) {
				getWallpaperColors();
			}
			else {
				doUpdate();
			}
		}
	};

	private final Runnable mAnimationRunnable = new Runnable() {
		@Override
		public void run() {
			animatedWidgetUpdate();
		}
	};

	private Handler mHandler;
	private long mAnimProgress;
	private long mAnimDuration;

	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(mUpdateReceiver, INTENT_FILTER);
		if (shouldUpdateColors()) {
			getWallpaperColors();
		}
		else {
			doUpdate();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		registerReceiver(mUpdateReceiver, INTENT_FILTER);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Update service destroyed.");
		unregisterReceiver(mUpdateReceiver);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void doUpdate() {
		boolean enableAnimation = getSharedPreferences(PrefUtils.PREFS, MODE_PRIVATE)
				.getBoolean(PrefUtils.PREF_ENABLE_ANIMATION, false);

		if (enableAnimation) {
			mAnimProgress = 0;
			updateAnimDuration();

			animatedWidgetUpdate();
		}
		else {
			staticWidgetUpdate();
		}
	}

	private void updateAnimDuration() {
		FormClockRenderer.Options options = new FormClockRenderer.Options();
		options.charSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, getResources().getDisplayMetrics());
		options.is24Hour = DateFormat.is24HourFormat(this);
		options.glyphAnimDuration = 2000;
		options.glyphAnimAverageDelay = options.glyphAnimDuration / 4;

		Calendar cal = Calendar.getInstance();
		String thisMinute = format(cal, options.is24Hour);
		cal.add(Calendar.MINUTE, -1);
		String previousMinute = format(cal, options.is24Hour);

		FormClockRenderer renderer = new FormClockRenderer(options, null);
		renderer.setTime(previousMinute, thisMinute);

		mAnimDuration = renderer.getAnimDuration();
	}

	private void animatedWidgetUpdate() {
		if (mAnimProgress <= mAnimDuration) {
			mAnimProgress += (mAnimDuration / FRAME_RATE);

			Intent animateIntent = new Intent(this, WidgetProvider.class);
			animateIntent.setAction(WidgetProvider.ANIMATE);
			animateIntent.putExtra("animation_progress", mAnimProgress);

			sendBroadcast(animateIntent);

			getHandler().postDelayed(mAnimationRunnable, 1000 / FRAME_RATE);
		}
		else {
			getHandler().removeCallbacks(mAnimationRunnable);
			mHandler = null;
		}
	}

	private void staticWidgetUpdate() {
		Intent updateIntent = new Intent(this, WidgetProvider.class);
		updateIntent.setAction(WidgetProvider.UPDATE);
		sendBroadcast(updateIntent);
	}

	private Handler getHandler() {
		if (mHandler == null) {
			mHandler = new Handler();
		}
		return mHandler;
	}

	private String format(int hour, int min) {
		return (hour < 10 ? " " : "")
				+ hour
				+ ":" + (min < 10 ? "0" : "")
				+ min;
	}

	private String format(Calendar cal, boolean is24Hour) {
		return format(
				cal.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR),
				cal.get(Calendar.MINUTE));
	}

	private boolean shouldUpdateColors() {
		SharedPreferences sp = getSharedPreferences(PrefUtils.PREFS, MODE_PRIVATE);

		// Check if user preferences require wallpaper colors. If all colors are locked then no point in updating wallpaper
		boolean needWallpaperColors = !PrefUtils.allColorsLocked(sp);
//		Log.d(TAG, "user needs wallpaper colors = " + needWallpaperColors);

		// Check if user preferences for update interval allows updating now.
		boolean longEnoughSinceLastUpdate = UpdateUtils.allowUpdate(sp);
//		Log.d(TAG, "user update interval allows update now = " + longEnoughSinceLastUpdate);

		boolean shouldUpdate = needWallpaperColors && longEnoughSinceLastUpdate;
		if (shouldUpdate) {
//			Log.d(TAG, "Color sync is allowed this time. Updating last_update=" + System.currentTimeMillis());
			sp.edit()
					.putLong(UpdateUtils.LAST_UPDATE, System.currentTimeMillis())
					.apply();
		}
		else {
//			Log.d(TAG, "Color sync denied.");
		}
		return shouldUpdate;
	}

	private void getWallpaperColors() {
		WallpaperUtils.extractColors(this, WallpaperUtils.getWallpaperBitmap(this));
		doUpdate();
	}
}
