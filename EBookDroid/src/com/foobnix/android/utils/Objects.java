package com.foobnix.android.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Objects {
    static String TAG = "Objects";

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreHashCode {

    }

    public static int hashCode(Object o) {
        StringBuilder res = new StringBuilder();

        for (Field f : o.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers())) {
                continue;
            }
            if (f.isAnnotationPresent(IgnoreHashCode.class)) {
                continue;
            }
            if (f.isAnnotationPresent(IgnoreHashCode.class)) {
                LOG.d("Objects", "Ignore", f.getName());
                continue;
            }
            try {
                res.append("" + f.get(o));
            } catch (Exception e) {
            }

        }
        int hashCode = res.toString().hashCode();
        LOG.d(TAG, "hashCode", hashCode);
        return hashCode;
    }

}
