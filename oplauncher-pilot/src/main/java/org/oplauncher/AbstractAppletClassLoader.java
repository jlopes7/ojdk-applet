package org.oplauncher;

import org.oplauncher.res.FileResource;

import java.util.List;

public abstract class AbstractAppletClassLoader<T> extends ClassLoader implements IAppletClassLoader<T> {

    protected AbstractAppletClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public String loadApplet(List<T> parameters) {
        try {
            String opcode = OpHelper.getOpCode(parameters);

            ///  switch the process based on the opcode given as parameter
            switch (OpCode.parse(opcode)) {
                ///  Load all operations as needed
                case LOAD_APPLET: return loadApplet(parameters);
                default:
                    throw new OPLauncherException(String.format("Unknown or unsupported opcode: [%s]", opcode), ErrorCode.UNSUPPORTED_OPCODE);
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);

            return e.getMessage();
        }
    }

    protected List<FileResource> loadAppletFromURL(List<T> parameters) {

    }

    public abstract String processLoadAppletOp(List<T> parameters) throws OPLauncherException;
}
