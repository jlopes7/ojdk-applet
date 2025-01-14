package org.oplauncher;

public enum ArchiveClassLoaderType {
    SIMPLE, CUSTOM, URL
      ;

    static public ArchiveClassLoaderType parse(String name) {
        if ( name == null ) return SIMPLE;

        for (ArchiveClassLoaderType type : values()) {
            if (type.name().equalsIgnoreCase(name.trim())) return type;
        }

        return SIMPLE;
    }
}
