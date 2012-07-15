package jp.sblo.pandora.jota;

import android.content.Context;
import android.media.MediaScannerConnection;

public class MediaScannerWrapper {
    static void scanFile(Context context, String []paths , String []mimeTypes )
    {
        MediaScannerConnection.scanFile(context, paths, mimeTypes, null);
    }
}
