package com.beatonma.formclockwidget.app;

import android.view.View;

import com.beatonma.formclockwidget.R;

/**
 * Created by Michael on 06/01/2016.
 */
public class ConfigFormatFragment extends ConfigBaseFragment {
    public static ConfigFormatFragment newInstance() {
        return new ConfigFormatFragment();
    }

    @Override
    int getLayout() {
        return R.layout.fragment_config_format;
    }

    @Override
    void init(View v) {

    }
}
