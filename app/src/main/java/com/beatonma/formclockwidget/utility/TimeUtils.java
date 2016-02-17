package com.beatonma.formclockwidget.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * Created by Michael on 06/01/2016.
 */
public class TimeUtils {
    public final static int FORMAT_SYSTEM = 0;
    public final static int FORMAT_12_HOUR = 1;
    public final static int FORMAT_24_HOUR = 2;

    public static boolean getFormat(Context context, SharedPreferences preferences) {
        int format = preferences.getInt(PrefUtils.PREF_FORMAT_TIME, FORMAT_SYSTEM);

        switch (format) {
            case FORMAT_SYSTEM:
                return DateFormat.is24HourFormat(context);
            case FORMAT_12_HOUR:
                return false;
            case FORMAT_24_HOUR:
                return true;
            default:
                return DateFormat.is24HourFormat(context);
        }
    }
}
