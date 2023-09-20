package com;

import static org.junit.Assert.assertEquals;

import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.AppDB;


public class TestDB {

    public void test1() {
        FileMeta it1 = AppDB.get().load("/test");
        it1.setPages(10);
        AppDB.get().save(it1);

        Integer pages = AppDB.get().getOrCreate("/test").getPages();
        assertEquals(10, pages.intValue());
    }
}
