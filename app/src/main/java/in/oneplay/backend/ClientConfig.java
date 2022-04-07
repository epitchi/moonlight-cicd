package in.oneplay.backend;

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
}
