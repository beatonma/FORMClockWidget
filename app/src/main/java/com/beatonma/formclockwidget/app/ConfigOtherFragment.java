package com.beatonma.formclockwidget.app;

import android.view.View;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 06/11/2015.
 */
public class ConfigOtherFragment extends ConfigBaseFragment {
	public static ConfigOtherFragment newInstance() {
		return new ConfigOtherFragment();
	}

	@Override
	protected int getLayout() {
		return R.layout.fragment_config_other;
	}

	@Override
	protected void init(View v) {

	}
}
