package com.foobnix.android.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import android.util.Log;

public class LOG {
    public static boolean isEnable = true;
    public static String TAG = "DEBUG";
    public static String DELIMITER = "|";

    public static void printlog(String statement) {
        if (isEnable) {
            Log.d(TAG, statement);
        }
    }

    public static void d(Object... statement) {
        if (isEnable) {
            Log.d(TAG, asString(statement));
        }
    }

    public static void dMeta(Object... statement) {
        String meta = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            meta = asString(stackTrace[3].getClassName(), stackTrace[3].getMethodName(), stackTrace[3].getLineNumber());
        }

        d(meta, asString(statement));

    }

    public static void e(Throwable e, Object... statement) {
        if (isEnable) {
            Log.e(TAG, asString(statement), e);
        }
    }

    private static String asString(Object... statements) {
        return TxtUtils.join(DELIMITER, statements) + "|";
    }

    public static String ojectAsString(Object obj) {
        if (!isEnable) {
            return null;
        }
        StringBuffer out = new StringBuffer();

        out.append("======== [ Begin ] ======== \n");
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            out.append(f.getName());
            out.append(":");
            try {
                out.append(f.get(obj));
                out.append("|\n");
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        out.append("======== [ End ] ========");
        return out.toString();
    }

}
