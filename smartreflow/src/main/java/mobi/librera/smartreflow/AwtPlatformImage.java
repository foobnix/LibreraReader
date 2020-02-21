package mobi.librera.smartreflow;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.imageio.ImageIO;

public class AwtPlatformImage implements PlatformImage<Image> {

    BufferedImage image;
    WritableRaster raster;

    public AwtPlatformImage() {

    }
    public AwtPlatformImage(PlatformImage image) {
        create(image.getWidth(), image.getHeight());

    }

    public AwtPlatformImage(String path) throws Exception {
        load(path);
    }

    public AwtPlatformImage(int w, int h) throws Exception {
        create(w, h);
    }

    @Override
    public void load(Image img) {
        image = (BufferedImage) img;
        raster = (WritableRaster) image.getData();
    }

    @Override
    public void load(String path) throws Exception {
        image = ImageIO.read(new File(path));
        raster = (WritableRaster) image.getData();
    }

    @Override
    public void create(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        raster = image.getData().createCompatibleWritableRaster();
    }


    @Override
    public int getWidth() {
        return raster.getWidth();
    }

    @Override
    public int getHeight() {
        return raster.getHeight();
    }

    @Override
    public void setPixel(int x, int y, int[] color) {
        try {
            raster.setPixel(x, y, color);
        }catch (Exception e){
            ImageUtils.log("setPixel", raster.getHeight(), raster.getHeight(), x,y);
            throw  e;
        }
    }

    @Override
    public Image getImage() {
        image.setData(raster);
        return image;
    }

    @Override
    public int[] getPixel(int x, int y) {
        int[] pixel = new int[4];
        raster.getPixel(x, y, pixel);
        return pixel;
    }

    @Override
    public int getRedPixel(int x, int y) {
        int[] pixel = new int[4];
        raster.getPixel(x, y, pixel);
        return pixel[0];
    }

    @Override
    public boolean isBlackPixel(int x, int y) {
        return getRedPixel(x, y) < 180;
    }
}
