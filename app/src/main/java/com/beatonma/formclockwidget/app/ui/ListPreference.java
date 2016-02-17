package com.beatonma.formclockwidget.app.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.app.ConfigActivity;
import com.beatonma.formclockwidget.data.AppContainer;
import com.beatonma.formclockwidget.data.DbHelper;
import com.beatonma.formclockwidget.utility.PrefUtils;

import java.util.ArrayList;

/**
 * Created by Michael on 09/11/2015.
 */
public class ListPreference extends Preference {
	private final static String TAG = "ListPreference";

	private boolean mMultiselect;
	private int mDefaultValue;
	private String[] mEntries;
	private boolean mShowSelected;

	public ListPreference(Context context) {
		super(context);
	}

	public ListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		View v = inflate(context, R.layout.view_preference, this);

		mTitle = (TextView) v.findViewById(R.id.title);
		mDescription = (TextView) v.findViewById(R.id.description);

		TypedArray a = context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes);

		try {
			int ref = a.getResourceId(R.styleable.ListPreference_name, 0);
			if (ref != 0) {
				mTitle.setText(getResources().getString(ref));
			}

			ref = a.getResourceId(R.styleable.ListPreference_description, 0);
			if (ref != 0) {
				mDescription.setText(getResources().getString(ref));
			}

			mKey = a.getString(R.styleable.ListPreference_key);

			ref = a.getResourceId(R.styleable.ListPreference_entries, 0);
			if (ref != 0) {
				mEntries = getResources().getStringArray(ref);
			}

			mDefaultValue = a.getInt(R.styleable.ListPreference_defaultValue, 0);

			mMultiselect = a.getBoolean(R.styleable.ListPreference_multiselect, false);
			mShowSelected = a.getBoolean(R.styleable.ListPreference_showSelected, false);
		}
		finally {
			a.recycle();
		}

		View container = v.findViewById(R.id.top_level_container);
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getContext() instanceof ConfigActivity) {
					int accentColor = ((ConfigActivity) getContext()).getAccentColor();
					String tag = getContext().getString(R.string.fragtag_listpreference);

					if (mKey.equals(PrefUtils.PREF_ON_TOUCH)) {
						showShortcutsFragment(tag, accentColor);
					}
					else if (mKey.equals(PrefUtils.PREF_DATE_FORMAT)) {
						showDateFormatFragment(tag, accentColor);
					}
					else if (mMultiselect) {
						MultiListPreferenceFragment fragment = new MultiListPreferenceFragment.Builder()
								.setKey(mKey)
								.setAccentColor(accentColor)
								.setItems(mEntries)
								.build();

						FragmentManager fm = ((ConfigActivity) getContext()).getSupportFragmentManager();
						fm.beginTransaction()
								.add(R.id.top_level_container, fragment, tag)
								.addToBackStack(tag)
								.commit();
					}
					else {
						int selectedValue = getContext()
								.getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE)
								.getInt(mKey, mDefaultValue);

						ListPreferenceFragment fragment = new ListPreferenceFragment.Builder()
								.setKey(mKey)
								.setAccentColor(accentColor)
								.setItems(mEntries)
								.setDefaultValue(selectedValue)
								.build();

						fragment.setCloseListener(new OnDialogClosedListener() {
							@Override
							public void onItemSelected(int position) {
								setSelectedPosition(position);
							}
						});

						FragmentManager fm = ((ConfigActivity) getContext()).getSupportFragmentManager();
						fm.beginTransaction()
								.add(R.id.top_level_container, fragment, tag)
								.addToBackStack(tag)
								.commit();
					}
				}
			}
		});

		if (mShowSelected) {
			int selectedValue = getContext()
					.getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE)
					.getInt(mKey, mDefaultValue);
			String selectedDescription = mEntries[selectedValue];
			if (!selectedDescription.equals("")) {
				mDescription.setText(selectedDescription);
			}
		}

		cleanUp();
	}

	private void showShortcutsFragment(String tag, int accentColor) {
		AppSelectFragment fragment = AppSelectFragment.newInstance();

		fragment.setEntries(((ConfigActivity) getContext()).getInstalledApps());
		fragment.setAccentColor(accentColor);
		fragment.setOnItemsSavedListener(new AppSelectFragment.OnItemsSavedListener() {
			@Override
			public void onItemsSaved(ArrayList<AppContainer> savedItems) {
				if (getContext() != null) {
					DbHelper.getInstance(getContext()).updateShortcuts(savedItems);

                    // If more than one shortcut is selected then we will need to show the shortcut menu activity
                    PrefUtils.get(getContext()).edit()
                            .putBoolean(PrefUtils.PREF_ON_TOUCH_SHOW_ACTIVITY, savedItems.size() > 1)
                            .commit();
				}
			}
		});

		FragmentManager fm = ((ConfigActivity) getContext()).getSupportFragmentManager();
		fm.beginTransaction()
				.add(R.id.top_level_container, fragment, tag)
				.addToBackStack(tag)
				.commit();
	}

	private void showDateFormatFragment(String tag, int accentColor) {
		DateFormatPreferenceFragment fragment = new DateFormatPreferenceFragment.Builder()
				.setKey(mKey)
				.setAccentColor(accentColor)
				.setItems(getContext().getResources().getStringArray(R.array.date_format_entries))
				.setCustomFormat(PrefUtils.get(getContext()).getString(PrefUtils.PREF_DATE_FORMAT_CUSTOM, ""))
				.setDefaultValue(PrefUtils.get(getContext()).getInt(PrefUtils.PREF_DATE_FORMAT, 0))
				.build();

		FragmentManager fm = ((ConfigActivity) getContext()).getSupportFragmentManager();
		fm.beginTransaction()
				.add(R.id.top_level_container, fragment, tag)
				.addToBackStack(tag)
				.commit();
	}

	public void setSelectedPosition(int position) {
		if (mShowSelected) {
			String selectedDescription = mEntries[position];
			if (!selectedDescription.equals("")) {
				mDescription.setText(selectedDescription);
			}
		}
	}

	public interface OnDialogClosedListener {
		void onItemSelected(int position);
	}
}
