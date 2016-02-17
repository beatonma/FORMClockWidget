package com.beatonma.formclockwidget.app.ui;

import android.content.res.ColorStateList;
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
import com.beatonma.formclockwidget.data.AppContainer;
import com.beatonma.formclockwidget.utility.Utils;

import java.util.ArrayList;

/**
 * Created by Michael on 08/02/2016.
 */
public class AppSelectFragment extends AnimatedPopupFragment {
    private RecyclerView mRecyclerView;
    private MultiListPreferenceAdapter mAdapter;
    private ProgressBar mProgressBar;

    private OnItemsSavedListener mListener;

    private int mViewHeight = 0;

    private int mAccentColor;
    private ArrayList<AppContainer> mEntries;

    public static AppSelectFragment newInstance() {
        AppSelectFragment fragment = new AppSelectFragment();
        return fragment;
    }

    @Override
    protected int getLayout() {
        return R.layout.view_preference_multilist;
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
                        AppContainer item = mAdapter.mDataset.get(i);
                        item.setChecked(false);
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

    public void setEntries(ArrayList<AppContainer> entries) {
        mEntries = entries;
    }

    public void setAccentColor(int color) {
        mAccentColor = color;
    }

    public void setOnItemsSavedListener(OnItemsSavedListener listener) {
        mListener = listener;
    }

    public void update() {
        Log.d(TAG, "Updating app list fragment");
        if (mEntries != null) {
            updateLayout();
            Log.d(TAG, "mEntries size=" + mEntries.size());

            mProgressBar.setVisibility(View.GONE);
            mAdapter.setDataset(mEntries);
        }
        else {
            Log.d(TAG, "mEntries is null!");
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    protected void updateLayout() {
        if (mViewHeight == 0) {
            mViewHeight = (int) getResources().getDimension(R.dimen.min_listview_height);
        }

        ViewGroup.LayoutParams lp = mRecyclerView.getLayoutParams();
        lp.height = Math.min(
                Math.min(mEntries.size(), 8) * mViewHeight,
                Utils.getScreenHeight(getContext()) - (mViewHeight * 2));
        mRecyclerView.setLayoutParams(lp);
        mRecyclerView.requestLayout();

        lp = mCard.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mCard.requestLayout();
    }

    protected class MultiListPreferenceAdapter extends RecyclerView.Adapter<MultiListPreferenceAdapter.ViewHolder> {
        private ArrayList<AppContainer> mDataset;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(R.layout.view_preference_app_item, parent, false);
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

        public void setDataset(ArrayList<AppContainer> dataset) {
            mDataset = dataset;
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            AppCompatCheckBox checkbox;
            TextView title;
            TextView description;

            public ViewHolder(View v) {
                super(v);

                checkbox = (AppCompatCheckBox) v.findViewById(R.id.checkbox);
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
            }

            public void bind(final int position) {
                AppContainer item = mDataset.get(position);

                checkbox.setChecked(item.isChecked());
                title.setText(item.getFriendlyName());
                description.setText(item.getPackageName());

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppContainer item = mDataset.get(position);
                        if (checkbox.isChecked()) {
                            item.setChecked(false);
                            checkbox.setChecked(false);
                        }
                        else {
                            item.setChecked(true);
                            checkbox.setChecked(true);
                        }

                        mDataset.set(position, item);
                        notifyItemChanged(position);
                    }
                });
            }
        }

        public ArrayList<AppContainer> getSelectedItems() {
            ArrayList<AppContainer> selected = new ArrayList<>();
            for (int i = 0; i < mDataset.size(); i++) {
                AppContainer item = mDataset.get(i);
                if (item.isChecked()) {
                    selected.add(item);
                }
            }

            if (selected.isEmpty()) {
                selected.add(new AppContainer(AppContainer.TYPE_NOTHING));
            }

            return selected;
        }
    }

    public interface OnItemsSavedListener {
        void onItemsSaved(ArrayList<AppContainer> savedItems);
    }
}