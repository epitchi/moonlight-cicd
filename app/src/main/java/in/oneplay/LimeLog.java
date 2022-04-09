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
        OneplayApi.getInstance().registerEvent(msg);
        if (BuildConfig.DEBUG) {
            LOGGER.severe(msg);
        }
    }
    
    public static void setFileHandler(String fileName) throws IOException {
        LOGGER.addHandler(new FileHandler(fileName));
    }
}
