package com.foobnix.pdf.info.demo;

import com.foobnix.pdf.info.wrapper.AppConfig;

public class DemoConfig implements AppConfig {


	@Override
	public boolean isContentEnable() {
		return true;
	}

	@Override
	public boolean isDayNightModeEnable() {
		return true;
	}
	@Override
	public boolean isSearchEnable() {
		return true;
	}

}
