package com.foobnix.pdf.info.view;

import android.widget.AdapterView;

public abstract class ItemClickListenerWithReference<T> implements AdapterView.OnItemClickListener {
    public T reference;

    public void setReference(T reference){
        this.reference = reference;
    }
}
