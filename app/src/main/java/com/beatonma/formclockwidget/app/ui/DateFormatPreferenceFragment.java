package com.beatonma.formclockwidget.app.ui;

import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.DateUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.Utils;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Michael on 12/11/2015.
 */
public class DateFormatPreferenceFragment extends ListPreferenceFragment {
	private final static String TAG = "DateFormatPref";

	private final static String CUSTOM_FORMAT = "date_format_custom";
//	private final static String ALLOWED_SYMBOLS = "[ emdy!`'*%#@~|?:;()\\.,_/-]+";
	private final static String ALLOWED_FORMAT_SYMBOLS = "[defglmwcy]";

	private View mHelpView;
	private TextView mFormatPreview;
	private boolean mHelpVisible = false;

	private Calendar mCalendar;

	private String mCustomFormat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			mKey = args.getString(KEY);
			mAccentColor = args.getInt(ACCENT_COLOR);
			mEntries = args.getStringArray(LIST_ENTRIES);
			mSelectedPosition = args.getInt(SELECTED_POSITION);
			mCustomFormat = args.getString(CUSTOM_FORMAT);
		}

		mViewHeight = Utils.dpToPx(getContext(), 52);
	}

	public static DateFormatPreferenceFragment newInstance(Builder builder) {
		Bundle args = new Bundle();
		args.putString(KEY, builder.key);
		args.putInt(ACCENT_COLOR, builder.accentColor);
		args.putStringArray(LIST_ENTRIES, builder.items);
		args.putInt(SELECTED_POSITION, builder.selectedValue);
		args.putString(CUSTOM_FORMAT, builder.customFormat);
		args.putFloat(POSITION_X, -1);
		args.putFloat(POSITION_Y, -1);

		DateFormatPreferenceFragment fragment = new DateFormatPreferenceFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static class Builder {
		String key;
		int accentColor;
		String[] items;
		int selectedValue;
		String customFormat;

		public Builder() {

		}

		public Builder setKey(String key) {
			this.key = key;
			return this;
		}

		public Builder setAccentColor(int accent) {
			this.accentColor = accent;
			return this;
		}

		public Builder setItems(String[] items) {
			this.items = items;
			return this;
		}

		public Builder setCustomFormat(String format) {
			this.customFormat = format;
			return this;
		}

		public Builder setDefaultValue(int selectedValue) {
			this.selectedValue = selectedValue;
			return this;
		}

		public DateFormatPreferenceFragment build() {
			return DateFormatPreferenceFragment.newInstance(this);
		}
	}

	@Override
	public void close() {
		if (mHelpVisible) {
			AnimationUtils.slideOutUp(mHelpView);
		}
		super.close();
	}

	@Override
	protected int getLayout() {
		return R.layout.view_preference_list_customdate;
	}

	@Override
	protected void initLayout(View v) {
		mCalendar = Calendar.getInstance();

		mHelpView = v.findViewById(R.id.formatting_help);
		mHelpView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AnimationUtils.slideOutUp(mHelpView);
				mHelpVisible = false;
			}
		});

		mFormatPreview = (TextView) v.findViewById(R.id.help_date_formatting_preview);
		mFormatPreview.setTextColor(mAccentColor);
		mFormatPreview.setText(DateFormat.format(mCustomFormat, mCalendar));

		TextView examplesUnformatted = (TextView) v.findViewById(R.id.help_date_formatting_examples_unformatted);
		examplesUnformatted.setText(
				DateUtils.EXAMPLE_FORMAT_1 + "\n"
						+ DateUtils.EXAMPLE_FORMAT_2 + "\n"
						+ DateUtils.EXAMPLE_FORMAT_3 + "\n"
						+ DateUtils.EXAMPLE_FORMAT_4);
		TextView examplesFormatted = (TextView) v.findViewById(R.id.help_date_formatting_examples_formatted);
		examplesFormatted.setText(
				DateFormat.format(DateUtils.EXAMPLE_FORMAT_1, mCalendar) + "\n"
						+ DateFormat.format(DateUtils.EXAMPLE_FORMAT_2, mCalendar) + "\n"
						+ DateFormat.format(DateUtils.EXAMPLE_FORMAT_3, mCalendar) + "\n"
						+ DateFormat.format(DateUtils.EXAMPLE_FORMAT_4, mCalendar));

		mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerview);

		LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		mRecyclerView.setLayoutManager(lm);

		mAdapter = new DateFormatPreferenceAdapter();
		mRecyclerView.setAdapter(mAdapter);

		mOkButton = (Button) v.findViewById(R.id.button);
		mOkButton.setTextColor(mAccentColor);
		mOkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PrefUtils.get(getActivity())
						.edit()
						.putInt(mKey, mSelectedPosition)
						.putString(PrefUtils.PREF_DATE_FORMAT_CUSTOM, mCustomFormat)
						.apply();
				close();
			}
		});

		update();
	}

	@Override
	protected void update() {
		if (mEntries != null) {
			updateLayout();

			ArrayList<PreferenceItem> dataset = new ArrayList<>();
			for (String s : mEntries) {
				dataset.add(new PreferenceItem(s));
			}

			mAdapter.setDataset(dataset);
		}
	}

	@Override
	protected void updateLayout() {
		ViewGroup.LayoutParams lp = mRecyclerView.getLayoutParams();
		lp.height = (int) ((mEntries.length * 1.1) * mViewHeight);
		mRecyclerView.setLayoutParams(lp);
		mRecyclerView.requestLayout();

		lp = mCard.getLayoutParams();
		lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		mCard.requestLayout();
	}

	// Parse user input and make sure it is usable as a date format
	private String formatDateString(String date) {
		String formatted = "";

		for (int i = 0; i < date.length(); i++) {
			char character = date.charAt(i);
            if (character == '\'' || character == ' ') {
//                formatted = formatted + "'";
				formatted = formatted + character;

                continue;
            }

			String left = date.substring(0, i);
			int n = countChar(left, '\'');
			if (n % 2 == 0) {
				String s = ("" + character).toLowerCase();
				if (s.matches(ALLOWED_FORMAT_SYMBOLS)) {
					s = s.replace("e", "E").replace("m", "M");
                    formatted = formatted + s;
				}
			}
            else {
                formatted = formatted + character;
            }
		}

		return formatted;
	}

	private int countChar(String string, char target) {
		int count = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == target) {
				count++;
			}
		}

		return count;
	}

	private class DateFormatPreferenceAdapter extends ListPreferenceAdapter {
		private final static int TYPE_TEXT = 0;
		private final static int TYPE_EDIT = 1;
		private final static String CUSTOM = "__custom__";

		ArrayList<PreferenceItem> mDataset;

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			View v;
			switch (viewType) {
				case TYPE_EDIT:
					v = inflater.inflate(R.layout.view_preference_list_customdate_item_edit, parent, false);
					return new CustomFormatViewHolder(v);
				default:
					v = inflater.inflate(R.layout.view_preference_list_item, parent, false);
					return new ViewHolder(v);

			}
		}

		@Override
		public int getItemCount() {
			if (mDataset == null) {
				return 0;
			}
			else {
				return mDataset.size();
			}
		}

		@Override
		public int getItemViewType(int position) {
			if (mDataset.get(position).mText.equals(CUSTOM)) {
				return TYPE_EDIT;
			}
			else {
				return TYPE_TEXT;
			}
		}

		@Override
		public void setDataset(ArrayList<PreferenceItem> dataset) {
			mDataset = dataset;
			notifyDataSetChanged();
		}

		public class ViewHolder extends ListPreferenceAdapter.ViewHolder {
			AppCompatRadioButton radioButton;
			TextView text;

			public ViewHolder(View v) {
				super(v);

				radioButton = (AppCompatRadioButton) v.findViewById(R.id.radio_button);
				text = (TextView) v.findViewById(R.id.text);
			}

			public void bind(final int position) {
				PreferenceItem container = mDataset.get(position);
				radioButton.setChecked(position == mSelectedPosition);
				text.setText(DateFormat.format(container.mText, mCalendar));

				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						radioButton.setChecked(true);
						radioButton.requestFocus();
					}
				});

				radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							mSelectedPosition = position;
							try {
								for (int i = 0; i < mDataset.size(); i++) {
									if (i != position) {
										((ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i)).radioButton.setChecked(false);
									}
								}
							} catch (Exception e) {
								Log.e(TAG, "Illegal recyclerview state: " + e.toString());
							}
						}
					}
				});
			}
		}

		private class CustomFormatViewHolder extends ViewHolder {
			AppCompatEditText editText;

			public CustomFormatViewHolder(View v) {
				super(v);

				radioButton = (AppCompatRadioButton) v.findViewById(R.id.radio_button);
				editText = (AppCompatEditText) v.findViewById(R.id.edit_text);
			}

			@Override
			public void bind(final int position) {
				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						radioButton.setChecked(true);
					}
				});
				editText.setText(mCustomFormat);
				editText.setHint("Custom format");
				editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							AnimationUtils.slideInDown(mHelpView);
							mHelpVisible = true;
						}
					}
				});
				editText.addTextChangedListener(new TextWatcher() {
					boolean editing = false;

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {

					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {

					}

					@Override
					public void afterTextChanged(Editable s) {
						if (editing) return;
						editing = true;
//						String text = s.toString().toLowerCase();
//						int length = s.length();

//						if (!text.matches(ALLOWED_SYMBOLS) && length > 0) {
//							s.delete(length - 1, length);
//						} else {
//							StringBuilder sb = new StringBuilder();
//							sb.append(text.replace("e", "E").replace("m", "M"));
//							s.replace(0, length, sb);
//						}


						mCustomFormat = formatDateString(s.toString());
						mFormatPreview.setText(DateFormat.format(mCustomFormat, mCalendar));
						editing = false;
					}
				});

				radioButton.setChecked(position == mSelectedPosition);
				radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							mSelectedPosition = position;
							try {
								for (int i = 0; i < mDataset.size(); i++) {
									if (i != position) {
										((ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i)).radioButton.setChecked(false);
									}
								}
							} catch (Exception e) {
								Log.e(TAG, "Illegal recyclerview state: " + e.toString());
							}
						}
					}
				});
			}
		}
	}
}
