package com.beatonma.formclockwidget.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.beatonma.colorpicker.ColorPickerFragment;
import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.app.ui.AnimatedPopupFragment;
import com.beatonma.formclockwidget.data.AppContainer;
import com.beatonma.formclockwidget.data.DbHelper;
import com.beatonma.formclockwidget.formclock.FormClockView;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.WallpaperUtils;
import com.beatonma.formclockwidget.utility.Utils;
import com.beatonma.formclockwidget.widget.WidgetUpdateService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Michael on 06/11/2015.
 */
public class ConfigActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "ConfigActivity";

	private final static int TAB_COLORS = 0;
	private final static int TAB_LAYOUT = 1;
	private final static int TAB_FORMAT = 2;
	private final static int TAB_OTHER = 3;
	private final static int TAB_BETA = 4;
    private final static int TAB_FEEDBACK = 4;

	private final static int TOP_CLOCK_ELEVATION = 2;
	private final static int BOTTOM_CLOCK_ELEVATION = 1;

	// Keys for save/restore state
	private final static String STATE_TAB = "selected_tab";

	private final static IntentFilter INTENT_FILTER;
	static {
		INTENT_FILTER = new IntentFilter();
		INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
		INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
	}
	private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!mEgg) {
				tick();
			}
		}
	};

	private Context mContext;

	private ViewPager mViewPager;
	private ViewPagerAdapter mAdapter;

	private SharedPreferences mPreferences;
	private int mThemeId;
	private int mAccentColor;

	private Handler mHandler;
	private Calendar mCalendar;
	private long mAnimDuration;
	private long mAnimProgress = 0;
	private long mAnimLastFrame = 0;
	private FormClockView mClock1;
	private FormClockView mClock2;
	private final Runnable mPreviewUpdateRunnable = new Runnable() {
		@Override
		public void run() {
			mClock1.setAnimationProgress(mAnimProgress);
			mClock2.setAnimationProgress(mAnimProgress);

			long frameTime = mAnimLastFrame == 0 ? 0 : System.currentTimeMillis() - mAnimLastFrame;

			mAnimProgress += frameTime;

			if (mAnimProgress <= mAnimDuration) {
				mAnimLastFrame = System.currentTimeMillis();
				mHandler.post(this);
			}
			else {
				mAnimLastFrame = 0;
			}
		}
	};

	private boolean mFirstRun = true;

	// User preferences
	private int mColor1;
	private int mColor2;
	private int mColor3;
	private int mColorShadow;
	private int mColorComplications;

	// App list used for setting up touch shortcuts
	private ArrayList<AppContainer> mInstalledApps;

	private boolean mAppListLoaded = false;
	private boolean mWallpaperLoaded = false;

	private int mSavedTab = -1;

	// ?!?
	private boolean mEgg = false;
	private long mEggAnimDuration = 500;
	private long mEggAnimProgress = 0;
	private float mEggRotationMultiplier = 1f;
	private final Runnable mEggUpdateRunnable = new Runnable() {
		@Override
		public void run() {
			float progress = (float) Math.sin(360f *
					((float) mEggAnimProgress / (float) mEggAnimDuration));
			progress *= mEggRotationMultiplier;
			mClock1.setRotation(progress);
			mClock2.setRotation(progress);

			long frameTime = mAnimLastFrame == 0 ? 0 : System.currentTimeMillis() - mAnimLastFrame;

			mEggAnimProgress += frameTime;

			if (mEggAnimProgress <= mEggAnimDuration) {
				mAnimLastFrame = System.currentTimeMillis();
				mHandler.post(this);
			}
			else {
				mAnimLastFrame = 0;
				mEggAnimProgress = 0;
				mClock1.setRotation(0);
				mClock2.setRotation(0);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		loadPreferences();

		mPreferences.registerOnSharedPreferenceChangeListener(this);

		mThemeId = WallpaperUtils.getThemeFromWallpaper(mPreferences);
		setTheme(mThemeId);

		if (savedInstanceState != null) {
			mSavedTab = savedInstanceState.getInt(STATE_TAB, -1);
		}

		setContentView(getLayout());

		init();
	}

	private int getLayout() {
		DisplayMetrics dm = getResources().getDisplayMetrics();

		int layoutId;

		int w = Utils.pxToDp(this, dm.widthPixels);
		int h = Utils.pxToDp(this, dm.heightPixels);

		if (w > h) { // Landscape
			if (w >= 650) {
				layoutId = R.layout.activity_config_wide_landscape;
			}
			else {
				layoutId = R.layout.activity_config_nopreview;
			}
		}
		else { // Portrait
			if (w >= 600) {
				layoutId = R.layout.activity_config_wide_portrait;
			}
			else if (h >= 400) {
				layoutId = R.layout.activity_config;
			}
			else {
				layoutId = R.layout.activity_config_nopreview;
			}
		}

		return layoutId;
	}

	private void init() {
		mHandler = new Handler(Looper.getMainLooper());
		mCalendar = Calendar.getInstance();

		mClock1 = (FormClockView) findViewById(R.id.clock_preview_1);
		mClock2 = (FormClockView) findViewById(R.id.clock_preview_2);

		regenClockPreview(false);

		TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
		mViewPager = (ViewPager) findViewById(R.id.viewpager);

		mAdapter = new ViewPagerAdapter();
		mViewPager.setAdapter(mAdapter);

		mViewPager.setOffscreenPageLimit(4);
		mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				if (position == TAB_OTHER && positionOffset > 0) {
					mViewPager.setBackgroundColor(ColorUtils.interpolate(
							getResources().getColor(R.color.Dialog),
							getResources().getColor(R.color.DialogDark),
							AnimationUtils.interpolateAccelerateDecelerate(positionOffset)));
				}
				else if (position == TAB_BETA) {
					mViewPager.setBackgroundColor(getResources().getColor(R.color.DialogDark));

					regenEggPreview(true);
				}
				else {
					if (mEgg) {
						regenEggPreview(false);
					}

					mViewPager.setBackgroundColor(getResources().getColor(R.color.Dialog));
				}
			}

			@Override
			public void onPageSelected(int position) {

			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		tabs.setupWithViewPager(mViewPager);
		tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
		tabs.setTabGravity(TabLayout.GRAVITY_CENTER);

		TypedArray a = getTheme().obtainStyledAttributes(mThemeId, new int[]{
				android.R.attr.colorPrimary,
				android.R.attr.colorAccent,
				R.attr.colorControlActivated
		});

		if (Utils.isLollipop()) {
			mAccentColor = a.getColor(1, 0);
		}
		else {
			mAccentColor = a.getColor(2, 0);
		}

		a.recycle();

		tabs.setSelectedTabIndicatorColor(mAccentColor);
		tabs.setTabTextColors(
				getResources().getColor(R.color.TextSecondaryLight),
				getResources().getColor(R.color.TextPrimaryLight));

		if (mSavedTab >= 0) {
			mViewPager.setCurrentItem(mSavedTab);
		}

		View overlay = findViewById(R.id.overlay);
		if (overlay != null) {
			overlay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mViewPager.getCurrentItem() == TAB_BETA) {
						if (mEgg && mAnimProgress >= mAnimDuration) {
							if (mEggAnimProgress != 0) {
								mEggRotationMultiplier = Math.max(2f, mEggRotationMultiplier + 0.1f);
							} else {
								mEggRotationMultiplier = 1f;
							}
							mEggAnimProgress = 0;
							mHandler.removeCallbacks(mEggUpdateRunnable);
							mHandler.post(mEggUpdateRunnable);
						}
					} else {
						debugTick(1);
					}
				}
			});

			overlay.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (mViewPager.getCurrentItem() != TAB_BETA) {
						debugTick(60);
						return true;
					}
					return false;
				}
			});
		}

		loadWallpaper();
		setColors(mColor1, mColor2, mColor3);

		loadInstalledAppsList();
	}

	private void loadWallpaper() {
		ImageView wallpaper = (ImageView) findViewById(R.id.wallpaper);
		if (wallpaper == null) {
			mWallpaperLoaded = true;
			return;
		}

		mWallpaperLoaded = false;
		showLoading();

		new AsyncTask<Void, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(Void... params) {
				Bitmap bitmap = WallpaperUtils.getWallpaperBitmap(mContext);
				try {
					WallpaperUtils.extractColors(mContext, bitmap);
				}
				catch (Exception e) {
					Log.e(TAG, "Error getting wallpaper palette: " + e.toString());
				}
				return bitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				ImageView wallpaper = (ImageView) findViewById(R.id.wallpaper);
				wallpaper.setImageBitmap(bitmap);
				wallpaper.animate()
						.alpha(1f)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.start();

				mWallpaperLoaded = true;
				hideLoading();
			}
		}.execute();
	}

	public void refreshWallpaperColors() {
		new AsyncTask<Void, Void, Bitmap>() {
			@Override
			protected void onPreExecute() {
				showLoading();
			}

			@Override
			protected Bitmap doInBackground(Void... params) {
				Bitmap bitmap = WallpaperUtils.getWallpaperBitmap(mContext);
				try {
					WallpaperUtils.extractColors(mContext, bitmap);
				}
				catch (Exception e) {
					Log.e(TAG, "Error getting wallpaper palette: " + e.toString());
				}
				return bitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				mWallpaperLoaded = true;
				hideLoading();
			}
		}.execute();
	}

	public void setColors() {
		setColors(mColor1, mColor2, mColor3);
	}

	public int[] getColors() {
		return new int[] { mColor1, mColor2, mColor3, mColorShadow, mColorComplications };
	}

	public void setColors(int color1, int color2, int color3) {
		ConfigColorsFragment fragment = ((ConfigColorsFragment) mAdapter.get(TAB_COLORS));
		if (fragment != null) {
			fragment.setColors(new int[]{color1, color2, color3, mColorShadow, mColorComplications});
		}
	}

	public SharedPreferences loadPreferences() {
        mPreferences = PrefUtils.get(this);

		mFirstRun = mPreferences.getBoolean(PrefUtils.PREF_FIRST_RUN, true);
		if (mFirstRun) {
			PrefUtils.initiate(this, mPreferences);
			mPreferences = PrefUtils.get(this);
		}

		mColor1 = ColorUtils.getColorContainerFromPreference(mPreferences, PrefUtils.PREF_COLOR1, getResources().getColor(R.color.DefaultMain1)).getChosenColor();
		mColor2 = ColorUtils.getColorContainerFromPreference(mPreferences, PrefUtils.PREF_COLOR2, getResources().getColor(R.color.DefaultMain2)).getChosenColor();
		mColor3 = ColorUtils.getColorContainerFromPreference(mPreferences, PrefUtils.PREF_COLOR3, getResources().getColor(R.color.DefaultMain3)).getChosenColor();
		mColorShadow = ColorUtils.getColorContainerFromPreference(mPreferences, PrefUtils.PREF_COLOR_SHADOW, getResources().getColor(R.color.DefaultShadow)).getChosenColor();
		mColorComplications = ColorUtils.getColorContainerFromPreference(mPreferences, PrefUtils.PREF_COLOR_COMPLICATIONS, getResources().getColor(R.color.DefaultComplications)).getChosenColor();

		if (mAdapter != null) {
			mAdapter.notifyPreferencesChanged(mPreferences);
		}

//        printPreferences(mPreferences);
        mFirstRun = false;

		return mPreferences;
	}

    private void printPreferences(SharedPreferences sp) {
        String output = "";
        Map<String, ?> prefs = sp.getAll();
        Set<String> keys = prefs.keySet();
        for (String key : keys) {
            output = output + key + ":" + prefs.get(key).toString() + "\n";
        }

        Log.i(TAG, "\nPreferences:\n" + output);
    }

	public int getAccentColor() {
		return mAccentColor;
	}

	@SuppressWarnings("NewApi")
	private void regenEggPreview(boolean toEgg) {
		if (mClock1 == null || mClock2 == null) {
			return;
		}

		FormClockView[] clocks = getClocks();
		final FormClockView oldClock = clocks[0];
		final FormClockView newClock = clocks[1];

		if (toEgg) {
			mEgg = true;
			newClock.regenEgg();

			AnimationUtils.createCircularHide(oldClock, AnimationUtils.LEFT);
			newClock.postDelayed(new Runnable() {
				@Override
				public void run() {
					mAnimProgress = 0;
					String chicken = "#";
					mAnimDuration = newClock.setTimeAndProgress(chicken, chicken, 0);
					oldClock.setTimeAndProgress(chicken, chicken, 0);

					AnimationUtils.createCircularReveal(newClock, AnimationUtils.RIGHT);
					if (Utils.isLollipop()) {
						newClock.setElevation(BOTTOM_CLOCK_ELEVATION);
					}
					oldClock.regenEgg();
					newClock.postDelayed(new Runnable() {
						@Override
						public void run() {
							mHandler.post(mPreviewUpdateRunnable);
						}
					}, AnimationUtils.ANIMATION_DURATION);
				}
			}, AnimationUtils.ANIMATION_DURATION);
		}
		else {
			mEgg = false;
			regenClockPreview(true);
			newClock.postDelayed(new Runnable() {
				@Override
				public void run() {
					tick();
				}
			}, AnimationUtils.ANIMATION_DURATION);
		}
	}

	@SuppressWarnings("NewApi")
    public void regenClockPreview(boolean toggle) {
		if (mClock1 == null || mClock2 == null) {
			return;
		}

		if (toggle) {
			FormClockView[] clocks = getClocks();
			final FormClockView oldClock = clocks[0];
			final FormClockView newClock = clocks[1];

			newClock.regen(mPreferences);

			if (Utils.isLollipop()) {
				newClock.setElevation(TOP_CLOCK_ELEVATION);
			}

			if (oldClock.isSameSize(newClock)) {
				// Reveal new clock on top of old clock
				AnimationUtils.createCircularReveal(newClock, AnimationUtils.BOTTOM);
				newClock.postDelayed(new Runnable() {
					@Override
					public void run() {
						oldClock.setVisibility(View.INVISIBLE);
						oldClock.regen(mPreferences);
						if (Utils.isLollipop()) {
							newClock.setElevation(BOTTOM_CLOCK_ELEVATION);
						}
					}
				}, AnimationUtils.ANIMATION_DURATION);
			}
			else {
				// Hide old clock then reveal new clock
				AnimationUtils.createCircularHide(oldClock, AnimationUtils.LEFT);
				newClock.postDelayed(new Runnable() {
					@Override
					public void run() {
						AnimationUtils.createCircularReveal(newClock, AnimationUtils.RIGHT);
						if (Utils.isLollipop()) {
							newClock.setElevation(BOTTOM_CLOCK_ELEVATION);
						}
						oldClock.regen(mPreferences);
					}
				}, AnimationUtils.ANIMATION_DURATION);
			}
		}
		else {
			mClock1.regen(mPreferences);
			mClock2.regen(mPreferences);
		}
	}

	private FormClockView[] getClocks() {
		if (mClock1.getVisibility() == View.VISIBLE) {
			return new FormClockView[] { mClock1, mClock2 };
		}
		else {
            mClock2.setVisibility(View.VISIBLE);
			return new FormClockView[] { mClock2, mClock1 };
		}
	}

	// We handle clock animation progress from here (instead of leaving it to the views themselves)
	// to make sure that both views stay in sync
	public void tick() {
		if (mEgg) {
			return;
		}
		if (mClock1 == null || mClock2 == null) {
			return;
		}

		mCalendar = Calendar.getInstance();
		mAnimDuration = mClock1.getAnimDuration(mCalendar);
		mClock2.getAnimDuration(mCalendar);

		mAnimProgress = 0;
		mHandler.post(mPreviewUpdateRunnable);
	}

	public void debugTick(int intervalMinutes) {
		if (mEgg) {
			return;
		}
		if (mClock1 == null || mClock2 == null) {
			return;
		}

		mCalendar.add(Calendar.MINUTE, intervalMinutes);
		mAnimDuration = mClock1.getAnimDuration(mCalendar);
		mClock2.getAnimDuration(mCalendar);

		mAnimProgress = 0;
		mHandler.post(mPreviewUpdateRunnable);
	}

	@Override
	public void onPause() {
		super.onPause();
		mPreferences.unregisterOnSharedPreferenceChangeListener(this);
		Intent intent = new Intent(WidgetUpdateService.UPDATE_COLORS);
		sendBroadcast(intent);

		unregisterReceiver(mUpdateReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		mWallpaperLoaded = false;
		loadWallpaper();

		registerReceiver(mUpdateReceiver, INTENT_FILTER);
	}

	@Override
	public void onSaveInstanceState(Bundle stateOut) {
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		stateOut.putInt(STATE_TAB, viewPager.getCurrentItem());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (mFirstRun) {
			// Initiation is not yet complete so don't mess with anything.
			return;
		}

		if (key.equals(PrefUtils.PREF_COLOR_BIAS)) {
			Log.d(TAG, "Color bias changed: " + sharedPreferences.getInt(key, WallpaperUtils.PALETTE_DEFAULT));
			refreshWallpaperColors();
			return;
		}
		loadPreferences();
		setColors();
		regenClockPreview(true);
        Log.d(TAG, "Updated preferences");
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(getString(R.string.fragtag_colorpicker));
		if (fragment != null) {
			if (((ColorPickerFragment) fragment).onBackPressed()) {
				return;
			}
		}
		fragment = fm.findFragmentByTag(getString(R.string.fragtag_listpreference));
		if (fragment != null) {
			((AnimatedPopupFragment) fragment).close();
			return;
		}
		fragment = fm.findFragmentByTag(getString(R.string.fragtag_changelog));
		if (fragment != null) {
			((ChangelogFragment) fragment).close();
			return;
		}

		super.onBackPressed();
	}

	public void showLoading() {
		View progressBar = findViewById(R.id.image_loading_progress_bar);
		if (progressBar != null) {
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	public void hideLoading() {
		if (mAppListLoaded && mWallpaperLoaded) {
			View progressBar = findViewById(R.id.image_loading_progress_bar);
			if (progressBar != null) {
				progressBar.setVisibility(View.GONE);
			}
		}
	}

	private void loadInstalledAppsList() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute () {
				showLoading();
			}

			@Override
			protected Void doInBackground (Void...params){
				// Load shortcut list
				try {
					ArrayList<AppContainer> subscriptions = DbHelper.getInstance(mContext).getShortcuts();
					mInstalledApps = new ArrayList<>();

					PackageManager pm = getPackageManager();
					Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
					mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

					List<ResolveInfo> launchables = pm.queryIntentActivities(mainIntent, 0);

					for (ResolveInfo info : launchables) {
						String niceName = (String) info.activityInfo.loadLabel(pm);
						String packageName = info.activityInfo.packageName;
						String activityName = info.activityInfo.name;

						if (!niceName.equals("")) {
							AppContainer app = new AppContainer();

							app.setFriendlyName(niceName);
							app.setPackageName(packageName);
							app.setActivityName(activityName);

							if (subscriptions.contains(app)) {
								app.setChecked(true);
							}

							mInstalledApps.add(app);
						}
					}

					Collections.sort(mInstalledApps, new AppContainer());
				}
				catch (Exception e) {
					Log.e(TAG, "Error loading packages: " + e.toString());
				}
				return null;
			}

			@Override
			protected void onPostExecute (Void aVoid){
                mAppListLoaded = true;
				hideLoading();
			}
		}.execute();
	}

	public ArrayList<AppContainer> getInstalledApps() {
		return mInstalledApps;
	}

	private class ViewPagerAdapter extends FragmentPagerAdapter {
		private String[] mTitles;
		private ConfigBaseFragment[] mFragments;
		private final int TAB_COUNT = 5;

		public ViewPagerAdapter() {
			super(getSupportFragmentManager());
			mTitles = getResources().getStringArray(R.array.tab_titles);
			mFragments = new ConfigBaseFragment[TAB_COUNT];
		}

		@Override
		public Fragment getItem(int position) {
			ConfigBaseFragment fragment;

			switch (position) {
				case TAB_LAYOUT:
					fragment = ConfigLayoutFragment.newInstance();
					mFragments[TAB_LAYOUT] = fragment;
					break;
				case TAB_COLORS:
					fragment = ConfigColorsFragment.newInstance();
					mFragments[TAB_COLORS] = fragment;
					break;
				case TAB_FORMAT:
					fragment = ConfigFormatFragment.newInstance();
					mFragments[TAB_FORMAT] = fragment;
					break;
				case TAB_OTHER:
					fragment = ConfigOtherFragment.newInstance();
					mFragments[TAB_OTHER] = fragment;
					break;
//				case TAB_BETA:
//					fragment = ConfigBetaFragment.newInstance();
//					mFragments[TAB_BETA] = fragment;
//					break;
                case TAB_FEEDBACK:
                    fragment = com.beatonma.formclockwidget.app.ConfigFeedbackFragment.newInstance();
                    mFragments[TAB_FEEDBACK] = fragment;
                    break;
				default:
					fragment = null;
			}

			return fragment;
		}

		@Override
		public int getCount() {
			return TAB_COUNT;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mTitles[position];
		}

		public ConfigBaseFragment get(int position) {
			if (mFragments == null) {
				return null;
			}

			return mFragments[position];
		}

		public void notifyPreferencesChanged(SharedPreferences preferences) {
			if (mFragments == null) {
				return;
			}

			for (ConfigBaseFragment f : mFragments) {
				f.notifyPrefencesUpdated(preferences);
			}
		}
	}
}