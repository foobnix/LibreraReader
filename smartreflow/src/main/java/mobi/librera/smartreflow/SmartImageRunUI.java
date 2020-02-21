package mobi.librera.smartreflow;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

public class SmartImageRunUI {

    static GraphicsConfiguration gc;

    //static String img = "/home/ivan-dev/Downloads/testSM/2.jpg";
    static String img = "/home/ivan-dev/IdeaProjects/SmartReflow/images/sample1.png";

    public static void main(String[] args) throws Exception {
        runIU(img);
    }

    private static JFrame runIU(String path) throws Exception {

        AwtPlatformImage image = new AwtPlatformImage(path);

        SmartReflow1 reflow = new SmartReflow1();
        reflow.process(image);
//        reflow.isDrawResult = true;
//        reflow.isDrawResultUsingWords = true;
//
//        reflow.isDrawColums = true;
//        reflow.isDrawLines = false;
//        reflow.isDrawWords = true;
//        reflow.isDrawChars = false;
//        reflow.isDrawWordsOffsetLeft = true;


        AwtPlatformImage out = new AwtPlatformImage();
        reflow.drawObjects(out);

        final JFrame frame = new JFrame(gc);

        JPanel horizontal = new JPanel();
        horizontal.setLayout(new BoxLayout(horizontal, BoxLayout.LINE_AXIS));


        JPanel vertical = new JPanel();
        vertical.setBorder(new EmptyBorder(10, 10, 10, 10));
        vertical.setLayout(new BoxLayout(vertical, BoxLayout.PAGE_AXIS));
        vertical.setAlignmentY(Component.TOP_ALIGNMENT);


        //vertical.add(makeLabel("Original Image"));
        //Image originalImage = image.getImage();
        //if (originalImage != null) {
        //vertical.add(new JLabel(new ImageIcon(originalImage)));
        //}


        Image objects = out.getImage();
        vertical.add(makeLabel("Objects " + out.getWidth() + "x" + out.getHeight()));

        if (objects != null) {
            try {
                vertical.add(new JLabel(new ImageIcon(objects)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vertical.add(makeLabel("Statistics"));

        for (String line : reflow.getStatistics()) {
            vertical.add(makeText(line));
        }

        JPanel vertical2 = new JPanel();
        vertical2.setAlignmentY(Component.TOP_ALIGNMENT);
        vertical2.setBorder(new EmptyBorder(10, 10, 10, 10));
        vertical2.setLayout(new BoxLayout(vertical2, BoxLayout.PAGE_AXIS));


        try {
            final int w = 407;
            AwtPlatformImage out2 = new AwtPlatformImage(w, w * 2);
            reflow.reflow(out2);
            vertical2.add(makeLabel("Reflow " + out2.getWidth() + "x" + out2.getHeight()));


            Image reflowed = out2.getImage();
            if (reflowed != null) {
                vertical2.add(new JLabel(new ImageIcon(reflowed)));
            }
        } catch (Exception e) {
            vertical2.add(makeLabel("Error : " + e.getMessage()));
            e.printStackTrace();

        }


        horizontal.add(vertical);
        horizontal.add(vertical2);


        JPanel main = new JPanel();
        main.setAlignmentY(Component.TOP_ALIGNMENT);
        main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));

        final JButton open1 = new JButton("Open");
        open1.setAlignmentX(Component.LEFT_ALIGNMENT);
        open1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File my = new File("/home/ivan-dev/IdeaProjects/SmartReflow/src1");
                if (!my.exists()) {
                    my = new File(System.getProperty("user.dir"));

                }

                JFileChooser fileChooser = null;
                if (my.exists()) {
                    fileChooser = new JFileChooser(my);
                } else {
                    fileChooser = new JFileChooser();
                }

                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getPath().endsWith(".png") || f.getPath().endsWith(".jpg");
                    }

                    @Override
                    public String getDescription() {
                        return null;
                    }
                });
                int res = fileChooser.showOpenDialog(open1);
                if (res == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    //open.setText(file.getPath());
                    //frame.dispose();
                    //frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                    frame.setVisible(false);

                    try {
                        runIU(file.getPath());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });


        main.add(open1);
        main.add(new JScrollPane(horizontal));


        frame.add(main);

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
        return frame;
    }

    private static JLabel makeLabel(String txt) {
        JLabel originalImage = new JLabel(txt);
        originalImage.setFont(new Font("Serif", Font.BOLD, 28));
        return originalImage;
    }

    private static JLabel makeText(String txt) {
        JLabel originalImage = new JLabel(txt);
        originalImage.setFont(new Font("Monospaced", Font.PLAIN, 28));
        return originalImage;
    }
}
