package com.beatonma.colorpicker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.utility.AnimationUtils;

/**
 * Created by Michael on 08/11/2015.
 */

public abstract class AnimatedPopupFragment extends Fragment {
	private final static String TAG = "PopupFragment";

	protected final static String POSITION_X = "position_x";
	protected final static String POSITION_Y = "position_y";

	protected float mPositionX = -1;
	protected float mPositionY = -1;

	private boolean mClosing = false;

	protected View mCard;
	protected View mBackground;

	protected Handler mHandler;

	protected abstract int getLayout();
	protected abstract void initLayout(View v);

	public void close() {
		if (!mClosing) {
			mClosing = true;

			AnimationUtils.fadeOut(mCard);
			AnimationUtils.fadeOut(mBackground);

			getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					FragmentManager fm = getActivity().getSupportFragmentManager();
					fm.popBackStack();
				}
			}, AnimationUtils.ANIMATION_DURATION);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
		View v = inflater.inflate(getLayout(), parent, false);

		mBackground = v.findViewById(R.id.overlay);
		if (mBackground != null) {
			mBackground.animate()
					.setInterpolator(new AccelerateDecelerateInterpolator())
					.alpha(0.5f)
					.start();

			mBackground.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					close();
				}
			});
		}

		mCard = v.findViewById(R.id.card);

		if (mPositionX != -1) {
			mCard.setX(mPositionX);
		}
		if (mPositionY != -1) {
			mCard.setY(mPositionY);
		}

		mCard.post(new Runnable() {
			@Override
			public void run() {
				AnimationUtils.expandAndFadeIn(mCard, AnimationUtils.CENTER);
			}
		});

		initLayout(v);

		return v;
	}

	private void centerOnScreen() {
		float targetX = (mBackground.getWidth() - mCard.getWidth()) / 2;
		float targetY = (mBackground.getHeight() - mCard.getHeight()) / 2;

		mCard.animate()
				.x(targetX)
				.y(targetY)
				.setInterpolator(new AccelerateDecelerateInterpolator())
				.setDuration(AnimationUtils.ANIMATION_DURATION)
				.start();

	}

	protected Handler getHandler() {
		if (mHandler == null) {
			mHandler = new Handler(Looper.getMainLooper());
		}
		return mHandler;
	}
}
