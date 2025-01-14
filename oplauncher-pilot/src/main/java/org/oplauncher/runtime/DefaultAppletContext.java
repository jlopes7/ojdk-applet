package org.oplauncher.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * Very simple implementation of an Applet context class
 */
public class DefaultAppletContext implements AppletContext {
    private static final Logger LOGGER = LogManager.getLogger(DefaultAppletContext.class);

    protected DefaultAppletContext(AppletController controller) {
        _streamMap = Collections.synchronizedMap(new LinkedHashMap<>());
        _controller = controller;
    }

    @Override
    public AudioClip getAudioClip(URL url) {
        return Applet.newAudioClip(url);
    }

    @Override
    public Image getImage(URL url) {
        // TODO: Use a default toolkit or custom method to load the image
        return Toolkit.getDefaultToolkit().createImage(url);
    }

    @Override
    public Applet getApplet(String name) {
        return AppletRegistry.get(name);
    }

    @Override
    public Enumeration<Applet> getApplets() {
        return AppletRegistry.getApplets();
    }

    @Override
    public void showDocument(URL url) {
        LOGGER.info("Navigating to [{}]", url);
    }

    @Override
    public void showDocument(URL url, String target) {
        LOGGER.info("Navigating to [{}], with target [{}]", url, target);
    }

    @Override
    public void showStatus(String status) {
        LOGGER.info("Status given: [{}]", status);
        SwingUtilities.invokeLater(() -> _controller.getAppletStatusBar().setText(status));
    }

    @Override
    public void setStream(String key, InputStream stream) throws IOException {
        InputStream existingStream = _streamMap.get(key);

        // Close any existing stream with the same key
        if (existingStream != null) {
            existingStream.close();
        }

        _streamMap.put(key, stream);
    }

    @Override
    public InputStream getStream(String key) {
        return _streamMap.get(key);
    }

    @Override
    public Iterator<String> getStreamKeys() {
        return _streamMap.keySet().iterator();
    }

    // class properties
    private Map<String, InputStream> _streamMap;
    private AppletController _controller;
}
