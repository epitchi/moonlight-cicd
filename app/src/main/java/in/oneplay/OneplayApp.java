package in.oneplay;

import android.app.Application;

import java.io.PrintWriter;
import java.io.StringWriter;

public class OneplayApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            LimeLog.severe("Uncaught exception", paramThrowable);

            // Without System.exit() this will not work.
            System.exit(2);
        });
    }
}
