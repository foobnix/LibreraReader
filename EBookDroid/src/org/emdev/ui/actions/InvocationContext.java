package org.emdev.ui.actions;

public @interface InvocationContext {

    InvokationType name() default InvokationType.Direct;
}
