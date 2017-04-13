package com.foobnix.pdf.info.wrapper;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

import com.foobnix.pdf.info.demo.DemoActivity;

public class DocumemtTabsActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		TabHost tabs = getTabHost();

		// tabs.setup();

		TabHost.TabSpec spec1 = tabs.newTabSpec("tab1");

		spec1.setContent(new Intent(this, DemoActivity.class));
		spec1.setIndicator("Docuemnt 1");

		tabs.addTab(spec1);

		TabHost.TabSpec spec2 = tabs.newTabSpec("tab1");

		spec2.setContent(new Intent(this, DemoActivity.class));
		spec2.setIndicator("Docuemnt 2");

		tabs.addTab(spec2);
		// setContentView(tabs);
	}

}
