package org.emdev.ui.actions.params;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

public class EditableValue extends AbstractActionParameter {

    private final EditText input;

    public EditableValue(final String name, final EditText input) {
        super(name);
        this.input = input;
    }

    @Override
    public Object getValue() {
        if ((this.input.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0) {
            return new PasswordEditable(this.input.getText());
        }
        return this.input.getText();
    }

    public static class PasswordEditable implements Editable {

        private final Editable editable;

        PasswordEditable(final Editable editable) {
            super();
            this.editable = editable;
        }

        public String getPassword() {
            return editable.toString();
        }

        @Override
        public void getChars(final int start, final int end, final char[] dest, final int destoff) {
            editable.getChars(start, end, dest, destoff);
        }

        @Override
        public void setSpan(final Object what, final int start, final int end, final int flags) {
            editable.setSpan(what, start, end, flags);
        }

        @Override
        public Editable replace(final int st, final int en, final CharSequence source, final int start, final int end) {
            return editable.replace(st, en, source, start, end);
        }

        @Override
        public int length() {
            return editable.length();
        }

        @Override
        public char charAt(final int index) {
            return editable.charAt(index);
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return editable.subSequence(start, end);
        }

        @Override
        public void removeSpan(final Object what) {
            editable.removeSpan(what);
        }

        @Override
        public Editable replace(final int st, final int en, final CharSequence text) {
            return editable.replace(st, en, text);
        }

        @Override
        public Editable insert(final int where, final CharSequence text, final int start, final int end) {
            return editable.insert(where, text, start, end);
        }

        @Override
        public Editable insert(final int where, final CharSequence text) {
            return editable.insert(where, text);
        }

        @Override
        public String toString() {
            return "*******";
        }

        @Override
        public Editable delete(final int st, final int en) {
            return editable.delete(st, en);
        }

        @Override
        public Editable append(final CharSequence text) {
            return editable.append(text);
        }

        @Override
        public Editable append(final CharSequence text, final int start, final int end) {
            return editable.append(text, start, end);
        }

        @Override
        public Editable append(final char text) {
            return editable.append(text);
        }

        @Override
        public void clear() {
            editable.clear();
        }

        @Override
        public void clearSpans() {
            editable.clearSpans();
        }

        @Override
        public void setFilters(final InputFilter[] filters) {
            editable.setFilters(filters);
        }

        @Override
        public InputFilter[] getFilters() {
            return editable.getFilters();
        }

        @Override
        public <T> T[] getSpans(final int start, final int end, final Class<T> type) {
            return editable.getSpans(start, end, type);
        }

        @Override
        public int getSpanStart(final Object tag) {
            return editable.getSpanStart(tag);
        }

        @Override
        public int getSpanEnd(final Object tag) {
            return editable.getSpanEnd(tag);
        }

        @Override
        public int getSpanFlags(final Object tag) {
            return editable.getSpanFlags(tag);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public int nextSpanTransition(final int start, final int limit, final Class type) {
            return editable.nextSpanTransition(start, limit, type);
        }
    }
}
