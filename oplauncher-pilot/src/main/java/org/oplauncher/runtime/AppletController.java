package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.*;
import org.oplauncher.load.SplashScreen;
import org.oplauncher.op.OPServer;
import org.oplauncher.op.OPServerFactory;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.oplauncher.ConfigurationHelper.loadReloadIconBytes;
import static org.oplauncher.IConstants.*;

public abstract class AppletController {
    static private final Lock LOCK = new ReentrantLock();
    static private final Logger LOGGER = LogManager.getLogger(AppletController.class);

    protected AppletController(AppletClassLoader clzloader, AppletContext context) throws OPLauncherException {
        _classLoader = clzloader;
        _context = context;

        AppletOPDispatcher opdisp = new AppletOPDispatcher(this);
        /// Configure the OP server with all its observers
        _opServer = OPServerFactory.newServer(opdisp::processSuccessRequest, opdisp::processFailureRequest);

        OPLauncherConfig.instance.registerController(this);

        deactivateApplet();
    }

    public <T>String executeOP(OpCode opcode, T... parameters) throws OPLauncherException {
        /// Visitor pattern for the respective operation code
        switch (opcode) {
            /**
             * Load the Applet
             */
            case LOAD_APPLET: {
                // Triggers the Java console if enabled
                if (ConfigurationHelper.isJavaConsoleActive()) {
                    triggerJavaConsole();
                }

                return loadAppletClass();
            }
            /**
             * Unloads the Applet. This OP destroys all the Applets
             */
            case UNLOAD_APPLET: return getAppletClassLoader().disposeApplets().successResponse();
            /**
             * Change the Applet frame position
             */
            case CHANGE_POSTION: return changeAppletPosition();
            /**
             * Asks for Applet Focus
             */
            case FOCUS_APPLET: return focusApplet((String[]) parameters);
            /**
             * Removes focus from Applet
             */
            case BLUR_APPLET: return blurApplet((String[]) parameters);
            default: {
                throw new OPLauncherException(String.format("Unsupported operational code: [%s]", opcode.name()));
            }
        }
    }

    public AppletController disposeAllApplets() {
        if ( getAppletFrame().isDisplayable() ) {
            getAppletFrame().dispose();
        }

         return this;
    }

    private boolean isSWTConsole() {
        return ConfigurationHelper.getConsoleType() == JavaConsoleBuilder.ConsoleType.SWT;
    }

    protected AppletController triggerJavaConsole() {
        LOCK.lock();
        try {
            if ( getJavaConsole() == null ) {
                _javaConsole = JavaConsoleBuilder.newConsole(this).load().show();
            }
            else if ( !getJavaConsole().isConsoleVisible() ) {
                getJavaConsole().display(DEFAULT_INIT_POSX, DEFAULT_INIT_POSY);
            }

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    protected AppletController configureApplet(Applet applet) throws OPLauncherException {
        applet.setName(getAppletClassLoader().getAppletName());
        //applet.setLayout(new BorderLayout());
        // TODO: Add additional configuration to the panel
        return this;
    }

    protected String renderApplet(Applet applet) throws Exception {
        LOCK.lock();
        try {
            _runningApplet = applet;

            // Applet frame icon
            ImageIcon frameIcon = ConfigurationHelper.getFrameIcon();
            getAppletFrame().setIconImage(frameIcon.getImage());

            getAppletFrame().setLayout(new BorderLayout(5, 5));
            getAppletFrame().setSize(getAppletClassLoader().getAppletParameters().getWidth(applet.getName()),
                                     getAppletClassLoader().getAppletParameters().getHeight(applet.getName()));
            getAppletFrame().setBackground(Color.white);
            getAppletFrame().setAlwaysOnTop(ConfigurationHelper.isFrameAlwaysOnTop());
            getAppletFrame().setResizable(ConfigurationHelper.isFrameResizable());

            if ( !ConfigurationHelper.isWindowCloseActive() || ConfigurationHelper.trackBrowserWindowPosition() ) {
                getAppletFrame().setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                getAppletFrame().setUndecorated(true);
            }
            else {
                getAppletFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }

            ///  Only add the status bar if enabled
            if ( ConfigurationHelper.isStatusBarActive() && !ConfigurationHelper.trackBrowserWindowPosition() ) {
                getAppletStatusBar().setBorder(BorderFactory.createEtchedBorder());
                //getAppletStatusBar().setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                getAppletStatusBar().add(getAppletStatusBarLabel(), BorderLayout.WEST);
                getAppletStatusBar().add(getButtonPanel(), BorderLayout.EAST);
                // Add the status bar to the bottom
                getAppletFrame().add(getAppletStatusBar(), BorderLayout.SOUTH);
            }

            getAppletFrame().add(applet, BorderLayout.CENTER);

            getAppletClassLoader().getAppletController().activateApplet(); /// Mark the applet as active
            LOGGER.info("Calling applet STARTED");
            applet.start();

            // SHOW THE APPLET !!!
            applet.validate();
            //applet.repaint();

            // Center the frame on the screen and pack
            setAppletPosition().getAppletFrame().pack();
            // Add events and shows the applet
            addEvents(applet).getAppletFrame().setVisible(true);
            getAppletFrame().toFront();

            // Dispose the splash
            SplashScreen.instance.closeSplash();

            // TODO: Workaround for the toggle mechanism
            if ( isSWTConsole() ) {
                // Disable the java console button if
                getJavaConsoleButton().setEnabled(!ConfigurationHelper.isJavaConsoleActive());
            }

            // Now it's time to start the server (if not running already)
            if (!getOPServer().isOPServerRunning()) {
                getOPServer().startOPServer();
            }

            LOGGER.info("Applet successfully loaded !");

            return "";
        }
        finally {
            LOCK.unlock();
        }
    }
    protected String parseClassName(final String klass) {
        return klass.replace("/", ".")
                .replace("\\", ".")
                .replace(".class", "");
    }

    public AppletController setAppletPosition() {
        String appletName = getAppletClassLoader().getAppletName();
        if ( getAppletClassLoader().getAppletParameters().getPositionX(appletName) >= 0 ||
                getAppletClassLoader().getAppletParameters().getPositionY(appletName) >= 0 ) {
            getAppletFrame().setLocation(getAppletClassLoader().getAppletParameters().getPositionX(appletName),
                                         getAppletClassLoader().getAppletParameters().getPositionY(appletName));
        }
        // Simply center the applet!
        else {
            getAppletFrame().setLocationRelativeTo(null);
        }

        return this;
    }

    private AppletController addEvents(final Applet applet) {
        // Handle window closing
        getAppletFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Cleanly stop applet
                LOGGER.warn("Stopping the applet: {}", applet.getName());
                applet.stop();
                LOGGER.warn("Destroying the applet: {}", applet.getName());
                applet.destroy();

                getAppletFrame().dispose();

                // closes the server
                if (getOPServer().isOPServerRunning()) {
                    getOPServer().stopOPServer();
                }

                // Exit application
                System.exit(EXIT_SUCCESS);
            }
        });

        return this;
    }

    protected AppletController move(int x, int y) {
        LOCK.lock();
        try {
            if ( getApplet()!=null ) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("About to move the applet ({}) to the position X:{} to Y:{}", getApplet().getName(), x, y);
                }

                getAppletClassLoader().getAppletParameters().setPosition(getAppletClassLoader().getAppletName(), x, y);

                // Reset position if it moves off-screen
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                if (x > screenSize.width - getAppletFrame().getWidth()) x = 0;   // Reset to left
                if (y > screenSize.height - getAppletFrame().getHeight()) y = 0; // Reset to top

                getAppletFrame().setLocation(x, y);
                getAppletFrame().repaint();
            }
            else {
                LOGGER.warn("No running Applet. Cannot move applet at {} to {}", x, y);
            }

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    abstract protected String loadAppletClass() throws OPLauncherException;
    abstract protected String changeAppletPosition() throws OPLauncherException;
    abstract protected String focusApplet(String ...parameters) throws OPLauncherException;
    abstract protected String blurApplet(String ...parameters) throws OPLauncherException;

    protected AppletClassLoader getAppletClassLoader() {
        return _classLoader;
    }

    protected AppletContext getAppletContext() {
        return _context;
    }

    public AppletController activateApplet() {
        LOCK.lock();
        try {
            _appletActive = true;

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }
    public AppletController deactivateApplet() {
        LOCK.lock();
        try {
            _appletActive = false;

            return this;
        }
        finally {
            LOCK.unlock();
        }
    }

    public boolean isAppletActive() {
        LOCK.lock();
        try {
            return _appletActive;
        }
        finally {
            LOCK.unlock();
        }
    }

    public OPServer getOPServer() {
        return _opServer;
    }

    protected AppletController defineAppletFrame(String title) {
        _appletFrame = new JFrame(title);
        return this;
    }
    protected AppletController defineStatusBar(String text, Applet applet) {
        _appletStatusBarLabel = new JLabel(text);
        _statusBarPanel = new JPanel(new BorderLayout());
        _buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        // Image processing
        ImageIcon originalRefreshIcon = new ImageIcon(loadReloadIconBytes(CONFIG_ICONRES_REFRESH));
        ImageIcon originalInfoIcon    = new ImageIcon(loadReloadIconBytes(CONFIG_ICONRES_INFO));
        ImageIcon originalJavaConsoleIcon = ConfigurationHelper.getFrameIcon();
        Image iconRefresh = originalRefreshIcon.getImage().getScaledInstance(APPLET_ICON_SIZE_16, APPLET_ICON_SIZE_16, Image.SCALE_SMOOTH);
        Image iconInfo    = originalInfoIcon.getImage().getScaledInstance(APPLET_ICON_SIZE_16, APPLET_ICON_SIZE_16, Image.SCALE_SMOOTH);
        Image javaConsole = originalJavaConsoleIcon.getImage().getScaledInstance(APPLET_ICON_SIZE_16, APPLET_ICON_SIZE_16, Image.SCALE_SMOOTH);

        _appletReloadButton = new JButton(new ImageIcon(iconRefresh));
        _appletReloadButton.setToolTipText("Reload Applet Code");
        _appletReloadButton.addActionListener(evt -> {
            LOGGER.info("Reloading the applet... {}", applet.getName());
            try {
                ///  RELOADS THE APPLET !!!
                applet.stop();
                applet.destroy();
                applet.init();
                applet.start();

                LOGGER.info("({}) Applet reloaded successfully!", applet.getName());
            }
            catch (Exception e) {
                LOGGER.error("Error while reloading applet", e);
                JOptionPane.showMessageDialog(getAppletFrame(), "Failed to reload the applet!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        _appletInfoButton = new JButton(new ImageIcon(iconInfo));
        _appletInfoButton.setToolTipText("OJDK Launcher Information");
        _appletInfoButton.addActionListener(evt -> {
            LOGGER.info("Displaying application info.");
            JOptionPane.showMessageDialog(getAppletFrame(),
                    _getInfoText(),
                    "About OPLauncher",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        _javaConsoleButton = new JButton(new ImageIcon(javaConsole));
        _javaConsoleButton.setToolTipText("Open Java Console");
        _javaConsoleButton.addActionListener(evt -> {
            LOGGER.info("Openning the Java Console if not already opened for: {}", applet.getName());
            triggerJavaConsole();

            // TODO: Workaround for the toggling problem with the SWT thread. Change in the future
            if (isSWTConsole()) {
                getJavaConsoleButton().setEnabled(false);
            }
        });

        getButtonPanel().add(getJavaConsoleButton());
        getButtonPanel().add(getAppletInfoButton());
        getButtonPanel().add(getAppletReloadButton());

        return this;
    }
    private String _getInfoText() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPLauncher ").append(ConfigurationHelper.getOPLauncherVersion()).append(DEFAULT_EOL).append(DEFAULT_EOL)
                .append("Developed by Jo^o Gonzalez at Azul Systems").append(REGISTERED_CHAR).append(DEFAULT_EOL)
                .append("This application allows applets to be launched from modern browsers and up-to-dated Java Runtimes.")
                    .append(DEFAULT_EOL)
                    .append(DEFAULT_EOL)
                .append("Applet running configuration:").append(DEFAULT_EOL)
                .append("- Java Runtime: ").append(System.getProperty("java.vm.name")).append(DEFAULT_EOL)
                //.append("- Java Vendor Version: ").append(System.getProperty("java.vendor.version")).append(DEFAULT_EOL)
                .append("- Java Runtime Version: ").append(System.getProperty("java.runtime.version")).append(DEFAULT_EOL)
                .append("- Java Vendor: ").append(System.getProperty("java.vendor")).append(DEFAULT_EOL);

        return sb.toString();
    }

    public JFrame getAppletFrame() {
        return _appletFrame;
    }
    public JLabel getAppletStatusBarLabel() {
        return _appletStatusBarLabel;
    }
    public JPanel getAppletStatusBar() {
        return _statusBarPanel;
    }
    public JButton getAppletReloadButton() {
        return _appletReloadButton;
    }
    public JButton getAppletInfoButton() {
        return _appletInfoButton;
    }
    public JButton getJavaConsoleButton() {
        return _javaConsoleButton;
    }
    public JPanel getButtonPanel() {
        return _buttonPanel;
    }
    public JavaConsole getJavaConsole() {
        return _javaConsole;
    }

    protected Applet getApplet() {
        return _runningApplet;
    }

    // class properties
    private AppletClassLoader _classLoader;
    private AppletContext _context;
    private Applet _runningApplet;

    private boolean _appletActive;

    private JavaConsole _javaConsole;
    private JFrame _appletFrame;
    private JLabel _appletStatusBarLabel;
    private JPanel _statusBarPanel;
    private JPanel _buttonPanel;
    private JButton _appletReloadButton;
    private JButton _appletInfoButton;
    private JButton _javaConsoleButton;

    private OPServer _opServer;
}
