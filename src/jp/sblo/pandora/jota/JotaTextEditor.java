package jp.sblo.pandora.jota;

import android.app.Application;
import android.os.Build;


public class JotaTextEditor extends Application {
    public static boolean sHoneycomb = ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB );

    @Override
    public void onCreate() {
        super.onCreate();

        SettingsActivity.isVersionUp(this);
        IS01FullScreen.createInstance();
    }

}
