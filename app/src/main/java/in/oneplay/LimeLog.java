package in.oneplay;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import in.oneplay.backend.OneplayApi;

public class LimeLog {
    private static final Logger LOGGER = Logger.getLogger(LimeLog.class.getName());

    public static void info(String msg) {
        if (BuildConfig.DEBUG) {
            LOGGER.info(msg);
        }
    }
    
    public static void warning(String msg) {
        if (BuildConfig.DEBUG) {
            LOGGER.warning(msg);
        }
    }
    
    public static void severe(String msg) {
        msg = msg + traceToString(new Throwable().getStackTrace());

        if (BuildConfig.DEBUG) {
        LOGGER.severe(msg);
        } else {
            OneplayApi.getInstance().registerEvent(msg);
        }
    }
    
    public static void setFileHandler(String fileName) throws IOException {
        LOGGER.addHandler(new FileHandler(fileName));
    }

    private static String traceToString(StackTraceElement[] trace) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trace.length; i++) {
            if (i != 0) {
                sb.append("\n\tat ");
                sb.append(trace[i]);
            }
        }

        return sb.toString();
    }
}
