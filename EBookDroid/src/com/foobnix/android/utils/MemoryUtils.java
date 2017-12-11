package com.foobnix.android.utils;

public class MemoryUtils {

    public static long MAX_MEMORY_SIZE = Runtime.getRuntime().maxMemory();
    public static long RECOMENDED_MEMORY_SIZE = getRecomendedAllocatedSizeInMB();
    public static boolean IS_SMALL_MEMORY_SIZE = RECOMENDED_MEMORY_SIZE <= 16;

    public static long getRecomendedAllocatedSizeInMB() {
        long recomended = getMaxSizeInMB() / 2;
        if (recomended <= 16) {
            return 16;
        }
        if (recomended >= 256) {
            return recomended;
        }
        return recomended;
    }

    public static long getMaxSizeInMB() {
        return MAX_MEMORY_SIZE / 1024 / 1024;
    }

}
