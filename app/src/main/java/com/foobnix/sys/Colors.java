package com.foobnix.sys;

import android.graphics.Color;

public class Colors {

	public static float[] toMupdfColor(int color) {
		float[] hsv = new float[3];
		hsv[0] = (float) Color.red(color) / 256;
		hsv[1] = (float) Color.green(color) / 256;
		hsv[2] = (float) Color.blue(color) / 256;
		return hsv;
	}
}
