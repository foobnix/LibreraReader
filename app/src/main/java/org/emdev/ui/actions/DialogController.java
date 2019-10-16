package org.emdev.ui.actions;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

/**
 * This class defines base features for action controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public class DialogController<ManagedComponent extends Dialog> extends AbstractComponentController<ManagedComponent> {

    /**
     * Constructor
     * 
     * @param dialog
     *            managed dialog
     */
    public DialogController(final ManagedComponent dialog) {
        this(null, dialog);
    }

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent controller
     * @param managedComponent
     *            managed component
     */
    public DialogController(final IActionController<?> parent, final ManagedComponent managedComponent) {
        super(parent, managedComponent);
    }

    public ActionEx connectViewToAction(final int viewId) {
        ActionEx action = getOrCreateAction(viewId);
        View view = getManagedComponent().findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(action);
        }
        return action;
    }

    public ActionEx connectViewToAction(final View view) {
        ActionEx action = getOrCreateAction(view.getId());
        view.setOnClickListener(action);
        return action;
    }

    public ActionEx connectViewToAction(final View view, final int onClickActionId) {
        ActionEx action = getOrCreateAction(onClickActionId);
        view.setOnClickListener(action);
        return action;
    }

    public ActionEx connectEditorToAction(final TextView view, final int onEditActionId) {
        ActionEx action = getOrCreateAction(onEditActionId);
        view.setOnEditorActionListener(action);
        return action;
    }

    public void connectViewToActions(final int viewId, final int onClickActionId, final int onLongClickActionId) {
        ActionEx action1 = getOrCreateAction(onClickActionId);
        ActionEx action2 = getOrCreateAction(onLongClickActionId);
        View view = getManagedComponent().findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(action1);
            view.setOnLongClickListener(action2);
        }
    }

    public void connectViewToActions(final View view, final int onClickActionId, final int onLongClickActionId) {
        ActionEx action1 = getOrCreateAction(onClickActionId);
        ActionEx action2 = getOrCreateAction(onLongClickActionId);
        view.setOnClickListener(action1);
        view.setOnLongClickListener(action2);
    }
}
