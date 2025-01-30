package org.oplauncher.load;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.awt.Font.BOLD;

public class SplashScreen extends JWindow {
    static public final SplashScreen instance = new SplashScreen();

    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(SplashScreen.class);

    static public final int WSIZE = 661;
    static public final int HSIZE = 371;
    static private final int NUM_DOTS = 6;

    private SplashScreen() {
        setSize(WSIZE, HSIZE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Image splash = ConfigurationHelper.getSplashIcon()
                                            .getImage()
                                            .getScaledInstance(WSIZE, HSIZE, Image.SCALE_SMOOTH);
        _imageLabel = new JLabel(new ImageIcon(splash));
        _statusLabel = new JLabel("Loading.", SwingConstants.LEFT);
        _executorService = Executors.newSingleThreadScheduledExecutor();

        additionUIConfig();

        // add the two components
        add(_imageLabel, BorderLayout.CENTER);
        add(_statusLabel, BorderLayout.SOUTH);

        setAlwaysOnTop(true);
    }
    private void additionUIConfig() {
        // Set the window shape (Rounded Corners)
        setShape(new RoundRectangle2D.Double(0, 0, WSIZE, HSIZE, 10, 10));

        // Make the background transparent
        setBackground(new Color(0, 0, 0, 0));

        getStatusLabel().setFont(new Font("Monospaced", BOLD, 15));
        getStatusLabel().setForeground(Color.YELLOW);
    }

    public SplashScreen showSplash() {
        LOCK.lock();
        try {
            LOGGER.info("Loading the splash screen...");
            SwingUtilities.invokeLater(() -> {
                setVisible(true);
                LOGGER.info("Splash screen loaded!");
            });

            // starts the animation
            startLoadingAnimation();

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }
    public SplashScreen closeSplash() {
        LOCK.lock();
        try {
            LOGGER.info("Closing the splash screen...");
            // Stop the scheduled updates
            getExecutorService().shutdown();

            SwingUtilities.invokeLater(() -> {
                setVisible(false);
                dispose();

                LOGGER.info("Splash screen closed!");
            });

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    private String _dots_() {
        String dots = "";
        for (int i = 0; i < _dotCount; i++) {dots += ".";}
        return dots;
    }
    protected void startLoadingAnimation() {
        getExecutorService().scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> {
                String loadingText = "Loading".concat(_dots_());
                getStatusLabel().setText(loadingText);
                _dotCount = (_dotCount % NUM_DOTS) + 1;  // Cycle through 1, 2, 3...,NUM_DOTS dots
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    protected JLabel getImageLabel() {
        return _imageLabel;
    }
    protected JLabel getStatusLabel() {
        return _statusLabel;
    }
    protected ScheduledExecutorService getExecutorService() {
        return _executorService;
    }

    // class properties
    private JLabel _imageLabel;
    private JLabel _statusLabel;

    private ScheduledExecutorService _executorService;
    private int _dotCount = 1;
}
