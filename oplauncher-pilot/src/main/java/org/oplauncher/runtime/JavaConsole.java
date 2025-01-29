package org.oplauncher.runtime;

public interface JavaConsole {

    public JavaConsole initializeUI();
    public JavaConsole display(int posx, int posy);
    public JavaConsole printHelloString();
    public JavaConsole redirectOutput();
    public JavaConsole displayCenter();
    public boolean isConsoleVisible();
    public AppletController getAppletController();
}
