package com.beatonma.colorpicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.PrefUtils;
import com.beatonma.formclockwidget.utility.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Michael on 08/11/2015.
 */
public class ColorPickerFragment extends AnimatedPopupFragment {
	private final static String TAG = "ColorPickerFragment";

	public final static String KEY = "key";
	public final static String ACCENT_COLOR = "accent_color";

	private final static int VIEW_LEVEL_SWATCHES = 0;
	private final static int VIEW_LEVEL_COLORS = 1;

	private ArrayList<String> mDataset;
	private RecyclerView mRecyclerView;
	private PatchAdapter mAdapter;
	private Button mCustomButton;
	private Button mBackButton;
	private Button mCustomOkButton;

	private View mCustomContainer;
//	private View mCustomPreview;
	private CustomColorPreviewView mCustomPreview;
	private ColorSeekbarView mRedSlider;
	private ColorSeekbarView mGreenSlider;
	private ColorSeekbarView mBlueSlider;
	private ColorSeekbarView mAlphaSlider;
	private AppCompatEditText mCustomHexEdit;

	private ColorContainer mColorContainer;
	private String mKey = "";
	private int mAccentColor;

	private int mLevel = VIEW_LEVEL_SWATCHES;
	private int mOpenSwatch = -1;

	private int mCustomRed = 0;
	private int mCustomGreen = 0;
	private int mCustomBlue = 0;
	private int mCustomAlpha = 255;

	// Remember which colours and/or swatches have been selected
	private ArrayList<ColorContainer> mSelectedItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			mKey = args.getString(KEY);
			mAccentColor = args.getInt(ACCENT_COLOR);
			mPositionX = args.getFloat(POSITION_X);
			mPositionY = args.getFloat(POSITION_Y);
		}
	}

	public static ColorPickerFragment newInstance(String key, int accentColor) {
		ColorPickerFragment fragment = new ColorPickerFragment();
		Bundle args = new Bundle();
		args.putString(KEY, key);
		args.putInt(ACCENT_COLOR, accentColor);
		args.putFloat(POSITION_X, -1);
		args.putFloat(POSITION_Y, -1);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	protected int getLayout() {
		return R.layout.view_color_picker;
	}

	@Override
	protected void initLayout(View v) {
		mRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerview);

		GridLayoutManager lm = new GridLayoutManager(getContext(), 4);
		mRecyclerView.setLayoutManager(lm);

		mAdapter = new PatchAdapter(null);
		mRecyclerView.setAdapter(mAdapter);

		mRecyclerView.addOnItemTouchListener(new ColorPatchOnClickListener());
		mRecyclerView.setClickable(true);

		mBackButton = (Button) v.findViewById(R.id.back_button);
		mBackButton.setTextColor(mAccentColor);
		mBackButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mBackButton.getVisibility() == View.VISIBLE) {
					showSwatches();
				}
			}
		});

		mCustomContainer = v.findViewById(R.id.custom_color_container);
		mCustomPreview = (CustomColorPreviewView) v.findViewById(R.id.custom_preview);

		mCustomButton = (Button) v.findViewById(R.id.custom_button);
		mCustomButton.setTextColor(mAccentColor);
		mCustomButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCustomButton.getText().equals("Custom")) {
					showPresets();
				}
				else {
					showCustoms();
				}
			}
		});

		mCustomOkButton = (Button) v.findViewById(R.id.custom_ok);
		mCustomOkButton.setTextColor(mAccentColor);
		mCustomOkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveCustomColor();
				close();
			}
		});

		mRedSlider = (ColorSeekbarView) v.findViewById(R.id.red_slider);
		mGreenSlider = (ColorSeekbarView) v.findViewById(R.id.green_slider);
		mBlueSlider = (ColorSeekbarView) v.findViewById(R.id.blue_slider);
		mAlphaSlider = (ColorSeekbarView) v.findViewById(R.id.alpha_slider);

		ColorSeekbarView.OnValueChangedListener listener = new ColorSeekbarView.OnValueChangedListener() {
			@Override
			public void onValueChanged(int channel, int value) {
				switch (channel) {
					case ColorSeekbarView.CHANNEL_RED:
						mCustomRed = value;
						break;
					case ColorSeekbarView.CHANNEL_GREEN:
						mCustomGreen = value;
						break;
					case ColorSeekbarView.CHANNEL_BLUE:
						mCustomBlue = value;
						break;
					case ColorSeekbarView.CHANNEL_ALPHA:
						mCustomAlpha = value;
						break;
				}

				updateCustomColorViews();
			}
		};

		mRedSlider.setListener(listener);
		mGreenSlider.setListener(listener);
		mBlueSlider.setListener(listener);
		mAlphaSlider.setListener(listener);

		mCustomHexEdit = (AppCompatEditText) v.findViewById(R.id.custom_hex);
		mCustomHexEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				String text = s.toString();
				int length = s.length();
				if (!text.matches("[a-fA-F0-9]+") && length > 0) {
					s.delete(length - 1, length);
				}

				if (s.length() == 8) {
					updateCustomColorViews(s.toString());
				}
			}
		});

//		ViewGroup.LayoutParams lp = mRecyclerView.getLayoutParams();
//		lp.height = (int) Math.min(getContext().getResources().getDimension(R.dimen.colorpicker_card_height), Utils.getScreenHeight(getContext()));
//		mRecyclerView.setLayoutParams(lp);
//		mRecyclerView.requestLayout();
//
//		View card = v.findViewById(R.id.card);
//		card.requestLayout();

		loadSelectedItems();

		new PaletteFiller(VIEW_LEVEL_SWATCHES).execute();
	}

	public boolean onBackPressed() {
		boolean consumed = false;
		if (mLevel == VIEW_LEVEL_SWATCHES) {
			consumed = true;
			close();
		} else if (mLevel == VIEW_LEVEL_COLORS) {
			consumed = true;
			showSwatches();
		}

		return consumed;
	}

	private void updateCustomColorViews(int color) {
		mCustomRed = Color.red(color);
		mCustomGreen = Color.green(color);
		mCustomBlue = Color.blue(color);
		mCustomAlpha = Color.alpha(color);

		mRedSlider.setValue(mCustomRed);
		mGreenSlider.setValue(mCustomGreen);
		mBlueSlider.setValue(mCustomBlue);
		mAlphaSlider.setValue(mCustomAlpha);

		mCustomHexEdit.setText(ColorUtils.toHex(color));
	}

	private void updateCustomColorViews(String hex) {
		int color = Color.parseColor("#" + hex);

		mCustomRed = Color.red(color);
		mCustomGreen = Color.green(color);
		mCustomBlue = Color.blue(color);
		mCustomAlpha = Color.alpha(color);

		mRedSlider.setValue(mCustomRed);
		mGreenSlider.setValue(mCustomGreen);
		mBlueSlider.setValue(mCustomBlue);
		mAlphaSlider.setValue(mCustomAlpha);
	}

	private void updateCustomColorViews() {
		int customColor = Color.argb(mCustomAlpha, mCustomRed, mCustomGreen, mCustomBlue);
//		mCustomPreview.setBackgroundColor(customColor);
		mCustomPreview.setColor(customColor);

		if (Utils.isLollipop()) {
			mCustomHexEdit.setSupportBackgroundTintList(ColorStateList.valueOf(customColor));
		}
		else {
			mCustomHexEdit.setSupportBackgroundTintList(Utils.getSimpleSelector(customColor, customColor));
		}

		mCustomHexEdit.setText(ColorUtils.toHex(customColor));
	}

	private void showPresets() {
		AnimationUtils.fadeOutCompletely(mRecyclerView);
		AnimationUtils.fadeIn(mCustomOkButton);
		AnimationUtils.fadeIn(mCustomContainer);

		if (mBackButton.getVisibility() == View.VISIBLE) {
			AnimationUtils.fadeOutCompletely(mBackButton);
		}

		mCustomButton.setText("Presets");
	}

	private void showCustoms() {
		AnimationUtils.fadeOutCompletely(mCustomContainer);
		AnimationUtils.fadeOutCompletely(mCustomOkButton);
		AnimationUtils.fadeIn(mRecyclerView);

		mCustomButton.setText("Custom");
	}

	private void showColors(int position) {
		mLevel = VIEW_LEVEL_COLORS;
		mOpenSwatch = position;
		AnimationUtils.fadeIn(mBackButton);
		new PaletteFiller(mLevel, position).execute();
	}

	private void showSwatches() {
		mLevel = VIEW_LEVEL_SWATCHES;
		mOpenSwatch = -1;
		AnimationUtils.fadeOutCompletely(mBackButton);
		new PaletteFiller(VIEW_LEVEL_SWATCHES).execute();
	}

//	@Override
//	public void onColorPicked(int swatch, int color) {
//		if (getActivity() instanceof ConfigActivity) {
//			((ConfigActivity) getActivity()).loadPreferences();
//			((ConfigActivity) getActivity()).setColors();
//		}
//	}

	private void selectPatchAt(int position) {
		FrameLayout frame = (FrameLayout) mRecyclerView.getChildAt(position);
		if (frame != null) {
			PatchView patch = (PatchView) frame.findViewById(R.id.patch);
			patch.setSelected(true);
			patch.invalidate();
		}
	}

	private void loadSelectedItems() {
		mSelectedItems = new ArrayList<>();
		SharedPreferences sp = getActivity().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);
		Set set = sp.getStringSet(mKey, new HashSet<String>());

		Iterator<String> iterator = set.iterator();

		while (iterator.hasNext()) {
			String s = iterator.next();
			mSelectedItems.add(new ColorContainer(s));
		}

		if (!mSelectedItems.isEmpty()) {
			mColorContainer = mSelectedItems.get(0);
			updateCustomColorViews(mSelectedItems.get(0).colorValue);
		}
	}

	private void updateSelection() {
		if (!mSelectedItems.isEmpty()) {
			if (mColorContainer.isCustom()) {
				// Don't select a preset if we are using a custom color
				return;
			}

			if (mOpenSwatch == -1) {
				selectPatchAt(mColorContainer.swatch);
			}
			else if (mOpenSwatch == mColorContainer.swatch) {
				selectPatchAt(mColorContainer.color);
			}
		}
	}

	private void saveCustomColor() {
		int color = Color.argb(mCustomAlpha, mCustomRed, mCustomGreen, mCustomBlue);

		SharedPreferences sp = getActivity().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);
		Set<String> set = new HashSet<>();
		mColorContainer.setCustomColor(color);
		set.add(mColorContainer.toString());

		sp.edit()
				.putStringSet(mKey, set)
				.apply();
	}

	// We are using forced single-selection mode so selected string set should have <= 1 entry
	private void saveSelectedItems() {
		SharedPreferences sp = getActivity().getSharedPreferences(PrefUtils.PREFS, Context.MODE_PRIVATE);
		Set<String> set = new HashSet<>();

		Iterator<ColorContainer> iterator = mSelectedItems.iterator();

		while (iterator.hasNext()) {
			ColorContainer container = iterator.next();
			set.add(container.toString());
		}

		sp.edit()
				.putStringSet(mKey, set)
				.commit();
	}

	// Populate recyclerView with the correct colors
	public class PaletteFiller extends AsyncTask<Void, Void, Void> {
		int level = -1;
		int chosenPosition = -1;

		public PaletteFiller(int newLevel) {
			this.level = newLevel;
		}

		public PaletteFiller(int newLevel, int chosen) {
			this.level = newLevel;
			this.chosenPosition = chosen;
		}

		@Override
		protected Void doInBackground(Void... n) {
			switch (level) {
				case VIEW_LEVEL_SWATCHES:
					try {
						String[][] palettes = ColorUtils.PALETTES;

						for (int i = 0; i < palettes.length; i++) {
							String[] swatch = palettes[i];
							// Add color 500 from each color swatch
							mDataset.add(swatch[0]);
						}
					} catch (Exception e) {
						Log.e(TAG, "AsyncTask error: " + e.toString());
					}
					break;
				case VIEW_LEVEL_COLORS:
					try {
						String[][] palettes = ColorUtils.PALETTES;

						String[] swatch = palettes[chosenPosition];
						for (int i = 0; i < swatch.length; i++) {
							if (i == 0) {
								// Skip 500 colour at start of list
							} else {
								mDataset.add(swatch[i]);
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "AsyncTask error." + e.toString());
						e.printStackTrace();
					}
					break;
				default:
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			mDataset = new ArrayList<>();
		}

		@Override
		protected void onPostExecute(Void file) {
			if (level == VIEW_LEVEL_SWATCHES) {
				mAdapter.updateDataset(mDataset);
			}
			else if (level == VIEW_LEVEL_COLORS) {
				mAdapter.updateDataset(mDataset);
			}
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					updateSelection();
				}
			}, 200);
		}
	}

	private class ColorPatchOnClickListener implements RecyclerView.OnItemTouchListener {
		@Override
		public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				View childView = view.findChildViewUnder(event.getX(), event.getY());
				int position = view.getChildAdapterPosition(childView);

				if (position != RecyclerView.NO_POSITION) {
					try {
						modeSingleColor(position);
					}
					catch (Exception e) {
						Log.e(TAG, "Error handling click: " + e.toString());
					}
				}
			}

			return false;
		}

		@Override
		public void onRequestDisallowInterceptTouchEvent(boolean b) {

		}

		@Override
		public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {

		}

		private void modeSingleColor(int position) {
			switch (mLevel) {
				case VIEW_LEVEL_SWATCHES:
					showColors(position);
					break;
				case VIEW_LEVEL_COLORS:
					mLevel = VIEW_LEVEL_SWATCHES;
					mSelectedItems.clear();
					mColorContainer.setPresetColor(mOpenSwatch, position);
					mSelectedItems.add(mColorContainer);
					saveSelectedItems();
					close();
					break;
			}
		}
	}
}
