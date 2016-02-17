package com.beatonma.formclockwidget.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 06/11/2015.
 */
public abstract class ConfigBaseFragment extends Fragment {
	protected final static String TAG = "ConfigBaseFragment";
	protected int mAccentColor;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View v = inflater.inflate(getLayout(), parent, false);

		if (getActivity() instanceof ConfigActivity) {
			mAccentColor = ((ConfigActivity) getActivity()).getAccentColor();
		}
		else {
			mAccentColor = getResources().getColor(R.color.Accent);
		}

		init(v);

		return v;
	}

	abstract int getLayout();
	abstract void init(View v);

	public void notifyPrefencesUpdated(SharedPreferences preferences) {

	}
}
