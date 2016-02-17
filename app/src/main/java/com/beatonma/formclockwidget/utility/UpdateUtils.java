package com.beatonma.formclockwidget.utility;

import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Created by Michael on 13/11/2015.
 */
public class UpdateUtils {
	private final static String TAG = "UpdateUtils";

	private final static int TYPE_EVERY_MINUTE = 0;
	private final static int TYPE_EVERY_HOUR = 1;
	private final static int TYPE_EVERY_DAY = 2;
	private final static int TYPE_MANUAL_ONLY = 3;

	private final static long INTERVAL_HOUR = 1000 * 60 * 60;

	// Only using 23 hours to allow some space for any timing inaccuracies
	private final static long INTERVAL_DAY = 1000 * 60 * 60 * 23;

	public final static String LAST_UPDATE = "last_update";

	// Return whether an update is allowed to occur based on user's preferences
	public static boolean allowUpdate(SharedPreferences preferences) {
		int type = preferences.getInt(PrefUtils.PREF_UPDATE_INTERVAL, 1);
		long lastUpdate = preferences.getLong(LAST_UPDATE, -1);

		if (lastUpdate < 0) {
			// last_update not yet populated so this is the first run
			return true;
		}

		switch (type) {
			case TYPE_EVERY_MINUTE:
				return true;
			case TYPE_EVERY_HOUR:
				Calendar calendar = Calendar.getInstance();
				return (calendar.get(Calendar.MINUTE) == 0);
			case TYPE_EVERY_DAY:
				return System.currentTimeMillis() - lastUpdate > INTERVAL_DAY;
			case TYPE_MANUAL_ONLY:
				return false;
			default:
				return true;
		}
	}
}
