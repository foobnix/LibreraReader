package com.foobnix.sys;

import com.foobnix.pdf.info.wrapper.AppConfig;

public class VuDroidConfig implements AppConfig {


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