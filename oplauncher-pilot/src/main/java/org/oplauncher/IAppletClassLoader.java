package org.oplauncher;

import java.util.List;

public interface IAppletClassLoader<T> {

    public String loadApplet(List<T> parameters);
}
