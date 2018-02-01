package com.foobnix.pdf.info;

import org.json.JSONObject;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

public class PageUrl {

    private String path;
    private int page;
    private int width;
    private int number;
    private int rotate;
    private boolean invert;
    private boolean crop;
    private int height;
    private int unic;
    private int cutp;
    private boolean isDouble;
    private boolean doText;

    public boolean tempWithWatermakr = false;

    public PageUrl(final String path, final int page, final int width, final int number, final boolean invert, final boolean crop, final int rotate) {
        this.path = path;
        this.page = page;
        this.width = width;
        this.number = number;
        this.invert = invert;
        this.crop = crop;
        this.rotate = rotate;
        unic = Dips.screenWidth();
    }

    public PageUrl(final String path, final int page, final int width, final int number, final boolean invert, final boolean crop, final int rotate, int heigth) {
        this.path = path;
        this.page = page;
        this.width = width;
        this.number = number;
        this.invert = invert;
        this.crop = crop;
        this.rotate = rotate;
        this.height = heigth;
        unic = Dips.screenWidth();
    }

    public PageUrl() {
        unic = Dips.screenWidth();
    }

    public static PageUrl buildSmall(String path, int page) {
        int w = Dips.dpToPx(AppState.get().coverSmallSize);
        int h = (int) (w * 1.5);
        return build(path, page, w, h);
    }

    public static int realToFake(int page) {
        if (AppState.get().isCut)
            return page * 2;

        if (AppState.get().isDouble) {
            int i = page / 2 + page % 2;
            if (AppState.get().isDoubleCoverAlone && page % 2 == 0) {
                i++;
            }
            return i;
        }
        return page;
    }

    public static int fakeToReal(int page) {
        if (AppState.get().isCut)
            return page / 2 + page % 2;

        if (AppState.get().isDouble)
            return page * 2;

        return page;
    }

    public static PageUrl build(String path, int page, int w, int h) {
        PageUrl url = new PageUrl();
        url.setPath(path);
        url.setPage(AppState.get().isCut ? page / 2 : page);
        url.setWidth(AppState.get().isCut ? (int) (w * 2) : w);
        url.setHeight(AppState.get().isCut ? (int) (h * 2) : h);
        url.setInvert(!AppState.get().isDayNotInvert);
        url.setCrop(AppState.get().isCrop);
        url.setRotate(AppState.get().rotate);
        url.setCutp(AppState.get().cutP);

        if (AppState.get().isCut) {
            if (AppState.get().isCutRTL) {
                url.setNumber(page % 2 == 0 ? 2 : 1);
            } else {
                url.setNumber(page % 2 == 0 ? 1 : 2);
            }
        }

        if (AppState.get().isDouble && !(AppState.get().isDoubleCoverAlone && page == 0)) {
            url.setPage(page * 2);
            url.setDouble(AppState.get().isDouble);

            if (!ExtUtils.isTextFomat(path)) {
                url.setWidth((int) (w * 0.7));
                url.setHeight((int) (h * 0.7));
            }
        }
        return url;

    }

    @Override
    public String toString() {
        try {
            final JSONObject obj = new JSONObject();
            obj.put("path", path);
            obj.put("page", page);
            obj.put("width", width);
            obj.put("number", number);
            obj.put("rotate", rotate);
            obj.put("invert", invert);
            obj.put("crop", crop);
            obj.put("height", height);
            obj.put("unic", unic);
            obj.put("cutp", cutp);
            obj.put("m", MagicHelper.hash());
            obj.put("isDouble", isDouble);
            obj.put("doText", doText);
            return obj.toString();
        } catch (final Exception e) {
            return "";
        }
    }

    public static PageUrl fromString(final String str) {
        try {
            final JSONObject obj = new JSONObject(str);
            final PageUrl url = new PageUrl();
            url.path = obj.optString("path");
            url.page = obj.optInt("page");
            url.width = obj.optInt("width");
            url.number = obj.optInt("number");
            url.rotate = obj.optInt("rotate");
            url.invert = obj.optBoolean("invert");
            url.crop = obj.optBoolean("crop");
            url.height = obj.optInt("height");
            url.unic = obj.optInt("unic");
            url.cutp = obj.optInt("cutp");
            url.isDouble = obj.optBoolean("isDouble");
            url.doText = obj.optBoolean("doText");
            return url;
        } catch (final Exception e) {

        }
        return null;

    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public int getPage() {
        return page;
    }

    public void setPage(final int page) {
        this.page = page;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(final int number) {
        this.number = number;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(final boolean invert) {
        this.invert = invert;
    }

    public boolean isCrop() {
        return crop;
    }

    public void setCrop(final boolean crop) {
        this.crop = crop;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(final int rotate) {
        this.rotate = rotate;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCutp() {
        return cutp;
    }

    public void setCutp(int cutp) {
        this.cutp = cutp;
    }

    public void setUnic(int unic) {
        this.unic = unic;
    }

    public boolean isDouble() {
        return isDouble;
    }

    public void setDouble(boolean isDouble) {
        this.isDouble = isDouble;
    }

    public boolean isDoText() {
        return doText;
    }

    public void setDoText(boolean doText) {
        this.doText = doText;
    }

}
