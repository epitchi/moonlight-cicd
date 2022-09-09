package in.oneplay.backend;

import org.json.JSONException;
import org.json.JSONObject;

public class ClientConfig {
    /**
     * Audio mode. One of "stereo", "5.1-surround", "7.1-surround".
     */
    private String audioType;
    /**
     * Bitrate. Bitrate must be in range: 500 - 150000 kbps.
     */
    private Integer bitrateKbps;
    /**
     * Get capture system-wide keyboard shortcuts mode (like Alt+Tab).
     * Available options: "never", "fullscreen", "always"
     */
    private Boolean captureSysKeys;
    /**
     * Long pressing the Start button will switch the gamepad into mouse mode
     */
    private Boolean controllerMouseEmulationEnabled;
    /**
     * Enables a built-in USB driver for devices without native Xbox controller support
     */
    private Boolean controllerUsbDriverSupportEnabled;
    /**
     * May reduce micro-stuttering on some devices, but can increase latency
     */
    private Boolean frameDropDisabled;
    /**
     * Stream HDR when the game and PC GPU support it. HDR requires a GTX 1000 series GPU or later.
     */
    private Boolean hdrEnabled;
    /**
     * Display real-time stream performance information while streaming
     */
    private Boolean perfOverlayEnabled;
    /**
     * Allows the stream to be viewed (but not controlled) while multitasking
     */
    private Boolean pipEnabled;
    /**
     * Display a latency information message after the stream ends
     */
    private Boolean postStreamToastEnabled;
    /**
     * FPS. FPS must be in range 30 - 240.
     */
    private Integer gameFps;
    /**
     * VSync.
     */
    private Boolean vsyncEnabled;
    /**
     * Max bitrate.
     */
    private Integer maxBitrateKbps;
    /**
     * Max FPS.
     */
    private Integer maxFps;
    /**
     * Max resolution.
     */
    private String maxResolution;
    /**
     * Enabling this option may break right clicking on some buggy devices
     */
    private Boolean mouseNavButtonsEnabled;
    /**
     * Show virtual controller overlay on touchscreen
     */
    private Boolean onscreenControlsEnabled;
    /**
     * Screen resolution.
     */
    private String resolution;
    /**
     * Video codec. One of "auto", "H.264", "HEVC".
     */
    private String streamCodec;
    /**
     * Streaming at 90 or 120 FPS may reduce latency on high-end devices but can cause lag or instability on devices that can\'t support it
     */
    private Boolean unlockFpsEnabled;
    /**
     * Vibrates your device to emulate rumble for the on-screen controls
     */
    private Boolean vibrateOscEnabled;
    /**
     * Choose decoder mode automatically. Available options: "auto", "software", "hardware".
     */
    private String videoDecoderSelection;
    /**
     * Window mode. One of "fullscreen", "windowed", "borderless".
     */
    private String windowMode;
    /**
     * Advance details.
     */
    private AdvanceDetails advanceDetails;
    /**
     * PortDetails. Which ports(such as http, https, audio, video, control, rtsp and pin) should we use for this connection.
     */
    private PortDetails portDetails;

    public static class AdvanceDetails {
        /**
         * Check if remote desktop optimized mouse control is enabled. Will not work in most games.
         */
        private Boolean absoluteMouseMode;
        /**
         * Check if touchscreen in trackpad mode is enabled.
         */
        private Boolean absoluteTouchMode;
        /**
         * Check if gamepad input processing is enabled if the streaming client window loses focus.
         */
        private Boolean backgroundGamepad;
        /**
         * Check if delay for frames that come too early is enabled.
         */
        private Boolean framePacing;
        /**
         * Check if optimize game settings for streaming is enabled.
         */
        private Boolean gameOptimizations;
        /**
         * Multiple controllers support
         */
        private Boolean multiControl;
        /**
         * Check if audio mute is enabled if the streaming client window loses focus.
         */
        private Boolean muteOnFocusLoss;
        /**
         * Video packet size. 0 means that video packet size will be resolved later by the client
         * depending on NvComputer::isReachableOverVpn() output (Either 1024 or 1392).
         */
        private Integer packetSize;
        /**
         * Check if play audio on the host PC is enabled.
         */
        private Boolean playAudioOnHost;
        /**
         * Is app or game needs to be closed if the streaming client is closed.
         */
        private Boolean quitAppAfter;
        /**
         * Is invert scroll direction enabled.
         */
        private Boolean reverseScrollDirection;
        /**
         * Is swap A/B and X/Y gamepad buttons enabled (Nintendo-style).
         */
        private Boolean swapFaceButtons;
        /**
         * Check if swap left and right mouse buttons is enabled.
         */
        private Boolean swapMouseButtons;

        public void setAbsoluteMouseMode(Boolean absoluteMouseMode) {
            this.absoluteMouseMode = absoluteMouseMode;
        }

        public void setAbsoluteTouchMode(Boolean absoluteTouchMode) {
            this.absoluteTouchMode = absoluteTouchMode;
        }

        public void setBackgroundGamepad(Boolean backgroundGamepad) {
            this.backgroundGamepad = backgroundGamepad;
        }

        public void setFramePacing(Boolean framePacing) {
            this.framePacing = framePacing;
        }

        public void setGameOptimizations(Boolean gameOptimizations) {
            this.gameOptimizations = gameOptimizations;
        }

        public void setMultiControl(Boolean multiControl) {
            this.multiControl = multiControl;
        }

        public void setMuteOnFocusLoss(Boolean muteOnFocusLoss) {
            this.muteOnFocusLoss = muteOnFocusLoss;
        }

        public void setPacketSize(Integer packetSize) {
            this.packetSize = packetSize;
        }

        public void setPlayAudioOnHost(Boolean playAudioOnHost) {
            this.playAudioOnHost = playAudioOnHost;
        }

        public void setQuitAppAfter(Boolean quitAppAfter) {
            this.quitAppAfter = quitAppAfter;
        }

        public void setReverseScrollDirection(Boolean reverseScrollDirection) {
            this.reverseScrollDirection = reverseScrollDirection;
        }

        public void setSwapFaceButtons(Boolean swapFaceButtons) {
            this.swapFaceButtons = swapFaceButtons;
        }

        public void setSwapMouseButtons(Boolean swapMouseButtons) {
            this.swapMouseButtons = swapMouseButtons;
        }

        public Boolean isAbsoluteMouseMode() {
            return absoluteMouseMode;
        }

        public Boolean isAbsoluteTouchMode() {
            return absoluteTouchMode;
        }

        public Boolean isBackgroundGamepad() {
            return backgroundGamepad;
        }

        public Boolean isFramePacing() {
            return framePacing;
        }

        public Boolean isGameOptimizations() {
            return gameOptimizations;
        }

        public Boolean isMultiControl() {
            return multiControl;
        }

        public Boolean isMuteOnFocusLoss() {
            return muteOnFocusLoss;
        }

        public Integer getPacketSize() {
            return packetSize;
        }

        public Boolean isPlayAudioOnHost() {
            return playAudioOnHost;
        }

        public Boolean isQuitAppAfter() {
            return quitAppAfter;
        }

        public Boolean isReverseScrollDirection() {
            return reverseScrollDirection;
        }

        public Boolean isSwapFaceButtons() {
            return swapFaceButtons;
        }

        public Boolean isSwapMouseButtons() {
            return swapMouseButtons;
        }
        
        
    }

    public static class PortDetails {
        /**
         * HTTP port for connection
         */
        private Integer httpPort;
        /**
         * HTTPS port for connection
         */
        private Integer httpsPort;
        /**
         * Audio port for stream
         */
        private Integer audioPort;
        /**
         * Video port for stream
         */
        private Integer videoPort;
        /**
         * Control port for stream
         */
        private Integer controlPort;
        /**
         * RTSP port for stream
         */
        private Integer rtspPort;

        /**
         * PIN request port
         */
        private Integer pinPort;

        public Integer getHttpPort() {
            return httpPort;
        }

        public void setHttpPort(Integer httpPort) {
            this.httpPort = httpPort;
        }

        public Integer getHttpsPort() {
            return httpsPort;
        }

        public void setHttpsPort(Integer httpsPort) {
            this.httpsPort = httpsPort;
        }

        public Integer getAudioPort() {
            return audioPort;
        }

        public void setAudioPort(Integer audioPort) {
            this.audioPort = audioPort;
        }

        public Integer getVideoPort() {
            return videoPort;
        }

        public void setVideoPort(Integer videoPort) {
            this.videoPort = videoPort;
        }

        public Integer getControlPort() {
            return controlPort;
        }

        public void setControlPort(Integer controlPort) {
            this.controlPort = controlPort;
        }

        public Integer getRtspPort() {
            return rtspPort;
        }

        public void setRtspPort(Integer rtspPort) {
            this.rtspPort = rtspPort;
        }

        public Integer getPinPort() {
            return pinPort;
        }

        public void setPinPort(Integer pinPort) {
            this.pinPort = pinPort;
        }
    }

    public void setAudioType(String audioType) {
        this.audioType = audioType;
    }

    public void setBitrateKbps(Integer bitrateKbps) {
        this.bitrateKbps = bitrateKbps;
    }

    public void setCaptureSysKeys(Boolean captureSysKeys) {
        this.captureSysKeys = captureSysKeys;
    }

    public void setControllerMouseEmulationEnabled(Boolean controllerMouseEmulationEnabled) {
        this.controllerMouseEmulationEnabled = controllerMouseEmulationEnabled;
    }

    public void setControllerUsbDriverSupportEnabled(Boolean controllerUsbDriverSupportEnabled) {
        this.controllerUsbDriverSupportEnabled = controllerUsbDriverSupportEnabled;
    }

    public void setFrameDropDisabled(Boolean frameDropDisabled) {
        this.frameDropDisabled = frameDropDisabled;
    }

    public void setHdrEnabled(Boolean hdrEnabled) {
        this.hdrEnabled = hdrEnabled;
    }

    public void setPerfOverlayEnabled(Boolean perfOverlayEnabled) {
        this.perfOverlayEnabled = perfOverlayEnabled;
    }

    public void setPostStreamToastEnabled(Boolean postStreamToastEnabled) {
        this.postStreamToastEnabled = postStreamToastEnabled;
    }

    public void setPipEnabled(Boolean pipEnabled) {
        this.pipEnabled = pipEnabled;
    }

    public void setGameFps(Integer gameFps) {
        this.gameFps = gameFps;
    }

    public void setVsyncEnabled(Boolean vsyncEnabled) {
        this.vsyncEnabled = vsyncEnabled;
    }

    public void setMaxBitrateKbps(Integer maxBitrateKbps) {
        this.maxBitrateKbps = maxBitrateKbps;
    }

    public void setMaxFps(Integer maxFps) {
        this.maxFps = maxFps;
    }

    public void setMaxResolution(String maxResolution) {
        this.maxResolution = maxResolution;
    }

    public void setMouseNavButtonsEnabled(Boolean mouseNavButtonsEnabled) {
        this.mouseNavButtonsEnabled = mouseNavButtonsEnabled;
    }

    public void setOnscreenControlsEnabled(Boolean onscreenControlsEnabled) {
        this.onscreenControlsEnabled = onscreenControlsEnabled;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public void setStreamCodec(String streamCodec) {
        this.streamCodec = streamCodec;
    }

    public void setUnlockFpsEnabled(Boolean unlockFpsEnabled) {
        this.unlockFpsEnabled = unlockFpsEnabled;
    }

    public void setVibrateOscEnabled(Boolean vibrateOscEnabled) {
        this.vibrateOscEnabled = vibrateOscEnabled;
    }

    public void setVideoDecoderSelection(String videoDecoderSelection) {
        this.videoDecoderSelection = videoDecoderSelection;
    }

    public void setWindowMode(String windowMode) {
        this.windowMode = windowMode;
    }

    public void setAdvanceDetails(AdvanceDetails advanceDetails) {
        this.advanceDetails = advanceDetails;
    }

    public void setPortDetails(PortDetails portDetails) {
        this.portDetails = portDetails;
    }

    public String getAudioType() {
        return audioType;
    }

    public Integer getBitrateKbps() {
        return bitrateKbps;
    }

    public Boolean isCaptureSysKeys() {
        return captureSysKeys;
    }

    public Boolean isControllerMouseEmulationEnabled() {
        return controllerMouseEmulationEnabled;
    }

    public Boolean isControllerUsbDriverSupportEnabled() {
        return controllerUsbDriverSupportEnabled;
    }

    public Boolean isFrameDropDisabled() {
        return frameDropDisabled;
    }

    public Boolean isHdrEnabled() {
        return hdrEnabled;
    }

    public Boolean isPerfOverlayEnabled() {
        return perfOverlayEnabled;
    }

    public Boolean isPipEnabled() {
        return pipEnabled;
    }

    public Boolean isPostStreamToastEnabled() {
        return postStreamToastEnabled;
    }

    public Integer getGameFps() {
        return gameFps;
    }

    public Boolean isVsyncEnabled() {
        return vsyncEnabled;
    }

    public Integer getMaxBitrateKbps() {
        return maxBitrateKbps;
    }

    public Integer getMaxFps() {
        return maxFps;
    }

    public String getMaxResolution() {
        return maxResolution;
    }

    public Boolean isMouseNavButtonsEnabled() {
        return mouseNavButtonsEnabled;
    }

    public Boolean isOnscreenControlsEnabled() {
        return onscreenControlsEnabled;
    }

    public String getResolution() {
        return resolution;
    }

    public String getStreamCodec() {
        return streamCodec;
    }

    public Boolean isUnlockFpsEnabled() {
        return unlockFpsEnabled;
    }

    public Boolean isVibrateOscEnabled() {
        return vibrateOscEnabled;
    }

    public String getVideoDecoderSelection() {
        return videoDecoderSelection;
    }

    public String getWindowMode() {
        return windowMode;
    }

    public AdvanceDetails getAdvanceDetails() {
        return advanceDetails;
    }

    public PortDetails getPortDetails() {
        return portDetails;
    }

    public static ClientConfig fromJsonString(String json) throws JSONException {
        JSONObject data = new JSONObject(json);
        JSONObject otherDetailsData = data.getJSONObject("other_details");
        JSONObject advanceDetailsData = otherDetailsData.getJSONObject("advance_details");
        JSONObject serverDetailsData = data.getJSONObject("server_details");
        JSONObject portDetailsData = serverDetailsData.getJSONObject("port_details");

        ClientConfig.PortDetails portDetails = new ClientConfig.PortDetails();
        portDetails.setHttpPort(Utils.getInt(portDetailsData, "http_port"));
        portDetails.setHttpsPort(Utils.getInt(portDetailsData, "https_port"));
        portDetails.setAudioPort(Utils.getInt(portDetailsData, "audio_port"));
        portDetails.setVideoPort(Utils.getInt(portDetailsData, "video_port"));
        portDetails.setControlPort(Utils.getInt(portDetailsData, "control_port"));
        portDetails.setRtspPort(Utils.getInt(portDetailsData, "rtsp_port"));
        portDetails.setRtspPort(Utils.getInt(portDetailsData, "pin_port"));

        ClientConfig.AdvanceDetails advanceDetails = new ClientConfig.AdvanceDetails();
//        advanceDetails.setAbsoluteMouseMode(Utils.getBoolean(advanceDetailsData, "absolute_mouse_mode"));
        advanceDetails.setAbsoluteTouchMode(Utils.getBoolean(advanceDetailsData, "absolute_touch_mode"));
//        advanceDetails.setBackgroundGamepad(Utils.getBoolean(advanceDetailsData, "background_gamepad"));
//        advanceDetails.setFramePacing(Utils.getBoolean(advanceDetailsData, "frame_pacing"));
        advanceDetails.setGameOptimizations(Utils.getBoolean(advanceDetailsData, "game_optimizations"));
        advanceDetails.setMultiControl(Utils.getBoolean(advanceDetailsData, "multi_color")); //typo "multi_color" it means "multi controller"
//        advanceDetails.setMuteOnFocusLoss(Utils.getBoolean(advanceDetailsData, "mute_on_focus_loss"));
//        advanceDetails.setPacketSize(Utils.getInt(advanceDetailsData, "packet_size"));
        advanceDetails.setPlayAudioOnHost(Utils.getBoolean(advanceDetailsData, "play_audio_on_host"));
//        advanceDetails.setQuitAppAfter(Utils.getBoolean(advanceDetailsData, "quit_app_after"));
        advanceDetails.setReverseScrollDirection(Utils.getBoolean(advanceDetailsData, "reverse_scroll_direction"));
        advanceDetails.setSwapFaceButtons(Utils.getBoolean(advanceDetailsData, "swap_face_buttons"));
//        advanceDetails.setSwapMouseButtons( Utils.getBoolean(advanceDetailsData, "swap_mouse_buttons"));

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setAudioType(Utils.getString(otherDetailsData, "audio_type"));
        clientConfig.setBitrateKbps(Utils.getInt(otherDetailsData, "bitrate_kbps"));
//        clientConfig.setCaptureSysKeys(Utils.getBoolean(otherDetailsData, "capture_sys_keys"));
        clientConfig.setControllerMouseEmulationEnabled(Utils.getBoolean(otherDetailsData, "controller_mouse_emulation"));
        clientConfig.setControllerUsbDriverSupportEnabled(Utils.getBoolean(otherDetailsData, "controller_usb_driver_support"));
        clientConfig.setFrameDropDisabled(Utils.getBoolean(otherDetailsData, "disable_frame_drop"));
        clientConfig.setHdrEnabled(Utils.getBoolean(otherDetailsData, "enable_hdr"));
        clientConfig.setPerfOverlayEnabled(Utils.getBoolean(otherDetailsData, "enable_perf_overlay"));
        clientConfig.setPipEnabled(Utils.getBoolean(otherDetailsData, "enable_pip"));
        clientConfig.setPostStreamToastEnabled(Utils.getBoolean(otherDetailsData, "enable_post_stream_toast"));
        clientConfig.setGameFps(Utils.getInt(otherDetailsData, "game_fps"));
//        clientConfig.setVsyncEnabled(Utils.getBoolean(otherDetailsData, "is_vsync_enabled"));
//        clientConfig.setMaxBitrateKbps(Utils.getInt(otherDetailsData, "max_bitrate_kbps"));
//        clientConfig.setMaxFps(Utils.getInt(otherDetailsData, "max_fps"));
//        clientConfig.setMaxResolution(Utils.getString(otherDetailsData, "max_resolution"));
        clientConfig.setMouseNavButtonsEnabled(Utils.getBoolean(otherDetailsData, "mouse_nav_buttons"));
        clientConfig.setOnscreenControlsEnabled(Utils.getBoolean(otherDetailsData, "onscreen_controls"));
        clientConfig.setResolution(Utils.getString(otherDetailsData, "resolution"));
        clientConfig.setStreamCodec(Utils.getString(otherDetailsData, "stream_codec"));
        clientConfig.setUnlockFpsEnabled(Utils.getBoolean(otherDetailsData, "unlock_fps"));
        clientConfig.setVibrateOscEnabled(Utils.getBoolean(otherDetailsData, "vibrate_osc"));
//        clientConfig.setVideoDecoderSelection(Utils.getString(otherDetailsData, "video_decoder_selection"));
        clientConfig.setWindowMode(Utils.getString(otherDetailsData, "window_mode"));
        clientConfig.setAdvanceDetails(advanceDetails);
        clientConfig.setPortDetails(portDetails);
        return clientConfig;
    }
}
