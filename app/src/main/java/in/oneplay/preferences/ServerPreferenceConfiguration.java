package in.oneplay.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

import in.oneplay.R;
import in.oneplay.backend.ClientConfig;

public class ServerPreferenceConfiguration {
    static final String AUDIO_CONFIG_PREF_STRING = "list_audio_config";

    private static void setAudioConfig(Context context, String audioType) {
        String[] audioNames = context.getResources().getStringArray(R.array.audio_config_names);
        String[] audioValues = context.getResources().getStringArray(R.array.audio_config_values);

        for (int i = 0; i < audioNames.length; i++) {
            if (audioNames[i].equals(audioType)) {
                PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .edit()
                        .putString(AUDIO_CONFIG_PREF_STRING, audioValues[i])
                        .apply();
                return;
            }
        }
    }

    public static void savePreferences(Context context, ClientConfig config) {
        // save audio config
        setAudioConfig(context, config.getAudioType());
    }
}
