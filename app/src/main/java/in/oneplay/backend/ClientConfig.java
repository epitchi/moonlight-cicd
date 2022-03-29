package in.oneplay.backend;

public class ClientConfig {
    /**
     * Audio mode. One of "stereo", "5.1-surround", "7.1-surround".
     */
    private final String audioType;
    /**
     * Bitrate. Bitrate must be in range: 500 - 150000 kbps.
     */
    private final Integer bitrateKbps;
    /**
     * Get capture system-wide keyboard shortcuts mode (like Alt+Tab).
     * Available options: "never", "fullscreen", "always"
     */
    private final Boolean captureSysKeys;
    /**
     * FPS. FPS must be in range 30 - 240.
     */
    private final Integer gameFps;
    /**
     * VSync.
     */
    private final Boolean isVsyncEnabled;
    /**
     * Max bitrate.
     */
    private final Integer maxBitrateKbps;
    /**
     * Max FPS.
     */
    private final Integer maxFps;
    /**
     * Max resolution.
     */
    private final String maxResolution;
    /**
     * Screen resolution.
     */
    private final String resolution;
    /**
     * Video codec. One of "auto", "H.264", "HEVC".
     */
    private final String streamCodec;
    /**
     * Choose decoder mode automatically. Available options: "auto", "software", "hardware".
     */
    private final String videoDecoderSelection;
    /**
     * Window mode. One of "fullscreen", "windowed", "borderless".
     */
    private final String windowMode;
    /**
     * Advance details.
     */
    private final AdvanceDetails advanceDetails;

    public static class AdvanceDetails {
        /**
         * Check if remote desktop optimized mouse control is enabled. Will not work in most games.
         */
        private final Boolean absoluteMouseMode;
        /**
         * Check if touchscreen in trackpad mode is enabled.
         */
        private final Boolean absoluteTouchMode;
        /**
         * Check if gamepad input processing is enabled if the streaming client window loses focus.
         */
        private final Boolean backgroundGamepad;
        /**
         * Check if delay for frames that come too early is enabled.
         */
        private final Boolean framePacing;
        /**
         * Check if optimize game settings for streaming is enabled.
         */
        private final Boolean gameOptimizations;
        /**
         * Multiple controllers support
         */
        private final Boolean multiControl;
        /**
         * Check if audio mute is enabled if the streaming client window loses focus.
         */
        private final Boolean muteOnFocusLoss;
        /**
         * Video packet size. 0 means that video packet size will be resolved later by the client
         * depending on NvComputer::isReachableOverVpn() output (Either 1024 or 1392).
         */
        private final Integer packetSize;
        /**
         * Check if play audio on the host PC is enabled.
         */
        private final Boolean playAudioOnHost;
        /**
         * Is app or game needs to be closed if the streaming client is closed.
         */
        private final Boolean quitAppAfter;
        /**
         * Is invert scroll direction enabled.
         */
        private final Boolean reverseScrollDirection;
        /**
         * Is swap A/B and X/Y gamepad buttons enabled (Nintendo-style).
         */
        private final Boolean swapFaceButtons;
        /**
         * Check if swap left and right mouse buttons is enabled.
         */
        private final Boolean swapMouseButtons;

        public AdvanceDetails(Boolean absoluteMouseMode, Boolean absoluteTouchMode, Boolean backgroundGamepad,
                              Boolean framePacing, Boolean gameOptimizations, Boolean multiControl, Boolean muteOnFocusLoss,
                              Integer packetSize, Boolean playAudioOnHost, Boolean quitAppAfter, Boolean reverseScrollDirection,
                              Boolean swapFaceButtons, Boolean swapMouseButtons) {
            this.absoluteMouseMode = absoluteMouseMode;
            this.absoluteTouchMode = absoluteTouchMode;
            this.backgroundGamepad = backgroundGamepad;
            this.framePacing = framePacing;
            this.gameOptimizations = gameOptimizations;
            this.multiControl = multiControl;
            this.muteOnFocusLoss = muteOnFocusLoss;
            this.packetSize = packetSize;
            this.playAudioOnHost = playAudioOnHost;
            this.quitAppAfter = quitAppAfter;
            this.reverseScrollDirection = reverseScrollDirection;
            this.swapFaceButtons = swapFaceButtons;
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

    public ClientConfig(String audioType, Integer bitrateKbps, Boolean captureSysKeys, Integer gameFps,
                        Boolean isVsyncEnabled, Integer maxBitrateKbps, Integer maxFps, String maxResolution,
                        String resolution, String streamCodec, String videoDecoderSelection, String windowMode,
                        AdvanceDetails advanceDetails) {
        this.audioType = audioType;
        this.bitrateKbps = bitrateKbps;
        this.captureSysKeys = captureSysKeys;
        this.gameFps = gameFps;
        this.isVsyncEnabled = isVsyncEnabled;
        this.maxBitrateKbps = maxBitrateKbps;
        this.maxFps = maxFps;
        this.maxResolution = maxResolution;
        this.resolution = resolution;
        this.streamCodec = streamCodec;
        this.videoDecoderSelection = videoDecoderSelection;
        this.windowMode = windowMode;
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

    public Integer getGameFps() {
        return gameFps;
    }

    public Boolean isVsyncEnabled() {
        return isVsyncEnabled;
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

    public String getResolution() {
        return resolution;
    }

    public String getStreamCodec() {
        return streamCodec;
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
