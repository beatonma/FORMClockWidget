package com.beatonma.formclockwidget.app.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 08/11/2015.
 */
public class PreferenceSection extends Preference {
	public PreferenceSection(Context context) {
		super(context);
	}

	public PreferenceSection(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PreferenceSection(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public PreferenceSection(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		View v = inflate(context, R.layout.view_preference_section, this);

		mTitle = (TextView) v.findViewById(R.id.title);

		TypedArray a = context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.Preference, 0, 0);

		int ref = a.getResourceId(R.styleable.Preference_name, 0);
		if (ref != 0) {
			mTitle.setText(getResources().getString(ref));
		}

		a.recycle();
	}
}
