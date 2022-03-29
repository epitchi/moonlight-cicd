package in.oneplay.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import in.oneplay.R;
import in.oneplay.backend.ClientConfig;

public class ServerPreferenceConfiguration {
    private static void setAbsoluteTouchMode(Boolean isAbsoluteTouchModeEnabled, SharedPreferences.Editor editor) {
        if (isAbsoluteTouchModeEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.TOUCHSCREEN_TRACKPAD_PREF_STRING, isAbsoluteTouchModeEnabled);
        }
    }

    private static void setGameOptimizations(Boolean isGameOptimizationsEnabled, SharedPreferences.Editor editor) {
        if (isGameOptimizationsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.SOPS_PREF_STRING, isGameOptimizationsEnabled);
        }
    }

    private static void setMultipleControllersSupport(Boolean isMultipleControllersSupportEnabled, SharedPreferences.Editor editor) {
        if (isMultipleControllersSupportEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.MULTI_CONTROLLER_PREF_STRING, isMultipleControllersSupportEnabled);
        }
    }

    private static void setPlayAudioOnHost(Boolean isPlayAudioOnHostEnabled, SharedPreferences.Editor editor) {
        if (isPlayAudioOnHostEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.HOST_AUDIO_PREF_STRING, isPlayAudioOnHostEnabled);
        }
    }

    private static void setSwapFaceButtons(Boolean isSwapFaceButtonsEnabled, SharedPreferences.Editor editor) {
        if (isSwapFaceButtonsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.FLIP_FACE_BUTTONS_PREF_STRING, isSwapFaceButtonsEnabled);
        }
    }

    private static void setAudioConfig(Context context, String audioType, SharedPreferences.Editor editor) {
        if (audioType != null) {
            String[] audioNames = context.getResources().getStringArray(R.array.oneplay_audio_config_names);
            String[] audioValues = context.getResources().getStringArray(R.array.audio_config_values);

            for (int i = 0; i < audioNames.length; i++) {
                if (audioNames[i].equals(audioType)) {
                    editor.putString(PreferenceConfiguration.AUDIO_CONFIG_PREF_STRING, audioValues[i]).apply();
                    return;
                }
            }
        }
    }

    private static void setBitrateKbps(Integer bitrateKbps, SharedPreferences.Editor editor) {
        if (bitrateKbps != null) {
            editor.putInt(PreferenceConfiguration.BITRATE_PREF_STRING, bitrateKbps);
        }
    }

    private static void setFps(Integer fps, SharedPreferences.Editor editor) {
        if (fps != null) {
            editor.putString(PreferenceConfiguration.FPS_PREF_STRING, fps.toString());
        }
    }

    private static void setScreenResolution(String screenResolution, SharedPreferences.Editor editor) {
        if (screenResolution != null) {
            editor.putString(PreferenceConfiguration.RESOLUTION_PREF_STRING, screenResolution);
        }
    }

    private static void setVideoCodecConfig(Context context, String streamCodec, SharedPreferences.Editor editor) {
        if (streamCodec != null) {
            String[] videoNames = context.getResources().getStringArray(R.array.oneplay_video_format_names);
            String[] videoValues = context.getResources().getStringArray(R.array.video_format_values);

            for (int i = 0; i < videoNames.length; i++) {
                if (videoNames[i].equals(streamCodec)) {
                    editor.putString(PreferenceConfiguration.VIDEO_FORMAT_PREF_STRING, videoValues[i]).apply();
                    return;
                }
            }
        }
    }

    private static void setWindowMode(Context context, String windowMode, SharedPreferences.Editor editor) {
        if (windowMode != null) {
            String[] modeNames = context.getResources().getStringArray(R.array.oneplay_video_format_names);
            editor.putBoolean(PreferenceConfiguration.STRETCH_PREF_STRING, windowMode.equals(modeNames[0]));
        }
    }

    public static void savePreferences(Context context, ClientConfig config) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        setAbsoluteTouchMode(config.getAdvanceDetails().isAbsoluteTouchMode(), editor);
        setGameOptimizations(config.getAdvanceDetails().isGameOptimizations(), editor);
        setMultipleControllersSupport(config.getAdvanceDetails().isMultiControl(), editor);
        setPlayAudioOnHost(config.getAdvanceDetails().isPlayAudioOnHost(), editor);
        setSwapFaceButtons(config.getAdvanceDetails().isSwapFaceButtons(), editor);
        setAudioConfig(context, config.getAudioType(), editor);
        setBitrateKbps(config.getBitrateKbps(), editor);
        setFps(config.getGameFps(), editor);
        setScreenResolution(config.getResolution(), editor);
        setVideoCodecConfig(context, config.getStreamCodec(), editor);
        setWindowMode(context, config.getWindowMode(), editor);

        editor.apply();
    }
}
