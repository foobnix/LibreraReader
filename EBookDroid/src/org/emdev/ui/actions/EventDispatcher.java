package org.emdev.ui.actions;


import android.app.Activity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.emdev.utils.LengthUtils;

public class EventDispatcher {


    private final Activity m_base;

    private final InvokationType m_type;

    /**
     * Supported interfaces.
     */
    private final Class<?>[] m_interfaces;

    /**
     * Real listeners.
     */
    private final Map<Class<?>, List<Object>> m_listeners = new HashMap<Class<?>, List<Object>>();

    private final Object m_proxy;

    private final InvocationHandler m_handler;

    /**
     * Constructor
     * 
     * @param type
     *            invocation type
     * @param target
     *            target object
     * @param listeners
     *            a list of listener interfaces
     */
    public EventDispatcher(final Activity base, final InvokationType type, final Class<?>... listeners) {

        if (LengthUtils.isEmpty(listeners)) {
            throw new IllegalArgumentException("Listeners list cannot be empty");
        }

        for (final Class<?> listener : listeners) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener class cannot be null");
            }
            if (!listener.isInterface()) {
                throw new IllegalArgumentException("Listener class should be an interface");
            }
        }

        m_base = base;
        m_type = type;
        m_handler = new Handler();
        m_interfaces = listeners;
        m_proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), listeners, m_handler);
    }

    /**
     * Adds the target listener.
     * 
     * @param listener
     *            the listener to add
     */
    public void addListener(final Object listener) {
        if (listener != null) {
            for (final Class<?> listenerClass : m_interfaces) {
                if (listenerClass.isInstance(listener)) {
                    List<Object> list = m_listeners.get(listenerClass);
                    if (list == null) {
                        list = new LinkedList<Object>();
                        m_listeners.put(listenerClass, list);
                    }

                    if (!list.contains(listener)) {
                        list.add(listener);
                    }
                }
            }
        }
    }

    /**
     * Removes the target listener.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeListener(final Object listener) {
        if (listener != null) {
            for (final Class<?> listenerClass : m_interfaces) {
                if (listenerClass.isInstance(listener)) {
                    final List<Object> list = m_listeners.get(listenerClass);
                    if (list != null) {
                        list.remove(listener);
                    }
                }
            }
        }
    }

    /**
     * Gets a listener of the given type.
     * 
     * @param <Listener>
     *            listener type
     * @return listener proxy object casted to the given type
     */
    @SuppressWarnings("unchecked")
    public <Listener> Listener getListener() {
        return (Listener) m_proxy;
    }

    /**
     * This class implements invocation handler for event listeners.
     */
    private class Handler implements InvocationHandler {

        /**
         * Processes a method invocation on a proxy instance and returns the
         * result.
         * 
         * @param proxy
         *            the proxy instance that the method was invoked on
         * @param method
         *            the <code>Method</code> instance corresponding to
         *            the interface method invoked on the proxy instance.
         * @param args
         *            an array of objects containing the values of the
         *            arguments passed in the method invocation on the proxy
         *            instance.
         * @return the value to return from the method invocation on the
         *         proxy instance.
         * @throws Throwable
         *             the exception to throw from the method
         *             invocation on the proxy instance.
         * @see InvocationHandler#invoke(Object, Method, Object[])
         */
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Class<?> listenerClass = method.getDeclaringClass();
            final List<Object> targets = m_listeners.get(listenerClass);
            if (LengthUtils.isNotEmpty(targets)) {
                final Task task = new Task(targets, method, args);
                switch (m_type) {
                    case AsyncUI:
                        m_base.runOnUiThread(task);
                        break;
                    case SeparatedThread:
                        new Thread(task).start();
                        break;
                    case Direct:
                    default:
                        task.run();
                        break;
                }
            }
            return null;
        }

    }

    /**
     * This class implements thread task for listener invocation.
     */
    private class Task implements Runnable {

        private final List<Object> m_targets;

        private final Method m_method;

        private final Object[] m_args;

        /**
         * Constructor
         * 
         * @param method
         *            called method
         * @param args
         *            method parameters
         */
        public Task(final List<Object> targets, final Method method, final Object[] args) {
            m_targets = targets;
            m_method = method;
            m_args = args;
        }

        /**
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public synchronized void run() {
            directInvoke();
        }

        /**
         * Direct invoke of the action.
         * 
         * @param method
         *            called method
         * @param args
         *            method parameters
         */
        protected void directInvoke() {
            for (final Object target : m_targets) {
                try {
                    m_method.invoke(target, m_args);
                } catch (final Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
