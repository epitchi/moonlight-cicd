package in.oneplay.binding;

import android.content.Context;

import in.oneplay.binding.audio.AndroidAudioRenderer;
import in.oneplay.binding.crypto.AndroidCryptoProvider;
import in.oneplay.nvstream.av.audio.AudioRenderer;
import in.oneplay.nvstream.http.LimelightCryptoProvider;

public class PlatformBinding {
    public static LimelightCryptoProvider getCryptoProvider(Context c) {
        return new AndroidCryptoProvider(c);
    }
}
