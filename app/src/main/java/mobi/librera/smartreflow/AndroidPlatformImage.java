package mobi.librera.smartreflow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

public class AndroidPlatformImage implements PlatformImage<Bitmap> {


    Bitmap bitmap;

    public AndroidPlatformImage(){

    }

    public AndroidPlatformImage(Bitmap bitmap){
        this.bitmap = bitmap;

    }
    public  AndroidPlatformImage(int w, int h) {
        create(w, h);
    }

    @Override
    public void load(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public void create(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    }

    @Override
    public void load(String path) {
        bitmap = BitmapFactory.decodeFile(path);
    }

    @Override
    public int getWidth() {
        return bitmap.getWidth();
    }

    @Override
    public int getHeight() {
        return bitmap.getHeight();
    }

    @Override
    public void setPixel(int x, int y, int color[]) {
        final int c = Color.rgb(color[0], color[1], color[2]);
        bitmap.setPixel(x, y, c);
    }

    @Override
    public int[] getPixel(int x, int y) {
        final int color = bitmap.getPixel(x, y);
        int[] pixels = new int[3];
        pixels[0] = Color.red(color);
        pixels[1] = Color.green(color);
        pixels[2] = Color.blue(color);
        return pixels;
    }

    @Override
    public boolean isBlackPixel(int x, int y) {
        return getRedPixel(x, y) < 180l;
    }

    @Override
    public int getRedPixel(int x, int y) {
        final int color = bitmap.getPixel(x, y);
        return Color.red(color);
    }

    @Override
    public Bitmap getImage() {
        return bitmap;
    }
}
