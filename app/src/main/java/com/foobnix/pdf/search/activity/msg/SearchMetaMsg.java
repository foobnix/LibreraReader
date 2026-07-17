package com.foobnix.pdf.search.activity.msg;

import com.foobnix.ui2.AppDB;

public class SearchMetaMsg {
    public AppDB.SEARCH_IN mode;
    public String text;

    public SearchMetaMsg(){

    }
    public SearchMetaMsg(AppDB.SEARCH_IN mode, String text){
            this.mode = mode;
            this.text = text;
    }



}
