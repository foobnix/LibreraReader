package org.ebookdroid.common.touch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.ebookdroid.common.settings.AppSettings;
import org.emdev.ui.actions.ActionEx;
import org.emdev.utils.LengthUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import  com.foobnix.pdf.info.R;

import android.graphics.Rect;
import android.graphics.RectF;

public class TouchManager {


    public static final String DEFAULT_PROFILE = "DocumentView.Default";

    private static final Map<String, TouchProfile> profiles = new HashMap<String, TouchManager.TouchProfile>();

    private static final LinkedList<TouchProfile> stack = new LinkedList<TouchProfile>();

    public static void loadFromSettings(final AppSettings newSettings) {
        profiles.clear();
        stack.clear();

        boolean fromJSON = false;
        final String str = newSettings.tapProfiles;
        if (LengthUtils.isNotEmpty(str)) {
            try {
                final List<TouchProfile> list = fromJSON(str);
                for (final TouchProfile p : list) {
                    profiles.put(p.name, p);
                }
            } catch (final Throwable ex) {
                ex.printStackTrace();
            }
            fromJSON = profiles.containsKey(DEFAULT_PROFILE);
        }

        if (!fromJSON) {
            final TouchProfile def = addProfile(DEFAULT_PROFILE);
            {
                final Region r = def.addRegion(0, 0, 100, 100);
                r.setAction(Touch.DoubleTap, R.id.adFrame, true);
            }
            {
                final Region r = def.addRegion(80, 0, 100, 20);
                r.setAction(Touch.DoubleTap, R.id.adFrame, true);
            }
            {
                final Region r = def.addRegion(0, 0, 100, 10);
                r.setAction(Touch.SingleTap, R.id.adFrame, true);
            }
            {
                final Region r = def.addRegion(0, 90, 100, 100);
                r.setAction(Touch.SingleTap, R.id.adFrame, true);
            }

            persist();
        }

        stack.addFirst(profiles.get(DEFAULT_PROFILE));
    }

    public static void persist() {
    }

    public static void setActionEnabled(final String profile, final int id, final boolean enabled) {
        final TouchProfile tp = profiles.get(profile);
        for (final Region r : tp.regions) {
            for (final ActionRef a : r.actions) {
                if (a != null && a.id == id) {
                    a.enabled = enabled;
                }
            }
        }
    }

    public static void setActionEnabled(final String profile, final int id, final boolean enabled, final int left,
            final int top, final int right, final int bottom) {
        final TouchProfile tp = profiles.get(profile);
        for (final Region r : tp.regions) {
            for (final ActionRef a : r.actions) {
                if (a != null && a.id == id) {
                    a.enabled = enabled;
                    r.rect.left = left;
                    r.rect.top = top;
                    r.rect.right = right;
                    r.rect.bottom = bottom;
                    return;
                }
            }
        }
    }

    public static Integer getAction(final Touch type, final float x, final float y, final float width,
            final float height) {
        return AppSettings.getInstance().tapsEnabled ? stack.peek().getAction(type, x, y, width, height) : null;
    }

    public static TouchProfile addProfile(final String name) {
        final TouchProfile tp = new TouchProfile(name);
        profiles.put(tp.name, tp);
        return tp;
    }

    public static TouchProfile topProfile() {
        return stack.isEmpty() ? null : stack.peek();
    }

    public static TouchProfile pushProfile(final String name) {
        final TouchProfile prev = stack.isEmpty() ? null : stack.peek();
        final TouchProfile tp = profiles.get(name);
        if (tp != null) {
            stack.addFirst(tp);
        }
        return prev;
    }

    public static TouchProfile popProfile() {
        if (stack.size() > 1) {
            stack.removeFirst();
        }
        return stack.peek();
    }

    public static JSONObject toJSON() throws JSONException {
        final JSONObject object = new JSONObject();
        final JSONArray array = new JSONArray();
        for (final TouchProfile p : profiles.values()) {
            array.put(p.toJSON());
        }
        object.put("profiles", array);
        return object;
    }

    private static List<TouchProfile> fromJSON(final String str) throws JSONException {
        final List<TouchProfile> list = new ArrayList<TouchProfile>();

        final JSONObject root = new JSONObject(str);

        final JSONArray profiles = root.getJSONArray("profiles");
        for (int pIndex = 0; pIndex < profiles.length(); pIndex++) {
            final JSONObject p = profiles.getJSONObject(pIndex);
            final TouchProfile profile = TouchProfile.fromJSON(p);
            list.add(profile);
        }
        return list;
    }

    public static class TouchProfile {

        public final String name;
        final LinkedList<Region> regions = new LinkedList<Region>();

        public TouchProfile(final String name) {
            super();
            this.name = name;
        }

        public ListIterator<Region> regions() {
            return regions.listIterator();
        }

        public void clear() {
            regions.clear();
        }

        public Integer getAction(final Touch type, final float x, final float y, final float width, final float height) {
            for (final Region r : regions) {
                final RectF rect = r.getActualRect(width, height);
                if (rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom) {
                    final ActionRef action = r.getAction(type);
                    if (action != null && action.enabled) {
                        return action.id;
                    }
                }
            }
            return null;
        }

        public Region getRegion(final float x, final float y, final float width, final float height) {
            for (final Region r : regions) {
                final RectF rect = r.getActualRect(width, height);
                if (rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom) {
                    return r;
                }
            }
            return null;
        }

        public Region addRegion(final int left, final int top, final int right, final int bottom) {
            final Region r = new Region(new Rect(left, top, right, bottom));
            return addRegion(r);
        }

        public Region addRegion(final Region r) {
            regions.addFirst(r);
            return r;
        }

        public void removeRegion(final Region r) {
            regions.remove(r);
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            buf.append("name").append("=").append(name);
            buf.append(", ");
            buf.append("regions").append("=").append(regions);
            buf.append("]");

            return buf.toString();
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();
            object.put("name", this.name);

            final JSONArray array = new JSONArray();
            for (final Region r : regions) {
                array.put(r.toJSON());
            }
            object.put("regions", array);

            return object;
        }

        public static TouchProfile fromJSON(final JSONObject json) throws JSONException {
            final TouchProfile profile = new TouchProfile(json.getString("name"));

            final JSONArray regions = json.getJSONArray("regions");
            for (int rIndex = 0; rIndex < regions.length(); rIndex++) {
                final JSONObject r = regions.getJSONObject(rIndex);
                final Region region = Region.fromJSON(r);
                profile.regions.add(region);
            }
            return profile;
        }
    }

    public static enum Touch {
        SingleTap, DoubleTap, LongTap, TwoFingerTap;
    }

    public static class Region {

        private final Rect rect;
        private final ActionRef[] actions = new ActionRef[Touch.values().length];

        public Region(final Rect r) {
            rect = r;
        }

        public Region(final Region r) {
            this.rect = new Rect(r.rect);
            for (int i = 0; i < actions.length; i++) {
                this.actions[i] = r.actions[i];
            }
        }

        public Rect getRect() {
            return rect;
        }

        public ActionRef getAction(final Touch type) {
            return actions[type.ordinal()];
        }

        public ActionRef setAction(final Touch type, final int id, final boolean enabled) {
            final ActionRef a = new ActionRef(type, id, enabled);
            actions[type.ordinal()] = a;
            return a;
        }

        public RectF getActualRect(final float width, final float height) {
            return new RectF(width * rect.left / 100.0f, height * rect.top / 100.0f, width * rect.right / 100.0f,
                    height * rect.bottom / 100.0f);
        }

        public void clear(Touch type) {
            this.actions[type.ordinal()] = null;
        }

        public void clear() {
            for (int i = 0; i < actions.length; i++) {
                this.actions[i] = null;
            }
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            buf.append("rect").append("=").append(rect);
            buf.append(", ");
            buf.append("actions").append("=").append(Arrays.toString(actions));
            buf.append("]");

            return buf.toString();
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();

            final JSONObject r = new JSONObject();
            r.put("left", rect.left);
            r.put("top", rect.top);
            r.put("right", rect.right);
            r.put("bottom", rect.bottom);
            object.put("rect", r);

            final JSONArray a = new JSONArray();
            for (final ActionRef action : actions) {
                if (action != null) {
                    a.put(action.toJSON());
                }
            }
            object.put("actions", a);
            return object;
        }

        public static Region fromJSON(final JSONObject json) throws JSONException {
            final JSONObject r = json.getJSONObject("rect");
            final Rect rect = new Rect(r.getInt("left"), r.getInt("top"), r.getInt("right"), r.getInt("bottom"));

            final Region region = new Region(rect);
            final JSONArray actions = json.getJSONArray("actions");
            for (int aIndex = 0; aIndex < actions.length(); aIndex++) {
                try {
                    final JSONObject a = actions.getJSONObject(aIndex);
                    final Touch type = Touch.valueOf(a.getString("type"));
                    final String name = a.getString("name");
                    final Integer id = ActionEx.getActionId(name);
                    if (id != null) {
                        region.setAction(type, id, a.getBoolean("enabled"));
                    } else {
                    }
                } catch (final JSONException ex) {
                    throw new JSONException("Old perssitent format found. Touch action are returned to default ones: "
                            + ex.getMessage());
                }
            }
            return region;
        }
    }

    public static class ActionRef {

        public final Touch type;
        public final int id;
        public final String name;
        public boolean enabled;

        public ActionRef(final Touch type, final int id, final boolean enabled) {
            this.type = type;
            this.id = id;
            this.name = ActionEx.getActionName(id);
            this.enabled = enabled;
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject object = new JSONObject();
            object.put("type", type.name());
            object.put("name", name);
            object.put("enabled", enabled);
            return object;
        }

        @Override
        public String toString() {
            return "(" + type + ", " + name + ", " + enabled + ")";
        }
    }
}
