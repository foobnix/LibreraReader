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
import java.io.IOException;

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

public class Main {

    static GraphicsConfiguration gc;

    static String img = "/home/data/IdeaProjects/SmartReflow/images/sample6.png";

    public static void main(String[] args) throws IOException {
        runIU(img);
    }

    private static JFrame runIU(String path) throws IOException {
        LibreraSmartReflow reflow = new LibreraSmartReflow(path);
        reflow.isDrawResult = true;
        reflow.isDrawResultUsingWords = true;

        reflow.isDrawColums = false;
        reflow.isDrawLines = false;
        reflow.isDrawWords = true;
        reflow.isDrawChars = false;
        reflow.isDrawWordsOffsetLeft = true;


        int width = 800;
        int heigth = 800*10;

        final JFrame frame = new JFrame(gc);

        JPanel horizontal = new JPanel();
        horizontal.setLayout(new BoxLayout(horizontal, BoxLayout.LINE_AXIS));


        JPanel vertical = new JPanel();
        vertical.setBorder(new EmptyBorder(10, 10, 10, 10));
        vertical.setLayout(new BoxLayout(vertical, BoxLayout.PAGE_AXIS));
        vertical.setAlignmentY(Component.TOP_ALIGNMENT);


        vertical.add(makeLabel("Original Image"));
        Image originalImage = reflow.getOriginalImage();
        if (originalImage != null) {
            vertical.add(new JLabel(new ImageIcon(originalImage)));
        }


        vertical.add(makeLabel("Objects"));
        Image objects = reflow.getObjects();
        if (objects != null) {
            try {
                vertical.add(new JLabel(new ImageIcon(objects)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vertical.add(makeLabel("Statistics"));
        vertical.add(makeText("Columns :" + reflow.columns.size()));
        vertical.add(makeText("Lines   :" + reflow.lines.size()));
        vertical.add(makeText("Words   :" + reflow.words.size()));

        JPanel vertical2 = new JPanel();
        vertical2.setAlignmentY(Component.TOP_ALIGNMENT);
        vertical2.setBorder(new EmptyBorder(10, 10, 10, 10));
        vertical2.setLayout(new BoxLayout(vertical2, BoxLayout.PAGE_AXIS));

        vertical2.add(makeLabel("Native Reflow"));
        if (reflow.isDrawResult) {
            try {
                Image reflowed = reflow.getReflowed(width, heigth);
                if (reflowed != null) {
                    vertical2.add(new JLabel(new ImageIcon(reflowed)));
                }
            } catch (Exception e) {
                vertical2.add(makeLabel("Error : " + e.getMessage()));
                e.printStackTrace();

            }
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
                File my = new File("/home/data/IdeaProjects/SmartReflow/src1");
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
                    } catch (IOException ex) {
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
