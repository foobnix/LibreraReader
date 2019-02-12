package org.emdev.ui.actions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.collections.SparseArrayEx;

import  com.foobnix.pdf.info.R;

import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

public class ActionEx implements Runnable, View.OnClickListener, View.OnLongClickListener,
        AdapterView.OnItemClickListener, DialogInterface.OnClickListener, OnMultiChoiceClickListener,
        TextView.OnEditorActionListener {

    private static final String SHORT_DESCRIPTION = "ShortDescription";

    private static SparseArrayEx<String> s_names;

    private static Map<String, Integer> s_ids;

    public final int id;

    public final String name;

    private final ActionControllerMethod m_method;

    private final Map<String, Object> m_values = new LinkedHashMap<String, Object>();

    private final Map<String, IActionParameter> m_actionParameters = new LinkedHashMap<String, IActionParameter>();

    private boolean enabled = true;

    /**
     * Constructor
     * 
     * @param controller
     *            action controller
     * @param category
     *            action category
     * @param id
     *            action id
     */
    ActionEx(final IActionController<?> controller, final int id) {
        this.id = id;
        this.name = getActionName(id);
        this.m_method = new ActionControllerMethod(controller, this);
    }

    /**
     * Returns the action's description.
     * 
     * @return the actions description
     * @see #setDescription
     */
    public String getDescription() {
        return getParameter(SHORT_DESCRIPTION);
    }

    /**
     * Sets the action's description.
     * 
     * @param description
     *            the string used to set the action's description
     * @see #getDescription
     * @beaninfo bound: true preferred: true attribute: visualUpdate true
     *           description: The action's name.
     */
    public void setDescription(final String description) {
        putValue(SHORT_DESCRIPTION, description);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return ActionControllerMethod
     */
    public ActionControllerMethod getMethod() {
        return m_method;
    }

    /**
     * Returns component managed by action controller created this action.
     * 
     * @param <ManagedComponent>
     *            managed component type
     * @return component managed by action controller created this action
     */
    @SuppressWarnings("unchecked")
    public <ManagedComponent> ManagedComponent getManagedComponent() {
        return (ManagedComponent) getValue(IActionController.MANAGED_COMPONENT_PROPERTY);
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     * 
     * @param key
     *            a string containing the specified <code>key</code>
     * @return the binding <code>Object</code> stored with this key; if
     *         there are no keys, it will return <code>null</code>
     * @see javax.swing.AbstractAction#getValue(java.lang.String)
     */
    @Deprecated
    public Object getValue(final String key) {
        return m_values.get(key);
    }

    public ActionEx putValue(final String key, final Object value) {
        m_values.put(key, value);
        return this;
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     * 
     * @param <T>
     *            parameter type
     * @param key
     *            a string containing the specified <code>key</code>
     * @return the binding <code>Object</code> stored with this key; if
     *         there are no keys, it will return defaultValue parameter
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(final String key) {
        return (T) m_values.get(key);
    }

    /**
     * Gets the <code>Object</code> associated with the specified key.
     * 
     * @param <T>
     *            parameter type
     * @param key
     *            a string containing the specified <code>key</code>
     * @param defaultValue
     *            default value
     * @return the binding <code>Object</code> stored with this key; if
     *         there are no keys, it will return defaultValue parameter
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(final String key, final T defaultValue) {
        final Object value = getParameter(key);
        return (T) (value != null ? value : defaultValue);
    }

    /**
     * Adds a parameter to the action
     * 
     * @param parameter
     *            action parameter to set
     */
    public ActionEx addParameter(final IActionParameter parameter) {
        m_actionParameters.put(parameter.getName(), parameter);
        return this;
    }

    @Override
    public void run() {
        try {
            if (!enabled) {
                return;
            }
            setParameters();
            m_method.invoke(this);
        } catch (final InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (final Throwable th) {
            th.printStackTrace();
        } finally {
        }
    }

    @Override
    public void onClick(final View view) {
        this.putValue(IActionController.VIEW_PROPERTY, view);
        run();
    }

    @Override
    public boolean onLongClick(final View view) {
        this.putValue(IActionController.VIEW_PROPERTY, view);
        run();
        return true;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        this.putValue(IActionController.DIALOG_PROPERTY, dialog);
        this.putValue(IActionController.DIALOG_ITEM_PROPERTY, which);
        this.run();
    }

    public boolean isDialogItemSelected(final int which) {
        final SparseBooleanArray map = this.getParameter(IActionController.DIALOG_SELECTED_ITEMS_PROPERTY);
        return map != null ? map.get(which) : false;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
        SparseBooleanArray map = this.getParameter(IActionController.DIALOG_SELECTED_ITEMS_PROPERTY);
        if (map == null) {
            map = new SparseBooleanArray();
            this.putValue(IActionController.DIALOG_SELECTED_ITEMS_PROPERTY, map);
        }
        map.put(which, isChecked);
    }

    @Override
    public boolean onEditorAction(final TextView textView, final int actionId, final KeyEvent keyEvent) {
        /*
         * if ((actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE)) {
         * if ((keyEvent == null || keyEvent.getAction() == KeyEvent.ACTION_UP)) {
         * this.putValue(IActionController.VIEW_PROPERTY, textView);
         * run();
         * }
         * return true;
         * }
         */
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.putValue(IActionController.VIEW_PROPERTY, parent);
        this.putValue(IActionController.ADAPTER_SELECTED_ITEM_PROPERTY, parent.getAdapter().getItem(position));
        run();
    }

    /**
     * Sets parameter values to the action
     */
    private void setParameters() {
        for (final Entry<String, IActionParameter> entry : m_actionParameters.entrySet()) {
            putValue(entry.getKey(), entry.getValue().getValue());
        }
    }

    public static String getActionName(final int id) {
        if (s_names == null) {
            fillMapping();
        }
        return LengthUtils.safeString(s_names.get(id), "0x" + Integer.toHexString(id));
    }

    public static Integer getActionId(final String name) {
        if (s_ids == null) {
            fillMapping();
        }
        return s_ids.get(name);
    }

    private static void fillMapping() {
        s_names = new SparseArrayEx<String>();
        s_ids = new HashMap<String, Integer>();

        for (final Field f : R.id.class.getFields()) {
            final int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                if (f.getType() == int.class) {
                    try {
                        final int value = f.getInt(null);
                        s_names.put(value, f.getName());
                        s_ids.put(f.getName(), value);
                    } catch (final Throwable th) {
                    }
                }
            }
        }
    }
}
