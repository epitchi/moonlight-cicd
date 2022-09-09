package in.oneplay;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class OneplayApp extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        OneplayApp.context = getApplicationContext();

        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            LimeLog.severe("Uncaught exception", paramThrowable);

            // Without System.exit() this will not work.
            System.exit(2);
        });
    }

    public static Context getAppContext() {
        return OneplayApp.context;
    }
}
