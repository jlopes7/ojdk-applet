package org.oplauncher.runtime;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oplauncher.ConfigurationHelper;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Date;

import static org.oplauncher.IConstants.*;

public class SwingJavaConsole extends JFrame implements JavaConsole {
    static private final Logger LOGGER = LogManager.getLogger(SwingJavaConsole.class);

    protected SwingJavaConsole(AppletController ctrl) {
        super("OPLauncher Java Console");

        _buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        _outputText = new JTextPane();
        _outputText.setEditable(false);
        _outputText.setFont(new Font(CONFIG_JAVACONSOLE_FONTOPT1, Font.PLAIN, 12));
        _styleDoc = _outputText.getStyledDocument();
        _styleOut = _outputText.addStyle("SystemOut", null);
        _styleErr = _outputText.addStyle("SystemErr", null);
        StyleConstants.setForeground(_styleErr, Color.RED);  // Red for System.err
        StyleConstants.setBold(_styleErr, true);

        // Process initialization
        initializeUI().redirectOutput().printHelloString();
    }

    @Override
    public JavaConsole printHelloString() {
        System.out.println(ConfigurationHelper.loadJavaConsoleText());
        System.out.println(new Date(System.currentTimeMillis()));
        System.out.println();
        return this;
    }

    @Override
    public SwingJavaConsole initializeUI() {
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(5, 5));

        JScrollPane scrollPane = new JScrollPane(getOutputTextPane());
        add(scrollPane, BorderLayout.CENTER);

        // Create buttons with icons
        _clearButton = createButton("Clear", CONFIG_ICONRES_CLEAR);
        _saveButton = createButton("Save", CONFIG_ICONRES_SAVE);
        _infoButton = createButton("Info", CONFIG_ICONRES_INFO2);

        registerEvents().getButtonPanel().add(getClearButton());
        getButtonPanel().add(getSaveButton());
        getButtonPanel().add(getInfoButton());
        add(getButtonPanel(), BorderLayout.SOUTH);

        // Process frame icon
        ImageIcon frameIcon = new ImageIcon(getClass().getResource(CONFIG_ICONRES_JAVACONSOLE));
        setIconImage(frameIcon.getImage());

        return this;
    }

    @Override
    public SwingJavaConsole display(int posx, int posy) {
        // Position the Java Console to always near the top-right corner
        setLocation(posx, posy);
        setVisible(true);

        return this;
    }

    protected SwingJavaConsole registerEvents() {
        getClearButton().addActionListener(e -> clearConsole());
        getSaveButton().addActionListener(e -> saveConsole());
        return this;
    }

    private void clearConsole() {
        getOutputTextPane().setText(""); // Clear all content
        printHelloString();
    }
    private void saveConsole() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Console Output");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            LOGGER.info("Saving console output to: {}", fileChooser.getSelectedFile().getAbsolutePath());
            try {
                FileUtils.copyToFile(new ByteArrayInputStream(getOutputTextPane().getText().getBytes(Charset.defaultCharset())),
                                     fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "File saved successfully!");

                LOGGER.info("File ({}) was saved successfully!", fileChooser.getSelectedFile().getAbsolutePath());
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createButton(String text, String iconPath) {
        JButton button = new JButton();
        button.setToolTipText(text);

        // Load and scale the icon
        ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));
        Image scaledIcon = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaledIcon));
        button.setText(text);

        // Position the text and icon
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setIconTextGap(10 /*10 pixels between icons and text*/);
        button.setMargin(new Insets(5, 10, 5, 10)); // Add padding (top, left, bottom, right)

        return button;
    }

    @Override
    public JavaConsole redirectOutput() {
        // Redirect System.out
        PrintStream outStream = new PrintStream(new CustomOutputStream(getStyleOut()));
        System.setOut(outStream);

        // Redirect System.err
        PrintStream errStream = new PrintStream(new CustomOutputStream(getStyleErr()));
        System.setErr(errStream);

        return this;
    }

    @Override
    public JavaConsole displayCenter() {
        setLocationRelativeTo(null); // Center the screen
        repaint();
        setVisible(true);
        return this;
    }

    @Override
    public boolean isConsoleVisible() {
        return isVisible();
    }

    private class CustomOutputStream extends OutputStream {
        private final Style style;

        public CustomOutputStream(Style style) {
            this.style = style;
        }

        @Override
        public void write(int b) {
            appendText(String.valueOf((char) b));
        }

        @Override
        public void write(byte[] b, int off, int len) {
            appendText(new String(b, off, len));
        }

        private void appendText(String text) {
            try {
                getStyledDocument().insertString(getStyledDocument().getLength(), text, style); // Append text with style
                getOutputTextPane().setCaretPosition(getStyledDocument().getLength());          // Auto-scroll to the bottom
            }
            catch (BadLocationException e) {
                LOGGER.error(e);
            }
        }
    }

    @Override
    public AppletController getAppletController() {
        return _appletController;
    }

    protected JTextPane getOutputTextPane() {
        return _outputText;
    }
    protected Style getStyleOut() {
        return _styleOut;
    }
    protected Style getStyleErr() {
        return _styleErr;
    }
    protected StyledDocument getStyledDocument() {
        return _styleDoc;
    }
    protected JPanel getButtonPanel() {
        return _buttonPanel;
    }

    protected JButton getClearButton() {
        return _clearButton;
    }
    protected JButton getSaveButton() {
        return _saveButton;
    }
    protected JButton getInfoButton() {
        return _infoButton;
    }

    // class properties
    private JTextPane _outputText;
    private JPanel _buttonPanel;
    private StyledDocument _styleDoc;
    private Style _styleOut;
    private Style _styleErr;

    private JButton _clearButton;
    private JButton _saveButton;
    private JButton _infoButton;

    private AppletController _appletController;
}
