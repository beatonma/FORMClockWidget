package com.beatonma.formclockwidget.app.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 06/11/2015.
 */
public class Preference extends RelativeLayout {
	protected final static String TAG = "Preference";

	protected TextView mTitle;
	protected TextView mDescription;
	protected String mKey = "";

	public Preference(Context context) {
		super(context);
		init(context, null, 0, 0);
	}

	public Preference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0, 0);
	}

	public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs, defStyleAttr, defStyleRes);
	}

	protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		View v = inflate(context, R.layout.view_preference, this);

		mTitle = (TextView) v.findViewById(R.id.title);
		mDescription = (TextView) v.findViewById(R.id.description);

		TypedArray a = context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes);

		try {
			int ref = a.getResourceId(R.styleable.Preference_name, 0);
			if (ref != 0) {
				mTitle.setText(getResources().getString(ref));
			}

			ref = a.getResourceId(R.styleable.Preference_description, 0);
			if (ref != 0) {
				mDescription.setText(getResources().getString(ref));
			}

			mKey = a.getString(R.styleable.Preference_key);
		}
		finally {
			a.recycle();
		}

		cleanUp();
	}


	// Hide empty views
	protected void cleanUp() {
		if (mTitle.getText().equals("")) {
			mTitle.setVisibility(GONE);
		}
		if (mDescription.getText().equals("")) {
			mDescription.setVisibility(GONE);
		}
	}

	public void setTitle(String title) {
		if (mTitle != null) {
			mTitle.setText(title);
		}
	}

	public void setDescription(String description) {
		if (mDescription != null) {
			mDescription.setText(description);
		}
	}

	public void setKey(String key) {
		mKey = key;
	}
}
