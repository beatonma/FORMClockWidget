package com.beatonma.colorpicker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.AnimationUtils;
import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 15/02/2015.
 */
public class PatchView extends ImageView {
    private final static String TAG = "ColorPicker";
    private final static int ALPHA = 120;

    private Context mContext;
    int mWidth = -1;
    int mHeight = -1;
    int mOuterRadius; // border
    int mInnerRadius; // main
    int mDiffRadius; // difference between above radii

    RectF mSelectedRingBounds; // Boundary in which to draw the arc when this item is selected
    Point mCenter;
    Paint mPaint;

    int mNewColor = -1;
    int mColor = -1;

    // Position in layout
    int mPosition = 0;

    boolean mTouched = false;

    boolean mFirstDraw = true;
    int mStartingAngle = 0;
    boolean mAnimationSelect = false;
    boolean mAnimationDeselect = false;
    int mSelectedRingDelay = 0;
    float mEnterDelay = 0f;

    boolean mEnableRipple = true; // Allow ripples to be drawn
    boolean mTouchDown = false; // Is a finger currently touching this view
    Paint mRipplePaint;
    RectF mMaskRingBounds; // Boundary for drawing mask (to cover any ripple paint going over the edge)
    float mHotspotX = -1;
    float mHotspotY = -1;
    float mRippleRadius = -1;
    int mRippleRate = 1;
    Handler mHandler = new Handler();
    Runnable mRippleRunner = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

	boolean mIsPreview = false;

    public PatchView(Context context) {
        this(context, null);
    }

    public PatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initPaint();
    }

    @Override
    protected void onDraw(final Canvas c) {
        if (mWidth == -1 || mHeight == -1 || mCenter == null) {
            mWidth = c.getWidth();
            mHeight = c.getHeight();
            mCenter = new Point(mWidth / 2, mHeight / 2);

            mOuterRadius = Math.min(mWidth, mHeight) / 2;
            mInnerRadius = mOuterRadius - Utils.dpToPx(mContext, 6);
            mDiffRadius = mOuterRadius - mInnerRadius;

            int padding = Utils.dpToPx(mContext, 2);
            mSelectedRingBounds = new RectF(padding, padding, mWidth - padding, mHeight - padding);
            if (isSelected()) {
                mMaskRingBounds = new RectF(-padding, -padding, mWidth + padding, mHeight + padding);
            }
            else {
                padding = Utils.dpToPx(mContext, 32);
                mMaskRingBounds = new RectF(-padding, -padding, mWidth + padding, mHeight + padding);
            }
            mStartingAngle = (int) (Math.floor(Math.random() * 360));
        }

        int size = Math.round(mEnterDelay * mOuterRadius);
        c.drawCircle(mCenter.x, mCenter.y, size, normalPaint());

        if (mEnableRipple) {
            drawRipple(c);
        }

        if (isSelected()) {
            if (mAnimationSelect) {
                c.drawArc(mSelectedRingBounds, mStartingAngle, mSelectedRingDelay, false, blankPaint());
                c.drawArc(mSelectedRingBounds, mStartingAngle, mSelectedRingDelay, false, selectedPaint());
            }
            else {
                mAnimationSelect = true;
                mAnimationDeselect = false;
                animateSelectionRing(true);
            }
        }
        else {
            if (mFirstDraw) {
                return;
            }

            if (mAnimationDeselect) {
                c.drawArc(mSelectedRingBounds, mStartingAngle, mSelectedRingDelay, false, blankPaint());
                c.drawArc(mSelectedRingBounds, mStartingAngle, mSelectedRingDelay, false, selectedPaint());
            }
            else {
                mAnimationDeselect = true;
                mAnimationSelect = false;
                animateSelectionRing(false);
            }
        }

        mFirstDraw = false;
        if (mTouchDown) {
            mRippleRadius++;
            animateNext();
        }
    }

    private void drawRipple(Canvas c) {
        if (mHotspotX >= 0 && mHotspotY >= 0 && mRippleRadius >= 0) {
            c.drawCircle(mHotspotX, mHotspotY, mRippleRadius, ripplePaint());
        }

        // Draw circle mask
        c.drawCircle(mCenter.x, mCenter.y, mOuterRadius + Utils.dpToPx(mContext, 16), maskPaint());
    }

    public void stopDrawingRipple() {
        mTouchDown = false;
        mHandler.removeCallbacks(mRippleRunner);
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(150);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedFraction() == 1f) {
                    reset();
                } else {
                    if (animation.getAnimatedFraction() > 0.6f) {
                        mRipplePaint.setAlpha(mRipplePaint.getAlpha() - 10);
                    }
                    mRippleRate += Utils.dpToPx(mContext, 2);
                    mRippleRadius += mRippleRate;
                }
                invalidate();
            }
        });
        animator.start();
    }

    public void animateEntry(int position) {
//        if (mColor != -1) {
//            animateColorChange(mNewColor);
//            return;
//        }
        ValueAnimator enterAnimator = ValueAnimator.ofInt(0,100);
        enterAnimator
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        long animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
        enterAnimator.setStartDelay(animationDelay);

        enterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mEnterDelay = animation.getAnimatedFraction();
                invalidate();
            }
        });
        enterAnimator.start();
    }

    public void animateExit(int position) {
        ValueAnimator enterAnimator = ValueAnimator.ofInt(100,0);
        enterAnimator
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        long animationDelay = (long) (((position % 4) * 50) + ((position / 4) * 40));
        enterAnimator.setStartDelay(animationDelay);

        enterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mEnterDelay = animation.getAnimatedFraction();
                invalidate();
            }
        });
        enterAnimator.start();
    }

    // Shrink, change color, grow back.
    public void animateColorChange(final int newColor) {
        final int originalColor = mColor;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(AnimationUtils.ANIMATION_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mColor = AnimationUtils.morphColors(originalColor, newColor, animation.getAnimatedFraction());
                invalidate();
            }
        });

//        ValueAnimator animator = ValueAnimator.ofInt(100,0);
//        animator
//                .setDuration(150)
//                .setInterpolator(new AccelerateDecelerateInterpolator());
//
//        long animationDelay = (long) (((mPosition % 4) * 50) + ((mPosition / 4) * 40));
//        animator.setStartDelay(animationDelay);
//
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                mEnterDelay = animation.getAnimatedFraction();
//                invalidate();
//            }
//        });
//        final ValueAnimator enterAnimator = animator;
//
//        ValueAnimator exitAnimator = ValueAnimator.ofInt(100,0);
//        exitAnimator
//                .setDuration(150)
//                .setInterpolator(new AccelerateDecelerateInterpolator());
//
//        animationDelay = (long) (((mPosition % 4) * 50) + ((mPosition / 4) * 40));
//        exitAnimator.setStartDelay(animationDelay);
//
//        exitAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                mEnterDelay = animation.getAnimatedFraction();
//                invalidate();
//                if (mEnterDelay == 1f) {
//                    mColor = newColor;
//                    enterAnimator.start();
//                }
//            }
//        });
//        exitAnimator.start();
    }

    private void animateSelectionRing(boolean selected) {
        ValueAnimator offsetAnimator = selected ? ValueAnimator.ofInt(0, 360) : ValueAnimator.ofInt(360, 0);
        offsetAnimator
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        if (!mTouched) {
            long animationDelay = (long) (((mPosition % 4) * 50) + ((mPosition / 4) * 40));
            offsetAnimator.setStartDelay(animationDelay);
        }
        else {
            mTouched = false;
        }

        offsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mSelectedRingDelay = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        offsetAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mIsPreview) {
			return false;
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDown = true;
                mHotspotX = e.getX();
                mHotspotY = e.getY();
                mRippleRadius = 0;
                break;
            case MotionEvent.ACTION_UP:
                mTouchDown = false;
                stopDrawingRipple();
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchDown = false;
                stopDrawingRipple();
                break;
            case MotionEvent.ACTION_MOVE:
                mHotspotX = e.getX();
                mHotspotY = e.getY();
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        if (mEnableRipple) {
            mRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            mRipplePaint.setStyle(Paint.Style.FILL);
            mRipplePaint.setAlpha(ALPHA);
        }
    }

    private Paint selectedPaint() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Utils.dpToPx(mContext, 4));
        mPaint.setColor(getOuterColor(mColor));
        return mPaint;
    }

    private Paint normalPaint() {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColor);
        return mPaint;
    }

    private Paint blankPaint() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mDiffRadius + Utils.dpToPx(mContext, 2));
        mPaint.setColor(getResources().getColor(R.color.Card));
        return mPaint;
    }

    private Paint ripplePaint() {
        mRipplePaint.setColor(getOuterColor(mColor));
        mRipplePaint.setAlpha(ALPHA);
        return mRipplePaint;
    }

    private Paint maskPaint() {
        mPaint.setColor(getResources().getColor(R.color.Card));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Utils.dpToPx(mContext, 32));
        return mPaint;
    }

    public void setColor(int c) {
        mColor = c;
        invalidate();
    }

    public int getColor() {
        return mColor;
    }

    private int getOuterColor(int color) {
        int r, g, b;
        float[] hsv = new float[3];

        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);

        Color.RGBToHSV(r, g, b, hsv);

        // Change colour brightness
        if (hsv[2] > 0.4) {
            hsv[2] -= 0.2;
        }
        else {
            hsv[2] += 0.3;
        }

        return Color.HSVToColor(hsv);
    }

    public void setPosition(int p) {
        mPosition = p;
    }

    public void setTouched(boolean b) {
        mTouched = true;
    }

    public void reset() {
        mHotspotX = -1;
        mHotspotY = -1;
        mRippleRadius = -1;
        mRipplePaint.setAlpha(ALPHA);
        mRippleRate = Utils.dpToPx(mContext, 8);

        invalidate();
    }

    private void animateNext() {
        mHandler.postDelayed(mRippleRunner, 15);
    }

	public boolean isPreview() {
		return mIsPreview;
	}

	public void setIsPreview(boolean b) {
		mIsPreview = b;
	}
}
