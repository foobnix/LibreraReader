package org.emdev.ui.actions;

/**
 * This class defines base features for action controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public class ActionController<ManagedComponent> extends AbstractComponentController<ManagedComponent> {

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


}
