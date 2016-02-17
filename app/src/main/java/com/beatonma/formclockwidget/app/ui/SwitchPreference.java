package com.beatonma.formclockwidget.app.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.app.ConfigActivity;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 06/11/2015.
 */
public class SwitchPreference extends Preference {
	private final static String TAG = "SwitchPreference";

	private SwitchCompat mSwitch;
	private boolean mDefaultIsChecked = false;

	public SwitchPreference(Context context) {
		super(context);
	}

	public SwitchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		View v = inflate(context, R.layout.view_preference_switch, this);

		mTitle = (TextView) v.findViewById(R.id.title);
		mDescription = (TextView) v.findViewById(R.id.description);
		mSwitch = (SwitchCompat) v.findViewById(R.id.switchview);

		TypedArray a = context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.Preference, 0, 0);

		try {
			int ref = a.getResourceId(R.styleable.Preference_name, 0);
			if (ref != 0) {
				mTitle.setText(getResources().getString(ref));
			}

			ref = a.getResourceId(R.styleable.Preference_description, 0);
			if (ref != 0) {
				mDescription.setText(Html.fromHtml(getResources().getString(ref)));
			}

			mKey = a.getString(R.styleable.Preference_key);
			mDefaultIsChecked = a.getBoolean(R.styleable.Preference_checked, false);
		}
		finally {
			a.recycle();
		}

		View container = v.findViewById(R.id.top_level_container);
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSwitch.toggle();
			}
		});

		if (context instanceof ConfigActivity) {
			setColor(((ConfigActivity) context).getAccentColor());
		}

		mSwitch.setChecked(context.getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE)
				.getBoolean(mKey, mDefaultIsChecked));

		mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				getContext().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE)
						.edit()
						.putBoolean(mKey, isChecked)
						.apply();
			}
		});

		cleanUp();
	}

	@SuppressWarnings("NewApi")
	public void setColor(int color) {
		mSwitch.setHighlightColor(color);

		if (Utils.isLollipop()) {
			ColorStateList thumbStates;
			ColorStateList trackStates;
			int grey = getResources().getColor(R.color.PrimaryLight);

			thumbStates = Utils.getSimpleSelector(grey, color);
			trackStates = Utils.getSimpleSelector(ColorUtils.lighten(grey, 0.2f), ColorUtils.lighten(color, 0.25f));

			Drawable d = mSwitch.getThumbDrawable();
			d.setTintList(thumbStates);
			mSwitch.setThumbDrawable(d);

			d = mSwitch.getTrackDrawable();
			d.setTintList(trackStates);
			mSwitch.setTrackDrawable(d);
		}
	}
}