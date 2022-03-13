package in.oneplay.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import in.oneplay.R;
import in.oneplay.backend.ClientConfig;

public class ServerPreferenceConfiguration {
    private static final String FPS_PREF_STRING = "list_fps";
    private static final String RESOLUTION_PREF_STRING = "list_resolution";
    private static final String VIDEO_FORMAT_PREF_STRING = "video_format";
    private static final String AUDIO_CONFIG_PREF_STRING = "list_audio_config";


    private static void setBitrate(Context context, int bitrateKbps, SharedPreferences.Editor editor) {
        if (bitrateKbps == 0) {
            bitrateKbps = PreferenceConfiguration.getDefaultBitrate(context);
        }
        editor.putInt(PreferenceConfiguration.BITRATE_PREF_STRING, bitrateKbps);
    }

    private static void setAudioConfig(Context context, String audioType, SharedPreferences.Editor editor) {
        String[] audioNames = context.getResources().getStringArray(R.array.audio_config_names);
        String[] audioValues = context.getResources().getStringArray(R.array.audio_config_values);

        for (int i = 0; i < audioNames.length; i++) {
            if (audioNames[i].equals(audioType)) {
                editor.putString(AUDIO_CONFIG_PREF_STRING, audioValues[i]).apply();
                return;
            }
        }
    }

    public static void savePreferences(Context context, ClientConfig config) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putString(FPS_PREF_STRING, Integer.toString(config.getGame_fps()));
        editor.putString(VIDEO_FORMAT_PREF_STRING, config.getStreamCodec());
        editor.putString(RESOLUTION_PREF_STRING, config.getResolution());

        setBitrate(context, (int) config.getBitrateKbps(), editor);

        setAudioConfig(context, config.getAudioType(), editor);

        editor.apply();
    }
}
