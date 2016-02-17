package com.beatonma.formclockwidget.data;

import java.util.Comparator;

/**
 * Created by Michael on 08/02/2016.
 */
public class AppContainer implements Comparator<AppContainer> {
    private final static String TAG = "AppContainer";

    private final static String SEPARATOR = "_;_";

    public final static String DEFAULT_ACTIVITY = "com.beatonma.formclockwidget.app.ConfigActivity";
    public final static String DEFAULT_PACKAGE = "com.beatonma.formclockwidget";
    public final static String DEFAULT_NAME = "FORM Clock Widget";
    public final static String NOTHING = "NOTHING_";

    public final static int TYPE_NOTHING = -1;
    public final static int TYPE_DEFAULT = 1;

    private String mFriendlyName;
    private String mPackageName;
    private String mActivityName;
    private boolean mChecked;

    public AppContainer() {
        mFriendlyName = "";
        mPackageName = "";
        mActivityName = "";
        mChecked = false;
    }

    public AppContainer(String packageName, String activityName, String friendlyName) {
        mPackageName = packageName;
        mActivityName = activityName;
        mFriendlyName = friendlyName;
        mChecked = true;
    }

    public AppContainer(AppInfoContainer oldShortcut) {
        mPackageName = oldShortcut.getPackageName();
        mActivityName = oldShortcut.getActivityName();
        mFriendlyName = oldShortcut.getNiceName();
        mChecked = true;
    }

    public AppContainer(int type) {
        switch (type) {
            case TYPE_NOTHING:
                mPackageName = NOTHING;
                mActivityName = NOTHING;
                mFriendlyName = NOTHING;
                mChecked = false;
                break;
            case TYPE_DEFAULT:
                mPackageName = DEFAULT_PACKAGE;
                mActivityName = DEFAULT_ACTIVITY;
                mFriendlyName = DEFAULT_NAME;
                mChecked = true;
                break;
        }
    }

    public String getFriendlyName() {
        return mFriendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        mFriendlyName = friendlyName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public String getActivityName() {
        return mActivityName;
    }

    public void setActivityName(String activityName) {
        mActivityName = activityName;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    @Override
    public String toString() {
        return mPackageName + SEPARATOR + mActivityName + SEPARATOR + mFriendlyName + SEPARATOR + mChecked;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof AppContainer)
                && this.getPackageName().equals(((AppContainer) other).getPackageName())
                && this.getActivityName().equals(((AppContainer) other).getActivityName());
    }

    @Override
    public int compare(AppContainer lhs, AppContainer rhs) {
        return lhs.getFriendlyName().toLowerCase()
                .compareTo(rhs.getFriendlyName().toLowerCase());
    }
}