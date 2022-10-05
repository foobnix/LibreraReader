package com.cloudrail.si.interfaces;

import com.cloudrail.si.types.CloudMetaData;
import com.cloudrail.si.types.SpaceAllocation;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class CloudStorage {

    public List<CloudMetaData> getChildren(String str){
        return Collections.emptyList();
    }
    public void logout(){

    }
    public void  delete(String path){

    }

    public boolean exists(String path){
        return false;
    }

    public void loadAsString(String str){


    }

    public SpaceAllocation getAllocation() {
        return new SpaceAllocation();
    }

    public void login() {
    }

    public String saveAsString() {
    return "";
    }

    public String getUserLogin() {
        return "";
    }

    public void useAdvancedAuthentication() {
    }

    public void createFolder(String extSyncFolder) {
    }

    public InputStream download(String path) {
        return null;
    }

    public void upload(String extSyncFile, FileInputStream outStream, long length, boolean b) {
    }
}
