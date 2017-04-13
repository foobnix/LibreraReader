package org.emdev.ui.actions;

import android.app.Activity;

/**
 * This class defines base features for action controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public class ActionController<ManagedComponent> extends AbstractComponentController<ManagedComponent> {

    private Activity activity;
    /**
     * Constructor
     * 
     * @param managedComponent
     *            managed component
     */
    public ActionController(final ManagedComponent managedComponent) {
        this(null, managedComponent);
    }

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent controller
     * @param managedComponent
     *            managed component
     */
    public ActionController(final IActionController<?> parent, final ManagedComponent managedComponent) {
        super(parent, managedComponent);
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
