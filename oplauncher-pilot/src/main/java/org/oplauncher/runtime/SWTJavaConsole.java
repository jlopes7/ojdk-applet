package org.oplauncher.runtime;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.StyledText;
import org.oplauncher.ConfigurationHelper;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.oplauncher.IConstants.*;

public class SWTJavaConsole implements JavaConsole {
    static private final Logger LOGGER = LogManager.getLogger(SWTJavaConsole.class);

    protected SWTJavaConsole(AppletController ctrl) {
        _display = new Display();
        _shell = new Shell(getDisplay());
        _controller = ctrl;
        getShell().setText("OPLauncher Java Console");
        getShell().setSize(800, 600);
        getShell().setLayout(new GridLayout(1, false));

        initializeUI().redirectOutput().printHelloString();
    }

    @Override
    public JavaConsole initializeUI() {
        // Text area for console output
        _outputText = new StyledText(getShell(), SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        getStyledText().setEditable(false);
        getStyledText().setFont(new Font(getDisplay(), new FontData(CONFIG_JAVACONSOLE_FONTOPT2, 10, SWT.NORMAL)));

        GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
        getStyledText().setLayoutData(textData);

        // Button panel
        _buttonPanel = new Composite(getShell(), SWT.NONE);
        RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.spacing = 10;
        getButtonPanel().setLayout(rowLayout);
        getButtonPanel().setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

        // Create buttons with icons
        _clearButton = createButton("Clear", CONFIG_ICONRES_CLEAR);
        _saveButton = createButton("Save",   CONFIG_ICONRES_SAVE);
        _infoButton = createButton("Info",   CONFIG_ICONRES_INFO2);

        registerEvents();

        // Set frame icon
        getShell().setImage(new Image(getDisplay(), getClass().getResourceAsStream(CONFIG_ICONRES_JAVACONSOLE)));

        getShell().open();
        getShell().setVisible(true);
        getShell().setActive();

        return this;
    }

    @Override
    public JavaConsole display(int posx, int posy) {
        // Move the Shell to the given coordinates
        getShell().setLocation(posx, posy);

        if (!isConsoleVisible()) {
            // Open the Shell (make it visible)
            getShell().open();
            getShell().setVisible(true);
            getShell().setActive();
        }

        return this;
    }

    @Override
    public JavaConsole displayCenter() {
        Rectangle screenSize = getDisplay().getPrimaryMonitor().getBounds();
        Rectangle shellSize = getShell().getBounds();

        int x = (screenSize.width - shellSize.width) / 2;
        int y = (screenSize.height - shellSize.height) / 2;

        getShell().setLocation(x, y);
        getShell().open();

        return this;
    }

    @Override
    public boolean isConsoleVisible() {
        return getShell().isVisible();
    }

    private Button createButton(String text, String iconPath) {
        Button button = new Button(getButtonPanel(), SWT.PUSH);
        button.setText(text);
        button.setToolTipText(text);

        // Load icon
        Image icon = new Image(getDisplay(), getClass().getResourceAsStream(iconPath));
        button.setImage(icon);

        return button;
    }

    private void registerEvents() {
        getShell().addListener(SWT.Dispose, event -> closeDisplay());
        getClearButton().addListener(SWT.Selection, event -> clearConsole());
        getSaveButton().addListener(SWT.Selection, event -> saveConsole());
    }

    @Override
    public JavaConsole printHelloString() {
        System.out.println(ConfigurationHelper.loadJavaConsoleText());
        System.out.println(new Date(System.currentTimeMillis()));
        System.out.println();

        return this;
    }

    private void closeDisplay() {
        SWTJavaConsoleMonitor.resetInited();
        getAppletController().getJavaConsoleButton().setEnabled(true);

    }
    private void clearConsole() {
        getStyledText().setText("");
        printHelloString();
    }
    private void saveConsole() {
        FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
        fileDialog.setText("Save Console Output");
        fileDialog.setFilterExtensions(new String[]{"*.txt", "*.log", "*.*"});
        fileDialog.setFilterNames(new String[]{"Text Files (*.txt)", "Log Files (*.log)", "All Files (*.*)"});
        String filePath = fileDialog.open();

        if (filePath != null) {
            LOGGER.info("Saving console output to: {}", filePath);
            try {
                FileUtils.copyToFile(new ByteArrayInputStream(getStyledText().getText().getBytes(StandardCharsets.UTF_8)), new java.io.File(filePath));
                MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                messageBox.setMessage("File saved successfully!");
                messageBox.open();
            }
            catch (Exception e) {
                LOGGER.error(e);
                MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                messageBox.setMessage("Error saving file: " + e.getMessage());
                messageBox.open();
            }
        }
    }

    @Override
    public JavaConsole redirectOutput() {
        // Redirect System.out
        PrintStream outStream = new PrintStream(new CustomOutputStream(getDisplay(), getStyledText(), SWT.COLOR_BLACK));
        System.setOut(outStream);

        // Redirect System.err
        PrintStream errStream = new PrintStream(new CustomOutputStream(getDisplay(), getStyledText(), SWT.COLOR_RED));
        System.setErr(errStream);

        return this;
    }

    private static class CustomOutputStream extends OutputStream {
        public CustomOutputStream(Display display, StyledText styledText, int color) {
            this.display = display;
            this.styledText = styledText;
            this.color = color;
        }

        @Override
        public void write(int b) {
            asyncAppend(String.valueOf((char) b));
        }

        @Override
        public void write(byte[] b, int off, int len) {
            asyncAppend(new String(b, off, len));
        }

        private void asyncAppend(String text) {
            display.asyncExec(() -> {
                styledText.append(text);
                styledText.setForeground(display.getSystemColor(color));
                styledText.setSelection(styledText.getText().length());
            });
        }

        // class properties
        private final Display display;
        private final StyledText styledText;
        private final int color;
    }

    protected Shell getShell() {
        return _shell;
    }
    protected Display getDisplay() {
        return _display;
    }
    protected StyledText getStyledText() {
        return _outputText;
    }
    protected Composite getButtonPanel() {
        return _buttonPanel;
    }
    protected Button getClearButton() {
        return _clearButton;
    }
    protected Button getSaveButton() {
        return _saveButton;
    }
    protected Button getInfoButton() {
        return _infoButton;
    }

    @Override
    public AppletController getAppletController() {
        return _controller;
    }

    // class properties
    private Shell _shell;
    private Display _display;
    private StyledText _outputText;
    private Composite _buttonPanel;
    private Button _clearButton;
    private Button _saveButton;
    private Button _infoButton;

    private AppletController _controller;
}
