package com.beatonma.formclockwidget.app;

import android.content.SharedPreferences;
import android.view.View;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 06/11/2015.
 */
public class ConfigLayoutFragment extends ConfigBaseFragment {

	public static ConfigLayoutFragment newInstance() {
		return new ConfigLayoutFragment();
	}

	@Override
	protected int getLayout() {
		return R.layout.fragment_config_layout;
	}

	@Override
	protected void init(View v) {

	}

	@Override
	public void notifyPrefencesUpdated(SharedPreferences preferences) {

	}
}
