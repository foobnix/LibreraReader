package com;

import com.foobnix.model.MyPath;
import com.foobnix.model.SimpleMeta;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSync {
    @Test
    public void mergeMyPath() {
       assertEquals("1",MyPath.toAbsolute("1"));
    }

    @Test
    public void merge1() {
        SimpleMeta s1 = new SimpleMeta("2", 2);
        SimpleMeta s2 = new SimpleMeta("2", 3);

        //assertEquals(s2, ExportConverter.merge(s1, s2));
    }

    @Test
    public void merge2() {
        SimpleMeta s1 = new SimpleMeta("2", 3);
        SimpleMeta s2 = new SimpleMeta("2", 2);

        //assertEquals(s1, ExportConverter.merge(s1, s2));
    }

    @Test
    public void merge3() {
        SimpleMeta s1 = new SimpleMeta("2", 2);
        SimpleMeta s2 = new SimpleMeta("2", 2);

        //assertEquals(s1, ExportConverter.merge(s1, s2));
    }



}
