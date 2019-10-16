package org.emdev.ui.actions;


/**
 * This enumeration contains supported invocation types for actions and
 * events.
 *
 * @author Alex Kasatkin
 */
public enum InvokationType {
    /**
     * An action or event executed using the
     * {@link android.app.Activity#runOnUiThread(Runnable)} method.
     */
    AsyncUI,
    /**
     * An action or event executed in a separated thread.
     */
    SeparatedThread,
    /**
     * An action or event executed in a current thread.
     */
    Direct;
}