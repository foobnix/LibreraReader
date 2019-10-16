package org.emdev.ui.actions.params;

import android.widget.CompoundButton;

public class CheckableValue extends AbstractActionParameter {

    private final CompoundButton input;

    public CheckableValue(final String name, final CompoundButton input) {
        super(name);
        this.input = input;
    }

    @Override
    public Object getValue() {
        return this.input.isChecked();
    }

}
