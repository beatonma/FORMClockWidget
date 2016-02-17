package com.beatonma.formclockwidget.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.beatonma.formclockwidget.data.AppContainer;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.data.DbHelper;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.Utils;
import com.beatonma.formclockwidget.utility.WallpaperUtils;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Michael on 31/10/2015.
 */
public class OnTouchActivity extends AppCompatActivity {
	private final static String TAG = "OnTouchActivity";

	private RecyclerView mRecyclerView;
	private TouchShortcutAdapter mAdapter;

	private int mViewHeight;
	private ArrayList<AppContainer> mShortcuts;

    private AsyncTask<Context, Void, Void> mLoadingTask;

	private int mThemeId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        boolean showActivity = PrefUtils.get(this).getBoolean(PrefUtils.PREF_ON_TOUCH_SHOW_ACTIVITY, false);
        if (showActivity) {
            mThemeId = WallpaperUtils.getThemeFromWallpaper(PrefUtils.get(this));
            setTheme(mThemeId);
            setContentView(R.layout.activity_ontouch_shortcuts);

			Handler handler = new Handler(getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					init();
				}
			});
        }
        else {
            mShortcuts = DbHelper.getInstance(this).getShortcuts();
                if (mShortcuts.size() == 1) {
                    AppContainer container = mShortcuts.get(0);
                    if (container.getFriendlyName().equals(AppContainer.NOTHING)) {
                        Intent intent = new Intent(WidgetUpdateService.UPDATE_COLORS);
                        sendBroadcast(intent);
                        finish();
                    }
                    else {
                        startShortcut(container);
                    }
            }
            else if (mShortcuts.isEmpty()) {
                startShortcut(new AppContainer(AppContainer.DEFAULT_PACKAGE,
						AppContainer.DEFAULT_ACTIVITY, AppContainer.DEFAULT_NAME));
            }

            Intent intent = new Intent(WidgetUpdateService.UPDATE_COLORS);
            sendBroadcast(intent);

            finish();
        }
	}

	private void startShortcut(AppContainer container) {
		ComponentName componentName = new ComponentName(container.getPackageName(), container.getActivityName());
		Intent intent = new Intent();
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(componentName);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);

		finish();
	}

	private void init() {
		mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);

		LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
		mRecyclerView.setLayoutManager(lm);

		mAdapter = new TouchShortcutAdapter();
		mRecyclerView.setAdapter(mAdapter);

		TypedArray a = getTheme().obtainStyledAttributes(mThemeId, new int[]{
				android.R.attr.colorPrimary,
				android.R.attr.colorAccent,
				R.attr.colorControlActivated
		});

		int accentColor;

		if (Utils.isLollipop()) {
			accentColor = a.getColor(1, 0);
		}
		else {
			accentColor = a.getColor(2, 0);
		}

		a.recycle();

		Button settingsButton = (Button) findViewById(R.id.settings);
		settingsButton.setTextColor(accentColor);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startShortcut(new AppContainer(AppContainer.DEFAULT_PACKAGE,
						AppContainer.DEFAULT_ACTIVITY, AppContainer.DEFAULT_NAME));
			}
		});

		DisplayMetrics dm = getResources().getDisplayMetrics();
		float maxWidth = getResources().getDimension(R.dimen.content_max_width);
		if (Utils.pxToDp(this, dm.widthPixels)
				> maxWidth) {
			View card = findViewById(R.id.card);
			ViewGroup.LayoutParams lp = card.getLayoutParams();
			lp.width = (int) maxWidth;
			card.setLayoutParams(lp);
		}

        updateLayout(1);
        loadShortcuts();
	}

	private void update() {
		updateLayout(mShortcuts == null ? 0 : mShortcuts.size());

		mAdapter.setDataset(mShortcuts);
	}

	protected void updateLayout(int items) {
		int targetHeight = Math.min(
				items * mViewHeight,
				Utils.getScreenHeight(this) - (mViewHeight * 3));

		ViewGroup.LayoutParams lp = mRecyclerView.getLayoutParams();
		lp.height = targetHeight;

		mRecyclerView.setLayoutParams(lp);
		mRecyclerView.requestLayout();
	}

    private void loadShortcuts() {
        mLoadingTask = new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... params) {
                if (params != null) {
                    Context context = params[0];
                    mShortcuts = DbHelper.getInstance(context).getShortcuts();
                    Collections.sort(mShortcuts, new AppContainer());
                    mViewHeight = Utils.dpToPx(context, 52);

                    Intent intent = new Intent(WidgetUpdateService.UPDATE_COLORS);
                    sendBroadcast(intent);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                hideLoading();
                update();
            }
        };
        mLoadingTask.execute(this);
    }

	@Override
	protected void onPause() {
		Intent intent = new Intent(WidgetUpdateService.UPDATE_COLORS);
		sendBroadcast(intent);

        if (mLoadingTask != null) {
            mLoadingTask.cancel(true);
        }

		super.onPause();
		overridePendingTransition(0, R.anim.abc_shrink_fade_out_from_bottom);
	}

    private void showLoading() {
        View progress = findViewById(R.id.progressbar);
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        View progress = findViewById(R.id.progressbar);
        if (progress != null) {
            progress.setVisibility(View.GONE);
        }
    }

	private class TouchShortcutAdapter extends RecyclerView.Adapter<TouchShortcutAdapter.ViewHolder> {
		private ArrayList<AppContainer> mDataset;

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			View v = inflater.inflate(R.layout.view_ontouch_item, parent, false);
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
			TextView text;

			public ViewHolder(View v) {
				super(v);

				text = (TextView) v.findViewById(R.id.text);
			}

			public void bind(final int position) {
				AppContainer container = mDataset.get(position);
				text.setText(container.getFriendlyName());

				itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startShortcut(mDataset.get(position));
					}
				});
			}
		}
	}
}
