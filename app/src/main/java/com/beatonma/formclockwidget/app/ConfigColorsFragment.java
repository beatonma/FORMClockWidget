package com.beatonma.formclockwidget.app;

import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.app.ui.ColorPreference;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;

/**
 * Created by Michael on 06/11/2015.
 */
public class ConfigColorsFragment extends ConfigBaseFragment {
	private final static String TAG = "ConfigColorsFragment";

	private ColorPreference mColor1;
	private ColorPreference mColor2;
	private ColorPreference mColor3;

	private ColorPreference mColorShadow;
	private ColorPreference mColorComplications;

	private View mHelpColors;

	public static ConfigColorsFragment newInstance() {
		return new ConfigColorsFragment();
	}

	@Override
	protected int getLayout() {
		return R.layout.fragment_config_colors;
	}

	@Override
	protected void init(View v) {
		mColor1 = (ColorPreference) v.findViewById(R.id.button_color1);
		mColor2 = (ColorPreference) v.findViewById(R.id.button_color2);
		mColor3 = (ColorPreference) v.findViewById(R.id.button_color3);

		mColorShadow = (ColorPreference) v.findViewById(R.id.button_color_shadow);
		mColorComplications = (ColorPreference) v.findViewById(R.id.button_color_complications);

		if (getActivity() instanceof ConfigActivity) {
			setColors(((ConfigActivity) getActivity()).getColors());
		}

		View container = v.findViewById(R.id.top_level_container);
		container.post(new Runnable() {
			@Override
			public void run() {
				float leftMargin = mColor1.getX();
				mColorShadow.animate()
						.x(leftMargin)
						.setDuration(AnimationUtils.ANIMATION_DURATION)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.start();
				mColorComplications.animate()
						.x(leftMargin)
						.setDuration(AnimationUtils.ANIMATION_DURATION)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.start();
			}
		});


		mHelpColors = v.findViewById(R.id.help_color_locking);
		if (PrefUtils.get(getActivity()).getBoolean(PrefUtils.PREF_SHOW_HELP, true)) {
			Button helpDismiss = (Button) v.findViewById(R.id.button_dismiss);
			helpDismiss.setTextColor(mAccentColor);
			helpDismiss.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AnimationUtils.hidePreference(mHelpColors, mHelpColors.getMeasuredHeight());
					PrefUtils.get(getActivity())
							.edit()
							.putBoolean(PrefUtils.PREF_SHOW_HELP, false)
							.apply();
				}
			});

			Button helpChangelog = (Button) v.findViewById(R.id.button_changelog);
			helpChangelog.setTextColor(mAccentColor);
			helpChangelog.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String tag = getString(R.string.fragtag_floating);
					FragmentManager fm = getActivity().getSupportFragmentManager();
					fm.beginTransaction()
							.add(R.id.top_level_container, ChangelogFragment.newInstance(), tag)
							.addToBackStack(tag)
							.commit();
				}
			});
		}
		else {
			mHelpColors.setVisibility(View.GONE);
		}
	}

	public void setColors(int[] colors) {
		if (colors.length != 5) {
			Log.e(TAG, "Call to setColors(int[]) failed - array is wrong length!");
			return;
		}

		mColor1.setColor(colors[0]);
		mColor2.setColor(colors[1]);
		mColor3.setColor(colors[2]);

		mColorShadow.setColor(colors[3]);
		mColorComplications.setColor(colors[4]);
	}
}
