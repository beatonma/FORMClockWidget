package com.beatonma.formclockwidget.app.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.Utils;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Michael on 10/11/2015.
 */
public class MultiListPreferenceFragment extends AnimatedPopupFragment {
	private final static String KEY = "key";
	private final static String ACCENT_COLOR = "accent_color";
	private final static String LIST_ENTRIES = "list_entries";
	private final static String SELECTED_POSITIONS = "selected_positions";

	private RecyclerView mRecyclerView;
	private MultiListPreferenceAdapter mAdapter;
	private ProgressBar mProgressBar;

	private OnItemsSavedListener mListener;

	private int mViewHeight;

	private String mKey;
	private int mAccentColor;
	private String[] mEntries;
	private int[] mPreSelected;
	private HashSet<Integer> mSelected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			mKey = args.getString(KEY);
			mAccentColor = args.getInt(ACCENT_COLOR);
			mEntries = args.getStringArray(LIST_ENTRIES);
			mPreSelected = args.getIntArray(SELECTED_POSITIONS);
		}

		mViewHeight = Utils.dpToPx(getContext(), 52);

		mSelected = new HashSet<>();
		if (mPreSelected != null) {
			for (int i : mPreSelected) {
				mSelected.add(i);
			}
		}
	}

	@Override
	protected int getLayout() {
		return R.layout.view_preference_multilist;
	}

	public static MultiListPreferenceFragment newInstance(Builder builder) {
		Bundle args = new Bundle();
		args.putString(KEY, builder.key);
		args.putInt(ACCENT_COLOR, builder.accentColor);
		args.putStringArray(LIST_ENTRIES, builder.items);
		args.putIntArray(SELECTED_POSITIONS, builder.selectedItems);

		MultiListPreferenceFragment fragment = new MultiListPreferenceFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static class Builder {
		String key;
		int accentColor;
		String[] items;
		int[] selectedItems;

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

		public Builder setSelectedItems(int[] selectedItems) {
			this.selectedItems = selectedItems;
			return this;
		}

		public MultiListPreferenceFragment build() {
			return MultiListPreferenceFragment.newInstance(this);
		}
	}

	public void setEntries(String[] entries) {
		this.mEntries = entries;
		update();
	}

	public void setSelectedPositions(int[] selected) {
		if (mAdapter != null && mAdapter.mDataset != null) {
			for (int n : selected) {
				mSelected.add(n);
				PreferenceItem item = mAdapter.mDataset.get(n);
				item.mSelected = true;
				mAdapter.mDataset.set(n, item);
				mAdapter.notifyItemChanged(n);
			}
		}
	}

	public void setOnItemsSavedListener(OnItemsSavedListener listener) {
		mListener = listener;
	}

	@SuppressWarnings("NewApi")
	@Override
	protected void initLayout(View v) {
		mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerview);

		LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		mRecyclerView.setLayoutManager(lm);

		mAdapter = new MultiListPreferenceAdapter();
		mRecyclerView.setAdapter(mAdapter);

		Button clearButton = (Button) v.findViewById(R.id.clear_button);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mAdapter.mDataset == null) {
					return;
				}

				try {
					for (int i = 0; i < mAdapter.mDataset.size(); i++) {
						PreferenceItem item = mAdapter.mDataset.get(i);
						item.mSelected = false;
						mAdapter.mDataset.set(i, item);
					}
					mAdapter.notifyDataSetChanged();
				} catch (Exception e) {
					Log.e(TAG, "Illegal recyclerview state: " + e.toString());
				}
			}
		});

		Button okButton = (Button) v.findViewById(R.id.ok_button);
		okButton.setTextColor(mAccentColor);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mListener != null) {
					mListener.onItemsSaved(mAdapter.getSelectedItems());
				}
				close();
			}
		});

		mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);
		if (Utils.isLollipop()) {
			mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(mAccentColor));
		}

		update();
	}

	protected void update() {
		if (mEntries != null) {
			updateLayout();

			ArrayList<PreferenceItem> dataset = new ArrayList<>();

			for (int i = 0; i < mEntries.length; i++) {
				PreferenceItem item = new PreferenceItem(mEntries[i]);

				if (mPreSelected != null) {
					for (int j = 0; j < mPreSelected.length; j++) {
						int n = mPreSelected[j];
						if (n == i) {
							item.mSelected = true;
							break;
						}
					}
				}

				dataset.add(item);
			}

			mProgressBar.setVisibility(View.GONE);
			mAdapter.setDataset(dataset);
		}
		else {
			mProgressBar.setVisibility(View.VISIBLE);
		}
	}

	protected void updateLayout() {
		ViewGroup.LayoutParams lp = mRecyclerView.getLayoutParams();
		lp.height = Math.min(
				Math.min(mEntries.length, 8) * mViewHeight,
				Utils.getScreenHeight(getContext()) - (mViewHeight * 2));
		mRecyclerView.setLayoutParams(lp);
		mRecyclerView.requestLayout();

		lp = mCard.getLayoutParams();
		lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		mCard.requestLayout();
	}

	protected class PreferenceItem {
		String mText;
		boolean mSelected;

		public PreferenceItem(String text) {
			mText = text;
			mSelected = false;
		}

		@Override
		public String toString() {
			return mText + ";" + mSelected;
		}
	}

	protected class MultiListPreferenceAdapter extends RecyclerView.Adapter<MultiListPreferenceAdapter.ViewHolder> {
		private ArrayList<PreferenceItem> mDataset;

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			View v = inflater.inflate(R.layout.view_preference_multilist_item, parent, false);
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
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			AppCompatCheckBox checkbox;
			TextView text;

			public ViewHolder(View v) {
				super(v);

				checkbox = (AppCompatCheckBox) v.findViewById(R.id.checkbox);
				text = (TextView) v.findViewById(R.id.text);
			}

			public void bind(final int position) {
				PreferenceItem item = mDataset.get(position);

				checkbox.setChecked(item.mSelected);
				text.setText(item.mText);

				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PreferenceItem item = mDataset.get(position);
						if (checkbox.isChecked()) {
							item.mSelected = false;
							checkbox.setChecked(false);
						}
						else {
							item.mSelected = true;
							checkbox.setChecked(true);
						}

						mDataset.set(position, item);
						notifyItemChanged(position);
					}
				});
			}
		}

		public int[] getSelectedItems() {
			ArrayList<Integer> selected = new ArrayList<>();
			for (int i = 0; i < mDataset.size(); i++) {
				PreferenceItem item = mDataset.get(i);
				if (item.mSelected) {
					selected.add(i);
				}
			}

			int[] asArray = new int[selected.size()];
			for (int i = 0; i < selected.size(); i++) {
				asArray[i] = selected.get(i);
			}

			return asArray;
		}
	}

	public interface OnItemsSavedListener {
		void onItemsSaved(int[] selectedItems);
	}
}
