package in.oneplay.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import in.oneplay.R;
import in.oneplay.backend.ClientConfig;

public class OneplayPreferenceConfiguration {
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

    public static void setBitrateKbps(Context context, Integer bitrateKbps) {
        if (bitrateKbps != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putInt(PreferenceConfiguration.BITRATE_PREF_STRING, bitrateKbps);
            editor.apply();
        }
    }

    private static void setControllerMouseEmulation(Boolean isControllerMouseEmulationEnabled, SharedPreferences.Editor editor) {
        if (isControllerMouseEmulationEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.MOUSE_EMULATION_STRING, isControllerMouseEmulationEnabled);
        }
    }

    private static void setControllerUsbDriverSupport(Boolean isControllerUsbDriverSupportEnabled, SharedPreferences.Editor editor) {
        if (isControllerUsbDriverSupportEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.USB_DRIVER_PREF_SRING, isControllerUsbDriverSupportEnabled);
        }
    }

    private static void setDisableFrameDrop(Boolean isFrameDropDisabled, SharedPreferences.Editor editor) {
        if (isFrameDropDisabled != null) {
            editor.putBoolean(PreferenceConfiguration.LEGACY_DISABLE_FRAME_DROP_PREF_STRING, isFrameDropDisabled);
        }
    }

    private static void setEnableHdr(Boolean isHdrEnabled, SharedPreferences.Editor editor) {
        if (isHdrEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ENABLE_HDR_PREF_STRING, isHdrEnabled);
        }
    }

    private static void setEnablePerfOverlay(Boolean isPerfOverlayEnabled, SharedPreferences.Editor editor) {
        if (isPerfOverlayEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ENABLE_PERF_OVERLAY_STRING, isPerfOverlayEnabled);
        }
    }

    private static void setEnablePip(Boolean isPipEnabled, SharedPreferences.Editor editor) {
        if (isPipEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ENABLE_PIP_PREF_STRING, isPipEnabled);
        }
    }

    private static void setLatencyToast(Boolean isPostStreamToastEnabled, SharedPreferences.Editor editor) {
        if (isPostStreamToastEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.LATENCY_TOAST_PREF_STRING, isPostStreamToastEnabled);
        }
    }

    private static void setFps(Integer fps, SharedPreferences.Editor editor) {
        if (fps != null) {
            editor.putString(PreferenceConfiguration.FPS_PREF_STRING, fps.toString());
        }
    }

    private static void setMaxBitrateKbps(Integer maxBitrateKbps, SharedPreferences.Editor editor) {
        if (maxBitrateKbps != null) {
            editor.putInt(PreferenceConfiguration.MAX_BITRATE_PREF_STRING, maxBitrateKbps);
        }
    }

    private static void setMouseNavButtons(Boolean isMouseNavButtonsEnabled, SharedPreferences.Editor editor) {
        if (isMouseNavButtonsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.MOUSE_NAV_BUTTONS_STRING, isMouseNavButtonsEnabled);
        }
    }

    private static void setOnscreenController(Boolean isOnscreenControlsEnabled, SharedPreferences.Editor editor) {
        if (isOnscreenControlsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ONSCREEN_CONTROLLER_PREF_STRING, isOnscreenControlsEnabled);
        }
    }

    private static void setScreenResolution(String screenResolution, SharedPreferences.Editor editor) {
        if (screenResolution != null) {
            editor.putString(PreferenceConfiguration.RESOLUTION_PREF_STRING, screenResolution);
        }
    }

    public static void setScreenResolution(Context context, String screenResolution) {
        if (screenResolution != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(PreferenceConfiguration.RESOLUTION_PREF_STRING, screenResolution);
            editor.apply();
        }
    }

    private static void setVideoCodecConfig(String streamCodec, SharedPreferences.Editor editor) {
        if (streamCodec != null) {
            editor.putString(PreferenceConfiguration.VIDEO_FORMAT_PREF_STRING, streamCodec).apply();
        }
    }

    public static void setVideoCodecConfig(Context context, String streamCodec) {
        if (streamCodec != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(PreferenceConfiguration.VIDEO_FORMAT_PREF_STRING, streamCodec);
            editor.apply();
        }
    }

    private static void setUnlockFps(Boolean isUnlockFpsEnabled, SharedPreferences.Editor editor) {
        if (isUnlockFpsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.UNLOCK_FPS_STRING, isUnlockFpsEnabled);
        }
    }

    private static void setVibrateOsc(Boolean isVibrateOscEnabled, SharedPreferences.Editor editor) {
        if (isVibrateOscEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.VIBRATE_OSC_PREF_STRING, isVibrateOscEnabled);
        }
    }

    private static void setWindowMode(Context context, String windowMode, SharedPreferences.Editor editor) {
        if (windowMode != null) {
            String[] modeNames = context.getResources().getStringArray(R.array.oneplay_window_mode_names);
            editor.putBoolean(PreferenceConfiguration.STRETCH_PREF_STRING, windowMode.equals(modeNames[0]));
        }
    }

    public static void setWindowMode(Context context, boolean isWindowModeDisable) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.STRETCH_PREF_STRING, isWindowModeDisable);
        editor.apply();
    }

    private static void setHttpPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_HTTP_PORT_STRING, port);
            editor.apply();
        }
    }

    private static void setHttpsPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_HTTPS_PORT_STRING, port);
            editor.apply();
        }
    }

    private static void setAudioPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_AUDIO_PORT_STRING, port);
            editor.apply();
        }
    }

    private static void setVideoPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_VIDEO_PORT_STRING, port);
            editor.apply();
        }
    }

    private static void setControlPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_CONTROL_PORT_STRING, port);
            editor.apply();
        }
    }

    private static void setRtspPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_RTSP_PORT_STRING, port);
            editor.apply();
        }
    }

    private static void setPinPort(Integer port, SharedPreferences.Editor editor) {
        if (port != null && (port > 0 && port <= 0xFFFF)) {
            editor.putInt(PreferenceConfiguration.CUSTOM_PIN_PORT_STRING, port);
            editor.apply();
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
        setControllerMouseEmulation(config.isControllerMouseEmulationEnabled(), editor);
        setControllerUsbDriverSupport(config.isControllerUsbDriverSupportEnabled(), editor);
        setDisableFrameDrop(config.isFrameDropDisabled(), editor);
        setEnableHdr(config.isHdrEnabled(), editor);
        setEnablePerfOverlay(config.isPerfOverlayEnabled(), editor);
        setEnablePip(config.isPipEnabled(), editor);
        setLatencyToast(config.isPostStreamToastEnabled(), editor);
        setFps(config.getGameFps(), editor);
        setMaxBitrateKbps(config.getMaxBitrateKbps(), editor);
        setMouseNavButtons(config.isMouseNavButtonsEnabled(), editor);
        setOnscreenController(config.isOnscreenControlsEnabled(), editor);
        setScreenResolution(config.getResolution(), editor);
        setVideoCodecConfig(config.getStreamCodec(), editor);
        setUnlockFps(config.isUnlockFpsEnabled(), editor);
        setVibrateOsc(config.isVibrateOscEnabled(), editor);
        setWindowMode(context, config.getWindowMode(), editor);
        setHttpPort(config.getPortDetails().getHttpPort(), editor);
        setHttpsPort(config.getPortDetails().getHttpsPort(), editor);
        setAudioPort(config.getPortDetails().getAudioPort(), editor);
        setVideoPort(config.getPortDetails().getVideoPort(), editor);
        setControlPort(config.getPortDetails().getControlPort(), editor);
        setRtspPort(config.getPortDetails().getRtspPort(), editor);
        setPinPort(config.getPortDetails().getPinPort(), editor);

        editor.apply();
    }

    public static String getResolution(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PreferenceConfiguration.RESOLUTION_PREF_STRING, PreferenceConfiguration.DEFAULT_RESOLUTION);
    }

    public static String getVideoCodecConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PreferenceConfiguration.VIDEO_FORMAT_PREF_STRING, PreferenceConfiguration.DEFAULT_VIDEO_FORMAT);
    }
}
