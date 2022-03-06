package in.oneplay.backend;

public class ClientConfig {
    private final String audioType;
    private final long bitrateKbps;
    private final boolean captureSysKeys;
    private final int game_fps;
    private final boolean isVsyncEnabled;
    private final long maxBitrateKbps;
    private final int max_fps;
    private final String maxResolution;
    private final String resolution;
    private final String streamCodec;
    private final String videoDecoderSelection;
    private final String windowMode;
    private final AdvanceDetails advanceDetails;

    public static class AdvanceDetails {
        private final boolean absoluteMouseMode;
        private final boolean absoluteTouchMode;
        private final boolean backgroundGamepad;
        private final boolean framePacing;
        private final boolean gameOptimizations;
        private final boolean multiColor;
        private final boolean muteOnFocusLoss;
        private final long packetSize;
        private final boolean playAudioOnHost;
        private final boolean quitAppAfter;
        private final boolean reverseScrollDirection;
        private final boolean swapFaceButtons;
        private final boolean swapMouseButtons;

        public AdvanceDetails(boolean absoluteMouseMode, boolean absoluteTouchMode, boolean backgroundGamepad,
                              boolean framePacing, boolean gameOptimizations, boolean multiColor, boolean muteOnFocusLoss,
                              long packetSize, boolean playAudioOnHost, boolean quitAppAfter, boolean reverseScrollDirection,
                              boolean swapFaceButtons, boolean swapMouseButtons) {
            this.absoluteMouseMode = absoluteMouseMode;
            this.absoluteTouchMode = absoluteTouchMode;
            this.backgroundGamepad = backgroundGamepad;
            this.framePacing = framePacing;
            this.gameOptimizations = gameOptimizations;
            this.multiColor = multiColor;
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

        public boolean isMultiColor() {
            return multiColor;
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

    public ClientConfig(String audioType, long bitrateKbps, boolean captureSysKeys, int game_fps,
                        boolean isVsyncEnabled, long maxBitrateKbps, int max_fps, String maxResolution,
                        String resolution, String streamCodec, String videoDecoderSelection, String windowMode,
                        AdvanceDetails advanceDetails) {
        this.audioType = audioType;
        this.bitrateKbps = bitrateKbps;
        this.captureSysKeys = captureSysKeys;
        this.game_fps = game_fps;
        this.isVsyncEnabled = isVsyncEnabled;
        this.maxBitrateKbps = maxBitrateKbps;
        this.max_fps = max_fps;
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

    public long getBitrateKbps() {
        return bitrateKbps;
    }

    public boolean isCaptureSysKeys() {
        return captureSysKeys;
    }

    public int getGame_fps() {
        return game_fps;
    }

    public boolean isVsyncEnabled() {
        return isVsyncEnabled;
    }

    public long getMaxBitrateKbps() {
        return maxBitrateKbps;
    }

    public int getMax_fps() {
        return max_fps;
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
