package com.mobi;

import java.nio.ByteBuffer;

public class MobiImage {

    public int id;
    public ByteBuffer data;

    public MobiImage(int id, ByteBuffer data) {
        super();
        this.id = id;
        this.data = data;
    }

}
