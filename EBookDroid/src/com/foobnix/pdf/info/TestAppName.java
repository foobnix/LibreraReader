package com.foobnix.pdf.info;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import android.app.Application;

public class TestAppName extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).threadPoolSize(1)//
				.threadPriority(5)//
				.defaultDisplayImageOptions(IMG.displayCacheMemoryDisc).build();

		ImageLoader.getInstance().init(config);
	}
}
