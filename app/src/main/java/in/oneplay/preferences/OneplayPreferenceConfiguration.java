package in.oneplay.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import in.oneplay.R;
import in.oneplay.backend.ClientConfig;
import in.oneplay.binding.input.virtual_controller.VirtualControllerConfigurationLoader;

public class OneplayPreferenceConfiguration {
    private static void setAbsoluteTouchMode(Boolean isAbsoluteTouchModeEnabled, SharedPreferences.Editor editor) {
        if (isAbsoluteTouchModeEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.TOUCHSCREEN_TRACKPAD_PREF_STRING, isAbsoluteTouchModeEnabled);
        }
    }

    public static void setTouchscreenTrackpad(Context context, boolean isTouchscreenTrackpadEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.TOUCHSCREEN_TRACKPAD_PREF_STRING, isTouchscreenTrackpadEnabled);
        editor.apply();
    }

    private static void setGameOptimizations(Boolean isGameOptimizationsEnabled, SharedPreferences.Editor editor) {
        if (isGameOptimizationsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.SOPS_PREF_STRING, isGameOptimizationsEnabled);
        }
    }

    public static void setGameOptimizations(Context context, boolean isGameOptimizationsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.SOPS_PREF_STRING, isGameOptimizationsEnabled);
        editor.apply();
    }

    private static void setMultipleControllersSupport(Boolean isMultipleControllersSupportEnabled, SharedPreferences.Editor editor) {
        if (isMultipleControllersSupportEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.MULTI_CONTROLLER_PREF_STRING, isMultipleControllersSupportEnabled);
        }
    }

    public static void setMultipleControllersSupport(Context context, boolean isMultipleControllersSupportEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.MULTI_CONTROLLER_PREF_STRING, isMultipleControllersSupportEnabled);
        editor.apply();
    }

    private static void setPlayAudioOnHost(Boolean isPlayAudioOnHostEnabled, SharedPreferences.Editor editor) {
        if (isPlayAudioOnHostEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.HOST_AUDIO_PREF_STRING, isPlayAudioOnHostEnabled);
        }
    }

    public static void setPlayAudioOnHost(Context context, boolean isPlayAudioOnHostEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.HOST_AUDIO_PREF_STRING, isPlayAudioOnHostEnabled);
        editor.apply();
    }

    private static void setSwapFaceButtons(Boolean isSwapFaceButtonsEnabled, SharedPreferences.Editor editor) {
        if (isSwapFaceButtonsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.FLIP_FACE_BUTTONS_PREF_STRING, isSwapFaceButtonsEnabled);
        }
    }

    public static void setSwapFaceButtons(Context context, boolean isSwapFaceButtonsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.FLIP_FACE_BUTTONS_PREF_STRING, isSwapFaceButtonsEnabled);
        editor.apply();
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

    public static void setAudioConfig(Context context, String audioType) {
        if (audioType != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(PreferenceConfiguration.AUDIO_CONFIG_PREF_STRING, audioType);
            editor.apply();
        }
    }

    public static void setEnableAudioFx(Context context, Boolean isAudioFxEnabled) {
        if (isAudioFxEnabled != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean(PreferenceConfiguration.ENABLE_AUDIO_FX_PREF_STRING, isAudioFxEnabled);
            editor.apply();
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
            editor.putInt(PreferenceConfiguration.BITRATE_PREF_STRING, bitrateKbps * 1000);
            editor.apply();
        }
    }

    private static void setControllerMouseEmulation(Boolean isControllerMouseEmulationEnabled, SharedPreferences.Editor editor) {
        if (isControllerMouseEmulationEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.MOUSE_EMULATION_STRING, isControllerMouseEmulationEnabled);
        }
    }

    public static void setControllerMouseEmulation(Context context, boolean isControllerMouseEmulationEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.MOUSE_EMULATION_STRING, isControllerMouseEmulationEnabled);
        editor.apply();
    }

    private static void setControllerUsbDriverSupport(Boolean isControllerUsbDriverSupportEnabled, SharedPreferences.Editor editor) {
        if (isControllerUsbDriverSupportEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.USB_DRIVER_PREF_SRING, isControllerUsbDriverSupportEnabled);
        }
    }

    public static void setControllerUsbDriverSupport(Context context, boolean isControllerUsbDriverSupportEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.USB_DRIVER_PREF_SRING, isControllerUsbDriverSupportEnabled);
        editor.apply();
    }

    public static void setBindAllUsb(Context context, boolean isBindAllUsbEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.BIND_ALL_USB_STRING, isBindAllUsbEnabled);
        editor.apply();
    }

    public static void setVibrateFallback(Context context, boolean isVibrateFallbackEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.VIBRATE_FALLBACK_PREF_STRING, isVibrateFallbackEnabled);
        editor.apply();
    }

    public static void setAbsoluteMouseMode(Context context, boolean isAbsoluteMouseModeEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.ABSOLUTE_MOUSE_MODE_PREF_STRING, isAbsoluteMouseModeEnabled);
        editor.apply();
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

    public static void setEnableHdr(Context context, boolean isHdrEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.ENABLE_HDR_PREF_STRING, isHdrEnabled);
        editor.apply();
    }

    private static void setEnablePerfOverlay(Boolean isPerfOverlayEnabled, SharedPreferences.Editor editor) {
        if (isPerfOverlayEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ENABLE_PERF_OVERLAY_STRING, isPerfOverlayEnabled);
        }
    }

    public static void setEnablePerfOverlay(Context context, boolean isPerfOverlayEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.ENABLE_PERF_OVERLAY_STRING, isPerfOverlayEnabled);
        editor.apply();
    }

    private static void setEnablePip(Boolean isPipEnabled, SharedPreferences.Editor editor) {
        if (isPipEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ENABLE_PIP_PREF_STRING, isPipEnabled);
        }
    }

    public static void setEnablePip(Context context, boolean isPipEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.ENABLE_PIP_PREF_STRING, isPipEnabled);
        editor.apply();
    }

    private static void setLatencyToast(Boolean isPostStreamToastEnabled, SharedPreferences.Editor editor) {
        if (isPostStreamToastEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.LATENCY_TOAST_PREF_STRING, isPostStreamToastEnabled);
        }
    }

    public static void setLatencyToast(Context context, boolean isPostStreamToastEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.LATENCY_TOAST_PREF_STRING, isPostStreamToastEnabled);
        editor.apply();
    }

    private static void setFps(Integer fps, SharedPreferences.Editor editor) {
        if (fps != null) {
            editor.putString(PreferenceConfiguration.FPS_PREF_STRING, fps.toString());
        }
    }

    public static void setFps(Context context, String fps) {
        if (fps != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(PreferenceConfiguration.FPS_PREF_STRING, fps);
            editor.apply();
        }
    }

    public static void setFramePacing(Context context, String framePacing) {
        if (framePacing != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(PreferenceConfiguration.FRAME_PACING_PREF_STRING, framePacing);
            editor.apply();
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

    public static void setMouseNavButtons(Context context, boolean isMouseNavButtonsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.MOUSE_NAV_BUTTONS_STRING, isMouseNavButtonsEnabled);
        editor.apply();
    }

    private static void setOnscreenController(Boolean isOnscreenControlsEnabled, SharedPreferences.Editor editor) {
        if (isOnscreenControlsEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.ONSCREEN_CONTROLLER_PREF_STRING, isOnscreenControlsEnabled);
        }
    }

    public static void setOnscreenController(Context context, boolean isOnscreenControlsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.ONSCREEN_CONTROLLER_PREF_STRING, isOnscreenControlsEnabled);
        editor.apply();
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

    public static void setLanguage(Context context, String language) {
        if (language != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(PreferenceConfiguration.LANGUAGE_PREF_STRING, language);
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

    public static void setUnlockFps(Context context, boolean isUnlockFpsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.UNLOCK_FPS_STRING, isUnlockFpsEnabled);
        editor.apply();
    }

    private static void setVibrateOsc(Boolean isVibrateOscEnabled, SharedPreferences.Editor editor) {
        if (isVibrateOscEnabled != null) {
            editor.putBoolean(PreferenceConfiguration.VIBRATE_OSC_PREF_STRING, isVibrateOscEnabled);
        }
    }

    public static void setVibrateOsc(Context context, boolean isVibrateOscEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.VIBRATE_OSC_PREF_STRING, isVibrateOscEnabled);
        editor.apply();
    }

    public static void setOnlyShowL2R3(Context context, boolean isOnlyShowL2R3) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.ONLY_L3_R3_PREF_STRING, isOnlyShowL2R3);
        editor.apply();
    }

    public static void setOscOpacity(Context context, int oscOpacity) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(PreferenceConfiguration.OSC_OPACITY_PREF_STRING, oscOpacity);
        editor.apply();
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

    public static void setDeadzonePercentage(Context context, int deadzonePercentage) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(PreferenceConfiguration.DEADZONE_PREF_STRING, deadzonePercentage);
        editor.apply();
    }

    public static void setSmallIcons(Context context, boolean isSmallIconsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.SMALL_ICONS_PREF_STRING, isSmallIconsEnabled);
        editor.apply();
    }

    public static void setDisableToasts(Context context, boolean isDisableToastsEnabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(PreferenceConfiguration.DISABLE_TOASTS_PREF_STRING, isDisableToastsEnabled);
        editor.apply();
    }

    public static void resetOsc(Context context) {
        context.getSharedPreferences(VirtualControllerConfigurationLoader.OSC_PREFERENCE, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
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

    public static String getAudioConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PreferenceConfiguration.AUDIO_CONFIG_PREF_STRING, PreferenceConfiguration.DEFAULT_AUDIO_CONFIG);
    }
}
