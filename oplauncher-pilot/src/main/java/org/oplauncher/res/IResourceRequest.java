package org.oplauncher.res;

import org.oplauncher.OPLauncherException;

import java.net.URL;
import java.util.Map;

public interface IResourceRequest<T> {
    public <K,V>T getResource(URL url, Map<K,V> params) throws OPLauncherException;
}
