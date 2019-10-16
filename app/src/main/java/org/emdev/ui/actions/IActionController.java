package org.emdev.ui.actions;

/**
 * This interface defines base features for component controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public interface IActionController<ManagedComponent> {

    /**
     * Name of action property containing managed component
     */
    String MANAGED_COMPONENT_PROPERTY = "ManagedComponent";

    /**
     * Name of action property containing component controller
     */
    String COMPONENT_CONTROLLER_PROPERTY = "ComponentController";

    /**
     * Name of action property containing component controller
     */
    String VIEW_PROPERTY = "View";

    String ADAPTER_SELECTED_ITEM_PROPERTY = "AdapterSelectedItem";

    String DIALOG_PROPERTY = "Dialog";

    String DIALOG_ITEM_PROPERTY = "DialogItem";

    String DIALOG_SELECTED_ITEMS_PROPERTY = "DialogSelectedItems";

    /**
     * @return the parent controller
     */
    IActionController<?> getParent();

    /**
     * @return the managed component
     */
    ManagedComponent getManagedComponent();

    /**
     * @param component manager component to set
     */
    void setManagedComponent(ManagedComponent component);

    /**
     * Searches for a global action by the given action id.
     * 
     * @param id
     *            action id
     * @return an instance of the {@link ActionEx} object or <code>null</code>
     */
    ActionEx getAction(final int id);

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object or <code>null</code>
     */
    ActionEx getOrCreateAction(final int id);

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @param parameters
     *            action parameters
     * @return an instance of {@link ActionEx} object
     */
    ActionEx createAction(final int id, final IActionParameter... parameters);

}
