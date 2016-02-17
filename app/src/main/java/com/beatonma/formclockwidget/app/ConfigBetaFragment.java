package com.beatonma.formclockwidget.app;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 13/11/2015.
 */
public class ConfigBetaFragment extends ConfigBaseFragment {
	public static ConfigBetaFragment newInstance() {
		return new ConfigBetaFragment();
	}

	@Override
	int getLayout() {
		return R.layout.fragment_config_beta;
	}

	@Override
	void init(View v) {
		Button email = (Button) v.findViewById(R.id.button_email);
		email.setTextColor(mAccentColor);
		email.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
							"mailto", getString(R.string.contact_email), null));
					emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_email_subject));
					emailIntent.putExtra(Intent.EXTRA_TEXT, "");
					startActivity(emailIntent);
				}
				catch (Exception e) {
					Log.e(TAG, "Error starting email activity");
				}
			}
		});
		Button community = (Button) v.findViewById(R.id.button_community);
		community.setTextColor(mAccentColor);
		community.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(getString(R.string.contact_community)));
					startActivity(intent);
				}
				catch (Exception e) {
					Log.e(TAG, "Error opening community website");
				}
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
}
