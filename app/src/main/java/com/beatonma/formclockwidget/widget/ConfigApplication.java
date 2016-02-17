package com.beatonma.formclockwidget.widget;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;

import com.beatonma.formclockwidget.WidgetProvider;


/**
 * Created by Michael on 29/10/2015.
 */
public class ConfigApplication extends Application {

	/**
	 * Let the widgets know when the device rotates so they can re-render accordingly.
	 * @param newConfig
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Intent intent = new Intent(WidgetProvider.UPDATE, null, this, WidgetProvider.class);
		sendBroadcast(intent);
	}
}
