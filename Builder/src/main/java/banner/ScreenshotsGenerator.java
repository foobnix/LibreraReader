package banner;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ScreenshotsGenerator {

    public static void main(String[] args) throws Exception {
        System.out.println("Start banners");

        String bg = "/home/data/Dropbox/Projects/Librera/pdf-v2.0-screenshots/Projects/Чистые фоны/Монтажная область ";
        String phone = "/home/data/Dropbox/Projects/Librera/Screenshots/6p.png";
        String screenshot = "/home/data/Dropbox/Projects/Librera/Screenshots/Source/";
        String out = "/home/data/Dropbox/Projects/Librera/Screenshots/gen/";

        for (File file : new File(out).listFiles()) {
            if (file.isFile()) {
                file.delete();
            }
        }
        // show(false, "Read aloud using TTS", bg + "4.png", phone, screenshot +
        // "1.png", out);

        for (File file : new File(screenshot).listFiles()) {
            String name = file.getName().replace(".png", "");
            show(false, name, bg + (new Random().nextInt(16) + 1) + ".png", phone, file.getPath(), out);
        }

        System.out.println("Finish");
    }

    public static void show(boolean show, String text, String img1, String img2, String img3, String out) throws Exception {
        int n = text.indexOf("_");
        if (n > 0) {
            text = text.substring(n + 1).trim();
        }

        System.out.println("Process: " + text);
        out += new File(img3).getName().replace(" ", "_");
        File outFile = new File(out);

        BufferedImage image1 = ImageIO.read(new File(img1));
        BufferedImage image2 = ImageIO.read(new File(img2));
        BufferedImage imgScreenshot = ImageIO.read(new File(img3));

        if (imgScreenshot.getWidth() > imgScreenshot.getHeight()) {
            ImageIO.write(imgScreenshot, "png", outFile);
            System.out.println("Write: " + outFile.getPath());
            return;
        }

        image2 = scale(image2, 1100);
        imgScreenshot = scale(imgScreenshot, 884);

        Graphics g = image1.getGraphics();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int center2 = (image1.getWidth() - image2.getWidth()) / 2;
        int center3 = (image1.getWidth() - imgScreenshot.getWidth()) / 2 + 5;
        g.drawImage(image2, center2, (image1.getHeight() - image2.getHeight()) + 300, null);
        g.drawImage(imgScreenshot, center3, (image1.getHeight() - imgScreenshot.getHeight()), null);
        int sWidth = 0;
        Font font = null;
        for (int fSize = 70; fSize > 20; fSize -= 4) {
            font = new Font("Arial", Font.BOLD, fSize);

            Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
            attributes.put(TextAttribute.TRACKING, 0.1);
            font = font.deriveFont(attributes);

            g.setFont(font);
            sWidth = g.getFontMetrics().stringWidth(text);
            if (sWidth < 1040) {
                break;
            }
        }

        int center4 = (image1.getWidth() - sWidth) / 2;

        g.setColor(Color.BLACK);

        int textY = 120;

        g.drawString(text, center4 + 4, textY + 4);

        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(text, center4, textY);

        g.dispose();

        ImageIO.write(image1, "png", outFile);
        System.out.println("Write: " + outFile.getPath());

        if (show) {

            if (true) {
                image1 = toBufferedImage(image1.getScaledInstance(image1.getWidth(null) / 2, image1.getHeight(null) / 2, Image.SCALE_DEFAULT));
            }

            final JFrame frame = new JFrame();
            JPanel panel = new JPanel();

            ImageIcon icon = new ImageIcon(image1);
            JLabel label = new JLabel();
            label.setIcon(icon);
            panel.add(label);
            frame.getContentPane().add(panel);
            frame.setVisible(true);
            frame.setSize(image1.getWidth(), image1.getHeight());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setResizable(false);

            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) { // handler
                    if (ke.getKeyCode() == ke.VK_ESCAPE) {
                        frame.dispose();
                    } else {
                    }
                }
            });
            frame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentHidden(ComponentEvent e) {
                    frame.dispose();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    frame.dispose();
                }
            });
        }

    }

    public static BufferedImage scale(BufferedImage img, int width) {
        float k = (float) img.getHeight() / img.getWidth();
        return toBufferedImage(img.getScaledInstance(width, (int) (width * k), Image.SCALE_SMOOTH));
    }

    public static BufferedImage toBufferedImage(Image img) {
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

}
