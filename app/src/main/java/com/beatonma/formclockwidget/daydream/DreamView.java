package com.beatonma.formclockwidget.daydream;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.formclock.FormClockView;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.WallpaperUtils;

import java.util.Calendar;

/**
 * Created by Michael on 27/01/2016.
 */
public class DreamView extends RelativeLayout implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final static String TAG = "DreamView";

    private DaydreamService mService;
    private final static String MUZEI_ARTWORK_CHANGED = "com.google.android.apps.muzei.ACTION_ARTWORK_CHANGED";
    private final static IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
        INTENT_FILTER.addAction(MUZEI_ARTWORK_CHANGED);
    }
    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MUZEI_ARTWORK_CHANGED)) {
                loadWallpaper();
            }
            else {
                tick();
            }
        }
    };

    private Handler mHandler = new Handler();
    private FormClockView mClockView;
    private ImageView mBackground;

    private long mAnimDuration;
    private long mAnimProgress = 0;
    private long mAnimLastFrame = 0;

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            mClockView.setAnimationProgress(mAnimProgress);

            long frameTime = mAnimLastFrame == 0 ? 0 : System.currentTimeMillis() - mAnimLastFrame;

            mAnimProgress += frameTime;

            if (mAnimProgress <= mAnimDuration) {
                mAnimLastFrame = System.currentTimeMillis();
                mHandler.post(this);
            }
            else {
                mAnimLastFrame = 0;
            }
        }
    };

    public DreamView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public DreamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public DreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DreamView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        View v = inflate(context, R.layout.view_dream, this);

        mClockView = (FormClockView) v.findViewById(R.id.clock_view);
        mBackground = (ImageView) v.findViewById(R.id.background);

        loadWallpaper();
    }

    public void setDreamService(DaydreamService service) {
        mService = service;
    }

    private void tick() {
        Calendar cal = Calendar.getInstance();

        mAnimDuration = mClockView.getAnimDuration(cal);

        mAnimProgress = 0;
        mHandler.post(mUpdateRunnable);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        PrefUtils.get(getContext()).registerOnSharedPreferenceChangeListener(this);
        getContext().registerReceiver(mUpdateReceiver, INTENT_FILTER);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mUpdateRunnable);
        PrefUtils.get(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        getContext().unregisterReceiver(mUpdateReceiver);
    }

    public void loadWallpaper() {
        new AsyncTask<Context, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Context... params) {
                Context context = params[0];

                Bitmap bitmap = WallpaperUtils.getWallpaperBitmap(mService);
                if (context != null) {
                    WallpaperUtils.extractColors(context, bitmap);
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                AnimationUtils.refreshImage(new Handler(), mBackground, bitmap);
            }
        }.execute(getContext());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mClockView.regen(sharedPreferences);
    }
}
