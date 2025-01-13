package org.oplauncher;

import org.apache.commons.io.FilenameUtils;

import java.util.*;

import static java.util.regex.Pattern.quote;
import static org.oplauncher.IConstants.*;

public class CommunicationParameterParser {
    private static final int IDX_OPCODE  = 0x00;
    private static final int IDX_BASEURL = 0x01;
    private static final int IDX_APPLTTAG = 0x02;
    private static final int IDX_APPLTJARS = 0x03;
    private static final int IDX_APPLTPARAMS = 0x04;
    private static final int IDX_RESURL  = 0x05;

    public enum AppletTagDef {
        CODEBASE, ARCHIVES, UNKNOWN
          ;

        static public AppletTagDef getAppletTagDef(String tag) {
            if (tag == null) return UNKNOWN;

            for (AppletTagDef def : AppletTagDef.values()) {
                if (def.name().equalsIgnoreCase(tag.trim())) return def;
            }

            return UNKNOWN;
        }
    }

    static private <T>String paramValue(List<T> params, int idx) {
        if ( params!=null && params.size() > idx ) {
            return (String) params.get(idx);
        }

        return null;
    }

    static protected <T>String resolveOpCode(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_OPCODE)) != null ) {
            return val;
        }

        throw new RuntimeException(String.format("No opcode found for params: %s", params));
    }

    static protected <T>String resolveBaseUrl(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_BASEURL)) != null ) {
            String opcode = resolveOpCode(params);
            if ( val != null && OpCode.parse(opcode) == OpCode.LOAD_APPLET ) {
                return val;
            }
            else if (val != null) {
                throw new RuntimeException(String.format("Incorrect operation provided: %s. Expected op: load_applet", opcode));
            }
        }

        throw new RuntimeException(String.format("No Base URL found for params: %s", params));
    }

    static protected <T>List<String> resolveArchives(List<T> params) {
        String val;
        List<String> archives = new LinkedList<>();
        if ( params!=null && (val = paramValue(params, IDX_APPLTJARS)) != null && !val.trim().equals("") ) {
            String archiveParts[] = val.split(quote(","));
            Arrays.asList(archiveParts).stream().map(String::trim).forEach(archives::add);
        }

        return archives;
    }

    static public <T>AppletParameters resolveAppletParameters(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_APPLTPARAMS)) != null ) {
            String opcode = resolveOpCode(params);
            if (val != null && OpCode.parse(opcode) == OpCode.LOAD_APPLET) {
                return AppletParameters.getInstance(val);
            }
            else if (val != null) {
                throw new RuntimeException(String.format("Incorrect operation provided: %s. Expected op: load_applet", opcode));
            }
        }

        throw new RuntimeException(String.format("No applet parameters set in the request: %s", params));
    }

    static public <T>AppletTagDef resolveAppletTagDef(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_APPLTTAG)) != null ) {
            String opcode = resolveOpCode(params);
            if (val != null && OpCode.parse(opcode) == OpCode.LOAD_APPLET) {
                String parts[] = val.split(quote("="));

                return AppletTagDef.getAppletTagDef(parts[0]);
            }
        }

        return AppletTagDef.UNKNOWN;
    }
    static public <T>String resolveAppletTag(List<T> params, AppletTagDef tagdef) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_APPLTTAG)) != null ) {
            String opcode = resolveOpCode(params);
            if ( val != null && OpCode.parse(opcode) == OpCode.LOAD_APPLET ) {
                String parts[] = val.split(quote("="));
                if ( parts.length < 2 ) return "";

                AppletTagDef def = AppletTagDef.getAppletTagDef(parts[0]);

                if ( def == tagdef ) {
                    return parts[1];
                }
                else return null;
            }
            else if (val != null) {
                throw new RuntimeException(String.format("Incorrect operation provided: %s. Expected op: load_applet", opcode));
            }
        }

        throw new RuntimeException(String.format("No applet tag definition found for params: %s", params));
    }

    static protected <T>String resolveLoadResourceURL(List<T> params) {
        String val;
        if ( params!=null && (val = paramValue(params, IDX_RESURL)) != null ) {
            String opcode = resolveOpCode(params);
            if ( val != null && OpCode.parse(opcode) == OpCode.LOAD_APPLET ) {
                String ext = FilenameUtils.getExtension(val);

                /// Save the resource name for later usage
                ConfigurationHelper.CONFIG.setProperty(CONFIG_PROP_RESOURCENAME, val);

                if ( ext != null && ext.trim().equalsIgnoreCase("class") ) {
                    val = val.replace(".class", "")
                             .replace(".", "/"); // For situations where the code parameter have the package name:
                                                                  // com.xpto.Applet.class ==> com/xpto/Applet.class
                }

                return val.concat(".class");
            }
            else if (val != null) {
                throw new RuntimeException(String.format("Incorrect operation provided: %s. Expected op: load_applet", opcode));
            }
        }

        throw new RuntimeException(String.format("No URL found for params: %s", params));
    }

    static protected <T>Map<String,String> resolveCookies(List<T> params) {
        String opcode = resolveOpCode(params);
        Map<String,String> cookies = new LinkedHashMap<>();
        if ( params!=null && params.size() > (IDX_RESURL +1) ) {
            if ( OpCode.parse(opcode) == OpCode.LOAD_APPLET ) {
                for (int i=(IDX_RESURL+1); i<params.size(); i+=2 /*key value pairs*/ ) {
                    String key = paramValue(params, i);
                    if ( params.size() > (i +1)) {
                        String value = paramValue(params, i+1);
                        cookies.put(key, value);
                    }
                }
            }
            else {
                throw new RuntimeException(String.format("Incorrect operation provided: %s. Expected op: load_applet", opcode));
            }
        }

        return cookies;
    }
}
