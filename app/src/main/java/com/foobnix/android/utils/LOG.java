package com.foobnix.android.utils;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class LOG {
    public static boolean isEnable = false;
    public static String TAG = "DEBUG";
    public static String DELIMITER = "|";

    public static void printlog(String statement) {
        if (isEnable) {
            Log.d(TAG, statement);
        }
    }



    public static String toString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void d(Object msg1, Object... statement) {
        if (isEnable) {
            if (statement.length == 0) {
                Log.d(TAG, msg1.toString());
                return;
            }
            String msg = asString(statement);
            if (msg != null && msg.length() > 4000) {
                Log.d(msg1 + "[part1]", msg.substring(0, 4000));
                Log.d(msg1 + "[part2]", msg.substring(4000));
            } else {
                Log.d(msg1.toString(), msg);
            }
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

    public static void w(Throwable e, Object... statement) {
        if (isEnable) {
            Log.w(TAG, asString(statement), e);
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
                Object v = f.get(obj);
                if (v == null) {
                    out.append("@null");
                } else {
                    out.append(v);
                }
                out.append("|\n");
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        out.append("======== [ End ] ========");
        return out.toString();
    }

}
