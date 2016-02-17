package com.beatonma.formclockwidget.utility;

import android.content.SharedPreferences;

/**
 * Created by Michael on 12/11/2015.
 */
public class DateUtils {
	public final static String EXAMPLE_FORMAT_1 = "d/MM/yyyy";
	public final static String EXAMPLE_FORMAT_2 = "EEEE d MMMM";
	public final static String EXAMPLE_FORMAT_3 = "yy-MMM-d";
	public final static String EXAMPLE_FORMAT_4 = "'It\'\'s' EEEE\'!\'";

	public final static int TYPE_LONG = 0;
	public final static int TYPE_SHORT = 1;
	public final static int TYPE_CUSTOM = 2;

	public final static String LONG = "EEEE d MMMM";
	public final static String SHORT = "EEE d MMM";

	public static String getFormat(SharedPreferences preferences) {
		int type = preferences.getInt(PrefUtils.PREF_DATE_FORMAT, DateUtils.TYPE_LONG);

		switch (type) {
			case TYPE_LONG:
				return LONG;
			case TYPE_SHORT:
				return SHORT;
			case TYPE_CUSTOM:
				return preferences.getString(PrefUtils.PREF_DATE_FORMAT_CUSTOM, LONG);
			default:
				return LONG;
		}
	}
}
