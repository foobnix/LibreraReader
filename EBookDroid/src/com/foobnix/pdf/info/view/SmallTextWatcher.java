package com.foobnix.pdf.info.view;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SmallTextWatcher implements TextWatcher {

    public SmallTextWatcher() {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        onTextChanged(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public abstract void onTextChanged(String text);

}
