package in.oneplay;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    public static void warning(Throwable throwable) {
        warning("", throwable);
    }

    public static void warning(String msg, Throwable throwable) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));

        msg = msg + "\n" + errors;

        warning(msg);
    }
    
    public static void warning(String msg) {
        if (BuildConfig.DEBUG) {
            LOGGER.warning(msg);
        }
    }

    public static void severe(Throwable throwable) {
        severe("", throwable);
    }

    public static void severe(String msg, Throwable throwable) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));

        msg = msg + "\n" + errors;

        severe(msg);
    }
    
    public static void severe(String msg) {
        if (BuildConfig.DEBUG) {
            LOGGER.severe(msg);
        } else {
            OneplayApi.getInstance().registerEvent(msg);
        }
    }
    
    public static void setFileHandler(String fileName) throws IOException {
        LOGGER.addHandler(new FileHandler(fileName));
    }
}
