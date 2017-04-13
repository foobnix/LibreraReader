package com.foobnix.pdf.info.view;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AnchorHelper {

	public static void setXY(View anchor, float x, float y) {
		if (Build.VERSION.SDK_INT >= 11) {
			anchor.setX(x);
			anchor.setY(y);
		} else {
			LayoutParams layoutParams = anchor.getLayoutParams();
			((RelativeLayout.LayoutParams) layoutParams).leftMargin = (int) x;
			((RelativeLayout.LayoutParams) layoutParams).topMargin = (int) y;
			anchor.requestLayout();
		}
	}

	public static void setX(View anchor, float x) {
		if (Build.VERSION.SDK_INT >= 11) {
			anchor.setX(x);
		} else {
			LayoutParams layoutParams = anchor.getLayoutParams();
			((RelativeLayout.LayoutParams) layoutParams).leftMargin = (int) x;
			anchor.requestLayout();
		}
	}

	public static void setY(View anchor, float y) {
		if (Build.VERSION.SDK_INT >= 11) {
			anchor.setY(y);
		} else {
			LayoutParams layoutParams = anchor.getLayoutParams();
			((RelativeLayout.LayoutParams) layoutParams).topMargin = (int) y;
			anchor.requestLayout();
		}
	}

	public static float getX(View anchor) {
		if (Build.VERSION.SDK_INT >= 11) {
			return anchor.getX();
		}
		return 0;
	}

	public static float getY(View anchor) {
		if (Build.VERSION.SDK_INT >= 11) {
			return anchor.getY();
		}
		return 0;
	}

}
