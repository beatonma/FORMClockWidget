package com.beatonma.formclockwidget.app;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.beatonma.formclockwidget.R;
import com.beatonma.formclockwidget.app.ui.AnimatedPopupFragment;
import com.beatonma.formclockwidget.utility.Utils;

/**
 * Created by Michael on 13/11/2015.
 */
public class ChangelogFragment extends AnimatedPopupFragment {
	public static ChangelogFragment newInstance() {
		return new ChangelogFragment();
	}

	@Override
	protected int getLayout() {
		return R.layout.fragment_changelog;
	}

	@Override
	protected void initLayout(View v) {
		int accentColor;
		if (getActivity() instanceof ConfigActivity) {
			accentColor = ((ConfigActivity) getActivity()).getAccentColor();
		} else {
			accentColor = getResources().getColor(R.color.Accent);
		}

		Button dismissButton = (Button) v.findViewById(R.id.button_dismiss);
		dismissButton.setTextColor(accentColor);
		dismissButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				close();
			}
		});

		if (Utils.isLollipop()) {
				View card = v.findViewById(R.id.card);
				ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();

				lp.topMargin = lp.topMargin + Utils.dpToPx(getActivity(), 16);
		}
	}
}
