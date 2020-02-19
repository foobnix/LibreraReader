package mobi.librera.smartreflow;

public interface PlatformImage<T> {
     int[] RED = new int[]{255, 0, 0};
     int[] GREEN = new int[]{0, 255, 0};
     int[] BLUE = new int[]{0, 0, 255};
     int[] WHITE = new int[]{255, 255, 255};
     int[] BLACK = new int[]{0, 0, 0};
     int[] YELLOW = new int[]{128, 255, 0};
     int[] MAROON = new int[]{128, 0, 0};


    void create(int width, int height);

    void load(T img);

    int getWidth();

    int getHeight();

    void setPixel(int x, int y, int color[]);

    void load(String path) throws Exception;

    T getImage();

    int[] getPixel(int x, int y);

    int getRedPixel(int x, int y);

    boolean isBlackPixel(int x, int y);





}
