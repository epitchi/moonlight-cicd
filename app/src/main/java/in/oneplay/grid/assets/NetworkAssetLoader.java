package in.oneplay.grid.assets;

import android.content.Context;

import in.oneplay.LimeLog;
import in.oneplay.binding.PlatformBinding;
import in.oneplay.nvstream.http.NvHTTP;
import in.oneplay.utils.ServerHelper;

import java.io.IOException;
import java.io.InputStream;

public class NetworkAssetLoader {
    private final Context context;
    private final String uniqueId;

    public NetworkAssetLoader(Context context, String uniqueId) {
        this.context = context;
        this.uniqueId = uniqueId;
    }

    public InputStream getBitmapStream(CachedAppAssetLoader.LoaderTuple tuple) {
        InputStream in = null;
        try {
            NvHTTP http = new NvHTTP(context, ServerHelper.getCurrentAddressFromComputer(tuple.computer), uniqueId,
                    tuple.computer.serverCert, PlatformBinding.getCryptoProvider(context));
            in = http.getBoxArt(tuple.app);
        } catch (IOException ignored) {}

        if (in != null) {
            LimeLog.info("Network asset load complete: " + tuple);
        }
        else {
            LimeLog.info("Network asset load failed: " + tuple);
        }

        return in;
    }
}
