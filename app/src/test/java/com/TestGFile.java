package com;

import com.foobnix.sync.GSync;

import org.junit.Test;

import java.io.File;
import java.util.List;

public class TestGFile {

    @Test
    public void test1() {
        GSync sync = new GSync();

        List<File> local = sync.getRemoteFiles();
        List<File> remote = sync.getLocalFiles();

        sync.merge(remote, local);

    }
    @Test
    public void testMergeFile(){
        File local = new File("");
        File remote = new File("");

        //check to delete

        //check to merge

        //check to download

        //check to upload





    }
}
