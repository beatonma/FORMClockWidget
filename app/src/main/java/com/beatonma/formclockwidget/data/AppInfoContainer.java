package com.beatonma.formclockwidget.data;

import java.util.Comparator;

/**
 * Created by Michael on 10/11/2015.
*/
@Deprecated
public class AppInfoContainer implements Comparator<AppInfoContainer>{
	String mNiceName;
	String mPackageName;
	String mActivityName;

	boolean mChecked;

	public AppInfoContainer() {

	}

	public AppInfoContainer(String niceName, String packageName, String activityName) {
		mNiceName = niceName;
		mPackageName = packageName;
		mActivityName = activityName;
		mChecked = false;
	}

	public AppInfoContainer(String asString) {
		String[] parts = asString.split(";");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (part.contains("niceName=")) {
				mNiceName = part.split("=")[1];
			}
			else if (part.contains("packageName=")) {
				mPackageName = part.split("=")[1];
			}
			else if (part.contains("activityName=")) {
				mActivityName = part.split("=")[1];
			}
			else if (part.contains("isChecked=")) {
				mChecked = Boolean.valueOf(part.split("=")[1]);
			}
		}
	}

	@Override
	public String toString() {
		return "niceName=" + mNiceName
				+ ";packageName=" + mPackageName
				+ ";activityName=" + mActivityName
				+ ";isChecked=" + mChecked;
	}

	public String getNiceName() {
		return mNiceName;
	}

	public void setNiceName(String niceName) {
		mNiceName = niceName;
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

	@Override
	public int compare(AppInfoContainer lhs, AppInfoContainer rhs) {
		int comparison = lhs.getNiceName().compareTo(rhs.getNiceName());
		if (comparison < 0) {
			return -1;
		}
		else if (comparison > 0) {
			return 1;
		}
		else {
			return 0;
		}
	}
}