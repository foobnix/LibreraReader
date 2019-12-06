package com.foobnix.android.utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class Objects {
    static String TAG = "Objects";

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreHashCode {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreCalculateHashCode {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SaveToSharedPreferences {
    }

    public static String toJSONString(Object obj) {
        return toJSONObject(obj).toString();
    }

    public static LinkedJSONObject toJSONObject(Object obj) {
        LOG.d(TAG, "saveToSP");
        final LinkedJSONObject edit = new LinkedJSONObject();
        for (final Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            try {
                edit.put(f.getName(), f.get(obj));
                LOG.d(TAG, "saveToSP", f.getType(), f.getName(), f.get(obj));
            } catch (Exception e) {
                LOG.e(e, f.getName());
            }
        }
        return edit;
    }

    public static void loadFromJson(Object obj, String json) {
        try {
            loadFromJson(obj, new LinkedJSONObject(json));
        } catch (JSONException e) {
            LOG.e(e);
        }
    }

    public static void loadFromJson(Object obj, LinkedJSONObject sp) {
        try {
            for (final Field f : obj.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                    continue;
                }

                try {
                    if (f.getType().equals(int.class)) {
                        f.setInt(obj, sp.optInt(f.getName(), f.getInt(obj)));
                    } else if (f.getType().equals(String.class)) {
                        Object object = f.get(obj);
                        f.set(obj, sp.optString(f.getName(), object != null ? "" + object : null));
                    } else if (f.getType().equals(float.class)) {
                        f.setFloat(obj, (float) sp.optDouble(f.getName(), f.getDouble(obj)));
                    } else if (f.getType().equals(long.class)) {
                        f.setLong(obj, sp.optLong(f.getName(), f.getLong(obj)));
                    } else if (f.getType().equals(boolean.class)) {
                        f.setBoolean(obj, sp.optBoolean(f.getName(), f.getBoolean(obj)));
                    }

                    LOG.d(TAG, "loadFromSp", f.getType(), f.getName(), f.get(obj));

                } catch (Exception e) {
                    LOG.e(e);
                }

            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void saveToSP(Object obj, SharedPreferences sp) {
        LOG.d(TAG, "saveToSP");

        Editor edit = sp.edit();
        for (final Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            try {

                LOG.d(TAG, "saveToSP", f.getType(), f.getName(), f.get(obj));

                if (f.getType().equals(int.class)) {
                    edit.putInt(f.getName(), f.getInt(obj));
                } else if (f.getType().equals(String.class)) {
                    Object object = f.get(obj);
                    edit.putString(f.getName(), object != null ? object.toString() : null);
                } else if (f.getType().equals(float.class)) {
                    edit.putFloat(f.getName(), f.getFloat(obj));
                } else if (f.getType().equals(long.class)) {
                    edit.putLong(f.getName(), f.getLong(obj));
                } else if (f.getType().equals(boolean.class)) {
                    edit.putBoolean(f.getName(), f.getBoolean(obj));
                } else if (f.getType().equals(java.util.Set.class)) {
                    edit.putStringSet(f.getName(), (Set<String>) f.get(obj));
                }

            } catch (Exception e) {
                LOG.e(e, f.getName());
            }
        }
        edit.commit();
    }

    public static void loadFromSp(Object obj, SharedPreferences sp) {

        for (final Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }

            try {
                if (f.getType().equals(int.class)) {
                    f.setInt(obj, sp.getInt(f.getName(), f.getInt(obj)));
                } else if (f.getType().equals(String.class)) {
                    Object object = f.get(obj);
                    f.set(obj, sp.getString(f.getName(), object != null ? "" + object : null));
                } else if (f.getType().equals(float.class)) {
                    f.setFloat(obj, sp.getFloat(f.getName(), f.getFloat(obj)));
                } else if (f.getType().equals(long.class)) {
                    f.setLong(obj, sp.getLong(f.getName(), f.getLong(obj)));
                } else if (f.getType().equals(boolean.class)) {
                    f.setBoolean(obj, sp.getBoolean(f.getName(), f.getBoolean(obj)));
                } else if (f.getType().equals(java.util.Set.class)) {
                    f.set(obj, sp.getStringSet(f.getName(), new HashSet<String>()));
                }

                LOG.d(TAG, "loadFromSp", f.getType(), f.getName(), f.get(obj));

            } catch (Exception e) {
                LOG.e(e);
            }
        }

    }

    public static int appHash() {
        return Objects.hashCode(BookCSS.get(), AppState.get(), AppSP.get().hypenLang);
    }

    public static int hashCode(Object... objects) {
        int res = 0;
        for (Object o : objects) {
            res += hashCode(o, true);
        }
        return res;
    }

    static String hashStringID = "hashCode";

    public static int hashCode(Object obj, boolean ignoreSomeHash) {
        if (obj == null) {
            return 0;
        } else if (obj instanceof String) {
            return obj.hashCode();
        } else if (obj instanceof Integer) {
            return (int) obj;
        }
        StringBuilder res = new StringBuilder();

        final Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field f : declaredFields) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers())) {
                continue;
            }
            if (ignoreSomeHash && f.isAnnotationPresent(IgnoreHashCode.class)) {
                continue;
            }
            if (f.isAnnotationPresent(IgnoreCalculateHashCode.class)) {
                continue;
            }
            if (f.getName().equals(hashStringID)) {
                continue;
            }


            try {
                if (f.getType().equals(float.class)) {
                    res.append(f.getName() + ":" + TxtUtils.substring(f.get(obj).toString(), 6) + ",");
                } else {
                    res.append(f.getName() + ":" + f.get(obj) + ",");
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
        int hashCode = res.toString().hashCode();
        LOG.d(TAG, "hashCodeString", hashCode, res.toString());
        return hashCode;
    }


    public static void compareObjects(Object obj1, AppState obj2) {
        for (Field f : obj1.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers())) {
                continue;
            }

            try {

                String value1 = "" + f.get(obj1);
                String value2 = "" + f.get(obj2);
                if (!value1.equals(value2)) {
                    LOG.d(TAG, "compareObjects not same", f.getName(), value1, value2);
                }

            } catch (Exception e) {
                LOG.e(e);
            }

        }

    }


    public static Object getInstanceValue(final Object classInstance, final String fieldName) throws SecurityException, NoSuchFieldException,
            ClassNotFoundException, IllegalArgumentException, IllegalAccessException {

        // Get the private field
        final Field field = classInstance.getClass().getDeclaredField(fieldName);
        // Allow modification on the field
        field.setAccessible(true);
        // Return the Obect corresponding to the field
        return field.get(classInstance);

    }

}
