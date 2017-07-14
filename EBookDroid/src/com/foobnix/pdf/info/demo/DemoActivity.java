package com.foobnix.pdf.info.demo;

import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.DocumentWrapperUI;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class DemoActivity extends Activity {
	private DocumentWrapperUI wrapperUIControlls;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
        DocumentController.runFullScreen(this);

        // wrapperUIControlls = new DocumentWrapperUI(new DemoConfig(), new
        // DemoController(this, wrapperUIControlls));
        // wrapperUIControlls.initUI(this);
        // wrapperUIControlls.updateUI();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
