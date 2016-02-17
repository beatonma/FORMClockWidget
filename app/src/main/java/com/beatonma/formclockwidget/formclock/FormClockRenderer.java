package com.beatonma.formclockwidget.formclock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.format.DateFormat;
import android.util.Log;

import static com.beatonma.formclockwidget.utility.MathUtils.accelerate5;
import static com.beatonma.formclockwidget.utility.MathUtils.decelerate5;
import static com.beatonma.formclockwidget.utility.MathUtils.interpolate;
import static com.beatonma.formclockwidget.utility.MathUtils.progress;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael Beaton on 25/10/2015.
 *
 * An extension of Roman Nurik's
 * {@see <a hrerf="https://github.com/romannurik/FORMWatchFace/blob/master/common/src/main/java/net/nurik/roman/formwatchface/common/FormClockRenderer.java">FormClockRenderer</a>}
 *
 * Added functionality includes rendering of date and alarm complications, alternative vertical
 * layout orientation, and rendering of a thin 'shadow' around content items.
 */
public class FormClockRenderer {
	private final static String TAG = "FormClockRenderer";

	public final static String ALARMFORMAT_TODAY_24 = "H:mm";
	public final static String ALARMFORMAT_TODAY_12 = "K:mm";

	public final static int ORIENTATION_HORIZONTAL = 0;
	public final static int ORIENTATION_VERTICAL = 1;
	public final static int ORIENTATION_FIT_DEVICE_ORIENTATION = 2;
	public final static int ORIENTATION_FIT_SPACE = 3;

	// Rendering stuff
	private Options mOptions;
	private ClockPaints mPaints;
	private Font mFont = new Font();
	private AlarmContainer mAlarm;

	// Glyphs representing current time.
	private Glyph[] mGlyphs = new Glyph[5];
	private int mGlyphCount = 0;

	// Which glyphs should animate and which we are currently animating.
	private int[] mAnimatedGlyphIndices = new int[5];
	private int mAnimatedGlyphIndexCount;

	// Total animation length and current progress (0 <= t <= 1) through that time
	private long mAnimDuration;
	private long mAnimTime;

	private PointF mMeasuredSize = new PointF();

	// Glyph outline
	private Canvas mShadowCanvas;
	private Bitmap mShadowBitmap;
    private int mShadowOpacity = 100;

	// Simple, colorless representation of the filled glyphs. By subtracting this from the shadow bitmap we can get an outline with no 'internal' outlines of a glyph's compositional parts
	private Canvas mSimpleCanvas;
	private Bitmap mSimpleBitmap;

	// The time string to be rendered. Can be any combination of numbers and colons
	private String mTime = "";

	public FormClockRenderer(Options options) {
		this.mOptions = options;
	}

	public FormClockRenderer(Options options, ClockPaints paints) {
		mOptions = options;
		mPaints = paints;

		if (mPaints != null) {
			mPaints.shadow.setStrokeWidth(Math.max(mOptions.charSpacing / 3, mOptions.textSize / 40f));
            mShadowOpacity = mPaints.shadow.getAlpha();
            mPaints.shadow.setAlpha(255);
		}
	}

	public void setTextSize(int size) {
		mOptions.textSize = size;
	}

	public float getComplicationTextSize() {
//		return Math.min(mOptions.textSize / 4, mOptions.maxComplicationSize);
		return mOptions.textSize / 4;
	}

	public String getFormattedDate() {
		return getFormattedDate(Calendar.getInstance());
	}

	public String getFormattedDate(Calendar cal) {
		String formatted = DateFormat.format(mOptions.dateFormat, cal).toString();
		if (mOptions.uppercase) {
			formatted = formatted.toUpperCase();
		}
		return formatted;
	}

	public String getFormattedAlarm(Calendar cal) {
		String formatted = DateFormat.format(getAlarmFormat(), cal).toString();
		if (mOptions.uppercase) {
			formatted = formatted.toUpperCase();
		}
		return formatted;
	}

	public String getAlarmFormat() {
		String dayFormat = mOptions.dateFormat.replaceAll("[^eE]+", "");
		return dayFormat + " " + (mOptions.is24Hour ? ALARMFORMAT_TODAY_24 : ALARMFORMAT_TODAY_12);
	}

	public void setTime(String time) {
		String currentTime = mTime;
		mTime = time;
		if (currentTime.equals("")) {
			currentTime = mTime;
		}
		updateGlyphsAndAnimDuration(currentTime, mTime);
	}

	public void setTime(String currentTime, String nextTime) {
		mTime = nextTime;
		updateGlyphsAndAnimDuration(currentTime, nextTime);
	}

	public void setAlarmTime(long alarmTriggerTime) {
		mAlarm = new AlarmContainer(alarmTriggerTime);
	}

	public void setAlarmTime(String formattedAlarmTime) {
		mAlarm = new AlarmContainer(formattedAlarmTime);
	}

	public String getAlarm() {
		if (mAlarm == null) {
			return "No alarm set";
		}
		else {
			return mAlarm.asString;
		}
	}

	public void setAnimationTime(long time) {
		mAnimTime = time;
	}

	public Options getOptions() {
		return mOptions;
	}

	public PointF measure() {
		mMeasuredSize.set(0, 0);
		layoutPass(new LayoutPassCallback() {
			@Override
			public void visitGlyph(Glyph glyph, float glyphAnimProgress, RectF rect) {
				mMeasuredSize.x = Math.max(mMeasuredSize.x, rect.right);
				mMeasuredSize.y = Math.max(mMeasuredSize.y, rect.bottom);
			}
		}, new RectF());
		return mMeasuredSize;
	}

	public PointF synchronousMeasure() {
		mMeasuredSize.set(0, 0);

		float x;
		float y;
		float scale = mOptions.textSize / Font.DRAWHEIGHT;

		if (mOptions.orientation == ORIENTATION_HORIZONTAL) {
			x = 0;
			for (int i = 0; i < mGlyphCount; i++) {
				Glyph glyph = mGlyphs[i];
				float t = getGlyphAnimProgress(i);
				float glyphWidth = glyph.getWidthAtProgress(t) * scale;

				x += glyphWidth + (i > 0 ? mOptions.charSpacing : 0);
			}

			y = mOptions.textSize;
			if (mOptions.showDate || mOptions.showAlarm) {
				y += (mOptions.charSpacing * 2) + getComplicationTextSize();
			}

			mMeasuredSize.x = x;
			mMeasuredSize.y = y;
		}
		else if (mOptions.orientation == ORIENTATION_VERTICAL) {
			x = 0;
			y = (2 * mOptions.textSize) + mOptions.charSpacing;
			if (mOptions.showDate) {
				y += mOptions.charSpacing + getComplicationTextSize();
			}
			if (mOptions.showAlarm) {
				y += mOptions.charSpacing + getComplicationTextSize();
			}
			mMeasuredSize.y = y;

			for (int i = 0; i < mGlyphCount; i++) {
				Glyph glyph = mGlyphs[i];

				if (glyph.getCanonicalStartGlyph().equals(":")) {
					x = 0;
				}
				else {
					float t = getGlyphAnimProgress(i);
					float glyphWidth = glyph.getWidthAtProgress(t) * scale;
					if (x == 0) {
						x += mOptions.charSpacing;
					}

					x += glyphWidth;
					mMeasuredSize.x = Math.max(mMeasuredSize.x, x);
				}
			}
		}

		return mMeasuredSize;
	}

	// Find the largest size that any time can be with the current Options
	// Widest Glyph is 0_1 at ~0.5 progress but in horizontal mode we use 1_2 because
	// four zeroes cannot appear together.
	public PointF getMaxSize() {
		float scale = mOptions.textSize / Font.DRAWHEIGHT;

		Glyph widestDigit;

		if (mOptions.orientation == ORIENTATION_VERTICAL) {
			widestDigit = mFont.getGlyph("0_1");
		}
		else {
			widestDigit = mFont.getGlyph("1_2");
		}

		Glyph colon = mFont.getGlyph(":");
		float widestGlyph = widestDigit.getWidthAtProgress(0.5f) * scale;
		float colonWidth = colon.getWidthAtProgress(0f) * scale;

		float totalWidth;
		float height;
		if (mOptions.orientation == ORIENTATION_VERTICAL) {
			totalWidth = (2 * widestGlyph)
					+ (3 * mOptions.charSpacing);
			height = (2 * mOptions.textSize)
					+ (3 * mOptions.charSpacing);

			if (mOptions.showDate) {
				height += mOptions.charSpacing
						+ getComplicationTextSize()
						+ mPaints.complications.descent();
			}
			if (mOptions.showAlarm) {
				height += mOptions.charSpacing
						+ getComplicationTextSize()
						+ mPaints.complications.descent();
			}
		}
		else {
			totalWidth = (4 * widestGlyph)
					+ colonWidth
					+ (6 * mOptions.charSpacing);
			height = mOptions.textSize
					+ (2 * mOptions.charSpacing);

			if (mOptions.showDate || mOptions.showAlarm) {
				height += mOptions.charSpacing
						+ getComplicationTextSize()
						+ mPaints.complications.descent();
			}
		}

		float complicationWidth = 0;
		mPaints.complications.setTextSize(getComplicationTextSize());
		float dateWidth = 0;
		float alarmWidth = 0;

		if (mOptions.showDate) {
			Calendar widestDate = Calendar.getInstance();
			widestDate.set(Calendar.MONTH, Calendar.SEPTEMBER);
			widestDate.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
			widestDate.set(Calendar.DAY_OF_MONTH, 23);
			dateWidth = mPaints.complications.measureText(getFormattedDate(widestDate));
		}
		if (mOptions.showAlarm) {
			Calendar widestAlarm = Calendar.getInstance();
			widestAlarm.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
			widestAlarm.set(Calendar.HOUR_OF_DAY, 22);
			widestAlarm.set(Calendar.MINUTE, 22);
			alarmWidth = mPaints.complications.measureText(getFormattedAlarm(widestAlarm))
					+ getComplicationTextSize();
		}

		complicationWidth = mOptions.orientation == ORIENTATION_HORIZONTAL
				? dateWidth + alarmWidth + mPaints.complications.measureText("  ")
				: Math.max(dateWidth, alarmWidth);

		totalWidth = Math.max(totalWidth, complicationWidth);

		return new PointF(totalWidth, height);
	}

	// find the largest test size that will fit completely within given bounds
	public int getMaxTextSize(PointF maxBounds) {
		int textSize = 1;

		PointF measuredDimensions = new PointF(1, 1);

		while (textFits(measuredDimensions, maxBounds)) {
			setTextSize(textSize++);
			measuredDimensions = getMaxSize();
		}

		textSize -= 1;

		return textSize;
	}

	public int getMaxEggTextSize(PointF maxBounds) {
		int textSize = 1;

		PointF measuredDimensions = new PointF(1, 1);

		while(textFits(measuredDimensions, maxBounds)) {
			setTextSize(textSize++);
			measuredDimensions = getMaxEggSize();
		}

		textSize -= 1;
		textSize *= 0.9;
		return textSize;
	}

	private PointF getMaxEggSize() {
		float scale = mOptions.textSize / Font.DRAWHEIGHT;
		return new PointF((Font.DRAWHEIGHT + (mOptions.charSpacing * 2)) * scale, mOptions.textSize);
	}

	private boolean textFits(PointF measuredTextSize, PointF maxDimensions) {
		if (measuredTextSize.x > maxDimensions.x) {
			return false;
		}
		if (measuredTextSize.y > maxDimensions.y) {
			return false;
		}
		return true;
	}

	public void debugFindWidestDigit() {
		int widest = 0;
		float widestWidth = 0f;
		float progress = -1f;

		for (int i = 0; i < 10; i++) {
			Glyph glyph = mFont.getGlyph(String.valueOf(i));
			for (float j = 0f; j <= 1f; j += 0.05f) {
				float width = glyph.getWidthAtProgress(j);

				Log.d(TAG, "glyph " + i + " width = " + width + " at progress = " + j);
				if (width > widestWidth) {
					widest = i;
					widestWidth = width;
					progress = j;
				}
			}
		}

		Log.d(TAG, "widest digit = " + widest + "; widest width = " + widestWidth + " at progress = " + progress);
	}

	public long getAnimDuration() {
		return mAnimDuration;
	}

	public void draw(final Canvas canvas, float left, float top) {
		Bitmap tempBitmap = null;
		Canvas tempCanvas = null;
		Canvas canvasArray[];

		if (mOptions.showShadow) {
			mShadowBitmap = Bitmap.createBitmap(
					canvas.getWidth(),
					canvas.getHeight(),
					Bitmap.Config.ARGB_8888);
			mShadowCanvas = new Canvas(mShadowBitmap);

			mSimpleBitmap = Bitmap.createBitmap(
					canvas.getWidth(),
					canvas.getHeight(),
					Bitmap.Config.ARGB_8888);
			mSimpleCanvas = new Canvas(mSimpleBitmap);

			tempBitmap = Bitmap.createBitmap(
					canvas.getWidth(),
					canvas.getHeight(),
					Bitmap.Config.ARGB_8888);
			tempCanvas = new Canvas(tempBitmap);

			canvasArray = new Canvas[] { mShadowCanvas, mSimpleCanvas, tempCanvas };
		}
		else {
			canvasArray = new Canvas[] { canvas };
		}

		for (final Canvas c : canvasArray) {
			mFont.canvas = c;
			int sc = c.save();
			c.translate(left, top);

			RectF bounds = new RectF();

			layoutPass(new LayoutPassCallback() {
				@Override
				public void visitGlyph(Glyph glyph, float glyphAnimProgress, RectF rect) {

					if (glyphAnimProgress == 0) {
						glyph = mFont.mGlyphMap.get(glyph.getCanonicalStartGlyph());
					}
					else if (glyphAnimProgress == 1) {
						glyph = mFont.mGlyphMap.get(glyph.getCanonicalEndGlyph());
						glyphAnimProgress = 0;
					}

					int sc = c.save();
					c.translate(rect.left, rect.top);
					float scale = mOptions.textSize / Font.DRAWHEIGHT;
					c.scale(scale, scale);
					glyph.draw(glyphAnimProgress);
					c.restoreToCount(sc);
				}
			}, bounds);

			c.restoreToCount(sc);

			float complicationSize = getComplicationTextSize();
			float x = 0;
			float y = bounds.bottom + (mOptions.charSpacing * 2) + complicationSize;
			Paint paint = c.equals(mShadowCanvas) ? mPaints.shadow : mPaints.complications;
			paint.setTextSize(complicationSize);

			if (mOptions.orientation == ORIENTATION_HORIZONTAL
					&& mOptions.showDate && mOptions.showAlarm
					&& mAlarm != null && !mAlarm.asString.equals("")) {
				// Render date and alarm in a single line

				String date = getFormattedDate();
				String alarm = mAlarm.asString;

				float totalLength = paint.measureText(date + "  " + alarm) + complicationSize;
				x = (c.getWidth() - totalLength) / 2;
				c.drawText(date, x, y, paint);
				x += paint.measureText(date + "  ") + complicationSize;

				c.drawText(alarm, x, y, paint);
				mAlarm.draw(c,
						x - (complicationSize / 1.5f),
						y - (complicationSize / 3f),
						complicationSize,
						paint);
			}
			else {
				if (mOptions.showDate) {
					String date = getFormattedDate();
					x = (c.getWidth() - paint.measureText(date)) / 2;
					c.drawText(date, x, y, paint);
					if (mOptions.orientation == ORIENTATION_VERTICAL) {
						y += mOptions.charSpacing + complicationSize;
					}
				}
				if (mOptions.showAlarm) {
					if (mAlarm != null && !mAlarm.asString.equals("")) {
						String alarm = mAlarm.asString;
						x = (c.getWidth() - paint.measureText(alarm) + (complicationSize)) / 2;
						c.drawText(alarm, x, y, paint);
						mAlarm.draw(c,
								x - (complicationSize / 1.5f),
								y - (complicationSize / 3f),
								complicationSize,
								paint);
						if (mOptions.orientation == ORIENTATION_VERTICAL) {
							y += mOptions.charSpacing + complicationSize;
						}
					}
				}
			}
		}

		// Render shadows with support for alpha in colors
		if (mOptions.showShadow) {
			Bitmap tempOutlineBitmap = Bitmap.createBitmap(mShadowBitmap.getWidth(), mShadowBitmap.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas tempOutlineCanvas = new Canvas(tempOutlineBitmap);

			Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setAlpha(mShadowOpacity); // Re-apply shadow opacity after rendering the shape

			tempOutlineCanvas.drawBitmap(mShadowBitmap, 0, 0, p);

			p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
			tempOutlineCanvas.drawBitmap(mSimpleBitmap, 0, 0, p);

			canvas.drawBitmap(tempOutlineBitmap, 0, 0, null);
			canvas.drawBitmap(tempBitmap, 0, 0, null);

			tempOutlineBitmap.recycle();
			mSimpleBitmap.recycle();
			mShadowBitmap.recycle();
			tempBitmap.recycle();
		}

		// Proper code (old, no alpha support - deprecated as of 2016-01-04)
//		if (mOptions.showShadow) {
//			Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
//			p.setAlpha(SHADOW_ALPHA);
//
//			canvas.drawBitmap(mShadowBitmap, 0, 0, p);
//			canvas.drawBitmap(tempBitmap, 0, 0, null);
//
//			mShadowBitmap.recycle();
//			tempBitmap.recycle();
//		}

		mFont.canvas = null;
	}

	private void updateGlyphsAndAnimDuration(String currentTime, String nextTime) {
		int[] tempAnimatedGlyphIndices = new int[mGlyphs.length];
		mAnimatedGlyphIndexCount = 0;
		mGlyphCount = 0;

//		if (mOptions.orientation == ORIENTATION_VERTICAL) {
		if (mOptions.zeroPadding) {
			currentTime = currentTime.replace(" ", "0");
			nextTime = nextTime.replace(" ", "0");
		}

		int len = currentTime.length();
		for (int i = 0; i < len; i++) {
			char c1 = currentTime.charAt(i);
			char c2 = nextTime.charAt(i);

			if (c1 == '#') {
				tempAnimatedGlyphIndices[mAnimatedGlyphIndexCount++] = i;
				mGlyphs[mGlyphCount++] = mFont.getGlyph("#");
				continue;
			}

			if (c1 == ':') {
				mGlyphs[mGlyphCount++] = mFont.getGlyph(":");
				continue;
			}

			if (c1 == c2) {
				mGlyphs[mGlyphCount++] = mFont.getGlyph(String.valueOf(c1));
			}
			else {
				tempAnimatedGlyphIndices[mAnimatedGlyphIndexCount++] = i;
				mGlyphs[mGlyphCount++] = mFont.getGlyph(c1 + "_" + c2);
			}
		}

		// reverse animated glyph indices
		for (int i = 0; i < mAnimatedGlyphIndexCount; i++) {
			mAnimatedGlyphIndices[i] = tempAnimatedGlyphIndices[mAnimatedGlyphIndexCount - i - 1];
		}

		mAnimDuration = mAnimatedGlyphIndexCount * mOptions.glyphAnimAverageDelay
				+ mOptions.glyphAnimDuration;
		mAnimTime = 0;
	}

	private void layoutPass(LayoutPassCallback cb, RectF rectF) {
		switch (mOptions.orientation) {
			case ORIENTATION_HORIZONTAL:
				layoutPassHorizontal(cb, rectF);
				break;
			case ORIENTATION_VERTICAL:
				layoutPassVertical(cb, rectF);
				break;
			default:
				layoutPassHorizontal(cb, rectF);
				break;
		}
	}

	private void layoutPassHorizontal(LayoutPassCallback cb, RectF rectF) {
		float x = 0;
		float scale = mOptions.textSize / Font.DRAWHEIGHT;

		for (int i = 0; i < mGlyphCount; i++) {
			Glyph glyph = mGlyphs[i];

			float t = getGlyphAnimProgress(i);
			float glyphWidth = glyph.getWidthAtProgress(t) * scale;

			rectF.set(x, 0, x + glyphWidth, mOptions.textSize);
			cb.visitGlyph(glyph, t, rectF);

			x += Math.floor(glyphWidth +
					(i >= 0 ? mOptions.charSpacing : 0));
		}
	}

	private void layoutPassVertical(LayoutPassCallback cb, RectF rectF) {
		float x = 0;
		float y = 0;

		int charsOnLine = 0;
		float scale = mOptions.textSize / Font.DRAWHEIGHT;

		float topWidth = getNaiveWidth(mGlyphs[0], scale) + getNaiveWidth(mGlyphs[1], scale);
		float bottomWidth = getNaiveWidth(mGlyphs[3], scale) + getNaiveWidth(mGlyphs[4], scale);

		if (bottomWidth > topWidth) {
			x = ((bottomWidth - topWidth)) / 2f;
		}

		for (int i = 0; i < mGlyphCount; i++) {
			Glyph glyph = mGlyphs[i];
			if (glyph.getCanonicalStartGlyph().equals(":")) { // replace colon with new line
				if (topWidth > bottomWidth) {
					x = ((topWidth - bottomWidth)) / 2f;
				}
				else {
					x = 0;
				}
				y = mOptions.textSize + mOptions.charSpacing;
				charsOnLine = 0;
			}
			else {
				float t = getGlyphAnimProgress(i);
				float glyphWidth = glyph.getWidthAtProgress(t) * scale;

				rectF.set(x, y, x + glyphWidth, y + mOptions.textSize);
				cb.visitGlyph(glyph, t, rectF);

				x += Math.floor(glyphWidth +
						(charsOnLine >= 0 ? mOptions.charSpacing : 0));
				charsOnLine += 1;
			}
		}
	}

	private float getGlyphAnimProgress(int glyphIndex) {
		int indexIntoAnimatedGlyphs = -1;
		for (int i = 0; i < mAnimatedGlyphIndexCount; i++) {
			if (mAnimatedGlyphIndices[i] == glyphIndex) {
				indexIntoAnimatedGlyphs = i;
				break;
			}
		}

		// Glyph not in list of updated glyphs so does not need to animate
		if (indexIntoAnimatedGlyphs < 0) {
			return 0;
		}

		float glyphStartAnimTime = 0;
		if (mAnimatedGlyphIndexCount > 1) {
			glyphStartAnimTime = interpolate(accelerate5(indexIntoAnimatedGlyphs * 1f / (mAnimatedGlyphIndexCount - 1)),
					0, mAnimDuration - mOptions.glyphAnimDuration);
		}
		return progress(mAnimTime, glyphStartAnimTime, glyphStartAnimTime + mOptions.glyphAnimDuration);
	}

	// Get static width (not accounting for changes made by animation)
	private float getNaiveWidth(Glyph glyph, float scale) {
		if (glyph == null) {
			return 0;
		}
		else {
			return glyph.getWidthAtProgress(0) * scale;
		}
	}

	private interface LayoutPassCallback {
		void visitGlyph(Glyph glyph, float glyphAnimProgress, RectF rect);
	}

	public static class Options {
		public float textSize;
		public float maxComplicationSize;
		public float charSpacing;
		public boolean is24Hour;
		public boolean zeroPadding;
		public int orientation;
		public int glyphAnimDuration;
		public int glyphAnimAverageDelay;
		public boolean showShadow;
		public boolean showAlarm;
		public boolean showDate;
		public String dateFormat;
		public boolean uppercase;
		public boolean allowTransparency;

		public Options() {

		}

		public Options(Options copy) {
			this.textSize = copy.textSize;
			this.maxComplicationSize = copy.maxComplicationSize;
			this.charSpacing = copy.charSpacing;
			this.zeroPadding = copy.zeroPadding;
			this.is24Hour = copy.is24Hour;
			this.orientation = copy.orientation;
			this.glyphAnimDuration = copy.glyphAnimDuration;
			this.glyphAnimAverageDelay = copy.glyphAnimAverageDelay;
			this.showShadow = copy.showShadow;
			this.showAlarm = copy.showAlarm;
			this.showDate = copy.showDate;
			this.dateFormat = copy.dateFormat;
			this.uppercase = copy.uppercase;
			this.allowTransparency = copy.allowTransparency;
		}

		@Override
		public String toString() {
			return "\ntextSize=" + textSize
					+ "\nmaxComplicationSize=" + maxComplicationSize
					+ "\ncharSpacing=" + charSpacing
					+ "\nis24Hour=" + is24Hour
					+ "\nzeroPadding=" + zeroPadding
					+ "\norientation=" + orientation
					+ "\nglyphAnimDuration=" + glyphAnimDuration
					+ "\nglyphAnimAverageDelay=" + glyphAnimAverageDelay
					+ "\nshowShadow=" + showShadow
					+ "\nshowAlarm=" + showAlarm
					+ "\nshowDate=" + showDate
					+ "\ndateFormat=" + dateFormat
					+ "\nuppercase=" + uppercase
					+ "\nallowTransparency=" + allowTransparency;
		}

		// Determine if this Options will render at the same size as another Options object
		public boolean isSameSize(Options other) {
			if (this.textSize != other.textSize) {
				return false;
			}
			if (this.showAlarm != other.showAlarm) {
				return false;
			}
			if (this.showDate != other.showDate) {
				return false;
			}
			if (this.orientation != other.orientation) {
				return false;
			}
			if (this.uppercase != other.uppercase) {
				return false;
			}
			if (!this.dateFormat.equals(other.dateFormat)) {
				return false;
			}
			if (this.is24Hour != other.is24Hour) {
				return false;
			}
			if (this.zeroPadding != other.zeroPadding) {
				return false;
			}
			return true;
		}
	}

	public static class ClockPaints {
		public Paint fills[] = new Paint[3];
		public Paint complications = new Paint();
		public Paint shadow = new Paint();
		public Paint simple = new Paint(Paint.ANTI_ALIAS_FLAG);

	}

	public class AlarmContainer {
		private long triggerTime;
		private int hour = 3;
		private int minute = 0;
		public String asString = "";

		public AlarmContainer(long triggerTime) {
			this.triggerTime = triggerTime;
			init();
		}

		public AlarmContainer(String formattedTriggerTime) {
			this.asString = formattedTriggerTime;
		}

		private void init() {
			Calendar time = Calendar.getInstance();
			String now = "" + time.get(Calendar.DAY_OF_MONTH) + time.get(Calendar.MONTH) + time.get(Calendar.YEAR);
			time.setTimeInMillis(triggerTime);
			String alarmTime = "" + time.get(Calendar.DAY_OF_MONTH) + time.get(Calendar.MONTH) + time.get(Calendar.YEAR);

			String alarmFormat;

			if (now.equals(alarmTime)) {
				alarmFormat = mOptions.is24Hour ? ALARMFORMAT_TODAY_24 : ALARMFORMAT_TODAY_12;
			}
			else {
				alarmFormat = getAlarmFormat();
			}

			asString = DateFormat.format(alarmFormat, time).toString();
			if (mOptions.uppercase) {
				asString = asString.toUpperCase();
			}
			hour = time.get(Calendar.HOUR);
			minute = time.get(Calendar.MINUTE);
		}

		public void draw(Canvas canvas, float x, float y, float size, Paint paint) {
			Paint.Style paintStyle = paint.getStyle();
			float strokeWidth = paint.getStrokeWidth();

			float radius = size / 2 * 0.8f;
			float hourRadius = radius * 0.4f;
			float minuteRadius = radius * 0.8f;

			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(radius / (canvas.equals(mShadowCanvas) ? 3 : 6));

			// body
			canvas.drawCircle(x, y, radius, paint);

			// bells
			PointF center = new PointF(x, y);
			PointF point = new PointF(x, y - (size / 2));
			PointF start = rotateAround(point, center, 25);
			PointF end = rotateAround(point, center, 55);

			canvas.drawLine(start.x, start.y, end.x, end.y, paint);

			start = rotateAround(point, center, -25);
			end = rotateAround(point, center, -55);

			canvas.drawLine(start.x, start.y, end.x, end.y, paint);

			paint.setStrokeWidth(radius / (canvas.equals(mShadowCanvas) ? 4 : 8));

			// minute hand
			float endX = (float) (x + minuteRadius * Math.sin(2 * Math.PI * minute / 60));
			float endY = (float) (y - minuteRadius * Math.cos(2 * Math.PI * minute / 60));

			canvas.drawLine(x, y, endX, endY, paint);

			// hour hand
			endX = (float) (x + hourRadius * Math.sin(2 * Math.PI * getTotalSeconds() / 43200));
			endY = (float) (y - hourRadius * Math.cos(2 * Math.PI * getTotalSeconds() / 43200));

			canvas.drawLine(x, y, endX, endY, paint);

			// Restore paint attributes
			paint.setStyle(paintStyle);
			paint.setStrokeWidth(strokeWidth);
		}

		// get number of seconds from noon/midnight to alarm time
		private int getTotalSeconds() {
			return (hour * 60 * 60) + (minute * 60);
		}

		private PointF rotateAround(PointF point, PointF center, float angle) {
			float cos = (float) Math.cos(Math.toRadians(angle));
			float sin = (float) Math.sin(Math.toRadians(angle));

			float x = (center.x + cos * (point.x - center.x)) - (sin * (point.y - center.y));
			float y = (center.y + sin * (point.x - center.x)) + (cos * (point.y - center.y));

			return new PointF(x, y);
		}
	}

	public interface Glyph {
		void draw(float t);
		float getWidthAtProgress(float t);
		String getCanonicalStartGlyph();
		String getCanonicalEndGlyph();
	}

	/**
	 * Font data and common drawing operations.
	 */
	private class Font {
		private static final int DRAWHEIGHT = 144;

		private static final int COLOR_1 = 0;
		private static final int COLOR_2 = 1;
		private static final int COLOR_3 = 2;

		private Map<String, Glyph> mGlyphMap = new HashMap<>();

		public Canvas canvas;
		private Path path = new Path();

		private RectF tempRectF = new RectF();

		public Font() {
			initGlyphs();
		}

		public Glyph getGlyph(String key) {
			Glyph glyph = mGlyphMap.get(key);
			if (glyph == null) {
				return mGlyphMap.get("0_1");
			}
			return glyph;
		}

		private Paint getPaint(int color) {
			if (canvas.equals(mShadowCanvas)) {
				return mPaints.shadow;
			}
			else if (canvas.equals(mSimpleCanvas)) {
				return mPaints.simple;
			}
			else {
				return mPaints.fills[color];
			}
		}

		private void scaleUniform(float s, float px, float py) {
			canvas.scale(s, s, px, py);
		}

        /*
            API 21 compat methods
         */

		private void arcTo(float l, float t, float r, float b, float startAngle, float sweepAngle, boolean forceMoveTo) {
			tempRectF.set(l, t, r, b);
			path.arcTo(tempRectF, startAngle, sweepAngle, forceMoveTo);
		}

		private void drawArc(float l, float t, float r, float b, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
			tempRectF.set(l, t, r, b);
			canvas.drawArc(tempRectF, startAngle, sweepAngle, useCenter, paint);
		}

		private void drawArcShadow(float l, float t, float r, float b, float startAngle, float sweepAngle, boolean useCenter) {
			tempRectF.set(l, t, r, b);
			canvas.drawArc(tempRectF, startAngle, sweepAngle, useCenter, mPaints.shadow);
		}

		private void drawRoundRect(float l, float t, float r, float b, float rx, float ry, Paint paint) {
			tempRectF.set(l, t, r, b);
			canvas.drawRoundRect(tempRectF, rx, ry, paint);
		}

		private void drawRoundRectShadow(float l, float t, float r, float b, float rx, float ry) {
			tempRectF.set(l, t, r, b);
			canvas.drawRoundRect(tempRectF, rx, ry, mPaints.shadow);
		}

		private void drawOval(float l, float t, float r, float b, Paint paint) {
			tempRectF.set(l, t, r, b);
			canvas.drawOval(tempRectF, paint);
		}

		private void drawOvalShadow(float l, float t, float r, float b) {
			tempRectF.set(l, t, r, b);
			canvas.drawOval(tempRectF, mPaints.shadow);
		}

        /*
            Stroke + fill drawing wrappers
         */

		private void drawArc(float l, float t, float r, float b, float startAngle, float sweepAngle, boolean useCenter, int color) {
			drawArc(l, t, r, b, startAngle, sweepAngle, useCenter, getPaint(color));
		}

		private void drawRoundRect(float l, float t, float r, float b, float rx, float ry, int color) {
			drawRoundRect(l, t, r, b, rx, ry, getPaint(color));
		}

		private void drawOval(float l, float t, float r, float b, int color) {
			drawOval(l, t, r, b, getPaint(color));
		}

		private void drawRect(float l, float t, float r, float b, int color) {
			canvas.drawRect(l, t, r, b, getPaint(color));
		}

		private void drawPath(Path path, int color) {
			canvas.drawPath(path, getPaint(color));
		}

		private void initGlyphs() {
			mGlyphMap.put("0_1", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "0";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "1";
				}

				@Override
				public void draw(float t) {
					float d1 = decelerate5(progress(t, 0, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					// 0
					canvas.save();

					// temporarily make space for the squashed zero
					canvas.translate(interpolate(d1, 0, interpolate(d2, 24, 0)), 0);

					scaleUniform(interpolate(d1, 1, 2f / 3), 72, 144);
					scaleUniform(interpolate(d2, 1, 0.7f), 72, 96);
					canvas.rotate(interpolate(d1, 45, 0), 72, 72);

					float stretchX = interpolate(d1, 0, interpolate(d2, 72, -36));

					path.reset();
					path.moveTo(72 - stretchX, 144);
					arcTo(-stretchX, 0, 144 - stretchX, 144, 90, 180, true);
					path.lineTo(72 + stretchX, 0);
					path.lineTo(72 + stretchX, 144);
					path.lineTo(72 - stretchX, 144);
					path.close();
					drawPath(path, COLOR_2);

					path.reset();
					arcTo(stretchX, 0, 144 + stretchX, 144, -90, 180, true);
					path.close();
					drawPath(path, COLOR_3);

					canvas.restore();

					// 1
					if (d2 > 0) {
						drawRect(
								interpolate(d2, 28, 0), interpolate(d2, 72, 0), 100, interpolate(d2, 144, 48),
								COLOR_2);

						drawRect(28, interpolate(d2, 144, 48), 100, 144,
								COLOR_3);
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(
							decelerate5(progress(t, 0.5f, 1)),
							interpolate(decelerate5(progress(t, 0, 0.5f)), 144, 192), 100);
				}
			});

			mGlyphMap.put("1_2", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "1";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "2";
				}

				@Override
				public void draw(float t) {
					float d = 1 - decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.3f, 0.8f));
					float d2 = decelerate5(progress(t, 0.5f, 1.0f));

					// 2
					if (d1 > 0) {
						canvas.save();
						canvas.translate(interpolate(d2, 72, 0), 0);
						path.reset();
						path.moveTo(0, 144);
						path.lineTo(72, 72);
						path.lineTo(72, 144);
						path.lineTo(0, 144);
						drawPath(path, COLOR_3);
						canvas.restore();

						canvas.save();
						// TODO: interpolate colors
						//ctx.fillStyle = interpolateColors(d2, o.color2, o.color1);
						canvas.translate(108, interpolate(d1, 72, 0));
						//drawHorzHalfCircle(0, 0, 36, 72, true);
						drawArc(-36, 0, 36, 72, -90, 180, true, COLOR_1);
						canvas.restore();

						canvas.save();
						canvas.translate(0, interpolate(d1, 72, 0));
						drawRect(interpolate(d2, 72, 8), 0, interpolate(d2, 144, 108), 72, COLOR_1);
						canvas.restore();

						drawRect(72, 72, 144, 144, COLOR_2);
					}

					// 1
					if (d > 0) {
						canvas.save();
						canvas.translate(interpolate(d, 44, 0), 0);
						drawRect(interpolate(d, 28, 0), interpolate(d, 72, 0), 100, interpolate(d, 144, 48), COLOR_2);
						drawRect(28, interpolate(d, 144, 48), 100, 144, COLOR_3);
						canvas.restore();
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0f, 0.5f)), 100, 144);
				}
			});

			mGlyphMap.put("2_3", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "2";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "3";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.5f, 1.0f));

					// 2
					if (d < 1) {
						canvas.save();
						canvas.translate(interpolate(d, 0, -16), 0);

						canvas.save();
						canvas.translate(interpolate(d, 0, 72), 0);
						path.reset();
						if (canvas.equals(mShadowCanvas)) {
							path.moveTo(72, 144);
							path.lineTo(0, 144);
							path.lineTo(72, 72);
						}
						else {
							path.moveTo(0, 144);
							path.lineTo(72, 72);
							path.lineTo(72, 144);
							path.lineTo(0, 144);
						}
						drawPath(path, COLOR_3);
						canvas.restore();

						// TODO: interpolateColors
						//.fillStyle = interpolateColors(d, o.color1, o.color2);
						if (d == 0) {
							path.reset();
							path.moveTo(8, 0);
							path.lineTo(108, 0);
							arcTo(108 - 36, 0, 108 + 36, 72, -90, 180, true);
							path.lineTo(108, 72);
							path.lineTo(8, 72);
							path.lineTo(8, 0);
							path.close();
							drawPath(path, COLOR_1);
						} else {
							drawArc(108 - 36, interpolate(d, 0, 72),
									108 + 36, 72 + interpolate(d, 0, 72),
									-90, 180, true, COLOR_1);
							drawRect(interpolate(d, 8, 72), interpolate(d, 0, 72),
									interpolate(d, 108, 144), interpolate(d, 72, 144), COLOR_1);
						}
						drawRect(72, 72, 144, 144, COLOR_2);

						canvas.restore();
					} else {
						// 3
						// half-circle
						canvas.save();
						scaleUniform(interpolate(d1, 0.7f, 1), 128, 144);
						drawArc(32, 48, 128, 144, -90, 180, true, COLOR_3);
						canvas.restore();

						// bottom rectangle
						drawRect(
								interpolate(d1, 56, 0), interpolate(d1, 72, 96),
								interpolate(d1, 128, 80), interpolate(d1, 144, 144), COLOR_1);

						// top part with triangle
						canvas.save();
						canvas.translate(0, interpolate(d1, 72, 0));
						path.reset();
						path.moveTo(128, 0);
						path.lineTo(80, 48);
						path.lineTo(80, 0);
						path.close();
						drawPath(path, COLOR_3);
						drawRect(
								interpolate(d1, 56, 0), 0,
								interpolate(d1, 128, 80), interpolate(d1, 72, 48), COLOR_3);
						canvas.restore();

						// middle rectangle
						canvas.save();
						drawRect(
								interpolate(d1, 56, 32), interpolate(d1, 72, 48),
								interpolate(d1, 128, 80), interpolate(d1, 144, 96), COLOR_2);
						canvas.restore();
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0f, 0.5f)), 144, 128);
				}
			});

			mGlyphMap.put("3_4", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "3";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "4";
				}

				@Override
				public void draw(float t) {
					float d1 = 1 - decelerate5(progress(t, 0, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					// 3
					if (d1 > 0) {
						canvas.save();
						canvas.translate(interpolate(d1, 16, 0), 0);

						// middle rectangle
						canvas.save();
						drawRect(
								interpolate(d1, 56, 32), interpolate(d1, 72, 48),
								interpolate(d1, 128, 80), interpolate(d1, 144, 96), COLOR_2);
						canvas.restore();

						// half-circle
						canvas.save();
						scaleUniform(interpolate(d1, 0.7f, 1), 128, 144);
						drawArc(32, 48, 128, 144, -90, 180, true, COLOR_3);
						canvas.restore();

						// bottom rectangle
						drawRect(
								interpolate(d1, 56, 0), interpolate(d1, 72, 96),
								interpolate(d1, 128, 80), interpolate(d1, 144, 144), COLOR_1);

						// top part with triangle
						canvas.save();
						canvas.translate(0, interpolate(d1, 72, 0));
						path.reset();
						path.moveTo(80, 0);
						path.lineTo(128, 0);
						path.lineTo(80, 48);
						if (d1 == 1) {
							path.lineTo(0, 48);
							path.lineTo(0, 0);
							path.lineTo(80, 0);
							path.close();
							drawPath(path, COLOR_3);
						} else {
							path.close();
							drawPath(path, COLOR_3);
							drawRect(
									interpolate(d1, 56, 0), 0,
									interpolate(d1, 128, 80), interpolate(d1, 72, 48), COLOR_3);
						}
						canvas.restore();

						canvas.restore();
					} else {
						// 4
						// bottom rectangle
						drawRect(72, interpolate(d2, 144, 108), 144, 144, COLOR_2);

						// middle rectangle
						drawRect(interpolate(d2, 72, 0), interpolate(d2, 144, 72), 144, interpolate(d2, 144, 108), COLOR_1);

						// triangle
						canvas.save();
						scaleUniform(d2, 144, 144);
						path.reset();
						path.moveTo(72, 72);
						path.lineTo(72, 0);
						path.lineTo(0, 72);
						path.lineTo(72, 72);
						drawPath(path, COLOR_2);

						canvas.restore();

						// top rectangle
						drawRect(72, interpolate(d2, 72, 0), 144, interpolate(d2, 144, 72), COLOR_3);
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0.5f, 1f)), 128, 144);
				}
			});

			mGlyphMap.put("4_5", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "4";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "5";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.5f, 1));

					// 4
					if (d < 1) {
						// bottom rectangle
						drawRect(interpolate(d, 72, 0), 108, interpolate(d, 144, 72), 144, COLOR_2);

						// top rectangle
						drawRect(interpolate(d, 72, 0), interpolate(d, 0, 72),
								interpolate(d, 144, 72), interpolate(d, 72, 144), COLOR_3);

						// triangle
						canvas.save();
						scaleUniform(1 - d, 0, 144);
						path.reset();
						if (canvas.equals(mShadowCanvas)) {
							path.moveTo(72, -1);
							path.lineTo(-1, 72);
						}
						else {
							path.moveTo(72, 72);
							path.lineTo(72, 0);
							path.lineTo(0, 72);
							path.lineTo(72, 72);
						}
						drawPath(path, COLOR_2);

						canvas.restore();

						// middle rectangle
						drawRect(0, 72,
								interpolate(d, 144, 72), interpolate(d, 108, 144), COLOR_1);
					} else {
						// 5
						// wing rectangle
						canvas.save();
						drawRect(
								80, interpolate(d1, 72, 0),
								interpolate(d1, 80, 128), interpolate(d1, 144, 48), COLOR_2);
						canvas.restore();

						// half-circle
						canvas.save();
						scaleUniform(interpolate(d1, 0.75f, 1), 0, 144);
						canvas.translate(interpolate(d1, -48, 0), 0);
						drawArc(32, 48, 128, 144, -90, 180, true, COLOR_3);
						canvas.restore();

						// bottom rectangle
						drawRect(0, 96, 80, 144, COLOR_2);

						// middle rectangle
						drawRect(
								0, interpolate(d1, 72, 0),
								80, interpolate(d1, 144, 96), COLOR_1);
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0f, 0.5f)), 144, 128);
				}
			});

			mGlyphMap.put("5_6", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "5";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "6";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.7f));
					float d1 = decelerate5(progress(t, 0.1f, 1));

					// 5 (except half-circle)
					if (d < 1) {
						canvas.save();
						scaleUniform(interpolate(d, 1, 0.25f), 108, 96);

						// wing rectangle
						drawRect(80, 0, 128, 48, COLOR_2);

						// bottom rectangle
						drawRect(0, 96, 80, 144, COLOR_2);

						// middle rectangle
						drawRect(0, 0, 80, 96, COLOR_1);

						canvas.restore();
					}

					// half-circle
					canvas.save();

					canvas.rotate(interpolate(d1, 0, 90), 72, 72);

					if (d1 == 0) {
						drawArc(
								32, 48,
								128, 144, -90, 180, true, COLOR_3);
					} else {
						scaleUniform(interpolate(d1, 2f / 3, 1), 80, 144);
						canvas.translate(interpolate(d1, 8, 0), 0);
						drawArc(
								0, 0,
								144, 144, -90, 180, true, COLOR_3);
					}

					// 6 (just the parallelogram)
					if (d1 > 0) {
						canvas.save();
						canvas.rotate(-90, 72, 72);
						path.reset();
						path.moveTo(0, 72);
						path.lineTo(interpolate(d1, 0, 36), interpolate(d1, 72, 0));
						path.lineTo(interpolate(d1, 72, 108), interpolate(d1, 72, 0));
						path.lineTo(72, 72);
						path.lineTo(0, 72);
						drawPath(path, COLOR_2);

						canvas.restore();
					}

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0.1f, 1f)), 128, 144);
				}
			});

			mGlyphMap.put("6_7", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "6";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "7";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(t);

					// 7 rectangle
					drawRect(interpolate(d, 72, 0), 0, 72, 72, COLOR_3);

					// 6 circle
					canvas.save();

					canvas.translate(interpolate(d, 0, 36), 0);

					if (d < 1) {
						if (canvas.equals(mShadowCanvas)) {
							drawArc(0, 0, 144, 144,
									interpolate(d, 178, -64f),
									-178, true, COLOR_3);
						}
						else {
							drawArc(0, 0, 144, 144,
									interpolate(d, 180, -64f),
									-180, true, COLOR_3);
						}
					}

					// parallelogram
					path.reset();
					if (canvas.equals(mShadowCanvas)) {
						path.moveTo(0, 72);
						path.lineTo(36, 0); // left side
						path.lineTo(108, 0); // top side
						path.lineTo(72, 72); // right side
					}
					else {
						path.moveTo(36, 0);
						path.lineTo(108, 0);
						path.lineTo(interpolate(d, 72, 36), interpolate(d, 72, 144));
						path.lineTo(interpolate(d, 0, -36), interpolate(d, 72, 144));
						path.close();
					}
					drawPath(path, COLOR_2);

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 144;
				}
			});

			mGlyphMap.put("7_8", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "7";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "8";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.2f, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					// 8
					if (d1 > 0) {
						if (d2 > 0) {
							// top
							canvas.save();
							canvas.translate(0, interpolate(d2, 96, 0));
							drawRoundRect(24, 0, 120, 48, 24, 24, COLOR_3);
							canvas.restore();
						}

						// left bottom
						canvas.save();
						canvas.translate(interpolate(d1, 24, 0), 0);
						scaleUniform(interpolate(d2, 0.5f, 1), 48, 144);
						drawArc(0, 48, 96, 144, 90, 180, true, COLOR_1);
						canvas.restore();

						// right bottom
						canvas.save();
						canvas.translate(interpolate(d1, -24, 0), 0);
						scaleUniform(interpolate(d2, 0.5f, 1), 96, 144);
						drawArc(48, 48, 144, 144, -90, 180, true, COLOR_2);
						canvas.restore();

						// bottom middle
						canvas.save();
						canvas.scale(interpolate(d1, 0, 1), 1, 72, 0);
						drawRect(48, interpolate(d2, 96, 48), 96, 144, COLOR_1);
						drawRect(interpolate(d2, 48, 96), interpolate(d2, 96, 48), 96, 144, COLOR_2);
						canvas.restore();
					}

					if (d < 1) {
						// 7 rectangle
						drawRect(
								interpolate(d, 0, 48), interpolate(d, 0, 96),
								interpolate(d, 72, 96), interpolate(d, 72, 144), COLOR_3);

						// 7 parallelogram
						path.reset();
						path.moveTo(interpolate(d, 72, 48), interpolate(d, 0, 96));
						path.lineTo(interpolate(d, 144, 96), interpolate(d, 0, 96));
						path.lineTo(interpolate(d, 72, 96), 144);
						path.lineTo(interpolate(d, 0, 48), 144);
						path.close();
						drawPath(path, COLOR_2);

					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 144;
				}
			});

			mGlyphMap.put("8_9", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "8";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "9";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.5f, 1));

					// 8
					if (d < 1) {
						// top
						canvas.save();
						canvas.translate(0, interpolate(d, 0, 48));
						drawRoundRect(24, 0, 120, 48, 24, 24, COLOR_3);
						canvas.restore();

						if (d == 0) {
							// left + middle bottom
							canvas.save();
							path.reset();
							path.moveTo(48, 48);
							path.lineTo(96, 48);
							path.lineTo(96, 144);
							path.lineTo(48, 144);
							arcTo(0, 48, 96, 144, 90, 180, true);
							drawPath(path, COLOR_1);
							canvas.restore();

							// right bottom
							drawArc(48, 48, 144, 144, -90, 180, true, COLOR_2);
						} else {
							// bottom middle
							drawRect(interpolate(d, 48, 72) - 2, interpolate(d, 48, 0),
									interpolate(d, 96, 72) + 2, 144, COLOR_1);

							// left bottom
							canvas.save();
							scaleUniform(interpolate(d, 2f / 3, 1), 0, 144);
							drawArc(0, 0, 144, 144, 90, 180, true, COLOR_1);
							canvas.restore();

							// right bottom
							canvas.save();
							scaleUniform(interpolate(d, 2f / 3, 1), 144, 144);
							drawArc(0, 0, 144, 144, -90, 180, true, COLOR_2);
							canvas.restore();
						}
					} else {
						// 9
						canvas.save();

						canvas.rotate(interpolate(d1, -90, -180), 72, 72);

						// parallelogram
						path.reset();
						path.moveTo(0, 72);
						path.lineTo(interpolate(d1, 0, 36), interpolate(d1, 72, 0));
						path.lineTo(interpolate(d1, 72, 108), interpolate(d1, 72, 0));
						path.lineTo(72, 72);
						path.lineTo(0, 72);
						drawPath(path, COLOR_3);

						// vanishing arc
						drawArc(0, 0, 144, 144,
								-180,
								interpolate(d1, 180, 0), true, COLOR_1);

						// primary arc
						drawArc(0, 0, 144, 144, 0, 180, true, COLOR_2);

						canvas.restore();
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 144;
				}
			});

			mGlyphMap.put("9_0", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "9";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "0";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(t);

					// 9
					canvas.save();

					canvas.rotate(interpolate(d, -180, -225), 72, 72);

					// parallelogram
					canvas.save();
					path.reset();
					path.moveTo(0, 72);
					path.lineTo(interpolate(d, 36, 0), interpolate(d, 0, 72));
					path.lineTo(interpolate(d, 108, 72), interpolate(d, 0, 72));
					path.lineTo(72, 72);
					path.lineTo(0, 72);
					drawPath(path, COLOR_3);

					canvas.restore();

					// TODO: interpolate colors
					//ctx.fillStyle = interpolateColors(d, COLOR_1, COLOR_3);
					if (canvas.equals(mShadowCanvas)) {
						drawArc(0, 0, 144, 144, 0, 178, true, COLOR_2);
					}
					else {
						drawArc(0, 0, 144, 144,
								0, interpolate(d, 0, -180), true, COLOR_3);
						drawArc(0, 0, 144, 144, 0, 180, true, COLOR_2);
					}

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 144;
				}
			});

			mGlyphMap.put(" _1", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return " ";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "1";
				}

				@Override
				public void draw(float t) {
					float d1 = decelerate5(progress(t, 0, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					// 1
					scaleUniform(interpolate(d1, 0, 1), 0, 144);
					drawRect(
							interpolate(d2, 28, 0), interpolate(d2, 72, 0),
							100, interpolate(d2, 144, 48), COLOR_2);

					if (d2 > 0) {
						drawRect(28, interpolate(d2, 144, 48), 100, 144, COLOR_3);
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0, 0.5f)), 0, 100);
				}
			});

			mGlyphMap.put("1_ ", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "1";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return " ";
				}

				@Override
				public void draw(float t) {
					float d1 = decelerate5(progress(t, 0, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					scaleUniform(interpolate(d2, 1, 0), 0, 144);
					drawRect(
							interpolate(d1, 0, 28), interpolate(d1, 0, 72),
							100, interpolate(d1, 48, 144), COLOR_2);

					if (d1 < 1) {
						drawRect(28, interpolate(d1, 48, 144), 100, 144, COLOR_3);
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0.5f, 1)), 100, 0);
				}
			});

			// 24 hour only
			mGlyphMap.put("2_ ", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "2";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return " ";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.5f, 1.0f));

					// 2
					canvas.save();
					canvas.translate(interpolate(d, 0, -72), 0);

					if (d < 1) {
						canvas.save();
						canvas.translate(interpolate(d, 0, 72), 0);
						path.reset();
						path.moveTo(0, 144);
						path.lineTo(72, 72);
						path.lineTo(72, 144);
						path.lineTo(0, 144);
						drawPath(path, COLOR_3);
						canvas.restore();

						canvas.save();
						canvas.translate(0, interpolate(d, 0, 72));
						canvas.translate(108, 0);
						drawArc(-36, 0, 36, 72, -90, 180, true, COLOR_1);
						canvas.restore();

						canvas.save();
						drawRect(interpolate(d, 8, 72), interpolate(d, 0, 72),
								interpolate(d, 108, 144), interpolate(d, 72, 144), COLOR_1);
						canvas.restore();
					}

					canvas.save();
					scaleUniform(interpolate(d1, 1, 0), 72, 144);
					drawRect(72, 72, 144, 144, COLOR_2);
					canvas.restore();

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0, 0.5f)), 144,
							interpolate(decelerate5(progress(t, 0.5f, 1)), 72, 0));
				}
			});

			// 24 hour only
			mGlyphMap.put("3_0", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "3";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "0";
				}

				@Override
				public void draw(float t) {
					float d1 = 1 - decelerate5(progress(t, 0, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					canvas.save();
					canvas.rotate(interpolate(d2, 0, 45), 72, 72);
					canvas.translate(interpolate(d1, interpolate(d2, 16, -8), 0), 0);

					if (d1 > 0) {
						// top part of 3 with triangle
						canvas.save();
						canvas.translate(0, interpolate(d1, 48, 0));
						float x = interpolate(d1, 48, 0);
						path.reset();
						path.moveTo(128 - x, 0);
						path.lineTo(80 - x, 48);
						path.lineTo(80 - x, 0);
						drawPath(path, COLOR_3);
						drawRect(interpolate(d1, 32, 0), 0, 80, 48, COLOR_3);
						canvas.restore();
					}

					// bottom rectangle in 3
					drawRect(
							interpolate(d1, interpolate(d2, 32, 80), 0), 96,
							80, 144, COLOR_1);

					// middle rectangle
					drawRect(
							interpolate(d2, 32, 80), 48,
							80, 96, COLOR_2);

					// 0

					scaleUniform(interpolate(d2, 2f / 3, 1), 80, 144);

					// half-circles
					canvas.translate(8, 0);
					if (d2 > 0) {
						canvas.save();
						canvas.rotate(interpolate(d2, -180, 0), 72, 72);
						drawArc(
								0, 0,
								144, 144, 90, 180, true, COLOR_2);
						canvas.restore();
					}

					drawArc(
							0, 0,
							144, 144, -90, 180, true, COLOR_3);

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0, 0.5f)), 128, 144);
				}
			});

			mGlyphMap.put("5_0", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "5";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "0";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.5f, 1));

					canvas.save();
					canvas.rotate(interpolate(d1, 0, 45), 72, 72);

					// 5 (except half-circle)
					if (d < 1) {
						// wing rectangle
						canvas.save();
						drawRect(
								80, interpolate(d, 0, 48),
								interpolate(d, 128, 80), interpolate(d, 48, 144), COLOR_2);
						canvas.restore();

						// bottom rectangle
						drawRect(0, 96, 80, 144, COLOR_2);
					}

					// middle rectangle
					drawRect(
							interpolate(d1, 0, 80), interpolate(d, 0, interpolate(d1, 48, 0)),
							80, interpolate(d, 96, 144), COLOR_1);

					scaleUniform(interpolate(d1, 2f / 3, 1), 80, 144);

					// half-circles
					if (d1 > 0) {
						canvas.save();
						canvas.rotate(interpolate(d1, -180, 0), 72, 72);
						drawArc(
								0, 0,
								144, 144, 90, 180, true, COLOR_2);
						canvas.restore();
					}

					canvas.translate(interpolate(d1, 8, 0), 0);
					drawArc(
							0, 0,
							144, 144, -90, 180, true, COLOR_3);

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0, 0.5f)), 128, 144);
				}
			});

			mGlyphMap.put("2_1", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return "2";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "1";
				}

				@Override
				public void draw(float t) {
					float d = decelerate5(progress(t, 0, 0.5f));
					float d1 = decelerate5(progress(t, 0.2f, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					// 2
					if (d1 < 1) {
						canvas.save();
						canvas.translate(interpolate(d, 0, 28), 0);
						path.reset();
						path.moveTo(0, 144);
						path.lineTo(72, 72);
						path.lineTo(72, 144);
						path.lineTo(0, 144);
						drawPath(path, COLOR_3);
						canvas.restore();

						canvas.save();
						// TODO: interpolate colors
						//ctx.fillStyle = interpolateColors(d1, COLOR_1, COLOR_2);
						canvas.translate(interpolate(d, 108, 64), interpolate(d1, 0, 72));
						drawArc(-36, 0, 36, 72, -90, 180, true, COLOR_1);
						canvas.restore();

						canvas.save();
						canvas.translate(0, interpolate(d1, 0, 72));
						drawRect(interpolate(d, 8, 28), 0, interpolate(d, 108, 100), 72, COLOR_1);
						canvas.restore();

						canvas.save();
						canvas.translate(interpolate(d, 0, -44), 0);
						drawRect(72, 72, 144, 144, COLOR_2);
						canvas.restore();
					} else {
						// 1
						canvas.save();
						drawRect(interpolate(d2, 28, 0), interpolate(d2, 72, 0), 100, interpolate(d2, 144, 48), COLOR_2);

						drawRect(28, interpolate(d2, 144, 48), 100, 144, COLOR_3);
						canvas.restore();
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return interpolate(decelerate5(progress(t, 0, 0.5f)), 144, 100);
				}
			});

			mGlyphMap.put(":", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return ":";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return ":";
				}

				@Override
				public void draw(float t) {
					drawOval(0, 0, 48, 48, COLOR_2);
					drawOval(0, 96, 48, 144, COLOR_3);
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 48;
				}
			});

			mGlyphMap.put(" ", new Glyph() {
				@Override
				public String getCanonicalStartGlyph() {
					return " ";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return " ";
				}

				@Override
				public void draw(float t) {
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 0;
				}
			});

			// # -> <3
			// TODO POLISH THE EGG!
			mGlyphMap.put("#", new Glyph() {
				@Override
				public void draw(float t) {
					float d0 = decelerate5(progress(t, 0, 0.2f));
					float d1 = decelerate5(progress(t, 0.2f, 0.5f));
					float d2 = decelerate5(progress(t, 0.5f, 1));

					canvas.save();
					canvas.translate(0, 144 - 6);
					canvas.scale(1, -1);

					if (d1 == 0) {
						canvas.translate(interpolate(d0, 72, 0), 0);

						drawOval(interpolate(d0, 0, 21),
								interpolate(d0, 0, 42),
								interpolate(d0, 0, 63),
								interpolate(d0, 0, 0), COLOR_3);

						drawOval(interpolate(d0, 0, 81),
								interpolate(d0, 0, 42),
								interpolate(d0, 0, 123),
								interpolate(d0, 0, 0), COLOR_2);

						drawRect(interpolate(d0, 0, 42),
								interpolate(d0, 0, 42),
								interpolate(d0, 0, 102),
								interpolate(d0, 0, 0), COLOR_3);
					}
					else if (d2 == 0) {
						drawOval(interpolate(d1, 21, 51),
								interpolate(d1, 42, 42),
								interpolate(d1, 63, 51),
								interpolate(d1, 0, 0), COLOR_3);

						drawOval(interpolate(d1, 81, 93),
								interpolate(d1, 42, 42),
								interpolate(d1, 123, 93),
								interpolate(d1, 0, 0), COLOR_2);

						drawRect(interpolate(d1, 42, 51),
								interpolate(d1, 42, 42),
								interpolate(d1, 102, 93),
								interpolate(d1, 0, 0), COLOR_3);
					}
					else {
						canvas.translate(interpolate(d2, 0, 72), 0);
						canvas.rotate(interpolate(d2, 0, 45));

						drawOval(interpolate(d2, 42, 0),
								interpolate(d2, 42, 126),
								interpolate(d2, 114, 84),
								interpolate(d2, 0, 42), COLOR_1);

						drawOval(interpolate(d2, 93, 42),
								interpolate(d2, 42, 84),
								interpolate(d2, 93, 126),
								interpolate(d2, 0, 0), COLOR_2);

						drawRect(interpolate(d2, 51, 0),
								interpolate(d2, 42, 84),
								interpolate(d2, 93, 84),
								interpolate(d2, 0, 0), COLOR_3);

						canvas.restore();
					}
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 144;
				}

				@Override
				public String getCanonicalStartGlyph() {
					return "#";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "<3";
				}
			});

			// Static heart
			mGlyphMap.put("<3", new Glyph() {
				@Override
				public void draw(float t) {
					canvas.save();

					canvas.translate(0, 144 - 6);
					canvas.scale(1, -1);
					canvas.translate(72, 0);
					canvas.rotate(45);
					drawOval(0, 126, 84, 42, COLOR_1);
					drawOval(42, 84, 126, 0, COLOR_2);
					drawRect(0, 84, 84, 0, COLOR_3);

					canvas.restore();
				}

				@Override
				public float getWidthAtProgress(float t) {
					return 144;
				}

				@Override
				public String getCanonicalStartGlyph() {
					return "<3";
				}

				@Override
				public String getCanonicalEndGlyph() {
					return "<3";
				}
			});

			mGlyphMap.put("0", mGlyphMap.get("0_1"));
			mGlyphMap.put("1", mGlyphMap.get("1_2"));
			mGlyphMap.put("2", mGlyphMap.get("2_3"));
			mGlyphMap.put("3", mGlyphMap.get("3_4"));
			mGlyphMap.put("4", mGlyphMap.get("4_5"));
			mGlyphMap.put("5", mGlyphMap.get("5_6"));
			mGlyphMap.put("6", mGlyphMap.get("6_7"));
			mGlyphMap.put("7", mGlyphMap.get("7_8"));
			mGlyphMap.put("8", mGlyphMap.get("8_9"));
			mGlyphMap.put("9", mGlyphMap.get("9_0"));
		}
	}
}
