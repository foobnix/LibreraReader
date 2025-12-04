package com.foobnix.android.utils;

public class MemoryUtils {

    public static long MAX_MEMORY_SIZE = Runtime.getRuntime().maxMemory();
    public static long RECOMENDED_MEMORY_SIZE = getRecomendedAllocatedSizeInMB();

    public static long getRecomendedAllocatedSizeInMB() {
        long recomended = getMaxSizeInMB() / 2;

        if (recomended >= 96) {
            return 96;
        }
        return recomended;
    }

    public static long getMaxSizeInMB() {
        return MAX_MEMORY_SIZE / 1024 / 1024;
    }

}
