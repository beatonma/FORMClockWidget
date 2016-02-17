package com.beatonma.formclockwidget.app.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

import com.beatonma.colorpicker.ColorContainer;
import com.beatonma.colorpicker.ColorPickerFragment;
import com.beatonma.colorpicker.ColorUtils;
import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.app.ConfigActivity;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 07/11/2015.
 */
public class ColorPreference extends Preference {
	private final static String TAG = "ColorPreference";

	private ColorPreviewButton mButton;
	private SwitchCompat mSwitch;

	private int mColor = Color.GRAY;

	public ColorPreference(Context context) {
		super(context);
	}

	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		View v = inflate(context, R.layout.view_preference_color, this);

		TypedArray a = context.getTheme()
				.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, defStyleRes);

		try {
			mKey = a.getString(R.styleable.Preference_key);
		}
		finally {
			a.recycle();
		}

		mButton = (ColorPreviewButton) v.findViewById(R.id.color_button);
		mSwitch = (SwitchCompat) v.findViewById(R.id.color_lock);

		mButton.setKey(mKey);
		mButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getContext() instanceof ConfigActivity) {
					int accentColor = ((ConfigActivity) getContext()).getAccentColor();

					String tag = getContext().getString(R.string.fragtag_colorpicker);
					FragmentManager fm = ((ConfigActivity) getContext()).getSupportFragmentManager();
					fm.beginTransaction()
							.add(R.id.top_level_container, ColorPickerFragment.newInstance(mKey, accentColor), tag)
							.addToBackStack(tag)
							.commit();
				}
			}
		});

		if (context instanceof ConfigActivity) {
			setSwitchColor(((ConfigActivity) context).getAccentColor());
		}

		ColorContainer container = ColorUtils.getColorContainerFromPreference(
				context.getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE), mKey);

		mSwitch.setChecked(container.isLocked());
		mButton.post(new Runnable() {
			@Override
			public void run() {
				setActive(mSwitch.isChecked());
			}
		});

		mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences sp = getContext().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);
				ColorContainer container = ColorUtils.getColorContainerFromPreference(sp, mKey);
				container.setLocked(isChecked);
				ColorUtils.writeColorContainerToPreference(sp, mKey, container);

				setActive(isChecked);
			}
		});

		View background = v.findViewById(R.id.top_level_container);
		background.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSwitch.toggle();
			}
		});
	}

	@SuppressWarnings("NewApi")
	public void setColor(int color) {
		mColor = color;

		SharedPreferences sp = getContext().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);
		ColorContainer container = ColorUtils.getColorContainerFromPreference(sp, mKey);

		setActive(container.isLocked());

		mButton.setColor(color);
	}

	public int getColor() {
		return mColor;
	}

	@SuppressWarnings("NewApi")
	public void setSwitchColor(int color) {
		mSwitch.setHighlightColor(color);

		if (Utils.isLollipop()) {
			ColorStateList thumbStates;
			ColorStateList trackStates;
			int grey = getResources().getColor(R.color.PrimaryLight);

			thumbStates = Utils.getSimpleSelector(grey, color);
			trackStates = Utils.getSimpleSelector(ColorUtils.lighten(grey, 0.2f), ColorUtils.lighten(color, 0.25f));

			Drawable d = mSwitch.getThumbDrawable();
			d.setTintList(thumbStates);
			mSwitch.setThumbDrawable(d);

			d = mSwitch.getTrackDrawable();
			d.setTintList(trackStates);
			mSwitch.setTrackDrawable(d);
		}
	}

	public void setActive(boolean b) {
		mButton.setActive(b);
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}
}
