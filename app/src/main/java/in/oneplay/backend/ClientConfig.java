package in.oneplay.backend;

public class ClientConfig {
    /**
     * Audio mode. One of "stereo", "5.1-surround", "7.1-surround".
     */
    private final String audioType;
    /**
     * Bitrate. Bitrate must be in range: 500 - 150000 kbps.
     */
    private final int bitrateKbps;
    /**
     * Get capture system-wide keyboard shortcuts mode (like Alt+Tab).
     * Available options: "never", "fullscreen", "always"
     */
    private final boolean captureSysKeys;
    /**
     * FPS. FPS must be in range 30 - 240.
     */
    private final int gameFps;
    /**
     * VSync.
     */
    private final boolean isVsyncEnabled;
    /**
     * Max bitrate.
     */
    private final int maxBitrateKbps;
    /**
     * Max FPS.
     */
    private final int maxFps;
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
        private final boolean absoluteMouseMode;
        /**
         * Check if touchscreen in trackpad mode is enabled.
         */
        private final boolean absoluteTouchMode;
        /**
         * Check if gamepad input processing is enabled if the streaming client window loses focus.
         */
        private final boolean backgroundGamepad;
        /**
         * Check if delay for frames that come too early is enabled.
         */
        private final boolean framePacing;
        /**
         * Check if optimize game settings for streaming is enabled.
         */
        private final boolean gameOptimizations;
        /**
         * Multiple controllers support
         */
        private final boolean multiControl;
        /**
         * Check if audio mute is enabled if the streaming client window loses focus.
         */
        private final boolean muteOnFocusLoss;
        /**
         * Video packet size. 0 means that video packet size will be resolved later by the client
         * depending on NvComputer::isReachableOverVpn() output (Either 1024 or 1392).
         */
        private final long packetSize;
        /**
         * Check if play audio on the host PC is enabled.
         */
        private final boolean playAudioOnHost;
        /**
         * Is app or game needs to be closed if the streaming client is closed.
         */
        private final boolean quitAppAfter;
        /**
         * Is invert scroll direction enabled.
         */
        private final boolean reverseScrollDirection;
        /**
         * Is swap A/B and X/Y gamepad buttons enabled (Nintendo-style).
         */
        private final boolean swapFaceButtons;
        /**
         * Check if swap left and right mouse buttons is enabled.
         */
        private final boolean swapMouseButtons;

        public AdvanceDetails(boolean absoluteMouseMode, boolean absoluteTouchMode, boolean backgroundGamepad,
                              boolean framePacing, boolean gameOptimizations, boolean multiControl, boolean muteOnFocusLoss,
                              long packetSize, boolean playAudioOnHost, boolean quitAppAfter, boolean reverseScrollDirection,
                              boolean swapFaceButtons, boolean swapMouseButtons) {
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

        public boolean isAbsoluteMouseMode() {
            return absoluteMouseMode;
        }

        public boolean isAbsoluteTouchMode() {
            return absoluteTouchMode;
        }

        public boolean isBackgroundGamepad() {
            return backgroundGamepad;
        }

        public boolean isFramePacing() {
            return framePacing;
        }

        public boolean isGameOptimizations() {
            return gameOptimizations;
        }

        public boolean isMultiControl() {
            return multiControl;
        }

        public boolean isMuteOnFocusLoss() {
            return muteOnFocusLoss;
        }

        public long getPacketSize() {
            return packetSize;
        }

        public boolean isPlayAudioOnHost() {
            return playAudioOnHost;
        }

        public boolean isQuitAppAfter() {
            return quitAppAfter;
        }

        public boolean isReverseScrollDirection() {
            return reverseScrollDirection;
        }

        public boolean isSwapFaceButtons() {
            return swapFaceButtons;
        }

        public boolean isSwapMouseButtons() {
            return swapMouseButtons;
        }
    }

    public ClientConfig(String audioType, int bitrateKbps, boolean captureSysKeys, int gameFps,
                        boolean isVsyncEnabled, int maxBitrateKbps, int maxFps, String maxResolution,
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

    public int getBitrateKbps() {
        return bitrateKbps;
    }

    public boolean isCaptureSysKeys() {
        return captureSysKeys;
    }

    public int getGameFps() {
        return gameFps;
    }

    public boolean isVsyncEnabled() {
        return isVsyncEnabled;
    }

    public int getMaxBitrateKbps() {
        return maxBitrateKbps;
    }

    public int getMaxFps() {
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
