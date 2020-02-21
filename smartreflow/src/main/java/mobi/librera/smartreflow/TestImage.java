package mobi.librera.smartreflow;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class TestImage {

    static String img = "/home/ivan-dev/IdeaProjects/SmartReflow/images/sample6.png";


    static GraphicsConfiguration gc;

    public static void main(String[] args) throws Exception {
        final JFrame frame = new JFrame(gc);

        final AwtPlatformImage input = new AwtPlatformImage(img);

        final AwtPlatformImage output = new AwtPlatformImage(600,1000);

        SmartReflow sm = new SmartReflow(input);
        sm.reflow(output);


        frame.add(new JLabel(new ImageIcon(output.getImage())));


        frame.setTitle("Librera Native Reflow");
        //frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) { // handler
                if (ke.getKeyCode() == ke.VK_ESCAPE) {
                    frame.dispose();
                } else {
                }
            }
        });
    }



    public static void test1(PlatformImage image, PlatformImage output) throws Exception {

        image.create(200, 200);
        //image.load(img);


        ImageUtils.setBackgroundColor(image, PlatformImage.WHITE);
        for (int x = 0; x < 200; x++) {
            image.setPixel(x, x, PlatformImage.RED);
        }
        ImageUtils.drawRect(image, 10, 10, 100, 100, PlatformImage.BLUE);


        output.create(400, 400);

        ImageUtils.copyRect(image, output, 0, 0, 150, 150);

    }
}
