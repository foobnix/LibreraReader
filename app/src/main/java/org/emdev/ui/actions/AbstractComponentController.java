package org.emdev.ui.actions;

import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class defines base features for action controller.
 * 
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public abstract class AbstractComponentController<ManagedComponent> implements IActionController<ManagedComponent> {

    protected final Map<Integer, ActionEx> m_actions = new LinkedHashMap<Integer, ActionEx>();

    protected final ReentrantReadWriteLock m_actionsLock = new ReentrantReadWriteLock();

    protected final IActionController<?> m_parent;

    protected ManagedComponent m_managedComponent;


    /**
     * Constructor
     * 
     * @param managedComponent
     *            managed component
     */
    protected AbstractComponentController(final ManagedComponent managedComponent) {
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
    protected AbstractComponentController(final IActionController<?> parent, final ManagedComponent managedComponent) {
        m_parent = parent;
        m_managedComponent = managedComponent;
    }

    /**
     * @return the parent controller
     * @see IActionController#getParent()
     */
    @Override
    public IActionController<?> getParent() {
        return m_parent;
    }

    /**
     * @return the managed component
     * @see IActionController#getManagedComponent()
     */
    @Override
    public ManagedComponent getManagedComponent() {
        return m_managedComponent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.actions.IActionController#setManagedComponent(java.lang.Object)
     */
    @Override
    public void setManagedComponent(final ManagedComponent component) {
        m_actionsLock.writeLock().lock();
        try {
            m_managedComponent = component;
            for (final ActionEx action : m_actions.values()) {
                action.putValue(MANAGED_COMPONENT_PROPERTY, component);
            }
        } finally {
            m_actionsLock.writeLock().unlock();
        }
    }

    /**
     * Searches for an action by the given id
     * 
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object or <code>null</code>
     * @see IActionController#getAction(java.lang.String)
     */
    @Override
    public ActionEx getAction(final int id) {
        m_actionsLock.readLock().lock();
        try {
            ActionEx actionEx = m_actions.get(id);
            if (actionEx == null && m_parent != null) {
                actionEx = m_parent.getAction(id);
            }
            if (actionEx != null) {
                actionEx.putValue(DIALOG_PROPERTY, null);
                actionEx.putValue(DIALOG_ITEM_PROPERTY, null);
                actionEx.putValue(DIALOG_SELECTED_ITEMS_PROPERTY, null);
                actionEx.putValue(VIEW_PROPERTY, null);
            }
            return actionEx;
        } finally {
            m_actionsLock.readLock().unlock();
        }
    }

    /**
     * Creates and register a global action
     * 
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object
     * @see IActionController#getOrCreateAction(String, String, IActionParameter[])
     */
    @Override
    public ActionEx getOrCreateAction(final int id) {
        ActionEx result = null;
        m_actionsLock.writeLock().lock();
        try {
            result = getAction(id);
            if (result == null) {
                result = createAction(id);
            }
        } finally {
            m_actionsLock.writeLock().unlock();
        }

        return result;
    }

    /**
     * Creates an action
     * 
     * @param id
     *            action id
     * @param parameters
     *            action parameters
     * @return an instance of {@link ActionEx} object
     * @see IActionController#createAction(String, String, IActionParameter[])
     */
    @Override
    public ActionEx createAction(final int id, final IActionParameter... parameters) {
        final ActionEx result = new ActionEx(this, id);

        result.putValue(MANAGED_COMPONENT_PROPERTY, m_managedComponent);
        result.putValue(COMPONENT_CONTROLLER_PROPERTY, this);

        for (final IActionParameter actionParameter : parameters) {
            result.addParameter(actionParameter);
        }

        m_actionsLock.writeLock().lock();
        try {
            m_actions.put(result.id, result);
        } finally {
            m_actionsLock.writeLock().unlock();
        }

        return result;
    }

    public void noAction(final ActionEx action) {
    }

    public final ActionEx setActionForView(final View view) {
        final ActionEx action = getOrCreateAction(view.getId());
        view.setOnClickListener(action);
        return action;
    }

}
