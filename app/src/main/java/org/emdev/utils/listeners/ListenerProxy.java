package org.emdev.utils.listeners;


import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.emdev.utils.LengthUtils;

public class ListenerProxy {

    /**
     * All objects
     */
    private final List<WeakReference<Object>> references = new LinkedList<WeakReference<Object>>();

    /**
     * Real listeners.
     */
    private final Map<Class<?>, List<WeakReference<Object>>> realListeners = new HashMap<Class<?>, List<WeakReference<Object>>>();

    /**
     * Supported interfaces.
     */
    private final Class<?>[] interfaces;

    /**
     * Proxy object.
     */
    private final Object proxy;

    /**
     * Constructor.
     * 
     * @param listenerInterfaces
     *            a list of listener interfaces to implement
     */
    public ListenerProxy(final Class<?>... listenerInterfaces) {
        if (LengthUtils.isEmpty(listenerInterfaces)) {
            throw new IllegalArgumentException("Listeners list cannot be empty");
        }

        for (final Class<?> listener : listenerInterfaces) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener class cannot be null");
            }
            if (!listener.isInterface()) {
                throw new IllegalArgumentException("Listener class should be an interface");
            }
        }

        interfaces = listenerInterfaces;

        proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), interfaces, new Handler());
    }

    /**
     * Adds the target listener.
     * 
     * @param listener
     *            the listener to add
     */
    public void addListener(final Object listener) {
        if (listener != null) {
            WeakReference<Object> ref = new WeakReference<Object>(listener);
            for (WeakReference<Object> r : references) {
                if (r.get() == listener) {
                    return;
                }
            }

            references.add(ref);
            for (final Class<?> listenerClass : interfaces) {
                if (listenerClass.isInstance(listener)) {
                    List<WeakReference<Object>> list = realListeners.get(listenerClass);
                    if (list == null) {
                        list = new LinkedList<WeakReference<Object>>();
                        realListeners.put(listenerClass, list);
                    }
                    list.add(ref);
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
            WeakReference<Object> ref = null;
            for (WeakReference<Object> r : references) {
                if (r.get() == listener) {
                    ref = r;
                    break;
                }
            }
            if (ref != null) {
                references.remove(ref);
                for (final Class<?> listenerClass : interfaces) {
                    if (listenerClass.isInstance(listener)) {
                        final List<WeakReference<Object>> list = realListeners.get(listenerClass);
                        if (list != null) {
                            list.remove(ref);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes the all target listeners.
     */
    public void removeAllListeners() {
        references.clear();
        for (final List<WeakReference<Object>> list : realListeners.values()) {
            list.clear();
        }
        realListeners.clear();
    }

    /**
     * Gets the proxy listener casted to the given listener type.
     * 
     * @param <Listener>
     *            listener type
     * @return an instance of the <code>Listener</code> type
     */
    @SuppressWarnings("unchecked")
    public <Listener> Listener getListener() {
        return (Listener) proxy;
    }

    /**
     * This class implements invocation handler.
     */
    private class Handler implements InvocationHandler {

        /**
         * Processes a method invocation on a proxy instance and returns the result.
         * 
         * @param proxy
         *            the proxy instance that the method was invoked on
         * @param method
         *            the <code>Method</code> instance corresponding to the interface method invoked on the proxy instance.
         * @param args
         *            an array of objects containing the values of the arguments passed in the method invocation on the proxy
         *            instance.
         * @return the value to return from the method invocation on the proxy instance.
         * @throws Throwable
         *             the exception to throw from the method invocation on the proxy instance.
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Class<?> listenerClass = method.getDeclaringClass();
            final List<WeakReference<Object>> list = realListeners.get(listenerClass);

            if (LengthUtils.isNotEmpty(list)) {
                for (final WeakReference<Object> ref : list) {
                    Object listener = ref.get();
                    if (listener != null) {
                        method.invoke(listener, args);
                    }
                }
            }

            return null;
        }
    }
}
