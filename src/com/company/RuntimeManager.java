package com.company;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class RuntimeManager {

    private static final int MODE_STDOUT = 0;
    private static final int MODE_FILE = 1;
    private final int[] touchPoint = {-1, -1};
    private int mode = MODE_STDOUT;
    private PrintWriter pr = null;
    private String fileName = "output.txt";
    private AtomicBoolean mousePressed = new AtomicBoolean(false);
    private AtomicBoolean terminationFlag = new AtomicBoolean(false);
    private JFrame frame = null;
    private JPanel jPanel;
    private JFrame closeDialog;

    void start(String args[]) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (mode == MODE_FILE && pr != null)
                pr.close();
            System.out.println("closing");
        }));

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (i == args.length - 1 || args[i + 1].startsWith("-")) {
                    String usage = "USAGE";
                    System.out.println(usage);
                    return;
                }

                String command = args[i].replace("-", "");

                if (command.equals("o")) {
                    mode = MODE_FILE;
                    fileName = args[i + 1];
                }

            }
        }

        int[] coords = new int[4];
        coords[0] = -1;
        coords[1] = -1;
        coords[2] = -1;
        coords[3] = -1;

        BufferedImage img;
        try {
            Thread.sleep(2000);
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            Robot robot = new Robot();
            img = robot.createScreenCapture(new Rectangle(size));
            frame = new JFrame();
            frame.getContentPane().setLayout(new FlowLayout());
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);
            frame.setSize(size);
            jPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    g.drawImage(img, 0, 0, size.width,size.height, this);
                    if (mousePressed.get())
                    {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setColor(new Color(255,143,126));
                        g2.setStroke(new BasicStroke(5));
                        g2.drawRect(touchPoint[0],touchPoint[1],MouseInfo.getPointerInfo().getLocation().x-touchPoint[0],MouseInfo.getPointerInfo().getLocation().y-touchPoint[1]);

                    }
                }
            };

            jPanel.setMinimumSize(size);
            jPanel.setMaximumSize(size);
            jPanel.setPreferredSize(size);
            frame.add(jPanel);

            GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = graphics.getDefaultScreenDevice();
            device.setFullScreenWindow(frame);
            frame.getContentPane().addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    coords[0] = e.getX();
                    coords[1] = e.getY();
                    touchPoint[0] = e.getX();
                    touchPoint[1] = e.getY();
                    mousePressed.set(true);

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    coords[2] = e.getX();
                    coords[3] = e.getY();
                    mousePressed.set(false);
                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });
            frame.getContentPane().addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    super.mouseDragged(e);
                    frame.repaint();
                    jPanel.repaint();
                    }
            });
            // frame.getContentPane().add(new JLabel(new ImageIcon(img)));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {

	} // end try

        if (mode == MODE_FILE) {
            File outputFile = new File(fileName);

            try {
                pr = new PrintWriter(outputFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
        while (coords[0] == -1 || coords[1] == -1 || coords[2] == -1 || coords[3] == -1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (frame != null)
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));


        closeDialog = new JFrame();
        closeDialog.getContentPane().setLayout(new FlowLayout());
        closeDialog.setSize(new Dimension(700,200));
        JButton btnTerminate = new JButton();
        btnTerminate.setText("TERMINATE");
        btnTerminate.addActionListener(new Action() {
            @Override
            public Object getValue(String key) {
                return null;
            }

            @Override
            public void putValue(String key, Object value) {

            }

            @Override
            public void setEnabled(boolean b) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {

            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                terminationFlag.set(true);
                closeDialog.dispatchEvent(new WindowEvent(closeDialog, WindowEvent.WINDOW_CLOSING));

            }
        });
        closeDialog.add(btnTerminate);
        closeDialog.pack();
        closeDialog.setVisible(true);

        while (!terminationFlag.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Robot robot = new Robot();
                BufferedImage section = robot.createScreenCapture(new Rectangle(coords[0], coords[1], coords[2] - coords[0], coords[3] - coords[1]));
                File sectionFile = new File("section.png");
                try {
                    ImageIO.write(section, "png", sectionFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (AWTException e) {
                e.printStackTrace();
            }

            try {
                Runtime.getRuntime().exec("rm .TESSERACT_OUTPUT.txt");
                Runtime.getRuntime().exec("tesseract section.png output");
            } catch (IOException e1) {
                e1.printStackTrace();
            }


            File file = new File("output.txt");
            String result = "";
            Scanner sc = null;
            while (sc == null) {
                try {
                    if (! file.exists())break;

                    sc = new Scanner(file);

                    while (sc.hasNext()) {
                        result = result + sc.nextLine() + "\n";
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            switch (mode) {
                case MODE_STDOUT:
                    System.out.println(result);
                    break;
                case MODE_FILE:
                    if (pr != null) {
                        pr.write(result);
                    }
            }

        }
	// No matter what this will force an exit status code of non 0. Why?
	System.exit(1);
    }

}





