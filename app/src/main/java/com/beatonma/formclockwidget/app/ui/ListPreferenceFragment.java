package com.beatonma.formclockwidget.app.ui;

import android.os.Bundle;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.PrefUtils;

import java.util.ArrayList;

/**
 * Created by Michael on 09/11/2015.
 */
public class ListPreferenceFragment extends AnimatedPopupFragment {
	protected final static String KEY = "key";
	protected final static String ACCENT_COLOR = "accent_color";
	protected final static String LIST_ENTRIES = "list_entries";
	protected final static String SELECTED_POSITION = "selected_position";

	protected int mViewHeight;

	protected RecyclerView mRecyclerView;
	protected ListPreferenceAdapter mAdapter;

	protected Button mOkButton;

	protected int mSelectedPosition = 0;

	protected String mKey;
	protected int mAccentColor;
	protected String[] mEntries;

	protected ListPreference.OnDialogClosedListener mCloseListener;

//	private boolean mClicked = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			mKey = args.getString(KEY);
			mAccentColor = args.getInt(ACCENT_COLOR);
			mEntries = args.getStringArray(LIST_ENTRIES);
			mSelectedPosition = args.getInt(SELECTED_POSITION);
		}

//		mViewHeight = Utils.dpToPx(getContext(), 48);
		mViewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
	}

	public static ListPreferenceFragment newInstance(Builder builder) {
		Bundle args = new Bundle();
		args.putString(KEY, builder.key);
		args.putInt(ACCENT_COLOR, builder.accentColor);
		args.putStringArray(LIST_ENTRIES, builder.items);
		args.putInt(SELECTED_POSITION, builder.selectedValue);
		args.putFloat(POSITION_X, -1);
		args.putFloat(POSITION_Y, -1);

		ListPreferenceFragment fragment = new ListPreferenceFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static class Builder {
		String key;
		int accentColor;
		String[] items;
		int selectedValue;

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

		public Builder setDefaultValue(int selectedValue) {
			this.selectedValue = selectedValue;
			return this;
		}

		public ListPreferenceFragment build() {
			return ListPreferenceFragment.newInstance(this);
		}
	}

	public void setCloseListener(ListPreference.OnDialogClosedListener listener) {
		mCloseListener = listener;
	}

	@Override
	public void close() {
		if (mCloseListener != null) {
			mCloseListener.onItemSelected(mSelectedPosition);
		}
		super.close();
	}

	@Override
	protected int getLayout() {
		return R.layout.view_preference_list;
	}

	@Override
	protected void initLayout(View v) {
		mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerview);

		LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		mRecyclerView.setLayoutManager(lm);

		mAdapter = new ListPreferenceAdapter();
		mRecyclerView.setAdapter(mAdapter);

//		mOkButton = (Button) v.findViewById(R.id.button);
//		mOkButton.setTextColor(mAccentColor);
//		mOkButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				getActivity().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE)
//						.edit()
//						.putInt(mKey, mSelectedPosition)
//						.apply();
//				close();
//			}
//		});

		update();
	}

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

	protected void updateLayout() {
		ViewGroup.LayoutParams lp = mRecyclerView.getLayoutParams();
		lp.height = mEntries.length * mViewHeight;
		mRecyclerView.setLayoutParams(lp);
		mRecyclerView.requestLayout();
	}

	protected class PreferenceItem {
		String mText;
		boolean mSelected;

		public PreferenceItem(String text) {
			mText = text;
			mSelected = false;
		}
	}

	protected class ListPreferenceAdapter extends RecyclerView.Adapter<ListPreferenceAdapter.ViewHolder> {
		private ArrayList<PreferenceItem> mDataset;

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			View v = inflater.inflate(R.layout.view_preference_list_item, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			holder.bind(position);
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

		public void setDataset(ArrayList<PreferenceItem> dataset) {
			mDataset = dataset;
			notifyDataSetChanged();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
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
				text.setText(container.mText);

				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
//						mClicked = true;
//						radioButton.setChecked(true);
						mSelectedPosition = position;
						radioButton.setChecked(true);

						for (int i = 0; i < mAdapter.getItemCount(); i++) {
							if (i != position) {
								ViewHolder holder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
								if (holder != null) {
									holder.radioButton.setChecked(false);
								}
							}
						}

                        PrefUtils.get(getActivity())
								.edit()
								.putInt(mKey, mSelectedPosition)
								.apply();
						close();
					}
				});

//				radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//					@Override
//					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//						// We use mClicked to make sure this event comes from user input.
//						// Without this, rebinding can cause multiple items to show as selected
//						if (mClicked && isChecked) {
//							mSelectedPosition = position;
//							for (int i = 0; i < mAdapter.getItemCount(); i++) {
//								if (i != position) {
//									ViewHolder holder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
//									if (holder != null) {
//										holder.radioButton.setChecked(false);
//									}
//								}
//							}
//
//                            getActivity().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE)
//                                    .edit()
//                                    .putInt(mKey, mSelectedPosition)
//                                    .apply();
//                            close();
//						}
//
//						mClicked = false;
//					}
//				});
			}
		}
	}
}
