package com.foobnix.pdf.info.demo;

import com.foobnix.pdf.info.wrapper.AppConfig;

public class VuConfig implements AppConfig {


	@Override
	public boolean isContentEnable() {
		return false;
	}

	@Override
	public boolean isDayNightModeEnable() {
		return false;
	}
	@Override
	public boolean isSearchEnable() {
		return false;
	}

}
