package org.emdev.ui;

import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionEx;

import com.foobnix.ui2.AdsFragmentActivity;

import android.content.Intent;
import android.view.View;

public abstract class AbstractActionActivity<A extends AdsFragmentActivity, C extends ActionController<A>> extends AdsFragmentActivity {

    public static final String MENU_ITEM_SOURCE = "source";
    public static final String ACTIVITY_RESULT_DATA = "activityResultData";
    public static final String ACTIVITY_RESULT_CODE = "activityResultCode";
    public static final String ACTIVITY_RESULT_ACTION_ID = "activityResultActionId";

    private C controller;

    protected AbstractActionActivity() {
    }

    public final C getController() {
        if (controller == null) {
            controller = createController();
        }
        return controller;
    }


    protected abstract C createController();


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (data != null) {
            final int actionId = data.getIntExtra(ACTIVITY_RESULT_ACTION_ID, 0);
            if (actionId != 0) {
                final ActionEx action = getController().getOrCreateAction(actionId);
                action.putValue(ACTIVITY_RESULT_CODE, Integer.valueOf(resultCode));
                action.putValue(ACTIVITY_RESULT_DATA, data);
                action.run();
            }
        }
    }

    public final void setActionForView(final int id) {
        final View view = findViewById(id);
        final ActionEx action = getController().getOrCreateAction(id);
        if (view != null && action != null) {
            view.setOnClickListener(action);
        }
    }

}
