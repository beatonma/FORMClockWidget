package com.beatonma.formclockwidget.daydream;

import android.service.dreams.DreamService;
import android.view.View;

import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 27/01/2016.
 */
public class DaydreamService extends DreamService {
    private DreamView mDream;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setInteractive(false);
        setScreenBright(true);
        setFullscreen(true);

        mDream = new DreamView(this);
        mDream.setDreamService(this);

        if (Utils.isKitkat()) {
            mDream.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        setContentView(mDream);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
//        mDream.stop();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
