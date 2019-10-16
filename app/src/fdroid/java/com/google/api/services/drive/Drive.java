package com.google.api.services.drive;


import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;

import at.stefl.commons.util.object.ObjectFactory;

public class Drive {

    public Drive files() {
        return null;
    }

    public Drive update(String id, File metadata) {
        return null;
    }

    public File execute() {
        return null;
    }

    public Drive create(File metadata) {
        return null;
    }

    public Drive list() {
        return null;
    }

    public Drive setSpaces(String drive) {
        return null;
    }

    public Drive setQ(String q) {
        return null;
    }

    public Drive setPageToken(String nextPageToken) {
        return null;
    }

    public Drive update(String id, File metadata, ByteArrayContent contentStream) {
        return null;

    }

    public Drive update(String id, File metadata, FileContent contentStream) {
        return null;
    }

    public Drive get(String fileId) {
        return null;
    }

    public InputStream executeMediaAsInputStream() throws IOException {
            return null;
    }

    public InputStream executeAsInputStream() {
            return null;
    }

    public Drive setFields(String s) {
        return null;
    }

    public Drive setPageSize(int pageSize) {
        return null;
    }

    public Drive setOrderBy(String modifiedTime) {
        return null;
    }

    public static class Builder {

        public Builder(Object newCompatibleTransport, GsonFactory gsonFactory, GoogleAccountCredential credential) {
        }

        public Builder setApplicationName(String applicationName) {
            return null;
        }

        public Drive build() {
            return null;
        }
    }
}
