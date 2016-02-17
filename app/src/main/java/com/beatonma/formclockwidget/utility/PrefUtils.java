package com.beatonma.formclockwidget.utility;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.data.AppContainer;
import com.beatonma.formclockwidget.data.AppInfoContainer;
import com.beatonma.formclockwidget.data.DbHelper;
import com.beatonma.formclockwidget.formclock.FormClockRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Michael on 03/06/2015.
 *
 * Convenience class to minimise need to look up preference names
 */
public class PrefUtils {
	private final static String TAG = "PrefUtils";

	public static boolean DEBUG = false;

	public final static String PREFS = "widget_preferences";
	public final static String PREFS_DREAM = "dream_preferences";

	// Track if the app has been opened before
	public final static String PREF_FIRST_RUN = "app_first_run_v2.0";

	// Never show help again once user has dismissed it
	public final static String PREF_SHOW_HELP = "show_help";

	// Theme
	public final static String PREF_USE_WALLPAPER_PALETTE = "pref_use_wallpaper_palette";

	public final static String PREF_THEME_SHADOW = "pref_theme_show_shadows";
	public final static String PREF_THEME_ORIENTATION = "pref_theme_orientation";

	public final static String PREF_COLOR1 = "pref_color1";
	public final static String PREF_COLOR2 = "pref_color2";
	public final static String PREF_COLOR3 = "pref_color3";

	public final static String PREF_COLOR_BIAS = "pref_color_bias";

	public final static String PREF_COLOR_COMPLICATIONS = "pref_color_complications";
	public final static String PREF_COLOR_SHADOW = "pref_color_shadow";

	public final static String WALLPAPER_COLORS_UPDATED = "wallpaper_colors_updated";
	public final static String WALLPAPER_COLOR1 = "wallpaper_color1";
	public final static String WALLPAPER_COLOR2 = "wallpaper_color2";
	public final static String WALLPAPER_COLOR3 = "wallpaper_color3";

	// Complications
	public final static String PREF_SHOW_DATE = "pref_complication_date";
	public final static String PREF_DATE_FORMAT = "pref_date_format";
	public final static String PREF_DATE_FORMAT_CUSTOM = "pref_date_format_custom";
	public final static String PREF_DATE_UPPERCASE = "pref_date_uppercase";
	public final static String PREF_SHOW_ALARM = "pref_complication_alarm";
	public final static String PREF_ON_TOUCH = "pref_on_touch";

	// Formatting
	public final static String PREF_FORMAT_TIME = "pref_format_time";
	public final static String PREF_FORMAT_ZERO_PADDING = "pref_format_zero_padding";

	// Other
	public final static String PREF_ENABLE_ANIMATION = "pref_enable_animation";
	public final static String PREF_ON_TOUCH_PACKAGE = "pref_on_touch_package";
	public final static String PREF_ON_TOUCH_LAUNCHER = "pref_on_touch_launcher";

	public final static String PREF_ON_TOUCH_SHOW_ACTIVITY = "pref_on_touch_show_activity";

	public final static String MAX_WIDGET_WIDTH = "max_widget_width";
	public final static String MAX_WIDGET_HEIGHT = "max_widget_height";
	public final static String MIN_WIDGET_WIDTH = "min_widget_width";
	public final static String MIN_WIDGET_HEIGHT = "min_widget_height";

	// Track color updates
	public final static String PREF_UPDATE_INTERVAL = "pref_update_interval";

	public static SharedPreferences get(Context context) {
		return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	public static SharedPreferences getDream(Context context) {
		return context.getSharedPreferences(PREFS_DREAM, Context.MODE_PRIVATE);
	}

	/**
	 * Version 2.x uses some different preferences and preference formats from version 1.x.
	 * Here we update old preferences to work with the new version, or initiate values
	 * if they don't already exist. This should only run once.
	 * @return Valid, up-to-date preferences.
	 */
	@SuppressLint("CommitPrefEdits")
	public static SharedPreferences initiate(Context context, SharedPreferences preferences) {
		SharedPreferences.Editor editor = preferences.edit();

		// Update colors from old format and set lock depending on the now-deprecated USE_WALLPAPER_PALETTE setting.
		boolean lockColors = !preferences.getBoolean(PREF_USE_WALLPAPER_PALETTE, false);

		ColorUtils.updateColorFormat(preferences, PREF_COLOR1, context.getResources().getColor(R.color.DefaultMain1), lockColors);
		ColorUtils.updateColorFormat(preferences, PREF_COLOR2, context.getResources().getColor(R.color.DefaultMain2), lockColors);
		ColorUtils.updateColorFormat(preferences, PREF_COLOR3, context.getResources().getColor(R.color.DefaultMain3), lockColors);

		// Shadow and complication colors should be locked by default. These options didn't exist in older versions
		ColorUtils.updateColorFormat(preferences, PREF_COLOR_SHADOW, context.getResources().getColor(R.color.DefaultShadow), true);
		ColorUtils.updateColorFormat(preferences, PREF_COLOR_COMPLICATIONS, context.getResources().getColor(R.color.DefaultComplications), true);

		// Orientation format has changed from a string to an int. Not sure why it was every stored as a string tbh. Here we fix that nonsense, if necessary.
		int orientation = FormClockRenderer.ORIENTATION_HORIZONTAL;
		try {
			orientation = Integer.valueOf(preferences.getString(PREF_THEME_ORIENTATION, String.valueOf(FormClockRenderer.ORIENTATION_HORIZONTAL)));
					editor.putInt(PREF_THEME_ORIENTATION, orientation);
			Log.d(TAG, "Successfully updated orientation format from string to int");
		}
		catch (Exception e) {
			Log.e(TAG, "Orientation format is already an integer. No problems here!");
		}

		// Convert old 1.x touch shortcut to new format.
		String touchActivity = preferences.getString(PREF_ON_TOUCH_LAUNCHER, "");
		String touchPackage = preferences.getString(PREF_ON_TOUCH_PACKAGE, "");
		if (!touchPackage.equals("")) {
			// Update package structure if necessary
			if (touchActivity.equals("com.beatonma.formclockwidget.ConfigActivity")) {
				touchActivity = "com.beatonma.formclockwidget.app.ConfigActivity";
			}

			AppContainer container = new AppContainer();
			container.setPackageName(touchPackage);
			container.setActivityName(touchActivity);
			container.setChecked(true);

			DbHelper.getInstance(context).updateSingleShortcut(container);
			editor
					.putBoolean(PREF_ON_TOUCH_SHOW_ACTIVITY, false);

			Log.d(TAG, "Updated v1.x shortcut to new format");
		}

		// Convert beta touch shortcut to new format
		Set<String> betaShortcuts = preferences.getStringSet(PREF_ON_TOUCH, new HashSet<String>());
		if (!betaShortcuts.isEmpty()) {
			ArrayList<AppContainer> newShortcuts = new ArrayList<>();
			for (String s : betaShortcuts) {
				AppInfoContainer old = new AppInfoContainer(s);
				newShortcuts.add(new AppContainer(old));
			}
			DbHelper.getInstance(context).updateShortcuts(newShortcuts);

            editor
					.putBoolean(PREF_ON_TOUCH_SHOW_ACTIVITY, newShortcuts.size() > 1);

			Log.d(TAG, String.format("Updated %d shortcuts from beta format.", betaShortcuts.size()));
		}

		if (DbHelper.getInstance(context).getShortcuts().isEmpty()) {
			AppContainer defaultApp = new AppContainer(AppContainer.TYPE_DEFAULT);
			DbHelper.getInstance(context).updateSingleShortcut(defaultApp);
		}

		boolean showOutline = preferences.getBoolean(PREF_THEME_SHADOW, true);

		editor
				.putInt(PREF_UPDATE_INTERVAL, 1)						// Hourly color updates by default
				.putBoolean(PREF_THEME_SHADOW, showOutline)			// Show outline by default
				.putBoolean(PREF_DATE_UPPERCASE, true)				// Uppercase by default
				.putInt(PREF_FORMAT_TIME, TimeUtils.FORMAT_SYSTEM)	// Follow system time formatting by default
                .putBoolean(PREF_FORMAT_ZERO_PADDING,
                        orientation == FormClockRenderer.ORIENTATION_VERTICAL)	// Pad single digit numbers with a leading zero
				.putBoolean(PREF_FIRST_RUN, false);					// Remember that initial setup has been completed

		editor.commit();

		return preferences;
	}

	// Return true if all colors are locked, this means we don't have to update wallpaper colors now
	public static boolean allColorsLocked(SharedPreferences sp) {
		return ColorUtils.getColorContainerFromPreference(sp, PREF_COLOR1).isLocked()
				&& ColorUtils.getColorContainerFromPreference(sp, PREF_COLOR2).isLocked()
				&& ColorUtils.getColorContainerFromPreference(sp, PREF_COLOR3).isLocked()
				&& ColorUtils.getColorContainerFromPreference(sp, PREF_COLOR_SHADOW).isLocked()
				&& ColorUtils.getColorContainerFromPreference(sp, PREF_COLOR_COMPLICATIONS).isLocked();
	}
}
