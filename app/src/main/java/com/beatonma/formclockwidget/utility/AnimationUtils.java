package com.beatonma.formclockwidget.utility;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

/**
 * Created by Michael on 24/07/2015.
 */
public class AnimationUtils {
	private final static String TAG = "AnimationUtils";

	public final static int ANIMATION_DURATION = 300;
	public final static int ANIMATION_DURATION_LONG = 500;
	public final static int ANIMATION_DURATION_VERY_LONG = 1000;

	// Used for circular reveal/hide methods to easily start animation from a certain side
	// or corner if two of these are used
	public final static int LEFT = 0;
	public final static int TOP = 1;
	public final static int RIGHT = 2;
	public final static int BOTTOM = 3;
	public final static int CENTER = 4;

	public static void createCircularReveal(final View v, int cx, int cy) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				int finalRadius = Math.max(v.getWidth(), v.getHeight());

				Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
				anim.setDuration(ANIMATION_DURATION);
				anim.setInterpolator(new AccelerateDecelerateInterpolator());

				v.setVisibility(View.VISIBLE);
				anim.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						v.setVisibility(View.VISIBLE);
					}
				});
				anim.start();
			}
			else {
				fadeIn(v);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Error creating circular reveal: " + e.toString());
			v.setVisibility(View.VISIBLE);
		}
	}

	public static void createCircularReveal(View v, int side) {
		createCircularReveal(v, new int[]{side});
	}

	// Create reveal from a certain side
	public static void createCircularReveal(View v, int[] sides) {
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;

		for (int i : sides) {
			switch (i) {
				case LEFT:
					cx = 0;
					break;
				case RIGHT:
					cx = v.getRight();
					break;
			}
			switch (i) {
				case TOP:
					cy = 0;
					break;
				case BOTTOM:
					cy = v.getBottom();
					break;
			}
		}

		createCircularReveal(v, cx, cy);
	}

	public static void createCircularHide(final View v, int cx, int cy) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				// get the initial radius for the clipping circle
				int initialRadius = v.getWidth();

				// create the animation (the final radius is zero)
				Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, initialRadius, 0);
				anim.setDuration(ANIMATION_DURATION);
				anim.setInterpolator(new AccelerateDecelerateInterpolator());

				// make the view invisible when the animation is done
				anim.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						v.setVisibility(View.INVISIBLE);
					}
				});

				// start the animation
				anim.start();
			}
			else {
				fadeOutCompletely(v);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Error creating circular hide: " + e.toString());
			v.setVisibility(View.INVISIBLE);
		}
	}

	public static void createCircularHide(View v, int side) {
		createCircularHide(v, new int[]{side});
	}

	// Create reveal from a certain side
	public static void createCircularHide(View v, int[] sides) {
		int cx = (v.getLeft() + v.getRight()) / 2;
		int cy = (v.getTop() + v.getBottom()) / 2;

		for (int i : sides) {
			switch (i) {
				case LEFT:
					cx = 0;
					break;
				case RIGHT:
					cx = v.getRight();
					break;
			}
			switch (i) {
				case TOP:
					cy = 0;
					break;
				case BOTTOM:
					cy = v.getBottom();
					break;
			}
		}

		createCircularHide(v, cx, cy);
	}

	public static Point getCenter(View v) {
		Point point = new Point();
		point.x = (int) (v.getX() + (v.getMeasuredWidth() / 2));
		point.y = (int) (v.getY() + (v.getMeasuredHeight() / 2));
		return point;
	}

	public static void expandAndFadeIn(final View v, final int... sides) {
		final int targetHeight = v.getHeight();
		final int targetWidth = v.getWidth();
		v.setAlpha(0);

		ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.width = 0;
		lp.height = 0;
		v.setLayoutParams(lp);

		v.setVisibility(View.VISIBLE);

		boolean translateFromRight = false;
		boolean translateFromBottom = false;
		boolean center = false;

		for (int i = 0; i < sides.length; i++) {
			if (sides[i] == RIGHT) {
				translateFromRight = true;
			}
			else if (sides[i] == BOTTOM) {
				translateFromBottom = true;
			}
			else if (sides[i] == CENTER) {
				center = true;
			}
		}

		final boolean backwardsVertical = translateFromBottom;
		final boolean backwardsHorizontal = translateFromRight;
		final boolean fromCenter = center;

		ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.setDuration(ANIMATION_DURATION);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				ViewGroup.LayoutParams lp = v.getLayoutParams();
				float fraction = animation.getAnimatedFraction();
				lp.width = (int) (fraction * targetWidth);
				lp.height = (int) (fraction * targetHeight);
				v.setLayoutParams(lp);

				if (fromCenter) {

				}
				else {
					if (backwardsVertical) {
						v.setTranslationY(targetHeight - (targetHeight * fraction));
					}
					if (backwardsHorizontal) {
						v.setTranslationX(targetWidth - (targetWidth * fraction));
					} else {
						v.setTranslationX((targetWidth * fraction) - targetWidth);
					}
				}

				v.setAlpha(fraction);
			}
		});
		animator.start();
	}

	public static void fadeIn(final View v) {
		v.setAlpha(0);
		v.setVisibility(View.VISIBLE);
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(ANIMATION_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                v.setAlpha(fraction);
                if (fraction == 1f) {
                    v.setVisibility(View.VISIBLE);
                }
            }
        });
        anim.start();
	}

	public static void fadeIn(View v, float opacity) {
		v.setAlpha(0);
		v.setVisibility(View.VISIBLE);
		v.animate()
				.setInterpolator(new AccelerateDecelerateInterpolator())
				.setDuration(ANIMATION_DURATION)
				.alpha(opacity)
				.start();
	}

	public static void fadeOut(View v) {
		v.animate()
				.setInterpolator(new AccelerateDecelerateInterpolator())
				.setDuration(ANIMATION_DURATION)
				.alpha(0)
				.start();
	}

	public static void fadeOutCompletely(final View v) {
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(ANIMATION_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = 1f - animation.getAnimatedFraction();
                v.setAlpha(fraction);
                if (fraction == 1f) {
                    v.setVisibility(View.INVISIBLE);
                }
            }
        });
        anim.start();
	}

	public static void slideInDown(View v) {
		v.setTranslationY(-v.getMeasuredHeight());
		v.setVisibility(View.VISIBLE);
		v.animate()
				.setInterpolator(new AccelerateDecelerateInterpolator())
				.setDuration(ANIMATION_DURATION)
				.translationY(0)
				.start();
	}

	public static void slideOutUp(View v) {
		v.animate()
				.setInterpolator(new AccelerateDecelerateInterpolator())
				.setDuration(ANIMATION_DURATION)
				.translationY(-v.getMeasuredHeight())
				.start();
	}

	public static void showPreference(final View v, final int fullHeight) {
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.height = 0;
		v.setLayoutParams(lp);

		v.setVisibility(View.INVISIBLE);

		ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
		animator.setDuration(ANIMATION_DURATION);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float fraction = animation.getAnimatedFraction();
				ViewGroup.LayoutParams lp = v.getLayoutParams();
				lp.height = (int) (fraction * fullHeight);
				v.setLayoutParams(lp);

				if (fraction == 1f) {
					fadeIn(v);
				}
			}
		});
		animator.start();
	}

	public static void hidePreference(final View v, final int fullHeight) {
		fadeOut(v);

		v.postDelayed(new Runnable() {
			@Override
			public void run() {
				ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
				animator.setDuration(ANIMATION_DURATION);
				animator.setInterpolator(new AccelerateDecelerateInterpolator());
				animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						float fraction = animation.getAnimatedFraction();
						ViewGroup.LayoutParams lp = v.getLayoutParams();

						if (fraction == 1f) {
							v.setVisibility(View.GONE);
							lp.height = fullHeight;
						}
						else {
							lp.height = (int) ((1 -fraction) * fullHeight);
						}
						v.setLayoutParams(lp);
					}
				});
				animator.start();
			}
		}, ANIMATION_DURATION);
	}

	// Replace image using a simple fade-out fade-in animation
	public static void refreshImage(Handler handler, final ImageView v, final Bitmap newImage) {
		fadeOut(v);
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				v.setImageBitmap(newImage);
				v.invalidate();
				fadeIn(v);
			}
		}, ANIMATION_DURATION);
	}

	/** Replace image using circular hide/reveal animations
	 * This is unreliable and I'm not sure why so app is using fade in/out methods to
	 * replace images instead
	 */
	public static void refreshImage(Handler handler, final ImageView v, final Bitmap newImage, final int side) {
		Log.d(TAG, "refreshing image");
		if (newImage == null) {
			Log.e(TAG, "New image bitmap is null!");
		}
		else {
			Log.d(TAG, "New image bitmap: " + newImage.getByteCount());
		}
		createCircularHide(v, side);

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				v.setImageBitmap(newImage);
				v.invalidate();
				createCircularReveal(v, side);
			}
		}, ANIMATION_DURATION);
	}

	public static TransitionDrawable morphColors(int fromColor, int toColor) {
		TransitionDrawable drawable = new TransitionDrawable(new ColorDrawable[] { new ColorDrawable(fromColor), new ColorDrawable(toColor) });
		return drawable;
	}

	public static float interpolateAccelerateDecelerate(float input) {
		return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
	}

	public static int morphColors(int fromColor, int toColor, float progress) {
		float[] fromHsv = new float[3];
		float[] toHsv = new float[3];

		Color.RGBToHSV(Color.red(fromColor), Color.green(fromColor), Color.blue(fromColor), fromHsv);
		Color.RGBToHSV(Color.red(toColor), Color.green(toColor), Color.blue(toColor), toHsv);

		float h = (toHsv[0] - fromHsv[0]) * progress;
		float s = (toHsv[1] - fromHsv[1]) * progress;
		float v = (toHsv[2] - fromHsv[2]) * progress;

		return Color.HSVToColor(new float[] { h, s, v });
	}
}
