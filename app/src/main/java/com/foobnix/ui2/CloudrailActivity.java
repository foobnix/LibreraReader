package com.foobnix.ui2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cloudrail.si.CloudRail;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.ui2.fragment.UIFragment;

public class CloudrailActivity extends Activity {
    public static final int REQUEST_CODE_ADD_RESOURCE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.d("CloudrailActivity ", "onCreate", getIntent());
        checkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        LOG.d("CloudrailActivity ", "onNewIntent", getIntent());
        checkIntent(intent);

    }

    private void checkIntent(final Intent intent) {
        LOG.d("CloudrailActivity", "checkIntent", intent);
        if (intent.getCategories() != null && intent.getCategories().contains("android.intent.category.BROWSABLE")) {
            CloudRail.setAuthenticationResponse(intent);
            LOG.d("CloudRail response", intent);

            Intent intent1 = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                    .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
        }
        finish();
    }

}
